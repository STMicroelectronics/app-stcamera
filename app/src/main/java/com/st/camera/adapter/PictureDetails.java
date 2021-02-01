package com.st.camera.adapter;

public class PictureDetails {
    private String mPictureName;
    private String mPicturePath;

    public void setPictureName(String pictureName) {
        this.mPictureName = pictureName;
    }

    String getPictureName(){
        return this.mPictureName;
    }

    public void setPicturePath(String picturePath) {
        this.mPicturePath = picturePath;
    }

    String getPicturePath() {
        return this.mPicturePath;
    }
}
