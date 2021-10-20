package com.huawei.agc.photoplaza;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.huawei.agc.photoplaza.appLinkingAction.AppLinkingAction;
import com.huawei.agc.photoplaza.authAction.AuthDeleteDialog;
import com.huawei.agc.photoplaza.callbacklist.Icallback;
import com.huawei.agc.photoplaza.cloudDBAction.CommentDBAction;
import com.huawei.agc.photoplaza.cloudDBAction.CouponDBAction;
import com.huawei.agc.photoplaza.cloudDBAction.CouponTableFields;
import com.huawei.agc.photoplaza.cloudDBAction.PhotoDBAction;
import com.huawei.agc.photoplaza.cloudDBAction.PhotoTableFields;
import com.huawei.agc.photoplaza.model.CouponTable;
import com.huawei.agc.photoplaza.model.PhotoTable;
import com.huawei.agc.photoplaza.model.ToastUtils;
import com.huawei.agc.photoplaza.viewAndAdapter.CircleImageView;
import com.huawei.agc.photoplaza.viewAndAdapter.CouponItem;
import com.huawei.agc.photoplaza.viewAndAdapter.ImageObj;
import com.huawei.agc.photoplaza.viewAndAdapter.RvAdapter;
import com.huawei.agc.photoplaza.viewAndAdapter.UtilTool;
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.auth.AGConnectAuthCredential;
import com.huawei.agconnect.auth.AGConnectUser;
import com.huawei.agconnect.auth.PhoneAuthProvider;
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery;

import java.util.ArrayList;

