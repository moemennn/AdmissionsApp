package edu.fandm.mibrahi1.admissionsapp;

//Building class, represents the information of a single building


/**
 * FILE SUMMARY:
 * This is a "Data Model" (POJO). It defines the exact shape of a
 * building object as it exists in our application logic.
 * KEY CONCEPTS:
 * - Encapsulation: Variables are private, access is via public getters.
 * - Immutability: Variables are final, set only once in the constructor.
 */

public class Building {

    private final String name;
    private final String description;
    private final String imageFileNames;
    private final String type;
    private final String videoLink;
    private final double latitude;
    private final double longitude;

    // CONSTRUCTOR: BuildingRepository uses this to build the object from Firebase data
    public Building(String name,
                    String description,
                    String imageFileNames,
                    String type,
                    String videoLink,
                    double latitude,
                    double longitude) {

        this.name = name;
        this.description = description;
        this.imageFileNames = imageFileNames;
        this.type = type;
        this.videoLink = videoLink;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // GETTERS: Used by Activities to read properties for display

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageFileNames() {
        return imageFileNames;
    }

    public String getType() {
        return type;
    }

    public String getVideoLink() {
        return videoLink;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}