package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// Simple data class that holds counselor details
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

// Result object returned when a territory lookup is done
// It includes both the counselor's name AND their full info
class TerritoryResult {
    public final String counselorName;
    public final CounselorInfo counselorInfo;

    TerritoryResult(String counselorName, CounselorInfo counselorInfo) {
        this.counselorName = counselorName;
        this.counselorInfo = counselorInfo;
    }
}

// Main class responsible for loading and organizing territory + counselor data
public class TerritoryRepository {

    private static final String TAG = "TerritoryRepository";

    // Callback for UI: called when each tab's data is ready
    public interface OnDataLoadedListener {
        void onTabLoaded(String tabName, List<String> autocompleteItems);
        void onAllTabsLoaded(); // called when EVERYTHING finishes loading
    }

    // Callback for returning school/county lists
    public interface OnSchoolsLoadedCallback {
        void onSchoolsLoaded(List<String> items);
    }

    // Configuration for each Firebase collection
    // This avoids repeating similar code for each dataset
    private static class CollectionConfig {
        final String collectionName;   // Firebase collection name
        final String tabName;          // UI tab name
        final String locationField;    // column for location (state, county, school, etc.)
        final String counselorField;   // column for counselor name
        final boolean autocomplete;    // should this be used for autocomplete?

        CollectionConfig(String collectionName, String tabName,
                         String locationField, String counselorField,
                         boolean autocomplete) {
            this.collectionName = collectionName;
            this.tabName = tabName;
            this.locationField = locationField;
            this.counselorField = counselorField;
            this.autocomplete = autocomplete;
        }
    }

    // List of all collections we want to fetch from Firebase
    private static final CollectionConfig[] COLLECTION_CONFIGS = {
            new CollectionConfig("International_Countries", "International by Country",
                    "Addresses Country", "Staff Assigned Full Name", true),

            new CollectionConfig("US_States", "Territory By State",
                    "State", "Admission Counselor", true),

            new CollectionConfig("NJ_Counties", "NJ Territories By County",
                    "NJ County", "Admission Counselor", true),

            new CollectionConfig("VA_Counties", "VA Territories By County",
                    "VA County", "Admission Counselor", true),

            new CollectionConfig("NY_Counties", "NY Territories By County",
                    "NY County", "Admission Counselor", true),

            new CollectionConfig("PA_Counties", "PA Territories By County",
                    "PA County", "Admission Counselor", true),

            // School collections (not used for autocomplete)
            new CollectionConfig("NY_Schools", "NY Schools",
                    "School Name", "Admission Counselor", false),

            new CollectionConfig("PA_Schools", "PA Schools",
                    "School Name", "Admission Counselor", false)
    };

    // Total number of async fetch tasks (all collections + counselors)
    private static final int TOTAL_FETCH_TASKS = COLLECTION_CONFIGS.length + 1;

    private final Context context;
    private final OnDataLoadedListener listener;
    private final FirebaseFetcher firebaseFetcher;

    // Maps location → counselor name
    private final Map<String, Map<String, String>> territoryMaps = new HashMap<>();

    // Separate maps for schools
    private final Map<String, Map<String, String>> schoolMaps = new HashMap<>();

    // Map counselor name → full counselor info
    private final Map<String, CounselorInfo> counselorInfoMap = new HashMap<>();

    // Tracks how many async tasks have finished
    private final AtomicInteger fetchTasksCompleted = new AtomicInteger(0);

    public TerritoryRepository(Context context, OnDataLoadedListener listener) {
        this.context = context;
        this.listener = listener;
        this.firebaseFetcher = new FirebaseFetcher(context);

        // Initialize empty maps for each tab
        for (CollectionConfig config : COLLECTION_CONFIGS) {
            territoryMaps.put(config.tabName, new HashMap<>());
        }

        // Initialize school maps
        schoolMaps.put("NY Schools", new HashMap<>());
        schoolMaps.put("PA Schools", new HashMap<>());
    }

    // Start fetching ALL data
    public void fetchAll() {
        fetchTasksCompleted.set(0);

        // Fetch counselor info first
        fetchCounselors();

        // Fetch all territory/school collections
        for (CollectionConfig config : COLLECTION_CONFIGS) {
            fetchTerritoryCollection(config);
        }
    }

    // Look up a territory (state, county, school, etc.)
    public TerritoryResult lookupTerritory(String territory) {

        // Check territory maps first
        for (Map<String, String> map : territoryMaps.values()) {
            if (map.containsKey(territory)) {
                return buildResult(map.get(territory));
            }
        }

        // Then check school maps
        for (Map<String, String> map : schoolMaps.values()) {
            if (map.containsKey(territory)) {
                return buildResult(map.get(territory));
            }
        }

        // Not found
        return null;
    }

