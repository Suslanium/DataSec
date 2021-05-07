package com.suslanium.encryptor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.suslanium.encryptor.ui.home.HomeFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import static com.suslanium.encryptor.ui.home.HomeFragment.sortFiles;

public class GoogleDriveUploadSelector extends AppCompatActivity {
    private ArrayList<String> fileList = new ArrayList<>();
    public FloatingActionButton upFolder;
    public int currentOperationNumber = 0;
    private TextView storagePath;
    private TextView freeSpace;
    private ArrayList<String> storagePaths;
    private String currentStorageName;
    private String currentStoragePath;
    public boolean searchEnded = false;


    @Override
    public void onBackPressed() {
        upFolder.performClick();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean darkTheme = preferences.getBoolean("dark_Theme", true);
        if(darkTheme) setTheme(R.style.Theme_MaterialComponents_NoActionBar);
        else setTheme(R.style.Theme_MaterialComponents_Light_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_drive_upload_selector);
        storagePath = findViewById(R.id.storagePath2);
        freeSpace = findViewById(R.id.freeSpace2);
        ImageButton b1 = findViewById(R.id.searchButtonUpload);
        EditText layout = findViewById(R.id.searchTextUpload);
        final TextView[] search = {findViewById(R.id.searchTextUploadProgress)};
        final ProgressBar[] bar = {findViewById(R.id.progressBarSearchUpload)};
        bar[0].setVisibility(View.GONE);
        search[0].setVisibility(View.GONE);
        RecyclerView fileView = findViewById(R.id.deviceFiles);
        File[] dir = getExternalFilesDirs(null);
        ArrayList<String> storagePaths = new ArrayList<>();
        for (int i = 0; i < dir.length; i++) {
            //Recaclulate substring end index after changing package name
            storagePaths.add(dir[i].getPath().substring(0, dir[i].getPath().length() - 43));
        }
        currentStorageName = "Internal Storage";
        currentStoragePath = Environment.getExternalStorageDirectory().getPath();
        calculateFreeSpace(currentStoragePath);
        ListIterator<String> pathIterator = storagePaths.listIterator();
        pathIterator.next();
        File internalStorageDir = Environment.getExternalStorageDirectory();
        File[] files = internalStorageDir.listFiles();
        ArrayList<String> paths = new ArrayList<>();
        for(int i=0; i<files.length;i++){
            paths.add(files[i].getPath());
        }
        List<String> sorted = sortFiles(paths);
        ArrayList<File> filesSorted = new ArrayList<>();
        for(int i=0;i<sorted.size();i++){
            filesSorted.add(new File(sorted.get(i)));
        }
        ArrayList<String> fileNames = new ArrayList<>();
        for (int i = 0; i < filesSorted.size(); i++) {
            fileNames.add(filesSorted.get(i).getName());
        }
        fileList.addAll(fileNames);
        GDriveUploadSelectorAdapter adapter = new GDriveUploadSelectorAdapter(fileList, Environment.getExternalStorageDirectory().getPath(), fileView, this);
        fileView.setLayoutManager(new LinearLayoutManager(this));
        fileView.setAdapter(adapter);
        upFolder = findViewById(R.id.gDriveSelectorUp);
        FloatingActionButton sdcardButton = findViewById(R.id.gDriveChangeStorage);
        sdcardButton.setOnClickListener(v -> {
            if (storagePaths.size() > 1) {
                String path;
                if (pathIterator.hasNext()) {
                    path = pathIterator.next();
                    if (new File(path).canWrite()) {
                        Snackbar.make(v, "Switched to External Storage " + (pathIterator.previousIndex()), Snackbar.LENGTH_LONG).show();
                        currentStorageName = "External Storage " + (pathIterator.previousIndex());
                        currentStoragePath = path;
                        setStoragePath(path);
                        calculateFreeSpace(path);
                    } else {
                        Snackbar.make(v, "Sorry, this app cannot access your external storage due to system restrictions.", Snackbar.LENGTH_LONG).show();
                        path = pathIterator.previous();
                    }
                } else {
                    while (pathIterator.hasPrevious()) {
                        pathIterator.previous();
                    }
                    path = pathIterator.next();
                    currentStorageName = "Internal Storage";
                    currentStoragePath = path;
                    setStoragePath(path);
                    calculateFreeSpace(path);
                    Snackbar.make(v, "Switched to Internal Storage", Snackbar.LENGTH_LONG).show();
                }
                File parent = new File(path);
                if (parent.canWrite()) {
                    if (currentOperationNumber == 0) {
                        fileView.stopScroll();
                        currentOperationNumber++;
                        Animation fadeIn = AnimationUtils.loadAnimation(GoogleDriveUploadSelector.this, android.R.anim.slide_out_right);
                        fadeIn.setDuration(200);
                        fadeIn.setFillAfter(true);
                        fileView.startAnimation(fadeIn);
                        Thread thread = new Thread(() -> {
                            File[] files2 = parent.listFiles();
                            ArrayList<String> paths1 = new ArrayList<>();
                            if (files2 != null) {
                                for (int i = 0; i < files2.length; i++) {
                                    paths1.add(files2[i].getPath());
                                }
                            }
                            List<String> sorted1 = sortFiles(paths1);
                            ArrayList<File> filesSorted1 = new ArrayList<>();
                            for (int i = 0; i < sorted1.size(); i++) {
                                filesSorted1.add(new File(sorted1.get(i)));
                            }
                            ArrayList<String> fileNames2 = new ArrayList<>();
                            for (int i = 0; i < filesSorted1.size(); i++) {
                                fileNames2.add(filesSorted1.get(i).getName());
                            }
                            fileList.clear();
                            fileList.addAll(fileNames2);
                            while (!fadeIn.hasEnded()) {
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    Thread.currentThread().interrupt();
                                }
                            }
                            Animation fadeOut = AnimationUtils.loadAnimation(GoogleDriveUploadSelector.this, android.R.anim.slide_in_left);
                            fadeOut.setDuration(200);
                            runOnUiThread(() -> {
                                adapter.setNewData(parent.getPath(), fileList);
                                fileView.scrollToPosition(0);
                                fileView.startAnimation(fadeOut);
                            });
                            while (!fadeOut.hasEnded()) {
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
                }
            }
        });
        upFolder.setOnClickListener(v -> {
            if (currentOperationNumber == 0) {
                fileView.stopScroll();
                String path = adapter.getPath();
                File parent = new File(path).getParentFile();
                boolean matches = false;
                if(searchEnded){
                    Log.d("searchEnded", "True");
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
                    currentOperationNumber++;
                    Animation fadeIn = AnimationUtils.loadAnimation(GoogleDriveUploadSelector.this, android.R.anim.slide_out_right);
                    fadeIn.setDuration(200);
                    fadeIn.setFillAfter(true);
                    fileView.startAnimation(fadeIn);
                    File finalParent = parent;
                    Thread thread = new Thread(() -> {
                        File[] files2 = new File[0];
                        if (finalParent != null) {
                            files2 = finalParent.listFiles();
                        }
                        ArrayList<String> paths12 = new ArrayList<>();
                        if (files2 != null) {
                            for (int i = 0; i < files2.length; i++) {
                                paths12.add(files2[i].getPath());
                            }
                        }
                        List<String> sorted12 = sortFiles(paths12);
                        ArrayList<File> filesSorted12 = new ArrayList<>();
                        for (int i = 0; i < sorted12.size(); i++) {
                            filesSorted12.add(new File(sorted12.get(i)));
                        }
                        ArrayList<String> fileNames2 = new ArrayList<>();
                        for (int i = 0; i < filesSorted12.size(); i++) {
                            fileNames2.add(filesSorted12.get(i).getName());
                        }
                        fileList.clear();
                        fileList.addAll(fileNames2);
                        while (!fadeIn.hasEnded()) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                Thread.currentThread().interrupt();
                            }
                        }
                        Animation fadeOut = AnimationUtils.loadAnimation(GoogleDriveUploadSelector.this, android.R.anim.slide_in_left);
                        fadeOut.setDuration(200);
                        runOnUiThread(() -> {
                            if (finalParent != null) {
                                adapter.setNewData(finalParent.getPath(), fileList);
                                setStoragePath(finalParent.getPath());
                            }
                            fileView.scrollToPosition(0);
                            fileView.startAnimation(fadeOut);
                        });
                        while (!fadeOut.hasEnded()) {
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
            }
        });
        FloatingActionButton upload = findViewById(R.id.gDriveSubmit);
        upload.setOnClickListener(v -> {
            ArrayList<String> paths13 = adapter.getCheckedFiles();
            if(!paths13.isEmpty()) {
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
                Snackbar.make(v, "Please select files/folders", Snackbar.LENGTH_LONG).show();
            }
        });
        View.OnClickListener searchListener = v -> {
            String fileName = layout.getText().toString();
            if (!fileName.matches("")) {
                adapter.isSearching = true;
                b1.setEnabled(false);
                layout.setVisibility(View.GONE);
                final InputMethodManager inputMethodManager = (InputMethodManager) GoogleDriveUploadSelector.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                search[0].setText("Searching...");
                HomeFragment.fadeIn(fileView);
                search[0] = findViewById(R.id.searchTextUploadProgress);
                bar[0] = findViewById(R.id.progressBarSearchUpload);
                search[0].setVisibility(View.VISIBLE);
                bar[0].setVisibility(View.VISIBLE);
                HomeFragment.fadeOut(search[0]);
                HomeFragment.fadeOut(bar[0]);
                String path = adapter.getPath();
                Thread thread = new Thread(() -> {
                    ArrayList<File> searchResult = searchFiles(path, fileName);
                    if (searchResult.isEmpty()) {
                        runOnUiThread(() -> {
                            try {
                                HomeFragment.fadeOut(fileView);
                                HomeFragment.fadeIn(search[0]);
                                HomeFragment.fadeIn(bar[0]);
                                Snackbar.make(v, "No results", Snackbar.LENGTH_LONG).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } else {
                        ArrayList<String> fileNamesResult = new ArrayList<>();
                        for (int i = 0; i < searchResult.size(); i++) {
                            fileNamesResult.add(searchResult.get(i).getPath().substring(adapter.getPath().length() + 1));
                        }
                        fileList.clear();
                        fileList.addAll(fileNamesResult);
                        runOnUiThread(() -> {
                            adapter.setNewData(adapter.getPath(), fileList);
                            fileView.scrollToPosition(0);
                            HomeFragment.fadeOut(fileView);
                            HomeFragment.fadeIn(search[0]);
                            HomeFragment.fadeIn(bar[0]);
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
                Snackbar.make(v, "Please enter file name", Snackbar.LENGTH_LONG).show();
            }
        };
        b1.setOnClickListener(v -> {
            if (layout.getVisibility() == View.GONE) {
                layout.setVisibility(View.VISIBLE);
                layout.setOnKeyListener((v14, keyCode, event) -> {
                    if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER){
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

    public void setStoragePath(String path){
        String pathToShow = path.replace(currentStoragePath, currentStorageName);
        pathToShow = fitString(storagePath, pathToShow);
        storagePath.setText(pathToShow);
    }

    private void calculateFreeSpace(String path){
        StatFs fs = new StatFs(path);
        double freeSpace = fs.getFreeBlocksLong() * fs.getBlockSizeLong();
        int spaceDivisonCount = 0;
        while (freeSpace > 1024){
            freeSpace = freeSpace/1024;
            spaceDivisonCount++;
        }
        freeSpace = (double) Math.round(freeSpace * 100) / 100;
        switch (spaceDivisonCount){
            case 0:
                this.freeSpace.setText(freeSpace + " B free");
                break;
            case 1:
                this.freeSpace.setText(freeSpace + " KB free");
                break;
            case 2:
                this.freeSpace.setText(freeSpace + " MB free");
                break;
            case 3:
                this.freeSpace.setText(freeSpace + " GB free");
                break;
            case 4:
                this.freeSpace.setText(freeSpace + " TB free");
                break;
            default:
                break;
        }
    }
    private String fitString (TextView text, String newText) {
        float textWidth = text.getPaint().measureText(newText);
        int startIndex = 1;
        while (textWidth >= text.getMeasuredWidth()){
            Log.d(String.valueOf(textWidth), String.valueOf(text.getMeasuredWidth()));
            newText = newText.substring(startIndex);
            textWidth = text.getPaint().measureText(newText);
            startIndex++;
        }
        return newText;
    }
    private ArrayList<File> searchFiles(String path, String fileName) {
        ArrayList<File> result = new ArrayList<>();
        File parent = new File(path);
        File[] childs = parent.listFiles();
        if (childs != null && childs.length > 0) {
            for (int i = 0; i < childs.length; i++) {
                if (childs[i].getName().contains(fileName)) {
                    result.add(childs[i]);
                }
                if (childs[i].isDirectory())
                    result.addAll(searchFiles(childs[i].getPath(), fileName));
            }
        }
        return result;
    }
}