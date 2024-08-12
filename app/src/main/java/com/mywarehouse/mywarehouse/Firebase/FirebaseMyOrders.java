package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.User;
import com.mywarehouse.mywarehouse.Utilities.MyUser;

import java.util.ArrayList;
import java.util.List;

public class FirebaseMyOrders extends FirebaseManager {

    public interface OrdersCallback {
        void onOrdersFetched(List<Order> orders);
        void onFailure(Exception e);
    }

    public interface OrderUpdateCallback {
        void onOrderUpdated(Order updatedOrder);
        void onFailure(Exception e);
    }

    public static void listenToUserOrders(String userId, OrdersCallback callback) {
        db.collection("users").document(userId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    callback.onFailure(e);
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    MyUser.getInstance().setUser(user);
                    if (user != null && user.getOrders() != null) {
                        fetchOrders(user.getOrders(), callback);
                    } else {
                        callback.onFailure(new Exception("User or user orders not found"));
                    }
                } else {
                    callback.onFailure(new Exception("DocumentSnapshot is null or doesn't exist"));
                }
            }
        });
    }

    private static void fetchOrders(List<String> orderIds, OrdersCallback callback) {
        List<Order> orders = new ArrayList<>();
        if(orderIds.isEmpty())
        {
            callback.onOrdersFetched(orders);
            return;
        }
        for (String orderId : orderIds) {
            db.collection("orders").document(orderId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Order order = task.getResult().toObject(Order.class);
                    if (order != null) {
                        orders.add(order);
                    }
                }
                // Notify callback after fetching all orders
                if (orders.size() == orderIds.size()) {
                    callback.onOrdersFetched(orders);
                }
            }).addOnFailureListener(callback::onFailure);
        }
    }

    public static void listenToOrderChanges(String orderId, OrderUpdateCallback callback) {
        db.collection("orders").document(orderId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    callback.onFailure(e);
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Order updatedOrder = documentSnapshot.toObject(Order.class);
                    if (updatedOrder != null) {
                        callback.onOrderUpdated(updatedOrder);
                    } else {
                        callback.onFailure(new Exception("Order data is null"));
                    }
                } else {
                    callback.onFailure(new Exception("DocumentSnapshot is null or doesn't exist"));
                }
            }
        });
    }
}
