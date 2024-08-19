package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentChange;
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
        void onPickupAdded(Order pickup);
        void onPickupModified(Order pickup);
        void onPickupRemoved(String pickupId);
        void onListCleared(); // New method to clear the list
        void onFailure(Exception e);
    }

    public interface PickupCountCallback {
        void onPickupCountFetched(int count);
        void onFailure(Exception e);
    }

    public static void listenToUserPickups(String userId, PickupsCallback pickupsCallback, PickupCountCallback countCallback) {
        if (userListener != null) {
            userListener.remove(); // Remove any existing listener to avoid duplicates
        }

        // Listen to the user's document to get the list of pickup IDs and count
        userListener = db.collection("users").document(userId).addSnapshotListener((userSnapshot, e) -> {
            if (e != null) {
                pickupsCallback.onFailure(e);
                countCallback.onFailure(e);
                return;
            }

            if (userSnapshot != null && userSnapshot.exists()) {
                User user = userSnapshot.toObject(User.class);
                MyUser.getInstance().setUser(user);
                if (user != null && user.getPickups() != null) {
                    pickupsCallback.onListCleared(); // Clear the list before processing new data
                    listenToPickupChanges(user.getPickups(), pickupsCallback, countCallback);
                } else {
                    pickupsCallback.onFailure(new Exception("User or user pickups not found"));
                    countCallback.onFailure(new Exception("User or user pickups not found"));
                }
            } else {
                pickupsCallback.onFailure(new Exception("DocumentSnapshot is null or doesn't exist"));
                countCallback.onFailure(new Exception("DocumentSnapshot is null or doesn't exist"));
            }
        });
    }

    private static void listenToPickupChanges(List<String> pickupIds, PickupsCallback pickupsCallback, PickupCountCallback countCallback) {
        AtomicInteger inProgressCount = new AtomicInteger(0);
        if (pickupIds==null || pickupIds.isEmpty() ) {
            countCallback.onPickupCountFetched(0);
            return;
        }
        // Split the list into chunks of 10 (the maximum supported by Firestore for whereIn queries)
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < pickupIds.size(); i += 10) {
            chunks.add(pickupIds.subList(i, Math.min(i + 10, pickupIds.size())));
        }

        // AtomicInteger to track the number of processed chunks
        AtomicInteger processedChunks = new AtomicInteger(0);

        // Set up a listener for each chunk
        for (List<String> chunk : chunks) {
            ListenerRegistration chunkListener = db.collection("orders")
                    .whereIn("orderId", chunk)
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            pickupsCallback.onFailure(e);
                            return;
                        }

                        if (snapshots != null) {
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                Order order = dc.getDocument().toObject(Order.class);
                                switch (dc.getType()) {
                                    case ADDED:
                                        if (order.getStatus() == OrderType.IN_PROGRESS) {
                                            pickupsCallback.onPickupAdded(order);
                                            inProgressCount.incrementAndGet();
                                        }
                                        break;
                                    case MODIFIED:
                                        if (order.getStatus() == OrderType.IN_PROGRESS) {
                                            pickupsCallback.onPickupModified(order);
                                        } else {
                                            pickupsCallback.onPickupRemoved(order.getOrderId());
                                            inProgressCount.decrementAndGet();
                                        }
                                        break;
                                    case REMOVED:
                                        pickupsCallback.onPickupRemoved(dc.getDocument().getId());
                                        inProgressCount.decrementAndGet();
                                        break;
                                }
                            }

                            // Once all chunks are processed, send the in-progress count
                            if (processedChunks.incrementAndGet() == chunks.size()) {
                                countCallback.onPickupCountFetched(inProgressCount.get());
                            }
                        }
                    });

            // Store the listener registration so we can remove it later
            for (String pickupId : chunk) {
                orderListeners.put(pickupId, chunkListener);
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
