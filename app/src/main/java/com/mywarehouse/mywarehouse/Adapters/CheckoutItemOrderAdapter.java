package com.mywarehouse.mywarehouse.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.textview.MaterialTextView;
import com.mywarehouse.mywarehouse.Models.ItemOrder;
import com.mywarehouse.mywarehouse.R;

import java.util.List;

public class CheckoutItemOrderAdapter extends RecyclerView.Adapter<CheckoutItemOrderAdapter.ViewHolder> {

    private Context context;
    private List<ItemOrder> itemOrderList;
    private OnQuantityChangeListener onQuantityChangeListener;

    public interface OnQuantityChangeListener {
        void onQuantityChange(int totalItems);
    }

    public CheckoutItemOrderAdapter(Context context, List<ItemOrder> itemOrderList, OnQuantityChangeListener onQuantityChangeListener) {
        this.context = context;
        this.itemOrderList = itemOrderList;
        this.onQuantityChangeListener = onQuantityChangeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_addorder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemOrder itemOrder = itemOrderList.get(position);
        holder.itemBarcode.setText(itemOrder.getBarcode());
        holder.itemName.setText(itemOrder.getName());
        holder.itemQuantity.setText(String.valueOf(itemOrder.getSelectedQuantity()));

        // Set up the ImageShowAdapter for images
        ImageShowAdapter imageShowAdapter = new ImageShowAdapter(context, itemOrder.getImageUrls());
        holder.recyclerViewImages.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.recyclerViewImages.setAdapter(imageShowAdapter);

        holder.buttonPlus.setOnClickListener(v -> {
            int selectedQuantity = itemOrder.getSelectedQuantity();
            if (selectedQuantity < itemOrder.getTotalQuantity()-itemOrder.getRequestedAmount()) {
                itemOrder.setSelectedQuantity(selectedQuantity + 1);
                holder.itemQuantity.setText(String.valueOf(itemOrder.getSelectedQuantity()));
                onQuantityChangeListener.onQuantityChange(getTotalSelectedItems());
            } else {
                Toast.makeText(context, "Cannot add more items than available", Toast.LENGTH_SHORT).show();
            }
        });

        holder.buttonMinus.setOnClickListener(v -> {
            int selectedQuantity = itemOrder.getSelectedQuantity();
            if (selectedQuantity > 0) {
                itemOrder.setSelectedQuantity(selectedQuantity - 1);
                holder.itemQuantity.setText(String.valueOf(itemOrder.getSelectedQuantity()));
                if (itemOrder.getSelectedQuantity() == 0) {
                    itemOrderList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, itemOrderList.size());
                }
                onQuantityChangeListener.onQuantityChange(getTotalSelectedItems());
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemOrderList.size();
    }

    private int getTotalSelectedItems() {
        int total = 0;
        for (ItemOrder itemOrder : itemOrderList) {
            total += itemOrder.getSelectedQuantity();
        }
        return total;
    }

    public void updateItems(List<ItemOrder> itemOrders) {
        this.itemOrderList = itemOrders;
        notifyDataSetChanged();
    }

    public void setMaxQuantityForItem(ItemOrder updatedItem) {
        for (ItemOrder itemOrder : itemOrderList) {
            if (itemOrder.getBarcode().equals(updatedItem.getBarcode()) && itemOrder.getName().equals(updatedItem.getName())) {
                if (itemOrder.getSelectedQuantity() > updatedItem.getSelectedQuantity()-updatedItem.getRequestedAmount()) {
                    itemOrder.setSelectedQuantity(updatedItem.getSelectedQuantity());
                }
                notifyDataSetChanged();
                break;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView itemBarcode, itemName, itemQuantity;
        AppCompatImageButton buttonPlus, buttonMinus;
        RecyclerView recyclerViewImages;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemBarcode = itemView.findViewById(R.id.item_barcode);
            itemName = itemView.findViewById(R.id.item_name);
            itemQuantity = itemView.findViewById(R.id.item_quantity);
            buttonPlus = itemView.findViewById(R.id.button_plus);
            buttonMinus = itemView.findViewById(R.id.button_minus);
            recyclerViewImages = itemView.findViewById(R.id.recycler_view_images);
        }
    }
}
