package com.huawei.agc.photoplaza;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.agc.photoplaza.appLinkingAction.AppLinkingAction;
import com.huawei.agc.photoplaza.authAction.AuthRegisterAction;
import com.huawei.agc.photoplaza.cloudDBAction.CouponDBAction;
import com.huawei.agc.photoplaza.model.CouponTable;
import com.huawei.agc.photoplaza.model.ToastUtils;
import com.huawei.agc.photoplaza.viewAndAdapter.CouponItem;
import com.huawei.agc.photoplaza.viewAndAdapter.UtilTool;
import com.huawei.agconnect.auth.SignInResult;
import com.huawei.agconnect.auth.VerifyCodeResult;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.huawei.agc.photoplaza.MainApplication.instance;

/**
 * RegisterActivity, registering a HuaWei-authenticated user.
 *
 * @since 2020-09-03
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class RegisterActivity extends AppCompatActivity
        implements AuthRegisterAction.AuthReCallBack, CouponDBAction.CouponUiCallBack {

    private static final String TAG = "RegisterPage";
    EditText verifyCodeEditText;
    private EditText accountEditText;
    private EditText secretCodeEditText;
    private String inviteUserName;
    private String couponPrice;
    private String inviteUserID;
    private Boolean openFromLink = false;
    private Handler mHandler = null;
    private Bundle data;
    private CouponDBAction couponDBAction;
    private AuthRegisterAction authRegisterAction;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDarkStatusIcon();
        setContentView(R.layout.register_layout);
        mHandler = new Handler(Looper.getMainLooper());
        initView();
        judgeLink();
    }

    private void initView() {
        accountEditText = findViewById(R.id.regist_accout_nmm);
        verifyCodeEditText = findViewById(R.id.regist_verify_code);
        secretCodeEditText = findViewById(R.id.regist_secret_code);
        authRegisterAction = new AuthRegisterAction();
        authRegisterAction.addCallBacks(RegisterActivity.this);
    }

    private void judgeLink() {
        Bundle data = getIntent().getExtras();
        if (data != null) {
            Log.i(TAG, "register from AppLinking");
            // Start for the first time after installation via the App Linking
            openFromLink = data.getBoolean("firstLink");
            inviteForRegister(data.getString("deepLink"));
        } else {
            // Register normally.
            ButterKnife.bind(this);
            Log.i(TAG, "normal startUp and register");
        }
    }

    private void inviteForRegister(String deepLink) {
        Log.i(TAG, "AppLinking deepLink: " + deepLink);
        inviteUserID = AppLinkingAction.parseLink(deepLink).get("UserID");
        inviteUserName = AppLinkingAction.parseLink(deepLink).get("UserName");
        couponPrice = AppLinkingAction.parseLink(deepLink).get("CouponPrice");
        TextView show_invite = findViewById(R.id.invite_userName);
        show_invite.setVisibility(View.VISIBLE);
        show_invite.setText(getString(R.string.invate_person, inviteUserName));
        mHandler.post(() -> {
            couponDBAction = new CouponDBAction();
            couponDBAction.addCallBacks(RegisterActivity.this);
            couponDBAction.openCloudDBZone();
        });
        ButterKnife.bind(this);
    }

    @OnClick(R.id.back_view)
    public void onBackBtnPressed() {
        onBackPressed();
    }

    /**
     * Obtain Verify Button, to get a email verification_code from Auth.
     */
    @OnClick(R.id.verification_code_obtain)
    public void sendVerification() {
        String accountNumber = accountEditText.getText().toString().trim();
        boolean status = accountNumber.contains("@");
        if (status) {
            if ("".equals(accountNumber)) {
                ToastUtils.showToast(this, getResources().getString(R.string.empty_error));
            } else {
                authRegisterAction.sendEmailVerify(accountNumber);
            }
        } else {
            if ("".equals(accountNumber)) {
                ToastUtils.showToast(this, getResources().getString(R.string.empty_error));
            } else {
                authRegisterAction.sendPhoneVerify(accountNumber);
            }
        }
    }

    /**
     * Register Button, register a user with HuaWei Auth.
     */
    @OnClick(R.id.btn_register)
    public void register() {
        String accountNumber = accountEditText.getText().toString().trim();
        String verifyCode = verifyCodeEditText.getText().toString().trim();
        String password = secretCodeEditText.getText().toString().trim();
        authRegisterAction.registerAuth(accountNumber, verifyCode, password);
    }

    /**
     * Anonymous Button, register a anonymous user with HuaWei Auth.
     */
    @OnClick(R.id.btn_anonymous)
    public void onAnonymous() {
        Log.i(TAG, "Anonymous Login For test");
        authRegisterAction.anonymousLogin();
    }

    private void registerSuccess(SignInResult signInResult, Integer LoginMod) {
        Log.i(TAG, "registerSuccess");
        UtilTool.LoginMod = LoginMod;
        String uid = signInResult.getUser().getUid();
        String name;
        if (LoginMod != 0) {
            name = this.getString(R.string.agc_user) + uid.substring(14, 18);
        } else {
            name = this.getString(R.string.anonymous_user) + uid.substring(14, 18);
        }
        if (openFromLink) {
            mHandler.post(() -> {
                Log.i(TAG, "go to insert CouponTables");
                CouponTable couponTable = couponDBAction.buildCouponTable(
                        inviteUserID, inviteUserName, uid, name, Double.parseDouble(couponPrice));
                couponDBAction.upsertCouponTables(couponTable);
            });
        } else {
            if (LoginMod != 0) {
                Log.i(TAG, "report AuthSuccess events");
                Bundle bundleEvent = new Bundle();
                bundleEvent.putString("userID", uid);
                bundleEvent.putString("exam_time", name);
                instance.onEvent("AuthSuccess", bundleEvent);
            } else {
                ToastUtils.showToast(this, getString(R.string.anonymous_test));
            }
        }
        data = new Bundle();
        data.putString("UserName", name);
        data.putString("uid", uid);
        Intent intent = new Intent(RegisterActivity.this, ImageListActivity.class);
        intent.putExtras(data);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
    public void onAddOrQueryCoupon(ArrayList<CouponItem> CouponTableList) {
    }

    @Override
    public void onSubscribeCoupon(ArrayList<CouponItem> CouponTableList) {
    }

    @Override
    public void onUpsertCoupon(Integer cloudDBZoneResult) {
        Log.i(TAG, "UpsertSuccess " + cloudDBZoneResult + " records");
        ToastUtils.showToast(this, getString(R.string.signup_success));
        Intent intent = new Intent(RegisterActivity.this, ImageListActivity.class);
        intent.putExtras(data);
        startActivity(intent);
        finish();
    }

    @Override
    public void onCouponErrorMessage(String errorMessage) {
        Log.e(TAG, "RegisterPage: " + errorMessage);
    }

    @Override
    public void onAuthSuccess(SignInResult signInResult, Integer LoginMode) {
        authRegisterAction.tokenListener();
        registerSuccess(signInResult, LoginMode);
    }

    @Override
    public void onFailed(String FailedMessage) {
        Log.e(TAG, "RegisterPage: " + FailedMessage);
    }

    @Override
    public void onEmpty() {
        ToastUtils.showToast(this, getResources().getString(R.string.empty_error));
    }

    @Override
    public void onSendVerify(VerifyCodeResult verifyCodeResult) {
        ToastUtils.showToast(this, getString(R.string.verifycode_success));
    }
}
