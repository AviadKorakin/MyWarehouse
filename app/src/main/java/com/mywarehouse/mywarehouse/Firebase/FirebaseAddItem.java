package com.mywarehouse.mywarehouse.Firebase;

import android.net.Uri;


import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.MyLog;
import com.mywarehouse.mywarehouse.Models.Warehouse;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FirebaseAddItem extends FirebaseManager{

    public interface WarehousesCallback {
        void onCallback(List<Warehouse> warehouseList);
    }

    public interface DocumentExistsCallback {
        void onCallback(boolean exists);
    }

    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void fetchWarehouses(WarehousesCallback callback) {
        db.collection("warehouses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Warehouse> warehouseList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Warehouse warehouse = document.toObject(Warehouse.class);
                        warehouse.sortPoints();
                        warehouseList.add(warehouse);
                    }
                    callback.onCallback(warehouseList);
                })
                .addOnFailureListener(e -> callback.onCallback(null));
    }

    public static void checkDocumentExists(String documentId, DocumentExistsCallback callback) {
        db.collection("items").document(documentId).get()
                .addOnSuccessListener(documentSnapshot -> callback.onCallback(documentSnapshot.exists()))
                .addOnFailureListener(e -> callback.onCallback(false));
    }

    public static void saveItemToFirestore( Item item, String documentId, FirestoreCallback callback) {
        db.collection("items").document(documentId).set(item)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void saveLog( MyLog myLog, FirestoreCallback callback) {
        String logId = "item_creation_" + myLog.getInvokedBy() + "_" + UUID.randomUUID().toString();
        db.collection("logs").document(logId).set(myLog)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void uploadImageToFirebase(Uri imageUri, String imageId, ImageUploadCallback callback) {
        if (imageUri != null) {
            StorageReference storageReference = FirebaseStorage.getInstance().getReference("items").child(imageUri.getLastPathSegment());
            storageReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        callback.onSuccess(uri, imageId);
                    }))
                    .addOnFailureListener(callback::onFailure);
        }
    }

    public interface ImageUploadCallback {
        void onSuccess(Uri uri, String imageId);
        void onFailure(Exception e);
    }
}
