package com.huawei.agc.photoplaza;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.huawei.agc.photoplaza.appLinkingAction.AppLinkingAction;
import com.huawei.agc.photoplaza.callbacklist.Icallback;
import com.huawei.agc.photoplaza.cloudDBAction.CommentDBAction;
import com.huawei.agc.photoplaza.cloudDBAction.CommentTableFields;
import com.huawei.agc.photoplaza.cloudDBAction.PhotoDBAction;
import com.huawei.agc.photoplaza.cloudDBAction.PhotoTableFields;
import com.huawei.agc.photoplaza.model.CommentTable;
import com.huawei.agc.photoplaza.model.PhotoTable;
import com.huawei.agc.photoplaza.model.ToastUtils;
import com.huawei.agc.photoplaza.storageAction.StorageAction;
import com.huawei.agc.photoplaza.viewAndAdapter.CommentAdapter;
import com.huawei.agc.photoplaza.viewAndAdapter.CommentItem;
import com.huawei.agc.photoplaza.viewAndAdapter.ImageObj;
import com.huawei.agc.photoplaza.viewAndAdapter.UtilTool;
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * ImageDetailActivity to display the details and comments of a photo.
 *
 * @since 2020-09-02
 * <p>
 * Copyright (c) HuaWei Technologies Co., Ltd. 2012-2021. All rights reserved.
 */
