package com.mywarehouse.mywarehouse.Adapters;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImages;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Firebase.FirebaseForAdapters;
import com.mywarehouse.mywarehouse.Models.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyOrdersAdapter extends RecyclerView.Adapter<MyOrdersAdapter.MyOrdersViewHolder> {

    private Context context;
    private List<Order> orderList;
    private Map<String, Item> cachedItemsMap;  // Cache for fetched items

    public MyOrdersAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
        this.cachedItemsMap = new HashMap<>();  // Initialize the cache
    }

    @NonNull
    @Override
    public MyOrdersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_order, parent, false);
        return new MyOrdersViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyOrdersViewHolder holder, int position) {
        Order order = orderList.get(position);
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

                // Find all relevant positions in the order list that contain this itemKey
                for (int i = 0; i < orderList.size(); i++) {
                    List<PickupItem> pickupItems = orderList.get(i).getPickupItems();
                    for (PickupItem pickupItem : pickupItems) {
                        if ((pickupItem.getBarcode() + "_" + pickupItem.getName()).equals(itemKey)) {
                            notifyItemChanged(i);  // Notify the adapter to refresh the view
                        }
                    }
                }
            }
        });
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

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class MyOrdersViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView orderId, orderDate, orderStatus;
        RecyclerView recyclerViewOrderItems;
        boolean isExpanded = false;

        public MyOrdersViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id);
            orderDate = itemView.findViewById(R.id.order_date);
            orderStatus = itemView.findViewById(R.id.order_status);
            recyclerViewOrderItems = itemView.findViewById(R.id.recycler_view_order_items);
            recyclerViewOrderItems.setVisibility(View.GONE);
        }
    }
}