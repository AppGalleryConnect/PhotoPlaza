package com.huawei.agc.photoplaza.authAction;

import android.util.Log;

import com.huawei.agc.photoplaza.R;
import com.huawei.agc.photoplaza.model.ToastUtils;
import com.huawei.agc.photoplaza.viewAndAdapter.UtilTool;
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.auth.AGConnectAuthCredential;
import com.huawei.agconnect.auth.EmailAuthProvider;
import com.huawei.agconnect.auth.PhoneAuthProvider;
import com.huawei.agconnect.auth.SignInResult;
import com.huawei.agconnect.core.service.auth.OnTokenListener;
import com.huawei.agconnect.core.service.auth.TokenSnapshot;

/**
 * Handle the Operation of AuthServices in Login
 *
 * @author x00454024
 * @since 2021-08-20
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */

public class AuthLoginAction {

    private static final String TAG = "AuthLoginAction";
    private static AuthLoginCallBack mAuthLoginCallBack = AuthLoginCallBack.DEFAULT;

    public AuthLoginAction() {
        AGConnectAuth mAGConnectAuth = AGConnectAuth.getInstance();
    }

    public void addCallBacks(AuthLoginAction.AuthLoginCallBack authUiCallBack) {
        mAuthLoginCallBack = authUiCallBack;
    }

    public void loginAuth(String account, String passWord) {
        if ("".equals(account) || "".equals(passWord)) {
            mAuthLoginCallBack.onEmpty();
        } else {
            UtilTool.loginAccount = account;
            boolean status = account.contains("@");
            if (status) {
                emailLogin(account, passWord);
            } else {
                phoneLogin(account, passWord);
            }
        }
    }

    /**
     * Login Button, email login scenario.
     *
     * @param emailAccount  input email address
     * @param emailPassword input password
     */
    public void emailLogin(String emailAccount, String emailPassword) {
        AGConnectAuthCredential credential = EmailAuthProvider.credentialWithPassword(emailAccount, emailPassword);
        AGConnectAuth.getInstance().signIn(credential)
                .addOnSuccessListener(signInResult -> {
                    mAuthLoginCallBack.onAuthSuccess(signInResult, 12);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Login failed: " + e.getMessage());
                    mAuthLoginCallBack.onAuthFailed(e.getMessage());
                });
    }

    /**
     * Login Button, phone number login scenario.
     *
     * @param phoneAccount  input Phone number
     * @param photoPassword input password
     */
    public void phoneLogin(String phoneAccount, String photoPassword) {
        String countryCode = "86";
        AGConnectAuthCredential credential = PhoneAuthProvider.credentialWithVerifyCode(
                countryCode,
                phoneAccount,
                photoPassword,
                null);
        AGConnectAuth.getInstance().signIn(credential).addOnSuccessListener(signInResult -> {
            Log.i(TAG, "phoneLogin success");
            mAuthLoginCallBack.onAuthSuccess(signInResult, 11);
            signInResult.getUser().getToken(true).addOnSuccessListener(tokenResult -> {
                String token = tokenResult.getToken();
                Log.i(TAG, "getToken success：" + token);
                mAuthLoginCallBack.onAuthToken(token);
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Login failed: " + e.getMessage());
            mAuthLoginCallBack.onAuthFailed(e.getMessage());
        });
    }

    /**
     * Anonymous Button to login with anonymous.
     */
    public void anonymousLogin() {
        AGConnectAuth.getInstance().signInAnonymously().addOnSuccessListener(signInResult -> {
            mAuthLoginCallBack.onAuthSuccess(signInResult, 0);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Login failed: " + e.getMessage());
            mAuthLoginCallBack.onAuthFailed(e.getMessage());
        });
    }

    /**
     * Call back to update AuthServices
     */
    public interface AuthLoginCallBack {
        AuthLoginAction.AuthLoginCallBack DEFAULT = new AuthLoginAction.AuthLoginCallBack() {
            @Override
            public void onAuthSuccess(SignInResult signInResult, Integer LoginMode) {
                Log.i(TAG, "Using default onAuthSuccess");
            }

            @Override
            public void onAuthFailed(String FailedMessage) {
                Log.i(TAG, "Using default onAuthFailed");
            }

            @Override
            public void onEmpty() {
                Log.i(TAG, "default onEmpty");
            }

            @Override
            public void onAuthToken(String errorMessage) {
                Log.i(TAG, "default errorMessage");
            }
        };

        void onAuthSuccess(SignInResult signInResult, Integer LoginMode);

        void onAuthFailed(String FailedMessage);

        void onAuthToken(String AuthToken);

        void onEmpty();
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