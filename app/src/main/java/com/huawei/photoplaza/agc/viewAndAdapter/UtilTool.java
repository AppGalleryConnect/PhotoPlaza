package com.huawei.photoplaza.agc.viewAndAdapter;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import com.huawei.agconnect.cloud.storage.core.AGCStorageManagement;
import com.huawei.agconnect.cloud.storage.core.DownloadTask;
import com.huawei.agconnect.cloud.storage.core.OnProgressListener;
import com.huawei.agconnect.cloud.storage.core.StorageReference;
import com.huawei.hmf.tasks.OnCompleteListener;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.Task;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class UtilTool {


    public static String currentAccount;
    public static String currentLoginUid;
    private static String TAG = "photoplaza_";

    private static AGCStorageManagement storageManagement;

    // 以当前上传用户的UID +“_”+当前上传的时间戳为图片文件名
    public static String getFileName() {
        if (currentLoginUid == null) {

            Log.e(TAG + "getFileName", "currentLoginUid is null");
            return null;
        }
        String fileName = currentLoginUid + "_" + System.currentTimeMillis();
        return fileName;
    }


    @SuppressLint("NewApi")
    public static String getPathFromUri(final Context context, final Uri uri) {
        if (uri == null) {
            return null;
        }
        // 判斷是否為Android 4.4之後的版本
        final boolean after44 = Build.VERSION.SDK_INT >= 19;
        if (after44 && DocumentsContract.isDocumentUri(context, uri)) {
            // 如果是Android 4.4之後的版本，而且屬於文件URI
            final String authority = uri.getAuthority();
            // 判斷Authority是否為本地端檔案所使用的
            if ("com.android.externalstorage.documents".equals(authority)) {
                // 外部儲存空間
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] divide = docId.split(":");
                final String type = divide[0];
                if ("primary".equals(type)) {
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/").concat(divide[1]);
                    return path;
                } else {
                    String path = "/storage/".concat(type).concat("/").concat(divide[1]);
                    return path;
                }
            } else if ("com.android.providers.downloads.documents".equals(authority)) {
                // 下载目录
                final String docId = DocumentsContract.getDocumentId(uri);
                if (docId.startsWith("raw:")) {
                    final String path = docId.replaceFirst("raw:", "");
                    return path;
                }
                final Uri downloadUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(docId));
                String path = queryAbsolutePath(context, downloadUri);
                return path;
            } else if ("com.android.providers.media.documents".equals(authority)) {
                // 图片档案
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] divide = docId.split(":");
                final String type = divide[0];
                Uri mediaUri = null;
                if ("image".equals(type)) {
                    mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    mediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                } else {
                    return null;
                }
                mediaUri = ContentUris.withAppendedId(mediaUri, Long.parseLong(divide[1]));
                String path = queryAbsolutePath(context, mediaUri);
                return path;
            }
        } else {
            // 如果是一般的URI
            final String scheme = uri.getScheme();
            String path = null;
            if ("content".equals(scheme)) {
                // 內容URI
                path = queryAbsolutePath(context, uri);
            } else if ("file".equals(scheme)) {
                // 档案URI
                path = uri.getPath();
            }
            return path;
        }
        return null;
    }

    public static String queryAbsolutePath(final Context context, final Uri uri) {
        final String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                return cursor.getString(index);
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static String downLoadImage(String photoName) {

        Log.e("getImageBitmap:", "download file-------");

        // 云存储文件存放路径
        String downLoadPath = "images/" + photoName + ".jpg";

        // 文件下载至本地存储路径
        String localStoragePath = getAGCSdkDirPath() + downLoadPath;
        File file = new File(localStoragePath);

        if (!file.exists()) {
            if (storageManagement == null) {
                // 初始化云端存储实例对象 736430079244583900-12qji
                storageManagement = AGCStorageManagement.getInstance();
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 获取到云端存储文件引用对象
                    StorageReference reference = storageManagement.getStorageReference(downLoadPath);

                    Log.e(TAG, "local path:" + downLoadPath);

                    // 开启下载任务，下载云端文件至本地存放路径
                    DownloadTask task = reference.getFile(file);
                    task.addOnCompleteListener(new OnCompleteListener<DownloadTask.DownloadResult>() {
                        @Override
                        public void onComplete(Task<DownloadTask.DownloadResult> task) {
                            Long filesize = task.getResult().getTotalByteCount();
                            Log.e("getImageBitmap:", "download file size:" + filesize);
                           //  下载成功之后，返回本地图片存储路径
                            Log.e("getImageBitmap_onclet", localStoragePath);
                        }
                    }).addOnProgressListener(new OnProgressListener<DownloadTask.DownloadResult>() {
                        @Override
                        public void onProgress(DownloadTask.DownloadResult downloadResult) {
                            Log.e("getImageBitmap:", "download file size:" + downLoadPath + "--download percent is " + (downloadResult.getBytesTransferred() / downloadResult.getTotalByteCount())*100);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            // 下载失败，返回失败错误码
                            Log.e("getImageBitmap:", "DownloadTask failed:" + e.getMessage());
                        }
                    });
                }
            }).start();

        }
        return localStoragePath;
    }

    public static Bitmap getBitmapFromFile(String filePath) throws FileNotFoundException {
        Log.e("getBitmapFromFile", "this is getBitmapFromFile");
        Bitmap bitmap = null;

        try (FileInputStream fis = new FileInputStream(filePath)) {
            bitmap = BitmapFactory.decodeStream(fis);

            Log.e("getBitmapFromFile", "return bit  success");

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("getBitmapFromFile", "return bit  fail " + e.getMessage());
        }
        return bitmap;
    }

    public static String getAGCSdkDirPath() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AGC/";
        System.out.println("path=" + path);
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }

}
