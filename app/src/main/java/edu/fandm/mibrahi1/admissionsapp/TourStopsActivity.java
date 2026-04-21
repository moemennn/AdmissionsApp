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
        new Thread(() -> {
            selectedTour = TourRepository.getTourByName(tourName);

            runOnUiThread(() -> {
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
                    openBuildingFromSheet(buildingName);
                });
            });
        }).start();
    }

    private void openBuildingFromSheet(String buildingName) {
        new Thread(() -> {
            List<SheetFetcher.SheetRow> rows = SheetFetcher.fetch(
                    SheetFetcher.BUILDINGS_SHEET_ID,
                    "Sheet1",
                    true
            );

            SheetFetcher.SheetRow matchedRow = null;

            for (SheetFetcher.SheetRow row : rows) {
                String name = row.get(0).trim();
                if (name.equalsIgnoreCase(buildingName.trim())) {
                    matchedRow = row;
                    break;
                }
            }

            SheetFetcher.SheetRow finalMatchedRow = matchedRow;

            runOnUiThread(() -> {
                if (finalMatchedRow == null) {
                    Toast.makeText(
                            this,
                            "Could not find details for " + buildingName,
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                try {
                    String description = finalMatchedRow.get(4);
                    String imageFileNames = finalMatchedRow.get(5);
                    String videoId = finalMatchedRow.get(6);

                    double lat = Double.parseDouble(finalMatchedRow.get(2));
                    double lng = Double.parseDouble(finalMatchedRow.get(3));

                    NavigationHelper.startBuildingActivityFromSheet(
                            this,
                            description,
                            imageFileNames,
                            lat,
                            lng,
                            videoId
                    );

                } catch (Exception e) {
                    Toast.makeText(
                            this,
                            "Error opening building details.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }).start();
    }
}