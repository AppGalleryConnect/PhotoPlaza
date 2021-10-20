package com.huawei.agc.photoplaza.viewAndAdapter;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class UtilTool {
    public static String currentUserName;
    public static String currentLoginUid;
    public static String loginAccount;
    public static Integer LoginMod;
    private static final String TAG = "photoPlaza_";

    // Image fileName is UID + timeStamp of the current
    public static String getFileName() {
        if (currentLoginUid == null) {
            Log.e(TAG + "getFileName", "currentLoginUid is null");
            return null;
        }
        return currentLoginUid + "_" + System.currentTimeMillis();
    }

    @SuppressLint("NewApi")
    public static String getPathFromUri(final Context context, final Uri uri) {
        if (uri == null) {
            return null;
        }
        // Check whether the version is later than Android 4.4
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // If the version is later than Android 4.4 and belongs to the file URI
            final String authority = uri.getAuthority();
            // Check whether the authority is used by the local archive.
            if ("com.android.externalstorage.documents".equals(authority)) {
                // External storage space
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] divide = docId.split(":");
                final String type = divide[0];
                if ("primary".equals(type)) {
                    return Environment.getExternalStorageDirectory().getAbsolutePath().concat("/").concat(divide[1]);
                } else {
                    return "/storage/".concat(type).concat("/").concat(divide[1]);
                }
            } else if ("com.android.providers.downloads.documents".equals(authority)) {
                // Download Table of Contents
                final String docId = DocumentsContract.getDocumentId(uri);
                if (docId.startsWith("raw:")) {
                    return docId.replaceFirst("raw:", "");
                }
                final Uri downloadUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(docId));
                return queryAbsolutePath(context, downloadUri);
            } else if ("com.android.providers.media.documents".equals(authority)) {
                // Picture Archive
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] divide = docId.split(":");
                final String type = divide[0];
                Uri mediaUri;
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
                return queryAbsolutePath(context, mediaUri);
            }
        } else {
            final String scheme = uri.getScheme();
            String path = null;
            if ("content".equals(scheme)) {
                // Content URI
                path = queryAbsolutePath(context, uri);
            } else if ("file".equals(scheme)) {
                // Archive URI
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

}
