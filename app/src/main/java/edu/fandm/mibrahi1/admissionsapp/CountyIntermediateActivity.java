package edu.fandm.mibrahi1.admissionsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class CountyIntermediateActivity extends AppCompatActivity implements TerritoryRepository.OnDataLoadedListener {

    private AutoCompleteTextView actvCounty;
    private AutoCompleteTextView actvSchool;
    private ConstraintLayout schoolExceptionsLayout;
    private Button btnSubmit;
    private TerritoryRepository repository;
    private String state;
    private boolean showSchools;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_county_intermediate);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.countyLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        state = getIntent().getStringExtra("state");
        showSchools = getIntent().getBooleanExtra("boolean", false);

        actvCounty = findViewById(R.id.actvCounty);
        actvSchool = findViewById(R.id.actvSchool);
        schoolExceptionsLayout = findViewById(R.id.schoolExceptionsLayout);
        btnSubmit = findViewById(R.id.btnSubmit);

        schoolExceptionsLayout.setVisibility(showSchools ? View.VISIBLE : View.GONE);

        actvCounty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>()));
        actvSchool.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>()));

        repository = new TerritoryRepository(this, this);
        repository.fetchAll();

        btnSubmit.setOnClickListener(v -> handleSubmit());
    }
    private void handleSubmit() {
        String schoolInput = showSchools ? actvSchool.getText().toString().trim() : "";
        String countyInput = actvCounty.getText().toString().trim();

        if (countyInput.isEmpty() && schoolInput.isEmpty()) {
            Toast.makeText(this, "Please enter a county or school.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!schoolInput.isEmpty()) {
            TerritoryResult schoolResult = repository.lookupTerritory(schoolInput);
            if (schoolResult != null) {
                navigateWithResult(schoolResult, schoolInput);
                return;
            }
//            Toast.makeText(this, "No match for school \"" + schoolInput + "\", trying county...", Toast.LENGTH_SHORT).show();
        }

        if (countyInput.isEmpty()) {
            Toast.makeText(this, "Please enter a county.", Toast.LENGTH_SHORT).show();
            return;
        }

        TerritoryResult countyResult = repository.lookupTerritory(countyInput);
        navigateWithResult(countyResult, countyInput);
    }
    private void navigateWithResult(TerritoryResult result, String input) {
        if (result == null) {
            Toast.makeText(this, "No counselor found for \"" + input + "\".", Toast.LENGTH_SHORT).show();
            return;
        }

        if (result.counselorInfo == null) {
            Toast.makeText(this, "No contact information found for " + result.counselorName + ".", Toast.LENGTH_SHORT).show();
            return;
        }

        String profileLink = result.counselorInfo.profileLink;
        if (profileLink == null || profileLink.isEmpty() || profileLink.equalsIgnoreCase("N/A")) {
            NavigationHelper.startEmailDisplayActivity(this, EmailDisplayActivity.class, result.counselorName, result.counselorInfo.email);
            return;
        }

        NavigationHelper.startActivityWithURL(this, WebPage.class, profileLink);
    }
    @Override
    public void onTabLoaded(String tabName, List<String> autocompleteItems) {
        // Nothing needed here — adapters are set in onAllTabsLoaded to guarantee
        // all maps are fully populated first
    }
    @Override
    public void onAllTabsLoaded() {
        repository.getCountiesForState(state, counties -> {
            actvCounty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, counties));
        });

        if (showSchools) {
            repository.getSchoolsForState(state, schools -> {
                actvSchool.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, schools));
            });
        }
    }
}