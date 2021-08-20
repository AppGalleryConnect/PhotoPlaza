package com.huawei.photoplaza.agc;

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
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.photoplaza.agc.callbacklist.Icallback;
import com.huawei.photoplaza.agc.dbAction.PhotoDBAction;
import com.huawei.photoplaza.agc.model.PhotoTable;
import com.huawei.photoplaza.agc.model.ToastUtils;
import com.huawei.photoplaza.agc.viewAndAdapter.ImageObj;
import com.huawei.photoplaza.agc.viewAndAdapter.RvAdapter;
import com.huawei.photoplaza.agc.viewAndAdapter.UtilTool;
import java.io.IOException;
import java.util.ArrayList;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.huawei.photoplaza.agc.storageAction.StorageAction;

import static com.huawei.photoplaza.agc.MainApplication.photoDBAction;

/**
 * ImageListActivity lists and displays all photos.
 *
 * @since 2020-09-03
 *
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class ImageListActivity extends AppCompatActivity
        implements PhotoDBAction.PhotoUiCallBack, View.OnClickListener {
    private static final int PHOTO_FROM_GALLERY = 1001;
    private static final String TAG = "ImageListPage";
    public static String uid;
    public static String accountNumber;
    // 先定义动态获取文件读写权限
    ImageView imageShow;
    ArrayList<ImageObj> imageList;
    RvAdapter adapter;
    private RecyclerView rv;
    private long firstTime;
    private Handler mHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDarkStatusIcon();
        setContentView(R.layout.activity_my_photo);
        mHandler = new Handler(Looper.getMainLooper());
        Bundle data = getIntent().getExtras();
        if (data != null) {
            uid = data.getString("uid");
            accountNumber = data.getString("account");
        }
        mHandler = new Handler(Looper.getMainLooper());
        initView();
        // 初始化图片文件对象集合
        imageList = new ArrayList<ImageObj>();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent loginIntent = getIntent();
        if (loginIntent.getStringExtra("uid") != null) {
            UtilTool.currentAccount = loginIntent.getStringExtra("account");
            UtilTool.currentLoginUid = loginIntent.getStringExtra("uid");
            Log.i(TAG + "onStart", "currentAccount: " + UtilTool.currentAccount + "&--&" + "currentLoginUid: " + UtilTool.currentLoginUid);
        }
        // 应用打开或者跳转到此页面时，加载网络数据图片并且填充
        photoDBAction.addCallBacks(ImageListActivity.this);
        photoDBAction.openCloudDBZoneV2();
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
        curr_userName.setText(accountNumber);
        curr_userName.setOnClickListener(ImageListActivity.this);

        rv = findViewById(R.id.myPhotoRecyclerView);
        // 设置布局管理器
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        TextView reusult_show = findViewById(R.id.result_callback);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.add_photo:
                // 用户选择图片后，向CloudDB中插入用户图片数据
                selectAndUpload();
                break;
            case R.id.refresh_photo:
                // 刷新CloudDB中的照片数据
                ToastUtils.showToast(this, getResources().getString(R.string.loading_photo) );
                photoDBAction.queryAllPhotos();
                break;
            case R.id.login_out:
                AGConnectAuth.getInstance().signOut();
                startActivity(new Intent(ImageListActivity.this, LoginActivity.class));
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
     * @param photoURL input photoURL from CloudStorage
     * @param photoName input photoName from CloudStorage
     */
    private void upsertPhotoData(String photoURL, String photoName) {
        // 拼装PhotoTable，用于插入数据
        PhotoTable CurrPhoto = photoDBAction.buildPhotoTable(uid, accountNumber, photoURL,photoName);
        Log.i(TAG, "this is CurrPhototable"+ CurrPhoto);
        // 向PhotoZone插入拼装好的PhotoTable数据
        photoDBAction.upsertPhotoTables(CurrPhoto);
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
        Bitmap photoBmp = null;
        ContentResolver resolver = getContentResolver();
        if (requestCode == PHOTO_FROM_GALLERY) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        photoBmp = MediaStore.Images.Media.getBitmap(resolver, uri);
                        ToastUtils.showToast(this, getResources().getString(R.string.uploading_photo) );
                        Log.w("photo_pasrcPathth:", UtilTool.getPathFromUri(this, uri));
                        // 此处获取到了数据的uri，可以进行上传
                        StorageAction.uploadImage(UtilTool.getPathFromUri(this, uri), new Icallback() {
                            @Override
                            public void onSuccess(String result,String fileName) {
                                Log.i(TAG, "imageUrl--: " + result);
                                // 向CloudDB插入图片数据
                                upsertPhotoData(result, fileName);
                            }
                            @Override
                            public void onFailure(String result) {
                                // 提示图片上传失败，或者网络错误码
                                Log.e(TAG, "photo_upLoadImage_fail: " + result);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    @Override
    public void onBackPressed() {
        long secondTime = System.currentTimeMillis();
        if (secondTime - firstTime > 2000) {
            ToastUtils.showToast(this, getResources().getString(R.string.exit_app));
            firstTime = secondTime;
        } else {
            //  System.exit(0);
            finish();
        }
    }

    @Override
    public void onAddOrQueryPhoto(ArrayList<ImageObj> photoTableList) {
        Log.i(TAG, "Update Photo from query");
        adapter = new RvAdapter(ImageListActivity.this, photoTableList, rv);
        adapter.setOnItemClickListener((view, position, data) -> {
            //  item 点击事件触发后 可以传递imageobj对象数据至下一个评论界面
            Log.e("adpter itemclick", "position:" + position + "__" + data.getImageUrl());
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
    public void onSubscribePhoto(ArrayList<ImageObj> photoTableList) {
        Log.i(TAG, "Update Photo from onSubscribe");
        adapter = new RvAdapter(ImageListActivity.this, photoTableList, rv);
        adapter.setOnItemClickListener((view, position, data) -> {
            //  item 点击事件触发后 可以传递imageobj对象数据至下一个评论界面
            Log.e("adpter itemclick", "position:" + position + "__" + data.getImageUrl());
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
    public void onPhotoErrorMessage(String errorMessage) {
        ToastUtils.showToast(this, errorMessage);
    }

    /**
     * Defines the callback interface for the RecyclerView option click event.
     */
    public interface OnItemClickListener {
        //参数（当前单击的View,单击的View的位置，数据）
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
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

}