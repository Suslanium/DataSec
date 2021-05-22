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
    private String currentStorageName;
    private String currentStoragePath;
    public boolean searchEnded = false;
    public boolean showHiddenFiles = false;
    private TextView title;


    @Override
    public void onBackPressed() {
        upFolder.performClick();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean darkTheme = preferences.getBoolean("dark_Theme", false);
        if(darkTheme) setTheme(R.style.Theme_Encryptor_Dark);
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
        File[] dir = getExternalFilesDirs(null);
        ArrayList<String> storagePaths = new ArrayList<>();
        for (int i = 0; i < dir.length; i++) {
            //Recaclulate substring end index after changing package name
            storagePaths.add(dir[i].getPath().substring(0, dir[i].getPath().length() - 43));
        }
        storagePaths.add(this.getFilesDir().getPath());
        currentStorageName = getString(R.string.intStorage);
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
            File toAdd = new File(sorted.get(i));
            if((showHiddenFiles && toAdd.getName().startsWith(".")) || !toAdd.getName().startsWith(".")) {
                filesSorted.add(toAdd);
            }
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
        ImageButton back = findViewById(R.id.backUpload);
        back.setOnClickListener(v -> onBackPressed());
        FloatingActionButton sdcardButton = findViewById(R.id.gDriveChangeStorage);
        sdcardButton.setOnClickListener(v -> {
            if (storagePaths.size() > 1) {
                String path;
                if (pathIterator.hasNext()) {
                    path = pathIterator.next();
                    if (new File(path).canWrite()) {
                        if(path.equals(this.getFilesDir().getPath())){
                            currentStorageName = getString(R.string.privateFolder);
                            Snackbar.make(v, R.string.swPrivate, Snackbar.LENGTH_LONG).show();
                        } else {
                            currentStorageName = getString(R.string.extStorage)+" " + (pathIterator.previousIndex());
                            Snackbar.make(v, getString(R.string.swExt)+"" + (pathIterator.previousIndex()), Snackbar.LENGTH_LONG).show();
                        }
                        currentStoragePath = path;
                        setStoragePath(path);
                        calculateFreeSpace(path);
                    } else {
                        sdcardButton.performClick();
                    }
                } else {
                    while (pathIterator.hasPrevious()) {
                        pathIterator.previous();
                    }
                    path = pathIterator.next();
                    currentStorageName = getString(R.string.intStorage);
                    currentStoragePath = path;
                    setStoragePath(path);
                    calculateFreeSpace(path);
                    Snackbar.make(v, R.string.swInt, Snackbar.LENGTH_LONG).show();
                }
                File parent = new File(path);
                if (parent.canWrite()) {
                    updateUI(adapter, fileView, parent);
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
                    updateUI(adapter, fileView, parent);
                    setStoragePath(parent.getPath());
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
                                Snackbar.make(v, R.string.noResults, Snackbar.LENGTH_LONG).show();
                            } catch (Exception e) {

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
                Snackbar.make(v, R.string.enterFileNameErr, Snackbar.LENGTH_LONG).show();
            }
        };
        b1.setOnClickListener(v -> {
            if (layout.getVisibility() == View.GONE) {
                title.setVisibility(View.INVISIBLE);
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

    public void updateUI(GDriveUploadSelectorAdapter adapter, RecyclerView fileView, File parent) {
        if (currentOperationNumber == 0) {
            fileView.stopScroll();
            currentOperationNumber++;
            Animation fadeIn1 = AnimationUtils.loadAnimation(GoogleDriveUploadSelector.this, android.R.anim.slide_out_right);
            fadeIn1.setDuration(200);
            fadeIn1.setFillAfter(true);
            fileView.startAnimation(fadeIn1);
            Thread thread1 = new Thread(() -> {
                File[] files2 = parent.listFiles();
                ArrayList<String> paths14 = new ArrayList<>();
                for (int i = 0; i < files2.length; i++) {
                    paths14.add(files2[i].getPath());
                }
                List<String> sorted13 = sortFiles(paths14);
                ArrayList<File> filesSorted13 = new ArrayList<>();
                for (int i = 0; i < sorted13.size(); i++) {
                    //-----------------------------------------------
                    File toAdd = new File(sorted13.get(i));
                    if((showHiddenFiles && toAdd.getName().startsWith(".")) || !toAdd.getName().startsWith(".")) {
                        filesSorted13.add(toAdd);
                    }
                }
                ArrayList<String> fileNames2 = new ArrayList<>();
                for (int i = 0; i < filesSorted13.size(); i++) {
                    fileNames2.add(filesSorted13.get(i).getName());
                }
                fileList.clear();
                fileList.addAll(fileNames2);
                while (!fadeIn1.hasEnded()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {

                        Thread.currentThread().interrupt();
                    }
                }
                Animation fadeOut1 = AnimationUtils.loadAnimation(GoogleDriveUploadSelector.this, android.R.anim.slide_in_left);
                fadeOut1.setDuration(200);
                runOnUiThread(() -> {
                    adapter.setNewData(parent.getPath(), fileList);
                    fileView.scrollToPosition(0);
                    fileView.startAnimation(fadeOut1);
                });
                while (!fadeOut1.hasEnded()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {

                        Thread.currentThread().interrupt();
                    }
                }
                currentOperationNumber--;
            });
            thread1.start();
        }
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
                this.freeSpace.setText(freeSpace + " "+getString(R.string.bytes));
                break;
            case 1:
                this.freeSpace.setText(freeSpace + " "+getString(R.string.kbytes));
                break;
            case 2:
                this.freeSpace.setText(freeSpace + " "+getString(R.string.mbytes));
                break;
            case 3:
                this.freeSpace.setText(freeSpace + " "+getString(R.string.gbytes));
                break;
            case 4:
                this.freeSpace.setText(freeSpace + " "+getString(R.string.tbytes));
                break;
            default:
                break;
        }
    }
    private String fitString (TextView text, String newText) {
        float textWidth = text.getPaint().measureText(newText);
        int startIndex = 1;
        while (textWidth >= text.getMeasuredWidth()){
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
                    if((showHiddenFiles && childs[i].getName().startsWith(".")) || !childs[i].getName().startsWith(".")) {
                        result.add(childs[i]);
                    }
                }
                if (childs[i].isDirectory()) {
                    if((showHiddenFiles && childs[i].getName().startsWith(".")) || !childs[i].getName().startsWith(".")) {
                        result.addAll(searchFiles(childs[i].getPath(), fileName));
                    }
                }
            }
        }
        return result;
    }
}