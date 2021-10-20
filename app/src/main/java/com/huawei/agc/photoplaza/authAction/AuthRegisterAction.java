package com.huawei.agc.photoplaza.authAction;

import android.util.Log;

import com.huawei.agc.photoplaza.viewAndAdapter.UtilTool;
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.auth.EmailAuthProvider;
import com.huawei.agconnect.auth.EmailUser;
import com.huawei.agconnect.auth.PhoneAuthProvider;
import com.huawei.agconnect.auth.PhoneUser;
import com.huawei.agconnect.auth.SignInResult;
import com.huawei.agconnect.auth.VerifyCodeResult;
import com.huawei.agconnect.auth.VerifyCodeSettings;
import com.huawei.agconnect.core.service.auth.OnTokenListener;
import com.huawei.agconnect.core.service.auth.TokenSnapshot;
import com.huawei.hmf.tasks.Task;
import com.huawei.hmf.tasks.TaskExecutors;

import java.util.Locale;

/**
 * Handle the Operation of AuthServices in Register
 *
 * @author x00454024
 * @since 2021-08-20
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */

public class AuthRegisterAction {

    private static final String TAG = "AuthRegisterAction";
    private static AuthReCallBack mAuthReCallBack = AuthReCallBack.DEFAULT;

    public AuthRegisterAction() {
        AGConnectAuth mAGConnectAuth = AGConnectAuth.getInstance();
    }

    public void addCallBacks(AuthRegisterAction.AuthReCallBack authReCallBack) {
        mAuthReCallBack = authReCallBack;
    }

    public void registerAuth(String accountNumber, String verifyCode, String password) {
        if ("".equals(accountNumber) || "".equals(verifyCode) || "".equals(password)) {
            mAuthReCallBack.onEmpty();
        } else {
            UtilTool.loginAccount = accountNumber;
            boolean status = accountNumber.contains("@");
            if (status) {
                registerEmailUser(accountNumber, verifyCode, password);
            } else {
                registerPhoneUser(accountNumber, verifyCode, password);
            }
        }
    }

    public void registerPhoneUser(String accountNumber, String verifyCode, String password) {
        String countryCode = "86";
        PhoneUser phoneUser = new PhoneUser.Builder()
                .setCountryCode(countryCode)
                .setPhoneNumber(accountNumber)
                .setVerifyCode(verifyCode)
                .setPassword(password)
                .build();
        AGConnectAuth.getInstance().createUser(phoneUser)
                .addOnSuccessListener(signInResult -> {
                    mAuthReCallBack.onAuthSuccess(signInResult, 11);
                }).addOnFailureListener(e -> {
            mAuthReCallBack.onFailed(e.getMessage());
        });
    }

    public void registerEmailUser(String accountNumber, String verifyCode, String password) {
        EmailUser emailUser = new EmailUser.Builder()
                .setEmail(accountNumber)
                .setVerifyCode(verifyCode)
                .setPassword(password)
                .build();
        AGConnectAuth.getInstance().createUser(emailUser)
                .addOnSuccessListener(signInResult -> {
                    mAuthReCallBack.onAuthSuccess(signInResult, 12);
                })
                .addOnFailureListener(e -> {
                    mAuthReCallBack.onFailed(e.getMessage());
                });
    }

    /**
     * Anonymous Button to login with anonymous.
     */
    public void anonymousLogin() {
        AGConnectAuth.getInstance().signInAnonymously().addOnSuccessListener(signInResult -> {
            mAuthReCallBack.onAuthSuccess(signInResult, 0);
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Login failed: " + e.getMessage());
            mAuthReCallBack.onFailed(e.getMessage());
        });
    }

    /**
     * Obtain Verufy Button, to get a email verification_code from Auth.
     */
    public void sendEmailVerify(String accountNumber) {
        VerifyCodeSettings settings = VerifyCodeSettings.newBuilder()
                .action(VerifyCodeSettings.ACTION_REGISTER_LOGIN)
                .sendInterval(30)
                .locale(Locale.CHINA)
                .build();
        Task<VerifyCodeResult> task = EmailAuthProvider.requestVerifyCode(accountNumber, settings);
        task.addOnSuccessListener(TaskExecutors.uiThread(), verifyCodeResult -> {
            mAuthReCallBack.onSendVerify(verifyCodeResult);
        }).addOnFailureListener(TaskExecutors.uiThread(), e -> {
            Log.e(TAG, "requestVerifyCode fail:" + e.getMessage());
            mAuthReCallBack.onFailed(e.getMessage());
        });
    }

    public void sendPhoneVerify(String accountNumber) {
        String countryCode = "86";
        VerifyCodeSettings settings = VerifyCodeSettings.newBuilder()
                .action(VerifyCodeSettings.ACTION_REGISTER_LOGIN)
                .sendInterval(30)
                .locale(Locale.SIMPLIFIED_CHINESE)
                .build();
        if (notEmptyString(countryCode) && notEmptyString(accountNumber)) {
            Task<VerifyCodeResult> task = PhoneAuthProvider.requestVerifyCode(countryCode, accountNumber, settings);
            task.addOnSuccessListener(TaskExecutors.uiThread(), verifyCodeResult -> {
                mAuthReCallBack.onSendVerify(verifyCodeResult);
            }).addOnFailureListener(TaskExecutors.uiThread(), e -> {
                Log.e(TAG, "requestVerifyCode fail:" + e.getMessage());
                mAuthReCallBack.onFailed(e.getMessage());
            });
        } else {
            Log.w(TAG, "info empty");
        }
    }

    private boolean notEmptyString(String string) {
        return string != null && !string.isEmpty();
    }


    /**
     * Call back to update AuthServices
     */
    public interface AuthReCallBack {
        AuthRegisterAction.AuthReCallBack DEFAULT = new AuthRegisterAction.AuthReCallBack() {
            @Override
            public void onAuthSuccess(SignInResult signInResult, Integer LoginMode) {
                Log.i(TAG, "Using default onAuthSuccess");
            }

            @Override
            public void onFailed(String FailedMessage) {
                Log.i(TAG, "Using default onAuthFailed");
            }

            @Override
            public void onEmpty() {
                Log.i(TAG, "default onEmpty");
            }

            @Override
            public void onSendVerify(VerifyCodeResult verifyCodeResult) {
                Log.i(TAG, "default errorMessage");
            }
        };

        void onAuthSuccess(SignInResult signInResult, Integer LoginMode);

        void onFailed(String FailedMessage);

        void onEmpty();

        void onSendVerify(VerifyCodeResult verifyCodeResult);
    }

    public void tokenListener() {
        Log.i(TAG, "tokenListener");
        AGConnectAuth.getInstance().addTokenListener(tokenSnapshot -> {
            TokenSnapshot.State state = tokenSnapshot.getState();
            if (state == TokenSnapshot.State.TOKEN_UPDATED) {
                String token = tokenSnapshot.getToken();
                Log.i(TAG, "tokenListener, new token: " + token);
            }
        });
    }
}
