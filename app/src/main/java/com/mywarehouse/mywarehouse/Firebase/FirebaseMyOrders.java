package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.User;
import com.mywarehouse.mywarehouse.Utilities.MyUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseMyOrders extends FirebaseManager {

    private static ListenerRegistration userListener;
    private static final Map<String, ListenerRegistration> orderListeners = new HashMap<>();

    public interface OrdersCallback {
        void onOrderAdded(Order newOrder);
        void onOrderModified(Order updatedOrder);
        void onOrderRemoved(String orderId);
        void onListCleared(); // New method to clear the list
        void onFailure(Exception e);
    }

    public interface OrderCountCallback {
        void onOrderCountFetched(int count);
        void onFailure(Exception e);
    }

    public static void listenToUserOrders(String userId, OrdersCallback ordersCallback, OrderCountCallback countCallback) {
        if (userListener != null) {
            userListener.remove(); // Remove any existing listener to avoid duplicates
        }

        // Listen to the user's document to get the list of order IDs and count
        userListener = db.collection("users").document(userId).addSnapshotListener((userSnapshot, e) -> {
            if (e != null) {
                ordersCallback.onFailure(e);
                countCallback.onFailure(e);
                return;
            }

            if (userSnapshot != null && userSnapshot.exists()) {
                User user = userSnapshot.toObject(User.class);
                MyUser.getInstance().setUser(user);
                if (user != null && user.getOrders() != null) {
                    ordersCallback.onListCleared(); // Clear the list before processing new data
                    int orderCount = user.getOrders().size();  // Get the total order count
                    countCallback.onOrderCountFetched(orderCount);  // Send the order count to the callback
                    // Listen to changes in the orders collection for the user's orders
                    if(orderCount>0) {
                        listenToOrderChanges(user.getOrders(), ordersCallback);
                    }
                } else {
                    ordersCallback.onFailure(new Exception("User or user orders not found"));
                    countCallback.onFailure(new Exception("User or user orders not found"));
                }
            } else {
                ordersCallback.onFailure(new Exception("DocumentSnapshot is null or doesn't exist"));
                countCallback.onFailure(new Exception("DocumentSnapshot is null or doesn't exist"));
            }
        });
    }

    private static void listenToOrderChanges(List<String> orderIds, OrdersCallback callback) {
        // Split the list into chunks of 10 (the maximum supported by Firestore for whereIn queries)
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < orderIds.size(); i += 10) {
            chunks.add(orderIds.subList(i, Math.min(i + 10, orderIds.size())));
        }

        // Set up a listener for each chunk
        for (List<String> chunk : chunks) {
            ListenerRegistration chunkListener = db.collection("orders").whereIn("orderId", chunk).addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    callback.onFailure(e);
                    return;
                }

                if (snapshots != null) {
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        Order order = dc.getDocument().toObject(Order.class);
                        switch (dc.getType()) {
                            case ADDED:
                                callback.onOrderAdded(order);
                                break;
                            case MODIFIED:
                                callback.onOrderModified(order);
                                break;
                            case REMOVED:
                                callback.onOrderRemoved(dc.getDocument().getId());
                                break;
                        }
                    }
                }
            });

            // Store the listener registration so we can remove it later
            for (String orderId : chunk) {
                orderListeners.put(orderId, chunkListener);
            }
        }
    }

    public static void removeAllListeners() {
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        for (ListenerRegistration listener : orderListeners.values()) {
            listener.remove();
        }
        orderListeners.clear();
    }
}
