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

class TerritoryResult {
    public final String counselorName;
    public final CounselorInfo counselorInfo;

    TerritoryResult(String counselorName, CounselorInfo counselorInfo) {
        this.counselorName = counselorName;
        this.counselorInfo = counselorInfo;
    }
}

// --- Repository ---

public class TerritoryRepository {

    public interface OnDataLoadedListener {
        void onTabLoaded(String tabName, List<String> autocompleteItems);
        void onAllTabsLoaded();
    }

    public interface OnSchoolsLoadedCallback {
        void onSchoolsLoaded(List<String> items);
    }

    private static class TabConfig {
        final String tabName;
        final int autocompleteColumn;

        TabConfig(String tabName, int autocompleteColumn) {
            this.tabName = tabName;
            this.autocompleteColumn = autocompleteColumn;
        }
    }

    private static final TabConfig[] TAB_CONFIGS = {
            new TabConfig("International by Country",   0),
            new TabConfig("Territory By State",         0),
            new TabConfig("NJ Territories By County",   0),
            new TabConfig("VA Territories By County",   0),
            new TabConfig("NY Territories By County",   0),
            new TabConfig("PA Territories By County",   0),
            new TabConfig("Admission Rep Contact Info", -1),
    };

    private static final String PREFS_NAME = "SheetPrefs";
    private static final String PREFS_LAST_MODIFIED = "lastModified";
    private static final int    TOTAL_FETCH_TASKS   = TAB_CONFIGS.length;

    private final Map<String, Map<String, String>> territoryMaps = new HashMap<>();
    private final Map<String, Map<String, String>> schoolMaps    = new HashMap<>();
    private final Map<String, CounselorInfo> counselorInfoMap    = new HashMap<>();

    private final OnDataLoadedListener listener;
    private final Context context;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final AtomicInteger fetchTasksCompleted = new AtomicInteger(0);

