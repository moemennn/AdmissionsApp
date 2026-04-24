package edu.fandm.mibrahi1.admissionsapp;

public class Building {

    private final String name;
    private final String description;
    private final String imageFileNames;
    private final String type;
    private final String videoLink;
    private final double latitude;
    private final double longitude;

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