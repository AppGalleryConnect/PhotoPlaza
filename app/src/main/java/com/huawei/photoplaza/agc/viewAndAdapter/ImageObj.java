package com.huawei.photoplaza.agc.viewAndAdapter;

import android.graphics.Bitmap;
import android.net.Uri;

public class ImageObj {

    private String userId;
    private String photoID;
    private String imageUrl;
    private String userName;
    private Bitmap imageRes;
    private String createTime;

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    private String  localPath;

    public ImageObj(String userId, String photoID, String imageUrl, String userName, String createTime) {
        this.userId = userId;
        this.photoID = photoID;
        this.imageUrl = imageUrl;
        this.userName = userName;
//        this.imageRes = imageRes;
        this.createTime = createTime;
    }

    public ImageObj(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public ImageObj(Bitmap imageRes) {
        this.imageRes = imageRes;
    }
//    -----------------------------------------------------------------


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


    public String getPhotoID() {
        return photoID;
    }

    public void setPhotoID(String photoID) {
        this.photoID = photoID;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Bitmap getImageRes() {
        return imageRes;
    }

    public void setImageRes(Bitmap imageRes) {
        this.imageRes = imageRes;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "ImageObj{" +
                "imageOwnerId='" + userId + '\'' +
                ", imageID='" + photoID + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", imageDes='" + userName + '\'' +
                ", imageRes=" + imageRes +
                '}';
    }
}
