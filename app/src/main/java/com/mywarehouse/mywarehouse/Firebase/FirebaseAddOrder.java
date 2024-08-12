package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.mywarehouse.mywarehouse.Models.ItemOrder;

import java.util.ArrayList;
import java.util.List;

public class FirebaseAddOrder extends FirebaseManager {

    public interface ItemsCallback {
        void onItemsFetched(List<ItemOrder> items);
        void onFailure(Exception e);
    }

    public static void listenToItems(ItemsCallback callback) {
        db.collection("items").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    callback.onFailure(e);
                    return;
                }

                if (queryDocumentSnapshots != null) {
                    List<ItemOrder> itemList = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        ItemOrder itemOrder = document.toObject(ItemOrder.class);
                        if (itemOrder.getTotalQuantity() - itemOrder.getRequestedAmount() > 0 && itemOrder.isActive()) {
                            itemList.add(itemOrder);
                        }
                    }
                    callback.onItemsFetched(itemList);
                } else {
                    callback.onFailure(new Exception("QuerySnapshot is null"));
                }
            }
        });
    }
}
