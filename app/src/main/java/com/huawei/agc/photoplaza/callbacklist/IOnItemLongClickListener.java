package com.huawei.agc.photoplaza.callbacklist;

import android.view.View;

import com.huawei.agc.photoplaza.viewAndAdapter.ImageObj;

public interface IOnItemLongClickListener {
    void onItemLongClick(View view, int position, ImageObj data);
}
