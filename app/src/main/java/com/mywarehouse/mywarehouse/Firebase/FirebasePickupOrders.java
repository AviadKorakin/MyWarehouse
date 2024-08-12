package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImages;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.Warehouse;

import java.util.ArrayList;
import java.util.List;

public class FirebasePickupOrders extends FirebaseManager {

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

    public static void fetchWarehouses(FetchCallback<Warehouse> callback) {
        db.collection("warehouses").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Warehouse> warehouseList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Warehouse warehouse = document.toObject(Warehouse.class);
                    warehouseList.add(warehouse);
                }
                callback.onSuccess(warehouseList);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public static void fetchOrders(FetchCallback<Order> callback) {
        db.collection("orders").whereEqualTo("status", OrderType.REGISTERED.name()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Order> orderList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Order order = document.toObject(Order.class);
                    orderList.add(order);
                }
                callback.onSuccess(orderList);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public static void listenToOrderChanges(OrdersListenerCallback callback) {
        db.collection("orders").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    callback.onFailure(e);
                    return;
                }

                if (snapshots != null) {
                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        Order order = change.getDocument().toObject(Order.class);
                        switch (change.getType()) {
                            case ADDED:
                                callback.onOrderAdded(order);
                                break;
                            case MODIFIED:
                                callback.onOrderModified(order);
                                break;
                            case REMOVED:
                                callback.onOrderRemoved(order);
                                break;
                        }
                    }
                }
            }
        });
    }

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

    public static void listenToItemChanges(ItemCallback callback) {
        db.collection("items").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    // Handle error
                    return;
                }

                for (DocumentSnapshot document : snapshots.getDocuments()) {
                    Item item = document.toObject(Item.class);
                    callback.onCallback(item);
                }
            }
        });
    }
}
