package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

// --- Data Models ---

/**
 * Holds the contact information for a single admissions counselor.
 */
class CounselorInfo {
    public final String name;
    public final String email;
    public final String profileLink;

    CounselorInfo(String name, String email, String profileLink) {
        this.name = name;
        this.email = email;
        this.profileLink = profileLink;
    }
}

/**
 * Wraps the result of looking up a territory.
 * Contains the counselor's name AND their full contact info (if available).
 */
class TerritoryResult {
    //duplicate counselor name here as a failsafe in case the counselorInfo hasn't been loaded yet
    public final String counselorName;
    public final CounselorInfo counselorInfo;

    TerritoryResult(String counselorName, CounselorInfo counselorInfo) {
        this.counselorName = counselorName;
        this.counselorInfo = counselorInfo;
    }
}

// --- Repository ---

/**
 * TerritoryRepository is the main class responsible for:
 *   1. Downloading admissions territory data from a Google Sheet
 *   2. Caching that data on the device so we don't re-download it every time
 *   3. Providing lookup methods so other parts of the app can find counselors
 */
public class TerritoryRepository {

    /**
     * This interface (callback) lets other classes know when data has finished loading.
     * Because downloading data from the internet takes time, we notify the caller
     * when each tab is ready instead of making them wait.
     */
    public interface OnDataLoadedListener {
        // Called each time one sheet tab finishes loading.
        // 'autocompleteItems' is the list of searchable values (e.g., state names, counties).
        void onTabLoaded(String tabName, List<String> autocompleteItems);

        // Called once ALL tabs have finished loading.
        void onAllTabsLoaded();
    }

    /**
     * A simple callback used when we need to return a list of schools or counties
     * asynchronously
     */
    public interface OnSchoolsLoadedCallback {
        void onSchoolsLoaded(List<String> items);
    }

    /**
     * A small helper class that pairs a tab name with the column index
     * we should use for autocomplete suggestions.
     * For example, "Territory By State" uses column 0 (the state name column).
     */
    private static class TabConfig {
        final String tabName;
        final int autocompleteColumn; // -1 means "don't add anything to autocomplete for this tab"

        TabConfig(String tabName, int autocompleteColumn) {
            this.tabName = tabName;
            this.autocompleteColumn = autocompleteColumn;
        }
    }

    // All the sheet tabs we need to load, and which column to use for autocomplete.
    private static final TabConfig[] TAB_CONFIGS = {
            new TabConfig("International by Country",   0),
            new TabConfig("Territory By State",         0),
            new TabConfig("NJ Territories By County",   0),
            new TabConfig("VA Territories By County",   0),
            new TabConfig("NY Territories By County",   0),
            new TabConfig("PA Territories By County",   0),
            new TabConfig("Admission Rep Contact Info", -1), // contact info tab, not used for autocomplete
    };

    // The name of our shared preferences file (used to remember when we last downloaded data)
    private static final String PREFS_NAME = "SheetPrefs";
    // The key used to store/retrieve the "last modified" version string
    private static final String PREFS_LAST_MODIFIED = "lastModified";
    // Total number of tabs we need to load (used to know when ALL loading is done)
    private static final int    TOTAL_FETCH_TASKS   = TAB_CONFIGS.length;

    // Maps territory/country/county names → counselor names, organized by tab
    private final Map<String, Map<String, String>> territoryMaps = new HashMap<>();
    // Maps school names → counselor names (only for NY and PA tabs)
    private final Map<String, Map<String, String>> schoolMaps    = new HashMap<>();
    // Maps counselor names → their full contact info (from the "Admission Rep Contact Info" tab)
    private final Map<String, CounselorInfo> counselorInfoMap    = new HashMap<>();

    private final OnDataLoadedListener listener;
    private final Context context;
    // A thread pool so we can download multiple tabs at the same time (faster loading)
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    // Keeps a thread-safe count of how many tabs have finished loading
    private final AtomicInteger fetchTasksCompleted = new AtomicInteger(0);

    /**
     * Constructor: sets up the repository with empty maps for each tab.
     *
     * @param context  Android context (needed for file/cache access)
     * @param listener Callback that gets notified when data finishes loading
     */
    public TerritoryRepository(Context context, OnDataLoadedListener listener) {
        this.context  = context;
        this.listener = listener;

        // Pre-create an empty map for every tab so we can safely put data into them later
        for (TabConfig config : TAB_CONFIGS) {
            territoryMaps.put(config.tabName, new HashMap<>());
        }
        // Only NY and PA have school-level data, so only create school maps for those
        schoolMaps.put("NY Territories By County", new HashMap<>());
        schoolMaps.put("PA Territories By County", new HashMap<>());
    }

    // -------------------------------------------------------------------------
    // Public API – methods other classes call to use this repository
    // -------------------------------------------------------------------------

