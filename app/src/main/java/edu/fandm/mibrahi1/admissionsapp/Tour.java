package edu.fandm.mibrahi1.admissionsapp;

import java.util.List;

public class Tour {
    private final String name;
    private final List<String> stops;

    public Tour(String name, List<String> stops) {
        this.name = name;
        this.stops = stops;
    }

    public String getName() {
        return name;
    }

    public List<String> getStops() {
        return stops;
    }
}
