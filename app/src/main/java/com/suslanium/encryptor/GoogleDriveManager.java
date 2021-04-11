package com.suslanium.encryptor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class GoogleDriveManager extends AppCompatActivity {
    Scope ACCESS_DRIVE_SCOPE = new Scope(Scopes.DRIVE_FILE);
    Scope SCOPE_EMAIL = new Scope(Scopes.EMAIL);
    Scope SCOPE_APP = new Scope(Scopes.DRIVE_APPFOLDER);
    Drive googleDriveService = null;
    DriveServiceHelper mDriveServiceHelper = null;
    GoogleSignInClient mGoogleSignInClient;
    GoogleDriveAdapter adapter = null;
    int RC_AUTHORIZE_DRIVE = 1;
    private String currentFolderID = null;
    public HashMap<Integer, ArrayList<String>[]> lists = new HashMap<>();
    public ArrayList<String> ids = new ArrayList<>();
    private View sView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_drive_manager);
        sView = findViewById(R.id.gDriveUp);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            String personName = acct.getDisplayName();
            String personEmail = acct.getEmail();
            String personId = acct.getId();
            Uri personPhoto = acct.getPhotoUrl();
            checkForGooglePermissions();
        }
    }

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // ...
                    }
                });
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if(lists.size() > 1){
            //Log.d("a", String.valueOf(lists.size()));
            ArrayList<String>[] list = lists.get(lists.size()-1);
            currentFolderID = ids.get(ids.size() - 1);
            ids.remove(ids.size() - 1);
            adapter.setNewData(list[0],list[1]);
            lists.remove(lists.size()-1);
        } else {
            Snackbar snackbar = Snackbar.make(sView, "Sorry, this is the root", Snackbar.LENGTH_LONG);
            snackbar.setAction("Exit", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(GoogleDriveManager.this, Explorer.class);
                    intent.putExtra("pass", getIntent().getByteArrayExtra("pass"));
                    startActivity(intent);
                }
            });
            snackbar.show();
            //Log.d("a", "a");
        }
    }

    private void checkForGooglePermissions() {

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(getApplicationContext()), SCOPE_APP, SCOPE_EMAIL)) {
            GoogleSignIn.requestPermissions(GoogleDriveManager.this, RC_AUTHORIZE_DRIVE, GoogleSignIn.getLastSignedInAccount(getApplicationContext()), SCOPE_EMAIL, SCOPE_APP);
        } else {
            //Toast.makeText(this, "Permission to access Drive and Email has been granted", Toast.LENGTH_SHORT).show();
            driveSetUp();
        }

    }

    private void driveSetUp() {
        GoogleSignInAccount mAccount = GoogleSignIn.getLastSignedInAccount(GoogleDriveManager.this);
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Collections.singleton(Scopes.DRIVE_APPFOLDER));
        credential.setSelectedAccount(mAccount.getAccount());
        googleDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName("Encryptor").build();
        mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
        FloatingActionButton gDriveDelete = findViewById(R.id.gDriveDelete);
        gDriveDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                                    intent.putExtra("paths", ids2);
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
            }
        });
        FloatingActionButton gDriveUp = findViewById(R.id.gDriveUp);
        gDriveUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(lists.size() > 1){
                    //Log.d("a", String.valueOf(lists.size()));
                    ArrayList<String>[] list = lists.get(lists.size()-1);
                    currentFolderID = ids.get(ids.size() - 1);
                    ids.remove(ids.size() - 1);
                    adapter.setNewData(list[0],list[1]);
                    lists.remove(lists.size()-1);
                } else {
                    Snackbar.make(v, "Sorry, this is the root", Snackbar.LENGTH_LONG).show();
                    //Log.d("a", "a");
                }
            }
        });
        FloatingActionButton newFolder = findViewById(R.id.gDriveNewFolder);
        newFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(GoogleDriveManager.this, R.style.MaterialAlertDialog_rounded)
                        .setTitle("Set new folder name")
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                final EditText input = new EditText(GoogleDriveManager.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString();
                        if (!name.equals("") && !name.contains(".")) {
                            java.io.File file = new java.io.File(getFilesDir() + java.io.File.separator + name);
                            try {
                                file.createNewFile();
                                file.delete();
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            mDriveServiceHelper.createFolder(name, currentFolderID);
                                            List<File> files = mDriveServiceHelper.listDriveFiles(currentFolderID);
                                            ArrayList<String>[] names = new ArrayList[]{null, null};
                                            if (files != null) {
                                                if (files.size() == 0) {
                                                    Log.i("Drive", "No Files");
                                                } else {
                                                    names[0] = new ArrayList<>();
                                                    names[1] = new ArrayList<>();
                                                    for (File file : files) {
                                                        names[0].add(file.getName());
                                                        names[1].add(file.getId());
                                                        //Log.d("Drive", file.getName());
                                                    }
                                                    //lists.replace(lists.size() - 1, new ArrayList[]{names[0],names[1]});
                                                    adapter.setNewData(names[0], names[1]);
                                                }
                                            } else {
                                                names[0] = new ArrayList<>();
                                                names[1] = new ArrayList<>();
                                                adapter.setNewData(names[0], names[1]);
                                            }
                                        } catch (Exception e) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Snackbar.make(v, "Failed to create folder", Snackbar.LENGTH_LONG).show();
                                                }
                                            });
                                        }
                                    }
                                });
                                thread.start();
                            } catch (Exception e) {
                                Snackbar.make(v, "Please enter a valid folder name", Snackbar.LENGTH_LONG).show();
                            }
                        } else {
                            Snackbar.make(v, "Please enter a valid folder name", Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
                builder.show();
            }
        });
        FloatingActionButton download = findViewById(R.id.gDriveDownload);
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> checkedIds = adapter.getCheckedIds();
                if(checkedIds != null && checkedIds.size()>0){
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(GoogleDriveManager.this, R.style.MaterialAlertDialog_rounded)
                            .setTitle("Confirm action")
                            .setMessage("Do you want to download & decrypt selected file(s)?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(GoogleDriveManager.this, EncryptorService.class);
                                    intent.putExtra("actionType", "gDriveD");
                                    intent.putExtra("paths", checkedIds);
                                    intent.putExtra("names", adapter.getCheckedNames());
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
            }
        });
        FloatingActionButton uploadFiles = findViewById(R.id.gDriveUpload);
        uploadFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GoogleDriveManager.this, GoogleDriveUploadSelector.class);
                intent.putExtra("pass", getIntent().getByteArrayExtra("pass"));
                intent.putExtra("gDriveFolder", currentFolderID);
                if(currentFolderID == null){
                    Log.d("aaa", "null");
                } else Log.d("aaa", currentFolderID);
                startActivity(intent);
            }
        });
        showRootFilesInDrive();
    }

    private void createFolderInDrive(String name, String folderID) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mDriveServiceHelper.createFolder(name, folderID);
                } catch (Exception e) {
                    Log.d("GDrive", e.getMessage());
                }
            }
        });
        thread.start();
    }

    public void showRootFilesInDrive() {
        final ArrayList<String>[] names = new ArrayList[]{null, null};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<File> fileList = mDriveServiceHelper.listDriveFiles(null);
                    if (fileList != null) {
                        if (fileList.size() == 0) {
                            Log.i("Drive", "No Files");
                        } else {
                            names[0] = new ArrayList<>();
                            names[1] = new ArrayList<>();
                            for (File file : fileList) {
                                names[0].add(file.getName());
                                names[1].add(file.getId());
                                //Log.d("Drive", file.getName());
                            }
                        }
                        lists.put(lists.size(), new ArrayList[]{names[0],names[1]});
                        ids.add(null);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                RecyclerView recyclerView = findViewById(R.id.gDriveFileList);
                                recyclerView.setLayoutManager(new LinearLayoutManager(GoogleDriveManager.this));
                                adapter = new GoogleDriveAdapter(names[0], mDriveServiceHelper, names[1], GoogleDriveManager.this, recyclerView);
                                recyclerView.setAdapter(adapter);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
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