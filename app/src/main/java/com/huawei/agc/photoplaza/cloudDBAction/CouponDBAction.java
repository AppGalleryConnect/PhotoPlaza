package com.huawei.agc.photoplaza.cloudDBAction;

import android.annotation.SuppressLint;
import android.util.Log;

import com.huawei.agc.photoplaza.appLinkingAction.AppLinkingAction;
import com.huawei.agc.photoplaza.model.CouponTable;
import com.huawei.agc.photoplaza.viewAndAdapter.CouponItem;
import com.huawei.agconnect.cloud.database.AGConnectCloudDB;
import com.huawei.agconnect.cloud.database.CloudDBZone;
import com.huawei.agconnect.cloud.database.CloudDBZoneConfig;
import com.huawei.agconnect.cloud.database.CloudDBZoneObjectList;
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery;
import com.huawei.agconnect.cloud.database.CloudDBZoneSnapshot;
import com.huawei.agconnect.cloud.database.ListenerHandler;
import com.huawei.agconnect.cloud.database.OnSnapshotListener;
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException;
import com.huawei.hmf.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Handle the Operation CouponTable of CloudDB
 *
 * @author x00454024
 * @since 2021-08-19
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */

public class CouponDBAction {
    private static final String TAG = "CouponDBAction";
    private static final String ZONE_NAME = "CouponZone";
    private final AGConnectCloudDB mCloudDB;
    private CloudDBZone mCloudDBZone;
    private ListenerHandler mCouponRegister;
    private CloudDBZoneConfig mConfig;
    private CouponUiCallBack mCouponUiCallBack = CouponUiCallBack.DEFAULT;

    public CouponDBAction() {
        mCloudDB = AGConnectCloudDB.getInstance();
    }

    /**
     * Add a callback to update Coupon info list
     *
     * @param uiCallBack callback to update Coupon list
     */
    public void addCallBacks(CouponUiCallBack uiCallBack) {
        mCouponUiCallBack = uiCallBack;
    }

