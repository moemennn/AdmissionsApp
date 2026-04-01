package edu.fandm.mibrahi1.admissionsapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdmissionCounselor extends AppCompatActivity {

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

        //Dynamic options
        RadioGroup rgStudentType = findViewById(R.id.rgStudentType);
        AutoCompleteTextView actvLocation = findViewById(R.id.actvLocation);

        // Example data - replace with your actual lists
        String[] countries = {"Afghanistan", "Albania", "Argentina", "Australia", "Brazil", "Canada"};
        String[] states = {"Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado"};

        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, countries);
        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, states);

        // Default to international
        actvLocation.setAdapter(countryAdapter);
        rgStudentType.check(R.id.rbInternational);

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
}