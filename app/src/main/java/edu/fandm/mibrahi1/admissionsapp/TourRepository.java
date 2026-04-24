package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TourRepository {

    private static final String TAG = "TourRepository";
    private static final String COLLECTION_NAME = "Tours";

    public interface OnToursLoadedCallback {
        void onToursLoaded(List<Tour> tours);
    }

    public interface OnTourLoadedCallback {
        void onTourLoaded(Tour tour);
    }

    public static void getTours(Context context, OnToursLoadedCallback callback) {
        FirebaseFetcher firebaseFetcher = new FirebaseFetcher(context);

        firebaseFetcher.fetchCollectionWithCache(COLLECTION_NAME, new FirebaseFetcher.OnCollectionLoadedListener() {
            @Override
            public void onSuccess(List<FirebaseFetcher.FirebaseRow> rows) {
                List<Tour> tours = new ArrayList<>();

                for (FirebaseFetcher.FirebaseRow row : rows) {
                    String name = row.getString("name");

                    List<String> stops = new ArrayList<>();
                    Object stopsObject = row.getData().get("stops");

                    if (stopsObject instanceof List<?>) {
                        for (Object stop : (List<?>) stopsObject) {
                            if (stop != null && !stop.toString().trim().isEmpty()) {
                                stops.add(stop.toString().trim());
                            }
                        }
                    }

                    if (!name.isEmpty()) {
                        tours.add(new Tour(name, stops));
                    }
                }

                callback.onToursLoaded(tours);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load tours", e);
                callback.onToursLoaded(new ArrayList<>());
            }
        });
    }

    public static void getTourByName(Context context, String name, OnTourLoadedCallback callback) {
        getTours(context, tours -> {
            for (Tour tour : tours) {
                if (tour.getName().equalsIgnoreCase(name)) {
                    callback.onTourLoaded(tour);
                    return;
                }
            }

            callback.onTourLoaded(null);
        });
    }
}