package com.huawei.photoplaza.agc;

import android.app.Application;
import android.util.Log;
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.photoplaza.agc.dbAction.CommentDBAction;
import com.huawei.photoplaza.agc.dbAction.PhotoDBAction;

/**
 * MainApplication, processes initialization operations during startup.
 *
 * @since 2020-09-03
 *
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class MainApplication extends Application {

    public static PhotoDBAction photoDBAction;
    public static CommentDBAction commentDBAction;
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化CloudDB
        PhotoDBAction.initAGConnectCloudDB(this);
        photoDBAction = new PhotoDBAction();
        commentDBAction = new CommentDBAction();
        photoDBAction.createObjectType();
        commentDBAction.createObjectType();
        // signOut Current User
        AGConnectAuth.getInstance().signOut();
    }

}
