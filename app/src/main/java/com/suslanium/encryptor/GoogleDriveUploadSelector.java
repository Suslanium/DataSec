package com.suslanium.encryptor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.suslanium.encryptor.ui.explorer.ExplorerFragment;
import com.suslanium.encryptor.ui.explorer.ExplorerViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import static com.suslanium.encryptor.ui.explorer.ExplorerFragment.sortFiles;

public class GoogleDriveUploadSelector extends AppCompatActivity {
    private ArrayList<String> fileList = new ArrayList<>();
    public FloatingActionButton upFolder;
    public int currentOperationNumber = 0;
    private TextView storagePath;
    private TextView freeSpace;
    public boolean searchEnded = false;
    public boolean showHiddenFiles = false;
    private TextView title;
    private ExplorerViewModel viewModel;


    @Override
    public void onBackPressed() {
        upFolder.performClick();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())).get(ExplorerViewModel.class);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean darkTheme = preferences.getBoolean("dark_Theme", false);
        if (darkTheme) setTheme(R.style.Theme_Encryptor_Dark);
        else setTheme(R.style.Theme_Encryptor_Light);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_drive_upload_selector);
        storagePath = findViewById(R.id.storagePath2);
        freeSpace = findViewById(R.id.freeSpace2);
        title = findViewById(R.id.uploadText);
        ImageButton b1 = findViewById(R.id.searchButtonUpload);
        EditText layout = findViewById(R.id.searchTextUpload);
        final TextView[] search = {findViewById(R.id.searchTextUploadProgress)};
        final ProgressBar[] bar = {findViewById(R.id.progressBarSearchUpload)};
        bar[0].setVisibility(View.GONE);
        search[0].setVisibility(View.GONE);
        showHiddenFiles = preferences.getBoolean("showHidden", false);
        RecyclerView fileView = findViewById(R.id.deviceFiles);
        ArrayList<String> storagePaths;
        storagePaths = viewModel.getStoragePaths();
        ListIterator<String> pathIterator = storagePaths.listIterator();
        pathIterator.next();
        GDriveUploadSelectorAdapter adapter = new GDriveUploadSelectorAdapter(fileList, Environment.getExternalStorageDirectory().getPath(), fileView, this, viewModel);
        fileView.setLayoutManager(new LinearLayoutManager(this));
        fileView.setAdapter(adapter);
        LiveData<double[]> freeSpaces = viewModel.getFreeSpace();
        final Observer<double[]> spaceObserver = new Observer<double[]>() {
            @Override
            public void onChanged(double[] doubles) {
                double freeSpace1 = doubles[1];
                double spaceDivisionCount = doubles[0];
                switch ((int) spaceDivisionCount) {
                    case 0:
                        freeSpace.setText(freeSpace1 + " " + getString(R.string.bytes));
                        break;
                    case 1:
                        freeSpace.setText(freeSpace1 + " " + getString(R.string.kbytes));
                        break;
                    case 2:
                        freeSpace.setText(freeSpace1 + " " + getString(R.string.mbytes));
                        break;
                    case 3:
                        freeSpace.setText(freeSpace1 + " " + getString(R.string.gbytes));
                        break;
                    case 4:
                        freeSpace.setText(freeSpace1 + " " + getString(R.string.tbytes));
                        break;
                    default:
                        break;
                }
            }
        };
        freeSpaces.observe(this, spaceObserver);
        LiveData<ArrayList<String>> currentNames = viewModel.getCurrentNames();
        viewModel.calculateFreeSpace(viewModel.getPath().getValue());
        final Observer<ArrayList<String>> pathsObserver = new Observer<ArrayList<String>>() {
            @Override
            public void onChanged(ArrayList<String> strings) {
                fileList.clear();
                fileList.addAll(strings);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Animation fadeOut1 = AnimationUtils.loadAnimation(GoogleDriveUploadSelector.this, android.R.anim.slide_in_left);
                        fadeOut1.setAnimationListener(new Animation.AnimationListener() {
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
                        fadeOut1.setDuration(200);
                        adapter.setNewData(viewModel.getPath().getValue(), fileList);
                        fileView.scrollToPosition(0);
                        setStoragePath(viewModel.getPath().getValue());
                        fileView.startAnimation(fadeOut1);
                    }
                }, 200);
            }
        };
        currentNames.observe(this, pathsObserver);
        if(currentNames.getValue() != null && !currentNames.getValue().isEmpty()){
            fileList.addAll(currentNames.getValue());
            adapter.setNewData(viewModel.getPath().getValue(), fileList);
            fileView.scrollToPosition(0);
            setStoragePath(viewModel.getPath().getValue());
        } else {
            viewModel.getFileNames(new File(viewModel.getPath().getValue()));
        }
        upFolder = findViewById(R.id.gDriveSelectorUp);
        ImageButton back = findViewById(R.id.backUpload);
        back.setOnClickListener(v -> onBackPressed());
        FloatingActionButton sdcardButton = findViewById(R.id.gDriveChangeStorage);
        sdcardButton.setOnClickListener(v -> {
            if (currentOperationNumber == 0) {
                if (storagePaths.size() > 1) {
                    String path;
                    if (pathIterator.hasNext()) {
                        path = pathIterator.next();
                        if (new File(path).canWrite()) {
                            if (path.equals(this.getFilesDir().getPath())) {
                                viewModel.setCurrentStorageName(getString(R.string.privateFolder));
                                Snackbar.make(v, R.string.swPrivate, Snackbar.LENGTH_LONG).show();
                            } else {
                                viewModel.setCurrentStorageName(getString(R.string.extStorage) + " " + (pathIterator.previousIndex()));
                                Snackbar.make(v, getString(R.string.swExt) + "" + (pathIterator.previousIndex()), Snackbar.LENGTH_LONG).show();
                            }
                            viewModel.setCurrentStoragePath(path);
                            viewModel.calculateFreeSpace(path);
                        } else {
                            sdcardButton.performClick();
                        }
                    } else {
                        while (pathIterator.hasPrevious()) {
                            pathIterator.previous();
                        }
                        path = pathIterator.next();
                        viewModel.setCurrentStorageName(getString(R.string.intStorage));
                        viewModel.setCurrentStoragePath(path);
                        viewModel.calculateFreeSpace(path);
                        Snackbar.make(v, R.string.swInt, Snackbar.LENGTH_LONG).show();
                    }
                    File parent = new File(path);
                    if (parent.canWrite()) {
                        updateUI(fileView, parent);
                    }
                }
            }
        });
        upFolder.setOnClickListener(v -> {
            if (currentOperationNumber == 0) {
                fileView.stopScroll();
                String path = viewModel.getPath().getValue();
                File parent = new File(path).getParentFile();
                boolean matches = false;
                if (searchEnded) {
                    parent = new File(path);
                    searchEnded = false;
                } else {
                    for (int i = 0; i < storagePaths.size(); i++) {
                        if (path.matches(Pattern.quote(storagePaths.get(i)))) matches = true;
                    }
                }
                if (matches) {
                    finish();
                } else {
                    updateUI(fileView, parent);
                }
            }
        });
        FloatingActionButton upload = findViewById(R.id.gDriveSubmit);
        upload.setOnClickListener(v -> {
            ArrayList<String> paths13 = adapter.getCheckedFiles();
            if (!paths13.isEmpty()) {
                Intent intent = new Intent(GoogleDriveUploadSelector.this, EncryptorService.class);
                intent.putExtra("actionType", "gDriveE");
                EncryptorService.uniqueID++;
                int i = EncryptorService.uniqueID;
                EncryptorService.paths.put(i, paths13);
                intent.putExtra("index", i);
                intent.putExtra("pass", getIntent().getByteArrayExtra("pass"));
                intent.putExtra("gDriveFolder", getIntent().getStringExtra("gDriveFolder"));
                ContextCompat.startForegroundService(GoogleDriveUploadSelector.this, intent);
                finish();
            } else {
                Snackbar.make(v, R.string.selectFiles, Snackbar.LENGTH_LONG).show();
            }
        });
        View.OnClickListener searchListener = v -> {
            String fileName = layout.getText().toString();
            if (!fileName.matches("")) {
                adapter.isSearching = true;
                b1.setEnabled(false);
                layout.setVisibility(View.GONE);
                title.setVisibility(View.VISIBLE);
                final InputMethodManager inputMethodManager = (InputMethodManager) GoogleDriveUploadSelector.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                search[0].setText(R.string.searching);
                Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
                fadeIn.setFillAfter(true);
                Animation fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
                fadeOut.setDuration(200);
                fadeIn.setDuration(200);
                fileView.startAnimation(fadeIn);
                search[0] = findViewById(R.id.searchTextUploadProgress);
                bar[0] = findViewById(R.id.progressBarSearchUpload);
                search[0].setVisibility(View.VISIBLE);
                bar[0].setVisibility(View.VISIBLE);
                ExplorerFragment.fadeOut(search[0]);
                ExplorerFragment.fadeOut(bar[0]);
                String path = viewModel.getPath().getValue();
                Thread thread = new Thread(() -> {
                    boolean hasResults = viewModel.searchFile(path, fileName);
                    if (!hasResults) {
                        runOnUiThread(() -> {
                            try {
                                fileView.startAnimation(fadeOut);
                                ExplorerFragment.fadeIn(search[0]);
                                ExplorerFragment.fadeIn(bar[0]);
                                Snackbar.make(v, R.string.noResults, Snackbar.LENGTH_LONG).show();
                            } catch (Exception e) {

                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            ExplorerFragment.fadeIn(search[0]);
                            ExplorerFragment.fadeIn(bar[0]);
                        });
                    }
                    runOnUiThread(() -> {
                        b1.setEnabled(true);
                        adapter.isSearching = false;
                        searchEnded = true;
                    });
                });
                thread.start();
            } else {
                Snackbar.make(v, R.string.enterFileNameErr, Snackbar.LENGTH_LONG).show();
            }
        };
        b1.setOnClickListener(v -> {
            if (layout.getVisibility() == View.GONE) {
                title.setVisibility(View.INVISIBLE);
                layout.setVisibility(View.VISIBLE);
                layout.setOnKeyListener((v14, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        searchListener.onClick(v14);
                        return true;
                    }
                    return false;
                });
                layout.requestFocus();
                final InputMethodManager inputMethodManager = (InputMethodManager) GoogleDriveUploadSelector.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(layout, InputMethodManager.SHOW_IMPLICIT);
            } else {
                //Search
                searchListener.onClick(v);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void updateUI(RecyclerView fileView, File parent) {
        if (currentOperationNumber == 0) {
            fileView.stopScroll();
            currentOperationNumber++;
            Animation fadeIn1 = AnimationUtils.loadAnimation(GoogleDriveUploadSelector.this, android.R.anim.slide_out_right);
            fadeIn1.setDuration(200);
            fadeIn1.setFillAfter(true);
            fileView.startAnimation(fadeIn1);
            Thread thread1 = new Thread(() -> viewModel.getFileNames(parent));
            thread1.start();
        }
    }

    public void setStoragePath(String path) {
        String pathToShow = path.replace(viewModel.getCurrentStoragePath().getValue(), viewModel.getCurrentStorageName().getValue());
        pathToShow = fitString(storagePath, pathToShow);
        storagePath.setText(pathToShow);
    }

    private String fitString(TextView text, String newText) {
        float textWidth = text.getPaint().measureText(newText);
        int startIndex = 1;
        while (textWidth >= text.getMeasuredWidth()) {
            newText = newText.substring(startIndex);
            textWidth = text.getPaint().measureText(newText);
            startIndex++;
        }
        return newText;
    }
}