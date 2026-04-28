package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * EXAM NOTE: This class handles "Local Persistence" and "Asynchronous Data Fetching."
 */
public class FirebaseFetcher {

    // TAG used for debugging in Logcat to find errors for this specific class
    private static final String TAG = "FirebaseFetcher";
    // The name of the XML file where we store the data version IDs
    private static final String PREFS_NAME = "FirebaseCachePrefs";

    // Firestore instance: The main gateway to the Google database
    private final FirebaseFirestore db;
    // Context: Necessary to access local file storage (Internal Storage)
    private final Context context;
    // GSON: A Google library that converts Java Objects into JSON strings (and vice versa)
    private final Gson gson = new Gson();

    public FirebaseFetcher(Context context) {
        // We use getApplicationContext() to prevent memory leaks by not holding onto an Activity
        this.context = context.getApplicationContext();
        // Initialize the Firestore connection
        db = FirebaseFirestore.getInstance();
    }

    // INTERFACE: A custom listener that tells the UI when a list of data is ready
    public interface OnCollectionLoadedListener {
        void onSuccess(List<FirebaseRow> rows);
        void onFailure(Exception e);
    }

    // INTERFACE: A listener specifically for checking the "Version" document
    public interface OnVersionLoadedListener {
        void onSuccess(String version);
        void onFailure(Exception e);
    }

    /**
     * INNER CLASS: FirebaseRow
     * This acts as a "Wrapper." Instead of passing around raw Firestore documents,
     * we wrap them in this class to make it easier to get Strings safely.
     */
    public static class FirebaseRow {
        private final String documentId;
        private final Map<String, Object> data;

        public FirebaseRow(String documentId, Map<String, Object> data) {
            this.documentId = documentId;
            this.data = data;
        }

        public String getDocumentId() {
            return documentId;
        }

        // METHOD: Safely gets a value from the map. If the value is missing, it returns ""
        // to prevent NullPointerExceptions in the UI.
        public String getString(String fieldName) {
            Object value = data.get(fieldName);
            return value == null ? "" : value.toString();
        }

        public Map<String, Object> getData() {
            return data;
        }
    }

    // METHOD: Fetches a whole collection from Firestore directly (No caching here)
    public void fetchCollection(String collectionName, OnCollectionLoadedListener listener) {
        db.collection(collectionName)
                .get() // Triggers the network request
                .addOnSuccessListener(querySnapshot -> {
                    List<FirebaseRow> rows = new ArrayList<>();
                    // Loop through every document found in the collection
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        rows.add(new FirebaseRow(doc.getId(), doc.getData()));
                    }
                    // Trigger the success callback to the UI
                    listener.onSuccess(rows);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch collection: " + collectionName, e);
                    listener.onFailure(e);
                });
    }

    /**
     * METHOD: fetchVersion
     * Checks a specific document called "version" inside a collection called "Meta".
     * This tells the app if the building or counselor data has changed in the cloud.
     */
    public void fetchVersion(String collectionName, OnVersionLoadedListener listener) {
        db.collection("Meta")
                .document("version")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get the version string for the specific collection (e.g., "Buildings")
                        Object value = documentSnapshot.get(collectionName);
                        listener.onSuccess(value == null ? "" : value.toString());
                    } else {
                        listener.onSuccess("");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch version for: " + collectionName, e);
                    listener.onFailure(e);
                });
    }

    /**
     * THE MASTER DATA LOGIC: fetchCollectionWithCache
     * This is what the app actually calls.
     * 1. It checks the version on the cloud.
     * 2. It checks the version stored on the phone.
     * 3. If they match, it skips the download and reads from a local JSON file.
     */
    public void fetchCollectionWithCache(String collectionName, OnCollectionLoadedListener listener) {
        fetchVersion(collectionName, new OnVersionLoadedListener() {
            @Override
            public void onSuccess(String remoteVersion) {
                // Get the version we saved last time the app ran
                String localVersion = getLocalVersion(collectionName);
                // Check if the JSON data file actually exists on the phone
                boolean cacheExists = cacheFileExists(collectionName);

                // LOGIC: If Cloud Version == Local Version, use local storage (Fast + Offline)
                if (!remoteVersion.isEmpty() && remoteVersion.equals(localVersion) && cacheExists) {
                    Log.d(TAG, "Loading from local cache: " + collectionName);
                    List<FirebaseRow> cachedRows = readCacheFile(collectionName);
                    listener.onSuccess(cachedRows);
                    return; // Exit early: No need to download anything!
                }

                // LOGIC: Versions don't match or no cache exists. Need to download.
                Log.d(TAG, "Fetching fresh from Firestore: " + collectionName);

                fetchCollection(collectionName, new OnCollectionLoadedListener() {
                    @Override
                    public void onSuccess(List<FirebaseRow> rows) {
                        // Save the downloaded list as a local JSON file for next time
                        saveCacheFile(collectionName, rows);

                        // Save the new version number to SharedPreferences
                        if (!remoteVersion.isEmpty()) {
                            saveLocalVersion(collectionName, remoteVersion);
                        }
                        listener.onSuccess(rows);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Firestore fetch failed for: " + collectionName, e);
                        // FAIL-SAFE: If internet fails, try to show the user the old data anyway
                        if (cacheExists) {
                            Log.d(TAG, "Firestore failed, using old cache: " + collectionName);
                            listener.onSuccess(readCacheFile(collectionName));
                        } else {
                            listener.onFailure(e);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Version check failed for: " + collectionName, e);
                // Even if we can't check the version, try to load cache as a backup
                if (cacheFileExists(collectionName)) {
                    Log.d(TAG, "Version check failed, using cache: " + collectionName);
                    listener.onSuccess(readCacheFile(collectionName));
                } else {
                    listener.onFailure(e);
                }
            }
        });
    }

    // HELPER: Retrieves a version string from SharedPreferences (Key-Value storage)
    private String getLocalVersion(String collectionName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(collectionName + "_version", "");
    }

    // HELPER: Saves a version string to SharedPreferences
    private void saveLocalVersion(String collectionName, String version) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(collectionName + "_version", version).apply();
    }

    // HELPER: Checks if a .json file for this collection exists in internal storage
    private boolean cacheFileExists(String collectionName) {
        File file = new File(context.getFilesDir(), collectionName + ".json");
        return file.exists();
    }

    /**
     * METHOD: saveCacheFile
     * Converts a Java List into a JSON string and writes it to a file.
     * This is "Serialization."
     */
    private void saveCacheFile(String collectionName, List<FirebaseRow> rows) {
        File file = new File(context.getFilesDir(), collectionName + ".json");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // gson.toJson turns the Java object into a long text string
            writer.write(gson.toJson(rows));
        } catch (Exception e) {
            Log.e(TAG, "Failed to save cache for: " + collectionName, e);
        }
    }

    /**
     * METHOD: readCacheFile
     * Reads the JSON text from the phone and turns it back into Java Objects.
     * This is "Deserialization."
     */
    private List<FirebaseRow> readCacheFile(String collectionName) {
        File file = new File(context.getFilesDir(), collectionName + ".json");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // TypeToken is needed because GSON needs to know exactly what kind of List we want
            Type type = new TypeToken<List<FirebaseRow>>() {}.getType();
            List<FirebaseRow> rows = gson.fromJson(reader, type);
            return rows == null ? new ArrayList<>() : rows;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read cache for: " + collectionName, e);
            return new ArrayList<>();
        }
    }
}