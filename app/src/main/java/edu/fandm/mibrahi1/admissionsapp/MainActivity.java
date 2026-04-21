package edu.fandm.mibrahi1.admissionsapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

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

        MaterialButton startTour = findViewById(R.id.btnStartTour);
        startTour.setOnClickListener(v ->
                NavigationHelper.startActivity(this, MapActivity.class)
        );

        MaterialCardView startTourCard = findViewById(R.id.cardStartTour);
        startTourCard.setOnClickListener(v ->
                NavigationHelper.startActivity(this, TourSelectionActivity.class)
        );

        MaterialCardView bookVisit = findViewById(R.id.cardBookVisit);
        bookVisit.setOnClickListener(v ->
                NavigationHelper.startWebPageActivity(this, WebPage.class, "https://www.fandm.edu/visit/")
        );

        MaterialCardView meetCounselor = findViewById(R.id.cardCounselor);
        meetCounselor.setOnClickListener(v ->
                NavigationHelper.startActivity(this, AdmissionCounselor.class)
        );

        MaterialCardView contact = findViewById(R.id.cardContact);
        contact.setOnClickListener(v ->
                NavigationHelper.startActivity(this, ContactInfo.class)
        );
    }
}