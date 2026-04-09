package edu.fandm.mibrahi1.admissionsapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class AdmissionCounselor extends AppCompatActivity
        implements TerritoryRepository.OnDataLoadedListener {

    private AutoCompleteTextView actvLocation;
    private RadioGroup rgStudentType;
    private Button btnSubmit;
    private ArrayAdapter<String> countryAdapter;
    private ArrayAdapter<String> stateAdapter;

    private TerritoryRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admission_counselor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admissionCounselorLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rgStudentType = findViewById(R.id.rgStudentType);
        actvLocation  = findViewById(R.id.actvLocation);
        btnSubmit     = findViewById(R.id.btnSubmit);

        countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        stateAdapter   = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());

        actvLocation.setAdapter(countryAdapter);
        actvLocation.setHint("Enter country");
        rgStudentType.check(R.id.rbInternational);

        repository = new TerritoryRepository(this, this);
        repository.fetchAll();

        rgStudentType.setOnCheckedChangeListener((group, checkedId) -> {
            actvLocation.setText("");
            if (checkedId == R.id.rbInternational) {
                actvLocation.setHint("Enter country");
                actvLocation.setAdapter(countryAdapter);
            } else if (checkedId == R.id.rbFromUS) {
                actvLocation.setHint("Enter state");
                actvLocation.setAdapter(stateAdapter);
            }
        });

        btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        String input = actvLocation.getText().toString().trim();

        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter a country or state.", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (input) {
            case "New York":
                NavigationHelper.startIntermediaryActivity(this, CountyIntermediateActivity.class, true, "New York");
                return;
            case "Pennsylvania":
                NavigationHelper.startIntermediaryActivity(this, CountyIntermediateActivity.class, true, "Pennsylvania");
                return;
            case "New Jersey":
                NavigationHelper.startIntermediaryActivity(this, CountyIntermediateActivity.class, false, "New Jersey");
                return;
            case "Virginia":
                NavigationHelper.startIntermediaryActivity(this, CountyIntermediateActivity.class, false, "Virginia");
                return;
        }
        TerritoryResult result = repository.lookupTerritory(input);
        navigateWithResult(result, input);
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
            NavigationHelper.startEmailDisplayActivity(this, EmailDisplayActivity.class,
                    result.counselorName, result.counselorInfo.email);
            return;
        }
        NavigationHelper.startActivityWithURL(this, WebPage.class, profileLink);
    }

    @Override
    public void onTabLoaded(String tabName, List<String> autocompleteItems) {
        if (tabName.equals("International by Country")) {
            countryAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, autocompleteItems);
            if (rgStudentType.getCheckedRadioButtonId() == R.id.rbInternational) {
                actvLocation.setAdapter(countryAdapter);
            }
        } else if (tabName.equals("Territory By State")) {
            stateAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, autocompleteItems);
            if (rgStudentType.getCheckedRadioButtonId() == R.id.rbFromUS) {
                actvLocation.setAdapter(stateAdapter);
            }
        }
    }
    @Override
    public void onAllTabsLoaded() {
        // Could hide a loading spinner here if you add one
    }
}