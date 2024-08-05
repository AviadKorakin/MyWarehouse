package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mywarehouse.mywarehouse.Models.Item;

import java.util.ArrayList;
import java.util.List;

public class FirebaseInventory {

    public interface InventoryCallback {
        void onCallback(List<Item> itemList);
        void onError(Exception e);
    }

    public static void fetchItems(FirebaseFirestore db, InventoryCallback callback) {
        db.collection("items")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Item> itemList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Item item = document.toObject(Item.class);
                            itemList.add(item);
                        }
                        callback.onCallback(itemList);
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }
}
