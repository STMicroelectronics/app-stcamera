package com.st.camera.adapter;

import android.net.Uri;

public class PictureModel {
    private final Uri imageUri;

    public PictureModel(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public Uri getImageUri() {
        return imageUri;
    }
}
