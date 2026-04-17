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

public class CountyIntermediateActivity extends AppCompatActivity implements TerritoryRepository.OnDataLoadedListener {

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
        schoolSection = findViewById(R.id.schoolSection);
        btnSubmit = findViewById(R.id.btnSubmit);

        schoolSection.setVisibility(showSchools ? View.VISIBLE : View.GONE);

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

        //Order of priority: School (if provided), then county
        if (!schoolInput.isEmpty()) {
            TerritoryResult schoolResult = repository.lookupTerritory(schoolInput);
            if (schoolResult != null) {
                //Pull up the profile page according to the school result
                NavigationHelper.navigateWithResult(this, schoolResult, schoolInput);
                return;
            }
//            Toast.makeText(this, "No match for school \"" + schoolInput + "\", trying county...", Toast.LENGTH_SHORT).show();
        }

        if (countyInput.isEmpty()) {
            Toast.makeText(this, "Please enter a county.", Toast.LENGTH_SHORT).show();
            return;
        }

        TerritoryResult countyResult = repository.lookupTerritory(countyInput);
        //If school result is empty, then pull up the profile page according to the county
        NavigationHelper.navigateWithResult(this, countyResult, countyInput);
    }

    @Override
    public void onTabLoaded(String tabName, List<String> autocompleteItems) {
        // Nothing needed here — adapters are set in onAllTabsLoaded to guarantee
        // all maps are fully populated first
    }

    @Override
    /**
     * As stated above in onTabLoaded, we only set the adaptors once all the information is loaded.
     * This is because by the time this CountyIntermediateActivity starts, all the information has already been loaded
     */
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