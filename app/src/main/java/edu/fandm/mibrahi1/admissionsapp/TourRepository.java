package edu.fandm.mibrahi1.admissionsapp;

import java.util.ArrayList;
import java.util.List;

public class TourRepository {

    // Replace "Sheet1" with your actual tab name if different
    private static final String TAB_NAME = "Sheet1";

    public static List<Tour> getTours() {
        List<SheetFetcher.SheetRow> rows =
                SheetFetcher.fetch(SheetFetcher.TOURS_SHEET_ID, TAB_NAME, false);

        List<Tour> tours = new ArrayList<>();

        if (rows.isEmpty()) {
            return tours;
        }

        // First row = headers = tour names
        SheetFetcher.SheetRow headerRow = rows.get(0);
        int columnCount = headerRow.size();

        for (int col = 0; col < columnCount; col++) {
            String tourName = headerRow.get(col).trim();

            if (tourName.isEmpty()) continue;

            List<String> stops = new ArrayList<>();

            for (int row = 1; row < rows.size(); row++) {
                String stop = rows.get(row).get(col).trim();
                if (!stop.isEmpty()) {
                    stops.add(stop);
                }
            }

            tours.add(new Tour(tourName, stops));
        }

        return tours;
    }

    public static Tour getTourByName(String name) {
        for (Tour tour : getTours()) {
            if (tour.getName().equalsIgnoreCase(name)) {
                return tour;
            }
        }
        return null;
    }
}