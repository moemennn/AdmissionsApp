package edu.fandm.mibrahi1.admissionsapp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SheetFetcher {

    static final String SHEET_ID = "1gKC6h1FUn88JZbnggXAnBqJtY-dacVYxAzbzdoYLiuU";

    public static class SheetRow {
        private final String[] columns;

        SheetRow(String[] columns) {
            this.columns = columns;
        }

        public String get(int columnIndex) {
            if (columnIndex < 0 || columnIndex >= columns.length) return "";
            return columns[columnIndex];
        }

        public int size() {
            return columns.length;
        }
    }

    public static List<SheetRow> fetch(String tabName, boolean skipHeader) {
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
                if (firstLine && skipHeader) { firstLine = false; continue; }
                firstLine = false;
                results.add(new SheetRow(parseCSVLine(line)));
            }
            reader.close();

        } catch (Exception e) {
            Log.e("SheetFetcher", "Failed to fetch sheet: " + tabName, e);
        }
        return results;
    }

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