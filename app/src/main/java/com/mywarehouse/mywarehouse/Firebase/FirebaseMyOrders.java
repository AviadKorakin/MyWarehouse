package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.User;

import java.util.ArrayList;
import java.util.List;

public class FirebaseMyOrders {

    public interface OrdersCallback {
        void onOrdersFetched(List<Order> orders);
        void onFailure(Exception e);
    }

    public static void fetchUserOrders(FirebaseFirestore db, String userId, OrdersCallback callback) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult().toObject(User.class);
                if (user != null && user.getOrders() != null) {
                    fetchOrders(db, user.getOrders(), callback);
                } else {
                    callback.onFailure(new Exception("User or user orders not found"));
                }
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    private static void fetchOrders(FirebaseFirestore db, List<String> orderIds, OrdersCallback callback) {
        List<Order> orders = new ArrayList<>();
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
}
