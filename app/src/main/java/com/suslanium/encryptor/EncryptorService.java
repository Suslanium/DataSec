package com.suslanium.encryptor;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EncryptorService extends Service {
    private static final String CHANNEL_ID = "Encryptor";
    private int id = 1;
    private ArrayList<Boolean> isRunning = new ArrayList<>();
    public static HashMap<Integer, ArrayList<String>> paths = new HashMap<>();
    public static HashMap<Integer, String> path = new HashMap<>();
    public static HashMap<Integer, String> originalPath = new HashMap<>();
    public static HashMap<Integer, ArrayList<String>> originalPaths = new HashMap<>();
    public static HashMap<Integer, ArrayList<String>> names = new HashMap<>();
    public static HashMap<Integer, ArrayList<String>> mimeTypes = new HashMap<>();
    public static HashMap<Integer, HashMap<String, String>> folderReplacements = new HashMap<>();
    public static HashMap<Integer, Boolean> deletingFiles = new HashMap<>();
    public static boolean changingPassword = false;
    public static Integer uniqueID = 0;
    public static int backupRestoreReturn = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.encDec);
            String description = getString(R.string.channelDesc);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.locked)
                .setContentTitle(getString(R.string.serviceRunning))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        startForeground(++id, builder.build());
        String actionType = intent.getStringExtra("actionType");
        int index = intent.getIntExtra("index", 0);
        ArrayList<String> pathsList = EncryptorService.paths.remove(index);
        ArrayList<String> namesList = EncryptorService.names.remove(index);
        ArrayList<String> mimesList = EncryptorService.mimeTypes.remove(index);
        byte[] pass = intent.getByteArrayExtra("pass");
        String currentFolderID = intent.getStringExtra("gDriveFolder");
        isRunning.add(true);
        int operationID = ++id;
        switch (actionType) {
            case "E": {
                Thread thread = new Thread(() -> {
                    try {
                        SharedPreferences editor = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                        boolean autoDelete = editor.getBoolean("auto_Delete", false);
                        ArrayList<String> encryptedPaths = new ArrayList<>();
                        String password = Encryptor.rsadecrypt(pass);
                        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.encrypting))
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager1.notify(operationID, builder1.build());
                        final int[] errorsCount = {0};
                        if (pathsList != null) {
                            for (String currentPath : pathsList) {
                                File file = new File(currentPath);
                                if (file.isDirectory()) {
                                    File encdir = new File(currentPath + "Enc");
                                    if (encdir.exists()) {
                                        File[] currNames = encdir.listFiles();
                                        if (currNames != null) {
                                            for (int i = 0; i < currNames.length; i++) {
                                                String filePath = currNames[i].getPath();
                                                File pre = new File(filePath);
                                                if (pre.exists() && pre.isFile() && pre.getName().substring(pre.getName().length() - 4).matches(".enc"))
                                                    pre.delete();
                                                else if (pre.exists() && pre.isDirectory())
                                                    deleteEncDir(pre);
                                            }
                                        }
                                    }
                                    try {
                                        File file1 = new File(currentPath + "Enc");
                                        Encryptor.encryptFolderAESGCM(file, password, file1, getBaseContext());
                                        if (autoDelete) file.delete();
                                        encryptedPaths.add(file1.getPath());
                                    } catch (Exception e) {
                                        NotificationCompat.Builder builder22 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                                .setSmallIcon(R.drawable.locked)
                                                .setContentTitle(getString(R.string.serviceErrorEnc)+" " + file.getName())
                                                .setOngoing(true)
                                                .setProgress(1, 0, true)
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                        notificationManager.notify(operationID, builder22.build());
                                        errorsCount[0]++;
                                    }
                                } else {
                                    new File(currentPath + ".enc").delete();
                                    try {
                                        File file1 = new File(currentPath + ".enc");
                                        Encryptor.encryptFileAES256(file, password, file1);
                                        if (autoDelete) file.delete();
                                        encryptedPaths.add(file1.getPath());
                                    } catch (Exception e) {
                                        NotificationCompat.Builder builder22 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                                .setSmallIcon(R.drawable.locked)
                                                .setContentTitle(getString(R.string.serviceErrorEnc)+" " + file.getName())
                                                .setOngoing(true)
                                                .setProgress(1, 0, true)
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                        notificationManager.notify(operationID, builder22.build());
                                        errorsCount[0]++;
                                    }
                                }
                            }
                        }
                        ArrayList<String> filesWSub = constructFilePaths(encryptedPaths);
                        ArrayList<Uri> uris = new ArrayList<>();
                        if (!filesWSub.isEmpty()) {
                            for (int j = 0; j < filesWSub.size(); j++) {
                                uris.add(FileProvider.getUriForFile(getBaseContext(), "com.suslanium.encryptor.fileprovider", new File(filesWSub.get(j))));
                            }
                        }
                        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                        shareIntent.setType("*/*");
                        shareIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uris);
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 12345, shareIntent, 0);
                        NotificationCompat.Builder builder22 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.serviceEncSuccess))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .addAction(R.drawable.checkmark, getString(R.string.share), pendingIntent);
                        if (errorsCount[0] > 0)
                            builder22.setContentText(getString(R.string.errors)+" " + errorsCount[0]);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager.notify(operationID, builder22.build());
                        isRunning.remove(true);
                        if (!isRunning.contains(true)) stopSelf();
                    } catch (Exception e) {
                        isRunning.remove(true);
                        if (!isRunning.contains(true)) stopSelf();
                    }
                });
                thread.start();
                break;
            }
            case "D": {
                Thread thread = new Thread(() -> {
                    try {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                        boolean autoDelete2 = preferences.getBoolean("auto_Delete2", false);
                        String password = Encryptor.rsadecrypt(pass);
                        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.decrypting))
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager1.notify(operationID, builder1.build());
                        final int[] errorsCount = {0};
                        if (pathsList != null) {
                            for (String currPath : pathsList) {
                                File file = new File(currPath);
                                if (file.isDirectory()) {
                                    String pathname = (currPath).substring(0, (currPath).length() - 3);
                                    File file1 = new File(pathname);
                                    if (file1.exists() && file1.isDirectory()) {
                                        File[] namesEnc = file.listFiles();
                                        ArrayList<String> encNames = new ArrayList<>();
                                        if (namesEnc != null) {
                                            for (int i = 0; i < namesEnc.length; i++) {
                                                if (namesEnc[i].getName().length() > 4 && namesEnc[i].isFile()) {
                                                    encNames.add(namesEnc[i].getName().substring(0, namesEnc[i].getName().length() - 4));
                                                } else if (namesEnc[i].getName().length() > 3 && namesEnc[i].isDirectory()) {
                                                    encNames.add(namesEnc[i].getName().substring(0, namesEnc[i].getName().length() - 3) + "Dir");
                                                }
                                            }
                                            File[] currNames = file1.listFiles();
                                            if (currNames != null) {
                                                for (int i = 0; i < currNames.length; i++) {
                                                    String filePath = currNames[i].getPath();
                                                    File pre = new File(filePath);
                                                    if (pre.exists() && pre.isFile() && encNames.contains(pre.getName()))
                                                        pre.delete();
                                                    else if (pre.exists() && pre.isDirectory() && encNames.contains(pre.getName() + "Dir"))
                                                        deleteDecDir(pre, new File(currPath + File.separator + pre.getName() + "Enc"));
                                                }
                                            }
                                        }
                                    }
                                    try {
                                        Encryptor.decryptFolderAESGCM(file, password, new File(pathname), getBaseContext(), false);
                                        if (autoDelete2) file.delete();
                                    } catch (Exception e) {
                                        NotificationCompat.Builder builder23 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                                .setSmallIcon(R.drawable.locked)
                                                .setContentTitle(getString(R.string.serviceDecryptionError)+" " + file.getName())
                                                .setOngoing(true)
                                                .setProgress(1, 0, true)
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                        notificationManager.notify(operationID, builder23.build());
                                        errorsCount[0]++;
                                    }
                                } else {
                                    File file1 = new File(currPath.substring(0, (currPath).length() - 4));
                                    file1.delete();
                                    try {
                                        Encryptor.decryptFileAES256(file, password, file1);
                                        if (autoDelete2) file.delete();
                                    } catch (Exception e) {
                                        NotificationCompat.Builder builder23 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                                .setSmallIcon(R.drawable.locked)
                                                .setContentTitle(getString(R.string.serviceDecryptionError)+" " + file.getName())
                                                .setOngoing(true)
                                                .setProgress(1, 0, true)
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                        notificationManager.notify(operationID, builder23.build());
                                        errorsCount[0]++;
                                    }
                                }
                            }
                        }
                        NotificationCompat.Builder builder23 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.serviceDecSuccess))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        if (errorsCount[0] > 0)
                            builder23.setContentText(getString(R.string.errors)+" " + errorsCount[0]);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager.notify(operationID, builder23.build());
                        isRunning.remove(true);
                        if (!isRunning.contains(true)) stopSelf();
                    } catch (Exception ignored) {
                        isRunning.remove(true);
                        if (!isRunning.contains(true)) stopSelf();
                    }
                });
                thread.start();
                break;
            }
            case "gDriveE": {
                Thread thread = new Thread(() -> {
                    try {
                        String password = Encryptor.rsadecrypt(pass);
                        ArrayList<File> encryptedFiles = new ArrayList<>();
                        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.encrypting))
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager1.notify(operationID, builder1.build());
                        final int[] errorsCount = {0};
                        if (pathsList != null) {
                            for (String currPath : pathsList) {
                                File file = new File(currPath);
                                if (file.isDirectory()) {
                                    File encdir = new File(getCacheDir() + File.separator + file.getName() + "Enc");
                                    if (encdir.exists()) {
                                        File[] currNames = encdir.listFiles();
                                        for (int i = 0; i < currNames.length; i++) {
                                            String filePath = currNames[i].getPath();
                                            File pre = new File(filePath);
                                            if (pre.exists() && pre.isFile() && pre.getName().substring(pre.getName().length() - 4).matches(".enc"))
                                                pre.delete();
                                            else if (pre.exists() && pre.isDirectory())
                                                deleteEncDir(pre);
                                        }
                                    }
                                    try {
                                        Encryptor.encryptFolderAESGCM(file, password, new File(getCacheDir() + File.separator + file.getName() + "Enc"), null);
                                        encryptedFiles.add(new File(getCacheDir() + File.separator + file.getName() + "Enc"));
                                    } catch (Exception e) {
                                        NotificationCompat.Builder builder24 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                                .setSmallIcon(R.drawable.locked)
                                                .setContentTitle(getString(R.string.serviceErrorEnc)+" " + file.getName())
                                                .setOngoing(true)
                                                .setProgress(1, 0, true)
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                        notificationManager.notify(operationID, builder24.build());
                                        errorsCount[0]++;
                                    }
                                } else {
                                    new File(getCacheDir() + File.separator + file.getName() + ".enc").delete();
                                    try {
                                        Encryptor.encryptFileAES256(file, password, new File(getCacheDir() + File.separator + file.getName() + ".enc"));
                                        encryptedFiles.add(new File(getCacheDir() + File.separator + file.getName() + ".enc"));
                                    } catch (Exception e) {
                                        NotificationCompat.Builder builder24 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                                .setSmallIcon(R.drawable.locked)
                                                .setContentTitle(getString(R.string.serviceErrorEnc)+" " + file.getName())
                                                .setOngoing(true)
                                                .setProgress(1, 0, true)
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                        notificationManager.notify(operationID, builder24.build());
                                        errorsCount[0]++;
                                    }
                                }
                            }
                        }
                        NotificationCompat.Builder builder24 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.uploadingTo))
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        if (errorsCount[0] > 0)
                            builder24.setContentText(getString(R.string.errors)+" " + errorsCount[0]);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager.notify(operationID, builder24.build());
                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .build();
                        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getApplicationContext(), gso);
                        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                        if (acct != null) {
                            GoogleSignInAccount mAccount = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Collections.singleton(Scopes.DRIVE_APPFOLDER));
                            if (mAccount != null) {
                                credential.setSelectedAccount(mAccount.getAccount());
                            }
                            Drive googleDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName(CHANNEL_ID).build();
                            DriveServiceHelper mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                            List<com.google.api.services.drive.model.File> driveFiles = mDriveServiceHelper.listDriveFiles(currentFolderID);
                            ArrayList<String> driveFileNames = new ArrayList<>();
                            if (driveFiles != null) {
                                for (int i = 0; i < driveFiles.size(); i++) {
                                    driveFileNames.add(driveFiles.get(i).getName());
                                    Log.d("fileName", driveFiles.get(i).getName());
                                }
                            }
                            for (int i = 0; i < encryptedFiles.size(); i++) {
                                if (encryptedFiles.get(i).isFile()) {
                                    if (driveFileNames.contains(encryptedFiles.get(i).getName())) {
                                        for (int j = 0; j < driveFiles.size(); j++) {
                                            if (driveFiles.get(j).getName().equals(encryptedFiles.get(i).getName())) {
                                                mDriveServiceHelper.deleteFolderFile(driveFiles.get(j).getId());
                                                mDriveServiceHelper.uploadFile(encryptedFiles.get(i), currentFolderID);
                                                break;
                                            }
                                        }
                                    } else {
                                        mDriveServiceHelper.uploadFile(encryptedFiles.get(i), currentFolderID);
                                    }
                                } else {
                                    File[] files = encryptedFiles.get(i).listFiles();
                                    ArrayList<String> filePaths = new ArrayList<>();
                                    if (files != null && files.length > 0) {
                                        for (int u = 0; u < files.length; u++) {
                                            filePaths.add(files[u].getPath());
                                        }
                                    }
                                    if (driveFileNames.contains(encryptedFiles.get(i).getName())) {
                                        for (int j = 0; j < driveFiles.size(); j++) {
                                            if (driveFiles.get(j).getName().equals(encryptedFiles.get(i).getName()) && driveFiles.get(j).getMimeType().equals("application/vnd.google-apps.folder")) {
                                                String folderID = driveFiles.get(j).getId();
                                                uploadFilesInFolder(folderID, mDriveServiceHelper, filePaths);
                                            } else if(driveFiles.get(j).getName().equals(encryptedFiles.get(i).getName())){
                                                mDriveServiceHelper.deleteFolderFile(driveFiles.get(j).getId());
                                                GoogleDriveFileHolder folder = mDriveServiceHelper.createFolder(encryptedFiles.get(i).getName(), currentFolderID);
                                                String folderID = folder.getId();
                                                uploadFilesInFolder(folderID, mDriveServiceHelper, filePaths);
                                            }
                                        }
                                    } else {
                                        GoogleDriveFileHolder folder = mDriveServiceHelper.createFolder(encryptedFiles.get(i).getName(), currentFolderID);
                                        String folderID = folder.getId();
                                        uploadFilesInFolder(folderID, mDriveServiceHelper, filePaths);
                                    }
                                }
                            }
                        }
                        for (int i = 0; i < encryptedFiles.size(); i++) {
                            if (encryptedFiles.get(i).isFile()) encryptedFiles.get(i).delete();
                            else {
                                deleteFolder(encryptedFiles.get(i));
                            }
                        }
                        NotificationCompat.Builder builder3 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.serviceGEncSuccess))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        if (errorsCount[0] > 0)
                            builder3.setContentText(getString(R.string.errors) + errorsCount[0]);
                        NotificationManagerCompat notificationManager3 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager3.notify(operationID, builder3.build());
                        isRunning.remove(true);
                        if (!isRunning.contains(true)) stopSelf();
                    } catch (Exception e) {
                        e.printStackTrace();
                        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.serviceGEncError))
                                .setContentText(e.getMessage())
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager2 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager2.notify(operationID, builder2.build());
                        isRunning.remove(true);
                        if (!isRunning.contains(true)) stopSelf();
                    }
                });
                thread.start();
                break;
            }
            case "gDriveD": {
                Thread thread = new Thread(() -> {
                    try {
                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .build();
                        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getApplicationContext(), gso);
                        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                        if (acct != null) {
                            NotificationCompat.Builder builder2 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle(getString(R.string.serviceDownloading))
                                    .setOngoing(true)
                                    .setProgress(1, 0, true)
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManagerCompat notificationManager2 = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager2.notify(operationID, builder2.build());
                            GoogleSignInAccount mAccount = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Collections.singleton(Scopes.DRIVE_APPFOLDER));
                            if (mAccount != null) {
                                credential.setSelectedAccount(mAccount.getAccount());
                            }
                            Drive googleDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName(CHANNEL_ID).build();
                            DriveServiceHelper mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                            new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "EncryptorDownloads").mkdirs();
                            ArrayList<File> downloadedFiles = null;
                            if (pathsList != null) {
                                downloadedFiles = gDriveDownloadFiles(mDriveServiceHelper, pathsList, namesList, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "EncryptorDownloads",mimesList);
                            }
                            String password = Encryptor.rsadecrypt(pass);
                            NotificationCompat.Builder builder1 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle(getString(R.string.decrypting))
                                    .setOngoing(true)
                                    .setProgress(1, 0, true)
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager1.notify(operationID, builder1.build());
                            final int[] errorsCount = {0};
                            ArrayList<String> downloadedPaths = new ArrayList<>();
                            if (downloadedFiles != null && !downloadedFiles.isEmpty()) {
                                for (int i = 0; i < downloadedFiles.size(); i++) {
                                    downloadedPaths.add(downloadedFiles.get(i).getPath());
                                }
                                for (String currPath : downloadedPaths) {
                                    File file = new File(currPath);
                                    if (file.isDirectory()) {
                                        File file1 = new File((currPath).substring(0, (currPath).length() - 3));
                                        if (file1.exists() && file1.isDirectory()) {
                                            File[] namesEnc = file.listFiles();
                                            ArrayList<String> encNames = new ArrayList<>();
                                            if (namesEnc != null) {
                                                for (int i = 0; i < namesEnc.length; i++) {
                                                    if (namesEnc[i].getName().length() > 4 && namesEnc[i].isFile()) {
                                                        encNames.add(namesEnc[i].getName().substring(0, namesEnc[i].getName().length() - 4));
                                                    } else if (namesEnc[i].getName().length() > 3 && namesEnc[i].isDirectory()) {
                                                        encNames.add(namesEnc[i].getName().substring(0, namesEnc[i].getName().length() - 3) + "Dir");
                                                    }
                                                }
                                                File[] currNames = file1.listFiles();
                                                for (int i = 0; i < currNames.length; i++) {
                                                    String filePath = currNames[i].getPath();
                                                    File pre = new File(filePath);
                                                    if (pre.exists() && pre.isFile() && encNames.contains(pre.getName()))
                                                        pre.delete();
                                                    else if (pre.exists() && pre.isDirectory() && encNames.contains(pre.getName() + "Dir"))
                                                        deleteDecDir(pre, new File(currPath + File.separator + pre.getName() + "Enc"));
                                                }
                                            }
                                        }
                                        try {
                                            Encryptor.decryptFolderAESGCM(file, password, file1, null, true);
                                        } catch (Exception e) {
                                            NotificationCompat.Builder builder32 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                                    .setSmallIcon(R.drawable.locked)
                                                    .setContentTitle(getString(R.string.serviceDecryptionError)+" " + file.getName())
                                                    .setOngoing(true)
                                                    .setProgress(1, 0, true)
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                            notificationManager.notify(operationID, builder32.build());
                                            errorsCount[0]++;
                                        }
                                    } else {
                                        File file1 = new File(currPath.substring(0, (currPath).length() - 4));
                                        file1.delete();
                                        try {
                                            Encryptor.decryptFileAES256(file, password, file1);
                                            file.delete();
                                        } catch (Exception e) {
                                            NotificationCompat.Builder builder32 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                                    .setSmallIcon(R.drawable.locked)
                                                    .setContentTitle(getString(R.string.serviceDecryptionError)+" " + file.getName())
                                                    .setOngoing(true)
                                                    .setProgress(1, 0, true)
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                            notificationManager.notify(operationID, builder32.build());
                                            errorsCount[0]++;
                                        }
                                    }
                                }
                            }
                            NotificationCompat.Builder builder32 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle(getString(R.string.serviceGDecSuccess))
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            if (errorsCount[0] > 0)
                                builder32.setContentText(getString(R.string.errors)+" " + errorsCount[0]);
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager.notify(operationID, builder32.build());
                            isRunning.remove(true);
                            if (!isRunning.contains(true)) stopSelf();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.serviceDecryptionError)+"...")
                                .setContentText(e.getMessage())
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager2 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager2.notify(operationID, builder2.build());
                    }
                });
                thread.start();
                break;
            }
            case "gDriveDelete": {
                Thread thread = new Thread(() -> {
                    try {
                        NotificationCompat.Builder builder3 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.serviceGDelete))
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager3 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager3.notify(operationID, builder3.build());
                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .build();
                        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getApplicationContext(), gso);
                        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                        if (acct != null) {
                            GoogleSignInAccount mAccount = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Collections.singleton(Scopes.DRIVE_APPFOLDER));
                            if (mAccount != null) {
                                credential.setSelectedAccount(mAccount.getAccount());
                            }
                            Drive googleDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName(CHANNEL_ID).build();
                            DriveServiceHelper mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                            for (int i = 0; i < pathsList.size(); i++) {
                                mDriveServiceHelper.deleteFolderFile(pathsList.get(i));
                            }
                        }
                        NotificationCompat.Builder builder12 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.serviceGDeleteSuccess))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager.notify(operationID, builder12.build());
                        isRunning.remove(true);
                        if (!isRunning.contains(true)) stopSelf();
                    } catch (Exception e) {
                        NotificationCompat.Builder builder12 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.serviceGDeleteError))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager.notify(operationID, builder12.build());
                        isRunning.remove(true);
                        if (!isRunning.contains(true)) stopSelf();
                    }
                });
                thread.start();
                break;
            }
            case "changePass": {
                byte[] newPassEnc = intent.getByteArrayExtra("newPass");
                Thread thread = new Thread(() -> {
                    File toDownload = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "EncryptorDownloads" + File.separator + "database.db");
                    File origin = new File(getBaseContext().getApplicationInfo().dataDir + File.separator + "database.db");
                    String password = null;
                    try {
                        password = Encryptor.rsadecrypt(pass);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String newPass = null;
                    try {
                        newPass = Encryptor.rsadecrypt(newPassEnc);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if(origin.exists()) {
                        try {
                            EncryptorService.changingPassword = true;
                            deleteFiles(toDownload.getPath());
                            try (InputStream in = new BufferedInputStream(new FileInputStream(origin)); OutputStream out = new BufferedOutputStream(new FileOutputStream(toDownload))) {
                                byte[] buffer = new byte[256 * 1024];
                                int lengthRead;
                                while ((lengthRead = in.read(buffer)) > 0) {
                                    out.write(buffer, 0, lengthRead);
                                    out.flush();
                                }
                                in.close();
                                origin.delete();
                                SQLiteDatabase downloadedDatabase = Encryptor.openDownloadedTable(toDownload, password, getBaseContext());
                                SQLiteDatabase database = Encryptor.initDataBase(getBaseContext(), newPass);
                                ArrayList<String> categories = Encryptor.getCategories(downloadedDatabase);
                                for (int i = 0; i < categories.size(); i++) {
                                    Encryptor.createCategoryStub(database, categories.get(i));
                                }
                                Cursor cursor = downloadedDatabase.rawQuery("SELECT * FROM passwordTable", null);
                                if (cursor.moveToFirst()) {
                                    do {
                                        if (cursor.getInt(8) == 0) {
                                            Encryptor.insertDataIntoPasswordTable(database, cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getBlob(4), cursor.getString(5), cursor.getString(6), cursor.getString(7));
                                        }
                                    } while (cursor.moveToNext());
                                }
                                cursor.close();
                                Encryptor.closeDataBase(database);
                                Encryptor.closeDataBase(downloadedDatabase);
                                toDownload.delete();
                                MasterKey mainKey = new MasterKey.Builder(getBaseContext())
                                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                        .build();
                                SharedPreferences editor = EncryptedSharedPreferences.create(getBaseContext(), "encryptor_shared_prefs", mainKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                                SharedPreferences.Editor edit = editor.edit();
                                edit.putString("passHash", Encryptor.calculateHash(newPass, "SHA-512"));
                                edit.apply();
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (toDownload.exists() && !origin.exists()) {
                                    try (InputStream in = new BufferedInputStream(new FileInputStream(toDownload)); OutputStream out = new BufferedOutputStream(new FileOutputStream(origin))) {
                                        byte[] buffer = new byte[256 * 1024];
                                        int lengthRead;
                                        while ((lengthRead = in.read(buffer)) > 0) {
                                            out.write(buffer, 0, lengthRead);
                                            out.flush();
                                        }
                                        in.close();
                                        toDownload.delete();
                                    } catch (Exception e0) {
                                        e0.printStackTrace();
                                    }
                                }
                            } finally {
                                EncryptorService.changingPassword = false;
                                isRunning.remove(true);
                                if (!isRunning.contains(true)) stopSelf();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            isRunning.remove(true);
                            if (!isRunning.contains(true)) stopSelf();
                        }
                    } else {
                        try {
                            MasterKey mainKey = new MasterKey.Builder(getBaseContext())
                                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                    .build();
                            SharedPreferences editor = EncryptedSharedPreferences.create(getBaseContext(), "encryptor_shared_prefs", mainKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                            SharedPreferences.Editor edit = editor.edit();
                            edit.putString("passHash", Encryptor.calculateHash(newPass, "SHA-512"));
                            edit.apply();
                        } catch (Exception e){

                        }
                        EncryptorService.changingPassword = false;
                        isRunning.remove(true);
                        if (!isRunning.contains(true)) stopSelf();
                    }
                });
                thread.start();
                break;
            }
            case "delete": {
                Thread thread = new Thread(() -> {
                    EncryptorService.deletingFiles.put(index, true);
                    NotificationCompat.Builder builder3 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.locked)
                            .setContentTitle(getString(R.string.serviceDelete))
                            .setOngoing(true)
                            .setProgress(1, 0, true)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                    NotificationManagerCompat notificationManager3 = NotificationManagerCompat.from(EncryptorService.this);
                    notificationManager3.notify(operationID, builder3.build());
                    if (pathsList != null) {
                        deleteFiles(pathsList);
                    }
                    notificationManager3.cancel(operationID);
                    EncryptorService.deletingFiles.remove(index);
                    isRunning.remove(true);
                    if (!isRunning.contains(true)) stopSelf();
                });
                thread.start();
                break;
            }
            case "copyFiles": {
                String currentPath = EncryptorService.path.remove(index);
                String currentOriginalPath = EncryptorService.originalPath.remove(index);
                HashMap<String, String> pathsToReplace = EncryptorService.folderReplacements.remove(index);
                Thread thread = new Thread(() -> {
                    NotificationCompat.Builder builder3 = null;
                    if (pathsList != null) {
                        builder3 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.serviceCopy))
                                .setOngoing(true)
                                .setProgress(pathsList.size(), 0, false)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager3 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager3.notify(operationID, builder3.build());
                        copyFiles(pathsList, currentPath, currentOriginalPath, false, null, operationID, pathsToReplace);
                    }
                });
                thread.start();
                break;
            }
            case "moveFiles": {
                String currentPath = EncryptorService.path.remove(index);
                String currentOriginalPath = EncryptorService.originalPath.remove(index);
                HashMap<String, String> pathsToReplace = EncryptorService.folderReplacements.remove(index);
                ArrayList<String> currentOriginalPaths = EncryptorService.originalPaths.remove(index);
                Thread thread = new Thread(() -> {
                    if (pathsList != null) {
                        NotificationCompat.Builder builder3 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle(getString(R.string.serviceMove))
                                .setOngoing(true)
                                .setProgress(pathsList.size(), 0, false)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager3 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager3.notify(operationID, builder3.build());
                        copyFiles(pathsList, currentPath, currentOriginalPath, true, currentOriginalPaths, operationID, pathsToReplace);
                    }
                });
                thread.start();
                break;
            }
            case "gDriveDBU": {
                EncryptorService.deletingFiles.put(index, true);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            File fileToUpload = new File(getBaseContext().getApplicationInfo().dataDir + File.separator + "database.db");
                            if (fileToUpload.exists()) {
                                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestEmail()
                                        .build();
                                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getApplicationContext(), gso);
                                GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                                if (acct != null) {
                                    GoogleSignInAccount mAccount = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                                    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Collections.singleton(Scopes.DRIVE_APPFOLDER));
                                    if (mAccount != null) {
                                        credential.setSelectedAccount(mAccount.getAccount());
                                    }
                                    Drive googleDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName(CHANNEL_ID).build();
                                    DriveServiceHelper mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                                    List<com.google.api.services.drive.model.File> driveFiles = mDriveServiceHelper.listDriveFiles(null);
                                    ArrayList<String> driveFileNames = new ArrayList<>();
                                    if (driveFiles != null) {
                                        for (int i = 0; i < driveFiles.size(); i++) {
                                            driveFileNames.add(driveFiles.get(i).getName());
                                        }
                                    }
                                    if (driveFileNames.contains(fileToUpload.getName())) {
                                        for (int j = 0; j < driveFiles.size(); j++) {
                                            if (driveFiles.get(j).getName().equals(fileToUpload.getName())) {
                                                mDriveServiceHelper.deleteFolderFile(driveFiles.get(j).getId());
                                                mDriveServiceHelper.uploadFile(fileToUpload, null);
                                                break;
                                            }
                                        }
                                    } else {
                                        mDriveServiceHelper.uploadFile(fileToUpload, null);
                                    }
                                }
                            } else {
                                EncryptorService.backupRestoreReturn = 2;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            EncryptorService.backupRestoreReturn = 3;
                        }
                        EncryptorService.deletingFiles.remove(index);
                        isRunning.remove(true);
                        if (!isRunning.contains(true)) stopSelf();
                    }
                });
                thread.start();
                break;
            }
            case "gDriveDBD": {
                EncryptorService.deletingFiles.put(index, true);
                boolean mergeDatabases = intent.getBooleanExtra("mergeData", true);
                Thread thread = new Thread(() -> {
                    try {
                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .build();
                        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getApplicationContext(), gso);
                        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                        if (acct != null) {
                            GoogleSignInAccount mAccount = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Collections.singleton(Scopes.DRIVE_APPFOLDER));
                            if (mAccount != null) {
                                credential.setSelectedAccount(mAccount.getAccount());
                            }
                            Drive googleDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName(CHANNEL_ID).build();
                            DriveServiceHelper mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                            List<com.google.api.services.drive.model.File> driveFiles = mDriveServiceHelper.listDriveFiles(null);
                            ArrayList<String> driveFileNames = new ArrayList<>();
                            if (driveFiles != null) {
                                for (int i = 0; i < driveFiles.size(); i++) {
                                    driveFileNames.add(driveFiles.get(i).getName());
                                }
                            }
                            if (driveFileNames.contains("database.db")) {
                                File toDownload = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "EncryptorDownloads" + File.separator + "database.db");
                                if (toDownload.exists())
                                    deleteFiles(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "EncryptorDownloads" + File.separator + "database.db");
                                for (int j = 0; j < driveFiles.size(); j++) {
                                    if (driveFiles.get(j).getName().equals("database.db")) {
                                        mDriveServiceHelper.downloadFile(toDownload, driveFiles.get(j).getId());
                                    }
                                }
                                if (toDownload.exists()) {
                                    String password = Encryptor.rsadecrypt(pass);
                                    if (Encryptor.checkDatabase(toDownload, password, getBaseContext())) {
                                        if (!mergeDatabases) {
                                            Encryptor.deleteDatabase(getBaseContext());
                                            try (InputStream in = new BufferedInputStream(new FileInputStream(toDownload)); OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(getBaseContext().getApplicationInfo().dataDir + File.separator + "database.db")))) {
                                                byte[] buffer = new byte[256 * 1024];
                                                int lengthRead;
                                                while ((lengthRead = in.read(buffer)) > 0) {
                                                    out.write(buffer, 0, lengthRead);
                                                    out.flush();
                                                }
                                                in.close();
                                                toDownload.delete();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            SQLiteDatabase downloadedDatabase = Encryptor.openDownloadedTable(toDownload, password, getBaseContext());
                                            SQLiteDatabase database = Encryptor.initDataBase(getBaseContext(), password);
                                            ArrayList<String> categories = Encryptor.getCategories(downloadedDatabase);
                                            ArrayList<String> originalCategories = Encryptor.getCategories(database);
                                            for (int i = 0; i < categories.size(); i++) {
                                                if (!originalCategories.contains(categories.get(i))) {
                                                    Encryptor.createCategoryStub(database, categories.get(i));
                                                }
                                            }
                                            Cursor cursor = downloadedDatabase.rawQuery("SELECT * FROM passwordTable", null);
                                            if (cursor.moveToFirst()) {
                                                do {
                                                    if(cursor.getInt(8) == 0) {
                                                        Encryptor.insertDataIntoPasswordTable(database, cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getBlob(4), cursor.getString(5), cursor.getString(6), cursor.getString(7));
                                                    }
                                                } while (cursor.moveToNext());
                                            }
                                            cursor.close();
                                            Encryptor.closeDataBase(database);
                                            Encryptor.closeDataBase(downloadedDatabase);
                                            toDownload.delete();
                                        }
                                    } else {
                                        toDownload.delete();
                                        EncryptorService.backupRestoreReturn = 1;
                                        //Password doesn't match
                                    }
                                }
                            } else {
                                EncryptorService.backupRestoreReturn = 2;
                                //No database found
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        EncryptorService.backupRestoreReturn = 3;
                    }
                    EncryptorService.deletingFiles.remove(index);
                    isRunning.remove(true);
                    if (!isRunning.contains(true)) stopSelf();
                });
                thread.start();
                break;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + actionType);
        }
        return START_REDELIVER_INTENT;
    }

    private void copyFiles(ArrayList<String> paths, String toCopyPath, String originalPath, boolean cut, ArrayList<String> originalPaths, int operationID, HashMap<String, String> pathsToReplace) {
        int errorCount = 0;
        int pathsSize = paths.size();
        NotificationManagerCompat manager = NotificationManagerCompat.from(EncryptorService.this);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                .setSmallIcon(R.drawable.locked)
                .setContentTitle("")
                .setOngoing(true)
                .setProgress(paths.size(), 0, false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        Set<String> keys = null;
        if (pathsToReplace != null && !pathsToReplace.isEmpty()) {
            keys = pathsToReplace.keySet();
        }
        int copiedCount = 0;
        ArrayList<String> doNotDelete = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            boolean rename = false;
            if (paths.get(i).startsWith("Rename_")) {
                rename = true;
                String rightPath = paths.get(i).replaceFirst(Pattern.quote("Rename_"), Matcher.quoteReplacement(""));
                paths.set(i, rightPath);
            }
            File original = new File(paths.get(i));
            String copyPath = paths.get(i).replaceFirst(Pattern.quote(originalPath), Matcher.quoteReplacement(""));
            String firstFolder = copyPath.substring(1);
            if (firstFolder.contains(File.separator)) {
                firstFolder = firstFolder.substring(0, firstFolder.indexOf(File.separator));
            }
            if (keys != null && keys.contains(firstFolder)) {
                copyPath = copyPath.replaceFirst(Pattern.quote(firstFolder), Matcher.quoteReplacement(pathsToReplace.get(firstFolder)));
            }
            File copied = new File(toCopyPath + copyPath);
            if (original.isFile()) {
                int j = 0;
                while (copied.exists() && copied.isDirectory()) {
                    copied = new File(toCopyPath + copyPath + "(" + j + ")");
                    j++;
                }
                String extension = null;
                if (copied.getName().contains(".")) {
                    extension = copied.getName().substring(copied.getName().lastIndexOf("."));
                }
                while (rename && copied.exists()) {
                    if (extension != null) {
                        copied = new File(copied.getPath().replace(extension, "") + "(" + j + ")" + extension);
                    } else {
                        copied = new File(copied.getPath() + "(" + j + ")");
                    }
                }
                if (!original.getPath().matches(Pattern.quote(copied.getPath()))) {
                    if (copied.exists() && copied.isFile() && !rename) copied.delete();
                    if (original.canRead() && original.canWrite()) {
                        try {
                            copied.getParentFile().mkdirs();
                        } catch (Exception ignored) {
                        }
                        int errorCountBefore = errorCount;
                        try (InputStream in = new BufferedInputStream(new FileInputStream(original)); OutputStream out = new BufferedOutputStream(new FileOutputStream(copied))) {
                            byte[] buffer = new byte[256 * 1024];
                            int lengthRead;
                            while ((lengthRead = in.read(buffer)) > 0) {
                                out.write(buffer, 0, lengthRead);
                                out.flush();
                            }
                            copiedCount++;
                        } catch (Exception e) {
                            e.printStackTrace();
                            errorCount++;
                        } finally {
                            if (cut && errorCountBefore == errorCount) original.delete();
                        }
                    }
                } else {
                    copiedCount++;
                }
            } else {
                try {
                    if (original.getPath().matches(Pattern.quote(copied.getPath()))) {
                        doNotDelete.add(original.getPath());
                    }
                    copied.mkdirs();
                } catch (Exception ignored) {

                }
            }
            if (cut) builder.setContentTitle(getString(R.string.serviceMoving)+" " + copiedCount + " "+getString(R.string.serviceFiles));
            else builder.setContentTitle(getString(R.string.serviceCopying)+" " + copiedCount + " "+getString(R.string.serviceFiles));
            builder.setProgress(pathsSize, i, false);
            manager.notify(operationID, builder.build());
        }
        if (cut && originalPaths != null && !originalPaths.isEmpty()) {
            for (int i = 0; i < originalPaths.size(); i++) {
                if (!doNotDelete.contains(originalPaths.get(i))) {
                    File file = new File(originalPaths.get(i));
                    if (file.exists() && file.isDirectory()) deleteFiles(originalPaths.get(i));
                }
            }
        }
        manager.cancel(operationID);
        if (cut) builder.setContentTitle(getString(R.string.serviceMoveSuccess)+" " + copiedCount + " "+getString(R.string.serviceFiles));
        else builder.setContentTitle(getString(R.string.serviceCopySuccess)+" " + copiedCount + " "+getString(R.string.serviceFiles));
        if (errorCount > 0) builder.setContentText(getString(R.string.errors) + errorCount);
        builder.setOngoing(false);
        builder.setProgress(0, 0, false);
        manager.notify(operationID, builder.build());
        isRunning.remove(true);
        if (!isRunning.contains(true)) stopSelf();
    }

    private void deleteFiles(ArrayList<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            File file = new File(paths.get(i));
            if (!file.isFile()) {
                File[] files = file.listFiles();
                if (files != null && files.length > 0) {
                    ArrayList<String> subPaths = new ArrayList<>();
                    for (int j = 0; j < files.length; j++) {
                        subPaths.add(files[j].getPath());
                    }
                    deleteFiles(subPaths);
                }
            }
            file.delete();
        }
    }

    private void deleteFiles(String path) {
        File file = new File(path);
        if (!file.isFile()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                ArrayList<String> subPaths = new ArrayList<>();
                for (int j = 0; j < files.length; j++) {
                    subPaths.add(files[j].getPath());
                }
                deleteFiles(subPaths);
            }
        }
        file.delete();
    }

    private void uploadFilesInFolder(String folderID, DriveServiceHelper mDriveServiceHelper, ArrayList<String> filePaths) throws Exception {
        List<com.google.api.services.drive.model.File> driveFiles = mDriveServiceHelper.listDriveFiles(folderID);
        ArrayList<String> driveFileNames = new ArrayList<>();
        if (driveFiles != null) {
            for (int i = 0; i < driveFiles.size(); i++) {
                driveFileNames.add(driveFiles.get(i).getName());
            }
        }
        ArrayList<File> encryptedFiles = new ArrayList<>();
        if (filePaths != null && !filePaths.isEmpty()) {
            for (int i = 0; i < filePaths.size(); i++) {
                encryptedFiles.add(new File(filePaths.get(i)));
            }
        }
        for (int i = 0; i < encryptedFiles.size(); i++) {
            if (encryptedFiles.get(i).isFile()) {
                if (driveFileNames.contains(encryptedFiles.get(i).getName())) {
                    if (driveFiles != null) {
                        for (int j = 0; j < driveFiles.size(); j++) {
                            if (driveFiles.get(j).getName().equals(encryptedFiles.get(i).getName())) {
                                mDriveServiceHelper.deleteFolderFile(driveFiles.get(j).getId());
                                mDriveServiceHelper.uploadFile(encryptedFiles.get(i), folderID);
                                break;
                            }
                        }
                    }
                } else {
                    mDriveServiceHelper.uploadFile(encryptedFiles.get(i), folderID);
                }
            } else {
                File[] files = encryptedFiles.get(i).listFiles();
                ArrayList<String> subFilePaths = new ArrayList<>();
                if (files != null && files.length > 0 && filePaths != null) {
                    for (int u = 0; u < files.length; u++) {
                        filePaths.add(files[u].getPath());
                    }
                }
                if (driveFileNames.contains(encryptedFiles.get(i).getName())) {
                    if (driveFiles != null) {
                        for (int j = 0; j < driveFiles.size(); j++) {
                            if (driveFiles.get(j).getName().equals(encryptedFiles.get(i).getName()) && driveFiles.get(j).getMimeType().equals("application/vnd.google-apps.folder")) {
                                String subFolderID = driveFiles.get(j).getId();
                                uploadFilesInFolder(subFolderID, mDriveServiceHelper, subFilePaths);
                            } else if(driveFiles.get(j).getName().equals(encryptedFiles.get(i).getName())){
                                mDriveServiceHelper.deleteFolderFile(driveFiles.get(j).getId());
                                GoogleDriveFileHolder folder = mDriveServiceHelper.createFolder(encryptedFiles.get(i).getName(), folderID);
                                String subFolderID = folder.getId();
                                uploadFilesInFolder(subFolderID, mDriveServiceHelper, subFilePaths);
                            }
                        }
                    }
                } else {
                    GoogleDriveFileHolder folder = mDriveServiceHelper.createFolder(encryptedFiles.get(i).getName(), folderID);
                    String subFolderID = folder.getId();
                    uploadFilesInFolder(subFolderID, mDriveServiceHelper, subFilePaths);
                }
            }
        }
    }

    private void deleteDecDir(File file, File origin) {
        if (file.exists() && file.isDirectory()) {
            File[] namesEnc = origin.listFiles();
            ArrayList<String> encNames = new ArrayList<>();
            if (namesEnc != null) {
                for (int i = 0; i < namesEnc.length; i++) {
                    if (namesEnc[i].getName().length() > 4 && namesEnc[i].isFile()) {
                        encNames.add(namesEnc[i].getName().substring(0, namesEnc[i].getName().length() - 4));
                    } else if (namesEnc[i].getName().length() > 3 && namesEnc[i].isDirectory()) {
                        encNames.add(namesEnc[i].getName().substring(0, namesEnc[i].getName().length() - 3) + "Dir");
                    }
                }
                File[] currentNames = file.listFiles();
                for (int i = 0; i < currentNames.length; i++) {
                    String filePath = currentNames[i].getPath();
                    File pre = new File(filePath);
                    if (pre.exists() && pre.isFile() && encNames.contains(pre.getName()))
                        pre.delete();
                    else if (pre.exists() && pre.isDirectory() && encNames.contains(pre.getName() + "Dir"))
                        deleteDecDir(pre, new File(origin.getPath() + File.separator + pre.getName() + "Enc"));
                }
            }
        }
    }

    private void deleteEncDir(File file) {
        File[] currentNames = file.listFiles();
        for (int i = 0; i < currentNames.length; i++) {
            String filePath = currentNames[i].getPath();
            File pre = new File(filePath);
            if (pre.exists() && pre.isFile() && pre.getName().substring(pre.getName().length() - 4).matches(".enc"))
                pre.delete();
            else if (pre.exists() && pre.isDirectory()) deleteEncDir(pre);
        }
    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) files[i].delete();
                else deleteFolder(files[i]);
            }
        }
        folder.delete();
    }

    private ArrayList<File> gDriveDownloadFiles(DriveServiceHelper mDriveServiceHelper, ArrayList<String> paths, ArrayList<String> names, String dirPath, ArrayList<String> mime) throws Exception {
        ArrayList<File> downloadedFiles = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            if (!mime.get(i).equals("application/vnd.google-apps.folder")) {
                File toDownload = new File(dirPath + File.separator + names.get(i));
                if (toDownload.exists()) toDownload.delete();
                mDriveServiceHelper.downloadFile(toDownload, paths.get(i));
                downloadedFiles.add(toDownload);
            } else {
                File folderToDownload = new File(dirPath + File.separator + names.get(i));
                folderToDownload.mkdirs();
                List<com.google.api.services.drive.model.File> filesInFolder = mDriveServiceHelper.listDriveFiles(paths.get(i));
                ArrayList<String> paths2 = new ArrayList<>();
                ArrayList<String> names2 = new ArrayList<>();
                ArrayList<String> mime2 = new ArrayList<>();
                if (filesInFolder != null && !filesInFolder.isEmpty()) {
                    for (int j = 0; j < filesInFolder.size(); j++) {
                        paths2.add(filesInFolder.get(j).getId());
                        names2.add(filesInFolder.get(j).getName());
                        mime2.add(filesInFolder.get(j).getMimeType());
                    }
                    gDriveDownloadFiles(mDriveServiceHelper, paths2, names2, dirPath + File.separator + names.get(i),mime2);
                    downloadedFiles.add(folderToDownload);
                }
            }
        }
        return downloadedFiles;
    }

    private ArrayList<String> constructFilePaths(ArrayList<String> paths) {
        ArrayList<String> pathsWithFolders = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            if (new File(paths.get(i)).isDirectory()) {
                File[] files = new File(paths.get(i)).listFiles();
                ArrayList<String> subPaths = new ArrayList<>();
                if (files != null) {
                    for (int j = 0; j < files.length; j++) {
                        subPaths.add(files[j].getPath());
                    }
                }
                pathsWithFolders.addAll(constructFilePaths(subPaths));
            } else {
                pathsWithFolders.add(paths.get(i));
            }
        }
        return pathsWithFolders;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
