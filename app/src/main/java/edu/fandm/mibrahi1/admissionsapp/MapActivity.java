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

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    private MapView mapView;
    private final HashMap<String, Building> markerDataMap = new HashMap<>();

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
        map.getUiSettings().setScrollGesturesEnabled(true);
        map.getUiSettings().setZoomGesturesEnabled(true);
        map.getUiSettings().setTiltGesturesEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(true);

        LatLngBounds fandmBounds = new LatLngBounds(
                new LatLng(40.0460, -76.3250),
                new LatLng(40.0570, -76.3100)
        );

        map.setLatLngBoundsForCameraTarget(fandmBounds);
        map.setMinZoomPreference(14f);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(40.0513665, -76.3196468), 16f
        ));

        map.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        map.setOnInfoWindowClickListener(marker -> {
            String buildingName = marker.getTitle();
            Building building = markerDataMap.get(buildingName);

            if (building != null) {
                NavigationHelper.startBuildingActivityFromSheet(
                        this,
                        building.getDescription(),
                        building.getImageFileNames(),
                        building.getLatitude(),
                        building.getLongitude(),
                        building.getVideoLink()
                );
            } else {
                LatLng destination = marker.getPosition();
                Uri uri = Uri.parse("google.navigation:q="
                        + destination.latitude + ","
                        + destination.longitude
                        + "&mode=w");

                Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });

        loadBuildingMarkers(map);
    }

    private void loadBuildingMarkers(GoogleMap map) {
        BuildingRepository.getAllBuildings(this, buildings -> {
            for (Building building : buildings) {
                try {
                    if (building.getName().isEmpty()) {
                        continue;
                    }

                    double lat = building.getLatitude();
                    double lng = building.getLongitude();

                    if (lat == 0.0 || lng == 0.0) {
                        continue;
                    }

                    Marker marker = map.addMarker(new MarkerOptions()
                            .position(new LatLng(lat, lng))
                            .title(building.getName())
                            .snippet("Tap again for building details")
                            .icon(BitmapDescriptorFactory.defaultMarker(
                                    getMarkerColor(building.getType())
                            )));

                    if (marker != null) {
                        markerDataMap.put(building.getName(), building);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error adding marker for building: " + building.getName(), e);
                }
            }
        });
    }

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