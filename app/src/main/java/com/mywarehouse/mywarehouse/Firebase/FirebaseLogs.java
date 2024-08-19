package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.mywarehouse.mywarehouse.Models.MyLog;

import java.util.HashMap;
import java.util.Map;

public class FirebaseLogs extends FirebaseManager {

    private static ListenerRegistration logListener;

    public interface LogsCallback {
        void onLogAdded(MyLog log);
        void onLogModified(MyLog log);
        void onLogRemoved(String logId);
        void onFailure(Exception e);
    }

    public interface LogCountCallback {
        void onLogCountFetched(int count);
        void onFailure(Exception e);
    }

    public static void fetchTotalLogCount(LogCountCallback callback) {
        AggregateQuery countQuery = db.collection("logs").count();
        countQuery.get(AggregateSource.SERVER).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                AggregateQuerySnapshot snapshot = task.getResult();
                callback.onLogCountFetched((int) snapshot.getCount());
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public static void listenToLogs(LogsCallback callback) {
        if (logListener != null) {
            logListener.remove(); // Remove any existing listener to avoid duplicates
        }


        logListener = db.collection("logs")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        callback.onFailure(e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            MyLog log = dc.getDocument().toObject(MyLog.class);
                            switch (dc.getType()) {
                                case ADDED:
                                    callback.onLogAdded(log);
                                    break;
                                case MODIFIED:
                                    callback.onLogModified(log);
                                    break;
                                case REMOVED:
                                    callback.onLogRemoved(dc.getDocument().getId());
                                    break;
                            }
                        }
                    }
                });
    }

    public static void removeAllListeners() {
        if (logListener != null) {
            logListener.remove();
            logListener = null;
        }
    }
}
