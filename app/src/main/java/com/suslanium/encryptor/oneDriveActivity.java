package com.suslanium.encryptor;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.microsoft.onedrivesdk.picker.IPicker;
import com.microsoft.onedrivesdk.picker.IPickerResult;
import com.microsoft.onedrivesdk.picker.LinkType;
import com.microsoft.onedrivesdk.picker.Picker;
import com.microsoft.onedrivesdk.saver.ISaver;
import com.microsoft.onedrivesdk.saver.Saver;
import com.microsoft.onedrivesdk.saver.SaverException;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;

public class oneDriveActivity extends AppCompatActivity {

    private IPicker mPicker;
    private ISaver mSaver;
    private String ONEDRIVE_APP_ID = "4a85af0e-df80-4f4f-a172-625d168df915";
    private byte[] pass;
    private static final int FILE_SELECT_CODE = 123;
    private View view;

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    //TODO: this shit is not working, maybe just share it to OneDrive?
                    Uri uri = data.getData();
                    String path = getPathFromUri(oneDriveActivity.this, uri);
                    //Log.d("a", path);
                    File file = new File(path);
                    ArrayList<String> paths = new ArrayList<>();
                    paths.add(file.getPath());
                    Intent intent2 = new Intent(oneDriveActivity.this, EncryptorService.class);
                    intent2.putExtra("actionType", "E2");
                    intent2.putExtra("paths", paths);
                    intent2.putExtra("pass", pass);
                    ContextCompat.startForegroundService(oneDriveActivity.this, intent2);
                    Snackbar.make(view, "Your file is being encrypted, please wait.", Snackbar.LENGTH_LONG).show();
                    // Initiate the upload
                }
                break;
            case 1234:
                try {
                    mSaver.handleSave(requestCode, resultCode, data);
                } catch ( SaverException e) {
                    // Log error information
                    Log.e("OneDriveSaver", e.getErrorType().toString()); // Provides one of the SaverError enum
                    Log.d("OneDriveSaver", e.getDebugErrorInfo()); // Detailed debug error message
                }
                break;
            case 12345:
                IPickerResult result = mPicker.getPickerResult(requestCode, resultCode, data);
                // Handle the case if nothing was picked
                if (result != null) {
                    // Do something with the picked file
                    //Log.d("A", "Link to file '" + result.getName() + ": " + result.getLink());
                    DownloadManager.Request request = new DownloadManager.Request(result.getLink());
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/Encryptor/" + result.getName());
                    DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    manager.enqueue(request);
                    BroadcastReceiver onComplete = new BroadcastReceiver() {
                        public void onReceive(Context ctxt, Intent intent) {
                            File fileToDecrypt = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Encryptor/" + result.getName());
                            ArrayList<String> paths = new ArrayList<>();
                            paths.add(fileToDecrypt.getPath());
                            Intent intent2 = new Intent(oneDriveActivity.this, EncryptorService.class);
                            intent2.putExtra("actionType", "D2");
                            intent2.putExtra("paths", paths);
                            intent2.putExtra("pass", pass);
                            ContextCompat.startForegroundService(oneDriveActivity.this, intent2);
                        }
                    };
                    registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                    return;
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = this.findViewById(android.R.id.content);
        pass = getIntent().getByteArrayExtra("pass");
        String path = getIntent().getStringExtra("path");
        if(path != null){
            mSaver = Saver.createSaver(ONEDRIVE_APP_ID);
            mSaver.setRequestCode(1234);
            mSaver.startSaving((Activity)view.getContext(), new File(path).getName(), Uri.fromFile(new File(path)));
        }
        setContentView(R.layout.activity_one_drive);
        Button download = findViewById(R.id.downloadButton);
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPicker = Picker.createPicker(ONEDRIVE_APP_ID);
                mPicker.setRequestCode(12345);
                mPicker.startPicking((Activity) v.getContext(), LinkType.DownloadLink);
            }
        });
        Button upload = findViewById(R.id.uploadButton);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser();
            }
        });
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }

    }
    public static String getPathFromUri(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}