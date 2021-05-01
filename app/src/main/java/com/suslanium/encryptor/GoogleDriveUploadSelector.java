package com.suslanium.encryptor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean darkTheme = preferences.getBoolean("dark_Theme", true);
        if(darkTheme) setTheme(R.style.Theme_MaterialComponents);
        else setTheme(R.style.Theme_MaterialComponents_Light);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_drive_upload_selector);
        RecyclerView fileView = findViewById(R.id.deviceFiles);
        File[] dir = getExternalFilesDirs(null);
        ArrayList<String> storagePaths = new ArrayList<>();
        for (int i = 0; i < dir.length; i++) {
            //Recaclulate substring end index after changing package name
            storagePaths.add(dir[i].getPath().substring(0, dir[i].getPath().length() - 43));
        }
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
                    } else {
                        Snackbar.make(v, "Sorry, this app cannot access your external storage due to system restrictions.", Snackbar.LENGTH_LONG).show();
                        path = pathIterator.previous();
                    }
                } else {
                    while (pathIterator.hasPrevious()) {
                        pathIterator.previous();
                    }
                    path = pathIterator.next();
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
                for (int i = 0; i < storagePaths.size(); i++) {
                    if (path.matches(Pattern.quote(storagePaths.get(i)))) matches = true;
                }
                if (matches) {
                    Snackbar.make(v, "Sorry, this is the root.", Snackbar.LENGTH_LONG).show();
                } else {
                    currentOperationNumber++;
                    Animation fadeIn = AnimationUtils.loadAnimation(GoogleDriveUploadSelector.this, android.R.anim.slide_out_right);
                    fadeIn.setDuration(200);
                    fadeIn.setFillAfter(true);
                    fileView.startAnimation(fadeIn);
                    Thread thread = new Thread(() -> {
                        File[] files2 = new File[0];
                        if (parent != null) {
                            files2 = parent.listFiles();
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
                            if (parent != null) {
                                adapter.setNewData(parent.getPath(), fileList);
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
    }

}