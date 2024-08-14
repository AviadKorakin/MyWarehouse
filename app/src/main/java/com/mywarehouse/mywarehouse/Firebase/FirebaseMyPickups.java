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
import java.util.concurrent.atomic.AtomicInteger;

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

            // Step 1: Fetch the initial data
            db.collection("users").document(userId).get().addOnCompleteListener(userTask -> {
                if (userTask.isSuccessful() && userTask.getResult() != null) {
                    User user = userTask.getResult().toObject(User.class);
                    MyUser.getInstance().setUser(user);

                    if (user != null && user.getPickups() != null && !user.getPickups().isEmpty()) {
                        List<String> pickupIds = user.getPickups();
                        List<Order> pickupList = new ArrayList<>();
                        AtomicInteger counter= new AtomicInteger();
                        // Fetch all orders and add them to the list
                        for (String pickupId : pickupIds) {
                            db.collection("orders").document(pickupId).get().addOnCompleteListener(orderTask -> {
                                if (orderTask.isSuccessful() && orderTask.getResult() != null) {
                                    Order order = orderTask.getResult().toObject(Order.class);
                                    counter.getAndIncrement();
                                    if (order != null && order.getStatus() == OrderType.IN_PROGRESS) {
                                        pickupList.add(order);
                                    }

                                    // Check if we've processed all orders
                                    if (counter.get() == pickupIds.size()) {
                                        callback.onPickupsFetched(pickupList);

                                        // Step 2: Set up listeners after initial fetch
                                        setupOrderListeners(pickupIds, callback);
                                    }
                                } else {
                                    callback.onFailure(orderTask.getException());
                                }
                            });
                        }
                    } else {
                        callback.onPickupsFetched(new ArrayList<>());
                    }
                } else {
                    callback.onFailure(userTask.getException());
                }
            });
        }

        private static void setupOrderListeners(List<String> pickupIds, PickupsCallback callback) {
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