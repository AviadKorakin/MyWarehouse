package com.mywarehouse.mywarehouse.Adapters;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.mywarehouse.mywarehouse.Interfaces.ItemDiffCallback;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.Models.ItemWarehouse;
import com.mywarehouse.mywarehouse.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {
    private List<Item> itemList;
    private OnUpdateItemClickListener onUpdateItemClickListener;

    public InventoryAdapter(List<Item> itemList, OnUpdateItemClickListener listener) {
        this.itemList = new ArrayList<>(itemList); // Ensure to avoid direct list mutation
        this.onUpdateItemClickListener = listener;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        Item item = itemList.get(position);

        holder.itemBarcode.setText(item.getBarcode());
        holder.itemName.setText(item.getName());
        holder.itemQuantity.setText(String.valueOf(item.getTotalQuantity()));
        StringBuilder warehousesNames = new StringBuilder();
        List<ItemWarehouse> list = item.getItemWarehouses();
        Set<String> uniqueWarehouseNames = new HashSet<>();
        for (ItemWarehouse iw : list) {
            uniqueWarehouseNames.add(iw.getWarehouseName());
        }
        warehousesNames.append(TextUtils.join(", ", uniqueWarehouseNames)).append(".");
        holder.itemWarehouse.setText("Click here");
        holder.itemSupplier.setText(item.getSupplier());
        if (!item.isActive()) {
            holder.layout.setBackgroundColor(Color.BLACK);
            holder.itemDescription.setText("Deleted Item");
        } else if (item.getTotalQuantity() == 0) {
            holder.layout.setBackgroundColor(Color.rgb(180, 17, 44));
            holder.itemDescription.setText("Out of stock");
        } else {
            holder.layout.setBackgroundColor(Color.rgb(239, 200, 167));
            holder.itemDescription.setText("Click here");
        }

        // Set up the image RecyclerView
        ImageShowAdapter imageShowAdapter = new ImageShowAdapter(holder.itemView.getContext(), item.getImageUrls());
        holder.imageRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        holder.imageRecyclerView.setAdapter(imageShowAdapter);
        collapseTextView(holder.itemDescription, Item.MIN_LINES_COLLAPSED);
        collapseTextView(holder.itemWarehouse, Item.MIN_LINES_COLLAPSED);
        holder.updateIcon.setOnClickListener(v -> {
            if (onUpdateItemClickListener != null) {
                onUpdateItemClickListener.onUpdateItemClick(item);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (item.isCollapsed()) {
                holder.itemWarehouse.setText(warehousesNames.toString());
                holder.itemDescription.setText(item.getDescription());
                expandTextView(holder.itemDescription);
                expandTextView(holder.itemWarehouse);
            } else {
                collapseTextView(holder.itemDescription, Item.MIN_LINES_COLLAPSED);
                collapseTextView(holder.itemWarehouse, Item.MIN_LINES_COLLAPSED);
                holder.itemDescription.setText("Click here");
                holder.itemWarehouse.setText("Click here");

            }
            item.setCollapsed(!item.isCollapsed());
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateItems(List<Item> newItemList) {
        // Sort the new list as per the requirements
        Collections.sort(newItemList, new Comparator<Item>() {
            @Override
            public int compare(Item item1, Item item2) {
                // Sort by active status first
                if (item1.isActive() && !item2.isActive()) {
                    return -1;
                } else if (!item1.isActive() && item2.isActive()) {
                    return 1;
                } else if (item1.isActive() && item2.isActive()) {
                    // Then sort by quantity
                    if (item1.getTotalQuantity() == 0 && item2.getTotalQuantity() != 0) {
                        return 1;
                    } else if (item1.getTotalQuantity() != 0 && item2.getTotalQuantity() == 0) {
                        return -1;
                    } else {
                        // Finally, sort by barcode and name
                        int barcodeComparison = item1.getBarcode().compareTo(item2.getBarcode());
                        if (barcodeComparison != 0) {
                            return barcodeComparison;
                        } else {
                            return item1.getName().compareTo(item2.getName());
                        }
                    }
                }
                return 0;
            }
        });

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ItemDiffCallback(this.itemList, newItemList));
        this.itemList.clear();
        this.itemList.addAll(newItemList);
        diffResult.dispatchUpdatesTo(this);
    }

    public void updateItem(Item item) {
        int position = itemList.indexOf(item);
        if (position >= 0) {
            itemList.set(position, item);
            notifyItemChanged(position);
        }
    }

    private void expandTextView(MaterialTextView textView) {
        int initialHeight = textView.getMeasuredHeight();
        textView.setMaxLines(Integer.MAX_VALUE);
        textView.measure(View.MeasureSpec.makeMeasureSpec(textView.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int targetHeight = textView.getMeasuredHeight();

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, targetHeight);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            textView.getLayoutParams().height = animatedValue;
            textView.requestLayout();
        });

        animator.setDuration((long) (Math.max(targetHeight - initialHeight, 0) * 1.5));
        animator.start();
    }

    private void collapseTextView(MaterialTextView textView, int maxLines) {
        int initialHeight = textView.getMeasuredHeight();
        textView.setMaxLines(maxLines);
        textView.measure(View.MeasureSpec.makeMeasureSpec(textView.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int targetHeight = textView.getMeasuredHeight();

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, targetHeight);
        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            textView.getLayoutParams().height = animatedValue;
            textView.requestLayout();
        });

        animator.setDuration((long) (Math.max(initialHeight - targetHeight, 0) * 1.5));
        animator.start();
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        RecyclerView imageRecyclerView;
        LinearLayout layout;
        AppCompatImageView updateIcon;
        MaterialTextView itemBarcode, itemName, itemSupplier, itemDescription, itemQuantity, itemWarehouse;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageRecyclerView = itemView.findViewById(R.id.image_recycler_view);
            updateIcon = itemView.findViewById(R.id.update_icon);
            itemBarcode = itemView.findViewById(R.id.item_barcode);
            itemName = itemView.findViewById(R.id.item_name);
            itemDescription = itemView.findViewById(R.id.item_description);
            itemQuantity = itemView.findViewById(R.id.item_quantity);
            itemWarehouse = itemView.findViewById(R.id.item_warehouse);
            itemSupplier = itemView.findViewById(R.id.item_supplier);
            layout = itemView.findViewById(R.id.layout);
        }
    }

    public interface OnUpdateItemClickListener {
        void onUpdateItemClick(Item item);
    }
}
