package com.huawei.agc.photoplaza;

import android.app.Application;

import com.huawei.agconnect.AGConnectInstance;
import com.huawei.agconnect.appmessaging.AGConnectAppMessaging;
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.hms.analytics.HiAnalytics;
import com.huawei.hms.analytics.HiAnalyticsInstance;
import com.huawei.hms.analytics.HiAnalyticsTools;
import com.huawei.agc.photoplaza.cloudDBAction.PhotoDBAction;

/**
 * MainApplication, processes initialization operations during startup.
 *
 * @since 2020-09-03
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class MainApplication extends Application {

    public static HiAnalyticsInstance instance;

    @Override
    public void onCreate() {
        super.onCreate();
        // init CloudDB
        PhotoDBAction.initAGConnectCloudDB(this);
        PhotoDBAction photoDBAction = new PhotoDBAction();
        photoDBAction.createObjectType();
        AGConnectInstance.initialize(this);
        // init HiAnalytics
        HiAnalyticsTools.enableLog();
        instance = HiAnalytics.getInstance(this);
        // signOut Current User
        AGConnectAuth.getInstance().signOut();

        AGConnectAppMessaging.getInstance().setFetchMessageEnable(true);
        AGConnectAppMessaging appMessaging = AGConnectAppMessaging.getInstance();
        appMessaging.setDisplayEnable(true);
    }

}
