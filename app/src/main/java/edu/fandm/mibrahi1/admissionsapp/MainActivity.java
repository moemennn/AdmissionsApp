package edu.fandm.mibrahi1.admissionsapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * FILE SUMMARY:
 * This is the entry point of the application. It provides a visual dashboard
 * using Material Design Cards and Buttons to navigate to the app's features.
 *
 * DETAILED BREAKDOWN:
 * - EdgeToEdge: This makes the app's background "bleed" behind the status and navigation bars.
 * - ViewCompat.setOnApplyWindowInsetsListener: This logic ensures that our content
 *   doesn't get cut off by the phone's notch or system bars by adding internal padding.
 * - MaterialCardView: We used cards instead of standard buttons for a more modern,
 *   touch-friendly UI.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- 1. UI INITIALIZATION ---
        // EdgeToEdge enables a modern transparent system bar look
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // PROFESSOR GOTCHA: "What does this ViewCompat block do?"
        // ANSWER: It handles window insets. It calculates the height of system bars (like the status bar)
        // and applies that as padding to our layout so the UI is visible on all screen types.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- 2. NAVIGATION LOGIC ---

        // START TOUR BUTTON (Directly to the Map)
        MaterialButton startTour = findViewById(R.id.btnStartTour);
        startTour.setOnClickListener(v ->
                NavigationHelper.startActivity(this, MapActivity.class)
        );

        // TOUR SELECTION CARD (To choose specific tours)
        MaterialCardView startTourCard = findViewById(R.id.cardStartTour);
        startTourCard.setOnClickListener(v ->
                NavigationHelper.startActivity(this, TourSelectionActivity.class)
        );

        // BOOK A VISIT CARD (External Link)
        // PROFESSOR QUESTION: "How do you handle links outside the app?"
        // ANSWER: We use our NavigationHelper to start a WebPage activity, passing the target URL.
        MaterialCardView bookVisit = findViewById(R.id.cardBookVisit);
        bookVisit.setOnClickListener(v ->
                NavigationHelper.startWebPageActivity(this, WebPage.class, "https://www.fandm.edu/visit/")
        );

        // MEET COUNSELOR CARD
        MaterialCardView meetCounselor = findViewById(R.id.cardCounselor);
        meetCounselor.setOnClickListener(v ->
                NavigationHelper.startActivity(this, AdmissionCounselor.class)
        );

        // CONTACT INFO CARD
        MaterialCardView contact = findViewById(R.id.cardContact);
        contact.setOnClickListener(v ->
                NavigationHelper.startActivity(this, ContactInfo.class)
        );
    }
}