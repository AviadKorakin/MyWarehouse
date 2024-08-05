package com.mywarehouse.mywarehouse.Firebase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.mywarehouse.mywarehouse.Enums.LogType;
import com.mywarehouse.mywarehouse.Models.MyLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FirebaseLogs {

    public interface LogsCallback {
        void onCallback(List<MyLog> logs);
        void onFailure(Exception e);
    }

    public static void fetchLogs(FirebaseFirestore db, LogsCallback callback) {
        db.collection("logs")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            List<MyLog> logs = new ArrayList<>();
                            for (DocumentSnapshot document : querySnapshot) {
                                MyLog log = document.toObject(MyLog.class);
                                if (log != null) {
                                    logs.add(log);
                                }
                            }
                            callback.onCallback(logs);
                        } else {
                            callback.onFailure(new Exception("QuerySnapshot is null"));
                        }
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public static void filterLogsByDateAndType(FirebaseFirestore db, Date date, LogType type, LogsCallback callback) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = sdf.format(date);

        db.collection("logs")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<MyLog> filteredLogs = new ArrayList<>();
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            for (DocumentSnapshot document : querySnapshot) {
                                MyLog log = document.toObject(MyLog.class);
                                if (log != null && sdf.format(log.getDate()).equals(dateString) &&
                                        (type == LogType.ALL || log.getType() == type)) {
                                    filteredLogs.add(log);
                                }
                            }
                            callback.onCallback(filteredLogs);
                        } else {
                            callback.onFailure(new Exception("QuerySnapshot is null"));
                        }
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }
}

