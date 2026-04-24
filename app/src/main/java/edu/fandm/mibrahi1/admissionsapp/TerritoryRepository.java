package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

public class TerritoryRepository {

    private static final String TAG = "TerritoryRepository";

    public interface OnDataLoadedListener {
        void onTabLoaded(String tabName, List<String> autocompleteItems);
        void onAllTabsLoaded();
    }

    public interface OnSchoolsLoadedCallback {
        void onSchoolsLoaded(List<String> items);
    }

    private static class CollectionConfig {
        final String collectionName;
        final String tabName;
        final String locationField;
        final String counselorField;
        final boolean autocomplete;

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

    private static final CollectionConfig[] COLLECTION_CONFIGS = {
            new CollectionConfig(
                    "International_Countries",
                    "International by Country",
                    "Addresses Country",
                    "Staff Assigned Full Name",
                    true
            ),
            new CollectionConfig(
                    "US_States",
                    "Territory By State",
                    "State",
                    "Admission Counselor",
                    true
            ),
            new CollectionConfig(
                    "NJ_Counties",
                    "NJ Territories By County",
                    "NJ County",
                    "Admission Counselor",
                    true
            ),
            new CollectionConfig(
                    "VA_Counties",
                    "VA Territories By County",
                    "VA County",
                    "Admission Counselor",
                    true
            ),
            new CollectionConfig(
                    "NY_Counties",
                    "NY Territories By County",
                    "NY County",
                    "Admission Counselor",
                    true
            ),
            new CollectionConfig(
                    "PA_Counties",
                    "PA Territories By County",
                    "PA County",
                    "Admission Counselor",
                    true
            ),
            new CollectionConfig(
                    "NY_Schools",
                    "NY Schools",
                    "School Name",
                    "Admission Counselor",
                    false
            ),
            new CollectionConfig(
                    "PA_Schools",
                    "PA Schools",
                    "School Name",
                    "Admission Counselor",
                    false
            )
    };

    private static final int TOTAL_FETCH_TASKS = COLLECTION_CONFIGS.length + 1;

    private final Context context;
    private final OnDataLoadedListener listener;
    private final FirebaseFetcher firebaseFetcher;

    private final Map<String, Map<String, String>> territoryMaps = new HashMap<>();
    private final Map<String, Map<String, String>> schoolMaps = new HashMap<>();
    private final Map<String, CounselorInfo> counselorInfoMap = new HashMap<>();

    private final AtomicInteger fetchTasksCompleted = new AtomicInteger(0);

    public TerritoryRepository(Context context, OnDataLoadedListener listener) {
        this.context = context;
        this.listener = listener;
        this.firebaseFetcher = new FirebaseFetcher(context);

        for (CollectionConfig config : COLLECTION_CONFIGS) {
            territoryMaps.put(config.tabName, new HashMap<>());
        }

        schoolMaps.put("NY Schools", new HashMap<>());
        schoolMaps.put("PA Schools", new HashMap<>());
    }

    public void fetchAll() {
        fetchTasksCompleted.set(0);

        fetchCounselors();

        for (CollectionConfig config : COLLECTION_CONFIGS) {
            fetchTerritoryCollection(config);
        }
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
        String schoolMapName = stateToSchoolMapName(state);
        Map<String, String> schoolMap = schoolMaps.get(schoolMapName);

        if (schoolMap != null) {
            callback.onSchoolsLoaded(new ArrayList<>(schoolMap.keySet()));
        } else {
            callback.onSchoolsLoaded(new ArrayList<>());
        }
    }

    public void getCountiesForState(String state, OnSchoolsLoadedCallback callback) {
        String tabName = stateToCountyTabName(state);
        Map<String, String> territoryMap = territoryMaps.get(tabName);

        if (territoryMap != null) {
            callback.onSchoolsLoaded(new ArrayList<>(territoryMap.keySet()));
        } else {
            callback.onSchoolsLoaded(new ArrayList<>());
        }
    }

    private void fetchCounselors() {
        firebaseFetcher.fetchCollectionWithCache("Counselors", new FirebaseFetcher.OnCollectionLoadedListener() {
            @Override
            public void onSuccess(List<FirebaseFetcher.FirebaseRow> rows) {
                counselorInfoMap.clear();

                for (FirebaseFetcher.FirebaseRow row : rows) {
                    String name = row.getString("Admission Counselor");
                    String email = row.getString("Email Address");
                    String profileLink = row.getString("Profile Link");

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

    private void fetchTerritoryCollection(CollectionConfig config) {
        firebaseFetcher.fetchCollectionWithCache(config.collectionName, new FirebaseFetcher.OnCollectionLoadedListener() {
            @Override
            public void onSuccess(List<FirebaseFetcher.FirebaseRow> rows) {
                List<String> autocompleteItems = new ArrayList<>();

                Map<String, String> territoryMap = territoryMaps.get(config.tabName);
                if (territoryMap != null) {
                    territoryMap.clear();
                }

                for (FirebaseFetcher.FirebaseRow row : rows) {
                    String location = row.getString(config.locationField);
                    String counselor = row.getString(config.counselorField);

                    if (location.isEmpty()) {
                        continue;
                    }

                    if (config.collectionName.equals("NY_Schools")) {
                        schoolMaps.get("NY Schools").put(location, counselor);
                    } else if (config.collectionName.equals("PA_Schools")) {
                        schoolMaps.get("PA Schools").put(location, counselor);
                    } else if (territoryMap != null) {
                        territoryMap.put(location, counselor);
                    }

                    if (config.autocomplete) {
                        autocompleteItems.add(location);
                    }
                }

                listener.onTabLoaded(config.tabName, autocompleteItems);
                markTaskFinished();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to fetch collection: " + config.collectionName, e);
                listener.onTabLoaded(config.tabName, new ArrayList<>());
                markTaskFinished();
            }
        });
    }

    private void markTaskFinished() {
        if (fetchTasksCompleted.incrementAndGet() == TOTAL_FETCH_TASKS) {
            listener.onAllTabsLoaded();
        }
    }

    private TerritoryResult buildResult(String counselorName) {
        CounselorInfo info = counselorInfoMap.get(counselorName);
        return new TerritoryResult(counselorName, info);
    }

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