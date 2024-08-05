package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mywarehouse.mywarehouse.Models.ItemOrder;

import java.util.ArrayList;
import java.util.List;

public class FirebaseAddOrder {

    public interface ItemsCallback {
        void onItemsFetched(List<ItemOrder> items);
        void onFailure(Exception e);
    }

    public static void fetchItems(FirebaseFirestore db, ItemsCallback callback) {
        db.collection("items")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<ItemOrder> itemList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ItemOrder itemOrder = document.toObject(ItemOrder.class);
                            if (itemOrder.getTotalQuantity() - itemOrder.getRequestedAmount() > 0 && itemOrder.isActive()) {
                                itemList.add(itemOrder);
                            }
                        }
                        callback.onItemsFetched(itemList);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }
}
