package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImages;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;
import com.mywarehouse.mywarehouse.Models.User;

import java.util.ArrayList;
import java.util.List;

public class FirebaseForAdapters {

    public interface ItemCallback {
        void onCallback(Item item);
    }

    public interface PickupItemsCallback {
        void onCallback(List<PickupItemWithImages> pickupItemsWithImages);
    }

    public interface FirestoreCallback {
        void onSuccess();

        void onFailure(Exception e);
    }

    public interface UserCallback {
        void onCallback(User user);
    }

    public interface OrderCallback {
        void onCallback(Order order);

        void onFailure(Exception e);
    }

    public static void fetchItem(String documentId, ItemCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("items").document(documentId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Item item = task.getResult().toObject(Item.class);
                callback.onCallback(item);
            }
        });
    }

    public static void addUserPickup(String userId, String orderId, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).update("pickups", FieldValue.arrayUnion(orderId))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void createTransactionRequest(TransactionRequest request, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference = db.collection("transactionRequests").document();
        request.setRequestId(documentReference.getId());
        documentReference.set(request)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void fetchPickupItemsWithImages(List<PickupItem> pickupItems, PickupItemsCallback callback) {
        List<PickupItemWithImages> pickupItemsWithImagesList = new ArrayList<>();

        for (PickupItem pickupItem : pickupItems) {
            String documentId = pickupItem.getBarcode() + "_" + pickupItem.getName();
            fetchItem(documentId, item -> {
                PickupItemWithImages pickupItemWithImages = new PickupItemWithImages(pickupItem, item.getImageUrls());
                pickupItemsWithImagesList.add(pickupItemWithImages);

                if (pickupItemsWithImagesList.size() == pickupItems.size()) {
                    callback.onCallback(pickupItemsWithImagesList);
                }
            });
        }
    }

    public static void fetchOrder(String orderId, OrderCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders").document(orderId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Order order = task.getResult().toObject(Order.class);
                callback.onCallback(order);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public static void updateOrder(Order order, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders").document(order.getOrderId())
                .set(order)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void deleteTransactionRequest(String requestId, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("transactionRequests").document(requestId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void removeUserPickup(String userId, String orderId, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).update("pickups", FieldValue.arrayRemove(orderId))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void fetchUser(String userId, UserCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult().toObject(User.class);
                callback.onCallback(user);
            }
        });
    }
}
