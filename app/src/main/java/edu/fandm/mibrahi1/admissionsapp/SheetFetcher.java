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
 */
public class SheetFetcher {

    // The unique ID of the Google Sheet (found in the sheet's URL)
    static final String SHEET_ID = "1gKC6h1FUn88JZbnggXAnBqJtY-dacVYxAzbzdoYLiuU";

    /**
     * Represents a single row of data in the sheet.
     * Each row is stored as an array of columns (strings).
     */
    public static class SheetRow {
        private final String[] columns;

        // Constructor: receives an array of strings representing columns
        SheetRow(String[] columns) {
            this.columns = columns;
        }

        /**
         * Returns the value of a column by index.
         * If the index is out of bounds, returns an empty string.
         * @param columnIndex index of the column (0-based)
         * @return value of the column or empty string if index invalid
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
     * Fetches all rows from a given Google Sheet tab.
     *
     * @param tabName name of the sheet tab to fetch
     * @param skipHeader whether to skip the first row (usually the header)
     * @return a List of SheetRow objects containing the sheet data
     */
    public static List<SheetRow> fetch(String tabName, boolean skipHeader) {
        List<SheetRow> results = new ArrayList<>();

        try {
            // Convert the tab name to handle spaces in the URL
            String encodedTab = tabName.replace(" ", "%20");

            // Construct the URL to download CSV data for the given sheet tab
            String urlStr = "https://docs.google.com/spreadsheets/d/" + SHEET_ID
                    + "/gviz/tq?tqx=out:csv&sheet=" + encodedTab;

            // Open a network connection to the URL
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");  // Use HTTP GET method
            connection.connect();                // Establish the connection

            // Wrap the input stream with BufferedReader to read line by line
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));

            String line;
            boolean firstLine = true;  // Track first line to skip header if needed

            // Read each line of the CSV file
            while ((line = reader.readLine()) != null) {
                // Skip the first line if it's a header row
                if (firstLine && skipHeader) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                // Parse the CSV line into columns and create a SheetRow object
                results.add(new SheetRow(parseCSVLine(line)));
            }

            // Close the reader to free system resources
            reader.close();

        } catch (Exception e) {
            // If something goes wrong (network error, URL error, etc.), log the error
            Log.e("SheetFetcher", "Failed to fetch sheet: " + tabName, e);
        }

        return results;
    }

    /**
     * Parses a single CSV line into an array of strings, handling quoted commas.
     *
     * @param line CSV line to parse
     * @return an array of column values
     */
    static String[] parseCSVLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;           // Track whether we are inside quotes
        StringBuilder current = new StringBuilder();  // Build the current column

        for (char c : line.toCharArray()) {
            if (c == '"') {
                // Toggle inQuotes flag when encountering a quote
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                // If we see a comma outside quotes, column ends here
                tokens.add(current.toString().trim());
                current.setLength(0);  // Clear StringBuilder for next column
            } else {
                // Otherwise, append character to current column
                current.append(c);
            }
        }
        // Add the last column after finishing the line
        tokens.add(current.toString().trim());

        // Convert List<String> to String[] and return
        return tokens.toArray(new String[0]);
    }
}