    /**
     * Call AGConnectCloudDB.openCloudDBZone to open a cloudDBZone.
     * We set it with cloud cache mode, and data can be store in local storage
     */
    public void openCloudDBZone() {
        mConfig = new CloudDBZoneConfig(ZONE_NAME,
                CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
                CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC);
        mConfig.setPersistenceEnabled(true);
        try {
            mCloudDBZone = mCloudDB.openCloudDBZone(mConfig, true);
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "openCloudDBZone: " + e.getMessage());
        }
    }

    public void openCloudDBZoneV2(String UserID) {
        mConfig = new CloudDBZoneConfig(ZONE_NAME,
                CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
                CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC);
        mConfig.setPersistenceEnabled(true);
        Task<CloudDBZone> openDBZoneTask = mCloudDB.openCloudDBZone2(mConfig, true);
        openDBZoneTask.addOnSuccessListener(cloudDBZone -> {
            Log.i(TAG, "Open cloudDBZone success");
            mCloudDBZone = cloudDBZone;
            // Add subscription after opening cloudDBZone success
            addSubscription(UserID);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Open cloudDBZone failed for " + e.getMessage());
            mCouponUiCallBack.onCouponErrorMessage("openCloudDBZoneV2Error: " + e.getMessage());
        });
    }

    /**
     * Call AGConnectCloudDB.closeCloudDBZone
     */
    public void closeCloudDBZone() {
        try {
            mCouponRegister.remove();
            mCloudDB.closeCloudDBZone(mCloudDBZone);
            Log.w(TAG, "closeCloudDBZone Success");
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "closeCloudDBZone: " + e.getMessage());
        }
    }

    /**
     * Call AGConnectCloudDB.deleteCloudDBZone
     */
    public void deleteCloudDBZone() {
        try {
            mCloudDB.deleteCloudDBZone(mConfig.getCloudDBZoneName());
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "deleteCloudDBZone: " + e.getMessage());
        }
    }

    /**
     * Add mSnapshotListener to monitor data changes from storage
     */
    public void addSubscription(String UserID) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        try {
            // Subscribe to the results of inviting others in real time
            CloudDBZoneQuery<CouponTable> snapshotQuery = CloudDBZoneQuery.where(CouponTable.class)
                    .equalTo(CouponTableFields.Invite_UserID, UserID);
            mCouponRegister = mCloudDBZone.subscribeSnapshot(snapshotQuery,
                    CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY, mSnapshotListener);
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "subscribeSnapshot: " + e.getMessage());
            mCouponUiCallBack.onCouponErrorMessage("addSubscriptionError: " + e.getMessage());
        }
    }

    /**
     * Monitor data change from database. Update Coupon info list if data have changed
     */
    private final OnSnapshotListener<CouponTable> mSnapshotListener = (cloudDBZoneSnapshot, e) -> {
        if (e != null) {
            Log.w(TAG, "onSnapshot: " + e.getMessage());
            return;
        }
        CloudDBZoneObjectList<CouponTable> snapshotObjects = cloudDBZoneSnapshot.getSnapshotObjects();
        ArrayList<CouponItem> CouponTableList = new ArrayList<>();
        try {
            while (snapshotObjects.hasNext()) {
                CouponTable CouponTable = snapshotObjects.next();
                CouponItem CouponItem = new CouponItem(
                        CouponTable.getInvite_UserName(),
                        CouponTable.getBeInvited_Name(),
                        CouponTable.getEffective_Time().toLocaleString(),
                        CouponTable.getCoupon_Price().toString());
                CouponTableList.add(CouponItem);
                Log.w(TAG, "onSnapshot: " + CouponTable.getCoupon_Price());
            }
            mCouponUiCallBack.onSubscribeCoupon(CouponTableList);
            // return CouponTableList;
        } catch (AGConnectCloudDBException snapshotException) {
            Log.w(TAG, "onSnapshot:(getObject) " + snapshotException.getMessage());
            mCouponUiCallBack.onCouponErrorMessage(snapshotException.getMessage());
        } finally {
            cloudDBZoneSnapshot.release();
        }
    };

    /**
     * Query all Coupons in storage from cloud side with CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
     */
    public void queryAllCoupons() {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        Task<CloudDBZoneSnapshot<CouponTable>> queryTask = mCloudDBZone.executeQuery(
                CloudDBZoneQuery.where(CouponTable.class),
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);
        queryTask.addOnSuccessListener(this::processQueryResult)
                .addOnFailureListener(e -> mCouponUiCallBack.onCouponErrorMessage("queryAllCouponError: " + e.getMessage()));
    }

    /**
     * Query Coupons with condition
     *
     * @param query query condition
     */
    public void queryUserCoupons(CloudDBZoneQuery<CouponTable> query) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        Task<CloudDBZoneSnapshot<CouponTable>> queryTask = mCloudDBZone.executeQuery(
                query, CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);
        queryTask.addOnSuccessListener(snapshot -> {
            Log.i(TAG, "queryCoupons Success");
            processQueryResult(snapshot);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "queryCoupons Failed: " + e.getMessage());
            mCouponUiCallBack.onCouponErrorMessage(e.getMessage());
        });
    }

    private void processQueryResult(CloudDBZoneSnapshot<CouponTable> snapshot) {
        CloudDBZoneObjectList<CouponTable> snapshotObjects = snapshot.getSnapshotObjects();
        ArrayList<CouponItem> CouponTableList = new ArrayList<>();
        try {
            while (snapshotObjects.hasNext()) {
                CouponTable CouponTable = snapshotObjects.next();
                CouponItem CouponItem = new CouponItem(
                        CouponTable.getInvite_UserName(),
                        CouponTable.getBeInvited_Name(),
                        CouponTable.getEffective_Time().toLocaleString(),
                        CouponTable.getCoupon_Price().toString());
                CouponTableList.add(CouponItem);
            }
            mCouponUiCallBack.onAddOrQueryCoupon(CouponTableList);
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "processQueryResult: " + e.getMessage());
        } finally {
            snapshot.release();
        }
    }

    /**
     * Upsert CouponTable
     *
     * @param CouponInfo CouponInfo added or modified from local
     */
    public void upsertCouponTables(CouponTable CouponInfo) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        Task<Integer> upsertTask = mCloudDBZone.executeUpsert(CouponInfo);
        upsertTask.addOnSuccessListener(cloudDBZoneResult -> {
            Log.i(TAG, "Upsert " + cloudDBZoneResult + " records");
            mCouponUiCallBack.onUpsertCoupon(cloudDBZoneResult);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Upsert filed " + e.getMessage());
            mCouponUiCallBack.onCouponErrorMessage("UpsertCouponError: " + e.getMessage());
        });
    }

    public void deleteCoupons(List<CouponTable> CouponInfoList) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        Task<Integer> deleteTask = mCloudDBZone.executeDelete(CouponInfoList);
        deleteTask.addOnSuccessListener(integer -> Log.e(TAG, "Delete Coupon of Photo Success"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Delete photo info failed: " + e.getMessage());
                    mCouponUiCallBack.onCouponErrorMessage("deleteCouponError: " + e.getMessage());
                });
    }

    /**
     * Build inviter and beInvited info to a format CouponTable
     *
     * @param inviter_Id     input inviter_Id
     * @param inviter_Name   input inviter_Name
     * @param beInviter_Id   input beInviter_Id
     * @param beInviter_Name input beInviter_Name
     * @return CouponTable output a format CouponTable
     */
    public CouponTable buildCouponTable(String inviter_Id, String inviter_Name, String beInviter_Id, String beInviter_Name, double price) {
        CouponTable CouponTable = new CouponTable();
        CouponTable.setCouponID(String.valueOf(System.currentTimeMillis()));
        CouponTable.setInvite_UserID(inviter_Id);
        CouponTable.setInvite_UserName(inviter_Name);
        CouponTable.setBeInvited_UserID(beInviter_Id);
        CouponTable.setBeInvited_Name(beInviter_Name);
        CouponTable.setCoupon_Price(price);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss sss");
        String CurrTime = format.format(new Date());
        CouponTable.setEffective_Time(AppLinkingAction.parseDate(CurrTime));
        return CouponTable;
    }

    /**
     * Call back to update ui in CouponUi
     */
    public interface CouponUiCallBack {
        CouponUiCallBack DEFAULT = new CouponUiCallBack() {
            @Override
            public void onAddOrQueryCoupon(ArrayList<CouponItem> CouponTableList) {
                Log.i(TAG, "Using default onAddOrQuery");
            }

            @Override
            public void onSubscribeCoupon(ArrayList<CouponItem> CouponTableList) {
                Log.i(TAG, "Using default onSubscribe");
            }

            @Override
            public void onUpsertCoupon(Integer cloudDBZoneResult) {
                Log.i(TAG, "Using default onUpsert");
            }

            @Override
            public void onCouponErrorMessage(String errorMessage) {
                Log.i(TAG, "default errorMessage");
            }

        };

        void onAddOrQueryCoupon(ArrayList<CouponItem> CouponTableList);

        void onSubscribeCoupon(ArrayList<CouponItem> CouponTableList);

        void onUpsertCoupon(Integer cloudDBZoneResult);

        void onCouponErrorMessage(String errorMessage);

    }
}


