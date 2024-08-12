package com.mywarehouse.mywarehouse.Adapters;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImages;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Firebase.FirebaseForAdapters;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Utilities.MyUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MyPickupsAdapter extends RecyclerView.Adapter<MyPickupsAdapter.MyPickupsViewHolder> {

    private Context context;
    private List<Order> pickupList;
    private LaunchPickUpActivityCallback launchPickUpActivityCallback;
    private Map<String, Item> cachedItemsMap;  // Cache for fetched items

    public MyPickupsAdapter(Context context, List<Order> pickupList, LaunchPickUpActivityCallback callback) {
        this.context = context;
        this.pickupList = pickupList;
        this.launchPickUpActivityCallback = callback;
        this.cachedItemsMap = new HashMap<>();  // Initialize the cache
        sortPickups();
    }

    @NonNull
    @Override
    public MyPickupsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_pick_up, parent, false);
        return new MyPickupsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyPickupsViewHolder holder, int position) {
        Order order = pickupList.get(position);
        holder.orderId.setText(order.getOrderId());
        holder.orderDate.setText(order.getOrderDate().toString());
        holder.orderStatus.setText(order.getStatus().toString());

        fetchPickupItems(holder.recyclerViewOrderItems, order.getPickupItems());

        holder.itemView.setOnClickListener(v -> {
            if (holder.isExpanded) {
                collapse(holder.recyclerViewOrderItems);
                holder.isExpanded = false;
            } else {
                expand(holder.recyclerViewOrderItems);
                holder.isExpanded = true;
            }
        });

        holder.buttonCollect.setOnClickListener(v -> FirebaseForAdapters.checkOrderAvailability(order, new FirebaseForAdapters.FirestoreCallback() {
            @Override
            public void onSuccess() {
                launchPickUpActivityCallback.launch(order);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "Order cannot be collected: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                order.setStatus(OrderType.REGISTERED);
                FirebaseForAdapters.updateOrder(order, new FirebaseForAdapters.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        String userId = MyUser.getInstance().getUser().getUserId();
                        FirebaseForAdapters.removeUserPickup(userId, order.getOrderId(), new FirebaseForAdapters.FirestoreCallback() {
                            @Override
                            public void onSuccess() {
                                pickupList.remove(order);
                                notifyDataSetChanged();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(context, "Error removing user pickup.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(context, "Error updating order status.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }));
    }

    private void fetchPickupItems(RecyclerView recyclerView, List<PickupItem> pickupItems) {
        List<PickupItemWithImages> pickupItemsWithImagesList = new ArrayList<>();

        for (PickupItem pickupItem : pickupItems) {
            String itemKey = pickupItem.getBarcode() + "_" + pickupItem.getName();

            if (cachedItemsMap.containsKey(itemKey)) {
                Item cachedItem = cachedItemsMap.get(itemKey);
                PickupItemWithImages pickupItemWithImages = new PickupItemWithImages(pickupItem, cachedItem.getImageUrls());
                pickupItemsWithImagesList.add(pickupItemWithImages);
                setItemChangeListener(itemKey);  // Set the real-time listener for the item

                if (pickupItemsWithImagesList.size() == pickupItems.size()) {
                    setupRecyclerView(recyclerView, pickupItemsWithImagesList);
                }
            } else {
                FirebaseForAdapters.fetchItem(itemKey, item -> {
                    if (item != null) {
                        cachedItemsMap.put(itemKey, item);  // Store in cache
                        PickupItemWithImages pickupItemWithImages = new PickupItemWithImages(pickupItem, item.getImageUrls());
                        pickupItemsWithImagesList.add(pickupItemWithImages);
                        setItemChangeListener(itemKey);  // Set the real-time listener for the item

                        if (pickupItemsWithImagesList.size() == pickupItems.size()) {
                            setupRecyclerView(recyclerView, pickupItemsWithImagesList);
                        }
                    }
                });
            }
        }
    }

    private void setItemChangeListener(String itemKey) {
        FirebaseForAdapters.listenToItemChanges(itemKey, updatedItem -> {
            if (updatedItem != null) {
                cachedItemsMap.put(itemKey, updatedItem);  // Update the cache
                notifyItemChanged(itemKey);  // Notify the adapter to refresh the view
            }
        });
    }

    private void notifyItemChanged(String itemKey) {
        for (int i = 0; i < pickupList.size(); i++) {
            List<PickupItem> pickupItems = pickupList.get(i).getPickupItems();
            for (PickupItem pickupItem : pickupItems) {
                if ((pickupItem.getBarcode() + "_" + pickupItem.getName()).equals(itemKey)) {
                    notifyItemChanged(i);
                    return;
                }
            }
        }
    }

    private void setupRecyclerView(RecyclerView recyclerView, List<PickupItemWithImages> pickupItemsWithImagesList) {
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
    private void sortPickups() {
        Collections.sort(pickupList, new Comparator<Order>() {
            @Override
            public int compare(Order o1, Order o2) {
                int priorityComparison = getOrderPriority(o1.getStatus()) - getOrderPriority(o2.getStatus());
                if (priorityComparison != 0) {
                    return priorityComparison;
                }
                return o1.getOrderDate().compareTo(o2.getOrderDate());
            }

            private int getOrderPriority(OrderType status) {
                if (Objects.requireNonNull(status) == OrderType.IN_PROGRESS) {
                    return 1;
                }
                return 2;
            }
        });
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return pickupList.size();
    }

    public static class MyPickupsViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView orderId, orderDate, orderStatus;
        RecyclerView recyclerViewOrderItems;
        boolean isExpanded = false;
        ImageButton buttonCollect;

        public MyPickupsViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id);
            orderDate = itemView.findViewById(R.id.order_date);
            orderStatus = itemView.findViewById(R.id.order_status);
            recyclerViewOrderItems = itemView.findViewById(R.id.recycler_view_order_items);
            buttonCollect = itemView.findViewById(R.id.button_collect);
            recyclerViewOrderItems.setVisibility(View.GONE);
        }
    }

    public interface LaunchPickUpActivityCallback {
        void launch(Order order);
    }
}
