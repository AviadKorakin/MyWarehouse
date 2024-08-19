package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.mywarehouse.mywarehouse.Models.Item;

public class FirebaseInventory extends FirebaseManager {

    private static ListenerRegistration itemListener;

    public interface InventoryCallback {
        void onItemAdded(Item item);
        void onItemModified(Item item);
        void onItemRemoved(String itemId);
        void onError(Exception e);
    }

    public static void listenToItems(InventoryCallback callback) {
        if (itemListener != null) {
            itemListener.remove(); // Remove any existing listener to avoid duplicates
        }

        itemListener = db.collection("items").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                callback.onError(e);
                return;
            }

            if (snapshots != null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    Item item = dc.getDocument().toObject(Item.class);
                    switch (dc.getType()) {
                        case ADDED:
                            callback.onItemAdded(item);
                            break;
                        case MODIFIED:
                            callback.onItemModified(item);
                            break;
                        case REMOVED:
                            callback.onItemRemoved(dc.getDocument().getId());
                            break;
                    }
                }
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
