package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mywarehouse.mywarehouse.Enums.LogType;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.Models.MyLog;
import com.mywarehouse.mywarehouse.Models.Warehouse;
import com.mywarehouse.mywarehouse.Models.MyLatLng;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FirebaseUpdateItemBundled {

    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface ItemCallback {
        void onCallback(Item item);
    }

    public interface WarehousesCallback {
        void onCallback(List<Warehouse> warehouses);
    }

    public static void getDocumentId(Item item, ItemCallback callback) {
        String documentId = item.getBarcode() + "_" + item.getName();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("items").document(documentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Item currentItem = documentSnapshot.toObject(Item.class);
                        callback.onCallback(currentItem);
                    } else {
                        callback.onCallback(null);
                    }
                })
                .addOnFailureListener(e -> callback.onCallback(null));
    }

    public static void fetchWarehouses(FirebaseFirestore db, WarehousesCallback callback) {
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

    public static void saveItem(String documentId, Item item, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("items").document(documentId).set(item)
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public static void saveLog(Item newItem, Item oldItem, String invokedBy, FirestoreCallback callback) {
        StringBuilder notes = new StringBuilder("Item " + newItem.getName() + " has been updated. Changes:\n");

        if (!newItem.getDescription().equals(oldItem.getDescription())) {
            notes.append("Description changed from '").append(oldItem.getDescription()).append("' to '").append(newItem.getDescription()).append("'\n");
        }
        if (newItem.getTotalQuantity() != oldItem.getTotalQuantity()) {
            notes.append("Quantity changed from ").append(oldItem.getTotalQuantity()).append(" to ").append(newItem.getTotalQuantity()).append("\n");
        }
        if (!newItem.getSupplier().equals(oldItem.getSupplier())) {
            notes.append("Supplier changed from '").append(oldItem.getSupplier()).append("' to '").append(newItem.getSupplier()).append("'\n");
        }
        boolean flag = true;
        int countNew = newItem.getItemWarehouses().size();
        int countOld = oldItem.getItemWarehouses().size();
        if (countNew != countOld) {
            flag = false;
        } else {
            for (ItemWarehouse itemWarehouse : newItem.getItemWarehouses()) {
                for (ItemWarehouse itemWarehouseOld : oldItem.getItemWarehouses()) {
                    if (itemWarehouseOld.getWarehouseName().equals(itemWarehouse.getWarehouseName())) {
                        countOld--;
                        countNew--;
                        break;
                    }
                }
            }
            if (countNew != 0 || countOld != 0) flag = false;
        }

        if (!flag) {
            if (newItem.getItemWarehouses().isEmpty())
                notes.append("Warehouses: no present location, OUT OF STOCK");
            else {
                notes.append("Warehouses changed").append(" to ").append("\n");
                for (ItemWarehouse itemWarehouse : newItem.getItemWarehouses()) {
                    notes.append("- Warehouse: ").append(itemWarehouse.getWarehouseName()).append("- Qty: ").append(itemWarehouse.getQuantity()).append("\n");
                }
            }
        }
        if (!newItem.getImageUrls().equals(oldItem.getImageUrls())) {
            notes.append("Images updated.\n");
        }

        MyLog myLog = new MyLog("Item modification", new Date(), notes.toString(), invokedBy, LogType.ITEM_MODIFICATION);
        String logId = "item_modification_" + newItem.getBarcode() + "_" + newItem.getName() + "_" + UUID.randomUUID().toString();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("logs").document(logId).set(myLog)
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }


    public static void saveOutOfStockLog(String itemName, String barcode, Date date, String invokedBy, FirestoreCallback callback) {
        String notes = "Item " + itemName + " is out of stock because it was updated during a warehouse inventory check or it has run out due to orders.";
        MyLog myLog = new MyLog("Item out of stock", date, notes, invokedBy, LogType.OUT_OF_STOCK);
        String logId = "item_out_of_stock_" + barcode + "_" + itemName + "_" + UUID.randomUUID().toString();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("logs").document(logId).set(myLog)
                .addOnSuccessListener(documentReference -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
