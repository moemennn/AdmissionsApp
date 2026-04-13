package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * NavigationHelper is a utility class that centralizes all navigation logic
 * in the app. It contains static methods for starting activities and
 * handling transitions between screens, including passing necessary data.
 */
public class NavigationHelper {

    /**
     * Starts a new activity without any additional information.
     * Used when the destination activity does not depend on previous data.
     *
     * @param context the current context (usually an Activity)
     * @param activityClass the class of the activity to start
     */
    public static void startActivity(Context context, Class<?> activityClass) {
        // Create a new intent to start the specified activity
        Intent intent = new Intent(context, activityClass);
        context.startActivity(intent);
    }

    /**
     * Starts the CountyIntermediateActivity, passing information needed
     * for that activity to configure its UI.
     *
     * @param context the current context
     * @param activityClass the activity class to start (usually CountyIntermediateActivity)
     * @param bool determines whether a special UI element should be shown
     * @param state the state selected by the user
     */
    public static void startIntermediaryActivity(Context context, Class<?> activityClass, boolean bool, String state) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra("boolean", bool);
        intent.putExtra("state", state);
        context.startActivity(intent);
    }

    /**
     * Starts the EmailDisplayActivity to show the counselor's name and email.
     * This is used when a profile link is missing or not applicable.
     *
     * @param context the current context
     * @param activityClass the EmailDisplayActivity class
     * @param counselorName the name of the counselor
     * @param email the email address of the counselor
     */
    public static void startEmailDisplayActivity(Context context, Class<?> activityClass, String counselorName, String email) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra("counselorName", counselorName);
        intent.putExtra("email", email);
        context.startActivity(intent);
    }

    /**
     * Starts an activity with a URL, typically for opening a web page
     * within the WebPage activity.
     *
     * @param context the current context
     * @param activityClass the class of the activity to start (usually WebPage)
     * @param url the web URL to open
     */
    public static void startWebPageActivity(Context context, Class<?> activityClass, String url) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }

    /**
     * Starts the ScrollableBuildingActivity, passing building images,
     * description, and a YouTube video ID for display.
     *
     * @param context the current context
     * @param imageIds array of drawable resource IDs for images
     * @param buildingDescription text describing the building
     * @param videoId the YouTube video ID to embed
     */
    public static void startScrollableBuildingActivity(Context context, int[] imageIds, String buildingDescription, String videoId) {
        Intent intent = new Intent(context, ScrollableBuildingActivity.class);
        intent.putExtra("imageIds", imageIds);
        intent.putExtra("buildingDescription", buildingDescription);
        intent.putExtra("videoId", videoId);
        context.startActivity(intent);
    }

    /**
     * Handles the logic behind submit buttons in AdmissionCounselor
     * and CountyIntermediateActivity. Pulls up the counselor profile
     * or email display activity based on available data.
     *
     * @param activity the current AppCompatActivity
     * @param result the TerritoryResult containing counselor information
     * @param input the user input (school, county, or state)
     */
    public static void navigateWithResult(AppCompatActivity activity, TerritoryResult result, String input) {
        // If there’s no result at all, show a toast and return
        if (result == null) {
            Toast.makeText(activity, "No counselor found for \"" + input + "\".", Toast.LENGTH_SHORT).show();
            return;
        }

        CounselorInfo info = result.counselorInfo;

        // If the result exists but no counselor info is available, show a toast
        if (info == null) {
            Toast.makeText(activity, "No contact information found for " + result.counselorName + ".", Toast.LENGTH_SHORT).show();
            return;
        }

        String profileLink = info.profileLink;
        // If the profile link section is empty or "N/A", then start the EmailDisplayActivity
        // to display the counselor's name and email
        if (profileLink == null || profileLink.isEmpty() || profileLink.equalsIgnoreCase("N/A")) {
            NavigationHelper.startEmailDisplayActivity(activity, EmailDisplayActivity.class,
                    result.counselorName, info.email);
            return;
        }

        // Otherwise, pull up the profile page (from the website)
        NavigationHelper.startWebPageActivity(activity, WebPage.class, profileLink);
    }
}