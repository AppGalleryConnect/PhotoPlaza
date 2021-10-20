package com.huawei.agc.photoplaza;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.huawei.agc.photoplaza.callbacklist.Icallback;
import com.huawei.agc.photoplaza.cloudDBAction.PhotoDBAction;
import com.huawei.agc.photoplaza.model.PhotoTable;
import com.huawei.agc.photoplaza.model.ToastUtils;
import com.huawei.agc.photoplaza.storageAction.StorageAction;
import com.huawei.agc.photoplaza.viewAndAdapter.ImageObj;
import com.huawei.agc.photoplaza.viewAndAdapter.RvAdapter;
import com.huawei.agc.photoplaza.viewAndAdapter.UtilTool;
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.function.AGConnectFunction;
import com.huawei.agconnect.function.FunctionCallable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * ImageListActivity lists and displays all photos.
 *
 * @since 2020-09-03
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class ImageListActivity extends AppCompatActivity
        implements PhotoDBAction.PhotoUiCallBack, View.OnClickListener {

    private static final int PHOTO_FROM_GALLERY = 1001;
    private static final String TAG = "ImageListPage";

    ImageView imageShow;
    ArrayList<ImageObj> imageList;
    RvAdapter adapter;
    private RecyclerView rv;
    private long firstTime;
    private Handler mHandler = null;
    private PhotoDBAction photoDBAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDarkStatusIcon();
        setContentView(R.layout.activity_my_photo);
        mHandler = new Handler(Looper.getMainLooper());

        Intent loginIntent = getIntent();
        UtilTool.currentUserName = loginIntent.getStringExtra("UserName");
        UtilTool.currentLoginUid = loginIntent.getStringExtra("uid");

        initView();
        imageList = new ArrayList<>();
    }

    /**
     * Initialize all control and views.
     */
    private void initView() {
        findViewById(R.id.add_photo).setOnClickListener(ImageListActivity.this);
        findViewById(R.id.refresh_photo).setOnClickListener(ImageListActivity.this);
        findViewById(R.id.login_out).setOnClickListener(ImageListActivity.this);
        imageShow = findViewById(R.id.item_imageview);
        TextView curr_userName = findViewById(R.id.curr_user);

        Log.i(TAG, "UserName: " + UtilTool.currentUserName);
        curr_userName.setText(UtilTool.currentUserName);

        curr_userName.setOnClickListener(ImageListActivity.this);

        rv = findViewById(R.id.myPhotoRecyclerView);
        // Setting the Layout Manager
        rv.setLayoutManager(new GridLayoutManager(this, 3));

        mHandler.post(() -> {
            photoDBAction = new PhotoDBAction();
            photoDBAction.addCallBacks(ImageListActivity.this);
            photoDBAction.openCloudDBZoneV2();
        });
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.add_photo:
                selectAndUpload();
                break;
            case R.id.refresh_photo:
                ToastUtils.showToast(this, getResources().getString(R.string.loading_photo));
                photoDBAction.queryAllPhotos();
                break;
            case R.id.login_out:
                AGConnectAuth.getInstance().signOut();
                startActivity(new Intent(ImageListActivity.this, LoginActivity.class));
                mHandler.post(() -> {
                    photoDBAction.closeCloudDBZone();
                });
                finish();
                break;
            case R.id.curr_user:
                Intent intent = new Intent(ImageListActivity.this, PersonalDetailActivity.class);
                startActivity(intent);
            default:
                break;
        }
    }

    /**
     * upsert Photo data.
     *
     * @param photoURL  input photoURL from CloudStorage
     * @param photoName input photoName from CloudStorage
     */
    private void upsertPhotoData(String photoURL, String photoName) {
        StorageAction.getThumbnailUrl(photoName, new Icallback() {
            @Override
            public void onSuccess(String result, String fileName) {
                PhotoTable CurrPhoto = photoDBAction.buildPhotoTable(
                        UtilTool.currentLoginUid, UtilTool.currentUserName, photoURL, photoName, result);
                photoDBAction.upsertPhotoTables(CurrPhoto);
            }

            @Override
            public void onFailure(String result) {
                Log.e(TAG, "photo_upLoadImage_fail: " + result);
                ToastUtils.showToast(ImageListActivity.this, "Upload filed: " + result);
            }
        });
    }

    /**
     * Select a local photo and start to upload.
     */
    private void selectAndUpload() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PHOTO_FROM_GALLERY);
    }

    /**
     * Callback for obtaining and uploading Photo.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ContentResolver resolver = getContentResolver();
        if (requestCode == PHOTO_FROM_GALLERY) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    uploadSelectFile(resolver, uri);
                }
            }
        }
    }

    private void uploadSelectFile(ContentResolver resolver, Uri uri) {
        try {
            Bitmap photoBmp = MediaStore.Images.Media.getBitmap(resolver, uri);
            ToastUtils.showToast(this, getResources().getString(R.string.uploading_photo));
            Log.w(TAG, UtilTool.getPathFromUri(this, uri));
            StorageAction.uploadImage(UtilTool.getPathFromUri(this, uri), new Icallback() {
                @Override
                public void onSuccess(String result, String fileName) {
                    triggerFunction(result, fileName);
                }

                @Override
                public void onFailure(String result) {
                    Log.e(TAG, "photo_upLoadImage_fail: " + result);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "photo_upLoadImage_fail: " + e.getMessage());
            ToastUtils.showToast(this, "Upload filed: " + e.getMessage());
        }
    }

    private void triggerFunction(String result, String fileName) {
        Log.i(TAG, "imageOriginalUrl--: " + result);
        HashMap map = new HashMap();
        map.put("key", "images/" + fileName);
        AGConnectFunction function = AGConnectFunction.getInstance();
        FunctionCallable functionCallable = function.wrap("image-process-$latest");
        functionCallable.setTimeout(20000, TimeUnit.MILLISECONDS);
        functionCallable.call(map).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.i(TAG, "wrap Function success: " + task.getResult().getValue());
                ToastUtils.showToast(ImageListActivity.this, "云函数处理完成，图片已压缩");
                // Inserts image data into the CloudDB.
                upsertPhotoData(result, fileName);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "wrap Cloud Function failed: " + e.getMessage());
            ToastUtils.showToast(ImageListActivity.this, "wrap Cloud Function failed: " + e.getMessage());
        });
    }

    private void refreshPhoto(ArrayList<ImageObj> photoTableList) {
        adapter = new RvAdapter(ImageListActivity.this, photoTableList, rv);

        adapter.setOnItemClickListener((view, position, data) -> {
            Log.i(TAG, "position:" + position + ":" + data.getImageUrl());
            Bundle bundle = new Bundle();
            bundle.putString("imageUri", data.getImageUrl());
            bundle.putString("userName", data.getUserName());
            bundle.putString("photoID", data.getPhotoID());
            bundle.putString("createTime", data.getCreateTime());
            bundle.putBoolean("firstLink", false);
            Intent intent = new Intent(ImageListActivity.this, ImageDetailActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        });
        mHandler.post(() -> {
            rv.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onBackPressed() {
        long secondTime = System.currentTimeMillis();
        if (secondTime - firstTime > 2000) {
            ToastUtils.showToast(this, getResources().getString(R.string.exit_app));
            firstTime = secondTime;
        } else {
            finish();
        }
    }

    @Override
    public void onAddOrQueryPhoto(ArrayList<ImageObj> photoTableList) {
        Log.i(TAG, "Update Photo from query");
        refreshPhoto(photoTableList);
    }

    @Override
    public void onSubscribePhoto(ArrayList<ImageObj> photoTableList) {
        Log.i(TAG, "data changed, Update Photo from onSubscribe");
        refreshPhoto(photoTableList);
    }

    @Override
    public void onPhotoErrorMessage(String errorMessage) {
        Log.e(TAG, "ListPage: " + errorMessage);
    }

    /**
     * Defines the callback interface for the RecyclerView option click event.
     */
    public interface OnItemClickListener {
        void onItemClick(View view, int position, ImageObj data);
    }

    /**
     * Set the status bar color inversion.
     */
    public void setDarkStatusIcon() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

}