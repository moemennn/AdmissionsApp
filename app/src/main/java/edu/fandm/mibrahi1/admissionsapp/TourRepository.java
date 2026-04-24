package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

// Repository for fetching Tour data from Firebase
public class TourRepository {

    private static final String TAG = "TourRepository";
    private static final String COLLECTION_NAME = "Tours";

    // Callback for returning a list of tours
    public interface OnToursLoadedCallback {
        void onToursLoaded(List<Tour> tours);
    }

    // Callback for returning a single tour
    public interface OnTourLoadedCallback {
        void onTourLoaded(Tour tour);
    }

    // Fetch all tours from Firebase
    public static void getTours(Context context, OnToursLoadedCallback callback) {
        FirebaseFetcher firebaseFetcher = new FirebaseFetcher(context);

        firebaseFetcher.fetchCollectionWithCache(COLLECTION_NAME, new FirebaseFetcher.OnCollectionLoadedListener() {

            @Override
            public void onSuccess(List<FirebaseFetcher.FirebaseRow> rows) {
                List<Tour> tours = new ArrayList<>();

                // Convert each row into a Tour object
                for (FirebaseFetcher.FirebaseRow row : rows) {
                    String name = row.getString("name");

                    // Extract "stops" field (stored as a list in Firebase)
                    List<String> stops = new ArrayList<>();
                    Object stopsObject = row.getData().get("stops");

                    if (stopsObject instanceof List<?>) {
                        for (Object stop : (List<?>) stopsObject) {
                            if (stop != null && !stop.toString().trim().isEmpty()) {
                                stops.add(stop.toString().trim());
                            }
                        }
                    }

                    // Only add valid tours
                    if (!name.isEmpty()) {
                        tours.add(new Tour(name, stops));
                    }
                }

                callback.onToursLoaded(tours);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load tours", e);

                // Return empty list on failure
                callback.onToursLoaded(new ArrayList<>());
            }
        });
    }

    // Find a specific tour by name
    public static void getTourByName(Context context, String name, OnTourLoadedCallback callback) {
        getTours(context, tours -> {

            for (Tour tour : tours) {
                if (tour.getName().equalsIgnoreCase(name)) {
                    callback.onTourLoaded(tour);
                    return;
                }
            }

            // Return null if not found
            callback.onTourLoaded(null);
        });
    }
}