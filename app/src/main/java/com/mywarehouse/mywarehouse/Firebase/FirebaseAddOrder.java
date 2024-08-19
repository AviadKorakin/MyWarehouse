package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.mywarehouse.mywarehouse.Models.ItemOrder;

import java.util.concurrent.atomic.AtomicInteger;

public class FirebaseAddOrder extends FirebaseManager {

    private static ListenerRegistration itemListener;

    public interface ItemsCallback {
        void onItemAdded(ItemOrder item);
        void onItemModified(ItemOrder item);
        void onItemRemoved(String itemId);
        void onItemCountFetched(int count); // Callback for the total count of valid items
        void onFailure(Exception e);
    }

    public static void listenToItems(ItemsCallback callback) {
        if (itemListener != null) {
            itemListener.remove(); // Remove any existing listener to avoid duplicates
        }

        AtomicInteger validItemCount = new AtomicInteger(0);

        itemListener = db.collection("items").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                callback.onFailure(e);
                return;
            }

            if (snapshots != null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    ItemOrder itemOrder = dc.getDocument().toObject(ItemOrder.class);
                    switch (dc.getType()) {
                        case ADDED:
                            if (itemOrder.getTotalQuantity() - itemOrder.getRequestedAmount() > 0 && itemOrder.isActive()) {
                                callback.onItemAdded(itemOrder);
                                validItemCount.incrementAndGet();
                            }
                            break;
                        case MODIFIED:
                            if (itemOrder.getTotalQuantity() - itemOrder.getRequestedAmount() > 0 && itemOrder.isActive()) {
                                callback.onItemModified(itemOrder);
                            } else {
                                callback.onItemRemoved(dc.getDocument().getId());
                                validItemCount.decrementAndGet();
                            }
                            break;
                        case REMOVED:
                            callback.onItemRemoved(dc.getDocument().getId());
                            validItemCount.decrementAndGet();
                            break;
                    }
                }
                // Notify the total count after processing the snapshot
                callback.onItemCountFetched(validItemCount.get());
            }
        });
    }

    public static void removeAllListeners() {
        if (itemListener != null) {
            itemListener.remove();
            itemListener = null;
        }
    }
}
