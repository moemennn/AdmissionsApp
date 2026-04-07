package edu.fandm.mibrahi1.admissionsapp;

import android.os.AsyncTask;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdmissionCounselor extends AppCompatActivity {

    private AutoCompleteTextView actvLocation;
    private RadioGroup rgStudentType;

    private ArrayAdapter<String> countryAdapter;
    private ArrayAdapter<String> stateAdapter;

    // Maps country name -> counselor name, populated from the sheet
    private Map<String, String> countryCounselorMap = new HashMap<>();

    private String[] states = {"Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado"};

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
        actvLocation = findViewById(R.id.actvLocation);

        // Initialize state adapter (static list)
        stateAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, states);

        // Start with an empty country adapter until the sheet loads
        countryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<String>());

        // Default to international
        actvLocation.setAdapter(countryAdapter);
        actvLocation.setHint("Enter country");
        rgStudentType.check(R.id.rbInternational);

        // Fetch country + counselor data from the sheet
        new FetchInternationalDataTask().execute();

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
    }

    private class FetchInternationalDataTask extends AsyncTask<Void, Void, List<SheetFetcher.SheetRow>> {

        @Override
        protected List<SheetFetcher.SheetRow> doInBackground(Void... voids) {
            return SheetFetcher.fetch("International by Country", new int[]{0, 3}, true);
        }

        @Override
        protected void onPostExecute(List<SheetFetcher.SheetRow> rows) {
            List<String> countryNames = new ArrayList<>();

            for (SheetFetcher.SheetRow row : rows) {
                String country = row.get(0);   // Column A
                String counselor = row.get(3); // Column D

                if (!country.isEmpty()) {
                    countryNames.add(country);
                    countryCounselorMap.put(country, counselor);
                }
            }

            countryAdapter = new ArrayAdapter<>(AdmissionCounselor.this,
                    android.R.layout.simple_dropdown_item_1line, countryNames);

            // Update adapter if International is still selected
            if (rgStudentType.getCheckedRadioButtonId() == R.id.rbInternational) {
                actvLocation.setAdapter(countryAdapter);
            }

            Log.d("AdmissionCounselor", "Loaded " + countryNames.size() + " countries from sheet");
        }
    }

    /**
     * Returns the counselor assigned to the given country, or null if not found.
     * Call this wherever you need to display the counselor after a country is selected.
     */
    public String getCounselorForCountry(String country) {
        return countryCounselorMap.get(country);
    }
}