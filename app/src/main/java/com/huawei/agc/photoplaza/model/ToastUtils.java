package com.huawei.agc.photoplaza.model;


import android.content.Context;
import android.widget.Toast;

/**
 * Processing In-App Prompt Messages
 *
 * @since 2020-09-08
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2020. All rights reserved.
 */
public class ToastUtils {
    private static Toast toast;

    /**
     * Init AGConnectCloudDB in Application
     *
     * @param context application context
     */
    public static void showToast(Context context, String content) {

        if (toast == null) {
            toast = Toast.makeText(context, content, Toast.LENGTH_SHORT);
        } else {
            toast.setText(content);
        }
        toast.show();
    }

}
