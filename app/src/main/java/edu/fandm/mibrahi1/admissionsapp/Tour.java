package edu.fandm.mibrahi1.admissionsapp;

import java.util.List;

// Simple data model representing a tour
public class Tour {

    // Tour name (e.g., "Campus Tour")
    private final String name;

    // List of stops (e.g., building names)
    private final List<String> stops;

    // Constructor to initialize fields
    public Tour(String name, List<String> stops) {
        this.name = name;
        this.stops = stops;
    }

    // Returns the tour name
    public String getName() {
        return name;
    }

    // Returns the list of stops
    public List<String> getStops() {
        return stops;
    }
}