    public TerritoryRepository(Context context, OnDataLoadedListener listener) {
        this.context  = context;
        this.listener = listener;

        for (TabConfig config : TAB_CONFIGS) {
            territoryMaps.put(config.tabName, new HashMap<>());
        }
        schoolMaps.put("NY Territories By County", new HashMap<>());
        schoolMaps.put("PA Territories By County", new HashMap<>());
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void fetchAll() {
        executor.execute(this::checkLastModifiedThenLoad);
    }

    public TerritoryResult lookupTerritory(String territory) {
        for (Map<String, String> map : territoryMaps.values()) {
            if (map.containsKey(territory)) {
                return buildResult(map.get(territory));
            }
        }
        for (Map<String, String> map : schoolMaps.values()) {
            if (map.containsKey(territory)) {
                return buildResult(map.get(territory));
            }
        }
        return null;
    }

    public void getSchoolsForState(String state, OnSchoolsLoadedCallback callback) {
        String tabName = stateToTabName(state);
        Map<String, String> schoolMap = schoolMaps.get(tabName);
        if (schoolMap != null) {
            callback.onSchoolsLoaded(new ArrayList<>(schoolMap.keySet()));
        } else {
            callback.onSchoolsLoaded(new ArrayList<>());
        }
    }

    public void getCountiesForState(String state, OnSchoolsLoadedCallback callback) {
        String tabName = stateToTabName(state);
        Map<String, String> territoryMap = territoryMaps.get(tabName);
        if (territoryMap != null) {
            callback.onSchoolsLoaded(new ArrayList<>(territoryMap.keySet()));
        } else {
            callback.onSchoolsLoaded(new ArrayList<>());
        }
    }

    // -------------------------------------------------------------------------
    // Last Modified check
    // -------------------------------------------------------------------------

    private void checkLastModifiedThenLoad() {
        String remoteVersion = fetchLastModified();
        String localVersion  = getStoredLastModified();

        boolean anyCacheMissing = false;
        for (TabConfig config : TAB_CONFIGS) {
            if (!cacheFileExists(config.tabName)) {
                anyCacheMissing = true;
                break;
            }
        }

        // Re-fetch if: no cache files, version mismatch, or remote version is unknown
        boolean shouldRefetch = anyCacheMissing || remoteVersion.isEmpty() || !remoteVersion.equals(localVersion);

        if (shouldRefetch) {
            Log.d("TerritoryRepository", "Re-fetching all tabs. Remote: "
                    + remoteVersion + " Local: " + localVersion);
            clearCacheFiles();
            if (!remoteVersion.isEmpty()) {
                saveLastModified(remoteVersion);
            }
        } else {
            Log.d("TerritoryRepository", "Cache is up to date, loading from disk.");
        }

        for (TabConfig config : TAB_CONFIGS) {
            executor.execute(() -> loadTab(config));
        }
    }

    private String fetchLastModified() {
        List<String> lines = fetchRawLines("Last Modified");
        List<SheetFetcher.SheetRow> rows = parseRows(lines, true);
        if (!rows.isEmpty()) {
            return rows.get(0).get(0);
        }
        return "";
    }

    private String getStoredLastModified() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREFS_LAST_MODIFIED, "");
    }

    private void saveLastModified(String version) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREFS_LAST_MODIFIED, version)
                .apply();
    }

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
    // File helpers
    // -------------------------------------------------------------------------

    public boolean cacheFileExists(String tabName) {
        return new File(context.getCacheDir(), fileNameFor(tabName)).exists();
    }

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

    private String fileNameFor(String tabName) {
        return tabName.replace(" ", "_") + ".csv";
    }

    // -------------------------------------------------------------------------
    // Internal loading logic
    // -------------------------------------------------------------------------

    private void loadTab(TabConfig config) {
        List<String> rawLines;

        if (cacheFileExists(config.tabName)) {
            Log.d("TerritoryRepository", "Loading from cache: " + config.tabName);
            rawLines = readCacheFile(config.tabName);
        } else {
            Log.d("TerritoryRepository", "Fetching from network: " + config.tabName);
            rawLines = fetchRawLines(config.tabName);
            if (!rawLines.isEmpty()) {
                saveCacheFile(config.tabName, rawLines);
            }
        }

        List<SheetFetcher.SheetRow> rows = parseRows(rawLines, true);
        List<String> autocompleteItems = processRows(config, rows);

        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.post(() -> {
            listener.onTabLoaded(config.tabName, autocompleteItems);
            if (fetchTasksCompleted.incrementAndGet() == TOTAL_FETCH_TASKS) {
                Log.d("TerritoryRepository", "All sheet data loaded.");
                listener.onAllTabsLoaded();
            }
        });
    }

    private List<String> fetchRawLines(String tabName) {
        List<String> lines = new ArrayList<>();
        try {
            String encodedTab = tabName.replace(" ", "%20");
            String urlStr = "https://docs.google.com/spreadsheets/d/"
                    + SheetFetcher.SHEET_ID
                    + "/gviz/tq?tqx=out:csv&sheet=" + encodedTab;

            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

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

    private List<SheetFetcher.SheetRow> parseRows(List<String> lines, boolean skipHeader) {
        List<SheetFetcher.SheetRow> rows = new ArrayList<>();
        boolean first = true;
        for (String line : lines) {
            if (first && skipHeader) { first = false; continue; }
            first = false;
            rows.add(new SheetFetcher.SheetRow(SheetFetcher.parseCSVLine(line)));
        }
        return rows;
    }

    private List<String> processRows(TabConfig config, List<SheetFetcher.SheetRow> rows) {
        List<String> autocompleteItems = new ArrayList<>();
        Map<String, String> territoryMap = territoryMaps.get(config.tabName);

        for (SheetFetcher.SheetRow row : rows) {
            String territory = row.get(0);
            String counselor = row.get(1);

            if (territory.isEmpty()) continue;

            if (territoryMap != null) {
                territoryMap.put(territory, counselor);
            }

            if (config.autocompleteColumn == 0) {
                autocompleteItems.add(territory);
            }

            if (schoolMaps.containsKey(config.tabName)) {
                String school          = row.get(3);
                String schoolCounselor = row.get(4);
                if (!school.isEmpty()) {
                    schoolMaps.get(config.tabName).put(school, schoolCounselor);
                }
            }

            if (config.tabName.equals("Admission Rep Contact Info")) {
                String email       = row.get(1);
                String profileLink = row.get(2);
                counselorInfoMap.put(territory, new CounselorInfo(territory, email, profileLink));
            }
        }
        return autocompleteItems;
    }
    private TerritoryResult buildResult(String counselorName) {
        CounselorInfo info = counselorInfoMap.get(counselorName);
        return new TerritoryResult(counselorName, info);
    }
    private String stateToTabName(String state) {
        switch (state) {
            case "New York":     return "NY Territories By County";
            case "Pennsylvania": return "PA Territories By County";
            case "New Jersey":   return "NJ Territories By County";
            case "Virginia":     return "VA Territories By County";
            default:             return state + " Territories By County";
        }
    }
}