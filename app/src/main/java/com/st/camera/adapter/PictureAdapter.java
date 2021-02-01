package com.st.camera.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.st.camera.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PictureAdapter extends RecyclerView.Adapter<PictureAdapter.ViewHolder> {

    private static final String LOG_TAG = PictureAdapter.class.getSimpleName();

    private final PictureAdapter.OnClickListener mListener;
    private final List<PictureDetails> arrayList = new ArrayList<>();

    public PictureAdapter (OnClickListener listener) {
        mListener = listener;
    }

    private int mMaxHeight = 0;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_picture, parent,false);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = parent.getHeight();
        view.setLayoutParams(params);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PictureDetails picture = arrayList.get(position);
        File pictureFile = new File(picture.getPicturePath());
        if (pictureFile.exists()) {
            Bitmap pictureBitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
            holder.pictureView.setImageBitmap(scaledBitmap(pictureBitmap));
        }
    }

    private Bitmap scaledBitmap(Bitmap bitmap) {
        if (mMaxHeight > 0) {
            Log.d(LOG_TAG, "bitmap size (wxh) = " + bitmap.getWidth() + " x " + bitmap.getHeight());
            // take care of image orientation (width -> height)
            float ratio = (float) bitmap.getWidth() / (float) mMaxHeight;
            if (ratio > 1) {
                int width = (int) ( (float) bitmap.getWidth() / ratio);
                int height = (int) ( (float) bitmap.getHeight() / ratio);
                Log.d(LOG_TAG, "bitmap resized to (wxh) = " + width + " x " + height + " (ratio = " + ratio + ")");
                return Bitmap.createScaledBitmap(bitmap, width, height,false);
            }
        }
        return bitmap;
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public void addItems(ArrayList<PictureDetails> list) {
        int prevSize = arrayList.size();
        arrayList.addAll(list);
        notifyItemRangeInserted(prevSize,arrayList.size() - prevSize);
    }

    public void addItem(PictureDetails picture) {
        arrayList.add(0, picture);
        notifyItemRangeInserted(0,1);
        mListener.onAdded(0);
    }

    public void removeAllItems() {
        int prevSize = arrayList.size();
        arrayList.clear();
        notifyItemRangeRemoved(0, prevSize);
    }

    public void setMaxHeight(int height) {
        mMaxHeight = height;
    }

    public interface OnClickListener {
        void onAdded(int position);
        void onClick(String pictureName, String picturePath);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView pictureView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            pictureView = itemView.findViewById(R.id.picture);
            itemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int height = itemView.getMeasuredHeight();
            pictureView.setMaxHeight(height);
            Log.d(LOG_TAG, "pictureView set max height to " + height);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                PictureDetails picture = arrayList.get(adapterPosition);
                mListener.onClick(picture.getPictureName(),picture.getPicturePath());
            }
        }
    }
}
