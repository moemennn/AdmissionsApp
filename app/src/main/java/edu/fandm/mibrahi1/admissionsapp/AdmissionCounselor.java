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
import java.util.Set;

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
        //Initializing new array adaptors
        countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        stateAdapter   = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());

        //default to international option
        actvLocation.setAdapter(countryAdapter);
        actvLocation.setHint("Enter country");
        rgStudentType.check(R.id.rbInternational);

        actvLocation.setDropDownAnchor(R.id.tilLocation);
        actvLocation.post(()->{
            actvLocation.setDropDownVerticalOffset(-actvLocation.getHeight());
        });
        //fetching data from memory or google sheet
        repository = new TerritoryRepository(this, this);
        repository.fetchAll();
        //toggling between international and US student
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
        //Button logic
        btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        //gets the user input
        String input = actvLocation.getText().toString().trim();

        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter a country or state.", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<String> specialStates = Set.of("New York", "Pennsylvania", "New Jersey", "Virginia");
        boolean isSpecialState = specialStates.contains(input);
        /*New York and Pennsylvania have special cases where some schools have different admission counselors.
        //If the input is one of these cases, display the part that asks for the school.
        Otherwise, make that section invisible*/
        if (isSpecialState) {
            boolean showSchools = input.equals("New York") || input.equals("Pennsylvania");
            NavigationHelper.startIntermediaryActivity(this, CountyIntermediateActivity.class,
                    showSchools, input);
        } else {
            // Directly look up and navigate for other countries/states
            TerritoryResult result = repository.lookupTerritory(input);
            if (result == null){
                Toast.makeText(this, "No match for \"" + input + "\".", Toast.LENGTH_SHORT).show();
                return;
            }
            NavigationHelper.navigateWithResult(this, result, input);
        }
    }

    @Override
    //Updates the UI when the corresponding data loads
    public void onTabLoaded(String tabName, List<String> autocompleteItems) {
        // Create an adapter for whichever tab is loaded
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, autocompleteItems);

        if (tabName.equals("International by Country")) {
            //saves the ArrayAdaptor for reference
            countryAdapter = adapter;
            if (rgStudentType.getCheckedRadioButtonId() == R.id.rbInternational) {
                actvLocation.setAdapter(adapter);
            }
        } else if (tabName.equals("Territory By State")) {
            //saves the ArrayAdaptor for reference
            stateAdapter = adapter;
            //We need this inside the if-else blocks to ensure the options don't display the wrong information.
            //It wouldn't look good if the international option displays the US states
            if (rgStudentType.getCheckedRadioButtonId() == R.id.rbFromUS) {
                actvLocation.setAdapter(adapter);
            }
        }
    }
    @Override
    public void onAllTabsLoaded() {
        // Logic for when all tabs are loaded. Some apps need to run this logic. This one doesn't, but
        //I'll include it here for future use if someone wants to add a loading bar in the activity.
        //When done, make the loading bar invisible
    }
}