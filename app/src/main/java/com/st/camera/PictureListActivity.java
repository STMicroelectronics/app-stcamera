package com.st.camera;

import android.provider.MediaStore;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.st.camera.adapter.PictureAdapter;
import com.st.camera.adapter.PictureModel;

import java.util.ArrayList;
import java.util.List;

public class PictureListActivity extends AppCompatActivity implements PictureAdapter.OnItemClickListener {

    private PictureAdapter pictureAdapter;
    private final List<PictureModel> imageList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_list);

        RecyclerView recyclerView = findViewById(R.id.picture_list);
        assert recyclerView != null;

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        pictureAdapter = new PictureAdapter(this, imageList, this);
        recyclerView.setAdapter(pictureAdapter);

        ImageButton mDeleteAllPictures = findViewById(R.id.delete_all_picture);
        assert mDeleteAllPictures != null;

        loadImages();
    }

    public void deleteAllPictures(View view) {
        String selection = MediaStore.Images.Media.RELATIVE_PATH + "=?";
        String[] selectionArgs = new String[]{"Pictures/STCamera-Image/"};

        ContentResolver contentResolver = getContentResolver();
        Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = contentResolver.query(imagesUri, null, selection, selectionArgs, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                Uri uri = ContentUris.withAppendedId(imagesUri, id);
                contentResolver.delete(uri, null, null);
            }
            cursor.close();
        }
        loadImages();
    }

    public void loadImages() {
        int oldSize = imageList.size();
        if (oldSize > 0) {
            imageList.clear();
            pictureAdapter.notifyItemRangeRemoved(0, oldSize);
        }

        String selection = MediaStore.Images.Media.RELATIVE_PATH + "=?";
        String[] selectionArgs = new String[]{"Pictures/STCamera-Image/"};

        ContentResolver contentResolver = getContentResolver();
        Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = contentResolver.query(imagesUri, null, selection, selectionArgs, null);
        if (cursor != null) {
            int newItemsCount = 0;
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                Uri uri = ContentUris.withAppendedId(imagesUri, id);
                imageList.add(new PictureModel(uri));
                newItemsCount++;
            }
            cursor.close();
            if (newItemsCount > 0) {
                pictureAdapter.notifyItemRangeInserted(0, newItemsCount);
            }
        }
    }

    @Override
    public void onItemClick(String pictureName, String picturePath) {
        Log.d("PictureListActivity", "Following picture clicked: " + picturePath);
        Intent intent = new Intent(getApplicationContext(), PictureActivity.class);
        intent.putExtra(PictureActivity.EXTRA_FILE_PATH, picturePath);
        intent.putExtra(PictureActivity.EXTRA_FILE_NAME, pictureName);
        startActivity(intent);
    }
}
