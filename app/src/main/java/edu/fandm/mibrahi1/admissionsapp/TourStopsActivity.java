package edu.fandm.mibrahi1.admissionsapp;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class TourStopsActivity extends AppCompatActivity {

    private TextView tvTourTitle;
    private TextView tvTourSubtitle;
    private ListView lvTourStops;

    private Tour selectedTour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_stops);

        tvTourTitle = findViewById(R.id.tvTourTitle);
        tvTourSubtitle = findViewById(R.id.tvTourSubtitle);
        lvTourStops = findViewById(R.id.lvTourStops);

        String tourName = getIntent().getStringExtra("tour_name");

        if (tourName == null || tourName.trim().isEmpty()) {
            Toast.makeText(this, "No tour selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadTour(tourName);
    }

    private void loadTour(String tourName) {
        TourRepository.getTourByName(this, tourName, tour -> {
            selectedTour = tour;

            if (selectedTour == null) {
                Toast.makeText(this, "Could not load this tour.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            tvTourTitle.setText(selectedTour.getName());

            int stopCount = selectedTour.getStops().size();
            String stopText = stopCount == 1 ? "1 stop" : stopCount + " stops";
            tvTourSubtitle.setText(stopText);

            List<String> stops = new ArrayList<>(selectedTour.getStops());

            TourStopAdapter adapter = new TourStopAdapter(this, stops);
            lvTourStops.setAdapter(adapter);

            lvTourStops.setOnItemClickListener((parent, view, position, id) -> {
                String buildingName = stops.get(position);
                openBuildingFromRepository(buildingName);
            });
        });
    }


    private void openBuildingFromRepository(String buildingName) {
        BuildingRepository.getBuildingByName(this, buildingName, building -> {
            if (building == null) {
                Toast.makeText(
                        this,
                        "Could not find details for " + buildingName,
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            NavigationHelper.startBuildingActivity(
                    this,
                    building.getDescription(),
                    building.getImageFileNames(),
                    building.getLatitude(),
                    building.getLongitude(),
                    building.getVideoLink()
            );
        });
    }
}