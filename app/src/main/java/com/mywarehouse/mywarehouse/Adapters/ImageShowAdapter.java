package com.mywarehouse.mywarehouse.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mywarehouse.mywarehouse.R;

import java.util.ArrayList;
import java.util.List;

public class ImageShowAdapter extends RecyclerView.Adapter<ImageShowAdapter.ImageViewHolder> {

    private final List<String> imageUrls;
    private final Context context;

    public ImageShowAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_showimage, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        if (imageUrls.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(R.drawable.ic_loading_image) // Replace with your default image resource
                    .placeholder(R.drawable.loading_gif)
                    .into(holder.imageView);
        } else {
            String imageUrl = imageUrls.get(position);
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.loading_gif)
                    .into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.isEmpty() ? 1 : imageUrls.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }
    }
}
