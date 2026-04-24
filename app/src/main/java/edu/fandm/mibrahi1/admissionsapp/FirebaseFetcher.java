package edu.fandm.mibrahi1.admissionsapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
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

public class FirebaseFetcher {

    private static final String TAG = "FirebaseFetcher";
    private static final String PREFS_NAME = "FirebaseCachePrefs";

    private final FirebaseFirestore db;
    private final Context context;
    private final Gson gson = new Gson();

    public FirebaseFetcher(Context context) {
        this.context = context.getApplicationContext();
        db = FirebaseFirestore.getInstance();
    }

    public interface OnCollectionLoadedListener {
        void onSuccess(List<FirebaseRow> rows);
        void onFailure(Exception e);
    }

    public interface OnVersionLoadedListener {
        void onSuccess(String version);
        void onFailure(Exception e);
    }

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

        public String getString(String fieldName) {
            Object value = data.get(fieldName);
            return value == null ? "" : value.toString();
        }

        public Map<String, Object> getData() {
            return data;
        }
    }

    public void fetchCollection(String collectionName, OnCollectionLoadedListener listener) {
        db.collection(collectionName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<FirebaseRow> rows = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        rows.add(new FirebaseRow(doc.getId(), doc.getData()));
                    }

                    listener.onSuccess(rows);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch collection: " + collectionName, e);
                    listener.onFailure(e);
                });
    }

    public void fetchVersion(String collectionName, OnVersionLoadedListener listener) {
        db.collection("Meta")
                .document("version")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
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

    public void fetchCollectionWithCache(String collectionName, OnCollectionLoadedListener listener) {
        fetchVersion(collectionName, new OnVersionLoadedListener() {
            @Override
            public void onSuccess(String remoteVersion) {
                String localVersion = getLocalVersion(collectionName);
                boolean cacheExists = cacheFileExists(collectionName);

                if (!remoteVersion.isEmpty()
                        && remoteVersion.equals(localVersion)
                        && cacheExists) {

                    Log.d(TAG, "Loading from local cache: " + collectionName);
                    List<FirebaseRow> cachedRows = readCacheFile(collectionName);
                    listener.onSuccess(cachedRows);
                    return;
                }

                Log.d(TAG, "Fetching fresh from Firestore: " + collectionName);

                fetchCollection(collectionName, new OnCollectionLoadedListener() {
                    @Override
                    public void onSuccess(List<FirebaseRow> rows) {
                        saveCacheFile(collectionName, rows);

                        if (!remoteVersion.isEmpty()) {
                            saveLocalVersion(collectionName, remoteVersion);
                        }

                        listener.onSuccess(rows);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Firestore fetch failed for: " + collectionName, e);

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

                if (cacheFileExists(collectionName)) {
                    Log.d(TAG, "Version check failed, using cache: " + collectionName);
                    listener.onSuccess(readCacheFile(collectionName));
                } else {
                    listener.onFailure(e);
                }
            }
        });
    }

    private String getLocalVersion(String collectionName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(collectionName + "_version", "");
    }

    private void saveLocalVersion(String collectionName, String version) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(collectionName + "_version", version)
                .apply();
    }

    private boolean cacheFileExists(String collectionName) {
        File file = new File(context.getFilesDir(), collectionName + ".json");
        return file.exists();
    }

    private void saveCacheFile(String collectionName, List<FirebaseRow> rows) {
        File file = new File(context.getFilesDir(), collectionName + ".json");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(gson.toJson(rows));
        } catch (Exception e) {
            Log.e(TAG, "Failed to save cache for: " + collectionName, e);
        }
    }

    private List<FirebaseRow> readCacheFile(String collectionName) {
        File file = new File(context.getFilesDir(), collectionName + ".json");

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Type type = new TypeToken<List<FirebaseRow>>() {}.getType();
            List<FirebaseRow> rows = gson.fromJson(reader, type);
            return rows == null ? new ArrayList<>() : rows;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read cache for: " + collectionName, e);
            return new ArrayList<>();
        }
    }
}