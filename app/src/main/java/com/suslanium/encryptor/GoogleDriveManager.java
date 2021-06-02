package com.suslanium.encryptor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.suslanium.encryptor.ui.explorer.ExplorerFragment;

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
    public ArrayList<String> folders = new ArrayList<>();
    public ArrayList<String> ids = new ArrayList<>();
    private View sView;
    public int currentOperationNumber = 0;
    private RecyclerView recyclerView;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton newFolder;
    private FloatingActionButton upload;
    private TextView pathView;
    private TextView sizeView;
    private SwipeRefreshLayout layout;

    @Override
    protected void onStart() {
        super.onStart();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.menu_keyexchange);
        Drawable drawable;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            drawable = ContextCompat.getDrawable(this, R.drawable.backarrow);
        } else {
            drawable = getResources().getDrawable(R.drawable.backarrow);
        }
        actionBar.setHomeAsUpIndicator(drawable);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dark_theme = preferences.getBoolean("dark_Theme", false);
        if (dark_theme) setTheme(R.style.Theme_Encryptor_Dark_ActionBar);
        else setTheme(R.style.Theme_Encryptor_Light_ActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_drive_manager);
        sView = findViewById(R.id.gDriveBottomView);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void checkFileBar() {
        ArrayList<String> checkedIds = adapter.getCheckedIds();
        if (checkedIds != null && checkedIds.size() > 0 && bottomNavigationView.getVisibility() == View.GONE) {
            adapter.setCanSelect(false);
            bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
            for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
                bottomNavigationView.getMenu().getItem(i).setChecked(false);
            }
            bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
            ExplorerFragment.fadeOut(bottomNavigationView);
            ExplorerFragment.fadeIn(newFolder);
            ExplorerFragment.fadeIn(upload);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    adapter.setCanSelect(true);
                }
            }, 200);
        } else if (bottomNavigationView.getVisibility() == View.VISIBLE && checkedIds.isEmpty()) {
            adapter.setCanSelect(false);
            ExplorerFragment.fadeIn(bottomNavigationView);
            ExplorerFragment.fadeOut(newFolder);
            ExplorerFragment.fadeOut(upload);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    adapter.setCanSelect(true);
                }
            }, 200);
        }
    }

    public void updateUI(boolean search) {
        try {
            if (currentOperationNumber == 0) {
                currentOperationNumber++;
                List<File> files = mDriveServiceHelper.listDriveFiles(currentFolderID);
                ArrayList<String>[] names = new ArrayList[]{null, null, null};
                names[0] = new ArrayList<>();
                names[1] = new ArrayList<>();
                names[2] = new ArrayList<>();
                if (files != null) {
                    if (!files.isEmpty()) {
                        for (File file1 : files) {
                            names[0].add(file1.getName());
                            names[1].add(file1.getId());
                            names[2].add(file1.getMimeType());
                        }
                        runOnUiThread(() -> recyclerView.stopScroll());
                        adapter.setNewData(names[0], names[1], names[2]);
                    }
                } else {
                    runOnUiThread(() -> recyclerView.stopScroll());
                    adapter.setNewData(names[0], names[1], names[2]);
                }
                currentOperationNumber--;
            }
            if (search) {
                runOnUiThread(() -> layout.setRefreshing(false));
            }
        } catch (Exception e) {
            currentOperationNumber--;
            if (search) {
                runOnUiThread(() -> layout.setRefreshing(false));
            }

        }
    }


    @Override
    public void onBackPressed() {
        if (lists.size() > 1) {
            if (currentOperationNumber == 0) {
                folders.remove(folders.get(folders.size() - 1));
                constructAndSetPath();
                currentOperationNumber++;
                recyclerView.stopScroll();
                Animation fadeIn = AnimationUtils.loadAnimation(GoogleDriveManager.this, android.R.anim.slide_out_right);
                fadeIn.setDuration(200);
                fadeIn.setFillAfter(true);
                recyclerView.startAnimation(fadeIn);
                ArrayList<String>[] list = lists.get(lists.size() - 1);
                currentFolderID = ids.get(ids.size() - 1);
                ids.remove(ids.size() - 1);
                Thread thread = new Thread(() -> {
                    while (!fadeIn.hasEnded()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {

                            Thread.currentThread().interrupt();
                        }
                    }
                    if (list != null) {
                        adapter.setNewData(list[0], list[1], list[2]);
                    }
                    lists.remove(lists.size() - 1);
                    Animation fadeOut = AnimationUtils.loadAnimation(GoogleDriveManager.this, android.R.anim.slide_in_left);
                    fadeOut.setDuration(200);
                    runOnUiThread(() -> recyclerView.startAnimation(fadeOut));
                    runOnUiThread(this::checkFileBar);
                    while (!fadeOut.hasEnded()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {

                            Thread.currentThread().interrupt();
                        }
                    }
                    currentOperationNumber--;
                });
                thread.start();
            }
        } else {
            Intent intent = new Intent(GoogleDriveManager.this, Explorer.class);
            intent.putExtra("pass", getIntent().getByteArrayExtra("pass"));
            startActivity(intent);
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
        bottomNavigationView = findViewById(R.id.gDriveBottomView);
        folders.add(getString(R.string.rootFolder));
        pathView = findViewById(R.id.storagePathG);
        sizeView = findViewById(R.id.freeSpaceG);
        layout = findViewById(R.id.swipeGDrive);
        layout.setColorSchemeColors(Color.parseColor("#171E21"));
        layout.setProgressBackgroundColorSchemeColor(Color.parseColor("#90A4AE"));
        layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        updateUI(true);
                    }
                });
                thread.start();
            }
        });
        View.OnClickListener deleteListener = v -> {
            ArrayList<String> ids2 = adapter.getCheckedIds();
            if (ids2 != null && ids2.size() > 0) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(GoogleDriveManager.this, R.style.MaterialAlertDialog_rounded)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.goingToDelete)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
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
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
            } else {
                Snackbar.make(v, R.string.selectFiles, Snackbar.LENGTH_LONG).show();
            }
        };
        View.OnClickListener downloadListener = v -> {
            ArrayList<String> checkedIds = adapter.getCheckedIds();
            ArrayList<String> checkedNames = adapter.getCheckedNames();
            ArrayList<String> mimes = adapter.getCheckedMimes();
            if (checkedIds != null && !checkedIds.isEmpty()) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(GoogleDriveManager.this, R.style.MaterialAlertDialog_rounded)
                        .setTitle(R.string.confirmAction)
                        .setMessage(R.string.downloadDecryptFiles)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            Intent intent = new Intent(GoogleDriveManager.this, EncryptorService.class);
                            intent.putExtra("actionType", "gDriveD");
                            EncryptorService.uniqueID++;
                            int i = EncryptorService.uniqueID;
                            EncryptorService.paths.put(i, checkedIds);
                            EncryptorService.names.put(i, checkedNames);
                            EncryptorService.mimeTypes.put(i, mimes);
                            intent.putExtra("index", i);
                            intent.putExtra("pass", getIntent().getByteArrayExtra("pass"));
                            ContextCompat.startForegroundService(GoogleDriveManager.this, intent);
                        })
                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                builder.show();
            } else {
                Snackbar.make(v, R.string.selectFiles, Snackbar.LENGTH_LONG).show();
            }
        };
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_decryptFilesG:
                        downloadListener.onClick(bottomNavigationView);
                        adapter.deselectAll();
                        checkFileBar();
                        break;
                    case R.id.action_deleteFilesG:
                        deleteListener.onClick(bottomNavigationView);
                        adapter.deselectAll();
                        checkFileBar();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        newFolder = findViewById(R.id.gDriveNewFolder);
        newFolder.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(GoogleDriveManager.this, R.style.MaterialAlertDialog_rounded)
                    .setTitle(R.string.createFolder)
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            final EditText input = new EditText(GoogleDriveManager.this);
            Typeface ubuntu = ResourcesCompat.getFont(GoogleDriveManager.this, R.font.ubuntu);
            input.setTypeface(ubuntu);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            builder.setPositiveButton(R.string.create, (dialog, which) -> {
                String name = input.getText().toString();
                if (!name.equals("") && !name.contains(".")) {
                    java.io.File file = new java.io.File(getCacheDir() + java.io.File.separator + name);
                    try {
                        file.createNewFile();
                        file.delete();
                        Thread thread = new Thread(() -> {
                            try {
                                mDriveServiceHelper.createFolder(name, currentFolderID);
                                updateUI(false);
                            } catch (Exception e) {
                                runOnUiThread(() -> Snackbar.make(v, R.string.failedToCreateG, Snackbar.LENGTH_LONG).show());
                            }
                        });
                        thread.start();
                    } catch (Exception e) {
                        Snackbar.make(v, R.string.enterValidNameErr, Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    Snackbar.make(v, R.string.enterValidNameErr, Snackbar.LENGTH_LONG).show();
                }
            });
            builder.show();
        });
        upload = findViewById(R.id.gDriveUpload);
        upload.setOnClickListener(v -> {
            Intent intent = new Intent(GoogleDriveManager.this, GoogleDriveUploadSelector.class);
            intent.putExtra("pass", getIntent().getByteArrayExtra("pass"));
            intent.putExtra("gDriveFolder", currentFolderID);
            startActivity(intent);
        });
        showRootFilesInDrive();
    }

    public void showRootFilesInDrive() {
        final ArrayList<String>[] names = new ArrayList[]{null, null, null};
        Thread thread = new Thread(() -> {
            try {
                long total = 0;
                long used = 0;
                try {
                    About about = googleDriveService.about().get().setFields("storageQuota").execute();
                    if (about.getStorageQuota().getLimit() != null) {
                        total = about.getStorageQuota().getLimit();
                    }
                    if (about.getStorageQuota().getUsage() != null) {
                        used = about.getStorageQuota().getUsage();
                    }
                } catch (Exception e) {

                }
                long free = 0;
                if (total != 0 && used != 0) {
                    free = total - used;
                    double freeSpace = free;
                    int spaceDivisonCount = 0;
                    while (freeSpace > 1024) {
                        freeSpace = freeSpace / 1024;
                        spaceDivisonCount++;
                    }
                    freeSpace = (double) Math.round(freeSpace * 100) / 100;
                    int finalSpaceDivisonCount = spaceDivisonCount;
                    double finalFreeSpace = freeSpace;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            switch (finalSpaceDivisonCount) {
                                case 0:
                                    sizeView.setText(finalFreeSpace + " " + getString(R.string.bytes));
                                    break;
                                case 1:
                                    sizeView.setText(finalFreeSpace + " " + getString(R.string.kbytes));
                                    break;
                                case 2:
                                    sizeView.setText(finalFreeSpace + " " + getString(R.string.mbytes));
                                    break;
                                case 3:
                                    sizeView.setText(finalFreeSpace + " " + getString(R.string.gbytes));
                                    break;
                                case 4:
                                    sizeView.setText(finalFreeSpace + " " + getString(R.string.tbytes));
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sizeView.setText("");
                        }
                    });
                }
                List<File> fileList = mDriveServiceHelper.listDriveFiles(null);
                if (fileList != null) {
                    names[0] = new ArrayList<>();
                    names[1] = new ArrayList<>();
                    names[2] = new ArrayList<>();
                    if (!fileList.isEmpty()) {
                        for (File file : fileList) {
                            names[0].add(file.getName());
                            names[1].add(file.getId());
                            names[2].add(file.getMimeType());
                        }
                    }
                    lists.put(lists.size(), new ArrayList[]{names[0], names[1]});
                    ids.add(null);
                    runOnUiThread(() -> {
                        recyclerView = findViewById(R.id.gDriveFileList);
                        recyclerView.setLayoutManager(new LinearLayoutManager(GoogleDriveManager.this));
                        adapter = new GoogleDriveAdapter(names[0], mDriveServiceHelper, names[1], GoogleDriveManager.this, recyclerView, names[2]);
                        recyclerView.setAdapter(adapter);
                        constructAndSetPath();
                    });
                }
            } catch (Exception e) {

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

    public void constructAndSetPath() {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < folders.size(); i++) {
            path.append(java.io.File.separator).append(folders.get(i));
        }
        String pathStr = new String(path);
        String toSet = ExplorerFragment.fitString(pathView, pathStr);
        pathView.setText(toSet);
    }
}