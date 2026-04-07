package edu.fandm.mibrahi1.admissionsapp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SheetFetcher {

    private static final String SHEET_ID = "1gKC6h1FUn88JZbnggXAnBqJtY-dacVYxAzbzdoYLiuU";

    /**
     * Represents a single row, containing only the requested columns.
     * Access values by their original column index (e.g., row.get(3) for column D).
     */
    public static class SheetRow {
        private final String[] allColumns;

        SheetRow(String[] allColumns) {
            this.allColumns = allColumns;
        }

        /** Get value by original column index (0 = A, 1 = B, 3 = D, etc.) */
        public String get(int columnIndex) {
            if (columnIndex < 0 || columnIndex >= allColumns.length) return "";
            return allColumns[columnIndex];
        }
    }

    /**
     * Fetches a specific tab from the Google Sheet and returns only the requested columns.
     *
     * @param tabName    The sheet tab name (e.g., "International by Country")
     * @param columns    Column indices to extract (0 = A, 1 = B, 3 = D, etc.)
     * @param skipHeader Whether to skip the first row
     * @return List of SheetRow, each queryable by original column index
     */
    public static List<SheetRow> fetch(String tabName, int[] columns, boolean skipHeader) {
        List<SheetRow> results = new ArrayList<>();
        try {
            String encodedTab = tabName.replace(" ", "%20");
            String urlStr = "https://docs.google.com/spreadsheets/d/" + SHEET_ID
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

                String[] rawColumns = parseCSVLine(line);

                // Build a sparse array sized to the highest requested column
                int maxIndex = 0;
                for (int col : columns) maxIndex = Math.max(maxIndex, col);
                String[] extracted = new String[maxIndex + 1];

                for (int col : columns) {
                    extracted[col] = (col < rawColumns.length)
                            ? rawColumns[col].replace("\"", "").trim()
                            : "";
                }

                results.add(new SheetRow(extracted));
            }
            reader.close();

        } catch (Exception e) {
            android.util.Log.e("SheetFetcher", "Failed to fetch sheet: " + tabName, e);
        }
        return results;
    }

    /** Splits a CSV line, respecting quoted fields that may contain commas. */
    private static String[] parseCSVLine(String line) {
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