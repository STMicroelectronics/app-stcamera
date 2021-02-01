package com.st.camera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.st.camera.adapter.PictureAdapter;
import com.st.camera.adapter.PictureDetails;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements PictureAdapter.OnClickListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private FrameLayout mTextureFrame;
    private int mTextureFrameHeight = 0;
    private int mTextureFrameWidth = 0;
    private TextureView mTextureView;
    private Button mTakePictureButton;
    private Button mResetPictureListButton;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    protected CameraDevice mCameraDevice;
    protected CameraCaptureSession mCameraPreviewSession;
    protected CaptureRequest.Builder mCaptureRequestBuilder;

    private Size mImageDimension;
    private ImageReader mImageReader;
    private Handler mPreviewBackgroundHandler;
    private HandlerThread mPreviewBackgroundThread;
    private Handler mCaptureBackgroundHandler;
    private HandlerThread mCaptureBackgroundThread;

    private RecyclerView mPictureListView;
    private PictureAdapter mPictureAdapter;

    private static final double TEXTURE_RATIO = 0.8;

    private static final int MAX_INDEX = 2;
    private int mIndex = 1;

    private Boolean mCameraGranted = false;
    private Boolean mCameraRunningState = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextureFrame = findViewById(R.id.texture_frame);
        mTextureFrame.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTextureFrameHeight = mTextureFrame.getMeasuredHeight();
                mTextureFrameWidth = mTextureFrame.getMeasuredWidth();
                Log.d(LOG_TAG, "Texture FrameLayout (wxh) = " + mTextureFrameWidth + " + " + mTextureFrameHeight);
                mTextureFrame.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTextureView = findViewById(R.id.texture);
        assert mTextureView != null;
        mTextureView.setSurfaceTextureListener(textureListener);

        mTakePictureButton = findViewById(R.id.button_take_picture);
        mResetPictureListButton = findViewById(R.id.button_reset_picture_list);

        mPictureListView = findViewById(R.id.picture_list);

        initRecycler();

        checkPermission();
    }

    private static final int REQUEST_PERMISSION_STATE = 100;

    private void checkPermission() {
        // External storage access required to allow taking and displaying pictures
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            Log.d(LOG_TAG, "Permission denied : request it !");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                    REQUEST_PERMISSION_STATE);
        } else {
            Log.d(LOG_TAG, "Permission required (already) Granted!");
            mCameraGranted = true;
            mTextureView.setVisibility(View.VISIBLE);
            if (!mCameraRunningState) {
                startCamera();
                mCameraRunningState = true;
            }

            mTakePictureButton.setVisibility(View.VISIBLE);
            mResetPictureListButton.setVisibility(View.VISIBLE);
            initPictureList();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_STATE) {
            if (grantResults.length > 0) {
                int index = 0;
                for (String permission : permissions) {
                    if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE) && (grantResults[index] == PackageManager.PERMISSION_GRANTED)) {
                        initPictureList();
                    }
                    if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && (grantResults[index] == PackageManager.PERMISSION_GRANTED)) {
                        mTakePictureButton.setVisibility(View.VISIBLE);
                        mResetPictureListButton.setVisibility(View.VISIBLE);
                    }
                    if (permission.equals(Manifest.permission.CAMERA) && (grantResults[index] == PackageManager.PERMISSION_GRANTED)) {
                        mCameraGranted = true;
                        mTextureView.setVisibility(View.VISIBLE);
                        if (!mCameraRunningState) {
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

    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraGranted) {
            startCamera();
            mCameraRunningState = true;
        } else {
            // delay start of the camera to the permission request result
            mCameraRunningState = false;
        }
    }

    private void startCamera() {
        startPreviewBackgroundThread();
        startCaptureBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera();
        } else {
            mTextureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        if (mCameraGranted) {
            stopCamera();
            mCameraRunningState = false;
        }
        super.onPause();
    }

    private void stopCamera() {
        closeCamera();
        stopPreviewBackgroundThread();
        stopCaptureBackgroundThread();
    }

    private void initRecycler() {
        mPictureAdapter = new PictureAdapter(this);
        mPictureListView.setAdapter(mPictureAdapter);
        mPictureListView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        mPictureListView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d(LOG_TAG, "mPictureListView size = " + mPictureListView.getMeasuredWidth() + " x " + mPictureListView.getMeasuredHeight());
                mPictureAdapter.setMaxHeight(mPictureListView.getMeasuredHeight());
                if (mPictureListView.getLayoutManager() != null) {
                    mPictureListView.getLayoutManager().removeAllViews();
                    mPictureAdapter.notifyDataSetChanged();
                }
                mPictureListView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void initPictureList() {
        ArrayList<PictureDetails> pictureList = new ArrayList<>();

        File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if ((files != null) && (files.length > 0)) {
                mPictureAdapter.removeAllItems();
                for (File file : files) {
                    PictureDetails picture = new PictureDetails();
                    picture.setPictureName(file.getName());
                    picture.setPicturePath(file.getAbsolutePath());
                    pictureList.add(picture);
                }
                mPictureAdapter.addItems(pictureList);
            }
        }
    }

    private void updatePictureList(String name, String path) {
        PictureDetails picture = new PictureDetails();
        picture.setPictureName(name);
        picture.setPicturePath(path);
        mPictureAdapter.addItem(picture);
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(LOG_TAG, "Camera error: " + error);
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }
    };

    protected void startPreviewBackgroundThread() {
        mPreviewBackgroundThread = new HandlerThread("Camera Preview");
        mPreviewBackgroundThread.start();
        mPreviewBackgroundHandler = new Handler(mPreviewBackgroundThread.getLooper());
    }

    protected void stopPreviewBackgroundThread() {
        mPreviewBackgroundThread.quitSafely();
        try {
            mPreviewBackgroundThread.join();
            mPreviewBackgroundThread = null;
            mPreviewBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void startCaptureBackgroundThread() {
        mCaptureBackgroundThread = new HandlerThread("Camera Capture");
        mCaptureBackgroundThread.start();
        mCaptureBackgroundHandler = new Handler(mPreviewBackgroundThread.getLooper());
    }

    protected void stopCaptureBackgroundThread() {
        mCaptureBackgroundThread.quitSafely();
        try {
            mCaptureBackgroundThread.join();
            mCaptureBackgroundThread = null;
            mCaptureBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void takePicture(View view) {
        if (null == mCameraDevice) {
            Log.e(LOG_TAG, "mCameraDevice is null");
            return;
        }
        if (!checkExternalStorage()) {
            Log.e(LOG_TAG, "External Storage not available");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            assert manager != null;
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
            Size[] jpegSizes = null;
            if ((characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)) != null) {
                jpegSizes = Objects.requireNonNull(characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(ImageFormat.JPEG);
            }

            int width = 640;
            int height = 480;
            if (jpegSizes != null && mIndex < jpegSizes.length) {
                width = jpegSizes[mIndex].getWidth();
                height = jpegSizes[mIndex].getHeight();
                Log.d(LOG_TAG, "Take picture size " + width + " (width) x " + height + " (height)");
            }

            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);

            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Orientation
            int rotation;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                rotation = getDisplay().getRotation();
            } else {
                rotation = getWindowManager().getDefaultDisplay().getRotation();
            }
            Log.d(LOG_TAG, "Take picture orientation = " + ORIENTATIONS.get(rotation));
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            final File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "stcam_" + now2DateTime() + ".jpg");

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    try (Image image = reader.acquireLatestImage()) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updatePictureList(file.getName(), file.getAbsolutePath());
                                recreateCameraPreview();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    try (OutputStream output = new FileOutputStream(file)) {
                        output.write(bytes);
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mCaptureBackgroundHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                }
            };

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                List<OutputConfiguration> configurations = new ArrayList<>(2);
                configurations.add(new OutputConfiguration(reader.getSurface()));
                configurations.add(new OutputConfiguration(new Surface(mTextureView.getSurfaceTexture())));

                SessionConfiguration configuration = new SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        configurations,
                        getMainExecutor(),
                        new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                try {
                                    session.capture(captureBuilder.build(), captureListener, mCaptureBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {

                            }
                        });
                mCameraDevice.createCaptureSession(configuration);

            } else {
                List<Surface> outputSurfaces = new ArrayList<>(2);
                outputSurfaces.add(reader.getSurface());
                outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));

                mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            session.capture(captureBuilder.build(), captureListener, mCaptureBackgroundHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    }
                }, mCaptureBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void recreateCameraPreview() {
        Handler handler = new Handler(getMainLooper());
        handler.postDelayed(new  Runnable() {
            public void run () {
                createCameraPreview();
            }
        },1000);
    }

    protected Boolean checkExternalStorage() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this,"No external storage available. Please insert one, configured as portable device", Toast.LENGTH_LONG).show();
            return false;
        }

        File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (directory != null && !directory.exists()) {
            if (!directory.mkdir()) {
                Toast.makeText(this, "Not possible to create directory" + directory.getPath(), Toast.LENGTH_SHORT).show();
            }
        }
        return directory != null && directory.exists();
    }

    public void resetPictureList(View view) {
        // remove list of items in adapter
        mPictureAdapter.removeAllItems();

        // remove files from external storage
        File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if ((files != null) && (files.length > 0)) {
                for (File file : files) {
                    if (!file.delete()) {
                        Toast.makeText(this, "Can't delete file: " + file.getPath(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mImageDimension.getWidth(), mImageDimension.getHeight());
            Log.d(LOG_TAG,"Camera preview " + mImageDimension.getWidth() + " (width) x " + mImageDimension.getHeight() + " (height)");

            Surface mPreviewSurface = new Surface(texture);
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(mPreviewSurface);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                SessionConfiguration configuration = new SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        Collections.singletonList(new OutputConfiguration(mPreviewSurface)),
                        getMainExecutor(),
                        new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                //The camera is already closed
                                if (null == mCameraDevice) {
                                    return;
                                }
                                // When the session is ready, we start displaying the preview.
                                mCameraPreviewSession = session;
                                updatePreview();
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                                Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                            }
                        });
                mCameraDevice.createCaptureSession(configuration);
            } else {
                mCameraDevice.createCaptureSession(Collections.singletonList(mPreviewSurface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        //The camera is already closed
                        if (null == mCameraDevice) {
                            return;
                        }
                        // When the session is ready, we start displaying the preview.
                        mCameraPreviewSession = cameraCaptureSession;
                        updatePreview();
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                    }
                }, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            assert manager != null;
            String[] cameraList = manager.getCameraIdList();
            if (cameraList.length == 0) {
                Toast.makeText(getApplicationContext(),"No camera device available", Toast.LENGTH_SHORT).show();
                finish();
            }
            String cameraId = cameraList[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            if ((mTextureFrameHeight > 0) && (mTextureFrameWidth > 0)) {
                for (int i=MAX_INDEX; i>=0; i--) {
                    mImageDimension = map.getOutputSizes(SurfaceTexture.class)[i];
                    if ((mImageDimension.getHeight() <= mTextureFrameHeight) && (mImageDimension.getWidth() <= mTextureFrameWidth)) {
                        Log.d(LOG_TAG, "Preview Image Dimension (wxh) = " + mImageDimension.getWidth() + " + " + mImageDimension.getHeight());
                        mIndex = i;
                        break;
                    }
                }
            } else {
                // Force QVGA format
                mImageDimension = map.getOutputSizes(SurfaceTexture.class)[1];
            }

            // Initialize preview layout depending on Camera characteristics
            updatePreviewDisplayedResolution();

            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)  {
                Log.e(LOG_TAG,"Try to open camera while access permission has not been granted yet");
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreviewDisplayedResolution() {
        ViewGroup.LayoutParams layoutParams = mTextureView.getLayoutParams();
        layoutParams.width = (int) Math.round(mImageDimension.getWidth() * TEXTURE_RATIO);
        layoutParams.height = (int) Math.round(mImageDimension.getHeight() * TEXTURE_RATIO);
        mTextureView.setLayoutParams(layoutParams);
    }

    protected void updatePreview() {
        if(null == mCameraDevice) {
            Log.e(LOG_TAG, "updatePreview error, return");
        }
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            mCameraPreviewSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mPreviewBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private String now2DateTime () {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);
        return formatter.format(date);
    }

    @Override
    public void onAdded(int position) {
        mPictureListView.scrollToPosition(position);
    }

    @Override
    public void onClick(String pictureName, String picturePath) {
        Log.d(LOG_TAG,"Following picture clicked: " + picturePath);
        Intent intent = new Intent(getApplicationContext(), PictureActivity.class);
        intent.putExtra(PictureActivity.EXTRA_FILE_PATH, picturePath);
        intent.putExtra(PictureActivity.EXTRA_FILE_NAME, pictureName);
        startActivity(intent);
    }

}