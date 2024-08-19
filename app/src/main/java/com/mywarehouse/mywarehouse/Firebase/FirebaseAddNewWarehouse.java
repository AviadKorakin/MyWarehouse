package com.mywarehouse.mywarehouse.Firebase;

import com.google.android.gms.maps.model.LatLng;
import com.mywarehouse.mywarehouse.Enums.LogType;
import com.mywarehouse.mywarehouse.Models.MyLog;
import com.mywarehouse.mywarehouse.Models.Warehouse;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FirebaseAddNewWarehouse extends FirebaseManager{
    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void saveWarehouse(String name, List<LatLng> points, boolean active, FirestoreCallback callback) {
        Warehouse warehouse = new Warehouse(name, points, active);

        db.collection("warehouses").document(name).set(warehouse)
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void checkWarehouseExists(String name, FirestoreCallback callback) {
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
        MyLog myLog = new MyLog(UUID.randomUUID().toString(),"Warehouse creation", date, notes, invokedBy, LogType.WAREHOUSE_CREATION);
        db.collection("logs").document(myLog.getId()).set(myLog)
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
