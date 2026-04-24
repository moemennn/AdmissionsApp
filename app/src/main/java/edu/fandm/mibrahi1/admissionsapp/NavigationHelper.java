package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class NavigationHelper {

    public static void startActivity(Context context, Class<?> activityClass) {
        Intent intent = new Intent(context, activityClass);
        context.startActivity(intent);
    }

    public static void startIntermediaryActivity(Context context, Class<?> activityClass,
                                                 boolean bool, String state) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra("boolean", bool);
        intent.putExtra("state", state);
        context.startActivity(intent);
    }

    public static void startEmailDisplayActivity(Context context, Class<?> activityClass,
                                                 String counselorName, String email) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra("counselorName", counselorName);
        intent.putExtra("email", email);
        context.startActivity(intent);
    }

    public static void startWebPageActivity(Context context, Class<?> activityClass, String url) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }

    public static void startScrollableBuildingActivity(Context context,
                                                       String imageFileNames,
                                                       String buildingDescription) {
        Intent intent = new Intent(context, ScrollableBuildingActivity.class);
        intent.putExtra("imageFileNames", imageFileNames);
        intent.putExtra("buildingDescription", buildingDescription);
        context.startActivity(intent);
    }

    public static void startBuildingActivityFromSheet(Context context,
                                                      String description,
                                                      String imageFileNames,
                                                      double lat,
                                                      double lng,
                                                      String videoId) {
        Intent intent = new Intent(context, ScrollableBuildingActivity.class);
        intent.putExtra("imageFileNames", imageFileNames);
        intent.putExtra("buildingDescription", description);
        intent.putExtra("videoId", videoId);
        intent.putExtra("lat", lat);
        intent.putExtra("lng", lng);
        context.startActivity(intent);
    }

    public static void navigateWithResult(AppCompatActivity activity,
                                          TerritoryResult result,
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
            NavigationHelper.startEmailDisplayActivity(
                    activity,
                    EmailDisplayActivity.class,
                    result.counselorName,
                    info.email
            );
            return;
        }

        NavigationHelper.startWebPageActivity(activity, WebPage.class, profileLink);
    }
}