package com.mywarehouse.mywarehouse.Firebase;

import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.MyLog;
import com.mywarehouse.mywarehouse.Models.Warehouse;

import java.util.List;

public class FirebaseUpdateItem extends  FirebaseManager{

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
    public interface FirestoreCallbackNoType {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void fetchItem(String documentId, FirestoreCallback<Item> callback) {
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
        db.collection("items").document(documentId).set(item)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public static void saveLog(MyLog log, FirestoreCallback<Void> callback) {
        db.collection("logs").document(log.getId()).set(log)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public static void fetchWarehouses(FirestoreCallback<List<Warehouse>> callback) {
        db.collection("warehouses")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Warehouse> warehouseList = queryDocumentSnapshots.toObjects(Warehouse.class);
                    for (Warehouse warehouse: warehouseList) {
                        warehouse.sortPoints();
                    }
                    callback.onSuccess(warehouseList);
                })
                .addOnFailureListener(callback::onFailure);
    }
    public static void updateIsOnUpdateField(String documentId, boolean onUpdate, FirestoreCallbackNoType callback) {
        db.collection("items").document(documentId)
                .update("onUpdate", onUpdate)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}

