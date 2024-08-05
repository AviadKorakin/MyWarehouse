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
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImages;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Firebase.FirebaseForAdapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyOrdersAdapter extends RecyclerView.Adapter<MyOrdersAdapter.MyOrdersViewHolder> {

    private Context context;
    private List<Order> orderList;

    public MyOrdersAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
        sortOrders();
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

        fetchPickupItems(holder.recyclerViewOrderItems, order.getPickupItems(), holder);

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

    private void fetchPickupItems(RecyclerView recyclerView, List<PickupItem> pickupItems, MyOrdersViewHolder holder) {
        List<PickupItemWithImages> pickupItemsWithImagesList = new ArrayList<>();

        for (PickupItem pickupItem : pickupItems) {
            String documentId = pickupItem.getBarcode() + "_" + pickupItem.getName();
            FirebaseForAdapters.fetchItem(documentId, item -> {
                PickupItemWithImages pickupItemWithImages = new PickupItemWithImages(pickupItem, item.getImageUrls());
                pickupItemsWithImagesList.add(pickupItemWithImages);

                if (pickupItemsWithImagesList.size() == pickupItems.size()) {
                    PickupItemAdapter pickupItemAdapter = new PickupItemAdapter(context, pickupItemsWithImagesList);
                    recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
                    recyclerView.setAdapter(pickupItemAdapter);
                }
            });
        }
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

    private void sortOrders() {
        Collections.sort(orderList, new Comparator<Order>() {
            @Override
            public int compare(Order o1, Order o2) {
                int priorityComparison = getOrderPriority(o1.getStatus()) - getOrderPriority(o2.getStatus());
                if (priorityComparison != 0) {
                    return priorityComparison;
                }
                return o1.getOrderDate().compareTo(o2.getOrderDate());
            }

            private int getOrderPriority(OrderType status) {
                switch (status) {
                    case REGISTERED:
                        return 1;
                    case IN_PROGRESS:
                        return 2;
                    case TRANSACTIONS_NEEDED:
                        return 3;
                    case PICKED_UP:
                        return 4;
                    case COMPLETED:
                        return 5;
                    default:
                        return 6;
                }
            }
        });
        notifyDataSetChanged();
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
