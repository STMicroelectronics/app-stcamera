package com.st.camera.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.st.camera.PictureListActivity;
import com.st.camera.R;

import java.util.List;

public class PictureAdapter extends RecyclerView.Adapter<PictureAdapter.PictureViewHolder> {

    private final List<PictureModel> pictureList;
    private final Context context;
    private final OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(String pictureName, String picturePath);
    }

    public PictureAdapter(Context context, List<PictureModel> pictureList, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.pictureList = pictureList;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public PictureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new PictureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PictureViewHolder holder, int position) {
        PictureModel pictureModel = pictureList.get(position);
        holder.imageView.setImageURI(pictureModel.getImageUri());

        holder.itemView.setOnClickListener(v -> {
            String picturePath = pictureModel.getImageUri().toString();
            String pictureName = picturePath.substring(picturePath.lastIndexOf('/') + 1);
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(pictureName, picturePath);
            }
        });

        holder.deleteButton.setOnClickListener(v -> deletePicture(pictureModel.getImageUri()));
    }

    @Override
    public int getItemCount() {
        return pictureList.size();
    }

    public static class PictureViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton deleteButton;

        public PictureViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            deleteButton = itemView.findViewById(R.id.delete_picture);
        }
    }

    private void deletePicture(Uri imageUri) {
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.delete(imageUri, null, null);
        if (context instanceof PictureListActivity) {
            ((PictureListActivity) context).loadImages();
        }
    }
}
