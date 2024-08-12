package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Query;
import com.mywarehouse.mywarehouse.Enums.LogType;
import com.mywarehouse.mywarehouse.Models.MyLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FirebaseLogs extends FirebaseManager {

    public interface LogsCallback {
        void onCallback(List<MyLog> logs);
        void onFailure(Exception e);
    }

    public static void listenToLogs(LogsCallback callback) {
        db.collection("logs")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            callback.onFailure(e);
                            return;
                        }
                        if (snapshots != null) {
                            List<MyLog> logs = new ArrayList<>();
                            for (DocumentSnapshot document : snapshots.getDocuments()) {
                                MyLog log = document.toObject(MyLog.class);
                                if (log != null) {
                                    logs.add(log);
                                }
                            }
                            callback.onCallback(logs);
                        }
                    }
                });
    }

    public static void filterLogsByDateAndType(Date date, LogType type, LogsCallback callback) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = sdf.format(date);

        db.collection("logs")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            callback.onFailure(e);
                            return;
                        }
                        if (snapshots != null) {
                            List<MyLog> filteredLogs = new ArrayList<>();
                            for (DocumentSnapshot document : snapshots.getDocuments()) {
                                MyLog log = document.toObject(MyLog.class);
                                if (log != null && sdf.format(log.getDate()).equals(dateString) &&
                                        (type == LogType.ALL || log.getType() == type)) {
                                    filteredLogs.add(log);
                                }
                            }
                            callback.onCallback(filteredLogs);
                        }
                    }
                });
    }
}
