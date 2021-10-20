package com.huawei.agc.photoplaza.callbacklist;

import java.io.FileNotFoundException;

public interface Icallback {
    void onSuccess(String result, String fileName) throws FileNotFoundException;

    void onFailure(String result);
}