    // Get all schools for a given state
    public void getSchoolsForState(String state, OnSchoolsLoadedCallback callback) {
        String schoolMapName = stateToSchoolMapName(state);
        Map<String, String> schoolMap = schoolMaps.get(schoolMapName);

        if (schoolMap != null) {
            callback.onSchoolsLoaded(new ArrayList<>(schoolMap.keySet()));
        } else {
            callback.onSchoolsLoaded(new ArrayList<>());
        }
    }

    // Get all counties for a given state
    public void getCountiesForState(String state, OnSchoolsLoadedCallback callback) {
        String tabName = stateToCountyTabName(state);
        Map<String, String> territoryMap = territoryMaps.get(tabName);

        if (territoryMap != null) {
            callback.onSchoolsLoaded(new ArrayList<>(territoryMap.keySet()));
        } else {
            callback.onSchoolsLoaded(new ArrayList<>());
        }
    }

    // ================================
    // FETCHING DATA FROM FIREBASE
    // ================================

    // Fetch counselor info (name, email, profile)
    private void fetchCounselors() {
        firebaseFetcher.fetchCollectionWithCache("Counselors", new FirebaseFetcher.OnCollectionLoadedListener() {

            @Override
            public void onSuccess(List<FirebaseFetcher.FirebaseRow> rows) {
                counselorInfoMap.clear();

                for (FirebaseFetcher.FirebaseRow row : rows) {
                    String name = row.getString("Admission Counselor");
                    String email = row.getString("Email Address");
                    String profileLink = row.getString("Profile Link");

                    // Only store valid counselors
                    if (!name.isEmpty()) {
                        counselorInfoMap.put(name, new CounselorInfo(name, email, profileLink));
                    }
                }

                markTaskFinished();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to fetch counselors", e);
                markTaskFinished();
            }
        });
    }

    // Fetch one territory/school collection
    private void fetchTerritoryCollection(CollectionConfig config) {
        firebaseFetcher.fetchCollectionWithCache(config.collectionName, new FirebaseFetcher.OnCollectionLoadedListener() {

            @Override
            public void onSuccess(List<FirebaseFetcher.FirebaseRow> rows) {

                // List for autocomplete suggestions
                List<String> autocompleteItems = new ArrayList<>();

                // Get correct map for this tab
                Map<String, String> territoryMap = territoryMaps.get(config.tabName);
                if (territoryMap != null) {
                    territoryMap.clear();
                }

                // Process each row
                for (FirebaseFetcher.FirebaseRow row : rows) {
                    String location = row.getString(config.locationField);
                    String counselor = row.getString(config.counselorField);

                    // Skip empty rows
                    if (location.isEmpty()) {
                        continue;
                    }

                    // Store data in the correct map
                    if (config.collectionName.equals("NY_Schools")) {
                        schoolMaps.get("NY Schools").put(location, counselor);

                    } else if (config.collectionName.equals("PA_Schools")) {
                        schoolMaps.get("PA Schools").put(location, counselor);

                    } else if (territoryMap != null) {
                        territoryMap.put(location, counselor);
                    }

                    // Add to autocomplete if enabled
                    if (config.autocomplete) {
                        autocompleteItems.add(location);
                    }
                }

                // Notify UI that this tab is ready
                listener.onTabLoaded(config.tabName, autocompleteItems);

                markTaskFinished();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to fetch collection: " + config.collectionName, e);

                // Return empty data so UI doesn't break
                listener.onTabLoaded(config.tabName, new ArrayList<>());

                markTaskFinished();
            }
        });
    }

    // ================================
    // TASK TRACKING (VERY IMPORTANT)
    // ================================

    // Called every time a fetch completes
    private void markTaskFinished() {

        // When ALL async tasks are done → notify UI
        if (fetchTasksCompleted.incrementAndGet() == TOTAL_FETCH_TASKS) {
            listener.onAllTabsLoaded();
        }
    }

    // ================================
    // HELPER METHODS
    // ================================

    // Build final result combining counselor name + full info
    private TerritoryResult buildResult(String counselorName) {
        CounselorInfo info = counselorInfoMap.get(counselorName);
        return new TerritoryResult(counselorName, info);
    }

    // Convert state name → correct county tab name
    private String stateToCountyTabName(String state) {
        switch (state) {
            case "New York":
                return "NY Territories By County";
            case "Pennsylvania":
                return "PA Territories By County";
            case "New Jersey":
                return "NJ Territories By County";
            case "Virginia":
                return "VA Territories By County";
            default:
                return state + " Territories By County";
        }
    }

    // Convert state name → correct school map name
    private String stateToSchoolMapName(String state) {
        switch (state) {
            case "New York":
                return "NY Schools";
            case "Pennsylvania":
                return "PA Schools";
            default:
                return "";
        }
    }
}