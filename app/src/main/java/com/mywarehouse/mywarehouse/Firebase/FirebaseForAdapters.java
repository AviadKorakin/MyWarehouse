package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import com.google.firebase.firestore.FieldValue;

import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImages;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;
import com.mywarehouse.mywarehouse.Models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;

public class FirebaseForAdapters extends FirebaseManager {

    private static Map<String, ListenerRegistration> itemListenersMap = new HashMap<>();

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

    public interface ItemChangeListener {
        void onItemChanged(Item updatedItem);
    }

    public static void fetchItem(String documentId, ItemCallback callback) {
        db.collection("items").document(documentId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Item item = task.getResult().toObject(Item.class);
                callback.onCallback(item);
            }
        });
    }

    public static void listenToItemChanges(String documentId, ItemChangeListener listener) {
        if (itemListenersMap.containsKey(documentId)) {
            // Listener already exists for this item, do nothing
            return;
        }

        ListenerRegistration listenerRegistration = db.collection("items").document(documentId)
                .addSnapshotListener((DocumentSnapshot snapshot, FirebaseFirestoreException e) -> {
                    if (e != null) {
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Item updatedItem = snapshot.toObject(Item.class);
                        listener.onItemChanged(updatedItem);
                    }
                });

        itemListenersMap.put(documentId, listenerRegistration);
    }

    public static void removeAllItemChangeListeners() {
        for (ListenerRegistration listener : itemListenersMap.values()) {
            if (listener != null) {
                listener.remove();  // Remove each listener
            }
        }
        itemListenersMap.clear();  // Clear the map after removing all listeners
    }

    public static void addUserPickup(String userId, String orderId, FirestoreCallback callback) {
        db.collection("users").document(userId).update("pickups", FieldValue.arrayUnion(orderId))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void createTransactionRequest(TransactionRequest request, FirestoreCallback callback) {
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
                if(item !=null) {
                    PickupItemWithImages pickupItemWithImages;
                    if (item.getImageUrls() == null)
                        pickupItemWithImages = new PickupItemWithImages(pickupItem, new ArrayList<>());
                    else
                        pickupItemWithImages = new PickupItemWithImages(pickupItem, item.getImageUrls());
                    pickupItemsWithImagesList.add(pickupItemWithImages);

                    if (pickupItemsWithImagesList.size() == pickupItems.size()) {
                        callback.onCallback(pickupItemsWithImagesList);
                    }
                }
            });
        }
    }

    public static void fetchOrder(String orderId, OrderCallback callback) {
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
        db.collection("orders").document(order.getOrderId())
                .set(order)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void deleteTransactionRequest(String requestId, FirestoreCallback callback) {
        db.collection("transactionRequests").document(requestId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void removeUserPickup(String userId, String orderId, FirestoreCallback callback) {
        db.collection("users").document(userId).update("pickups", FieldValue.arrayRemove(orderId))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void updateItemOnUpdateStatus(String documentId, boolean status, FirestoreCallback callback) {
        db.collection("items").document(documentId).update("onUpdate", status)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void checkOrderAvailability(Order order, FirestoreCallback callback) {
        fetchOrder(order.getOrderId(), new OrderCallback() {
            @Override
            public void onCallback(Order latestOrder) {
                if (latestOrder != null && latestOrder.getStatus() == OrderType.IN_PROGRESS) {
                    List<PickupItem> pickupItems = latestOrder.getPickupItems();
                    int totalItems = pickupItems.size();
                    AtomicInteger processedItems = new AtomicInteger(0);

                    for (PickupItem pickupItem : pickupItems) {
                        String documentId = pickupItem.getBarcode() + "_" + pickupItem.getName();
                        fetchItem(documentId, item -> {
                            int availableQuantity=0;
                            List<ItemWarehouse> list = item.getWarehouseItemMap().getOrDefault(latestOrder.getSelectedWarehouse(),null);
                            if(list!=null) {
                                availableQuantity = list.stream()
                                        .mapToInt(ItemWarehouse::getQuantity)
                                        .sum();
                            }

                            if (availableQuantity < pickupItem.getQuantity()) {
                                callback.onFailure(new Exception("Not enough quantity available for item: " + pickupItem.getName()));
                                return;
                            }

                            if (processedItems.incrementAndGet() == totalItems) {
                                callback.onSuccess();
                            }
                        });
                    }
                } else {
                    callback.onFailure(new Exception("Order status has changed or order is not in progress"));
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
}