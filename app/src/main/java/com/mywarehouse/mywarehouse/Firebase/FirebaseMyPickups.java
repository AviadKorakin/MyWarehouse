package com.mywarehouse.mywarehouse.Firebase;


import com.google.firebase.firestore.ListenerRegistration;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.User;
import com.mywarehouse.mywarehouse.Utilities.MyUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseMyPickups extends FirebaseManager {

    private static ListenerRegistration userListener;
    private static final Map<String, ListenerRegistration> orderListeners = new HashMap<>();

    public interface PickupsCallback {
        void onPickupsFetched(List<Order> pickups);
        void onOrderUpdated(Order order);
        void onOrderRemoved(String orderId);
        void onFailure(Exception e);
    }

    public static void fetchUserPickupsWithListeners(String userId, PickupsCallback callback) {
        if (userListener != null) {
            userListener.remove(); // Remove any existing listener to avoid duplicates
        }

        userListener = db.collection("users").document(userId).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                callback.onFailure(e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                User user = snapshot.toObject(User.class);
                MyUser.getInstance().setUser(user);

                if (user != null && user.getPickups() != null) {
                    setupOrderListeners(user.getPickups(), callback);
                } else {
                    callback.onPickupsFetched(new ArrayList<>());
                }
            } else {
                callback.onFailure(new Exception("User document does not exist"));
            }
        });
    }

    private static void setupOrderListeners(List<String> pickupIds, PickupsCallback callback) {
        List<Order> pickupList = new ArrayList<>();
        if(pickupIds.isEmpty())
        {
            callback.onPickupsFetched(pickupList);
            return;
        }
        for (String pickupId : pickupIds) {
            if (!orderListeners.containsKey(pickupId)) {
                ListenerRegistration orderListener = db.collection("orders").document(pickupId)
                        .addSnapshotListener((snapshot, e) -> {
                            if (e != null) {
                                callback.onFailure(e);
                                return;
                            }

                            if (snapshot != null && snapshot.exists()) {
                                Order order = snapshot.toObject(Order.class);
                                if (order != null) {
                                    if (order.getStatus() == OrderType.IN_PROGRESS) {
                                        callback.onOrderUpdated(order);
                                    }
                                }
                            } else {
                                callback.onOrderRemoved(pickupId);
                                removeOrderListener(pickupId);
                            }
                        });
                orderListeners.put(pickupId, orderListener);
            }

            // Fetch the initial order and add it to the list
            db.collection("orders").document(pickupId).get().addOnCompleteListener(orderTask -> {
                if (orderTask.isSuccessful() && orderTask.getResult() != null) {
                    Order order = orderTask.getResult().toObject(Order.class);
                    if (order != null && order.getStatus() == OrderType.IN_PROGRESS) {
                        pickupList.add(order);
                    }

                    if (pickupList.size() == pickupIds.size()) {
                        callback.onPickupsFetched(pickupList);
                    }
                } else {
                    callback.onFailure(orderTask.getException());
                }
            });
        }
    }

    private static void removeOrderListener(String orderId) {
        if (orderListeners.containsKey(orderId)) {
            orderListeners.get(orderId).remove();
            orderListeners.remove(orderId);
        }
    }

    public static void removeAllListeners() {
        if (userListener != null) {
            userListener.remove();
        }
        for (ListenerRegistration listener : orderListeners.values()) {
            listener.remove();
        }
        orderListeners.clear();
    }
}
