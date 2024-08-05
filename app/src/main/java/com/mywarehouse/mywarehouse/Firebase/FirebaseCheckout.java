package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mywarehouse.mywarehouse.Enums.LogType;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Models.ItemOrder;
import com.mywarehouse.mywarehouse.Models.MyLog;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FirebaseCheckout {

    public interface CheckoutCallback {
        void onSuccess(String orderId);
        void onFailure(Exception e);
    }

    public interface ItemsCallback {
        void onItemsFetched(List<ItemOrder> items, boolean canOrder, List<DocumentReference> itemsToUpdate);
        void onFailure(Exception e);
    }

    public static void fetchItems(FirebaseFirestore db, List<ItemOrder> itemOrderList, ItemsCallback callback) {
        db.collection("items")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean canOrder = true;
                        List<DocumentReference> itemsToUpdate = new ArrayList<>();
                        List<ItemOrder> items = new ArrayList<>();

                        for (ItemOrder orderItem : itemOrderList) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String documentId = orderItem.getBarcode() + "_" + orderItem.getName();
                                if (document.getId().equals(documentId)) {
                                    ItemOrder item = document.toObject(ItemOrder.class);
                                    if (item.getTotalQuantity() - item.getRequestedAmount() < orderItem.getSelectedQuantity()) {
                                        orderItem.setSelectedQuantity(item.getTotalQuantity() - item.getRequestedAmount());
                                        items.add(orderItem);
                                        canOrder = false;
                                    } else {
                                        item.setRequestedAmount(orderItem.getSelectedQuantity());
                                        itemsToUpdate.add(document.getReference());
                                        items.add(orderItem);
                                    }
                                }
                            }
                        }

                        callback.onItemsFetched(items, canOrder, itemsToUpdate);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public static void placeOrder(FirebaseFirestore db, List<DocumentReference> itemsToUpdate, List<ItemOrder> itemOrderList, String createdBy, CheckoutCallback callback) {
        List<PickupItem> pickupItems = new ArrayList<>();
        for (ItemOrder itemOrder : itemOrderList) {
            PickupItem pickupItem = new PickupItem(itemOrder.getName(), itemOrder.getBarcode(), itemOrder.getSelectedQuantity());
            pickupItems.add(pickupItem);
        }

        String orderId = UUID.randomUUID().toString();
        Order order = new Order(pickupItems, createdBy, orderId, new Date(), OrderType.REGISTERED, null,null);

        db.collection("orders").document(orderId)
                .set(order)
                .addOnCompleteListener(orderTask -> {
                    if (orderTask.isSuccessful()) {
                        for (int i = 0; i < itemsToUpdate.size(); i++) {
                            DocumentReference itemRef = itemsToUpdate.get(i);
                            ItemOrder item = itemOrderList.get(i);
                            itemRef.update("requestedAmount", item.getRequestedAmount() + item.getSelectedQuantity());
                        }

                        String logId = "order_creation_" + UUID.randomUUID().toString();
                        String logNotes = createLogNotes(pickupItems);
                        MyLog log = new MyLog("Order #" + orderId + " is ready for pickup", new Date(), logNotes, createdBy, LogType.ORDER_CREATION);

                        db.collection("logs").document(logId).set(log)
                                .addOnSuccessListener(documentReference -> callback.onSuccess(orderId))
                                .addOnFailureListener(callback::onFailure);
                    } else {
                        callback.onFailure(orderTask.getException());
                    }
                });
    }

    public static void saveOrderForUser(FirebaseFirestore db, String userDocumentId, String orderId, CheckoutCallback callback) {
        if (userDocumentId == null || userDocumentId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("User not logged in or user document ID not set"));
            return;
        }

        DocumentReference userRef = db.collection("users").document(userDocumentId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    List<String> userOrders = user.getOrders();
                    if (userOrders == null) {
                        userOrders = new ArrayList<>();
                    }
                    userOrders.add(orderId);

                    userRef.update("orders", userOrders)
                            .addOnSuccessListener(aVoid -> callback.onSuccess(orderId))
                            .addOnFailureListener(callback::onFailure);
                }
            } else {
                callback.onFailure(new Exception("User document not found"));
            }
        }).addOnFailureListener(callback::onFailure);
    }

    private static String createLogNotes(List<PickupItem> pickupItems) {
        StringBuilder notesBuilder = new StringBuilder("Items:\n");
        for (PickupItem pickupItem : pickupItems) {
            notesBuilder.append("Name: ").append(pickupItem.getName())
                    .append(" - Barcode: ").append(pickupItem.getBarcode())
                    .append(" - Quantity: ").append(pickupItem.getQuantity())
                    .append("\n");
        }
        return notesBuilder.toString();
    }
}
