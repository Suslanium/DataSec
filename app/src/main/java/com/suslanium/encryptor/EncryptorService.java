package com.suslanium.encryptor;

import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.microsoft.onedrivesdk.saver.ISaver;
import com.microsoft.onedrivesdk.saver.Saver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EncryptorService extends Service {
    private final String CHANNEL_ID = "Encryptor";
    private int id = 0;
    private ArrayList<Boolean> isRunning = new ArrayList<>();
    private ISaver mSaver;
    private String ONEDRIVE_APP_ID = "4a85af0e-df80-4f4f-a172-625d168df915";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Encrypting/decrypting";
            String description = "Show notifications when encrypting/decrypting files";
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
                .setContentTitle("Encrypting service is running")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        startForeground(++id, builder.build());
        String actionType = intent.getStringExtra("actionType");
        ArrayList<String> paths = intent.getStringArrayListExtra("paths");
        ArrayList<String> names = intent.getStringArrayListExtra("names");
        byte[] pass = intent.getByteArrayExtra("pass");
        String currentFolderID = intent.getStringExtra("gDriveFolder");
        Boolean running = true;
        isRunning.add(running);
        int operationID = ++id;
        if (actionType.equals("E")) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String password = Encryptor.RSADecrypt(pass);
                        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("Encrypting...")
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager1.notify(operationID, builder1.build());
                        final int[] errorsCount = {0};
                        for (String path : paths) {
                            File file = new File(path);
                            if (file.isDirectory()) {
                                File encdir = new File(path + "Enc");
                                if (encdir.exists()) {
                                    File[] names = encdir.listFiles();
                                    for (int i = 0; i < names.length; i++) {
                                        String filePath = names[i].getPath();
                                        File pre = new File(filePath);
                                        if (pre.exists() && pre.isFile() && pre.getName().substring(pre.getName().length() - 4).matches(".enc"))
                                            pre.delete();
                                        else if (pre.exists() && pre.isDirectory())
                                            deleteEncDir(pre);
                                    }
                                }
                                try {
                                    Encryptor.encryptFolderAES_GCM(file, password, new File(path + "Enc"));
                                } catch (Exception e) {
                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                            .setSmallIcon(R.drawable.locked)
                                            .setContentTitle("Something went wrong while encrypting " + file.getName())
                                            .setOngoing(true)
                                            .setProgress(1, 0, true)
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                    notificationManager.notify(operationID, builder.build());
                                    errorsCount[0]++;
                                }
                            } else {
                                new File(path + ".enc").delete();
                                try {
                                    Encryptor.encryptFileAES256(file, password, new File(path + ".enc"));
                                } catch (Exception e) {
                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                            .setSmallIcon(R.drawable.locked)
                                            .setContentTitle("Something went wrong while encrypting " + file.getName())
                                            .setOngoing(true)
                                            .setProgress(1, 0, true)
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                    notificationManager.notify(operationID, builder.build());
                                    errorsCount[0]++;
                                }
                            }
                        }
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("File(s) have been successfully encrypted!")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        if (errorsCount[0] > 0) builder.setContentText("Errors: " + errorsCount[0]);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager.notify(operationID, builder.build());
                        isRunning.remove(running);
                        if (!isRunning.contains(true)) stopSelf();
                    } catch (Exception e) {

                    }
                }
            });
            thread.start();
        } else if (actionType.equals("D")) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String password = Encryptor.RSADecrypt(pass);
                        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("Decrypting...")
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager1.notify(operationID, builder1.build());
                        final int[] errorsCount = {0};
                        for (String path : paths) {
                            File file = new File(path);
                            if (file.isDirectory()) {
                                File file1 = new File((path).substring(0, (path).length() - 3));
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
                                        File[] names = file1.listFiles();
                                        for (int i = 0; i < names.length; i++) {
                                            String filePath = names[i].getPath();
                                            File pre = new File(filePath);
                                            if (pre.exists() && pre.isFile() && encNames.contains(pre.getName()))
                                                pre.delete();
                                            else if (pre.exists() && pre.isDirectory() && encNames.contains(pre.getName() + "Dir"))
                                                deleteDecDir(pre, new File(path + File.separator + pre.getName() + "Enc"));
                                        }
                                    }
                                }
                                try {
                                    Encryptor.decryptFolderAES_GCM(file, password, new File((path).substring(0, (path).length() - 3)));
                                } catch (Exception e) {
                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                            .setSmallIcon(R.drawable.locked)
                                            .setContentTitle("Something went wrong while decrypting " + file.getName())
                                            .setOngoing(true)
                                            .setProgress(1, 0, true)
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                    notificationManager.notify(operationID, builder.build());
                                    errorsCount[0]++;
                                }
                            } else {
                                File file1 = new File(path.substring(0, (path).length() - 4));
                                file1.delete();
                                try {
                                    Encryptor.decryptFileAES256(file, password, file1);
                                } catch (Exception e) {
                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                            .setSmallIcon(R.drawable.locked)
                                            .setContentTitle("Something went wrong while decrypting " + file.getName())
                                            .setOngoing(true)
                                            .setProgress(1, 0, true)
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                    notificationManager.notify(operationID, builder.build());
                                    errorsCount[0]++;
                                }
                            }
                        }
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("File(s) have been successfully decrypted!")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        if (errorsCount[0] > 0) builder.setContentText("Errors: " + errorsCount[0]);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager.notify(operationID, builder.build());
                        isRunning.remove(running);
                        if (!isRunning.contains(true)) stopSelf();
                    } catch (Exception e) {

                    }
                }
            });
            thread.start();
        } else if (actionType.equals("E1")) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String password = Encryptor.RSADecrypt(pass);
                        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("Encrypting...")
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager1.notify(operationID, builder1.build());
                        File enc = new File(paths.get(0) + ".enc");
                        enc.delete();
                        try {
                            Encryptor.encryptFileAES256(new File(paths.get(0)), password, enc);
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle("File have been successfully encrypted!")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager.notify(operationID, builder.build());
                            isRunning.remove(running);
                            if (!isRunning.contains(true)) stopSelf();
                        } catch (Exception e) {
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle("Something went wrong...")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager.notify(operationID, builder.build());
                            isRunning.remove(running);
                            if (!isRunning.contains(true)) stopSelf();
                        }
                    } catch (Exception e) {

                    }
                }
            });
            thread.start();
        } else if (actionType.equals("D1")) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String password = Encryptor.RSADecrypt(pass);
                        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("Decrypting...")
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager1.notify(operationID, builder1.build());
                        File enc = new File((paths.get(0)).substring(0, (paths.get(0)).length() - 4));
                        enc.delete();
                        try {
                            Encryptor.decryptFileAES256(new File(paths.get(0)), password, enc);
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle("File have been successfully decrypted!")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager.notify(operationID, builder.build());
                            isRunning.remove(running);
                            if (!isRunning.contains(true)) stopSelf();
                        } catch (Exception e) {
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle("Something went wrong...")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager.notify(operationID, builder.build());
                            isRunning.remove(running);
                            if (!isRunning.contains(true)) stopSelf();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        } else if (actionType.equals("D2")) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String password = Encryptor.RSADecrypt(pass);
                        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("Decrypting...")
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager1.notify(operationID, builder1.build());
                        File enc = new File((paths.get(0)).substring(0, (paths.get(0)).length() - 4));
                        enc.delete();
                        try {
                            Encryptor.decryptFileAES256(new File(paths.get(0)), password, enc);
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle("File have been successfully decrypted!")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager.notify(operationID, builder.build());
                            //new File(paths.get(0)).delete();
                            isRunning.remove(running);
                            if (!isRunning.contains(true)) stopSelf();
                        } catch (Exception e) {
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle("Something went wrong...")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager.notify(operationID, builder.build());
                            //new File(paths.get(0)).delete();
                            isRunning.remove(running);
                            if (!isRunning.contains(true)) stopSelf();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        } else if (actionType.equals("E2")) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String password = Encryptor.RSADecrypt(pass);
                        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("Encrypting...")
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager1.notify(operationID, builder1.build());
                        File enc = new File(paths.get(0) + ".enc");
                        enc.delete();
                        try {
                            Encryptor.encryptFileAES256(new File(paths.get(0)), password, enc);
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle("File have been successfully encrypted!")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager.notify(operationID, builder.build());
                            Intent dialogIntent = new Intent(getBaseContext(), oneDriveActivity.class);
                            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            dialogIntent.putExtra("path", enc.getPath());
                            dialogIntent.putExtra("pass", pass);
                            startActivity(dialogIntent);
                            isRunning.remove(running);
                            if (!isRunning.contains(true)) stopSelf();
                        } catch (Exception e) {
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle("Something went wrong...")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            Log.d("Exception", e.getMessage());
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager.notify(operationID, builder.build());
                            isRunning.remove(running);
                            if (!isRunning.contains(true)) stopSelf();
                        }
                    } catch (Exception e) {

                    }
                }
            });
            thread.start();
        } else if (actionType.equals("gDriveE")) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String password = Encryptor.RSADecrypt(pass);
                        ArrayList<File> encryptedFiles = new ArrayList<>();
                        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("Encrypting...")
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager1.notify(operationID, builder1.build());
                        final int[] errorsCount = {0};
                        for (String path : paths) {
                            File file = new File(path);
                            if (file.isDirectory()) {
                                File encdir = new File(getFilesDir() + File.separator + file.getName() + "Enc");
                                if (encdir.exists()) {
                                    File[] names = encdir.listFiles();
                                    for (int i = 0; i < names.length; i++) {
                                        String filePath = names[i].getPath();
                                        File pre = new File(filePath);
                                        if (pre.exists() && pre.isFile() && pre.getName().substring(pre.getName().length() - 4).matches(".enc"))
                                            pre.delete();
                                        else if (pre.exists() && pre.isDirectory())
                                            deleteEncDir(pre);
                                    }
                                }
                                try {
                                    Encryptor.encryptFolderAES_GCM(file, password, new File(getFilesDir() + File.separator + file.getName() + "Enc"));
                                    encryptedFiles.add(new File(getFilesDir() + File.separator + file.getName() + "Enc"));
                                } catch (Exception e) {
                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                            .setSmallIcon(R.drawable.locked)
                                            .setContentTitle("Something went wrong while encrypting " + file.getName())
                                            .setOngoing(true)
                                            .setProgress(1, 0, true)
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                    notificationManager.notify(operationID, builder.build());
                                    errorsCount[0]++;
                                }
                            } else {
                                new File(getFilesDir() + File.separator + file.getName() + ".enc").delete();
                                try {
                                    Encryptor.encryptFileAES256(file, password, new File(getFilesDir() + File.separator + file.getName() + ".enc"));
                                    encryptedFiles.add(new File(getFilesDir() + File.separator + file.getName() + ".enc"));
                                } catch (Exception e) {
                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                            .setSmallIcon(R.drawable.locked)
                                            .setContentTitle("Something went wrong while encrypting " + file.getName())
                                            .setOngoing(true)
                                            .setProgress(1, 0, true)
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                    notificationManager.notify(operationID, builder.build());
                                    errorsCount[0]++;
                                }
                            }
                        }
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("Uploading files to Google Drive...")
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        if (errorsCount[0] > 0) builder.setContentText("Errors: " + errorsCount[0]);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager.notify(operationID, builder.build());
                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .build();
                        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getApplicationContext(), gso);
                        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                        if (acct != null) {
                            String personName = acct.getDisplayName();
                            String personEmail = acct.getEmail();
                            String personId = acct.getId();
                            Uri personPhoto = acct.getPhotoUrl();
                            GoogleSignInAccount mAccount = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Collections.singleton(Scopes.DRIVE_APPFOLDER));
                            credential.setSelectedAccount(mAccount.getAccount());
                            Drive googleDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName("Encryptor").build();
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
                                            if (driveFiles.get(j).getName().equals(encryptedFiles.get(i).getName())) {
                                                String folderID = driveFiles.get(j).getId();
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
                                .setContentTitle("Encrypted files have been successfully uploaded to GDrive!")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        if (errorsCount[0] > 0)
                            builder3.setContentText("Errors: " + errorsCount[0]);
                        NotificationManagerCompat notificationManager3 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager3.notify(operationID, builder3.build());
                        isRunning.remove(running);
                        if (!isRunning.contains(true)) stopSelf();
                    } catch (Exception e) {
                        e.printStackTrace();
                        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("Something went wrong while uploading...")
                                .setContentText(e.getMessage())
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager2 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager2.notify(operationID, builder2.build());
                        isRunning.remove(running);
                        if (!isRunning.contains(true)) stopSelf();
                    }
                }
            });
            thread.start();
        } else if (actionType.equals("gDriveD")) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .build();
                        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getApplicationContext(), gso);
                        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                        if (acct != null) {
                            NotificationCompat.Builder builder2 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle("Downloading files from G Drive...")
                                    .setOngoing(true)
                                    .setProgress(1, 0, true)
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManagerCompat notificationManager2 = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager2.notify(operationID, builder2.build());
                            String personName = acct.getDisplayName();
                            String personEmail = acct.getEmail();
                            String personId = acct.getId();
                            Uri personPhoto = acct.getPhotoUrl();
                            GoogleSignInAccount mAccount = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Collections.singleton(Scopes.DRIVE_APPFOLDER));
                            credential.setSelectedAccount(mAccount.getAccount());
                            Drive googleDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName("Encryptor").build();
                            DriveServiceHelper mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                            new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "EncryptorDownloads").mkdirs();
                            ArrayList<File> downloadedFiles = gDriveDownloadFiles(mDriveServiceHelper, paths, names, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "EncryptorDownloads");
                            String password = Encryptor.RSADecrypt(pass);
                            NotificationCompat.Builder builder1 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle("Decrypting...")
                                    .setOngoing(true)
                                    .setProgress(1, 0, true)
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager1.notify(operationID, builder1.build());
                            final int[] errorsCount = {0};
                            ArrayList<String> downloadedPaths = new ArrayList<>();
                            if (downloadedFiles.size() > 0) {
                                for (int i = 0; i < downloadedFiles.size(); i++) {
                                    downloadedPaths.add(downloadedFiles.get(i).getPath());
                                }
                                for (String path : downloadedPaths) {
                                    File file = new File(path);
                                    if (file.isDirectory()) {
                                        File file1 = new File((path).substring(0, (path).length() - 3));
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
                                                File[] names = file1.listFiles();
                                                for (int i = 0; i < names.length; i++) {
                                                    String filePath = names[i].getPath();
                                                    File pre = new File(filePath);
                                                    if (pre.exists() && pre.isFile() && encNames.contains(pre.getName()))
                                                        pre.delete();
                                                    else if (pre.exists() && pre.isDirectory() && encNames.contains(pre.getName() + "Dir"))
                                                        deleteDecDir(pre, new File(path + File.separator + pre.getName() + "Enc"));
                                                }
                                            }
                                        }
                                        try {
                                            Encryptor.decryptFolderAES_GCM(file, password, new File((path).substring(0, (path).length() - 3)));
                                        } catch (Exception e) {
                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                                    .setSmallIcon(R.drawable.locked)
                                                    .setContentTitle("Something went wrong while decrypting " + file.getName())
                                                    .setOngoing(true)
                                                    .setProgress(1, 0, true)
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                            notificationManager.notify(operationID, builder.build());
                                            errorsCount[0]++;
                                        }
                                    } else {
                                        File file1 = new File(path.substring(0, (path).length() - 4));
                                        file1.delete();
                                        try {
                                            Encryptor.decryptFileAES256(file, password, file1);
                                        } catch (Exception e) {
                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                                    .setSmallIcon(R.drawable.locked)
                                                    .setContentTitle("Something went wrong while decrypting " + file.getName())
                                                    .setOngoing(true)
                                                    .setProgress(1, 0, true)
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                                            notificationManager.notify(operationID, builder.build());
                                            errorsCount[0]++;
                                        }
                                    }
                                }
                            }
                            for (int i = 0; i < downloadedFiles.size(); i++) {
                                if (downloadedFiles.get(i).isFile())
                                    downloadedFiles.get(i).delete();
                                else deleteFolder(downloadedFiles.get(i));
                            }
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.locked)
                                    .setContentTitle("File(s) have been successfully downloaded and decrypted!")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            if (errorsCount[0] > 0)
                                builder.setContentText("Errors: " + errorsCount[0]);
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                            notificationManager.notify(operationID, builder.build());
                            isRunning.remove(running);
                            if (!isRunning.contains(true)) stopSelf();
                        }
                    } catch (Exception e) {
                        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("Something went wrong...")
                                .setOngoing(true)
                                .setProgress(1, 0, true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager2 = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager2.notify(operationID, builder2.build());
                    }
                }
            });
            thread.start();
        } else if (actionType.equals("gDriveDelete")) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        NotificationCompat.Builder builder3 = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("Deleting file(s) from Google drive...")
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
                            String personName = acct.getDisplayName();
                            String personEmail = acct.getEmail();
                            String personId = acct.getId();
                            Uri personPhoto = acct.getPhotoUrl();
                            GoogleSignInAccount mAccount = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Collections.singleton(Scopes.DRIVE_APPFOLDER));
                            credential.setSelectedAccount(mAccount.getAccount());
                            Drive googleDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName("Encryptor").build();
                            DriveServiceHelper mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                            for (int i = 0; i < paths.size(); i++) {
                                mDriveServiceHelper.deleteFolderFile(paths.get(i));
                            }
                        }
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("File(s) have been successfully deleted!")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager.notify(operationID, builder.build());
                        isRunning.remove(running);
                        if (!isRunning.contains(true)) stopSelf();
                    } catch (Exception e) {
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(EncryptorService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.locked)
                                .setContentTitle("Something went wrong while deleting file(s)")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(EncryptorService.this);
                        notificationManager.notify(operationID, builder.build());
                        isRunning.remove(running);
                        if (!isRunning.contains(true)) stopSelf();
                    }
                }
            });
            thread.start();
        }
        //code
        return START_REDELIVER_INTENT;
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
        if (filePaths != null && filePaths.size() > 0) {
            for (int i = 0; i < filePaths.size(); i++) {
                encryptedFiles.add(new File(filePaths.get(i)));
            }
        }
        for (int i = 0; i < encryptedFiles.size(); i++) {
            if (encryptedFiles.get(i).isFile()) {
                if (driveFileNames.contains(encryptedFiles.get(i).getName())) {
                    for (int j = 0; j < driveFiles.size(); j++) {
                        if (driveFiles.get(j).getName().equals(encryptedFiles.get(i).getName())) {
                            mDriveServiceHelper.deleteFolderFile(driveFiles.get(j).getId());
                            mDriveServiceHelper.uploadFile(encryptedFiles.get(i), folderID);
                            break;
                        }
                    }
                } else {
                    mDriveServiceHelper.uploadFile(encryptedFiles.get(i), folderID);
                }
            } else {
                File[] files = encryptedFiles.get(i).listFiles();
                ArrayList<String> subFilePaths = new ArrayList<>();
                if (files != null && files.length > 0) {
                    for (int u = 0; u < files.length; u++) {
                        filePaths.add(files[u].getPath());
                    }
                }
                if (driveFileNames.contains(encryptedFiles.get(i).getName())) {
                    for (int j = 0; j < driveFiles.size(); j++) {
                        if (driveFiles.get(j).getName().equals(encryptedFiles.get(i).getName())) {
                            String subFolderID = driveFiles.get(j).getId();
                            uploadFilesInFolder(subFolderID, mDriveServiceHelper, subFilePaths);
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
        //File[] names = file.listFiles();
        File[] namesOrigin = origin.listFiles();
        //File file1 = new File((path).substring(0, (path).length() - 3));
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
                File[] names = file.listFiles();
                for (int i = 0; i < names.length; i++) {
                    String filePath = names[i].getPath();
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
        File[] names = file.listFiles();
        for (int i = 0; i < names.length; i++) {
            String filePath = names[i].getPath();
            File pre = new File(filePath);
            if (pre.exists() && pre.isFile() && pre.getName().substring(pre.getName().length() - 4).matches(".enc"))
                pre.delete();
            else if (pre.exists() && pre.isDirectory()) deleteEncDir(pre);
        }
    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) files[i].delete();
            else deleteFolder(files[i]);
        }
        folder.delete();
    }

    private ArrayList<File> gDriveDownloadFiles(DriveServiceHelper mDriveServiceHelper, ArrayList<String> paths, ArrayList<String> names, String dirPath) throws Exception {
        ArrayList<File> downloadedFiles = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            if (names.get(i).contains(".")) {
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
                if (filesInFolder != null && filesInFolder.size() > 0) {
                    for (int j = 0; j < filesInFolder.size(); j++) {
                        paths2.add(filesInFolder.get(j).getId());
                        names2.add(filesInFolder.get(j).getName());
                    }
                    gDriveDownloadFiles(mDriveServiceHelper, paths2, names2, dirPath + File.separator + names.get(i));
                    downloadedFiles.add(folderToDownload);
                }
            }
        }
        return downloadedFiles;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
