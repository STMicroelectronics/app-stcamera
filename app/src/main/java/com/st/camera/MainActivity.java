package com.st.camera;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ResolutionInfo;

import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.util.Size;
import android.view.View;

import android.widget.CheckBox;

import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.Preview;
import androidx.camera.core.CameraSelector;
import android.util.Log;

import androidx.camera.core.ImageCaptureException;

import java.text.SimpleDateFormat;
import java.util.Locale;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private PreviewView mPreviewView;
    private ImageButton mPictureListButton;

    private LinearLayout mSettingLayout;
    private RadioButton mRadioButtonResolution1;

    private ImageCapture mImageCapture;
    private Preview mPreview;
    private CameraSelector mCameraSelector;
    private ProcessCameraProvider mCameraProvider;
    private ResolutionInfo mResolutionInfo;
    private TextView mResolutionTextview;
    private CheckBox mDisplayPreviewResolution;

    public enum Resolution {
        resolution1(2592, 1944, AspectRatio.RATIO_4_3),
        resolution2(1600, 1200, AspectRatio.RATIO_4_3),
        resolution3(640, 480, AspectRatio.RATIO_4_3),
        resolution4(1920, 1080, AspectRatio.RATIO_16_9),
        resolution5(1280, 720, AspectRatio.RATIO_16_9);

        private final int width;
        private final int height;

        private final int aspectRatio;

        Resolution(int width, int height, int aspectRatio) {
            this.width = width;
            this.height = height;
            this.aspectRatio = aspectRatio;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        @AspectRatio.Ratio
        public int getAspectRatio() { return aspectRatio; }
    }

    private Boolean mCameraRunningState = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreviewView = findViewById(R.id.previewView);
        assert mPreviewView != null;

        mPictureListButton = findViewById(R.id.button_gallery);
        assert mPictureListButton != null;

        mSettingLayout = findViewById(R.id.layout_settings);
        assert mSettingLayout != null;
        mRadioButtonResolution1 = findViewById(R.id.radio_button_resolution1);
        assert mRadioButtonResolution1 != null;

        mResolutionTextview = findViewById(R.id.resolution_textview);
        assert mResolutionTextview != null;
        mDisplayPreviewResolution = findViewById(R.id.display_preview_resolution_checkbox);
        assert mDisplayPreviewResolution != null;

        mDisplayPreviewResolution.setOnClickListener(view -> {
            if (mDisplayPreviewResolution.isChecked()) {
                mResolutionTextview.setVisibility(View.VISIBLE);
            } else {
                mResolutionTextview.setVisibility(View.INVISIBLE);
            }
        });

        checkPermission();
    }

    private static final int REQUEST_PERMISSION_STATE = 100;

    private void checkPermission() {
        // External storage access required to allow taking and displaying pictures
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            Log.d(LOG_TAG, "Permission denied : request it !");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_PERMISSION_STATE);
        } else {
            Log.d(LOG_TAG, "Permission required (already) Granted!");
            mPreviewView.setVisibility(View.VISIBLE);
            if (!mCameraRunningState) {
                mPictureListButton.setVisibility(View.VISIBLE);
                startCamera();
                mCameraRunningState = true;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_STATE) {
            if (grantResults.length > 0) {
                int index = 0;
                for (String permission : permissions) {
                    if (permission.equals(Manifest.permission.CAMERA) && (grantResults[index] == PackageManager.PERMISSION_GRANTED)) {
                        mPreviewView.setVisibility(View.VISIBLE);
                        if (!mCameraRunningState) {
                            mPictureListButton.setVisibility(View.VISIBLE);
                            startCamera();
                            mCameraRunningState = true;
                        }
                    }
                    index++;
                }
            } else {
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                mCameraProvider = cameraProviderFuture.get();

                mPreview = getPreview();

                // TODO : get back current selected value instead
                setImageCapture(Resolution.resolution2);

                mCameraSelector = new CameraSelector.Builder()
                        .build();

                mCameraProvider.unbindAll();

                mCameraProvider.bindToLifecycle(this, mCameraSelector, mPreview, mImageCapture);

                mResolutionInfo = mPreview.getResolutionInfo();
                if (mResolutionInfo != null) {
                    mResolutionTextview.setText(mResolutionInfo.getResolution().toString());
                }

                ResolutionInfo info = mImageCapture.getResolutionInfo();
                assert info != null;
                Size value = info.getResolution();
                Log.d(LOG_TAG, "resolution selected width: " + value.getWidth() + " and height: " + value.getHeight());

            } catch (ExecutionException | InterruptedException e) {
                Log.e(LOG_TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Preview getPreview() {
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
        return preview;
    }

    private void setImageCapture(Resolution resolution) {
        ResolutionStrategy strategy = new ResolutionStrategy(new Size(resolution.getWidth(), resolution.getHeight()),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER);
        AspectRatioStrategy aspectRatioStrategy = new AspectRatioStrategy(resolution.getAspectRatio(), AspectRatioStrategy.FALLBACK_RULE_AUTO);
        ResolutionSelector selector = new ResolutionSelector.Builder().setResolutionStrategy(strategy).setAspectRatioStrategy(aspectRatioStrategy).build();
        ImageCapture.Builder builder = new ImageCapture.Builder().setResolutionSelector(selector);

        mImageCapture = builder.build();
    }

    public void toggleVisibility(View view) {
        if (mSettingLayout.getVisibility() == View.INVISIBLE) {
            mSettingLayout.setVisibility(View.VISIBLE);
        } else {
            mSettingLayout.setVisibility(View.INVISIBLE);
        }
    }

    public void takePicture(View view) {

        if (mImageCapture == null) {
            return;
        }

        String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/STCamera-Image");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();


        mImageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                String msg = "Photo capture succeeded: " + outputFileResults.getSavedUri();
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                Log.d(LOG_TAG, msg);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(LOG_TAG, "Photo capture failed: " + exception.getMessage(), exception);
            }
        });
    }

    public void openPictureList(View view) {
        Intent intent = new Intent(MainActivity.this, PictureListActivity.class);
        startActivity(intent);
    }

    public void selectResolution1(View view) {
        changeResolution(Resolution.resolution1);
    }

    public void selectResolution2(View view) {
        changeResolution(Resolution.resolution2);
    }

    public void selectResolution3(View view) {
        changeResolution(Resolution.resolution3);
    }

    public void selectResolution4(View view) {
        changeResolution(Resolution.resolution4);
    }

    public void selectResolution5(View view) {
        changeResolution(Resolution.resolution5);
    }

    public void changeResolution(Resolution resolution) {
        mCameraProvider.unbind(mImageCapture);
        setImageCapture(resolution);
        mCameraProvider.bindToLifecycle(this, mCameraSelector, mPreview, mImageCapture);
    }

    public void resetSettings(View view) {
        mRadioButtonResolution1.performClick();
        if (mDisplayPreviewResolution.isChecked()) {
            mDisplayPreviewResolution.performClick();
        }
    }
}