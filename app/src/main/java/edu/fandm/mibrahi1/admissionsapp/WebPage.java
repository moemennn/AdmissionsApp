package edu.fandm.mibrahi1.admissionsapp;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Activity that displays a webpage inside the app using WebView
public class WebPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge UI layout
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_web_page);

        // Handle system bar insets (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.webPageLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Add padding so content doesn't overlap system bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            return insets;
        });

        // Get URL passed from previous activity
        String url = getIntent().getStringExtra("url");

        // Initialize WebView
        WebView webView = findViewById(R.id.my_web_view);

        // Keep navigation inside the app instead of opening browser
        webView.setWebViewClient(new WebViewClient());

        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();

        // Enable JavaScript for modern webpages
        webSettings.setJavaScriptEnabled(true);

        // Load the requested webpage
        webView.loadUrl(url);
    }
}