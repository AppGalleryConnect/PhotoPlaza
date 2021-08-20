package com.huawei.photoplaza.agc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

import com.huawei.agconnect.applinking.AGConnectAppLinking;
import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery;
import com.huawei.photoplaza.agc.callbacklist.Icallback;
import com.huawei.photoplaza.agc.dbAction.CommentDBAction;
import com.huawei.photoplaza.agc.dbAction.CommentTableFields;
import com.huawei.photoplaza.agc.dbAction.PhotoDBAction;
import com.huawei.photoplaza.agc.appLinkingAction.AppLinkingAction;
import com.huawei.photoplaza.agc.dbAction.PhotoTableFields;
import com.huawei.photoplaza.agc.model.CommentTable;
import com.huawei.photoplaza.agc.model.PhotoTable;
import com.huawei.photoplaza.agc.model.ToastUtils;
import com.huawei.photoplaza.agc.storageAction.StorageAction;
import com.huawei.photoplaza.agc.viewAndAdapter.CommentAdapter;
import com.huawei.photoplaza.agc.viewAndAdapter.CommentItem;
import com.huawei.photoplaza.agc.viewAndAdapter.ImageObj;
import com.huawei.photoplaza.agc.viewAndAdapter.UtilTool;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import static com.huawei.photoplaza.agc.MainApplication.commentDBAction;
import static com.huawei.photoplaza.agc.MainApplication.photoDBAction;

