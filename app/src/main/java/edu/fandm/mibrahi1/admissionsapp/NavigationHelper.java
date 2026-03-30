package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.content.Intent;

public class NavigationHelper {

    public static void startActivity(Context context, Class<?> activityClass) {
        Intent intent = new Intent(context, activityClass);
        context.startActivity(intent);
    }

    public static void startActivityWithURL(Context context, Class<?> activityClass, String url) {
        Intent intent = new Intent(context, activityClass);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }

    public static void startScrollableBuildingActivity(Context context, int[] imageIds, String buildingDescription, String videoId) {
        Intent intent = new Intent(context, ScrollableBuildingActivity.class);
        intent.putExtra("imageIds", imageIds);
        intent.putExtra("buildingDescription", buildingDescription);
        intent.putExtra("videoId", videoId);
        context.startActivity(intent);
    }
}