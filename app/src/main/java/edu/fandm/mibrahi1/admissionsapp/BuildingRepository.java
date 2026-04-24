package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

// This class is responsible for getting Building data (from Firebase in this case)
public class BuildingRepository {

    // Tag used for logging errors in Logcat
    private static final String TAG = "BuildingRepository";

    // The name of the Firebase collection we are reading from
    private static final String COLLECTION_NAME = "Buildings";

    // Callback interface for when a LIST of buildings is loaded
    public interface OnBuildingsLoadedCallback {
        void onBuildingsLoaded(List<Building> buildings);
    }

    // Callback interface for when a SINGLE building is loaded
    public interface OnBuildingLoadedCallback {
        void onBuildingLoaded(Building building);
    }

    // Fetch all buildings from Firebase
    public static void getAllBuildings(Context context, OnBuildingsLoadedCallback callback) {

        // Create a helper object that handles Firebase communication
        FirebaseFetcher firebaseFetcher = new FirebaseFetcher(context);

        // Request the collection (with caching support)
        firebaseFetcher.fetchCollectionWithCache(COLLECTION_NAME, new FirebaseFetcher.OnCollectionLoadedListener() {

            // Called when data is successfully retrieved
            @Override
            public void onSuccess(List<FirebaseFetcher.FirebaseRow> rows) {

                // Create an empty list to store Building objects
                List<Building> buildings = new ArrayList<>();

                // Loop through each row returned from Firebase
                for (FirebaseFetcher.FirebaseRow row : rows) {

                    // Convert the raw Firebase row into a Building object
                    Building building = rowToBuilding(row);

                    // Only add valid buildings (skip null ones)
                    if (building != null) {
                        buildings.add(building);
                    }
                }

                // Send the final list back through the callback
                callback.onBuildingsLoaded(buildings);
            }

            // Called if something goes wrong (network error, etc.)
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load buildings", e);

                // Return an empty list instead of crashing the app
                callback.onBuildingsLoaded(new ArrayList<>());
            }
        });
    }

    // Find a single building by its name
    public static void getBuildingByName(Context context, String buildingName, OnBuildingLoadedCallback callback) {

        // First, get all buildings
        getAllBuildings(context, buildings -> {

            // Loop through each building
            for (Building building : buildings) {

                // Compare names (ignore case and extra spaces)
                if (building.getName().equalsIgnoreCase(buildingName.trim())) {

                    // If found, return it
                    callback.onBuildingLoaded(building);
                    return;
                }
            }

            // If no match is found, return null
            callback.onBuildingLoaded(null);
        });
    }

    // Convert a Firebase row into a Building object
    private static Building rowToBuilding(FirebaseFetcher.FirebaseRow row) {
        try {
            // Extract values from the row using column names
            String name = row.getString("Building");
            String description = row.getString("Descriptions");
            String imageFileNames = row.getString("Images (ids, comma seperated)");
            String type = row.getString("Type");
            String videoLink = row.getString("Youtube video link");

            // Convert latitude/longitude from String to double
            double latitude = parseDouble(row.getString("latitude"));
            double longitude = parseDouble(row.getString("longitude"));

            // If name is empty, this is not a valid building
            if (name.isEmpty()) {
                return null;
            }

            // Create and return a new Building object
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
            // If anything goes wrong during parsing, log the error
            Log.e(TAG, "Failed to parse building row", e);
            return null;
        }
    }

    // Helper method to safely convert a String to a double
    private static double parseDouble(String value) {

        // If the value is null or empty, return 0 instead of crashing
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }

        // Convert the string to a double
        return Double.parseDouble(value.trim());
    }
}