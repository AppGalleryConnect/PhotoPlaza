package com.huawei.agc.photoplaza;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.huawei.agc.photoplaza.viewAndAdapter.UtilTool;
import com.huawei.agconnect.applinking.AGConnectAppLinking;
import com.huawei.agconnect.auth.AGConnectUser;
import com.huawei.agconnect.auth.SignInResult;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.agconnect.crash.AGConnectCrash;
import com.huawei.agconnect.remoteconfig.AGConnectConfig;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.aaid.entity.AAIDResult;
import com.huawei.hms.common.ApiException;
import com.huawei.agc.photoplaza.authAction.AuthLoginAction;
import com.huawei.agc.photoplaza.model.ToastUtils;
import com.huawei.agc.photoplaza.storageAction.StorageAction;

import androidx.appcompat.app.AppCompatActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * The MainActivity, reName to LoginActivity to log in.
 *
 * @since 2020-09-07
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class LoginActivity extends AppCompatActivity implements AuthLoginAction.AuthLoginCallBack {
    public static final String TAG = "LoginPage";
    EditText accountEditText;
    EditText passwordEditText;
    TextView slogan;
    private AGConnectConfig config;
    private Boolean anrTestEnable = false;
    private AuthLoginAction authLoginAction;
    public static String FALLBACK_URL = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDarkStatusIcon();
        setContentView(R.layout.activity_main);
        hmsInit();
        authLoginAction = new AuthLoginAction();
        authLoginAction.addCallBacks(LoginActivity.this);

        AGConnectAppLinking.getInstance().getAppLinking(this).addOnSuccessListener(resolvedLinkData -> {
            Log.i(TAG, "StartUp From AppLinking");
            if (resolvedLinkData != null) {
                String deepLink = resolvedLinkData.getDeepLink().toString();
                Log.i("AppLinking", "deepLink:" + deepLink);
                judgeLink(deepLink);
            }
        }).addOnFailureListener(e -> {
            Log.i(TAG, "Normal StartUp");
            initView();
            getRemoteConfig();
        });
    }

    private void judgeLink(String deepLink) {
        Bundle bundle = new Bundle();
        String linkPrefix = "share";
        bundle.putBoolean("firstLink", true);
        bundle.putString("deepLink", deepLink);

        if (deepLink.startsWith(linkPrefix)) {
            Intent intent = new Intent(LoginActivity.this, ImageDetailActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
            finish();
        } else {
            if (deepLink.startsWith("invite")) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
            } else {
                ToastUtils.showToast(this, "The link is a wrong App Linking");
            }
        }
    }

    private void hmsInit() {
        getAAID();
        getToken();
    }

    private void initView() {
        StorageAction.verifyStoragePermissions(this);
        accountEditText = findViewById(R.id.accout_nmm);
        passwordEditText = findViewById(R.id.secret_code);
        slogan = findViewById(R.id.welcome_slogan);
        ButterKnife.bind(this);
    }

    private void getRemoteConfig() {
        // Obtaining Remote Config Instances
        config = AGConnectConfig.getInstance();
        config.applyDefault(R.xml.remote_config);
        config.fetch(180).addOnSuccessListener(configValues -> {
            config.apply(configValues);
            String newSlogan = config.getValueAsString("welcome_slogan");
            Log.i(TAG, "RemoteConfig Success: " + newSlogan);
            slogan.setText(newSlogan);
            String newLink = config.getValueAsString("newDownLoadLink");
            Log.i(TAG, "RemoteConfig Success: " + newLink);
            FALLBACK_URL = newLink;
        }).addOnFailureListener(e1 -> Log.e(TAG, "getRemoteConfig failed: " + e1.getMessage()));
    }

    public void getAAID() {
        Task<AAIDResult> idResult = HmsInstanceId.getInstance(this).getAAID();
        idResult.addOnSuccessListener(aaidResult -> {
            String aaid = aaidResult.getId();
            Log.d(TAG, "getAAID successfully, aaid is " + aaid);
        }).addOnFailureListener(myException -> {
            Log.e(TAG, "getAAID failed, catch exception: " + myException);
        });
    }

    /**
     * Login button, which is used to perform pre-check on login.
     */
    @OnClick(R.id.btn_login)
    public void login() {
        String account = accountEditText.getText().toString().trim();
        String passWord = passwordEditText.getText().toString().trim();
        authLoginAction.loginAuth(account, passWord);
    }

    /**
     * Photo Button to phone login page.
     */
    @OnClick(R.id.phoneRegister)
    public void launchPhoneRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.emailRegister)
    public void launchEmailRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.btn_anonymous)
    public void anonymousLogin() {
        authLoginAction.anonymousLogin();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * get Push Token for EMUI 9.1 and later.
     */
    private void getToken() {
        new Thread() {
            @Override
            public void run() {
                try {
                    String appId = AGConnectServicesConfig.fromContext(LoginActivity.this).getString("client/app_id");
                    String tokenScope = "HCM";
                    String token = HmsInstanceId.getInstance(LoginActivity.this).getToken(appId, tokenScope);
                    Log.i(TAG, "get token: " + token);
                    if (!TextUtils.isEmpty(token)) {
                        sendRegTokenToServer(token);
                    }
                } catch (ApiException e) {
                    Log.w(TAG, "get token failed, " + e);
                }
            }
        }.start();
    }

    /**
     * send Push Token for EMUI 9.1 and later.
     */
    private void sendRegTokenToServer(String token) {
        Log.i("PushLog", "sending to server. token:" + token);
    }

    /**
     * Crash button to make a crash.
     */
    @OnClick(R.id.btn_Crash)
    public void testCrash() {
        Log.d(TAG, "Test Crash");
        AGConnectCrash.getInstance().setUserId("testUser");
        AGConnectCrash.getInstance().log(Log.DEBUG, "set debug log.");
        AGConnectCrash.getInstance().log(Log.INFO, "set info log.");
        AGConnectCrash.getInstance().log(Log.WARN, "set warning log.");
        AGConnectCrash.getInstance().log(Log.ERROR, "set error log.");
        AGConnectCrash.getInstance().setCustomKey("stringKey", "Hello world");
        AGConnectCrash.getInstance().setCustomKey("booleanKey", false);
        AGConnectCrash.getInstance().setCustomKey("doubleKey", 1.1);
        AGConnectCrash.getInstance().setCustomKey("floatKey", 1.1f);
        AGConnectCrash.getInstance().setCustomKey("intKey", 0);
        AGConnectCrash.getInstance().setCustomKey("longKey", 11L);
        AGConnectCrash.getInstance().testIt(this);
    }

    /**
     * ANR button to make a KeyDispatchTimeout ANR .
     */
    @OnClick(R.id.btn_Anr)
    public void anr_Trigger() {
        ToastUtils.showToast(this, "Test ANR");
        Log.d(TAG, "Test ANR");
        anrTestEnable = true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (anrTestEnable) {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (anrTestEnable) {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return super.dispatchTouchEvent(ev);
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
    public void onAuthSuccess(SignInResult signInResult, Integer LoginMod) {
        // get Login info
        authLoginAction.tokenListener();
        UtilTool.LoginMod = LoginMod;
        AGConnectUser user = signInResult.getUser();
        String uid = user.getUid();
        String name;
        if (LoginMod != 0) {
            name = this.getString(R.string.agc_user) + uid.substring(14, 18);
            ToastUtils.showToast(this, getResources().getString(R.string.login_success));
        } else {
            name = this.getString(R.string.anonymous_user) + uid.substring(14, 18);
            ToastUtils.showToast(this, getString(R.string.anonymous_test));
        }
        Bundle data = new Bundle();
        data.putString("UserName", name);
        data.putString("uid", uid);
        Intent intent = new Intent(LoginActivity.this, ImageListActivity.class);
        intent.putExtras(data);
        startActivity(intent);
        finish();
    }

    @Override
    public void onAuthFailed(String FailedMessage) {
        Log.d(TAG, "LoginPage: " + FailedMessage);
        ToastUtils.showToast(this, "LoginPage: " + FailedMessage);
    }

    @Override
    public void onAuthToken(String AuthToken) {
        Log.w(TAG, "get AuthToken, " + AuthToken);
    }

    @Override
    public void onEmpty(){
        ToastUtils.showToast(this, getResources().getString(R.string.edit_account));
    }
}