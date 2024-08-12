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
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImages;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Firebase.FirebaseForAdapters;
import com.mywarehouse.mywarehouse.Utilities.MyUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PickupOrderAdapter extends RecyclerView.Adapter<PickupOrderAdapter.PickupOrderViewHolder> {

    private Context context;
    private List<Order> orderList;
    private Map<String, List<PickupItemWithImages>> orderPickupItemsMap;
    private Map<String, List<Item>> orderItemsMap;
    private Map<String, Boolean> visibilityMap;
    private Map<String, List<PickupItem>> orderRequestedItemsMap;
    private String selectedWarehouse;

    public PickupOrderAdapter(Context context, List<Order> orderList, Map<String, List<PickupItemWithImages>> orderPickupItemsMap, Map<String, List<Item>> orderItemsMap, String selectedWarehouse) {
        this.context = context;
        this.orderList = orderList;
        this.orderPickupItemsMap = orderPickupItemsMap;
        this.orderItemsMap = orderItemsMap;
        this.visibilityMap = new HashMap<>();
        this.orderRequestedItemsMap = new HashMap<>();
        this.selectedWarehouse = selectedWarehouse;
    }

    public void setSelectedWarehouse(String selectedWarehouse) {
        this.selectedWarehouse = selectedWarehouse;
        if (orderList == null || orderList.isEmpty()) return;
        checkOrderAvailability();
    }

    public void checkOrderAvailability() {
        for (Order order : orderList) {
            if ("NONE".equals(selectedWarehouse)) {
                visibilityMap.put(order.getOrderId(), false);
                continue;
            }

            boolean allItemsAvailable = true;
            List<PickupItemWithImages> pickupItemsWithImagesList = orderPickupItemsMap.get(order.getOrderId());
            List<Item> itemsList = orderItemsMap.get(order.getOrderId());
            List<PickupItem> requestedItemsToMove = new ArrayList<>();

            if (itemsList == null || pickupItemsWithImagesList == null) continue;

            for (int i = 0; i < pickupItemsWithImagesList.size(); i++) {
                PickupItemWithImages pickupItemWithImages = pickupItemsWithImagesList.get(i);
                Item item = itemsList.get(i);

                int availableQuantity = item.getItemWarehouses().stream()
                        .filter(wh -> wh.getWarehouseName().equals(selectedWarehouse))
                        .mapToInt(ItemWarehouse::getQuantity)
                        .sum();

                if (availableQuantity < pickupItemWithImages.getPickupItem().getQuantity()) {
                    allItemsAvailable = false;
                    requestedItemsToMove.add(pickupItemWithImages.getPickupItem());
                }
            }

            if (allItemsAvailable) {
                order.setStatus(OrderType.REGISTERED);
                orderRequestedItemsMap.remove(order.getOrderId());
            } else {
                order.setStatus(OrderType.TRANSACTIONS_NEEDED);
                orderRequestedItemsMap.put(order.getOrderId(), requestedItemsToMove);
            }
            visibilityMap.put(order.getOrderId(), true);
        }
        sortOrders();
        notifyDataSetChanged();
    }

    private void sortOrders() {
        orderList.sort((o1, o2) -> {
            if (o1.getStatus() == OrderType.REGISTERED && o2.getStatus() != OrderType.REGISTERED) {
                return -1;
            } else if (o1.getStatus() != OrderType.REGISTERED && o2.getStatus() == OrderType.REGISTERED) {
                return 1;
            } else {
                return o1.getOrderDate().compareTo(o2.getOrderDate());
            }
        });
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

        if (visibilityMap.getOrDefault(order.getOrderId(), true)) {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.orderId.setText(order.getOrderId());
            holder.orderDate.setText(order.getOrderDate().toString());
            if(order.getStatus()!=null) {
                holder.orderStatus.setText(order.getStatus().toString());
            }

            if (order.getStatus() == OrderType.REGISTERED) {
                holder.itemView.setBackgroundColor(Color.parseColor("#228B22")); // Forest green
            } else if (order.getStatus() == OrderType.TRANSACTIONS_NEEDED) {
                holder.itemView.setBackgroundColor(Color.parseColor("#B22222")); // Firebrick red
            } else {
                holder.itemView.setBackgroundColor(Color.WHITE);
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

            setupRecyclerView(holder.recyclerViewOrderItems, order.getOrderId());
        } else {
            holder.itemView.setVisibility(View.INVISIBLE);
        }
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
        FirebaseForAdapters.fetchOrder(order.getOrderId(), new FirebaseForAdapters.OrderCallback() {
            @Override
            public void onCallback(Order latestOrder) {
                if (latestOrder != null && latestOrder.getStatus() == OrderType.REGISTERED) {
                    boolean allItemsAvailable = order.getStatus() == OrderType.REGISTERED;
                    List<PickupItem> requestedItemsToMove = orderRequestedItemsMap.get(order.getOrderId());
                    processRequest(order, allItemsAvailable, requestedItemsToMove);
                } else {
                    Toast.makeText(context, "Order status has changed. Please refresh.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "Error checking order status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processRequest(Order order, boolean allItemsAvailable, List<PickupItem> requestedItemsToMove) {
        String userId = MyUser.getInstance().getUser().getUserId();
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
                            TransactionRequest transactionRequest = new TransactionRequest(UUID.randomUUID().toString(), selectedWarehouse, order.getOrderId(), MyUser.getInstance().getUser().getUserId(), requestedItemsToMove, false);
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
