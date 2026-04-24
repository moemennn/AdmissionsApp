package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BuildingRepository {

    private static final String TAG = "BuildingRepository";
    private static final String COLLECTION_NAME = "Buildings";

    public interface OnBuildingsLoadedCallback {
        void onBuildingsLoaded(List<Building> buildings);
    }

    public interface OnBuildingLoadedCallback {
        void onBuildingLoaded(Building building);
    }

    public static void getAllBuildings(Context context, OnBuildingsLoadedCallback callback) {
        FirebaseFetcher firebaseFetcher = new FirebaseFetcher(context);

        firebaseFetcher.fetchCollectionWithCache(COLLECTION_NAME, new FirebaseFetcher.OnCollectionLoadedListener() {
            @Override
            public void onSuccess(List<FirebaseFetcher.FirebaseRow> rows) {
                List<Building> buildings = new ArrayList<>();

                for (FirebaseFetcher.FirebaseRow row : rows) {
                    Building building = rowToBuilding(row);

                    if (building != null) {
                        buildings.add(building);
                    }
                }

                callback.onBuildingsLoaded(buildings);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load buildings", e);
                callback.onBuildingsLoaded(new ArrayList<>());
            }
        });
    }

    public static void getBuildingByName(Context context, String buildingName, OnBuildingLoadedCallback callback) {
        getAllBuildings(context, buildings -> {
            for (Building building : buildings) {
                if (building.getName().equalsIgnoreCase(buildingName.trim())) {
                    callback.onBuildingLoaded(building);
                    return;
                }
            }

            callback.onBuildingLoaded(null);
        });
    }

    private static Building rowToBuilding(FirebaseFetcher.FirebaseRow row) {
        try {
            String name = row.getString("Building");
            String description = row.getString("Descriptions");
            String imageFileNames = row.getString("Images (ids, comma seperated)");
            String type = row.getString("Type");
            String videoLink = row.getString("Youtube video link");

            double latitude = parseDouble(row.getString("latitude"));
            double longitude = parseDouble(row.getString("longitude"));

            if (name.isEmpty()) {
                return null;
            }

            return new Building(
                    name,
                    description,
                    imageFileNames,
                    type,
                    videoLink,
                    latitude,
                    longitude
            );

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse building row", e);
            return null;
        }
    }

    private static double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }

        return Double.parseDouble(value.trim());
    }
}