package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.FirebaseFirestore;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.User;

import java.util.ArrayList;
import java.util.List;

public class FirebaseMyPickups {

    public interface PickupsCallback {
        void onPickupsFetched(List<Order> pickups);
        void onFailure(Exception e);
    }

    public static void fetchUserPickups(FirebaseFirestore db, String userId, PickupsCallback callback) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult().toObject(User.class);
                if (user != null && user.getPickups() != null) {
                    List<String> pickupIds = user.getPickups();
                    List<Order> pickupList = new ArrayList<>();
                    List<String> inProgressPickupIds = new ArrayList<>();

                    for (String pickupId : pickupIds) {
                        db.collection("orders").document(pickupId).get().addOnCompleteListener(orderTask -> {
                            if (orderTask.isSuccessful() && orderTask.getResult() != null) {
                                Order order = orderTask.getResult().toObject(Order.class);
                                if (order != null) {
                                    if (order.getStatus() == OrderType.IN_PROGRESS) {
                                        pickupList.add(order);
                                    }
                                    inProgressPickupIds.add(pickupId); // Track processed pickups
                                }

                                // Call the callback when all pickups have been processed
                                if (inProgressPickupIds.size() == pickupIds.size()) {
                                    callback.onPickupsFetched(pickupList);
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
                callback.onFailure(task.getException());
            }
        });
    }
}
