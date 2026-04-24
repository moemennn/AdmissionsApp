package edu.fandm.mibrahi1.admissionsapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

// Activity for selecting a tour and navigating to its stops
public class TourSelectionActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteTours;
    private TextView tvTourDescription;
    private MaterialButton btnViewTour;

    private List<Tour> tours = new ArrayList<>();
    private Tour selectedTour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_selection);

        // Initialize UI components
        autoCompleteTours = findViewById(R.id.autoCompleteTours);
        tvTourDescription = findViewById(R.id.tvTourDescription);
        btnViewTour = findViewById(R.id.btnViewTour);

        // Load tour data from repository
        loadTours();

        // Handle "View Tour" button click
        btnViewTour.setOnClickListener(v -> {
            if (selectedTour == null) {
                Toast.makeText(this, "Please select a tour first.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Navigate to TourStopsActivity with selected tour name
            Intent intent = new Intent(TourSelectionActivity.this, TourStopsActivity.class);
            intent.putExtra("tour_name", selectedTour.getName());
            startActivity(intent);
        });
    }

    // Fetch tours and populate dropdown
    private void loadTours() {
        TourRepository.getTours(this, fetchedTours -> {

            tours.clear();
            tours.addAll(fetchedTours);

            // Handle empty state
            if (tours.isEmpty()) {
                tvTourDescription.setText("No tours available right now.");
                Toast.makeText(this, "Could not load tours.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Extract tour names for dropdown
            List<String> tourNames = new ArrayList<>();
            for (Tour tour : tours) {
                tourNames.add(tour.getName());
            }

            // Set up autocomplete dropdown
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    tourNames
            );

            autoCompleteTours.setAdapter(adapter);

            // Handle selection from dropdown
            autoCompleteTours.setOnItemClickListener((parent, view, position, id) -> {
                String selectedName = parent.getItemAtPosition(position).toString();

                selectedTour = findTourByName(selectedName);

                if (selectedTour != null) {
                    int stopCount = selectedTour.getStops().size();
                    String stopText = stopCount == 1 ? " stop" : " stops";

                    // Update UI with number of stops
                    tvTourDescription.setText(stopCount + stopText + " in this tour.");
                }
            });
        });
    }

    // Helper to find a tour by name
    private Tour findTourByName(String name) {
        for (Tour tour : tours) {
            if (tour.getName().equalsIgnoreCase(name)) {
                return tour;
            }
        }
        return null;
    }
}