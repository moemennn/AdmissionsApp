package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * NavigationHelper is a utility class that centralizes all navigation logic
 * in the app. It contains static methods for starting activities and
 * handling transitions between screens, including passing necessary data.
 */
public class NavigationHelper {

    /**
     * Starts a new activity without any additional information.
     */
    public static void startActivity(Context context, Class<?> activityClass) {
        Intent intent = new Intent(context, activityClass);
        context.startActivity(intent);
    }

    /**
     * Starts the CountyIntermediateActivity passing info needed to configure its UI.
     */
    public static void startIntermediaryActivity(Context context, Class<?> activityClass,
                                                 boolean bool, String state) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra("boolean", bool);
        intent.putExtra("state", state);
        context.startActivity(intent);
    }

    /**
     * Starts the EmailDisplayActivity to show the counselor's name and email.
     */
    public static void startEmailDisplayActivity(Context context, Class<?> activityClass,
                                                 String counselorName, String email) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra("counselorName", counselorName);
        intent.putExtra("email", email);
        context.startActivity(intent);
    }

    /**
     * Starts an activity with a URL for opening a web page within the WebPage activity.
     */
    public static void startWebPageActivity(Context context, Class<?> activityClass, String url) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }

    /**
     * Starts the ScrollableBuildingActivity passing building images and description.
     */
    public static void startScrollableBuildingActivity(Context context, int[] imageIds,
                                                       String buildingDescription) {
        Intent intent = new Intent(context, ScrollableBuildingActivity.class);
        intent.putExtra("imageIds", imageIds);
        intent.putExtra("buildingDescription", buildingDescription);
        context.startActivity(intent);
    }

    /**
     * Starts the ScrollableBuildingActivity using data from the Google Sheet.
     * Converts image filename strings into drawable resource IDs.
     * Also passes lat/lng for directions and videoId for future video support.
     */
    public static void startBuildingActivityFromSheet(Context context, String description,
                                                      String imageFileNames, double lat,
                                                      double lng, String videoId) {
        String[] fileNames = imageFileNames.split(",");
        List<Integer> imageIdList = new ArrayList<>();

        for (String fileName : fileNames) {
            String cleaned = fileName.trim()
                    .replace(".jpg", "")
                    .replace(".JPG", "")
                    .replace(".jpeg", "")
                    .toLowerCase();

            int resId = context.getResources().getIdentifier(
                    cleaned, "drawable", context.getPackageName());

            if (resId != 0) {
                imageIdList.add(resId);
            }
        }

        int[] imageIds = new int[imageIdList.size()];
        for (int i = 0; i < imageIdList.size(); i++) {
            imageIds[i] = imageIdList.get(i);
        }

        Intent intent = new Intent(context, ScrollableBuildingActivity.class);
        intent.putExtra("imageIds", imageIds);
        intent.putExtra("buildingDescription", description);
        intent.putExtra("videoId", videoId);
        intent.putExtra("lat", lat);
        intent.putExtra("lng", lng);
        context.startActivity(intent);
    }

    /**
     * Handles navigation after a territory lookup result is returned.
     */
    public static void navigateWithResult(AppCompatActivity activity, TerritoryResult result,
                                          String input) {
        if (result == null) {
            Toast.makeText(activity, "No counselor found for \"" + input + "\".",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        CounselorInfo info = result.counselorInfo;

        if (info == null) {
            Toast.makeText(activity, "No contact information found for "
                    + result.counselorName + ".", Toast.LENGTH_SHORT).show();
            return;
        }

        String profileLink = info.profileLink;
        if (profileLink == null || profileLink.isEmpty() || profileLink.equalsIgnoreCase("N/A")) {
            NavigationHelper.startEmailDisplayActivity(activity, EmailDisplayActivity.class,
                    result.counselorName, info.email);
            return;
        }

        NavigationHelper.startWebPageActivity(activity, WebPage.class, profileLink);
    }
}