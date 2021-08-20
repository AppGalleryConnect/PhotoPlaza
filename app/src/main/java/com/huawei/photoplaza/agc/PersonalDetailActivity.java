package com.huawei.photoplaza.agc;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery;
import com.huawei.photoplaza.agc.dbAction.PhotoDBAction;
import com.huawei.photoplaza.agc.model.PhotoTable;
import com.huawei.photoplaza.agc.dbAction.PhotoTableFields;
import com.huawei.photoplaza.agc.model.ToastUtils;
import com.huawei.photoplaza.agc.viewAndAdapter.CircleImageView;
import com.huawei.photoplaza.agc.viewAndAdapter.ImageObj;
import com.huawei.photoplaza.agc.viewAndAdapter.RvAdapter;
import com.huawei.photoplaza.agc.viewAndAdapter.UtilTool;
import java.util.ArrayList;

import static com.huawei.photoplaza.agc.MainApplication.commentDBAction;
import static com.huawei.photoplaza.agc.MainApplication.photoDBAction;

/**
 * PersonalDetailActivity, all photos of the user name are displayed.
 *
 * @since 2020-09-03
 *
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class PersonalDetailActivity extends AppCompatActivity
        implements PhotoDBAction.PhotoUiCallBack, View.OnClickListener {
    public static final String TAG = "PersonalDetailActivity";
    RvAdapter adapter;
    private RecyclerView rv;
    private TextView myPhotoAcc;
    private TextView myUserName;
    private ImageView myBackBttn;
    private ImageButton deletView;
    private ArrayList<ImageObj> mList;
    private Handler mHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDarkStatusIcon();
        setContentView(R.layout.personal_detail);
        mHandler = new Handler(Looper.getMainLooper());
        initView();
        photoDBAction.addCallBacks(PersonalDetailActivity.this);
        // 加载图片
        loadPersonalPhoto();
    }

    /**
     * Loading user's all Photo from CloudDB and CloudStorage.
     */
    private void loadPersonalPhoto() {
        // 使用PhotoID构造equalTo查询条件
        CloudDBZoneQuery<PhotoTable> query = CloudDBZoneQuery.where(PhotoTable.class)
                .equalTo(PhotoTableFields.UserID, String.valueOf(UtilTool.currentLoginUid));
        // 根据PhotoID向CloudDB条件查询评论数据
        photoDBAction.queryUserPhotos(query);
    }

     /**
     * Calculate the total number of photos in a user.
     */
    private void setPhotoAcc(int account) {
        String data = getResources().getString(R.string.personal_pho);
        data = String.format(data, account);
        myPhotoAcc.setText(data);
    }

    /**
     * Initialize all control and views.
     */
    private void initView() {
        Log.i(TAG, "initview");
        CircleImageView headImage = findViewById(R.id.roundImageView);
        rv = findViewById(R.id.personalPhotoRecyclerView);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        myPhotoAcc = findViewById(R.id.personal_pho_Acc);
        myUserName = findViewById(R.id.personal_name);
        myUserName.setText(UtilTool.currentAccount);
        myBackBttn = findViewById(R.id.person_back_view);
        myBackBttn.setOnClickListener(PersonalDetailActivity.this);
    }

    public interface OnDeleteClickListener {
        // 参数（当前单击的View,单击的View的位置，数据）
        void onDeleteClick(int position, ImageObj data);
    }
    /**
     * 定义RecyclerView选项单击事件的回调接口
     */
    public interface OnItemClickListener {
        // 参数（当前单击的View,单击的View的位置，数据）
        void onItemClick(View view, int position, ImageObj data);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.person_back_view) {
            onBackPressed();
        }
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


    @Override
    public void onSubscribePhoto(ArrayList<ImageObj> photoTableList) {
    }

    @Override
    public void onAddOrQueryPhoto(ArrayList<ImageObj> photoTableList) {
        Log.d(TAG, "queryMyPhoto success. photoList size is" + photoTableList.size());
        mList= photoTableList;
        adapter = new RvAdapter(PersonalDetailActivity.this, mList, rv);
        setPhotoAcc(photoTableList.size());

        mHandler.post(() -> {
            rv.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            adapter.setOnDeleteClickListener((position, data) -> {
                String deletePhotoId = data.getPhotoID();
                photoDBAction.queryForDelete(deletePhotoId);
                commentDBAction.queryForDelete(deletePhotoId);
                ToastUtils.showToast(this, getResources().getString(R.string.delete_success));
            });
            adapter.setOnItemLongClickListener((view, position, data) -> {
                deletView = view.findViewById(R.id.delete);
                if (deletView.getVisibility() != View.VISIBLE) {
                    deletView.setVisibility(View.VISIBLE);
                }
            });
            adapter.setOnItemClickListener((view, position, data) -> {
                // item 点击事件触发后 可以传递imageobj对象数据至下一个评论界面
                Log.i("adpter itemclick", "position:" + position + "__" + data.getImageUrl());
                Bundle bundle = new Bundle();
                bundle.putString("imageUri", data.getImageUrl());
                bundle.putString("userName", data.getUserName());
                bundle.putString("photoID", data.getPhotoID());
                bundle.putString("createTime", data.getCreateTime());
                Intent intent = new Intent(PersonalDetailActivity.this, ImageDetailActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            });
        });
    }

    @Override
    public void onPhotoErrorMessage(String errorMessage) {
        ToastUtils.showToast(this, errorMessage);
    }

}