/**
 * ImageDetailActivity to display the details and comments of a photo.
 *
 * @since 2020-09-02
 *
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2021. All rights reserved.
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDarkStatusIcon();
        setContentView(R.layout.image_detail);
        mHandler = new Handler(Looper.getMainLooper());
        StorageAction.verifyStoragePermissions(this);
        mHandler.post(() -> {
            photoDBAction.addCallBacks(ImageDetailActivity.this);
            photoDBAction.openCloudDBZone();
        });
        AGConnectAppLinking.getInstance().getAppLinking(ImageDetailActivity.this).addOnSuccessListener(resolvedLinkData -> {
            // get photo from AppLinking
            if(AGConnectAuth.getInstance().getCurrentUser() != null){
                userLoginFlag = true;
                currUser = UtilTool.currentAccount;
            }
            if (resolvedLinkData!= null) {
                Uri deepLink = resolvedLinkData.getDeepLink();
                Log.i(TAG, "Open From AppLinking:"  + deepLink);
                // Get picture details.
                getSharePicDetails(deepLink.toString());
            }
        }).addOnFailureListener(e->{
            Bundle data = getIntent().getExtras();
            if (data != null) {
                if (data.getBoolean("firstLink")) {
                    getSharePicDetails(data.getString("deepLink"));
                }
                else{
                    getPicDetails();
                    initView();
                }
            }
        });
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
        if(!userLoginFlag){
            mCommentTextView.setText(getResources().getString(R.string.reLogin));
        }
        mHandler.post(() -> {
            commentDBAction.addCallBacks(ImageDetailActivity.this);
            commentDBAction.openCloudDBZoneV2(mPhotoID);
        });
    }

    /**
     * Get a picture detail from a App Linking which share from other.
     *
     * @param link input a deepLink of App Linking
     */
    private void getSharePicDetails(String link) {
        ToastUtils.showToast(this, getResources().getString(R.string.loading_photo));
        sharePhotoID = AppLinkingAction.getParamByUrl(link, "PhotoID");
        Log.i(TAG, "query:" + sharePhotoID);
        Log.i("Applinking", "query:" + sharePhotoID);
        new Handler().postDelayed(() -> {
//            photoDBAction.openCloudDBZoneInShare(sharePhotoID);
            CloudDBZoneQuery<PhotoTable> query = CloudDBZoneQuery.where(PhotoTable.class)
                    .equalTo(PhotoTableFields.PhotoID, sharePhotoID);
            photoDBAction.queryUserPhotos(query);
        }, 650);
    }

    /**
     * Obtain picture details from Intent on the previous page.
     */
    private void getPicDetails() {
        userLoginFlag = true;
        String mPicName = getResources().getString(R.string.pic_detail_string);
        Bundle data = getIntent().getExtras();
        if (data != null) {
            currUser = UtilTool.currentAccount;
            mUserName = data.getString("userName");
            mPhotoID = data.getString("photoID");
            mPicTime = data.getString("createTime");
            mImageUrl = data.getString("imageUri");
            // 拼接图片上传时间字符串
            mPicTimeString = this.getString(R.string.pic_upload_time) + " " + mPicTime;
            // 获取评论
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
        // 向CloudDB插入评论数据
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
        // 拼装评论数据
        CommentTable currComment = commentDBAction.buildCommentTable(mPhotoID, UtilTool.currentLoginUid, UtilTool.currentAccount, commentText);
        // 向CommentZone插入评论数据
        commentDBAction.upsertCommentTables(currComment);
        ToastUtils.showToast(this,  getResources().getString(R.string.comment_success) );
    }

    /**
     * Reserved method, used to create a download task.
     */
    private class DownloadImageTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPostExecute(String result) {
            // 对UI组件的更新操作
            mPicDetailView.setImageBitmap(bitmap);
        }
        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            try {
                bitmap = getBitmap(mImageUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return params[0];
        }
    }

    /**
     * Obtains the image URL and displays the image on the GUI.
     */
    public Bitmap getBitmap(String path) throws IOException {
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
    @SuppressLint("ResourceType")
    private void setCommentCount(int mCommentCount) {
        String data = getResources().getString(R.string.comment_total);
        data = String.format(data, mCommentCount);
        String mCommentTotalString = data;
        mCommentTotalView.setText(mCommentTotalString);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if(!userLoginFlag){
            AppLinkingFlag = true;
            Intent intent = new Intent(ImageDetailActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }else{
            finish();
        }
    }
    @Override
    public void onDestroy() {
        mHandler.post(() -> {
            commentDBAction.closeCloudDBZone();
        });
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_comment:
                if(!userLoginFlag){
                    ToastUtils.showToast(this, getResources().getString(R.string.reLogin) );
                }else{
                    addComment();
                }
                break;

            case R.id.back_toList:
                if(!userLoginFlag){
                    AppLinkingFlag = true;
                    Intent intent = new Intent(ImageDetailActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }else{
                    finish();
                }
                break;
            case R.id.btn_share:
                AppLinkingAction appLink = new AppLinkingAction();
                appLink.createAppLinking(currUser, String.valueOf(mPhotoID), mImageUrl, new Icallback() {
                    @Override
                    public void onSuccess(String result, String fileName) {
                        shareLink(result);
                    }
                    @Override
                    public void onFailure(String result) {
                        Log.e("AppLinking", "createAppLinking failed:"+ result);
                        ToastUtils.showToast(ImageDetailActivity.this, "createAppLinking failed:"+ result);
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
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
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
    public void onCommentErrorMessage(String errorMessage){
        ToastUtils.showToast(this, errorMessage);
    }

    @Override
    public void onAddOrQueryPhoto(ArrayList<ImageObj> photoTableList) {
        mUserName = photoTableList.get(0).getUserName();
        mPhotoID = sharePhotoID;
        mPicTime = photoTableList.get(0).getCreateTime();
        mImageUrl= photoTableList.get(0).getImageUrl();
        //拼接图片上传时间字符串
        mPicTimeString = "上传时间："+mPicTime;
        initView();
        //获取评论
        loadComment();
    }

    @Override
    public void onSubscribePhoto(ArrayList<ImageObj> photoTableList) {

    }

    @Override
    public void onPhotoErrorMessage(String errorMessage) {
        ToastUtils.showToast(this, errorMessage);
    }

}