public class ImageDetailActivity extends Activity
        implements CommentDBAction.CommentUiCallBack,
        PhotoDBAction.PhotoUiCallBack, View.OnClickListener {

    public static final String TAG = "CommentPage";
    private String mPicTimeString = "";
    private String mPicTime = "";
    private String mUserName = "";
    private ImageView mPicDetailView;
    private TextView mCommentTotalView;
    private CommentAdapter adapter;
    private ListView listview;
    private String mImageUrl = "";
    private EditText mCommentTextView;
    private String sharePhotoID;
    private String mPhotoID;
    private Bitmap bitmap;
    private String currUser;
    private boolean userLoginFlag = false;
    public boolean AppLinkingFlag = false;
    private Handler mHandler = null;
    private PhotoDBAction photoDBAction;
    private CommentDBAction commentDBAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDarkStatusIcon();
        setContentView(R.layout.image_detail);
        mHandler = new Handler(Looper.getMainLooper());
        StorageAction.verifyStoragePermissions(this);
        initCloudDB();

        Bundle data = getIntent().getExtras();
        if (data != null) {
            if (data.getBoolean("firstLink")) {
                getSharePicDetails(data.getString("deepLink"));
            } else {
                getPicDetails();
                initView();
            }
        }
    }

    private void initCloudDB() {
        Log.i(TAG, "start to initCloudDB");
        photoDBAction = new PhotoDBAction();
        photoDBAction.addCallBacks(ImageDetailActivity.this);
        photoDBAction.openCloudDBZone();

        commentDBAction = new CommentDBAction();
        commentDBAction.addCallBacks(ImageDetailActivity.this);
        commentDBAction.openCloudDBZone();
    }

    /**
     * Initialize all control and views.
     */
    private void initView() {
        TextView mUserNameView = findViewById(R.id.user_name);
        TextView mPicTimeView = findViewById(R.id.pic_time);
        mCommentTotalView = findViewById(R.id.comment_total);
        mPicDetailView = findViewById(R.id.picture);
        Button btn_add = findViewById(R.id.add_comment);
        btn_add.setOnClickListener(this);
        findViewById(R.id.btn_share).setOnClickListener(this);
        findViewById(R.id.back_toList).setOnClickListener(this);
        mUserNameView.setText(mUserName);
        mPicTimeView.setText(mPicTimeString);
        new Thread(() -> new DownloadImageTask().execute("JSON")).start();
        mCommentTextView = findViewById(R.id.comment_input);
        if (!userLoginFlag) {
            mCommentTextView.setText(getResources().getString(R.string.reLogin));
        }
        mHandler.post(() -> commentDBAction.openCloudDBZoneV2(mPhotoID));
    }

    /**
     * Get a picture detail from a App Linking which share from other.
     *
     * @param link input a deepLink of App Linking
     */
    private void getSharePicDetails(String link) {
        ToastUtils.showToast(this, getResources().getString(R.string.loading_photo));
        sharePhotoID = AppLinkingAction.parseLink(link).get("PhotoID");
        Log.i(TAG, "sharePhotoID: " + sharePhotoID);
        new Handler().postDelayed(() -> {
            CloudDBZoneQuery<PhotoTable> query = CloudDBZoneQuery.where(PhotoTable.class)
                    .equalTo(PhotoTableFields.PhotoID, sharePhotoID);
            photoDBAction.queryUserPhotos(query);
        }, 500);
    }

    /**
     * Obtain picture details from Intent on the previous page.
     */
    private void getPicDetails() {
        userLoginFlag = true;
        String mPicName = getResources().getString(R.string.pic_detail_string);
        Bundle data = getIntent().getExtras();
        if (data != null) {
            currUser = UtilTool.currentUserName;
            mUserName = data.getString("userName");
            mPhotoID = data.getString("photoID");
            mPicTime = data.getString("createTime");
            mImageUrl = data.getString("imageUri");
            Log.i(TAG, "mImageUrl: " + mImageUrl);
            mPicTimeString = this.getString(R.string.pic_upload_time) + " " + mPicTime;
            // load Comment of this photo
            loadComment();
        }
    }

    /**
     * Load comments under photos from CloudDB.
     */
    private void loadComment() {
        CloudDBZoneQuery<CommentTable> query = CloudDBZoneQuery.where(CommentTable.class)
                .equalTo(CommentTableFields.PhotoID, mPhotoID);
        commentDBAction.queryUserComments(query);
    }

    /**
     * Add Comment Data to CloudDB.
     */
    private void addComment() {
        String mComment = mCommentTextView.getText().toString();
        if (mComment.isEmpty()) {
            return;
        }
        // Insert comment data into the Cloud DB
        upsertCommentData(mComment);
        mHandler.post(() -> {
            adapter.notifyDataSetChanged();
            mCommentTextView.setText("");
            loadComment();
        });
    }

    /**
     * upsert Comment Table to CloudDB.
     */
    private void upsertCommentData(String commentText) {
        // Assembling Comment Data
        CommentTable currComment = commentDBAction.buildCommentTable(mPhotoID, UtilTool.currentLoginUid, UtilTool.currentUserName, commentText);
        // Insert comment data into a comment zone.
        commentDBAction.upsertCommentTables(currComment);
        ToastUtils.showToast(this, getResources().getString(R.string.comment_success));
    }

    /**
     * Reserved method, used to create a download task.
     */
    private class DownloadImageTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPostExecute(String result) {
            // Update UI
            mPicDetailView.setImageBitmap(bitmap);
        }

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            bitmap = getBitmap(mImageUrl);
            return params[0];
        }
    }

    /**
     * Obtains the image URL and displays the image on the GUI.
     */
    public Bitmap getBitmap(String path) {
        try {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == 200) {
                InputStream inputStream = conn.getInputStream();
                return BitmapFactory.decodeStream(inputStream);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Calculate the total number of comments in a photo.
     */
    private void setCommentCount(int mCommentCount) {
        String AllStringData = getResources().getString(R.string.comment_total);
        String mCommentTotalString = String.format(AllStringData, mCommentCount);
        mCommentTotalView.setText(mCommentTotalString);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (!userLoginFlag) {
            AppLinkingFlag = true;
            Intent intent = new Intent(ImageDetailActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            finish();
        }
    }

    @Override
    public void onDestroy() {
        mHandler.post(() -> commentDBAction.closeCloudDBZone());
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_comment:
                if (!userLoginFlag) {
                    ToastUtils.showToast(this, getResources().getString(R.string.reLogin));
                } else {
                    addComment();
                }
                break;

            case R.id.back_toList:
                if (!userLoginFlag) {
                    AppLinkingFlag = true;
                    Intent intent = new Intent(ImageDetailActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    finish();
                }
                break;
            case R.id.btn_share:
                AppLinkingAction appLink = new AppLinkingAction();
                appLink.createShareLinking(currUser, String.valueOf(mPhotoID), mImageUrl, new Icallback() {
                    @Override
                    public void onSuccess(String result, String fileName) {
                        shareLink(result);
                    }

                    @Override
                    public void onFailure(String result) {
                        Log.e(TAG, "createAppLinking failed:" + result);
                        ToastUtils.showToast(ImageDetailActivity.this, "createAppLinking failed:" + result);
                    }
                });
                break;
            default:
                break;
        }
    }

    /**
     * Share App Linking after click share button.
     *
     * @param appLinking input a AppLinking
     */
    private void shareLink(String appLinking) {
        if (appLinking != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, appLinking);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    /**
     * Set the status bar color inversion.
     */
    public void setDarkStatusIcon() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    @Override
    public void onAddOrQueryComment(ArrayList<CommentItem> commentTableList) {
        Log.i(TAG, "load commentItemsList size:" + commentTableList.size());
        adapter = new CommentAdapter(ImageDetailActivity.this,
                R.layout.item_comment, commentTableList);
        listview = findViewById(R.id.comment_listview);
        int mCommentCount = commentTableList.size();
        setCommentCount(mCommentCount);
        mHandler.post(() -> {
            listview.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onSubscribeComment(ArrayList<CommentItem> commentTableList) {
        Log.i(TAG, "load comment form Subscribe ");
        adapter = new CommentAdapter(ImageDetailActivity.this,
                R.layout.item_comment, commentTableList);
        listview = findViewById(R.id.comment_listview);
        int mCommentCount = commentTableList.size();
        setCommentCount(mCommentCount);

        mHandler.post(() -> {
            listview.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onCommentErrorMessage(String errorMessage) {
        Log.e(TAG, "DetailPage, Comment: " + errorMessage);
        ToastUtils.showToast(this, "DetailPage, Comment: " + errorMessage);
    }

    @Override
    public void onAddOrQueryPhoto(ArrayList<ImageObj> photoTableList) {
        Log.i(TAG, "load Photo form query ");
        mUserName = photoTableList.get(0).getUserName();
        mPhotoID = sharePhotoID;
        mPicTime = photoTableList.get(0).getCreateTime();
        mImageUrl = photoTableList.get(0).getImageUrl();
        mPicTimeString = "上传时间：" + mPicTime;
        initView();
        loadComment();
    }

    @Override
    public void onSubscribePhoto(ArrayList<ImageObj> photoTableList) {

    }

    @Override
    public void onPhotoErrorMessage(String errorMessage) {
        Log.e(TAG, "DetailPage, Photo: " + errorMessage);
        ToastUtils.showToast(this, "DetailPage, Photo: " + errorMessage);
    }

}
