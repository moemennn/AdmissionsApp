package edu.fandm.mibrahi1.admissionsapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;

/**
 * PURPOSE: Displays the interactive campus map with custom markers.
 * INTERFACES: OnMapReadyCallback is required to initialize the Google Map object.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    // Key used to save and restore the map's state during screen rotations
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    private MapView mapView;

    /**
     * HASHMAP: Maps Marker Titles (String) to Building Objects.
     * WHY: Google Markers can't store complex data, so we "bridge" them here.
     */
    private final HashMap<String, Building> markerDataMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Standard MapView setup required by Google SDK
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(mapViewBundle);

        // This triggers the onMapReady callback once the map is initialized
        mapView.getMapAsync(this);
    }

    /**
     * onMapReady: Called when the map is ready to be configured.
     * logic: Handles camera constraints, zoom, and click listeners.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        // Enable UI interactions like pinching to zoom or rotating the map
        map.getUiSettings().setScrollGesturesEnabled(true);
        map.getUiSettings().setZoomGesturesEnabled(true);
        map.getUiSettings().setTiltGesturesEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(true);

        // Define the geographic boundaries of F&M Campus
        LatLngBounds fandmBounds = new LatLngBounds(
                new LatLng(40.0460, -76.3250), // Southwest corner
                new LatLng(40.0570, -76.3100)  // Northeast corner
        );

        // Constrain the camera so users stay focused on campus
        map.setLatLngBoundsForCameraTarget(fandmBounds);
        map.setMinZoomPreference(14f);

        // Set the initial camera position to the center of campus
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(40.0513665, -76.3196468), 16f
        ));

        // When a user taps a pin, show the little popup window above it
        map.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        /**
         * InfoWindowClick: Handles the action when the popup window is tapped.
         * Decision Logic: Open App details OR launch External Google Maps.
         */
        map.setOnInfoWindowClickListener(marker -> {
            String buildingName = marker.getTitle();
            Building building = markerDataMap.get(buildingName);

            if (building != null) {
                // If building exists in our repository, navigate to our internal detail screen
                NavigationHelper.startBuildingActivity(
                        this,
                        building.getDescription(),
                        building.getImageFileNames(),
                        building.getLatitude(),
                        building.getLongitude(),
                        building.getVideoLink()
                );
            } else {
                // If building data is missing, use an Implicit Intent to open external Navigation
                LatLng destination = marker.getPosition();
                Uri uri = Uri.parse("google.navigation:q="
                        + destination.latitude + ","
                        + destination.longitude
                        + "&mode=w"); // 'w' stands for walking mode

                Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });

        // Trigger the background data fetch for the pins
        loadBuildingMarkers(map);
    }

    /**
     * loadBuildingMarkers: Asks the repository for data and adds pins to map.
     * Pattern: Uses an asynchronous callback to prevent UI freezing.
     */
    private void loadBuildingMarkers(GoogleMap map) {
        BuildingRepository.getAllBuildings(this, buildings -> {
            for (Building building : buildings) {
                try {
                    // Skip entries without names
                    if (building.getName().isEmpty()) {
                        continue;
                    }

                    double lat = building.getLatitude();
                    double lng = building.getLongitude();

                    // Skip entries without valid geographic coordinates
                    if (lat == 0.0 || lng == 0.0) {
                        continue;
                    }

                    // Place the marker on the Google Map
                    Marker marker = map.addMarker(new MarkerOptions()
                            .position(new LatLng(lat, lng))
                            .title(building.getName())
                            .snippet("Tap again for building details")
                            .icon(BitmapDescriptorFactory.defaultMarker(
                                    getMarkerColor(building.getType())
                            )));

                    // Store the marker-to-building mapping for later lookup
                    if (marker != null) {
                        markerDataMap.put(building.getName(), building);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error adding marker for building: " + building.getName(), e);
                }
            }
        });
    }

    /**
     * getMarkerColor: Maps building categories to specific pin colors.
     * UI Purpose: Allows users to visually distinguish between Dorms, Academic halls, etc.
     */
    private float getMarkerColor(String type) {
        if (type == null) {
            return BitmapDescriptorFactory.HUE_RED;
        }

        switch (type.toLowerCase().trim()) {
            case "special":
                return BitmapDescriptorFactory.HUE_GREEN;
            case "administrative":
                return BitmapDescriptorFactory.HUE_BLUE;
            case "academic":
                return BitmapDescriptorFactory.HUE_YELLOW;
            case "athletics":
                return BitmapDescriptorFactory.HUE_MAGENTA;
            case "studentlife":
                return BitmapDescriptorFactory.HUE_ORANGE;
            case "dorms":
                return BitmapDescriptorFactory.HUE_VIOLET;
            default:
                return BitmapDescriptorFactory.HUE_RED;
        }
    }

    /**
     * LIFECYCLE METHODS:
     * The Google MapView must be synced with the Activity lifecycle
     * to manage memory and ensure the map cleans up correctly.
     */

    @Override protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}