package com.huawei.agc.photoplaza.storageAction;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.huawei.agc.photoplaza.callbacklist.Icallback;
import com.huawei.agc.photoplaza.viewAndAdapter.UtilTool;
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.cloud.storage.core.AGCStorageManagement;
import com.huawei.agconnect.cloud.storage.core.DownloadTask;
import com.huawei.agconnect.cloud.storage.core.FileMetadata;
import com.huawei.agconnect.cloud.storage.core.StorageReference;
import com.huawei.agconnect.cloud.storage.core.UploadTask;
import com.huawei.hmf.tasks.Task;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Handle the Operation  of AGC CloudStorage
 *
 * @author x00454024
 * @since 2020-08-11
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class StorageAction {
    private static final String TAG = "StorageAction";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static AGCStorageManagement storageManagement;
    private static final String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    private static void initAGCStorageManagement() {
        storageManagement = AGCStorageManagement.getInstance();
    }

    /**
     * Uploading Images to CloudStorage and Inserting PhotoTables to CloudDB.
     *
     * @param upBt  input upload address
     * @param mcall iuput Icallback
     */

    public static void uploadImage(String upBt, final Icallback mcall) {
        if (storageManagement == null) {
            initAGCStorageManagement();
        }
        String fileName = UtilTool.getFileName();
        String upLoadPath = "images/" + fileName + ".jpg";
        FileMetadata fm = new FileMetadata();
        fm.setContentType("image/jpeg");
        StorageReference reference = storageManagement.getStorageReference(upLoadPath);
        UploadTask task = reference.putFile(new File(upBt), fm);
        task.addOnSuccessListener(uploadResult -> {
            Log.w(TAG, "UploadTask.success: ---" + fileName);
            reference.getDownloadUrl().addOnSuccessListener(uri -> {
                try {
                    mcall.onSuccess(uri.toString(), fileName + ".jpg");
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "getDownloadUrl.failed: ---" + e.getMessage());
                }
            });
        }).addOnFailureListener(exception -> {
            mcall.onFailure(exception.getMessage());
            Log.e(TAG, "UploadTask.failed: ---" + exception.getMessage());
        });
    }

    public static void getThumbnailUrl(String photoName, final Icallback mcall) {
        if (storageManagement == null) {
            initAGCStorageManagement();
        }
        String photoPatn = "thumbnail/" + photoName;

        StorageReference reference = storageManagement.getStorageReference(photoPatn);
        Task<Uri> task = reference.getDownloadUrl();
        task.addOnSuccessListener(uri -> {
            try {
                Log.i(TAG, "getThumbnailUrl.success: " + uri.toString());
                mcall.onSuccess(uri.toString(), "Success");
            } catch (FileNotFoundException e) {
                Log.e(TAG, "getThumbnailUrl.failed: " + e.getMessage());
            }
        });
        task.addOnFailureListener(e -> {
            Log.e(TAG, "getDownloadUrl.failed: ---" + e.getMessage());
            mcall.onFailure(e.getMessage());
        });
    }

    /**
     * Call reference.delete to delete Photo OnStorage.
     *
     * @param photoName input photoName
     */

    public static void deletePhotoOnStorage(String photoName) {
        if (storageManagement == null) {
            initAGCStorageManagement();
        }
        StorageReference reference = storageManagement.getStorageReference("images/" + photoName);
        StorageReference referenceShort = storageManagement.getStorageReference("thumbnail/" + photoName);
        referenceShort.delete();
        reference.delete();
    }

    public static String downLoadImage(String photoName) {
        Log.w("getImageBitmap:", "download file-------");
        String downLoadPath = "images/" + photoName + ".jpg";

        String localStoragePath = getAGCSdkDirPath() + downLoadPath;
        File file = new File(localStoragePath);

        if (!file.exists()) {
            if (storageManagement == null) {
                // Initializing CloudStorage Instance: 736430079244583900-12qji
                initAGCStorageManagement();
            }
            new Thread(() -> {
                StorageReference reference = storageManagement.getStorageReference(downLoadPath);
                Log.w(TAG, "local path:" + downLoadPath);
                // download task
                DownloadTask task = reference.getFile(file);
                task.addOnCompleteListener(task1 -> {
                    long fileSize = task1.getResult().getTotalByteCount();
                    Log.i("getImageBitmap:", "download file size:" + fileSize);
                }).addOnProgressListener(downloadResult ->{
                        Log.i("getImageBitmap:", "download file size:" + downLoadPath );
                        Log.i("getImageBitmap","download percent is " + (downloadResult.getBytesTransferred() / downloadResult.getTotalByteCount()) * 100);
                }).addOnFailureListener(e -> {
                    // download failed
                    Log.e("getImageBitmap:", "DownloadTask failed:" + e.getMessage());
                });
            }).start();
        }
        return localStoragePath;
    }

    /**
     * Dynamically apply for read and write permissions.
     */
    public static void verifyStoragePermissions(Activity activity) {
        try {
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getAGCSdkDirPath() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AGC/";
        Log.i(TAG, "Storage location path= " + path);
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }

}
