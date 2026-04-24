package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Utility class that centralizes all navigation logic in one place.
 * This avoids repeating Intent creation logic across multiple activities.
 */
public class NavigationHelper {

    /**
     * Simple navigation to another activity with no extra data.
     */
    public static void startActivity(Context context, Class<?> activityClass) {
        Intent intent = new Intent(context, activityClass);
        context.startActivity(intent);
    }

    /**
     * Navigation that passes basic state information (boolean + string).
     * Used when the destination screen needs simple configuration data.
     */
    public static void startIntermediaryActivity(Context context, Class<?> activityClass,
                                                 boolean bool, String state) {

        Intent intent = new Intent(context, activityClass);

        // Pass lightweight state information to the next activity
        intent.putExtra("boolean", bool);
        intent.putExtra("state", state);

        context.startActivity(intent);
    }

    /**
     * Opens an email display screen with counselor contact details.
     * Used when a direct email action is required instead of a web page.
     */
    public static void startEmailDisplayActivity(Context context, Class<?> activityClass,
                                                 String counselorName, String email) {

        Intent intent = new Intent(context, activityClass);

        // Pass counselor identity and email for display
        intent.putExtra("counselorName", counselorName);
        intent.putExtra("email", email);

        context.startActivity(intent);
    }

    /**
     * Opens a WebView-based activity to display an external profile or URL.
     */
    public static void startWebPageActivity(Context context, Class<?> activityClass, String url) {

        Intent intent = new Intent(context, activityClass);

        // URL to be loaded in WebView
        intent.putExtra("url", url);

        context.startActivity(intent);
    }

    /**
     * Navigates to the building detail screen, passing all required building data.
     * Includes description, images, map coordinates, and optional video content.
     */
    public static void startBuildingActivity(Context context,
                                             String description,
                                             String imageFileNames,
                                             double lat,
                                             double lng,
                                             String videoId) {

        Intent intent = new Intent(context, ScrollableBuildingActivity.class);

        // Building text content
        intent.putExtra("imageFileNames", imageFileNames);
        intent.putExtra("buildingDescription", description);

        // Optional multimedia content (YouTube video ID)
        intent.putExtra("videoId", videoId);

        // Geographic coordinates for map navigation
        intent.putExtra("lat", lat);
        intent.putExtra("lng", lng);

        context.startActivity(intent);
    }

    /**
     * Handles navigation logic based on a territory lookup result.
     *
     * Flow:
     * 1. If no result → show error message
     * 2. If missing counselor info → show fallback message
     * 3. If profile link exists → open web page
     * 4. Otherwise → fallback to email display screen
     */
    public static void navigateWithResult(AppCompatActivity activity,
                                          TerritoryResult result,
                                          String input) {

        // Case 1: No matching counselor found
        if (result == null) {
            Toast.makeText(activity,
                    "No counselor found for \"" + input + "\".",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        CounselorInfo info = result.counselorInfo;

        // Case 2: Result exists but missing detailed contact info
        if (info == null) {
            Toast.makeText(activity,
                    "No contact information found for "
                            + result.counselorName + ".",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String profileLink = info.profileLink;

        // Case 3: No profile link available → fallback to email screen
        if (profileLink == null
                || profileLink.isEmpty()
                || profileLink.equalsIgnoreCase("N/A")) {

            NavigationHelper.startEmailDisplayActivity(
                    activity,
                    EmailDisplayActivity.class,
                    result.counselorName,
                    info.email
            );
            return;
        }

        // Case 4: Valid profile link exists → open web page
        NavigationHelper.startWebPageActivity(activity, WebPage.class, profileLink);
    }
}