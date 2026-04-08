package edu.fandm.mibrahi1.admissionsapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;

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

        countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        stateAdapter   = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());

        actvLocation.setAdapter(countryAdapter);
        actvLocation.setHint("Enter country");
        rgStudentType.check(R.id.rbInternational);

        repository = new TerritoryRepository(this);
        repository.fetchAll();

        rgStudentType.setOnCheckedChangeListener((group, checkedId) -> {
            actvLocation.setText("");
            if (checkedId == R.id.rbInternational) {
                actvLocation.setHint("Enter country");
                actvLocation.setAdapter(countryAdapter);
            } else if (checkedId == R.id.rbFromUS) {
                actvLocation.setHint("Enter state or county");
                actvLocation.setAdapter(stateAdapter);
            }
        });

        actvLocation.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            TerritoryResult result = repository.lookupTerritory(selected);
            if (result != null) {
                Log.d("AdmissionCounselor", "Counselor: " + result.counselorName);
                if (result.counselorInfo != null) {
                    Log.d("AdmissionCounselor", "Email: "   + result.counselorInfo.email);
                    Log.d("AdmissionCounselor", "Profile: " + result.counselorInfo.profileLink);
                }
                // TODO: display counselor info in your UI here
            }
        });
    }

    // Called each time a single tab finishes loading
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

    // Called once every tab has finished loading
    @Override
    public void onAllTabsLoaded() {
        Log.d("AdmissionCounselor", "All data ready.");
        // e.g. hide a loading spinner here
    }
}