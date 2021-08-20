package com.huawei.photoplaza.agc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.auth.EmailAuthProvider;
import com.huawei.agconnect.auth.EmailUser;
import com.huawei.agconnect.auth.PhoneAuthProvider;
import com.huawei.agconnect.auth.PhoneUser;
import com.huawei.agconnect.auth.VerifyCodeResult;
import com.huawei.agconnect.auth.VerifyCodeSettings;
import com.huawei.agconnect.crash.AGConnectCrash;
import com.huawei.hmf.tasks.Task;
import com.huawei.hmf.tasks.TaskExecutors;
import com.huawei.photoplaza.agc.ImageListActivity;
import com.huawei.photoplaza.agc.model.ToastUtils;

import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * RegisterActivity, registering a Huawei-authenticated user.
 *
 * @since 2020-09-03
 *
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    EditText verifiCodeEditText;
    private EditText accountEditText;
    private EditText secretCodeEditText;
    private String type;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDarkStatusIcon();
        setContentView(R.layout.register_layout);
        accountEditText = findViewById(R.id.regist_accout_nmm);
        verifiCodeEditText = findViewById(R.id.regist_verify_code);
        secretCodeEditText = findViewById(R.id.regist_secret_code);
        Intent intent = getIntent();
        type = intent.getStringExtra("type");
        ButterKnife.bind(this);
    }

    @OnClick(R.id.back_view)
    public void onBackBttonPressed() {
        onBackPressed();
    }

    /**
     * Obtain Verufy Button, to get a email verification_code from Auth.
     */
    @OnClick(R.id.verification_code_obtain)
    public void sendVerification() {
        String accountNumber = accountEditText.getText().toString().trim();
        if ("phone".equals(type)) {
            if( "".equals(accountNumber)){
                ToastUtils.showToast(this, getResources().getString(R.string.empty_error));
            }else {
                String countryCode = "86";
                VerifyCodeSettings settings = VerifyCodeSettings.newBuilder()
                        .action(VerifyCodeSettings.ACTION_REGISTER_LOGIN)
                        .sendInterval(30) //shortest send interval ，30-120s
                        .locale(Locale.SIMPLIFIED_CHINESE) //optional,must contain country and language eg:zh_CN
                        .build();
                if (notEmptyString(countryCode) && notEmptyString(accountNumber)) {
                    Task<VerifyCodeResult> task = PhoneAuthProvider.requestVerifyCode(countryCode, accountNumber, settings);
                    task.addOnSuccessListener(TaskExecutors.uiThread(), verifyCodeResult ->
                            Toast.makeText(RegisterActivity.this, "verify code has been sent.", Toast.LENGTH_SHORT).show()
                    ).addOnFailureListener(TaskExecutors.uiThread(), e -> {
                        Toast.makeText(RegisterActivity.this, "Failed. "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "requestVerifyCode fail:" + e.getMessage());
                    });
                } else {
                    AGConnectCrash.getInstance().testIt(RegisterActivity.this);
                }
            }
        } else {
            if ("".equals(accountNumber)) {
                ToastUtils.showToast(this, getResources().getString(R.string.empty_error));
            } else {
                VerifyCodeSettings settings = VerifyCodeSettings.newBuilder()
                        .action(VerifyCodeSettings.ACTION_REGISTER_LOGIN)   // ACTION_REGISTER_LOGIN/ACTION_RESET_PASSWORD
                        .sendInterval(30) // 最小发送间隔，30-120s
                        .locale(Locale.CHINA) // 发送验证码的语言，locale必须包含Langrage和Country,默认为Locale.getDefault
                        .build();
                Task<VerifyCodeResult> task = EmailAuthProvider.requestVerifyCode(accountNumber, settings);
                task.addOnSuccessListener(TaskExecutors.uiThread(), verifyCodeResult -> {
                    // 验证码申请成功
                    Toast.makeText(RegisterActivity.this, "verify code has been sent.", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(TaskExecutors.uiThread(), e -> {
                    Toast.makeText(RegisterActivity.this, "Failed. "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "requestVerifyCode fail:" + e);
                });
            }
        }
    }
    /**
     * Register Button, register a user with Huawei Auth.
     */
    @OnClick(R.id.btn_register)
    public void register() {
        // 去云端记录注册信息
        String accountNumber = accountEditText.getText().toString().trim();
        String countryCode = "86";
        String verifyCode = verifiCodeEditText.getText().toString().trim();
        String password = secretCodeEditText.getText().toString().trim();
        if( "".equals(accountNumber)||"".equals(verifyCode)||"".equals(password)){
            ToastUtils.showToast(this, getResources().getString(R.string.empty_error));
        }else {
            if ("phone".equals(type)) {
                PhoneUser phoneUser = new PhoneUser.Builder()
                        .setCountryCode(countryCode)
                        .setPhoneNumber(accountNumber)
                        .setVerifyCode(verifyCode)
                        .setPassword(password)
                        .build();
                AGConnectAuth.getInstance().createUser(phoneUser).addOnSuccessListener(signInResult -> {
                    Bundle data = new Bundle();
                    String uid = signInResult.getUser().getUid();
                    data.putString("account", this.getString(R.string.agc_user) + uid.substring(0, 4));
                    data.putString("uid", uid);
                    Intent intent = new Intent(RegisterActivity.this, ImageListActivity.class);
                    intent.putExtras(data);
                    startActivity(intent);
                    finish();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "register error, " + e);
                    ToastUtils.showToast(this, getResources().getString(R.string.register_error) + e.getMessage() );
                });
            } else {
                EmailUser emailUser = new EmailUser.Builder()
                        .setEmail(accountNumber)
                        .setVerifyCode(verifyCode)
                        .setPassword(password) // 表示为当前用户默认创建了密码，后续可以通过密码登录
                        .build();
                AGConnectAuth.getInstance().createUser(emailUser)
                        .addOnSuccessListener(signInResult -> {
                            // 创建帐号成功后，默认已登录
                            Bundle data = new Bundle();
                            String uid = signInResult.getUser().getUid();
                            data.putString("account", this.getString(R.string.agc_user) + uid.substring(0, 4));
                            data.putString("uid", uid);
                            Intent intent = new Intent(RegisterActivity.this, ImageListActivity.class);
                            intent.putExtras(data);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                                ToastUtils.showToast(this, getResources().getString(R.string.register_error) + e.getMessage());
                        });
            }
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private boolean notEmptyString(String string) {
        return string != null && !string.isEmpty();
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
