package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImages;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImagesAndLocations;
import com.mywarehouse.mywarehouse.Models.Warehouse;

import java.util.ArrayList;
import java.util.List;

public class FirebaseShowPickUp {

    public interface ItemCallback {
        void onCallback(Item item);
    }

    public interface PickupItemsCallback {
        void onCallback(List<PickupItemWithImagesAndLocations> pickupItemsWithImagesAndLocations);
    }

    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface WarehouseCallback {
        void onCallback(Warehouse warehouse);
    }

    public static void fetchItem(String documentId, ItemCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("items").document(documentId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Item item = task.getResult().toObject(Item.class);
                callback.onCallback(item);
            }
        });
    }

    public static void fetchPickupItemsWithImagesAndLocations(List<PickupItem> pickupItems, String selectedWarehouse, PickupItemsCallback callback) {
        List<PickupItemWithImagesAndLocations> pickupItemsWithImagesAndLocationsList = new ArrayList<>();

        for (PickupItem pickupItem : pickupItems) {
            String documentId = pickupItem.getBarcode() + "_" + pickupItem.getName();
            fetchItem(documentId, item -> {
                List<ItemWarehouse> relevantLocations = new ArrayList<>();
                for (ItemWarehouse itemWarehouse : item.getItemWarehouses()) {
                    if (itemWarehouse.getWarehouseName().equals(selectedWarehouse)) {
                        relevantLocations.add(itemWarehouse);
                    }
                }
                PickupItemWithImages pickupItemWithImages = new PickupItemWithImages(pickupItem, item.getImageUrls());
                PickupItemWithImagesAndLocations pickupItemWithImagesAndLocations = new PickupItemWithImagesAndLocations(pickupItemWithImages, relevantLocations);
                pickupItemsWithImagesAndLocationsList.add(pickupItemWithImagesAndLocations);

                if (pickupItemsWithImagesAndLocationsList.size() == pickupItems.size()) {
                    callback.onCallback(pickupItemsWithImagesAndLocationsList);
                }
            });
        }
    }

    public static void fetchWarehouse(String warehouseName, WarehouseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("warehouses").document(warehouseName).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Warehouse warehouse = task.getResult().toObject(Warehouse.class);
                callback.onCallback(warehouse);
            }
        });
    }

    public static void updateOrder(Order order, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders").document(order.getOrderId())
                .set(order)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void removeUserPickup(String userId, String orderId, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).update("pickups", FieldValue.arrayRemove(orderId))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void updateItem(Item item, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("items").document(item.getBarcode() + "_" + item.getName())
                .set(item)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
