package com.huawei.agc.photoplaza;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.huawei.agconnect.cloud.database.CloudDBZoneQuery;
import com.huawei.agc.photoplaza.cloudDBAction.CouponDBAction;
import com.huawei.agc.photoplaza.cloudDBAction.CouponTableFields;
import com.huawei.agc.photoplaza.model.CouponTable;
import com.huawei.agc.photoplaza.model.ToastUtils;
import com.huawei.agc.photoplaza.viewAndAdapter.CouponAdapterBeIn;
import com.huawei.agc.photoplaza.viewAndAdapter.CouponAdapterIn;
import com.huawei.agc.photoplaza.viewAndAdapter.CouponItem;
import com.huawei.agc.photoplaza.viewAndAdapter.UtilTool;

import java.util.ArrayList;

/**
 * InviteActivity lists the invite record and give a invite Link.
 *
 * @since 2021-08-20
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class InviteActivity extends AppCompatActivity
        implements CouponDBAction.CouponUiCallBack, View.OnClickListener {

    private static final String TAG = "InvitePage";
    private CouponAdapterBeIn adapterBeIn;
    private CouponAdapterIn adapterIn;
    private ListView listview;
    private TextView inviteLinkForShow;
    private Handler mHandler = null;
    private String inviteLinking;
    private CouponDBAction couponDBAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.invitation_page);
        mHandler = new Handler(Looper.getMainLooper());
        Bundle data = getIntent().getExtras();
        initView();
        if (data != null) {
            inviteLinking = data.getString("inviteLinking");
            inviteLinkForShow.setText(inviteLinking);
        }
        queryBeInvited();
    }

    private void initView() {
        inviteLinkForShow = findViewById(R.id.invite_link);
        TextView myUserName = findViewById(R.id.personal_name);
        myUserName.setText(UtilTool.currentUserName);

        mHandler.post(() -> {
            couponDBAction = new CouponDBAction();
            couponDBAction.addCallBacks(InviteActivity.this);
            couponDBAction.openCloudDBZoneV2(UtilTool.currentLoginUid);
        });

        ImageView myBackBtn = findViewById(R.id.person_back_view);
        myBackBtn.setOnClickListener(InviteActivity.this);

        Button copy_Btn = findViewById(R.id.copy_btn);
        copy_Btn.setOnClickListener(InviteActivity.this);

        Button share_Btn = findViewById(R.id.share_btn);
        share_Btn.setOnClickListener(InviteActivity.this);
    }

    private void queryBeInvited() {
        Runnable runnable = () -> {
            // Querying Be Invited Data
            CloudDBZoneQuery<CouponTable> query = CloudDBZoneQuery.where(CouponTable.class)
                    .equalTo(CouponTableFields.BeInvited_UserID, UtilTool.currentLoginUid);
            couponDBAction.queryUserCoupons(query);
        };
        mHandler.postDelayed(runnable, 50);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        mHandler.post(() -> couponDBAction.closeCloudDBZone());
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.person_back_view) {
            onBackPressed();
        }
        if (view.getId() == R.id.copy_btn) {
            ClipboardManager cm = (ClipboardManager) getSystemService(InviteActivity.CLIPBOARD_SERVICE);
            ClipData mClipData = ClipData.newPlainText("Label", inviteLinking);
            cm.setPrimaryClip(mClipData);
            ToastUtils.showToast(InviteActivity.this, "已将链接复制到剪切板");
        }
        if (view.getId() == R.id.share_btn) {
            if (inviteLinking != null) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, inviteLinking);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                ToastUtils.showToast(InviteActivity.this, "AppLinking为空");
            }
        }
    }

    @Override
    public void onAddOrQueryCoupon(ArrayList<CouponItem> CouponTableList) {
        Log.i(TAG, "load be Invited ItemsList");
        adapterBeIn = new CouponAdapterBeIn(InviteActivity.this,
                R.layout.item_be_invited, CouponTableList);
        listview = findViewById(R.id.beInvited_list);
        mHandler.post(() -> {
            listview.setAdapter(adapterBeIn);
            adapterBeIn.notifyDataSetChanged();
        });
    }

    @Override
    public void onSubscribeCoupon(ArrayList<CouponItem> CouponTableList) {
        Log.i(TAG, "load Invite other ItemsList ");
        adapterIn = new CouponAdapterIn(InviteActivity.this,
                R.layout.item_invite, CouponTableList);
        listview = findViewById(R.id.invited_list);
        mHandler.post(() -> {
            listview.setAdapter(adapterIn);
            adapterIn.notifyDataSetChanged();
        });
    }

    @Override
    public void onUpsertCoupon(Integer cloudDBZoneResult) {

    }

    @Override
    public void onCouponErrorMessage(String errorMessage) {
        Log.e(TAG, "InvitePage: " + errorMessage);
        ToastUtils.showToast(this, "InvitePage: " + errorMessage);
    }
}