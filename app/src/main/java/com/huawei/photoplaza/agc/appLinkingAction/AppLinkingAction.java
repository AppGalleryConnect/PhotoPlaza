package com.huawei.photoplaza.agc.appLinkingAction;

import android.annotation.SuppressLint;
import android.net.Uri;

import com.huawei.agconnect.applinking.AppLinking;
import com.huawei.photoplaza.agc.callbacklist.Icallback;
import com.huawei.agconnect.applinking.ShortAppLinking;
import com.huawei.photoplaza.agc.callbacklist.Icallback;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handle AppLinking event and create AppLinking in App
 *
 * @author x00454024
 * @since 2020-09-03
 *
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class AppLinkingAction {
    private String shortLink;
    private static final String DOMAIN_URI_PREFIX = "https://photoagc.dra.agconnect.link";
    private static final String DEEP_LINK = "photo://photoplaza.agc.com";
    /**
     * Call AppLinking.Builderto create a App Linking in App
     *
     * @param UserName  input CloudDBZone
     * @param PhotoID  input PhotoID
     * @param ImageUrl  input ImageUrl
     * @param icallback  input Icallback
     */
    public void createAppLinking(String UserName, String PhotoID, String ImageUrl, Icallback icallback) {
        String newDeep_Link = DEEP_LINK  + "?PhotoID=" + PhotoID;
        AppLinking.Builder builder = new AppLinking.Builder().setUriPrefix(DOMAIN_URI_PREFIX)
                .setDeepLink(Uri.parse(newDeep_Link))
                .setAndroidLinkInfo(new AppLinking.AndroidLinkInfo.Builder("")
                        .build())
                .setSocialCardInfo(new AppLinking.SocialCardInfo.Builder()
                        .setTitle("It is a beautiful Photo")
                        .setImageUrl(ImageUrl)
                        .setDescription(UserName + " share a Photo to you").build())
                .setCampaignInfo(new AppLinking.CampaignInfo.Builder()
                        .setName("UserSharePhoto")
                        .setSource("ShareInApp")
                        .setMedium("WeChat").build());
        builder.buildShortAppLinking().addOnSuccessListener(shortAppLinking -> {
            shortLink = shortAppLinking.getShortUrl().toString();
            try {
                icallback.onSuccess(shortLink,"yes");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }).addOnFailureListener(e -> icallback.onFailure(e.getMessage()));
    }
    /**
     * Parse date from an input string. Return current time if parsed failed
     *
     * @param dateStr input date string
     * @return date from date string
     */
    public static Date parseDate(String dateStr) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat();  // 格式化时间
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss sss");    // a为am/pm的标记
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date(System.currentTimeMillis());
    }

    /**
     * Get a picture detail from a App Linking which share from other.
     *
     * @param url input a DeepLink
     * @param name input PhotoID to be parsed.
     */
    public static String getParamByUrl(String url, String name) {
        url += "&";
        String pattern = "(\\?|&){1}#{0,1}" + name + "=[a-zA-Z0-9]*(&{1})";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(url);
        if (m.find( )) {
            System.out.println(m.group(0));
            return m.group(0).split("=")[1].replace("&", "");
        } else {
            return null;
        }
    }

}
