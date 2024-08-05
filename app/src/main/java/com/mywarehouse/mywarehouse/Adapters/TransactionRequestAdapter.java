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

import com.mywarehouse.mywarehouse.Enums.OrderType;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.Models.Order;
import com.mywarehouse.mywarehouse.Models.PickupItem;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;
import com.mywarehouse.mywarehouse.R;
import com.mywarehouse.mywarehouse.Firebase.FirebaseForAdapters;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionRequestAdapter extends RecyclerView.Adapter<TransactionRequestAdapter.TransactionRequestViewHolder> {

    private Context context;
    private List<TransactionRequest> transactionRequestList;

    public TransactionRequestAdapter(Context context, List<TransactionRequest> transactionRequestList) {
        this.context = context;
        this.transactionRequestList = transactionRequestList;
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

        // Fetch the user name from Firestore
        FirebaseForAdapters.fetchUser(request.getCreatedBy(), user -> {
            if (user != null) {
                holder.createdBy.setText(user.getName());
            } else {
                holder.createdBy.setText(request.getCreatedBy()); // fallback
            }
        });

        // Setup nested RecyclerView for items
        FirebaseForAdapters.fetchPickupItemsWithImages(request.getRequestedItemsToMove(), pickupItemsWithImages -> {
            PickupItemAdapter pickupItemAdapter = new PickupItemAdapter(context, pickupItemsWithImages);
            holder.recyclerViewItems.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
            holder.recyclerViewItems.setAdapter(pickupItemAdapter);
        });

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

    @Override
    public int getItemCount() {
        return transactionRequestList.size();
    }

    private void acceptTransactionRequest(TransactionRequest request) {
        AtomicBoolean allItemsAvailable = new AtomicBoolean(true);
        AtomicInteger itemsChecked = new AtomicInteger(0); // To keep track of checked items

        for (PickupItem pickupItem : request.getRequestedItemsToMove()) {
            String documentId = pickupItem.getBarcode() + "_" + pickupItem.getName();
            FirebaseForAdapters.fetchItem(documentId, item -> {
                boolean itemAvailable = false;
                for (ItemWarehouse itemWarehouse : item.getItemWarehouses()) {
                    if (itemWarehouse.getWarehouseName().equals(request.getWarehouse()) && itemWarehouse.getQuantity() >= pickupItem.getQuantity()) {
                        itemAvailable = true;
                        break;
                    }
                }
                if (!itemAvailable) {
                    allItemsAvailable.set(false);
                }

                // Increment the checked items count
                if (itemsChecked.incrementAndGet() == request.getRequestedItemsToMove().size()) {
                    if (allItemsAvailable.get()) {
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
                        Toast.makeText(context, "Not all items are available in the warehouse", Toast.LENGTH_SHORT).show();
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
