package edu.fandm.mibrahi1.admissionsapp;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * FILE SUMMARY:
 * This activity takes a specific Tour (passed via Intent) and displays
 * every stop in that tour using a custom list adapter.
 */
public class TourStopsActivity extends AppCompatActivity {

    private TextView tvTourTitle;
    private TextView tvTourSubtitle;
    private ListView lvTourStops;

    private Tour selectedTour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_stops);

        // --- 1. SETUP UI ---
        tvTourTitle = findViewById(R.id.tvTourTitle);
        tvTourSubtitle = findViewById(R.id.tvTourSubtitle);
        lvTourStops = findViewById(R.id.lvTourStops);

        // --- 2. RETRIEVE DATA ---
        // We get the name of the tour the user clicked on in the previous screen
        String tourName = getIntent().getStringExtra("tour_name");

        // Safety Check: If someone manually started this activity without a name, go back
        if (tourName == null || tourName.trim().isEmpty()) {
            Toast.makeText(this, "No tour selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadTour(tourName);
    }

    /**
     * METHOD: loadTour
     * This uses the TourRepository to find the specific stops for the tour.
     */
    private void loadTour(String tourName) {
        // Asynchronous call: The UI doesn't freeze while we look up the tour
        TourRepository.getTourByName(this, tourName, tour -> {
            selectedTour = tour;

            if (selectedTour == null) {
                Toast.makeText(this, "Could not load this tour.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Update Header info
            tvTourTitle.setText(selectedTour.getName());
            int stopCount = selectedTour.getStops().size();
            String stopText = stopCount == 1 ? "1 stop" : stopCount + " stops";
            tvTourSubtitle.setText(stopText);

            // --- 3. LIST ADAPTER ---
            List<String> stops = new ArrayList<>(selectedTour.getStops());

            // PROFESSOR QUESTION: "What is the TourStopAdapter?"
            // ANSWER: It is a custom bridge that turns a List of Strings into a specific XML layout
            // for each row in our ListView.
            TourStopAdapter adapter = new TourStopAdapter(this, stops);
            lvTourStops.setAdapter(adapter);

            // Handle clicking a specific stop in the list
            lvTourStops.setOnItemClickListener((parent, view, position, id) -> {
                String buildingName = stops.get(position);
                openBuildingFromRepository(buildingName);
            });
        });
    }

    /**
     * METHOD: openBuildingFromRepository
     * Cross-references a tour stop name with the Building database.
     */
    private void openBuildingFromRepository(String buildingName) {
        BuildingRepository.getBuildingByName(this, buildingName, building -> {
            if (building == null) {
                Toast.makeText(this, "Details not found for " + buildingName, Toast.LENGTH_SHORT).show();
                return;
            }

            // PROFESSOR QUESTION: "How do you show the building details?"
            // ANSWER: We use NavigationHelper to bundle the building data and send it to ScrollableBuildingActivity.
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