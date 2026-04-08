package edu.fandm.mibrahi1.admissionsapp;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final Map<String, String> stateCounselorMap    = new HashMap<>();
    private final Map<String, String> countryCounselorMap  = new HashMap<>();
    private final Map<String, String> njCountyCounselorMap = new HashMap<>();
    private final Map<String, String> vaCountyCounselorMap = new HashMap<>();
    private final Map<String, String> nyCountyCounselorMap = new HashMap<>();
    private final Map<String, String> nySchoolCounselorMap = new HashMap<>();
    private final Map<String, String> paCountyCounselorMap = new HashMap<>();
    private final Map<String, String> paSchoolCounselorMap = new HashMap<>();
    private final Map<String, CounselorInfo> counselorInfoMap = new HashMap<>();

    private final OnDataLoadedListener listener;
    private int fetchTasksCompleted = 0;
    private static final int TOTAL_FETCH_TASKS = 7;

    public TerritoryRepository(OnDataLoadedListener listener) {
        this.listener = listener;
    }

    public void fetchAll() {
        new FetchSheetTabTask("International by Country").execute();
        new FetchSheetTabTask("Territory By State").execute();
        new FetchSheetTabTask("NJ Territories By County").execute();
        new FetchSheetTabTask("VA Territories By County").execute();
        new FetchSheetTabTask("NY Territories By County").execute();
        new FetchSheetTabTask("PA Territories By County").execute();
        new FetchSheetTabTask("Admission Rep Contact Info").execute();
    }

    public TerritoryResult lookupTerritory(String territory) {
        String counselorName = null;

        if      (countryCounselorMap.containsKey(territory))  counselorName = countryCounselorMap.get(territory);
        else if (stateCounselorMap.containsKey(territory))    counselorName = stateCounselorMap.get(territory);
        else if (njCountyCounselorMap.containsKey(territory)) counselorName = njCountyCounselorMap.get(territory);
        else if (vaCountyCounselorMap.containsKey(territory)) counselorName = vaCountyCounselorMap.get(territory);
        else if (nyCountyCounselorMap.containsKey(territory)) counselorName = nyCountyCounselorMap.get(territory);
        else if (nySchoolCounselorMap.containsKey(territory)) counselorName = nySchoolCounselorMap.get(territory);
        else if (paCountyCounselorMap.containsKey(territory)) counselorName = paCountyCounselorMap.get(territory);
        else if (paSchoolCounselorMap.containsKey(territory)) counselorName = paSchoolCounselorMap.get(territory);

        if (counselorName == null) return null;

        CounselorInfo info = counselorInfoMap.get(counselorName);
        return new TerritoryResult(counselorName, info);
    }

    private class FetchSheetTabTask extends AsyncTask<Void, Void, List<SheetFetcher.SheetRow>> {

        private final String tabName;

        FetchSheetTabTask(String tabName) {
            this.tabName = tabName;
        }

        @Override
        protected List<SheetFetcher.SheetRow> doInBackground(Void... voids) {
            return SheetFetcher.fetch(tabName, true);
        }

        @Override
        protected void onPostExecute(List<SheetFetcher.SheetRow> rows) {
            List<String> autocompleteItems = new ArrayList<>();

            switch (tabName) {
                case "International by Country": {
                    for (SheetFetcher.SheetRow row : rows) {
                        String country   = row.get(0);
                        String counselor = row.get(1);
                        if (!country.isEmpty()) {
                            autocompleteItems.add(country);
                            countryCounselorMap.put(country, counselor);
                        }
                    }
                    break;
                }
                case "Territory By State": {
                    for (SheetFetcher.SheetRow row : rows) {
                        String state     = row.get(0);
                        String counselor = row.get(1);
                        if (!state.isEmpty()) {
                            autocompleteItems.add(state);
                            stateCounselorMap.put(state, counselor);
                        }
                    }
                    break;
                }
                case "NJ Territories By County": {
                    for (SheetFetcher.SheetRow row : rows) {
                        String county    = row.get(0);
                        String counselor = row.get(1);
                        if (!county.isEmpty()) njCountyCounselorMap.put(county, counselor);
                    }
                    break;
                }
                case "VA Territories By County": {
                    for (SheetFetcher.SheetRow row : rows) {
                        String county    = row.get(0);
                        String counselor = row.get(1);
                        if (!county.isEmpty()) vaCountyCounselorMap.put(county, counselor);
                    }
                    break;
                }
                case "NY Territories By County": {
                    for (SheetFetcher.SheetRow row : rows) {
                        String county          = row.get(0);
                        String counselor       = row.get(1);
                        String school          = row.get(3);
                        String schoolCounselor = row.get(4);
                        if (!county.isEmpty()) nyCountyCounselorMap.put(county, counselor);
                        if (!school.isEmpty()) nySchoolCounselorMap.put(school, schoolCounselor);
                    }
                    break;
                }
                case "PA Territories By County": {
                    for (SheetFetcher.SheetRow row : rows) {
                        String county          = row.get(0);
                        String counselor       = row.get(1);
                        String school          = row.get(3);
                        String schoolCounselor = row.get(4);
                        if (!county.isEmpty()) paCountyCounselorMap.put(county, counselor);
                        if (!school.isEmpty()) paSchoolCounselorMap.put(school, schoolCounselor);
                    }
                    break;
                }
                case "Admission Rep Contact Info": {
                    for (SheetFetcher.SheetRow row : rows) {
                        String name        = row.get(0);
                        String email       = row.get(1);
                        String profileLink = row.get(2);
                        if (!name.isEmpty()) {
                            counselorInfoMap.put(name, new CounselorInfo(name, email, profileLink));
                        }
                    }
                    break;
                }
            }

            listener.onTabLoaded(tabName, autocompleteItems);

            fetchTasksCompleted++;
            if (fetchTasksCompleted == TOTAL_FETCH_TASKS) {
                Log.d("TerritoryRepository", "All sheet data loaded.");
                listener.onAllTabsLoaded();
            }
        }
    }
}