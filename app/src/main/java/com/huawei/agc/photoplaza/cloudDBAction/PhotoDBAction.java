package com.huawei.agc.photoplaza.cloudDBAction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.huawei.agc.photoplaza.appLinkingAction.AppLinkingAction;
import com.huawei.agc.photoplaza.model.ObjectTypeInfoHelper;
import com.huawei.agc.photoplaza.model.PhotoTable;
import com.huawei.agc.photoplaza.storageAction.StorageAction;
import com.huawei.agc.photoplaza.viewAndAdapter.ImageObj;
import com.huawei.agc.photoplaza.viewAndAdapter.UtilTool;
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
 * Handle the Operation PhotoTable of CloudDB
 *
 * @author x00454024
 * @since 2020-08-016
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class PhotoDBAction {
    private static final String TAG = "PhotoDBAction";
    private static final String ZONE_NAME = "PhotoZone";
    private final AGConnectCloudDB mCloudDB;
    private CloudDBZone mCloudDBZone;
    private ListenerHandler mPhotoRegister;
    private CloudDBZoneConfig mConfig;
    private PhotoUiCallBack mPhotoUiCallBack = PhotoUiCallBack.DEFAULT;
    private String deletePhotoName;

    public PhotoDBAction() {
        mCloudDB = AGConnectCloudDB.getInstance();
    }

    /**
     * Add a callback to update photo info list
     *
     * @param uiCallBack callback to update photo list
     */
    public void addCallBacks(PhotoUiCallBack uiCallBack) {
        mPhotoUiCallBack = uiCallBack;
    }

    /**
     * Init AGConnectCloudDB in Application
     *
     * @param context application context
     */
    public static void initAGConnectCloudDB(Context context) {
        AGConnectCloudDB.initialize(context);
    }

    /**
     * Call AGConnectCloudDB.createObjectType to init schema
     */
    public void createObjectType() {
        try {
            mCloudDB.createObjectType(ObjectTypeInfoHelper.getObjectTypeInfo());
        } catch (AGConnectCloudDBException e) {
            Log.e(TAG, "createObjectType: " + e.getMessage());
            mPhotoUiCallBack.onPhotoErrorMessage("createObjectType Failed: " + e.getMessage());
        }
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

    public void openCloudDBZoneV2() {
        mConfig = new CloudDBZoneConfig(ZONE_NAME,
                CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
                CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC);
        mConfig.setPersistenceEnabled(true);
        Task<CloudDBZone> openDBZoneTask = mCloudDB.openCloudDBZone2(mConfig, true);
        openDBZoneTask.addOnSuccessListener(cloudDBZone -> {
            Log.i(TAG, "Open cloudDBZone success");
            mCloudDBZone = cloudDBZone;
            // Add subscription after opening cloudDBZone success
            addSubscription();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Open cloudDBZone failed for " + e.getMessage());
            mPhotoUiCallBack.onPhotoErrorMessage("OpenCloudDBZone Failed: " + e.getMessage());
        });
    }

    public void openCloudDBZoneInShare(String sharePhotoID) {
        mConfig = new CloudDBZoneConfig(ZONE_NAME,
                CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
                CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC);
        mConfig.setPersistenceEnabled(true);
        Task<CloudDBZone> openDBZoneTask = mCloudDB.openCloudDBZone2(mConfig, true);
        openDBZoneTask.addOnSuccessListener(cloudDBZone -> {
            Log.i(TAG, "Open cloudDBZone success");
            mCloudDBZone = cloudDBZone;
            // Add subscription after opening cloudDBZone success
            CloudDBZoneQuery<PhotoTable> query = CloudDBZoneQuery.where(PhotoTable.class)
                    .equalTo(PhotoTableFields.PhotoID, sharePhotoID);
            queryUserPhotos(query);
        });
        openDBZoneTask.addOnFailureListener(e -> {
            Log.e(TAG, "Open cloudDBZone failed for " + e.getMessage());
            mPhotoUiCallBack.onPhotoErrorMessage("OpenCloudDBZone Failed: " + e.getMessage());
        });
    }

    /**
     * Call AGConnectCloudDB.closeCloudDBZone
     */
    public void closeCloudDBZone() {
        try {
            mPhotoRegister.remove();
            mCloudDB.closeCloudDBZone(mCloudDBZone);
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
    public void addSubscription() {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        try {
            CloudDBZoneQuery<PhotoTable> snapshotQuery = CloudDBZoneQuery.where(PhotoTable.class)
                    .equalTo(PhotoTableFields.Sub_Flag, true);
            mPhotoRegister = mCloudDBZone.subscribeSnapshot(snapshotQuery,
                    CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY, mSnapshotListener);
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "subscribeSnapshot: " + e.getMessage());
            mPhotoUiCallBack.onPhotoErrorMessage("addSubscriptionError: " + e.getMessage());
        }
    }

    /**
     * Monitor data change from database. Update photo info list if data have changed
     */
    private OnSnapshotListener<PhotoTable> mSnapshotListener = (cloudDBZoneSnapshot, e) -> {
        if (e != null) {
            Log.w(TAG, "onSnapshot: " + e.getMessage());
            return;
        }
        CloudDBZoneObjectList<PhotoTable> snapshotObjects = cloudDBZoneSnapshot.getSnapshotObjects();
        ArrayList<ImageObj> PhotoTableList = new ArrayList<>();
        try {
            while (snapshotObjects.hasNext()) {
                PhotoTable PhotoTable = snapshotObjects.next();
                String userName = PhotoTable.getUserName();
                ImageObj obj = new ImageObj(PhotoTable.getUserID(),
                        PhotoTable.getPhotoID(),
                        PhotoTable.getURL(),
                        userName,
                        PhotoTable.getCreateTime().toLocaleString(),
                        PhotoTable.getShort_Url()
                );
                PhotoTableList.add(obj);
            }
            mPhotoUiCallBack.onSubscribePhoto(PhotoTableList);
        } catch (AGConnectCloudDBException snapshotException) {
            Log.w(TAG, "onSnapshot:(getObject) " + snapshotException.getMessage());
            mPhotoUiCallBack.onPhotoErrorMessage("SnapshotError: " + snapshotException.getMessage());
        } finally {
            cloudDBZoneSnapshot.release();
        }
    };

    /**
     * Query all photos in storage from cloud side with CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
     */
    public void queryAllPhotos() {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        Task<CloudDBZoneSnapshot<PhotoTable>> queryTask = mCloudDBZone.executeQuery(
                CloudDBZoneQuery.where(PhotoTable.class),
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);
        queryTask.addOnSuccessListener(snapshot -> {
            Log.w(TAG, "query all photos success");
            processQueryResult(snapshot);
        }).addOnFailureListener(e -> {
            mPhotoUiCallBack.onPhotoErrorMessage("queryAllPhotoError: " + e.getMessage());
        });
    }

    /**
     * Query photos with condition
     *
     * @param query query condition
     */
    public void queryUserPhotos(CloudDBZoneQuery<PhotoTable> query) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        Task<CloudDBZoneSnapshot<PhotoTable>> queryTask = mCloudDBZone.executeQuery(
                query, CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);
        queryTask.addOnSuccessListener(snapshot -> {
            Log.i(TAG, "queryPhotos Success");
            processQueryResult(snapshot);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "queryPhotos Failed: " + e.getMessage());
            mPhotoUiCallBack.onPhotoErrorMessage("queryUserPhotoError: " + e.getMessage());
        });
    }

    private void processQueryResult(CloudDBZoneSnapshot<PhotoTable> snapshot) {
        CloudDBZoneObjectList<PhotoTable> snapshotObjects = snapshot.getSnapshotObjects();
        ArrayList<ImageObj> PhotoTableList = new ArrayList<>();
        try {
            while (snapshotObjects.hasNext()) {
                PhotoTable PhotoTable = snapshotObjects.next();
                String userName = PhotoTable.getUserName();
                ImageObj obj = new ImageObj(PhotoTable.getUserID(),
                        PhotoTable.getPhotoID(),
                        PhotoTable.getURL(),
                        userName,
                        PhotoTable.getCreateTime().toLocaleString(),
                        PhotoTable.getShort_Url()
                );
                PhotoTableList.add(obj);
            }
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "processQueryResult: " + e.getMessage());
        } finally {
            snapshot.release();
        }
        mPhotoUiCallBack.onAddOrQueryPhoto(PhotoTableList);
    }

    /**
     * Upsert photoTable
     *
     * @param photoInfo photoInfo added or modified from local
     */
    public void upsertPhotoTables(PhotoTable photoInfo) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        Task<Integer> upsertTask = mCloudDBZone.executeUpsert(photoInfo);
        upsertTask.addOnSuccessListener(cloudDBZoneResult -> Log.i(TAG, "Upsert " + cloudDBZoneResult + " records"))
                .addOnFailureListener(e -> mPhotoUiCallBack.onPhotoErrorMessage("upsertPhotoError: " + e.getMessage()));
    }

    /**
     * queryForDelete photoTable
     *
     * @param thisPhotoID photos selected by user
     */
    public void queryForDelete(String thisPhotoID) {
        CloudDBZoneQuery<PhotoTable> query = CloudDBZoneQuery.where(PhotoTable.class)
                .equalTo(PhotoTableFields.PhotoID, thisPhotoID);
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        Task<CloudDBZoneSnapshot<PhotoTable>> queryTask = mCloudDBZone.executeQuery(query,
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);

        queryTask.addOnSuccessListener(photoTableCloudDBZoneSnapshot -> {
            CloudDBZoneObjectList<PhotoTable> photoInfoCursor = photoTableCloudDBZoneSnapshot.getSnapshotObjects();
            List<PhotoTable> photoInfoList = new ArrayList<>();
            try {
                while (photoInfoCursor.hasNext()) {
                    PhotoTable photoInfo = photoInfoCursor.next();
                    photoInfoList.add(photoInfo);
                    deletePhotoName = photoInfo.getPhotoName();
                }
            } catch (AGConnectCloudDBException e) {
                Log.e(TAG, "queryDelete failed: " + e.getMessage());
            }
            photoTableCloudDBZoneSnapshot.release();
            deletePhotos(photoInfoList);
            StorageAction.deletePhotoOnStorage(deletePhotoName);
        });
    }

    public void deletePhotos(List<PhotoTable> bookInfoList) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        Task<Integer> deleteTask = mCloudDBZone.executeDelete(bookInfoList);
        deleteTask.addOnSuccessListener(integer -> {
            CloudDBZoneQuery<PhotoTable> query = CloudDBZoneQuery.where(PhotoTable.class)
                    .equalTo(PhotoTableFields.UserID, String.valueOf(UtilTool.currentLoginUid))
                    .orderByAsc(PhotoTableFields.PhotoID);
            queryUserPhotos(query);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Delete book info failed: " + e.getMessage());
            mPhotoUiCallBack.onPhotoErrorMessage("deletePhotoError: " + e.getMessage());
        });
    }


    /**
     * Call back to update ui in HomePageFragment
     */
    public interface PhotoUiCallBack {
        PhotoUiCallBack DEFAULT = new PhotoUiCallBack() {
            @Override
            public void onAddOrQueryPhoto(ArrayList<ImageObj> photoTableList) {
                Log.i(TAG, "Using default onAddOrQuery");
            }

            @Override
            public void onSubscribePhoto(ArrayList<ImageObj> photoTableList) {
                Log.i(TAG, "Using default onSubscribe");
            }

            @Override
            public void onPhotoErrorMessage(String errorMessage) {
                Log.i(TAG, "default errorMessage");
            }
        };

        void onAddOrQueryPhoto(ArrayList<ImageObj> photoTableList);

        void onSubscribePhoto(ArrayList<ImageObj> photoTableList);

        void onPhotoErrorMessage(String errorMessage);
    }

    /**
     * Build Photo and userInfo to a format PhotoTable
     *
     * @param userId    input userId
     * @param userName  input userName
     * @param photoURL  input photoURL
     * @param photoName input photoName
     * @return PhotoTable output a format PhotoTable
     */
    public PhotoTable buildPhotoTable(String userId, String userName, String photoURL, String photoName, String shortURL) {
        PhotoTable PhotoTable = new PhotoTable();
        PhotoTable.setPhotoID(String.valueOf(System.currentTimeMillis()));
        PhotoTable.setUserID(userId);
        PhotoTable.setURL(photoURL);
        PhotoTable.setUserName(userName);
        PhotoTable.setPhotoName(photoName);
        PhotoTable.setShort_Url(shortURL);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss sss");
        String CurrTime = format.format(new Date());
        PhotoTable.setCreateTime(AppLinkingAction.parseDate(CurrTime));
        return PhotoTable;
    }
}
