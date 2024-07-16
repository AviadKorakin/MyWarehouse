package com.mywarehouse.mywarehouse.Adapters;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mywarehouse.mywarehouse.Activities.UpdateItemActivity;
import com.mywarehouse.mywarehouse.Models.Item;
import com.mywarehouse.mywarehouse.R;

import java.util.ArrayList;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private List<Item> itemList;

    public InventoryAdapter(List<Item> itemList) {
        this.itemList = itemList;
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
        holder.itemDescription.setText(item.getDescription());
        holder.itemLatLng.setText(item.getLatitude() + ", " + item.getLongitude());
        holder.itemQuantity.setText(String.valueOf(item.getQuantity()));

        // Set up the image RecyclerView
        ImageShowAdapter imageShowAdapter = new ImageShowAdapter(holder.itemView.getContext(), item.getImageUrls());
        holder.imageRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        holder.imageRecyclerView.setAdapter(imageShowAdapter);

        holder.updateIcon.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), UpdateItemActivity.class);
            intent.putExtra("item", item);
            holder.itemView.getContext().startActivity(intent);
        });

        holder.itemView.setOnClickListener(v -> {
            ArrayList<ObjectAnimator> animators = new ArrayList<>();

            if (item.isCollapsed()) {
                animators.add(ObjectAnimator
                        .ofInt(holder.itemDescription, "maxLines", holder.itemDescription.getLineCount())
                        .setDuration(Math.max(holder.itemDescription.getLineCount() - Item.MIN_LINES_COLLAPSED, 0) * 50L));
            } else {
                animators.add(ObjectAnimator
                        .ofInt(holder.itemDescription, "maxLines", Item.MIN_LINES_COLLAPSED)
                        .setDuration(Math.max(holder.itemDescription.getLineCount() - Item.MIN_LINES_COLLAPSED, 0) * 50L));
            }

            animators.forEach(ObjectAnimator::start);
            item.setCollapsed(!item.isCollapsed());
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        RecyclerView imageRecyclerView;
        ImageView updateIcon;
        TextView itemBarcode, itemName, itemDescription, itemLatLng, itemQuantity;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageRecyclerView = itemView.findViewById(R.id.image_recycler_view);
            updateIcon = itemView.findViewById(R.id.update_icon);
            itemBarcode = itemView.findViewById(R.id.item_barcode);
            itemName = itemView.findViewById(R.id.item_name);
            itemDescription = itemView.findViewById(R.id.item_description);
            itemLatLng = itemView.findViewById(R.id.item_lat_lng);
            itemQuantity = itemView.findViewById(R.id.item_quantity);
        }
    }
}
