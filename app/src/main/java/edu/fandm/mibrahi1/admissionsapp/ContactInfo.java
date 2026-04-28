package edu.fandm.mibrahi1.admissionsapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * FILE SUMMARY:
 * This activity provides a simple contact interface for the Admissions office.
 * It uses Implicit Intents to delegate communication (email/phone) to external
 * apps already installed on the user's device.
 */
public class ContactInfo extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- 1. UI SETUP ---
        // Enables "Edge-to-Edge" display so the app draws behind the status bar/navigation bar
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contact_info);

        // This listener handles "Window Insets." It ensures that our UI components
        // are padded correctly so they don't overlap with the phone's notch or system bars.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.contact_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize button references from the XML layout
        Button btnEmail = findViewById(R.id.btnEmail);
        Button btnPhone = findViewById(R.id.btnPhone);

        // --- 2. EMAIL LOGIC ---
        // 📧 Email Admissions
        btnEmail.setOnClickListener(v -> {
            // ACTION_SENDTO is an Implicit Intent. We aren't opening a specific class
            // in our app; we are asking the Android system to find an app that handles mail.
            Intent intent = new Intent(Intent.ACTION_SENDTO);

            // The "mailto:" scheme tells the system specifically to look for email clients
            intent.setData(Uri.parse("mailto:admission@fandm.edu"));

            // Pre-filling the subject line for the user's convenience
            intent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry about F&M Admissions");

            startActivity(intent);
        });

        // --- 3. PHONE LOGIC ---
        // 📞 Call Admissions (default number)
        btnPhone.setOnClickListener(v -> {
            // ACTION_DIAL opens the phone's dialer with the number pre-filled.
            // NOTE: We use ACTION_DIAL instead of ACTION_CALL because DIAL
            // does not require the "CALL_PHONE" permission from the user.
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:7173583951"));

            startActivity(intent);
        });
    }
}