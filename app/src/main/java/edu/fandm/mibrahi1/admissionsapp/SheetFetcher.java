package edu.fandm.mibrahi1.admissionsapp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * SheetFetcher is a utility class for downloading data from a Google Sheet
 * and converting it into a structured format that your app can use.
 *
 * It fetches CSV data from a Google Sheet tab and parses each row into a SheetRow object.
 * It supports multiple sheets via the generalized fetch(sheetId, tabName, skipHeader) method.
 */
public class SheetFetcher {

    // Sheet IDs — add new ones here as needed
    public static final String COUNSELOR_SHEET_ID = "1gKC6h1FUn88JZbnggXAnBqJtY-dacVYxAzbzdoYLiuU";
    public static final String BUILDINGS_SHEET_ID = "1bQsD0Tg53nysaTcxgg76cXx8dQcaZB3a0K0xt09jaBE";

    // Kept for backward compatibility with TerritoryRepository
    static final String SHEET_ID = COUNSELOR_SHEET_ID;

    /**
     * Represents a single row of data in the sheet.
     * Each row is stored as an array of columns (strings).
     */
    public static class SheetRow {
        private final String[] columns;

        SheetRow(String[] columns) {
            this.columns = columns;
        }

        /**
         * Returns the value of a column by index.
         * If the index is out of bounds, returns an empty string.
         */
        public String get(int columnIndex) {
            if (columnIndex < 0 || columnIndex >= columns.length)
                return "";
            return columns[columnIndex];
        }

        /**
         * Returns the number of columns in this row
         */
        public int size() {
            return columns.length;
        }
    }

    /**
     * Original method — uses the counselor sheet ID by default.
     * TerritoryRepository calls this so it stays unchanged.
     */
    public static List<SheetRow> fetch(String tabName, boolean skipHeader) {
        return fetch(COUNSELOR_SHEET_ID, tabName, skipHeader);
    }

    /**
     * Generalized method — works with ANY sheet ID.
     * Use this for buildings, contacts, or any future sheet.
     */
    public static List<SheetRow> fetch(String sheetId, String tabName, boolean skipHeader) {
        List<SheetRow> results = new ArrayList<>();

        try {
            String encodedTab = tabName.replace(" ", "%20");

            String urlStr = "https://docs.google.com/spreadsheets/d/" + sheetId
                    + "/gviz/tq?tqx=out:csv&sheet=" + encodedTab;

            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine && skipHeader) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;
                results.add(new SheetRow(parseCSVLine(line)));
            }

            reader.close();

        } catch (Exception e) {
            Log.e("SheetFetcher", "Failed to fetch sheet: " + tabName, e);
        }

        return results;
    }

    /**
     * Parses a single CSV line into an array of strings, handling quoted commas.
     */
    static String[] parseCSVLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        tokens.add(current.toString().trim());
        return tokens.toArray(new String[0]);
    }
}