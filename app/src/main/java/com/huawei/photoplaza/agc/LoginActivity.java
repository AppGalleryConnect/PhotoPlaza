package com.huawei.photoplaza.agc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.huawei.agconnect.applinking.AGConnectAppLinking;
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.auth.AGConnectAuthCredential;
import com.huawei.agconnect.auth.AGConnectUser;
import com.huawei.agconnect.auth.EmailAuthProvider;
import com.huawei.agconnect.auth.PhoneAuthProvider;
import com.huawei.agconnect.auth.TokenResult;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.agconnect.crash.AGConnectCrash;
import com.huawei.agconnect.remoteconfig.AGConnectConfig;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.aaid.entity.AAIDResult;
import com.huawei.hms.analytics.HiAnalytics;
import com.huawei.hms.analytics.HiAnalyticsInstance;
import com.huawei.hms.common.ApiException;
import com.huawei.photoplaza.agc.model.ToastUtils;
import com.huawei.photoplaza.agc.storageAction.StorageAction;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.huawei.photoplaza.agc.storageAction.StorageAction.verifyStoragePermissions;

/**
 * The MainActivity, reName to LoginActivity to log in.
 *
 * @since 2020-09-07
 *
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class LoginActivity extends AppCompatActivity {
    public static final String TAG = "LoginActivity--";
    EditText accountEditText;
    EditText passwordEditText;
    TextView slogan;
    private AGConnectConfig config;
    private Boolean anrTestEnable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDarkStatusIcon();
        setContentView(R.layout.activity_main);
        HmsInit();
        AGConnectAppLinking.getInstance().getAppLinking(LoginActivity.this).addOnSuccessListener(resolvedLinkData -> {
            // 直接跳转到detail界面
            Log.i(TAG,"StartUp From AppLinking");
            if (resolvedLinkData!= null) {
                Uri deepLink = resolvedLinkData.getDeepLink();
                Log.i("AppLinking", "deepLink:" + deepLink.toString());
                Bundle bundle = new Bundle();
                bundle.putBoolean("firstLink", true);
                bundle.putString("deepLink", deepLink.toString());
                Intent intent = new Intent(LoginActivity.this, ImageDetailActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
            }
        }).addOnFailureListener(e-> {
            // 先定义动态获取文件读写权限
            Log.i(TAG,"Normal StartUp");
            initView();
            getRemoteConfig();
        });
    }
    private void HmsInit(){
        getAAID();
        getToken();
        HiAnalyticsInstance instance = HiAnalytics.getInstance(this);
    }

    private void initView(){
        StorageAction.verifyStoragePermissions(this);
        accountEditText = findViewById(R.id.accout_nmm);
        passwordEditText = findViewById(R.id.secret_code);
        slogan = findViewById(R.id.welcome_slogan);
        ButterKnife.bind(this);
    }

    private void getRemoteConfig(){
        // 获取远程配置实例
        config = AGConnectConfig.getInstance();
        config.applyDefault(R.xml.remote_config);
        config.fetch(10).addOnSuccessListener(configValues -> {
            config.apply(configValues);
            String newSlogan = config.getValueAsString("welcome_slogan");
            Log.i(TAG, "RemoteConfig Success: " + newSlogan);
            slogan.setText(newSlogan);
        }).addOnFailureListener(e1 ->
                Log.e(TAG, "getRemoteConfig failed: " + e1.getMessage())
        );
    }


    public void getAAID() {
        Task<AAIDResult> idResult = HmsInstanceId.getInstance(this).getAAID();
        idResult.addOnSuccessListener(new OnSuccessListener<AAIDResult>() {
            @Override
            public void onSuccess(AAIDResult aaidResult) {
                // 获取AAID方法成功
                String aaid = aaidResult.getId();
                Log.d(TAG, "getAAID successfully, aaid is " + aaid );
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception myException) {
                // 获取AAID失败
                Log.d(TAG, "getAAID failed, catch exception: " + myException);
            }
        });
    }

    /**
     * Login button, which is used to perform pre-check on login.
     */
    @OnClick(R.id.btn_login)
    public void login() {
        Log.e(TAG, "Login start");
        String account = accountEditText.getText().toString().trim();
        String passWord = passwordEditText.getText().toString().trim();
        if( "".equals(account)||"".equals(passWord)){
            ToastUtils.showToast(this, getResources().getString(R.string.edit_account));
        }else{
            boolean status = account.contains("@");
            if(status){
                emailLogin(account,passWord);
            }
            else{
                phoneLogin(account,passWord);
            }
        }
    }

    /**
     * Photo Button to phone login page.
     */
    @OnClick(R.id.phoneRegister)
    public void launchPhoneRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        intent.putExtra("type", "phone");
        startActivity(intent);
    }

    /**
     * Email Button to email login page.
     */
    @OnClick(R.id.emailRegister)
    public void launchEmailRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        intent.putExtra("type", "email");
        startActivity(intent);
    }

    /**
     * Anonymous Button to login with anonymous.
     */
    @OnClick(R.id.btn_anonymous)
    public void anonymousLogin() {
        AGConnectAuth.getInstance().signInAnonymously().addOnSuccessListener(signInResult -> {
            AGConnectUser user = signInResult.getUser();
            String uid = user.getUid();
            Bundle data = new Bundle();
            data.putString("account", this.getString(R.string.anonymous_user)+uid.substring(14, 18));
            data.putString("uid", uid);
            ToastUtils.showToast(this, getResources().getString(R.string.login_success));
            Intent intent = new Intent(LoginActivity.this, ImageListActivity.class);
            intent.putExtras(data);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Login failed: " + e.getMessage());
            ToastUtils.showToast(this, getResources().getString(R.string.login_failed) + e.getMessage());
        });
    }

    /**
     * Login Button, email login scenario.
     *
     * @param emailAccount input email address
     * @param emailPassword input password
     */
    private void emailLogin(String emailAccount, String emailPassword) {
        AGConnectAuthCredential credential = EmailAuthProvider.credentialWithPassword(emailAccount, emailPassword);
        AGConnectAuth.getInstance().signIn(credential)
                .addOnSuccessListener(signInResult -> {
                    //获取登录信息
                    Bundle data = new Bundle();
                    String uid = signInResult.getUser().getUid();
                    data.putString("account", this.getString(R.string.agc_user) + uid.substring(0, 4));
                    data.putString("uid", uid);
                    ToastUtils.showToast(this, getResources().getString(R.string.login_success));
                    Intent intent = new Intent(LoginActivity.this, ImageListActivity.class);
                    intent.putExtras(data);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Login failed: " + e.getMessage());
                    ToastUtils.showToast(this, getResources().getString(R.string.login_failed) + e.getMessage());
                });
    }

    /**
     * Login Button, phone number login scenario.
     *
     * @param phoneAccount input Phone number
     * @param photoPassword input password
     */
    private void phoneLogin(String phoneAccount, String photoPassword) {
        String countryCode = "86";
        AGConnectAuthCredential credential = PhoneAuthProvider.credentialWithVerifyCode(
                countryCode,
                phoneAccount,
                photoPassword,
                null ); // password, can be null
        AGConnectAuth.getInstance().signIn(credential).addOnSuccessListener(signInResult -> {
            signInResult.getUser().getToken(true).addOnSuccessListener(new OnSuccessListener<TokenResult>() {
                @Override
                public void onSuccess(TokenResult tokenResult) {
                    String token = tokenResult.getToken();
                    Log.i("getToken", token);
                }
            });
            Bundle data = new Bundle();
            String uid = signInResult.getUser().getUid();
            data.putString("account", this.getString(R.string.agc_user) + uid.substring(0, 4));
            data.putString("uid", uid);
            ToastUtils.showToast(this, getResources().getString(R.string.login_success) );
            Intent intent = new Intent(LoginActivity.this, ImageListActivity.class);
            intent.putExtras(data);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Login failed: " + e.getMessage());
            ToastUtils.showToast(this, getResources().getString(R.string.account_error) + e.getMessage());
        });
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
    }

    /**
     * get Push Token for EMUI 9.1 and later.
     */
    private void getToken() {
        // 创建一个新线程
        new Thread() {
            @Override
            public void run() {
                try {
                    // 从agconnect-service.json文件中读取appId
                    String appId = AGConnectServicesConfig.fromContext(LoginActivity.this).getString("client/app_id");
                    // 输入token标识"HCM"
                    String tokenScope = "HCM";
                    String token = HmsInstanceId.getInstance(LoginActivity.this).getToken(appId, tokenScope);
                    Log.i(TAG, "get token: " + token);

                    // 判断token是否为空
                    if(!TextUtils.isEmpty(token)) {
                        sendRegTokenToServer(token);
                    }
                } catch (ApiException e) {
                    Log.e(TAG, "get token failed, " + e);
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
    public void testCrash () {
        Log.d(TAG, "Test Crash");
        AGConnectCrash.getInstance().setUserId("testuser");
        AGConnectCrash.getInstance().log(Log.DEBUG,"set debug log.");
        AGConnectCrash.getInstance().log(Log.INFO,"set info log.");
        AGConnectCrash.getInstance().log(Log.WARN,"set warning log.");
        AGConnectCrash.getInstance().log(Log.ERROR,"set error log.");
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
    public void anr_Trigger () {
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
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

}