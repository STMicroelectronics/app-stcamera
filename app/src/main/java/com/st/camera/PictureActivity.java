package com.st.camera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;

public class PictureActivity extends AppCompatActivity {

    public static final String  EXTRA_FILE_NAME = "com.st.camera.extra.FILE_NAME";
    public static final String  EXTRA_FILE_PATH = "com.st.camera.extra.FILE_PATH";

    private ImageView mPictureImage;

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
        TextView pictureName = findViewById(R.id.picture_text_view);

        Intent intent = getIntent();
        if (intent != null) {
            String path = intent.getStringExtra(EXTRA_FILE_PATH);
            if (path != null) {
                Picasso.get().load(new File(path)).into(mPictureImage);
            }
            pictureName.setText(intent.getStringExtra(EXTRA_FILE_NAME));
        }
        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        mDoubleTapDetector = new GestureDetector(this,new DoubleTapListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (! mDoubleTapDetector.onTouchEvent(event)) {
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
}
