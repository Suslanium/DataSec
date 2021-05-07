package com.suslanium.encryptor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class GoogleDriveManager extends AppCompatActivity {
    Scope SCOPEEMAIL = new Scope(Scopes.EMAIL);
    Scope SCOPEAPP = new Scope(Scopes.DRIVE_APPFOLDER);
    Drive googleDriveService = null;
    DriveServiceHelper mDriveServiceHelper = null;
    GoogleSignInClient mGoogleSignInClient;
    GoogleDriveAdapter adapter = null;
    int RCAUTHORIZEDRIVE = 1;
    private String currentFolderID = null;
    public HashMap<Integer, ArrayList<String>[]> lists = new HashMap<>();
    public ArrayList<String> ids = new ArrayList<>();
    private View sView;
    public int currentOperationNumber = 0;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dark_theme = preferences.getBoolean("dark_Theme", true);
        if(dark_theme) setTheme(R.style.Theme_MaterialComponents);
        else setTheme(R.style.Theme_MaterialComponents_Light);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_drive_manager);
        sView = findViewById(R.id.gDriveUp);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            checkForGooglePermissions();
        }
    }

    @Override
    public void onBackPressed() {
        if(lists.size() > 1){
            if(currentOperationNumber == 0) {
                currentOperationNumber++;
                Animation fadeIn = AnimationUtils.loadAnimation(GoogleDriveManager.this, android.R.anim.slide_out_right);
                fadeIn.setDuration(200);
                fadeIn.setFillAfter(true);
                recyclerView.startAnimation(fadeIn);
                ArrayList<String>[] list = lists.get(lists.size() - 1);
                currentFolderID = ids.get(ids.size() - 1);
                ids.remove(ids.size() - 1);
                Thread thread = new Thread(() -> {
                    while (!fadeIn.hasEnded()){
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                        }
                    }
                    if (list != null) {
                        adapter.setNewData(list[0], list[1]);
                    }
                    lists.remove(lists.size() - 1);
                    Animation fadeOut = AnimationUtils.loadAnimation(GoogleDriveManager.this, android.R.anim.slide_in_left);
                    fadeOut.setDuration(200);
                    runOnUiThread(() -> recyclerView.startAnimation(fadeOut));
                    while (!fadeOut.hasEnded()){
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                        }
                    }
                    currentOperationNumber--;
                });
                thread.start();
            }
        } else {
            Snackbar snackbar = Snackbar.make(sView, "Sorry, this is the root", Snackbar.LENGTH_LONG);
            snackbar.setAction("Exit", v -> {
                Intent intent = new Intent(GoogleDriveManager.this, Explorer.class);
                intent.putExtra("pass", getIntent().getByteArrayExtra("pass"));
                startActivity(intent);
            });
            snackbar.show();
        }
    }

    private void checkForGooglePermissions() {

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(getApplicationContext()), SCOPEAPP, SCOPEEMAIL)) {
            GoogleSignIn.requestPermissions(GoogleDriveManager.this, RCAUTHORIZEDRIVE, GoogleSignIn.getLastSignedInAccount(getApplicationContext()), SCOPEEMAIL, SCOPEAPP);
        } else {
            driveSetUp();
        }

    }

    private void driveSetUp() {
        GoogleSignInAccount mAccount = GoogleSignIn.getLastSignedInAccount(GoogleDriveManager.this);
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Collections.singleton(Scopes.DRIVE_APPFOLDER));
        if (mAccount != null) {
            credential.setSelectedAccount(mAccount.getAccount());
        }
        googleDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName("Encryptor").build();
        mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
        FloatingActionButton gDriveDelete = findViewById(R.id.gDriveDelete);
        gDriveDelete.setOnClickListener(v -> {
            ArrayList<String> ids2 = adapter.getCheckedIds();
            if(ids2 != null && ids2.size() > 0) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(GoogleDriveManager.this, R.style.MaterialAlertDialog_rounded)
                        .setTitle("Warning!")
                        .setMessage("Are you sure you want to delete these files?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(GoogleDriveManager.this, EncryptorService.class);
                                intent.putExtra("actionType", "gDriveDelete");
                                EncryptorService.uniqueID++;
                                int i = EncryptorService.uniqueID;
                                EncryptorService.paths.put(i, ids2);
                                intent.putExtra("index", i);
                                intent.putExtra("pass", getIntent().getByteArrayExtra("pass"));
                                ContextCompat.startForegroundService(GoogleDriveManager.this, intent);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
            } else {
                Snackbar.make(v, "Please select files/folders", Snackbar.LENGTH_LONG).show();
            }
        });
        FloatingActionButton gDriveUp = findViewById(R.id.gDriveUp);
        gDriveUp.setOnClickListener(v -> {
            if(lists.size() > 1){
                if(currentOperationNumber == 0) {
                    currentOperationNumber++;
                    Animation fadeIn = AnimationUtils.loadAnimation(GoogleDriveManager.this, android.R.anim.slide_out_right);
                    fadeIn.setDuration(200);
                    fadeIn.setFillAfter(true);
                    recyclerView.startAnimation(fadeIn);
                    ArrayList<String>[] list = lists.get(lists.size() - 1);
                    currentFolderID = ids.get(ids.size() - 1);
                    ids.remove(ids.size() - 1);
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!fadeIn.hasEnded()){
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    Thread.currentThread().interrupt();
                                }
                            }
                            if (list != null) {
                                adapter.setNewData(list[0], list[1]);
                            }
                            lists.remove(lists.size() - 1);
                            Animation fadeOut = AnimationUtils.loadAnimation(GoogleDriveManager.this, android.R.anim.slide_in_left);
                            fadeOut.setDuration(200);
                            runOnUiThread(() -> recyclerView.startAnimation(fadeOut));
                            while (!fadeOut.hasEnded()){
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    Thread.currentThread().interrupt();
                                }
                            }
                            currentOperationNumber--;
                        }
                    });
                    thread.start();
                }
            } else {
                Snackbar.make(v, "Sorry, this is the root", Snackbar.LENGTH_LONG).show();
            }
        });
        FloatingActionButton newFolder = findViewById(R.id.gDriveNewFolder);
        newFolder.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(GoogleDriveManager.this, R.style.MaterialAlertDialog_rounded)
                    .setTitle("Set new folder name")
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            final EditText input = new EditText(GoogleDriveManager.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            builder.setPositiveButton("OK", (dialog, which) -> {
                String name = input.getText().toString();
                if (!name.equals("") && !name.contains(".")) {
                    java.io.File file = new java.io.File(getCacheDir() + java.io.File.separator + name);
                    try {
                        file.createNewFile();
                        file.delete();
                        Thread thread = new Thread(() -> {
                            try {
                                mDriveServiceHelper.createFolder(name, currentFolderID);
                                List<File> files = mDriveServiceHelper.listDriveFiles(currentFolderID);
                                ArrayList<String>[] names = new ArrayList[]{null, null};
                                if (files != null) {
                                    if (!files.isEmpty()) {
                                        names[0] = new ArrayList<>();
                                        names[1] = new ArrayList<>();
                                        for (File file1 : files) {
                                            names[0].add(file1.getName());
                                            names[1].add(file1.getId());
                                        }
                                        adapter.setNewData(names[0], names[1]);
                                    }
                                } else {
                                    names[0] = new ArrayList<>();
                                    names[1] = new ArrayList<>();
                                    adapter.setNewData(names[0], names[1]);
                                }
                            } catch (Exception e) {
                                runOnUiThread(() -> Snackbar.make(v, "Failed to create folder", Snackbar.LENGTH_LONG).show());
                            }
                        });
                        thread.start();
                    } catch (Exception e) {
                        Snackbar.make(v, "Please enter a valid folder name", Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    Snackbar.make(v, "Please enter a valid folder name", Snackbar.LENGTH_LONG).show();
                }
            });
            builder.show();
        });
        FloatingActionButton download = findViewById(R.id.gDriveDownload);
        download.setOnClickListener(v -> {
            ArrayList<String> checkedIds = adapter.getCheckedIds();
            if(checkedIds != null && !checkedIds.isEmpty()){
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(GoogleDriveManager.this, R.style.MaterialAlertDialog_rounded)
                        .setTitle("Confirm action")
                        .setMessage("Do you want to download & decrypt selected file(s)?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            Intent intent = new Intent(GoogleDriveManager.this, EncryptorService.class);
                            intent.putExtra("actionType", "gDriveD");
                            EncryptorService.uniqueID++;
                            int i = EncryptorService.uniqueID;
                            EncryptorService.paths.put(i, checkedIds);
                            EncryptorService.names.put(i,adapter.getCheckedNames());
                            intent.putExtra("index", i);
                            intent.putExtra("pass", getIntent().getByteArrayExtra("pass"));
                            ContextCompat.startForegroundService(GoogleDriveManager.this, intent);
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                builder.show();
            } else {
                Snackbar.make(v, "Please select files/folders", Snackbar.LENGTH_LONG).show();
            }
        });
        FloatingActionButton uploadFiles = findViewById(R.id.gDriveUpload);
        uploadFiles.setOnClickListener(v -> {
            Intent intent = new Intent(GoogleDriveManager.this, GoogleDriveUploadSelector.class);
            intent.putExtra("pass", getIntent().getByteArrayExtra("pass"));
            intent.putExtra("gDriveFolder", currentFolderID);
            if(currentFolderID == null){
                Log.d("aaa", "null");
            } else Log.d("aaa", currentFolderID);
            startActivity(intent);
        });
        showRootFilesInDrive();
    }

    public void showRootFilesInDrive() {
        final ArrayList<String>[] names = new ArrayList[]{null, null};
        Thread thread = new Thread(() -> {
            try {
                List<File> fileList = mDriveServiceHelper.listDriveFiles(null);
                if (fileList != null) {
                    if (!fileList.isEmpty()) {
                        names[0] = new ArrayList<>();
                        names[1] = new ArrayList<>();
                        for (File file : fileList) {
                            names[0].add(file.getName());
                            names[1].add(file.getId());
                        }
                    }
                    lists.put(lists.size(), new ArrayList[]{names[0],names[1]});
                    ids.add(null);
                    runOnUiThread(() -> {
                        recyclerView = findViewById(R.id.gDriveFileList);
                        recyclerView.setLayoutManager(new LinearLayoutManager(GoogleDriveManager.this));
                        adapter = new GoogleDriveAdapter(names[0], mDriveServiceHelper, names[1], GoogleDriveManager.this, recyclerView);
                        recyclerView.setAdapter(adapter);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public void setCurrentFolderID(String folderID) {
        currentFolderID = folderID;
    }

    public String getCurrentFolderID() {
        return currentFolderID;
    }
}