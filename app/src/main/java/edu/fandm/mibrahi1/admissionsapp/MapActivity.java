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
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    private final HashMap<String, SheetFetcher.SheetRow> markerDataMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        // Enable all gestures
        map.getUiSettings().setScrollGesturesEnabled(true);
        map.getUiSettings().setZoomGesturesEnabled(true);
        map.getUiSettings().setTiltGesturesEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(true);

        // Lock camera to F&M campus area
        LatLngBounds fandmBounds = new LatLngBounds(
                new LatLng(40.0460, -76.3250),
                new LatLng(40.0570, -76.3100)
        );
        map.setLatLngBoundsForCameraTarget(fandmBounds);
        map.setMinZoomPreference(14f);

        // Center camera on Lombardo Welcome Center
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(40.0513665, -76.3196468), 16f));

        // Tap marker → show info window
        map.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        // Tap info window → open Building Detail activity
        map.setOnInfoWindowClickListener(marker -> {
            String buildingName = marker.getTitle();
            SheetFetcher.SheetRow row = markerDataMap.get(buildingName);

            if (row != null) {
                String description    = row.get(4);
                String imageFileNames = row.get(5);
                String videoId        = row.get(6); // empty for now, ready for future
                double lat            = marker.getPosition().latitude;
                double lng            = marker.getPosition().longitude;

                NavigationHelper.startBuildingActivityFromSheet(
                        this, description, imageFileNames, lat, lng, videoId);
            } else {
                // Fallback to directions if no sheet data found
                LatLng destination = marker.getPosition();
                Uri uri = Uri.parse("google.navigation:q=" +
                        destination.latitude + "," +
                        destination.longitude + "&mode=w");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });

        // Load buildings dynamically from Google Sheet
        new Thread(() -> {
            List<SheetFetcher.SheetRow> rows = SheetFetcher.fetch(
                    SheetFetcher.BUILDINGS_SHEET_ID, "Sheet1", true);

            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                for (SheetFetcher.SheetRow row : rows) {
                    try {
                        String name = row.get(0);
                        String type = row.get(1);
                        double lat  = Double.parseDouble(row.get(2));
                        double lng  = Double.parseDouble(row.get(3));

                        if (name.isEmpty()) continue;

                        Marker marker = map.addMarker(new MarkerOptions()
                                .position(new LatLng(lat, lng))
                                .title(name)
                                .snippet("Tap again for building details")
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                        getMarkerColor(type))));

                        if (marker != null) {
                            markerDataMap.put(name, row);
                        }

                    } catch (Exception e) {
                        Log.e("MapActivity", "Error adding marker: " + e.getMessage());
                    }
                }
            });
        }).start();
    }

    // Returns marker color based on building type
    private float getMarkerColor(String type) {
        switch (type.toLowerCase().trim()) {
            case "special":        return BitmapDescriptorFactory.HUE_GREEN;
            case "administrative": return BitmapDescriptorFactory.HUE_BLUE;
            case "academic":       return BitmapDescriptorFactory.HUE_YELLOW;
            case "athletics":      return BitmapDescriptorFactory.HUE_MAGENTA;
            case "studentlife":    return BitmapDescriptorFactory.HUE_ORANGE;
            default:               return BitmapDescriptorFactory.HUE_RED;
        }
    }

    @Override protected void onResume()  { super.onResume();  mapView.onResume();  }
    @Override protected void onStart()   { super.onStart();   mapView.onStart();   }
    @Override protected void onStop()    { super.onStop();    mapView.onStop();    }
    @Override protected void onPause()   { super.onPause();   mapView.onPause();   }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}