/**
 * PersonalDetailActivity, all photos of the user name are displayed.
 *
 * @since 2020-09-03
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class PersonalDetailActivity extends AppCompatActivity
        implements PhotoDBAction.PhotoUiCallBack, CouponDBAction.CouponUiCallBack, View.OnClickListener {
    public static final String TAG = "PersonalDetailPage";
    RvAdapter adapter;
    private RecyclerView rv;
    private TextView myPhotoAcc;
    private ImageButton deleteView;
    private Handler mHandler = null;
    private PhotoDBAction photoDBAction;
    private CommentDBAction commentDBAction;
    private CouponDBAction couponDBAction;
    private double couponSum;
    private int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDarkStatusIcon();
        setContentView(R.layout.personal_detail);
        mHandler = new Handler(Looper.getMainLooper());
        initView();
        photoDBAction = new PhotoDBAction();
        photoDBAction.addCallBacks(PersonalDetailActivity.this);
        photoDBAction.openCloudDBZone();

        commentDBAction = new CommentDBAction();
        commentDBAction.openCloudDBZone();

        mHandler.post(() -> {
            couponDBAction = new CouponDBAction();
            couponDBAction.addCallBacks(PersonalDetailActivity.this);
            couponDBAction.openCloudDBZone();
            queryBeInvited();
        });

        loadPersonalPhoto();
    }

    private void queryBeInvited() {
        String uid = UtilTool.currentLoginUid;
        CloudDBZoneQuery<CouponTable> queryInvite = CloudDBZoneQuery.where(CouponTable.class)
                .equalTo(CouponTableFields.Invite_UserID, uid);
        couponDBAction.queryUserCoupons(queryInvite);

        Runnable runnable = () -> {
            CloudDBZoneQuery<CouponTable> queryBeInvite = CloudDBZoneQuery.where(CouponTable.class)
                    .equalTo(CouponTableFields.BeInvited_UserID, UtilTool.currentLoginUid);
            couponDBAction.queryUserCoupons(queryBeInvite);
        };
        mHandler.postDelayed(runnable, 20);
    }

    /**
     * Initialize all control and views.
     */
    private void initView() {
        Log.i(TAG, "initView");
        CircleImageView headImage = findViewById(R.id.roundImageView);
        rv = findViewById(R.id.personalPhotoRecyclerView);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        myPhotoAcc = findViewById(R.id.personal_pho_Acc);
        TextView myUserName = findViewById(R.id.personal_name);
        myUserName.setText(UtilTool.currentUserName);
        ImageView myBackBtn = findViewById(R.id.person_back_view);
        myBackBtn.setOnClickListener(PersonalDetailActivity.this);
        TextView inviteBtn = findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(PersonalDetailActivity.this);
        Button deleteBtn = findViewById(R.id.delete_btn);
        deleteBtn.setOnClickListener(PersonalDetailActivity.this);
        if(UtilTool.LoginMod == 11){
            deleteBtn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Loading user's all Photo from CloudDB and CloudStorage.
     */
    private void loadPersonalPhoto() {
        mHandler.post(() -> {
            CloudDBZoneQuery<PhotoTable> query = CloudDBZoneQuery.where(PhotoTable.class)
                    .equalTo(PhotoTableFields.UserID, String.valueOf(UtilTool.currentLoginUid));
            photoDBAction.queryUserPhotos(query);
        });
    }

    /**
     * Calculate the total number of photos in a user.
     */
    private void setPhotoAcc(int account) {
        String data = getResources().getString(R.string.personal_pho);
        data = String.format(data, account);
        myPhotoAcc.setText(data);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(int position, ImageObj data);
    }

    /**
     * 定义RecyclerView选项单击事件的回调接口
     */
    public interface OnItemClickListener {
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
        if (view.getId() == R.id.invite_btn) {
            AppLinkingAction appLink = new AppLinkingAction();
            appLink.createInviteLinking(UtilTool.currentUserName, UtilTool.currentLoginUid, new Icallback() {
                @Override
                public void onSuccess(String result, String fileName) {
                    Log.i(TAG, "CreateAppLink:" + result);
                    Bundle bundle = new Bundle();
                    bundle.putString("inviteLinking", result);
                    Intent intent = new Intent(PersonalDetailActivity.this, InviteActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }

                @Override
                public void onFailure(String result) {
                    Log.e("AppLinking", "createAppLinking failed:" + result);
                    ToastUtils.showToast(PersonalDetailActivity.this, "createAppLinking failed:" + result);
                }
            });
        }
        if (view.getId() == R.id.delete_btn) {
            AlertDialog.Builder deleteUserConfirm = new AlertDialog.Builder(this);
            deleteUserConfirm.setPositiveButton("确认", (arg0, arg1) -> {
                createPayDialog();
            });
            deleteUserConfirm.setNegativeButton("取消", (arg0, arg1) -> {
                arg0.dismiss();
            });
            deleteUserConfirm.setMessage("确定注销并且离开我们吗？");
            deleteUserConfirm.setTitle("注销确认");
            deleteUserConfirm.show();
        }
    }
     private void createPayDialog() {
        final AuthDeleteDialog authDeleteDialog = new AuthDeleteDialog(this);
         authDeleteDialog.setPasswordCallback(password -> {
             if ("".equals(password)) {
                 Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show();
             } else {
                 String Account = UtilTool.loginAccount;
                 AGConnectUser user = AGConnectAuth.getInstance().getCurrentUser();
                 AGConnectAuthCredential phoneAuthCredential = PhoneAuthProvider.credentialWithPassword("86", Account, password);
                 user.reauthenticate(phoneAuthCredential).addOnSuccessListener(result->{
                     Log.i(TAG,"reAuthenticate success");
                     AGConnectAuth.getInstance().deleteUser();
                     authDeleteDialog.dismiss();
                     Log.i(TAG, "deleteUser from reAuthenticate ");
                     startActivity(new Intent(PersonalDetailActivity.this, LoginActivity.class));
                     finish();
                 }).addOnFailureListener(e->{
                     Log.e(TAG,"reAuthenticate failure" + e.getMessage());
                     Toast.makeText(this, "密码错误，请重试", Toast.LENGTH_SHORT).show();
                 });
             }
         });
         authDeleteDialog.clearPasswordText();
         authDeleteDialog.show();
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

    @Override
    public void onSubscribePhoto(ArrayList<ImageObj> photoTableList) {
    }

    @Override
    public void onAddOrQueryPhoto(ArrayList<ImageObj> photoTableList) {
        Log.d(TAG, "queryMyPhoto success. photoList size is:" + photoTableList.size());
        adapter = new RvAdapter(PersonalDetailActivity.this, photoTableList, rv);
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
                deleteView = view.findViewById(R.id.delete);
                if (deleteView.getVisibility() != View.VISIBLE) {
                    deleteView.setVisibility(View.VISIBLE);
                }
            });
            adapter.setOnItemClickListener((view, position, data) -> {
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
        Log.e(TAG, "PersonalPage" + errorMessage);
    }

    @Override
    public void onAddOrQueryCoupon(ArrayList<CouponItem> CouponTableList) {
        count++;
        for (int i = 0; i < CouponTableList.size(); i++) {
            CouponItem couponItem = CouponTableList.get(i);
            couponSum = couponSum + Float.parseFloat(couponItem.getCoupon());
        }
        Log.d(TAG, "couponAll： " + couponSum);
        if (count == 2) {
            TextView allCoupons = findViewById(R.id.coupons_text);
            allCoupons.setText("礼品券：" + couponSum + "元");
            TextView allBalance = findViewById(R.id.balance_text);
            allBalance.setText("余额： 0.0元");
        }
    }

    @Override
    public void onSubscribeCoupon(ArrayList<CouponItem> CouponTableList) {

    }

    @Override
    public void onUpsertCoupon(Integer cloudDBZoneResult) {

    }

    @Override
    public void onCouponErrorMessage(String errorMessage) {
        Log.e(TAG, "PersonalPage" + errorMessage);
    }

}