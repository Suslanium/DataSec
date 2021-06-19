package com.suslanium.encryptor.ui.gdrive;

import android.annotation.SuppressLint;
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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.suslanium.encryptor.EncryptorService;
import com.suslanium.encryptor.R;
import com.suslanium.encryptor.ui.Explorer;
import com.suslanium.encryptor.ui.explorer.ExplorerFragment;

import java.util.ArrayList;

public class GoogleDriveManager extends AppCompatActivity {
    private final Scope SCOPEEMAIL = new Scope(Scopes.EMAIL);
    private final Scope SCOPEAPP = new Scope(Scopes.DRIVE_APPFOLDER);
    private GoogleDriveAdapter adapter = null;
    protected int currentOperationNumber = 0;
    private RecyclerView recyclerView;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton newFolder;
    private FloatingActionButton upload;
    private TextView pathView;
    private TextView sizeView;
    private SwipeRefreshLayout layout;
    private GoogleDriveViewModel viewModel;
    private boolean tutorialComplete = false;

    @Override
    protected void onStart() {
        super.onStart();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.menu_keyexchange);
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.backarrow);
        actionBar.setHomeAsUpIndicator(drawable);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())).get(GoogleDriveViewModel.class);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dark_theme = preferences.getBoolean("dark_Theme", false);
        tutorialComplete = preferences.getBoolean("gDriveTutorialComplete",false);
        if (dark_theme) setTheme(R.style.Theme_Encryptor_Dark_ActionBar);
        else setTheme(R.style.Theme_Encryptor_Light_ActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_drive_manager);
        recyclerView = findViewById(R.id.gDriveFileList);
        if (viewModel.setDrive()) {
            checkForGooglePermissions();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void checkFileBar() {
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
            new Handler().postDelayed(() -> adapter.setCanSelect(true), 200);
        } else if (bottomNavigationView.getVisibility() == View.VISIBLE && checkedIds.isEmpty()) {
            adapter.setCanSelect(false);
            ExplorerFragment.fadeIn(bottomNavigationView);
            ExplorerFragment.fadeOut(newFolder);
            ExplorerFragment.fadeOut(upload);
            new Handler().postDelayed(() -> adapter.setCanSelect(true), 200);
        }
    }

    protected void updateUI(boolean search) {
        try {
            if (currentOperationNumber == 0) {
                currentOperationNumber++;
                viewModel.listFilesInFolder(viewModel.getCurrentFolderID().getValue(), false, false, null);
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
        if (viewModel.getPrevLists().size() > 1) {
            if (currentOperationNumber == 0) {
                currentOperationNumber++;
                recyclerView.stopScroll();
                Animation fadeIn = AnimationUtils.loadAnimation(GoogleDriveManager.this, android.R.anim.slide_out_right);
                fadeIn.setDuration(200);
                fadeIn.setFillAfter(true);
                recyclerView.startAnimation(fadeIn);
                recyclerView.suppressLayout(true);
                try {
                    viewModel.listFilesInFolder(null, true, false, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                constructAndSetPath();
            }
        } else {
            Intent intent = new Intent(GoogleDriveManager.this, Explorer.class);
            intent.putExtra("pass", getIntent().getByteArrayExtra("pass"));
            startActivity(intent);
        }
    }

    private void checkForGooglePermissions() {
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(getApplicationContext()), SCOPEAPP, SCOPEEMAIL)) {
            GoogleSignIn.requestPermissions(GoogleDriveManager.this, 1, GoogleSignIn.getLastSignedInAccount(getApplicationContext()), SCOPEEMAIL, SCOPEAPP);
        } else {
            driveSetUp();
        }
    }

    @SuppressLint("NonConstantResourceId")
    private void driveSetUp() {
        viewModel.setUpDrive();
        Observer<ArrayList<String>[]> listObserver = arrayLists -> {
            if (arrayLists != null) {
                new Handler().postDelayed(() -> {
                    if (adapter == null) {
                        recyclerView.setLayoutManager(new LinearLayoutManager(GoogleDriveManager.this));
                        adapter = new GoogleDriveAdapter(arrayLists[0], arrayLists[1], GoogleDriveManager.this, recyclerView, arrayLists[2], viewModel);
                        recyclerView.setAdapter(adapter);
                        currentOperationNumber = 0;
                    } else {
                        recyclerView.suppressLayout(false);
                        recyclerView.stopScroll();
                        Animation fadeOut = AnimationUtils.loadAnimation(GoogleDriveManager.this, android.R.anim.slide_in_left);
                        fadeOut.setDuration(200);
                        fadeOut.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                currentOperationNumber = 0;
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });
                        recyclerView.startAnimation(fadeOut);
                        adapter.setNewData(arrayLists[0], arrayLists[1], arrayLists[2]);
                    }
                    checkFileBar();
                    recyclerView.scrollToPosition(0);
                }, 200);
            } else {
                Intent intent = new Intent(GoogleDriveManager.this, Explorer.class);
                intent.putExtra("pass", getIntent().getByteArrayExtra("pass"));
                startActivity(intent);
            }
        };
        viewModel.getFileList().observe(this, listObserver);
        bottomNavigationView = findViewById(R.id.gDriveBottomView);
        pathView = findViewById(R.id.storagePathG);
        sizeView = findViewById(R.id.freeSpaceG);
        layout = findViewById(R.id.swipeGDrive);
        layout.setColorSchemeColors(Color.parseColor("#171E21"));
        layout.setProgressBackgroundColorSchemeColor(Color.parseColor("#90A4AE"));
        layout.setOnRefreshListener(() -> {
            Thread thread = new Thread(() -> {
                try {
                    if (currentOperationNumber == 0) {
                        runOnUiThread(() -> {
                            recyclerView.stopScroll();
                            Animation fadeIn = AnimationUtils.loadAnimation(GoogleDriveManager.this, android.R.anim.slide_out_right);
                            fadeIn.setDuration(200);
                            fadeIn.setFillAfter(true);
                            recyclerView.startAnimation(fadeIn);
                            recyclerView.suppressLayout(true);
                        });
                        updateUI(true);
                    } else {
                        runOnUiThread(() -> layout.setRefreshing(false));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> layout.setRefreshing(false));
                }
            });
            thread.start();
        });
        View.OnClickListener deleteListener = v -> {
            ArrayList<String> ids2 = adapter.getCheckedIds();
            if (ids2 != null && ids2.size() > 0) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(GoogleDriveManager.this, R.style.MaterialAlertDialog_rounded)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.goingToDelete)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            Intent intent = new Intent(GoogleDriveManager.this, EncryptorService.class);
                            intent.putExtra("actionType", "gDriveDelete");
                            EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                            int i = EncryptorService.getUniqueID();
                            EncryptorService.getPaths().put(i, ids2);
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
                            EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                            int i = EncryptorService.getUniqueID();
                            EncryptorService.getPaths().put(i, checkedIds);
                            EncryptorService.getNames().put(i, checkedNames);
                            EncryptorService.getMimeTypes().put(i, mimes);
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
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
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
        });
        newFolder = findViewById(R.id.gDriveNewFolder);
        newFolder.setOnClickListener(v -> {
            if (currentOperationNumber == 0) {
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
                        Thread thread = new Thread(() -> {
                            switch (viewModel.createFolder(name)) {
                                case 0:
                                    if (recyclerView != null && adapter != null) {
                                        if (currentOperationNumber == 0) {
                                            runOnUiThread(() -> {
                                                recyclerView.stopScroll();
                                                Animation fadeIn = AnimationUtils.loadAnimation(GoogleDriveManager.this, android.R.anim.slide_out_right);
                                                fadeIn.setDuration(200);
                                                fadeIn.setFillAfter(true);
                                                recyclerView.startAnimation(fadeIn);
                                                recyclerView.suppressLayout(true);
                                            });
                                            updateUI(false);
                                        }
                                    }
                                    break;
                                case 1:
                                    runOnUiThread(() -> Snackbar.make(v, R.string.failedToCreateG, Snackbar.LENGTH_LONG).show());
                                    break;
                                case 2:
                                    runOnUiThread(() -> Snackbar.make(v, R.string.enterValidNameErr, Snackbar.LENGTH_LONG).show());
                                    break;
                                default:
                                    break;
                            }
                        });
                        thread.start();
                    } else {
                        Snackbar.make(v, R.string.enterValidNameErr, Snackbar.LENGTH_LONG).show();
                    }
                });
                builder.show();
            }
        });
        upload = findViewById(R.id.gDriveUpload);
        upload.setOnClickListener(v -> {
            Intent intent = new Intent(GoogleDriveManager.this, GoogleDriveUploadSelector.class);
            intent.putExtra("pass", getIntent().getByteArrayExtra("pass"));
            intent.putExtra("gDriveFolder", viewModel.getCurrentFolderID().getValue());
            startActivity(intent);
        });
        if(!tutorialComplete)showHints();
        showRootFilesInDrive();
    }

    private void showRootFilesInDrive() {
        @SuppressLint("SetTextI18n") Thread thread = new Thread(() -> {
            try {
                double[] freeSpace = viewModel.getFreeSpace();
                if (freeSpace != null) {
                    int finalSpaceDivisonCount = (int) freeSpace[1];
                    double finalFreeSpace = freeSpace[0];
                    runOnUiThread(() -> {
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
                    });
                } else {
                    runOnUiThread(() -> sizeView.setText(""));
                }
                if(viewModel.getFileList().getValue() == null) {
                    currentOperationNumber++;
                    viewModel.listFilesInFolder(viewModel.getCurrentFolderID().getValue(), false, true, getString(R.string.rootFolder));
                }
                runOnUiThread(this::constructAndSetPath);
            } catch (Exception ignored) {}
        });
        thread.start();
    }

    protected void constructAndSetPath() {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < viewModel.getFolders().size(); i++) {
            path.append(java.io.File.separator).append(viewModel.getFolders().get(i));
        }
        String pathStr = new String(path);
        String toSet = ExplorerFragment.fitString(pathView, pathStr);
        pathView.setText(toSet);
    }

    private void showHints(){
        Typeface ubuntu = ResourcesCompat.getFont(this, R.font.ubuntu);
        new TapTargetSequence(this).targets(
                ExplorerFragment.getTapTarget(upload, getString(R.string.gDriveHintTitle1), getString(R.string.gDriveHintMessage1),ubuntu),
                ExplorerFragment.getTapTarget(newFolder, getString(R.string.gDriveHintTitle2), getString(R.string.gDriveHintMessage2),ubuntu)
        ).listener(new TapTargetSequence.Listener() {
            @Override
            public void onSequenceFinish() {
                SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(GoogleDriveManager.this).edit();
                preferences.putBoolean("gDriveTutorialComplete", true);
                preferences.apply();
            }

            @Override
            public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {

            }

            @Override
            public void onSequenceCanceled(TapTarget lastTarget) {

            }
        }).start();
    }
}