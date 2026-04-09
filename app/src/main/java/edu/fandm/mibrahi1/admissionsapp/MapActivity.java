package edu.fandm.mibrahi1.admissionsapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import android.content.Intent;
import android.net.Uri;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

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

        // Lock camera to F&M campus area
        LatLngBounds fandmBounds = new LatLngBounds(
                new LatLng(40.0460, -76.3250), // Southwest corner
                new LatLng(40.0570, -76.3100)  // Northeast corner
        );
        map.setLatLngBoundsForCameraTarget(fandmBounds);
        map.setMinZoomPreference(14f);

        // Lombardo Welcome Center - GREEN (start here)
        map.addMarker(new MarkerOptions()
                .position(new LatLng(40.0513665, -76.3196468))
                .title("Lombardo Welcome Center")
                .snippet("Tap for walking directions")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Old Main - BLUE (administrative)
        map.addMarker(new MarkerOptions()
                .position(new LatLng(40.0453004, -76.3201826))
                .title("Old Main")
                .snippet("Tap for walking directions")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        // Shadek-Fackenthal Library - YELLOW (academic)
        map.addMarker(new MarkerOptions()
                .position(new LatLng(40.0446706, -76.3197777))
                .title("Shadek-Fackenthal Library")
                .snippet("Tap for walking directions")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

        // Steinman College Center - ORANGE (student life)
        map.addMarker(new MarkerOptions()
                .position(new LatLng(40.0473887, -76.3195619))
                .title("Steinman College Center")
                .snippet("Tap for walking directions")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        // Alumni Sports & Fitness Center - MAGENTA (athletics)
        map.addMarker(new MarkerOptions()
                .position(new LatLng(40.0521755, -76.3194044))
                .title("Alumni Sports & Fitness Center")
                .snippet("Tap for walking directions")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));

        // Tap marker → show info window
        map.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        // Tap info window → open Google Maps with walking directions
        map.setOnInfoWindowClickListener(marker -> {
            LatLng destination = marker.getPosition();
            Uri uri = Uri.parse("google.navigation:q=" +
                    destination.latitude + "," +
                    destination.longitude + "&mode=w");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        });

        // Center camera on Lombardo to start
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(40.0513665, -76.3196468), 16f));
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}







