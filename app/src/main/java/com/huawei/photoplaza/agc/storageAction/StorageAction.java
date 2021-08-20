package com.huawei.photoplaza.agc.storageAction;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.cloud.storage.core.AGCStorageManagement;
import com.huawei.agconnect.cloud.storage.core.FileMetadata;
import com.huawei.agconnect.cloud.storage.core.StorageReference;
import com.huawei.agconnect.cloud.storage.core.UploadTask;
import com.huawei.photoplaza.agc.callbacklist.Icallback;
import com.huawei.photoplaza.agc.viewAndAdapter.UtilTool;

import java.io.File;
import java.io.FileNotFoundException;
/**
 * Handle the Operation  of AGC CloudStorage
 *
 * @author x00454024
 * @since 2020-08-11
 *
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class StorageAction {
    private static String albumFolder = "images/";
    private static String TAG = "StorageAction";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    /**
     * Uploading Images to CloudStorage and Inserting PhotoTables to CloudDB.
     *
     * @param upBt input upload address
     * @param mcall iuput Icallback
     */
    public static void uploadImage(String upBt, final Icallback mcall) {
        if (isLogin()) {
            // 初始化云存储实例对象
            AGCStorageManagement storageManagement = AGCStorageManagement.getInstance();
            String fileName = UtilTool.getFileName();
            String upLoadPatn = albumFolder + fileName + ".jpg";
            FileMetadata fm = new FileMetadata();
            fm.setContentType("image/jpeg");
            // 创建云存储文件存放引用对象
            StorageReference reference = storageManagement.getStorageReference(upLoadPatn);
            //  开启文件上传任务， 读取本地文件至云存储
            UploadTask task = reference.putFile(new File(upBt), fm);
            task.addOnFailureListener(exception -> mcall.onFailure(exception.getMessage())).addOnSuccessListener(uploadResult -> {
                Log.e(TAG, "UploadTask.failed--: ---" + fileName);
                // 获取图片在云存储中的URL，用于展示图片
                reference.getDownloadUrl().addOnSuccessListener(uri -> {
                    try {
                        mcall.onSuccess(uri.toString(),fileName + ".jpg");
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                });
            });
        } else {
            System.out.println("no user signed");
        }
    }

    /**
     * Call reference.delete to delete Photo OnStorage.
     *
     * @param photoName  input photoName
     */
    public static void deletePhotoOnStorage(String photoName) {
        AGCStorageManagement storageManagement = AGCStorageManagement.getInstance();
        StorageReference reference = storageManagement.getStorageReference("images/"+photoName);
        reference.delete();
    }

    /**
     * Check whether to log in.
     */
    private static Boolean isLogin() {
        if (AGConnectAuth.getInstance().getCurrentUser() != null) {
            System.out.println("already signin a user");
            return true;
        }
        return false;
    }


    /**
     * Dynamically apply for read and write permissions.
     */
    public static void verifyStoragePermissions(Activity activity) {
        try {
            // 检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
