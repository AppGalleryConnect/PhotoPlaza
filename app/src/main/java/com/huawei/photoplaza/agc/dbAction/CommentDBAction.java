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
import com.huawei.photoplaza.agc.model.CommentTable;
import com.huawei.photoplaza.agc.model.ObjectTypeInfoHelper;
import com.huawei.photoplaza.agc.viewAndAdapter.CommentItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
/**
 * Handle the Operation CommentTable of CloudDB
 *
 * @author x00454024
 * @since 2020-08-20
 *
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class CommentDBAction {
    private static final String TAG = "CommentDBAction";
    private static final String ZONE_NAME = "CommentZone";
    private AGConnectCloudDB mCloudDB;
    private CloudDBZone mCloudDBZone;
    private ListenerHandler mCommentRegister;
    private CloudDBZoneConfig mConfig;
    private ReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();
    private CommentUiCallBack mCommentUiCallBack = CommentUiCallBack.DEFAULT;

    public CommentDBAction() {
        mCloudDB = AGConnectCloudDB.getInstance();
    }

    /**
     * Add a callback to update comment info list
     *
     * @param uiCallBack callback to update comment list
     */
    public void addCallBacks(CommentUiCallBack uiCallBack) {
        mCommentUiCallBack = uiCallBack;
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

    public void openCloudDBZoneV2(String mPhotoID) {
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
                addSubscription(mPhotoID);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.w(TAG, "Open cloudDBZone failed for " + e.getMessage());
                mCommentUiCallBack.onCommentErrorMessage("openCloudDBZoneV2Error: " + e.getMessage());
            }
        });
    }

    /**
     * Call AGConnectCloudDB.closeCloudDBZone
     */
    public void closeCloudDBZone() {
        try {
            mCommentRegister.remove();
            mCloudDB.closeCloudDBZone(mCloudDBZone);
            Log.w(TAG, "closeCloudDBZone Success" );
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
    public void addSubscription(String mPhotoID) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        try {
            CloudDBZoneQuery<CommentTable> snapshotQuery = CloudDBZoneQuery.where(CommentTable.class)
                    .equalTo(CommentTableFields.PhotoID, mPhotoID);
            mCommentRegister = mCloudDBZone.subscribeSnapshot(snapshotQuery,
                    CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY, mSnapshotListener);
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "subscribeSnapshot: " + e.getMessage());
            mCommentUiCallBack.onCommentErrorMessage("addSubscriptionError: " + e.getMessage());
        }
    }
    /**
     * Monitor data change from database. Update comment info list if data have changed
     */
    private OnSnapshotListener<CommentTable> mSnapshotListener = new OnSnapshotListener<CommentTable>() {
        @Override
        public void onSnapshot(CloudDBZoneSnapshot<CommentTable> cloudDBZoneSnapshot, AGConnectCloudDBException e) {
            if (e != null) {
                Log.w(TAG, "onSnapshot: " + e.getMessage());
                return;
            }
            CloudDBZoneObjectList<CommentTable> snapshotObjects = cloudDBZoneSnapshot.getSnapshotObjects();
            ArrayList<CommentItem> CommentTableList = new ArrayList<>();
            try {
                while (snapshotObjects.hasNext()) {
                    CommentTable CommentTable = snapshotObjects.next();
                    CommentItem commentItem = new CommentItem(CommentTable.getUserID(), CommentTable.getCommentID(), CommentTable.getCommentText(), CommentTable.getUserName(), CommentTable.getCommentText());
                    CommentTableList.add(commentItem);
                    Log.w(TAG, "onSnapshot: " + CommentTable.getCommentText());
                }
                mCommentUiCallBack.onSubscribeComment(CommentTableList);
                // 快照释放，数据处理，同时展示给前台
                // return CommentTableList;
            } catch (AGConnectCloudDBException snapshotException) {
                Log.w(TAG, "onSnapshot:(getObject) " + snapshotException.getMessage());
            } finally {
                cloudDBZoneSnapshot.release();
            }
        }
    };

    /**
     * Query all comments in storage from cloud side with CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
     */
    public void queryAllComments() {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        Task<CloudDBZoneSnapshot<CommentTable>> queryTask = mCloudDBZone.executeQuery(
                CloudDBZoneQuery.where(CommentTable.class),
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);
        queryTask.addOnSuccessListener(new OnSuccessListener<CloudDBZoneSnapshot<CommentTable>>() {
            @Override
            public void onSuccess(CloudDBZoneSnapshot<CommentTable> snapshot) {
                processQueryResult(snapshot);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                mCommentUiCallBack.onCommentErrorMessage("queryAllCommentError: " + e.getMessage());
            }
        });
    }

    /**
     * Query comments with condition
     *
     * @param query query condition
     */
    public void queryUserComments(CloudDBZoneQuery<CommentTable> query) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        // 调用executeQuery方法，查询数据
        Task<CloudDBZoneSnapshot<CommentTable>> queryTask = mCloudDBZone.executeQuery(
                query, CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);
        // 监听查询结果，获取数据快照
        queryTask.addOnSuccessListener(snapshot -> {
            Log.i(TAG, "queryComments Success");
            processQueryResult(snapshot);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "queryComments Failed: " + e.getMessage());
        });
    }

    private void processQueryResult(CloudDBZoneSnapshot<CommentTable> snapshot) {
        CloudDBZoneObjectList<CommentTable> snapshotObjects = snapshot.getSnapshotObjects();
        ArrayList<CommentItem> CommentTableList = new ArrayList<>();
        try {
            while (snapshotObjects.hasNext()) {
                CommentTable CommentTable = snapshotObjects.next();
                String userName = CommentTable.getUserName();
                CommentItem obj = new CommentItem(
                        CommentTable.getUserID(),
                        CommentTable.getCommentID(),
                        CommentTable.getCommentText(),
                        userName,
                        CommentTable.getCreateTime().toLocaleString());
                CommentTableList.add(obj);
            }
            mCommentUiCallBack.onAddOrQueryComment(CommentTableList);
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "processQueryResult: " + e.getMessage());
        } finally {
            snapshot.release();
        }
    }

    /**
     * Upsert commentTable
     *
     * @param commentInfo commentInfo added or modified from local
     */
    public void upsertCommentTables(CommentTable commentInfo) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        Task<Integer> upsertTask = mCloudDBZone.executeUpsert(commentInfo);
        upsertTask.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer cloudDBZoneResult) {
                Log.i(TAG, "Upsert " + cloudDBZoneResult + " records");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.i(TAG, "Upsert filed " + e.getMessage());
                mCommentUiCallBack.onCommentErrorMessage("UpsertCommentError: " + e.getMessage());
            }
        });
    }

    /**
     * queryForDelete commentTable
     *
     * @param thisPhotoID photos selected by user
     */
    public void queryForDelete(String thisPhotoID) {
        CloudDBZoneQuery<CommentTable> query = CloudDBZoneQuery.where(CommentTable.class)
                .equalTo(CommentTableFields.PhotoID, thisPhotoID);
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        Task<CloudDBZoneSnapshot<CommentTable>> queryTask = mCloudDBZone.executeQuery(query,
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);

        queryTask.addOnSuccessListener(CloudDBZoneSnapshot -> {
            CloudDBZoneObjectList<CommentTable> CommentInfoCursor = CloudDBZoneSnapshot.getSnapshotObjects();
            List<CommentTable> CommentInfoList = new ArrayList<>();
            try {
                while (CommentInfoCursor.hasNext()) {
                    CommentTable commentTable = CommentInfoCursor.next();
                    CommentInfoList.add(commentTable);
                }
            } catch (AGConnectCloudDBException e) {
                Log.e(TAG, "queryDelete failed: " + e.getMessage());
            }
            CloudDBZoneSnapshot.release();
            //查询到需要删除的数组以后，执行删除操作，包括删除云存储中的图片以及云数据库中的图片数据和该图片下的评论数据
            deleteComments(CommentInfoList);
        });
    }

    public void deleteComments(List<CommentTable> CommentInfoList) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        Task<Integer> deleteTask = mCloudDBZone.executeDelete(CommentInfoList);
        deleteTask.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                Log.e(TAG,"Delete Comment of Photo Success");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG,"Delete photo info failed: " + e.getMessage());
                mCommentUiCallBack.onCommentErrorMessage("deleteCommentError: " + e.getMessage());
            }
        });
    }


    /**
     * Call back to update ui in CommentUi
     */
    public interface CommentUiCallBack {
        CommentUiCallBack DEFAULT = new CommentUiCallBack() {
            @Override
            public void onAddOrQueryComment(ArrayList<CommentItem> commentTableList) {
                Log.i(TAG, "Using default onAddOrQuery");
            }

            @Override
            public void onSubscribeComment(ArrayList<CommentItem> commentTableList) {
                Log.i(TAG, "Using default onSubscribe");
            }

            @Override
            public void onCommentErrorMessage(String errorMessage) {
                Log.i(TAG, "default errorMessage");
            }

        };

        void onAddOrQueryComment(ArrayList<CommentItem> commentTableList);

        void onSubscribeComment(ArrayList<CommentItem> commentTableList);

        void onCommentErrorMessage(String errorMessage);

    }

    /**
     * Build Comment and userInfo to a format CommentTable
     *
     * @param photoId  input photoId
     * @param userId  input userId
     * @param userName  input userName
     * @param commentText  input commentText
     * @return CommentTable output a format CommentTable
     */
    public CommentTable  buildCommentTable(String photoId, String userId, String userName, String commentText) {
        CommentTable commentTable = new CommentTable();
        commentTable.setCommentID(String.valueOf(System.currentTimeMillis()));
        commentTable.setPhotoID(photoId);
        commentTable.setUserID(userId);
        commentTable.setUserName(userName);
        commentTable.setCommentText(commentText);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss sss");
        String CurrTime = format.format(new Date());
        commentTable.setCreateTime(AppLinkingAction.parseDate(CurrTime));
        return commentTable;
    }

}

