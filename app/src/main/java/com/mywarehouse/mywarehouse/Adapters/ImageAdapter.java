package com.mywarehouse.mywarehouse.Adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mywarehouse.mywarehouse.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private final List<Uri> imageUris;
    private final List<String> imageUrls;
    private final List<String> imageIds; // Unique IDs for images
    private String itemId;
    private final Context context;

    public ImageAdapter(Context context) {
        this.context = context;
        this.imageUris = new ArrayList<>();
        this.imageUrls = new ArrayList<>();
        this.imageIds = new ArrayList<>();
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getLastItemId() {
        return imageIds.size() > 0 ? imageIds.get(imageIds.size() - 1) : null;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);
        Glide.with(holder.itemView.getContext())
                .load(imageUri)
                .placeholder(R.drawable.loading_gif)
                .into(holder.imageView);

        holder.buttonDelete.setEnabled(!imageUrls.get(position).isEmpty());
        holder.buttonDelete.setOnClickListener(v -> {
            if (!imageUrls.get(position).isEmpty()) {
                deleteImageFromFirebase(imageIds.get(position)); // Use image ID for deletion
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public void addDefaultImage(String imageId, Uri defaultImageUri) {
        imageIds.add(imageId);
        imageUris.add(defaultImageUri);
        imageUrls.add("");
        notifyItemInserted(imageUris.size() - 1);
    }

    public void updateImageUri(String imageId, Uri newImageUri, String imageUrl) {
        int position = imageIds.indexOf(imageId);
        if (position >= 0 && position < imageUris.size()) {
            imageUris.set(position, newImageUri);
            imageUrls.set(position, imageUrl);
            notifyItemChanged(position);
        }
    }

    public ArrayList<String> getImageUrls() {
        return new ArrayList<>(imageUrls);
    }

    public void restoreState(ArrayList<String> savedImageUrls) {
        imageUrls.clear();
        imageUris.clear();
        imageIds.clear();
        for (String url : savedImageUrls) {
            String imageId = UUID.randomUUID().toString();
            imageUrls.add(url);
            imageUris.add(Uri.parse(url));
            imageIds.add(imageId);
        }
        notifyDataSetChanged();
    }

    public void removeAllImages() {
        for (String imageUrl : imageUrls) {
            if (!imageUrl.isEmpty()) {
                deleteImageFromFirebase(imageIds.get(imageUrls.indexOf(imageUrl)));
            }
        }
        imageUris.clear();
        imageUrls.clear();
        imageIds.clear();
        notifyDataSetChanged();
    }

    private void deleteImageFromFirebase(String imageId) {
        int position = imageIds.indexOf(imageId);
        if (position >= 0 && position < imageUrls.size()) {
            String imageUrl = imageUrls.get(position);
            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
            imageRef.delete().addOnSuccessListener(aVoid -> {
                // Image deleted successfully from Firebase Storage
                removeItem(imageId);
            }).addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to delete image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    public void removeItem(String imageId) {
        int position = imageIds.indexOf(imageId);
        if (position >= 0 && position < imageUris.size()) {
            imageUris.remove(position);
            imageUrls.remove(position);
            imageIds.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount()); // Update the range to ensure correct positions
        }
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton buttonDelete;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }
    }
}
