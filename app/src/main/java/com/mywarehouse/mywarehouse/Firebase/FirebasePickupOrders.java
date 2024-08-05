package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.Warehouse;

import java.util.ArrayList;
import java.util.List;

public class FirebasePickupOrders {

    public interface FetchCallback<T> {
        void onSuccess(List<T> items);
        void onFailure(Exception e);
    }

    public static void fetchWarehouses(FirebaseFirestore db, FetchCallback<Warehouse> callback) {
        db.collection("warehouses").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Warehouse> warehouseList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Warehouse warehouse = document.toObject(Warehouse.class);
                    warehouseList.add(warehouse);
                }
                callback.onSuccess(warehouseList);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public static void fetchOrders(FirebaseFirestore db, FetchCallback<Order> callback) {
        db.collection("orders").whereEqualTo("status", OrderType.REGISTERED.name()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Order> orderList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Order order = document.toObject(Order.class);
                    orderList.add(order);
                }
                callback.onSuccess(orderList);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }
}
