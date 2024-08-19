package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImages;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.Warehouse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebasePickupOrders extends FirebaseManager {

    private static ListenerRegistration ordersListenerRegistration;
    private static Map<String, ListenerRegistration> itemListenersMap = new HashMap<>();

    public interface FetchCallback<T> {
        void onSuccess(List<T> items);
        void onFailure(Exception e);
    }

    public interface ItemCallback {
        void onCallback(Item item);
    }

    public interface PickupItemsCallback {
        void onCallback(List<PickupItemWithImages> pickupItemsWithImages);
    }

    public interface OrdersListenerCallback {
        void onOrderAdded(Order order);
        void onOrderModified(Order order);
        void onOrderRemoved(Order order);
        void onFailure(Exception e);
    }

    public interface OrderCountCallback {
        void onOrderCountFetched(int count);
        void onFailure(Exception e);
    }

    // Fetch the list of warehouses
    public static void fetchWarehouses(FetchCallback<Warehouse> callback) {
        db.collection("warehouses").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Warehouse> warehouseList = new ArrayList<>();
                task.getResult().forEach(document -> {
                    Warehouse warehouse = document.toObject(Warehouse.class);
                    warehouseList.add(warehouse);
                });
                callback.onSuccess(warehouseList);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    // Fetch an individual item by its document ID
    public static void fetchItem(String documentId, ItemCallback callback) {
        db.collection("items").document(documentId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Item item = task.getResult().toObject(Item.class);
                callback.onCallback(item);
            } else {
                callback.onCallback(null); // Return null if the item fetch fails
            }
        });
    }

    // Fetch pickup items with associated images
    public static void fetchPickupItemsWithImages(List<PickupItem> pickupItems, PickupItemsCallback callback) {
        List<PickupItemWithImages> pickupItemsWithImagesList = new ArrayList<>();

        for (PickupItem pickupItem : pickupItems) {
            String documentId = pickupItem.getBarcode() + "_" + pickupItem.getName();
            fetchItem(documentId, item -> {
                if (item != null) {
                    PickupItemWithImages pickupItemWithImages = new PickupItemWithImages(pickupItem, item.getImageUrls());
                    pickupItemsWithImagesList.add(pickupItemWithImages);
                }

                // Once all pickup items are fetched, return the result via callback
                if (pickupItemsWithImagesList.size() == pickupItems.size()) {
                    callback.onCallback(pickupItemsWithImagesList);
                }
            });
        }
    }

    // Listen to changes on individual items
    public static void listenToItemChanges(String documentId, ItemCallback callback) {
        if (itemListenersMap.containsKey(documentId)) {
            return; // Listener already exists for this item
        }

        ListenerRegistration itemListener = db.collection("items").document(documentId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Item item = snapshot.toObject(Item.class);
                        callback.onCallback(item);
                    } else {
                        callback.onCallback(null); // Notify that the item was removed
                    }
                });

        itemListenersMap.put(documentId, itemListener);
    }

    // Listen to all orders and filter based on the registered status
    public static void listenToAllOrdersWithStatusFiltering(OrdersListenerCallback callback) {
        ordersListenerRegistration = db.collection("orders")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        callback.onFailure(e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange change : snapshots.getDocumentChanges()) {
                            Order order = change.getDocument().toObject(Order.class);

                            switch (change.getType()) {
                                case ADDED:
                                    if (order.getStatus() == OrderType.REGISTERED) {
                                        callback.onOrderAdded(order);
                                    }
                                    break;
                                case MODIFIED:
                                    if (order.getStatus() == OrderType.REGISTERED) {
                                        callback.onOrderModified(order);
                                    } else {
                                        // If the order's status changed from REGISTERED to something else, consider it removed
                                        callback.onOrderRemoved(order);
                                    }
                                    break;
                                case REMOVED:
                                    callback.onOrderRemoved(order);
                                    break;
                            }
                        }
                    }
                });

        // Store the listener in a map if you need to manage it later
        itemListenersMap.put("allOrdersListener", ordersListenerRegistration);
    }

    // Fetch the count of orders with status REGISTERED
    public static void fetchRegisteredOrderCount(OrderCountCallback callback) {
        AggregateQuery countQuery = db.collection("orders")
                .whereEqualTo("status", OrderType.REGISTERED.name())
                .count();  // Use the count method
        countQuery.get(AggregateSource.SERVER).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                AggregateQuerySnapshot snapshot = task.getResult();
                callback.onOrderCountFetched((int) snapshot.getCount());
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    // Remove the order listeners
    public static void removeAllListeners() {
        if (ordersListenerRegistration != null) {
            ordersListenerRegistration.remove();
            ordersListenerRegistration = null;
        }
        for (ListenerRegistration listener : itemListenersMap.values()) {
            if (listener != null) {
                listener.remove();
            }
        }
        itemListenersMap.clear();  // Clear the map after removing all listeners
    }
}
