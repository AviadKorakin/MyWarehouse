package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mywarehouse.mywarehouse.Models.Item;

import java.util.ArrayList;
import java.util.List;

public class FirebaseInventory extends FirebaseManager {

    public interface InventoryCallback {
        void onCallback(List<Item> itemList);
        void onError(Exception e);
    }

    // Real-time listener for changes in the "items" collection
    public static void listenToItems(InventoryCallback callback) {
        db.collection("items").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    callback.onError(e);
                    return;
                }
                if (snapshots != null && !snapshots.isEmpty()) {
                    List<Item> itemList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshots) {
                        Item item = document.toObject(Item.class);
                        itemList.add(item);
                    }
                    callback.onCallback(itemList);
                }
            }
        });
    }
}