    /**
     * Kicks off the data loading process.
     * Runs in the background so the UI doesn't freeze while data is being fetched.
     * The listener will be notified as each tab finishes loading.
     */
    public void fetchAll() {
        executor.execute(this::checkLastModifiedThenLoad);
    }

    /**
     * Looks up which counselor is responsible for a given territory, county, or school.
     *
     * Searches through both territory maps and school maps.
     * Returns null if no match is found.
     *
     * @param territory The search term (e.g., "Lancaster County", "Japan", "Lincoln High School")
     * @return A TerritoryResult with the counselor's name and contact info, or null if not found
     */
    public TerritoryResult lookupTerritory(String territory) {
        // Check all territory maps first (countries, states, counties)
        for (Map<String, String> map : territoryMaps.values()) {
            if (map.containsKey(territory)) {
                return buildResult(map.get(territory));
            }
        }
        // Then check school maps (individual high schools in NY and PA)
        for (Map<String, String> map : schoolMaps.values()) {
            if (map.containsKey(territory)) {
                return buildResult(map.get(territory));
            }
        }
        return null; // Nothing found
    }

    /**
     * Gets a list of all schools in a given state (currently only NY and PA have school data).
     * The result is returned via a callback because map lookups may eventually be async.
     *
     * @param state    The state name, e.g., "New York" or "Pennsylvania"
     * @param callback Called with the list of school names once ready
     */
    public void getSchoolsForState(String state, OnSchoolsLoadedCallback callback) {
        String tabName = stateToTabName(state);
        Map<String, String> schoolMap = schoolMaps.get(tabName);
        if (schoolMap != null) {
            callback.onSchoolsLoaded(new ArrayList<>(schoolMap.keySet()));
        } else {
            callback.onSchoolsLoaded(new ArrayList<>()); // Return empty list if no data
        }
    }

    /**
     * Gets a list of all counties for a given state.
     * Used to populate dropdowns or autocomplete fields.
     *
     * @param state    The state name, e.g., "New Jersey" or "Virginia"
     * @param callback Called with the list of county names once ready
     */
    public void getCountiesForState(String state, OnSchoolsLoadedCallback callback) {
        String tabName = stateToTabName(state);
        Map<String, String> territoryMap = territoryMaps.get(tabName);
        if (territoryMap != null) {
            callback.onSchoolsLoaded(new ArrayList<>(territoryMap.keySet()));
        } else {
            callback.onSchoolsLoaded(new ArrayList<>()); // Return empty list if no data
        }
    }

    // -------------------------------------------------------------------------
    // Last Modified check – decides whether to re-download or use the cache
    // -------------------------------------------------------------------------

    /**
     * Checks if the Google Sheet has been updated since we last downloaded it.
     * If so (or if there's no cached data), it re-downloads everything.
     * If the cache is still fresh, it loads from local files instead (much faster).
     */
    private void checkLastModifiedThenLoad() {
        String remoteVersion = fetchLastModified(); // Get the sheet's current version string
        String localVersion  = getStoredLastModified(); // Get the version we last saved

        // Check if any of our cache files are missing from disk
        boolean anyCacheMissing = false;
        for (TabConfig config : TAB_CONFIGS) {
            if (!cacheFileExists(config.tabName)) {
                anyCacheMissing = true;
                break;
            }
        }

        // We need to re-fetch if: cache files are gone, versions don't match, or remote version is unknown
        boolean shouldRefetch = anyCacheMissing || remoteVersion.isEmpty() || !remoteVersion.equals(localVersion);

        if (shouldRefetch) {
            Log.d("TerritoryRepository", "Re-fetching all tabs. Remote: "
                    + remoteVersion + " Local: " + localVersion);
            clearCacheFiles(); // Delete old cache so fresh data will be saved
            if (!remoteVersion.isEmpty()) {
                saveLastModified(remoteVersion); // Remember the new version
            }
        } else {
            Log.d("TerritoryRepository", "Cache is up to date, loading from disk.");
        }

        // Start loading every tab (each runs in its own background thread)
        for (TabConfig config : TAB_CONFIGS) {
            executor.execute(() -> loadTab(config));
        }
    }

    /**
     * Downloads the "Last Modified" tab from the Google Sheet.
     * This tab contains a single value that tells us when the sheet was last updated.
     * We use this to avoid re-downloading data that hasn't changed.
     *
     * @return The version string from the sheet, or an empty string if the request fails
     */
    private String fetchLastModified() {
        List<String> lines = fetchRawLines("Last Modified");
        List<SheetFetcher.SheetRow> rows = parseRows(lines, true);
        if (!rows.isEmpty()) {
            return rows.get(0).get(0); // The version is in the first cell
        }
        return "";
    }

