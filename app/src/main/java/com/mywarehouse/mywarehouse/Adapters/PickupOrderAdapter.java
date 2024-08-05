package com.mywarehouse.mywarehouse.Adapters;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImages;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Firebase.FirebaseForAdapters;
import com.mywarehouse.mywarehouse.Utilities.MyUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class PickupOrderAdapter extends RecyclerView.Adapter<PickupOrderAdapter.PickupOrderViewHolder> {

    private Context context;
    private List<Order> orderList;
    private String selectedWarehouse;
    private Map<String, List<PickupItemWithImages>> orderPickupItemsMap;
    private Map<String, Boolean> orderFetchedMap;
    private Map<String, Boolean> orderAvailabilityMap;
    private Map<String, List<PickupItem>> orderRequestedItemsMap;
    private Map<String, List<Item>> orderItemsMap;
    private Map<String, Boolean> visibilityMap;

    public PickupOrderAdapter(Context context, List<Order> orderList, String selectedWarehouse) {
        this.context = context;
        this.orderList = orderList;
        this.selectedWarehouse = selectedWarehouse;
        this.orderPickupItemsMap = new HashMap<>();
        this.orderFetchedMap = new HashMap<>();
        this.orderAvailabilityMap = new HashMap<>();
        this.orderRequestedItemsMap = new HashMap<>();
        this.orderItemsMap = new HashMap<>();
        this.visibilityMap = new HashMap<>();
    }

    public void setSelectedWarehouse(String selectedWarehouse) {
        this.selectedWarehouse = selectedWarehouse;
        if (orderList == null || orderList.isEmpty()) return;
        if (!selectedWarehouse.equals("NONE")) {
            checkOrderAvailability();
        } else {
            for (Order order : orderList) {
                visibilityMap.put(order.getOrderId(), false);
            }
            notifyDataSetChanged();
        }
    }

    private void checkOrderAvailability() {
        for (Order order : orderList) {
            List<PickupItem> requestedItemsToMove = Collections.synchronizedList(new ArrayList<>());
            AtomicBoolean allItemsAvailable = new AtomicBoolean(true);

            List<PickupItemWithImages> pickupItemsWithImagesList = orderPickupItemsMap.get(order.getOrderId());
            List<Item> itemsList = orderItemsMap.get(order.getOrderId());

            if (pickupItemsWithImagesList == null || itemsList == null) continue;

            synchronized (pickupItemsWithImagesList) {
                for (int i = 0; i < pickupItemsWithImagesList.size(); i++) {
                    PickupItemWithImages pickupItemWithImages = pickupItemsWithImagesList.get(i);
                    Item item = itemsList.get(i);
                    PickupItem pickupItem = pickupItemWithImages.getPickupItem();
                    int availableQuantity = 0;
                    for (ItemWarehouse itemWarehouse : item.getItemWarehouses()) {
                        if (itemWarehouse.getWarehouseName().equals(selectedWarehouse)) {
                            availableQuantity += itemWarehouse.getQuantity();
                        }
                    }
                    if (availableQuantity < pickupItem.getQuantity()) {
                        allItemsAvailable.set(false);
                        requestedItemsToMove.add(pickupItem);
                    }
                }
            }

            if (allItemsAvailable.get()) {
                order.setStatus(OrderType.REGISTERED);
                orderAvailabilityMap.put(order.getOrderId(), true);
            } else {
                order.setStatus(OrderType.TRANSACTIONS_NEEDED);
                orderAvailabilityMap.put(order.getOrderId(), false);
                orderRequestedItemsMap.put(order.getOrderId(), requestedItemsToMove);
            }
            visibilityMap.put(order.getOrderId(), true);
            notifyItemChanged(orderList.indexOf(order));
        }
        sortOrders();
    }

    private void sortOrders() {
        Collections.sort(orderList, (o1, o2) -> {
            if (o1.getStatus() == OrderType.REGISTERED && o2.getStatus() != OrderType.REGISTERED) {
                return -1;
            } else if (o1.getStatus() != OrderType.REGISTERED && o2.getStatus() == OrderType.REGISTERED) {
                return 1;
            } else {
                return o1.getOrderDate().compareTo(o2.getOrderDate());
            }
        });
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PickupOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_order_with_request, parent, false);
        return new PickupOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PickupOrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        if (visibilityMap.getOrDefault(order.getOrderId(), false) && !selectedWarehouse.equals("NONE")) {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.orderId.setText(order.getOrderId());
            holder.orderDate.setText(order.getOrderDate().toString());
            holder.orderStatus.setText(order.getStatus().toString());

            if (order.getStatus() == OrderType.REGISTERED) {
                holder.itemView.setBackgroundColor(Color.parseColor("#228B22")); // Forest green
            } else if (order.getStatus() == OrderType.TRANSACTIONS_NEEDED) {
                holder.itemView.setBackgroundColor(Color.parseColor("#B22222")); // Firebrick red
            } else {
                holder.itemView.setBackgroundColor(Color.WHITE);
            }
        } else {
            holder.itemView.setVisibility(View.INVISIBLE);
        }

        if (!orderFetchedMap.getOrDefault(order.getOrderId(), false)) {
            List<PickupItemWithImages> pickupItemsWithImagesList = Collections.synchronizedList(new ArrayList<>());
            List<Item> itemsList = Collections.synchronizedList(new ArrayList<>());
            orderPickupItemsMap.put(order.getOrderId(), pickupItemsWithImagesList);
            orderItemsMap.put(order.getOrderId(), itemsList);

            FirebaseForAdapters.fetchPickupItemsWithImages(order.getPickupItems(), new FirebaseForAdapters.PickupItemsCallback() {
                @Override
                public void onCallback(List<PickupItemWithImages> pickupItemsWithImages) {
                    pickupItemsWithImagesList.addAll(pickupItemsWithImages);
                    for (PickupItemWithImages pickupItemWithImages : pickupItemsWithImages) {
                        FirebaseForAdapters.fetchItem(pickupItemWithImages.getPickupItem().getBarcode() + "_" + pickupItemWithImages.getPickupItem().getName(), new FirebaseForAdapters.ItemCallback() {
                            @Override
                            public void onCallback(Item item) {
                                itemsList.add(item);
                                if (itemsList.size() == pickupItemsWithImages.size()) {
                                    orderFetchedMap.put(order.getOrderId(), true);
                                    setupRecyclerView(holder.recyclerViewOrderItems, order.getOrderId());
                                }
                            }
                        });
                    }
                }
            });
        } else {
            setupRecyclerView(holder.recyclerViewOrderItems, order.getOrderId());
        }

        holder.itemView.setOnClickListener(v -> {
            if (holder.isExpanded) {
                collapse(holder.recyclerViewOrderItems);
                holder.isExpanded = false;
            } else {
                expand(holder.recyclerViewOrderItems);
                holder.isExpanded = true;
            }
        });

        holder.requestButton.setOnClickListener(v -> handleRequestButton(order));
    }

    private void setupRecyclerView(RecyclerView recyclerView, String orderId) {
        List<PickupItemWithImages> pickupItemsWithImagesList = orderPickupItemsMap.get(orderId);
        PickupItemAdapter pickupItemAdapter = new PickupItemAdapter(context, pickupItemsWithImagesList);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(pickupItemAdapter);
    }

    private void expand(final RecyclerView recyclerView) {
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.measure(View.MeasureSpec.makeMeasureSpec(recyclerView.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        final int targetHeight = recyclerView.getMeasuredHeight();

        ValueAnimator animator = slideAnimator(0, targetHeight, recyclerView);
        animator.start();
    }

    private void collapse(final RecyclerView recyclerView) {
        final int initialHeight = recyclerView.getHeight();

        ValueAnimator animator = slideAnimator(initialHeight, 0, recyclerView);
        animator.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                recyclerView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationStart(android.animation.Animator animation) {
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
            }

            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {
            }
        });
        animator.start();
    }

    private ValueAnimator slideAnimator(int start, int end, View view) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.addUpdateListener(valueAnimator -> {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = (int) valueAnimator.getAnimatedValue();
            view.setLayoutParams(layoutParams);
        });
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        return animator;
    }

    private void handleRequestButton(Order order) {
        if (selectedWarehouse.equals("NONE")) return;

        FirebaseForAdapters.fetchOrder(order.getOrderId(), new FirebaseForAdapters.OrderCallback() {
            @Override
            public void onCallback(Order latestOrder) {
                if (latestOrder != null && latestOrder.getStatus() == OrderType.REGISTERED) {
                    boolean allItemsAvailable = orderAvailabilityMap.getOrDefault(order.getOrderId(), false);
                    List<PickupItem> requestedItemsToMove = orderRequestedItemsMap.getOrDefault(order.getOrderId(), new ArrayList<>());
                    processRequest(order, allItemsAvailable, requestedItemsToMove);
                } else {
                    Toast.makeText(context, "Order status has changed. Please refresh.", Toast.LENGTH_SHORT).show();
                    visibilityMap.put(order.getOrderId(), false);
                    notifyItemChanged(orderList.indexOf(order));
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "Error checking order status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processRequest(Order order, boolean allItemsAvailable, List<PickupItem> requestedItemsToMove) {
        String userId = MyUser.getInstance().getDocumentId();
        FirebaseForAdapters.addUserPickup(userId, order.getOrderId(), new FirebaseForAdapters.FirestoreCallback() {
            @Override
            public void onSuccess() {
                if (allItemsAvailable) {
                    order.setStatus(OrderType.IN_PROGRESS);
                    order.setSelectedWarehouse(selectedWarehouse);
                    FirebaseForAdapters.updateOrder(order, new FirebaseForAdapters.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(context, "Order added to your pickups", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(context, "Error updating order status.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    order.setStatus(OrderType.TRANSACTIONS_NEEDED);
                    order.setSelectedWarehouse(selectedWarehouse);
                    FirebaseForAdapters.updateOrder(order, new FirebaseForAdapters.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            TransactionRequest transactionRequest = new TransactionRequest(UUID.randomUUID().toString(), selectedWarehouse, order.getOrderId(), MyUser.getInstance().getDocumentId(), requestedItemsToMove);
                            FirebaseForAdapters.createTransactionRequest(transactionRequest, new FirebaseForAdapters.FirestoreCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(context, "Items are not available in this warehouse. Request sent.", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(context, "Error creating transaction request.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(context, "Error updating order status.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                orderList.remove(order);
                notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "Error adding pickup to user.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class PickupOrderViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView orderId, orderDate, orderStatus;
        RecyclerView recyclerViewOrderItems;
        boolean isExpanded = false;
        androidx.appcompat.widget.AppCompatImageButton requestButton;

        public PickupOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id);
            orderDate = itemView.findViewById(R.id.order_date);
            orderStatus = itemView.findViewById(R.id.order_status);
            recyclerViewOrderItems = itemView.findViewById(R.id.recycler_view_order_items);
            recyclerViewOrderItems.setVisibility(View.GONE);
            requestButton = itemView.findViewById(R.id.button_request);
        }
    }
}
