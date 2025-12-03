package com.st.camera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.exifinterface.media.ExifInterface ;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PictureActivity extends AppCompatActivity {
    private static final String LOG_TAG = PictureActivity.class.getSimpleName();

    public static final String EXTRA_FILE_NAME = "com.st.camera.extra.FILE_NAME";
    public static final String EXTRA_FILE_PATH = "com.st.camera.extra.FILE_PATH";

    private ImageView mPictureImage;
    private String picturePath;
    private String pictureName;

    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mDoubleTapDetector;

    private float mScaleFactor = 1.0f;
    private static final float MIN_SCALE_FACTOR = 0.5f;
    private static final float MAX_SCALE_FACTOR = 2.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);

        mPictureImage = findViewById(R.id.picture_image_view);
        ImageButton buttonInfo = findViewById(R.id.info_picture);

        // Set up the OnClickListener for the info button
        buttonInfo.setOnClickListener(v -> showInfoDialog());

        // Request permission to read external storage if necessary
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        Intent intent = getIntent();
        if (intent != null) {
            picturePath = intent.getStringExtra(EXTRA_FILE_PATH);
            pictureName = intent.getStringExtra(EXTRA_FILE_NAME);
            if (picturePath != null) {
                Uri uri = Uri.parse(picturePath);
                Picasso.get().load(uri).into(mPictureImage);
            }
        }

        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        mDoubleTapDetector = new GestureDetector(this, new DoubleTapListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mDoubleTapDetector.onTouchEvent(event)) {
            mScaleGestureDetector.onTouchEvent(event);
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(MIN_SCALE_FACTOR,
                    Math.min(mScaleFactor, MAX_SCALE_FACTOR));
            mPictureImage.setScaleX(mScaleFactor);
            mPictureImage.setScaleY(mScaleFactor);
            return true;
        }
    }

    private class DoubleTapListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            mScaleFactor = 1.0f;
            mPictureImage.setScaleX(mScaleFactor);
            mPictureImage.setScaleY(mScaleFactor);
            return true;
        }
    }

    public void showInfoDialog() {
        Uri uri = Uri.parse(picturePath);
        String fileSizeStr = getFileSize(uri);
        String fileDate = getFileDate(uri);
        int[] dimensions = getImageDimensions(uri);

        Bundle args = new Bundle();
        args.putString("path", picturePath);
        args.putString("name", pictureName);
        args.putString("size", fileSizeStr);
        args.putString("date", fileDate);
        args.putInt("width", dimensions[0]);
        args.putInt("height", dimensions[1]);

        DialogFragment infoDialog = new PictureInfoDialogFragment();
        infoDialog.setArguments(args);
        infoDialog.show(getSupportFragmentManager(), "PictureInfoDialogFragment");
    }

    private String getFileSize(Uri uri) {
        String[] projection = { MediaStore.Images.Media.SIZE };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
            cursor.close();
            return android.text.format.Formatter.formatFileSize(this, size);
        }
        return "Unknown";
    }

    private String getFileDate(Uri uri) {
        try {
            String[] projection = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                cursor.close();

                ExifInterface exif = new ExifInterface(filePath);
                String dateString = exif.getAttribute(ExifInterface.TAG_DATETIME);
                if (dateString != null) {
                    SimpleDateFormat exifDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault());
                    Date date = exifDateFormat.parse(dateString);
                    SimpleDateFormat displayDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    assert date != null;
                    return displayDateFormat.format(date);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred", e);
        }
        return "Unknown";
    }

    private int[] getImageDimensions(Uri uri) {
        int[] dimensions = new int[2];
        Cursor cursor = null;
        try {
            String[] projection = { MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT };
            cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                dimensions[0] = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
                dimensions[1] = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return dimensions;
    }
}