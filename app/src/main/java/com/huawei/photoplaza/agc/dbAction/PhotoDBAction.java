package com.huawei.photoplaza.agc.dbAction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import com.huawei.agconnect.cloud.database.AGConnectCloudDB;
import com.huawei.agconnect.cloud.database.CloudDBZone;
import com.huawei.agconnect.cloud.database.CloudDBZoneConfig;
import com.huawei.agconnect.cloud.database.CloudDBZoneObjectList;
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery;
import com.huawei.agconnect.cloud.database.CloudDBZoneSnapshot;
import com.huawei.agconnect.cloud.database.ListenerHandler;
import com.huawei.agconnect.cloud.database.OnSnapshotListener;
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.photoplaza.agc.appLinkingAction.AppLinkingAction;
import com.huawei.photoplaza.agc.model.ObjectTypeInfoHelper;
import com.huawei.photoplaza.agc.model.PhotoTable;
import com.huawei.photoplaza.agc.storageAction.StorageAction;
import com.huawei.photoplaza.agc.viewAndAdapter.ImageObj;
import com.huawei.photoplaza.agc.viewAndAdapter.UtilTool;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.huawei.photoplaza.agc.MainApplication.photoDBAction;
/**
 * Handle the Operation PhotoTable of CloudDB
 *
 * @author x00454024
 * @since 2020-08-016
 *
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class PhotoDBAction {
    private static final String TAG = "PhotoDBAction";
    private static final String ZONE_NAME = "PhotoZone";
    private final AGConnectCloudDB mCloudDB;
    private CloudDBZone mCloudDBZone;
    private ListenerHandler mPhotoRegister;
    private CloudDBZoneConfig mConfig;
    private final ReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();
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
            Log.w(TAG, "createObjectType: " + e.getMessage());
            mPhotoUiCallBack.onPhotoErrorMessage("createObjectType Failed: "+ e.getMessage());
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
        openDBZoneTask.addOnSuccessListener(new OnSuccessListener<CloudDBZone>() {
            @Override
            public void onSuccess(CloudDBZone cloudDBZone) {
                Log.i(TAG, "Open cloudDBZone success");
                mCloudDBZone = cloudDBZone;
                // Add subscription after opening cloudDBZone success
                addSubscription();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.w(TAG, "Open cloudDBZone failed for " + e.getMessage());
                mPhotoUiCallBack.onPhotoErrorMessage("OpenCloudDBZone Failed: "+ e.getMessage());
            }
        });
    }

    public void openCloudDBZoneInShare(String sharePhotoID) {
        mConfig = new CloudDBZoneConfig(ZONE_NAME,
                CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
                CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC);
        mConfig.setPersistenceEnabled(true);
        Task<CloudDBZone> openDBZoneTask = mCloudDB.openCloudDBZone2(mConfig, true);
        openDBZoneTask.addOnSuccessListener(new OnSuccessListener<CloudDBZone>() {
            @Override
            public void onSuccess(CloudDBZone cloudDBZone) {
                Log.i(TAG, "Open cloudDBZone success");
                mCloudDBZone = cloudDBZone;
                // Add subscription after opening cloudDBZone success
                CloudDBZoneQuery<PhotoTable> query = CloudDBZoneQuery.where(PhotoTable.class)
                        .equalTo(PhotoTableFields.PhotoID, sharePhotoID);
                queryUserPhotos(query);
            }
        });
        openDBZoneTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.w(TAG, "Open cloudDBZone failed for " + e.getMessage());
                mPhotoUiCallBack.onPhotoErrorMessage("OpenCloudDBZone Failed: " + e.getMessage());
            }
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
            mPhotoUiCallBack.onPhotoErrorMessage("addSubscriptionError: "+ e.getMessage());
        }
    }
    /**
     * Monitor data change from database. Update photo info list if data have changed
     */
    private OnSnapshotListener<PhotoTable> mSnapshotListener = new OnSnapshotListener<PhotoTable>() {
        @Override
        public void onSnapshot(CloudDBZoneSnapshot<PhotoTable> cloudDBZoneSnapshot, AGConnectCloudDBException e) {
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
                            PhotoTable.getCreateTime().toLocaleString());
                    PhotoTableList.add(obj);
                }
                mPhotoUiCallBack.onSubscribePhoto(PhotoTableList);
            } catch (AGConnectCloudDBException snapshotException) {
                Log.w(TAG, "onSnapshot:(getObject) " + snapshotException.getMessage());
                mPhotoUiCallBack.onPhotoErrorMessage("SnapshotError: " + snapshotException.getMessage());
            } finally {
                cloudDBZoneSnapshot.release();
            }
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
        queryTask.addOnSuccessListener(new OnSuccessListener<CloudDBZoneSnapshot<PhotoTable>>() {
            @Override
            public void onSuccess(CloudDBZoneSnapshot<PhotoTable> snapshot) {
                processQueryResult(snapshot);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                mPhotoUiCallBack.onPhotoErrorMessage("queryAllPhotoError: "+ e.getMessage());
            }
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
        // 调用executeQuery方法，查询数据
        Task<CloudDBZoneSnapshot<PhotoTable>> queryTask = mCloudDBZone.executeQuery(
                query, CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);
        // 监听查询结果，获取数据快照
        queryTask.addOnSuccessListener(snapshot -> {
            Log.i(TAG, "queryPhotos Success");
            processQueryResult(snapshot);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "queryPhotos Failed: " + e.getMessage());
            mPhotoUiCallBack.onPhotoErrorMessage("queryUserPhotoError: "+ e.getMessage());
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
                        PhotoTable.getCreateTime().toLocaleString());
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
        upsertTask.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer cloudDBZoneResult) {
                Log.i(TAG, "Upsert " + cloudDBZoneResult + " records");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                mPhotoUiCallBack.onPhotoErrorMessage("upsertPhotoError: "+ e.getMessage());
            }
        });
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
            //查询到需要删除的数组以后，执行删除操作，包括删除云存储中的图片以及云数据库中的图片数据和该图片下的评论数据
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
        deleteTask.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                CloudDBZoneQuery<PhotoTable> query = CloudDBZoneQuery.where(PhotoTable.class)
                        .equalTo(PhotoTableFields.UserID, String.valueOf(UtilTool.currentLoginUid));
                // 根据PhotoID向CloudDB条件查询评论数据
                queryUserPhotos(query);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
               Log.e(TAG,"Delete book info failed: " + e.getMessage());
               mPhotoUiCallBack.onPhotoErrorMessage("deletePhotoError: "+ e.getMessage());
            }
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
     * @param userId  input userId
     * @param userName  input userName
     * @param photoURL  input photoURL
     * @param photoName  input photoName
     * @return PhotoTable output a format PhotoTable
     */
    public PhotoTable  buildPhotoTable(String userId, String userName, String photoURL, String photoName) {
        PhotoTable PhotoTable = new PhotoTable();
        PhotoTable.setPhotoID(String.valueOf(System.currentTimeMillis()));
        PhotoTable.setUserID(userId);
        PhotoTable.setURL(photoURL);
        PhotoTable.setUserName(userName);
        PhotoTable.setPhotoName(photoName);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss sss");
        String CurrTime = format.format(new Date());
        PhotoTable.setCreateTime(AppLinkingAction.parseDate(CurrTime));
        return PhotoTable;
    }
}
