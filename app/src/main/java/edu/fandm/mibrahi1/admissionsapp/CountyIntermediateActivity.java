package edu.fandm.mibrahi1.admissionsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

// Activity that allows users to search by county and optionally by school
// and then routes them to the correct counselor profile
public class CountyIntermediateActivity extends AppCompatActivity
        implements TerritoryRepository.OnDataLoadedListener {

    private AutoCompleteTextView actvCounty;
    private AutoCompleteTextView actvSchool;
    private View schoolSection;
    private MaterialButton btnSubmit;

    private TerritoryRepository repository;
    private String state;
    private boolean showSchools;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_county_intermediate);

        // Adjust layout for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.countyLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Retrieve state and configuration from previous screen
        state = getIntent().getStringExtra("state");
        showSchools = getIntent().getBooleanExtra("boolean", false);

        // Initialize UI components
        actvCounty = findViewById(R.id.actvCounty);
        actvSchool = findViewById(R.id.actvSchool);
        schoolSection = findViewById(R.id.schoolSection);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Show or hide school input based on configuration
        schoolSection.setVisibility(showSchools ? View.VISIBLE : View.GONE);

        // Set empty adapters initially (populated later after data loads)
        actvCounty.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>()));

        actvSchool.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>()));

        // Load territory data from repository
        repository = new TerritoryRepository(this, this);
        repository.fetchAll();

        // Handle submit action
        btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    // Handles logic for determining which input to use (school vs county)
    private void handleSubmit() {
        String schoolInput = showSchools ? actvSchool.getText().toString().trim() : "";
        String countyInput = actvCounty.getText().toString().trim();

        // Require at least one input
        if (countyInput.isEmpty() && schoolInput.isEmpty()) {
            Toast.makeText(this, "Please enter a county or school.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Priority: school lookup first if provided
        if (!schoolInput.isEmpty()) {
            TerritoryResult schoolResult = repository.lookupTerritory(schoolInput);

            if (schoolResult != null) {
                NavigationHelper.navigateWithResult(this, schoolResult, schoolInput);
                return;
            }
        }

        // Ensure county is provided if school path fails or not used
        if (countyInput.isEmpty()) {
            Toast.makeText(this, "Please enter a county.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fallback: county lookup
        TerritoryResult countyResult = repository.lookupTerritory(countyInput);
        NavigationHelper.navigateWithResult(this, countyResult, countyInput);
    }

    @Override
    public void onTabLoaded(String tabName, List<String> autocompleteItems) {
        // Not used directly — data is applied after full load completes
    }

    @Override
    public void onAllTabsLoaded() {

        // Populate county dropdown once all data is fully loaded
        repository.getCountiesForState(state, counties -> {
            actvCounty.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    counties
            ));
        });

        // Populate school dropdown only if this state supports schools
        if (showSchools) {
            repository.getSchoolsForState(state, schools -> {
                actvSchool.setAdapter(new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        schools
                ));
            });
        }
    }
}