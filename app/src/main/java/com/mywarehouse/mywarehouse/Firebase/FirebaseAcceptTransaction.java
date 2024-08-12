package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.FirebaseFirestore;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;
import com.mywarehouse.mywarehouse.Models.Warehouse;

public class FirebaseAcceptTransaction extends FirebaseManager {

    public static void fetchWarehouse(String warehouseName, WarehouseCallback callback) {
        db.collection("warehouses").document(warehouseName).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Warehouse warehouse = documentSnapshot.toObject(Warehouse.class);
                        warehouse.sortPoints();
                        callback.onWarehouseLoaded(warehouse);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle any errors here
                });
    }

    public static void fetchItem(String documentId, ItemCallback callback) {
        db.collection("items").document(documentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Item item = documentSnapshot.toObject(Item.class);
                        callback.onItemLoaded(item);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle any errors here
                });
    }

    public static void updateItem(Item item) {
        db.collection("items").document(item.getBarcode() + "_" + item.getName())
                .set(item)
                .addOnSuccessListener(aVoid -> {
                    // Successfully updated item
                })
                .addOnFailureListener(e -> {
                    // Handle any errors here
                });
    }

    public static void updateItemOnUpdateStatus(String documentId, boolean status, FirestoreCallback callback) {
        db.collection("items").document(documentId).update("onUpdate", status)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void deleteTransactionRequest(String requestId, FirebaseAcceptTransaction.FirestoreCallback callback) {
        db.collection("transactionRequests").document(requestId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void fetchOrder(String orderId, FirebaseAcceptTransaction.OrderCallback callback) {
        db.collection("orders").document(orderId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Order order = task.getResult().toObject(Order.class);
                callback.onCallback(order);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public static void updateOrder(Order order, FirebaseAcceptTransaction.FirestoreCallback callback) {
        db.collection("orders").document(order.getOrderId())
                .set(order)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void fetchTransactionRequest(String requestId, TransactionRequestCallback callback) {
        db.collection("transactionRequests").document(requestId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                TransactionRequest request = task.getResult().toObject(TransactionRequest.class);
                callback.onCallback(request);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }


    public static void updateTransactionRequestOnUpdateStatus(String requestId, boolean status, FirestoreCallback callback) {
        db.collection("transactionRequests").document(requestId).update("onUpdate", status)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public interface WarehouseCallback {
        void onWarehouseLoaded(Warehouse warehouse);
    }

    public interface ItemCallback {
        void onItemLoaded(Item item);
    }

    public interface FirestoreCallback {
        void onSuccess();

        void onFailure(Exception e);
    }

    public interface OrderCallback {
        void onCallback(Order order);

        void onFailure(Exception e);
    }

    public interface TransactionRequestCallback {
        void onCallback(TransactionRequest request);

        void onFailure(Exception e);
    }
}