    /**
     * Reads the last-known sheet version from the device's shared preferences (local storage).
     * Returns an empty string if we've never saved a version before.
     */
    private String getStoredLastModified() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREFS_LAST_MODIFIED, "");
    }

    /**
     * Saves the current sheet version to the device's shared preferences.
     * Next time the app opens, we'll compare this saved value against the remote version.
     *
     * @param version The version string from the sheet (e.g., a timestamp or revision ID)
     */
    private void saveLastModified(String version) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREFS_LAST_MODIFIED, version)
                .apply();
    }

    /**
     * Deletes all cached CSV files from the device's cache directory.
     * Called when we detect that the Google Sheet has been updated and we need fresh data.
     */
    private void clearCacheFiles() {
        for (TabConfig config : TAB_CONFIGS) {
            File file = new File(context.getCacheDir(), fileNameFor(config.tabName));
            if (file.exists()) {
                file.delete();
                Log.d("TerritoryRepository", "Deleted cache: " + fileNameFor(config.tabName));
            }
        }
    }

    // -------------------------------------------------------------------------
    // File helpers – reading and writing cached CSV data to disk
    // -------------------------------------------------------------------------

    /**
     * Checks whether a cached CSV file already exists on the device for this tab.
     *
     * @param tabName The name of the sheet tab (e.g., "Territory By State")
     * @return true if the file exists, false if it needs to be downloaded
     */
    public boolean cacheFileExists(String tabName) {
        return new File(context.getCacheDir(), fileNameFor(tabName)).exists();
    }

    /**
     * Saves the raw CSV lines for a sheet tab to the device's cache directory.
     * This way, we can reload the data quickly next time without hitting the network.
     *
     * @param tabName The name of the sheet tab
     * @param lines   The raw CSV lines downloaded from the sheet
     */
    public void saveCacheFile(String tabName, List<String> lines) {
        File file = new File(context.getCacheDir(), fileNameFor(tabName));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (Exception e) {
            Log.e("TerritoryRepository", "Failed to save cache for: " + tabName, e);
        }
    }

    /**
     * Reads the cached CSV lines for a sheet tab from disk.
     * Returns an empty list if the file can't be read.
     *
     * @param tabName The name of the sheet tab
     * @return A list of raw CSV lines from the file
     */
    public List<String> readCacheFile(String tabName) {
        List<String> lines = new ArrayList<>();
        File file = new File(context.getCacheDir(), fileNameFor(tabName));
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (Exception e) {
            Log.e("TerritoryRepository", "Failed to read cache for: " + tabName, e);
        }
        return lines;
    }

    /**
     * Converts a tab name into a safe filename for storage.
     * Spaces are replaced with underscores, and ".csv" is appended.
     * Example: "Territory By State" → "Territory_By_State.csv"
     *
     * @param tabName The human-readable tab name
     * @return A filename-safe string
     */
    private String fileNameFor(String tabName) {
        return tabName.replace(" ", "_") + ".csv";
    }

    // -------------------------------------------------------------------------
    // Internal loading logic – the core of the data pipeline
    // -------------------------------------------------------------------------

    /**
     * Loads a single sheet tab, either from the local cache or from the network.
     * After loading, it parses the CSV data and notifies the listener that this tab is ready.
     *
     * @param config The tab configuration (name and autocomplete column)
     */
    private void loadTab(TabConfig config) {
        List<String> rawLines;

        if (cacheFileExists(config.tabName)) {
            // Use cached data if available (avoids network call)
            Log.d("TerritoryRepository", "Loading from cache: " + config.tabName);
            rawLines = readCacheFile(config.tabName);
        } else {
            // Download fresh data from the Google Sheet
            Log.d("TerritoryRepository", "Fetching from network: " + config.tabName);
            rawLines = fetchRawLines(config.tabName);
            if (!rawLines.isEmpty()) {
                saveCacheFile(config.tabName, rawLines); // Save for next time
            }
        }

        // Convert raw CSV text into structured row objects, then extract useful data
        List<SheetFetcher.SheetRow> rows = parseRows(rawLines, true);
        List<String> autocompleteItems = processRows(config, rows);

        // Switch back to the main (UI) thread to notify the listener safely
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.post(() -> {
            listener.onTabLoaded(config.tabName, autocompleteItems);
            // If this was the last tab to finish, notify that ALL tabs are done
            if (fetchTasksCompleted.incrementAndGet() == TOTAL_FETCH_TASKS) {
                Log.d("TerritoryRepository", "All sheet data loaded.");
                listener.onAllTabsLoaded();
            }
        });
    }

    /**
     * Downloads the raw CSV text for a given sheet tab over the network.
     * Uses Google Sheets' built-in CSV export URL format.
     *
     * @param tabName The name of the sheet tab to download
     * @return A list of raw CSV lines, or an empty list if the request fails
     */
    private List<String> fetchRawLines(String tabName) {
        List<String> lines = new ArrayList<>();
        try {
            // Encode spaces in the tab name so they work in a URL (" " → "%20")
            String encodedTab = tabName.replace(" ", "%20");
            String urlStr = "https://docs.google.com/spreadsheets/d/"
                    + SheetFetcher.SHEET_ID
                    + "/gviz/tq?tqx=out:csv&sheet=" + encodedTab;

            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // Read the response line by line
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
        } catch (Exception e) {
            Log.e("TerritoryRepository", "Network fetch failed for: " + tabName, e);
        }
        return lines;
    }

    /**
     * Converts raw CSV lines into a list of SheetRow objects (easier to work with).
     * Optionally skips the first line (the header row).
     *
     * @param lines      The raw CSV text lines
     * @param skipHeader If true, the first line (column headers) is skipped
     * @return A list of parsed rows ready for data extraction
     */
    private List<SheetFetcher.SheetRow> parseRows(List<String> lines, boolean skipHeader) {
        List<SheetFetcher.SheetRow> rows = new ArrayList<>();
        boolean first = true;
        for (String line : lines) {
            if (first && skipHeader) { first = false; continue; } // Skip header row
            first = false;
            rows.add(new SheetFetcher.SheetRow(SheetFetcher.parseCSVLine(line)));
        }
        return rows;
    }

    /**
     * Extracts useful data from a parsed sheet tab and stores it in the appropriate maps.
     * Also builds the list of items to show in the autocomplete search bar.
     *
     * Each row is expected to have at least:
     *   - Column 0: Territory/county/country name
     *   - Column 1: Assigned counselor name
     *   - Column 3 & 4 (NY/PA only): School name and school counselor
     *   - Column 1 & 2 (Contact Info tab): Email and profile link
     *
     * @param config The tab configuration (tells us which column to use for autocomplete)
     * @param rows   The parsed rows from the sheet
     * @return A list of strings to populate the autocomplete search field
     */
    private List<String> processRows(TabConfig config, List<SheetFetcher.SheetRow> rows) {
        List<String> autocompleteItems = new ArrayList<>();
        Map<String, String> territoryMap = territoryMaps.get(config.tabName);

        for (SheetFetcher.SheetRow row : rows) {
            String territory = row.get(0); // e.g., "Lancaster County" or "Japan"
            String counselor = row.get(1); // e.g., "Jane Smith"

            if (territory.isEmpty()) continue; // Skip blank rows

            // Store the territory → counselor mapping
            if (territoryMap != null) {
                territoryMap.put(territory, counselor);
            }

            // Add to autocomplete list if this tab uses column 0 for suggestions
            if (config.autocompleteColumn == 0) {
                autocompleteItems.add(territory);
            }

            // NY and PA tabs also have individual school data in columns 3 and 4
            if (schoolMaps.containsKey(config.tabName)) {
                String school          = row.get(3); // School name
                String schoolCounselor = row.get(4); // Counselor for that school
                if (!school.isEmpty()) {
                    schoolMaps.get(config.tabName).put(school, schoolCounselor);
                }
            }

            // The contact info tab has email (col 1) and profile link (col 2) instead of territory data
            if (config.tabName.equals("Admission Rep Contact Info")) {
                String email       = row.get(1);
                String profileLink = row.get(2);
                // Store using the counselor's name as the key
                counselorInfoMap.put(territory, new CounselorInfo(territory, email, profileLink));
            }
        }
        return autocompleteItems;
    }

    /**
     * Creates a TerritoryResult using a counselor's name.
     * Looks up the counselor's full contact info from counselorInfoMap (may be null if not loaded yet).
     *
     * @param counselorName The name of the counselor to look up
     * @return A TerritoryResult containing the name and contact info
     */
    private TerritoryResult buildResult(String counselorName) {
        CounselorInfo info = counselorInfoMap.get(counselorName); // May be null if contact tab isn't loaded yet
        return new TerritoryResult(counselorName, info);
    }

    /**
     * Converts a state name (as it appears in the UI) into the corresponding Google Sheet tab name.
     * Some states have dedicated county-level tabs; others fall back to a default pattern.
     *
     * @param state The full state name, e.g., "New York"
     * @return The matching tab name, e.g., "NY Territories By County"
     */
    private String stateToTabName(String state) {
        switch (state) {
            case "New York":     return "NY Territories By County";
            case "Pennsylvania": return "PA Territories By County";
            case "New Jersey":   return "NJ Territories By County";
            case "Virginia":     return "VA Territories By County";
            default:             return state + " Territories By County"; // Fallback for other states
        }
    }
}