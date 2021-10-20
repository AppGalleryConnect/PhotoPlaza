package com.huawei.agc.photoplaza.model;

import android.text.TextUtils;
import android.util.Log;

import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.push.HmsMessageService;

/**
 * MyPushService, Used to apply push tokens for devices earlier than EMUI 9.1.
 *
 * @since 2020-09-01
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2020. All rights reserved.
 */
public class MyPushService extends HmsMessageService {
    private static final String TAG = "PushLog";

    @Override
    public void onNewToken(String token) {
        HmsInstanceId inst = HmsInstanceId.getInstance(this);
        String id = inst.getId();
        Log.i(TAG, "aaid:" + id);
        Log.i(TAG, "received refresh token:" + token);
        // send the token to your app server.
        if (!TextUtils.isEmpty(token)) {
            refreshedTokenToServer(token);
        }
    }

    private void refreshedTokenToServer(String token) {
        Log.i(TAG, "sending token to server:" + token);
    }
}
