package com.mywarehouse.mywarehouse.Firebase;


import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.Warehouse;

import java.util.ArrayList;
import java.util.List;

public class FirebaseWarehouseMap extends FirebaseManager{
    public interface DataCallback {
        void onCallback(List<Item> items, List<Warehouse> warehouses);
        void onFailure(Exception e);
    }

    public static void loadData(DataCallback callback) {
        db.collection("items").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Item> itemList = new ArrayList<>();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Item item = document.toObject(Item.class);
                itemList.add(item);
            }
            fetchWarehouses(new WarehousesCallback() {
                @Override
                public void onCallback(List<Warehouse> warehouseList) {
                    if (warehouseList != null) {
                        callback.onCallback(itemList, warehouseList);
                    } else {
                        callback.onFailure(new Exception("Failed to fetch warehouses"));
                    }
                }
            });
        }).addOnFailureListener(e -> callback.onFailure(e));
    }

    public static void fetchWarehouses(WarehousesCallback callback) {
        db.collection("warehouses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Warehouse> warehouseList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Warehouse warehouse = document.toObject(Warehouse.class);
                        warehouseList.add(warehouse);
                    }
                    callback.onCallback(warehouseList);
                })
                .addOnFailureListener(e -> callback.onCallback(null));
    }

    public interface WarehousesCallback {
        void onCallback(List<Warehouse> warehouseList);
    }
}
