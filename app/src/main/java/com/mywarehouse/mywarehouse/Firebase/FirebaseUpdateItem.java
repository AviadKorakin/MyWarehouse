package com.mywarehouse.mywarehouse.Firebase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mywarehouse.mywarehouse.Enums.LogType;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.MyLog;
import com.mywarehouse.mywarehouse.Models.Warehouse;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FirebaseUpdateItem {

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    public static void fetchItem(String documentId, FirestoreCallback<Item> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("items").document(documentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Item item = documentSnapshot.toObject(Item.class);
                        callback.onSuccess(item);
                    } else {
                        callback.onFailure(new Exception("Item not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public static void saveItem(String documentId, Item item, FirestoreCallback<Void> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("items").document(documentId).set(item)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public static void saveLog(MyLog log, FirestoreCallback<Void> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String logId = "log_" + UUID.randomUUID().toString();
        db.collection("logs").document(logId).set(log)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public static void fetchWarehouses(FirestoreCallback<List<Warehouse>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("warehouses")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Warehouse> warehouseList = queryDocumentSnapshots.toObjects(Warehouse.class);
                    callback.onSuccess(warehouseList);
                })
                .addOnFailureListener(callback::onFailure);
    }
}

