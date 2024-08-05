package com.mywarehouse.mywarehouse.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.mywarehouse.mywarehouse.Models.PickupItemWithImagesAndLocations;
import com.mywarehouse.mywarehouse.R;

import java.util.List;

public class ShowPickUpAdapter extends RecyclerView.Adapter<ShowPickUpAdapter.PickupItemViewHolder> {

    private Context context;
    private List<PickupItemWithImagesAndLocations> pickupItemWithImagesAndLocationsList;
    private OnItemClickListener onItemClickListener;

    public ShowPickUpAdapter(Context context, List<PickupItemWithImagesAndLocations> pickupItemWithImagesAndLocationsList, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.pickupItemWithImagesAndLocationsList = pickupItemWithImagesAndLocationsList;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public PickupItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_picker_pickup, parent, false);
        return new PickupItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PickupItemViewHolder holder, int position) {
        PickupItemWithImagesAndLocations item = pickupItemWithImagesAndLocationsList.get(position);
        holder.itemName.setText(item.getPickupItemWithImages().getPickupItem().getName());
        holder.itemBarcode.setText(item.getPickupItemWithImages().getPickupItem().getBarcode());
        holder.itemQuantity.setText(String.valueOf(item.getPickupItemWithImages().getPickupItem().getQuantity()));

        // Set up the image RecyclerView
        ImageShowAdapter imageShowAdapter = new ImageShowAdapter(context, item.getPickupItemWithImages().getImageUrls());
        holder.recyclerViewImages.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.recyclerViewImages.setAdapter(imageShowAdapter);

        holder.mapButton.setOnClickListener(v -> onItemClickListener.onMapClick(position));
        holder.collectButton.setOnClickListener(v -> onItemClickListener.onCollectClick(position));
    }

    @Override
    public int getItemCount() {
        return pickupItemWithImagesAndLocationsList.size();
    }

    public static class PickupItemViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView itemName, itemBarcode, itemQuantity;
        AppCompatImageButton mapButton, collectButton;
        RecyclerView recyclerViewImages;

        public PickupItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemBarcode = itemView.findViewById(R.id.item_barcode);
            itemQuantity = itemView.findViewById(R.id.item_quantity);
            mapButton = itemView.findViewById(R.id.button_map);
            collectButton = itemView.findViewById(R.id.button_collect);
            recyclerViewImages = itemView.findViewById(R.id.recycler_view_images);
        }
    }

    public interface OnItemClickListener {
        void onMapClick(int position);
        void onCollectClick(int position);
    }
}
