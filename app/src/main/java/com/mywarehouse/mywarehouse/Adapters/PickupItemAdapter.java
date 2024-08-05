package com.mywarehouse.mywarehouse.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.textview.MaterialTextView;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImages;
import com.mywarehouse.mywarehouse.R;

import java.util.List;

public class PickupItemAdapter extends RecyclerView.Adapter<PickupItemAdapter.PickupItemViewHolder> {

    private Context context;
    private List<PickupItemWithImages> pickupItemsWithImages;

    public PickupItemAdapter(Context context, List<PickupItemWithImages> pickupItemsWithImages) {
        this.context = context;
        this.pickupItemsWithImages = pickupItemsWithImages;
    }

    @NonNull
    @Override
    public PickupItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pickup, parent, false);
        return new PickupItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PickupItemViewHolder holder, int position) {
        PickupItemWithImages pickupItemWithImages = pickupItemsWithImages.get(position);
        holder.itemBarcode.setText(pickupItemWithImages.getPickupItem().getBarcode());
        holder.itemName.setText(pickupItemWithImages.getPickupItem().getName());
        holder.itemQuantity.setText(String.valueOf(pickupItemWithImages.getPickupItem().getQuantity()));

        // Load images into the RecyclerView
        ImageShowAdapter imageShowAdapter = new ImageShowAdapter(context, pickupItemWithImages.getImageUrls());
        holder.recyclerViewImages.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.recyclerViewImages.setAdapter(imageShowAdapter);
    }

    @Override
    public int getItemCount() {
        return pickupItemsWithImages.size();
    }

    public static class PickupItemViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView itemBarcode, itemName, itemQuantity;
        RecyclerView recyclerViewImages;

        public PickupItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemBarcode = itemView.findViewById(R.id.item_barcode);
            itemName = itemView.findViewById(R.id.item_name);
            itemQuantity = itemView.findViewById(R.id.item_quantity);
            recyclerViewImages = itemView.findViewById(R.id.recycler_view_images);
        }
    }
}
