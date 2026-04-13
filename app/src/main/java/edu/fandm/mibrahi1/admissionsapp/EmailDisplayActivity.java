package edu.fandm.mibrahi1.admissionsapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EmailDisplayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_email_display);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.emailLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //Get the information that was passed in
        String counselorName = getIntent().getStringExtra("counselorName");
        String email = getIntent().getStringExtra("email");
        //Get the objects that need to update
        TextView tvCounselorName = findViewById(R.id.tvCounselorName);
        TextView tvCounselorEmail = findViewById(R.id.tvCounselorEmail);
        Button btnEmail = findViewById(R.id.btnEmail);
        //Update the corresponding texts
        tvCounselorName.setText(counselorName);
        tvCounselorEmail.setText(email);
        //link up the email button
        btnEmail.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + email));
            startActivity(emailIntent);
        });
    }
}