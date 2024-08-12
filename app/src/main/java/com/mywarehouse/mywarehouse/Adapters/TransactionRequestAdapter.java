package com.mywarehouse.mywarehouse.Adapters;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mywarehouse.mywarehouse.Activities.RequestsActivity;
import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Firebase.FirebaseForAdapters;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImages;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;
import com.mywarehouse.mywarehouse.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionRequestAdapter extends RecyclerView.Adapter<TransactionRequestAdapter.TransactionRequestViewHolder> {

    private Context context;
    private List<TransactionRequest> transactionRequestList;
    private Map<String, Item> cachedItemsMap;  // Cache for fetched items

    public TransactionRequestAdapter(Context context, List<TransactionRequest> transactionRequestList) {
        this.context = context;
        this.transactionRequestList = transactionRequestList;
        this.cachedItemsMap = new HashMap<>();  // Initialize the cache
    }

    @NonNull
    @Override
    public TransactionRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction_request, parent, false);
        return new TransactionRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionRequestViewHolder holder, int position) {
        TransactionRequest request = transactionRequestList.get(position);
        holder.orderId.setText(request.getOrderId());
        holder.warehouse.setText(request.getWarehouse());
        holder.createdBy.setText(request.getCreatedBy());

        // Setup nested RecyclerView for items with real-time listeners
        fetchPickupItems(holder.recyclerViewItems, request.getRequestedItemsToMove());

        holder.acceptButton.setOnClickListener(v -> acceptTransactionRequest(request));
        holder.denyButton.setOnClickListener(v -> denyTransactionRequest(request));

        holder.itemView.setOnClickListener(v -> {
            if (holder.isExpanded) {
                collapse(holder.recyclerViewItems);
                holder.isExpanded = false;
            } else {
                expand(holder.recyclerViewItems);
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

                // Find all relevant positions in the transaction request list that contain this itemKey
                for (int i = 0; i < transactionRequestList.size(); i++) {
                    List<PickupItem> pickupItems = transactionRequestList.get(i).getRequestedItemsToMove();
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

    private void acceptTransactionRequest(TransactionRequest request) {
        if (request.isOnUpdate()) {
            Toast.makeText(context, "On update", Toast.LENGTH_SHORT).show();
            return;
        }
        AtomicInteger itemsChecked = new AtomicInteger(0);// To keep track of checked items
        AtomicInteger anyItemOnUpdate = new AtomicInteger(0);
        AtomicInteger maxQuantityItem = new AtomicInteger(0);
        List<PickupItem> missingItems = Collections.synchronizedList(new ArrayList<>());
        for (PickupItem requestedItem : request.getRequestedItemsToMove()) {
            String documentId = requestedItem.getBarcode() + "_" + requestedItem.getName();
            FirebaseForAdapters.fetchItem(documentId, item -> {
                boolean itemAvailable = false;
                int calculatedQuantityonWarehouse = 0;
                if (item.isOnUpdate()) anyItemOnUpdate.set(-1);

                for (ItemWarehouse itemWarehouse : item.getItemWarehouses()) {
                    if (itemWarehouse.getWarehouseName().equals(request.getWarehouse())) {
                        if (itemWarehouse.getQuantity() >= requestedItem.getQuantity()) {
                            itemAvailable = true;
                            break;
                        }
                        else
                        {
                            calculatedQuantityonWarehouse=(calculatedQuantityonWarehouse+itemWarehouse.getQuantity());
                        }
                    } else {
                        maxQuantityItem.set(Math.max(maxQuantityItem.get(), itemWarehouse.getQuantity()));
                    }
                }
                if (!itemAvailable && calculatedQuantityonWarehouse<requestedItem.getQuantity()) {
                    requestedItem.setQuantity(requestedItem.getQuantity()-calculatedQuantityonWarehouse);
                    missingItems.add(requestedItem);
                }

                // Increment the checked items count
                if (itemsChecked.incrementAndGet() == request.getRequestedItemsToMove().size()) {
                    if (anyItemOnUpdate.get() == -1) {
                        Toast.makeText(context, "One or more of the items are on update", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!itemAvailable && maxQuantityItem.get() < requestedItem.getQuantity()) {
                        Toast.makeText(context, "The Item " + requestedItem.getName() + " quantity requests special modifications do it manually", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (missingItems.isEmpty()) {
                        // All items are available, proceed to update the order status
                        FirebaseForAdapters.fetchOrder(request.getOrderId(), new FirebaseForAdapters.OrderCallback() {
                            @Override
                            public void onCallback(Order order) {
                                if (order != null) {
                                    order.setStatus(OrderType.IN_PROGRESS);
                                    FirebaseForAdapters.updateOrder(order, new FirebaseForAdapters.FirestoreCallback() {
                                        @Override
                                        public void onSuccess() {
                                            // Delete the transaction request
                                            FirebaseForAdapters.deleteTransactionRequest(request.getRequestId(), new FirebaseForAdapters.FirestoreCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    transactionRequestList.remove(request);
                                                    notifyDataSetChanged();
                                                    Toast.makeText(context, "Request confirmed", Toast.LENGTH_SHORT).show();
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                    Toast.makeText(context, "Error deleting transaction request.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Toast.makeText(context, "Error updating order status.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(context, "Error fetching order.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // Some items are missing, update the request with missing items and navigate to AcceptTransactionActivity
                        request.setRequestedItemsToMove(missingItems);
                        if (context instanceof RequestsActivity) {
                            ((RequestsActivity) context).startAcceptTransactionActivity(request);
                        }
                    }
                }
            });
        }
    }

    private void denyTransactionRequest(TransactionRequest request) {
        // Fetch the order to update its status and collectedBy
        FirebaseForAdapters.fetchOrder(request.getOrderId(), new FirebaseForAdapters.OrderCallback() {
            @Override
            public void onCallback(Order order) {
                if (order != null) {
                    order.setCollectedBy(null);
                    order.setStatus(OrderType.REGISTERED);
                    order.setSelectedWarehouse(null);
                    FirebaseForAdapters.updateOrder(order, new FirebaseForAdapters.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            // Remove from pickuper's list
                            FirebaseForAdapters.removeUserPickup(request.getCreatedBy(), request.getOrderId(), new FirebaseForAdapters.FirestoreCallback() {
                                @Override
                                public void onSuccess() {
                                    // Remove the transaction request from the database
                                    FirebaseForAdapters.deleteTransactionRequest(request.getRequestId(), new FirebaseForAdapters.FirestoreCallback() {
                                        @Override
                                        public void onSuccess() {
                                            transactionRequestList.remove(request);
                                            notifyDataSetChanged();
                                            Toast.makeText(context, "Request denied", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Toast.makeText(context, "Error deleting transaction request.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(context, "Error removing pickup from user.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(context, "Error updating order status.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "Error fetching order.", Toast.LENGTH_SHORT).show();
            }
        });
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
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                recyclerView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
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
        return transactionRequestList.size();
    }

    public static class TransactionRequestViewHolder extends RecyclerView.ViewHolder {

        TextView orderId, warehouse, createdBy;
        RecyclerView recyclerViewItems;
        AppCompatImageButton acceptButton, denyButton;
        boolean isExpanded = false;

        public TransactionRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id);
            warehouse = itemView.findViewById(R.id.warehouse);
            createdBy = itemView.findViewById(R.id.created_by);
            recyclerViewItems = itemView.findViewById(R.id.recycler_view_requested_items);
            acceptButton = itemView.findViewById(R.id.button_accept);
            denyButton = itemView.findViewById(R.id.button_deny);
            recyclerViewItems.setVisibility(View.GONE);
        }
    }
}
