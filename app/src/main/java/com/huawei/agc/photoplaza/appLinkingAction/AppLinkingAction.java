package com.huawei.agc.photoplaza.appLinkingAction;

import android.annotation.SuppressLint;
import android.net.Uri;

import com.huawei.agc.photoplaza.callbacklist.Icallback;
import com.huawei.agconnect.applinking.AppLinking;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.huawei.agc.photoplaza.LoginActivity.FALLBACK_URL;
import static com.huawei.agconnect.applinking.AppLinking.AndroidLinkInfo.AndroidOpenType.CustomUrl;

/**
 * Handle AppLinking event and create AppLinking in App
 *
 * @author x00454024
 * @since 2020-09-03
 * <p>
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class AppLinkingAction {
    private String shortLink;
    private static final String DOMAIN_URI_PREFIX = "https://photoagc.drcn.agconnect.link";
    private static final String DEEP_LINK = "https://photoplaza-agc.dra.agchosting.link";
    private static final String SHARE_DEEP_LINK = "share://photoplaza.com";
    private static final String INVITE_DEEP_LINK = "invite://photoplaza.com";

    /**
     * Call AppLinking.Builder to create a App Linking in App
     *
     * @param UserName  input UserName
     * @param UserID    input UserID
     * @param icallback input Icallback
     */
    public void createInviteLinking(String UserName, String UserID, Icallback icallback) {
        String inviteLink = INVITE_DEEP_LINK + "?UserName=" + UserName + "&UserID=" + UserID + "&CouponPrice=200.00";
        if(FALLBACK_URL == null){
            FALLBACK_URL = DEEP_LINK;
        }
        AppLinking.Builder builder = AppLinking.newBuilder()
                .setUriPrefix(DOMAIN_URI_PREFIX)
                .setDeepLink(Uri.parse(DEEP_LINK))
                .setAndroidLinkInfo(AppLinking.AndroidLinkInfo.newBuilder()
                        .setAndroidDeepLink(inviteLink)
                        .setOpenType(CustomUrl)
                        .setFallbackUrl(FALLBACK_URL)
                        .build())
                .setPreviewType(AppLinking.LinkingPreviewType.AppInfo)
                .setCampaignInfo(AppLinking.CampaignInfo.newBuilder()
                        .setName("InviteUser")
                        .setSource("ShareInApp")
                        .setMedium("PhotoPlazaApp")
                        .build());
        builder.buildShortAppLinking().addOnSuccessListener(shortAppLinking -> {
            shortLink = shortAppLinking.getShortUrl().toString();
            try {
                icallback.onSuccess(shortLink, "yes");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }).addOnFailureListener(e -> icallback.onFailure(e.getMessage()));
    }

    /**
     * Call AppLinking.Builder to create a App Linking in App
     *
     * @param UserName  input UserName
     * @param PhotoID   input PhotoID
     * @param PhotoUrl  input PhotoUrl
     * @param icallback input icallback
     */
    public void createShareLinking(String UserName, String PhotoID, String PhotoUrl, Icallback icallback) {
        String photoDeepLink = SHARE_DEEP_LINK + "?PhotoID=" + PhotoID;
        if(FALLBACK_URL == null){
            FALLBACK_URL = DEEP_LINK;
        }
        AppLinking.Builder builder = AppLinking.newBuilder()
                .setUriPrefix(DOMAIN_URI_PREFIX)
                .setDeepLink(Uri.parse(DEEP_LINK))
                .setAndroidLinkInfo(AppLinking.AndroidLinkInfo.newBuilder()
                        .setAndroidDeepLink(photoDeepLink)
                        .setOpenType(CustomUrl)
                        .setFallbackUrl(FALLBACK_URL)
                        .build())
                .setSocialCardInfo(AppLinking.SocialCardInfo.newBuilder()
                        .setTitle("It is a beautiful Photo")
                        .setImageUrl(PhotoUrl)
                        .setDescription(UserName + " share a Photo to you")
                        .build())
                .setCampaignInfo(AppLinking.CampaignInfo.newBuilder()
                        .setName("UserSharePhoto")
                        .setSource("ShareInApp")
                        .setMedium("PhotoPlazaApp")
                        .build());
        builder.buildShortAppLinking().addOnSuccessListener(shortAppLinking -> {
            shortLink = shortAppLinking.getShortUrl().toString();
            try {
                icallback.onSuccess(shortLink, "yes");
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
     * Get param in received deepLink from AppLinking.
     *
     * @param url input the received deepLink
     * @return myMap output the param
     */
    public static Map<String, String> parseLink(String url) {
        Map<String, String> myMap = new HashMap<String, String>();
        String[] urlParts = url.split("\\?");
        String[] params = urlParts[1].split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            myMap.put(keyValue[0], keyValue[1]);
        }
        return myMap;
    }

}
