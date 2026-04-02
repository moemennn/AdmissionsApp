package edu.fandm.mibrahi1.admissionsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button startTour = (Button) findViewById(R.id.btnStartTour);
        startTour.setOnClickListener(v -> {
            NavigationHelper.startActivity(this, MapActivity.class);
        });

        Button bookVisit = (Button) findViewById(R.id.btnBookVisit);
        bookVisit.setOnClickListener(v -> {
            NavigationHelper.startActivityWithURL(this, WebPage.class, "https://www.fandm.edu/visit/");
        });

        Button meetCounselor = (Button) findViewById(R.id.btnMeetCounselor);
        meetCounselor.setOnClickListener(v -> {
            NavigationHelper.startActivity(this, AdmissionCounselor.class);
        });

        Button contact = (Button) findViewById(R.id.btnContactInfo);
        contact.setOnClickListener(v -> {
            NavigationHelper.startActivity(this, ContactInfo.class);
        });
    }
}