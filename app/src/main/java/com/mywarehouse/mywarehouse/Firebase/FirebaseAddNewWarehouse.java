package com.mywarehouse.mywarehouse.Firebase;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mywarehouse.mywarehouse.Enums.LogType;
import com.mywarehouse.mywarehouse.Models.MyLog;
import com.mywarehouse.mywarehouse.Models.Warehouse;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FirebaseAddNewWarehouse {

    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void saveWarehouse(String name, List<LatLng> points, boolean active, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Warehouse warehouse = new Warehouse(name, points, active);

        db.collection("warehouses").document(name).set(warehouse)
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void checkWarehouseExists(String name, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("warehouses").document(name).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onFailure(new Exception("Warehouse name already exists"));
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public static void saveLog(String warehouseName, Date date, String invokedBy, FirestoreCallback callback) {
        String notes = "Warehouse " + warehouseName + " created successfully";
        MyLog myLog = new MyLog("Warehouse creation", date, notes, invokedBy, LogType.WAREHOUSE_CREATION);
        String logId = "warehouse_creation_" + warehouseName + "_" + UUID.randomUUID().toString();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("logs").document(logId).set(myLog)
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
