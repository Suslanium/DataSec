package com.suslanium.encryptor.ui.explorer;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.suslanium.encryptor.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class ExplorerViewModel extends AndroidViewModel {
    private MutableLiveData<ArrayList<String>> paths;
    private boolean showHiddenFiles = false;
    private MutableLiveData<String> currentPath;

    public ExplorerViewModel(@NonNull Application application) {
        super(application);
        paths = new MutableLiveData<>();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application.getBaseContext());
        showHiddenFiles = preferences.getBoolean("showHidden", false);
    }

    public LiveData<ArrayList<String>> getFileNames(File parent){
        if(paths == null){
            paths = new MutableLiveData<>();
        }
        File[] files2 = parent.listFiles();
        ArrayList<String> paths14 = new ArrayList<>();
        for (int i = 0; i < files2.length; i++) {
            paths14.add(files2[i].getPath());
        }
        List<String> sorted13 = sortFiles(paths14);
        ArrayList<File> filesSorted13 = new ArrayList<>();
        for (int i = 0; i < sorted13.size(); i++) {
            File toAdd = new File(sorted13.get(i));
            if((showHiddenFiles && toAdd.getName().startsWith(".")) || !toAdd.getName().startsWith(".")) {
                filesSorted13.add(toAdd);
            }
        }
        ArrayList<String> fileNames2 = new ArrayList<>();
        for (int i = 0; i < filesSorted13.size(); i++) {
            fileNames2.add(filesSorted13.get(i).getName());
        }
        paths.setValue(fileNames2);
        setPath(parent);
        return paths;
    }

    public LiveData<ArrayList<String>> getCurrentNames(){
        if(paths == null){
            paths = new MutableLiveData<>();
            File internalStorageDir = Environment.getExternalStorageDirectory();
            File[] files = internalStorageDir.listFiles();
            ArrayList<String> paths = new ArrayList<>();
            for (int i = 0; i < files.length; i++) {
                paths.add(files[i].getPath());
            }
            List<String> sorted = sortFiles(paths);
            ArrayList<File> filesSorted = new ArrayList<>();
            for (int i = 0; i < sorted.size(); i++) {
                File toAdd = new File(sorted.get(i));
                if((showHiddenFiles && toAdd.getName().startsWith(".")) || !toAdd.getName().startsWith(".")) {
                    filesSorted.add(toAdd);
                }
            }
            ArrayList<String> fileNames = new ArrayList<>();
            for (int i = 0; i < filesSorted.size(); i++) {
                fileNames.add(filesSorted.get(i).getName());
            }
            setPath(internalStorageDir);
            this.paths.setValue(fileNames);
            return this.paths;
        } else {
            return paths;
        }
    }

    public void setPath(File parent){
        if(currentPath == null){
            currentPath = new MutableLiveData<>();
        }
        currentPath.setValue(parent.getPath());
    }

    public LiveData<String> getPath(){
        if(currentPath == null){
            currentPath = new MutableLiveData<>();
            File internalStorageDir = Environment.getExternalStorageDirectory();
            currentPath.setValue(internalStorageDir.getPath());
        }
        return currentPath;
    }

    public boolean searchFile(String path, String fileName){
        ArrayList<File> searchResult = searchFiles(path, fileName);
        ArrayList<String> fileNamesResult = new ArrayList<>();
        for (int i = 0; i < searchResult.size(); i++) {
            fileNamesResult.add(searchResult.get(i).getPath().substring(getPath().getValue().length() + 1));
        }
        if(!fileNamesResult.isEmpty()) {
            paths.setValue(fileNamesResult);
            return true;
        } else {
            return false;
        }
    }

    private ArrayList<File> searchFiles(String path, String fileName) {
        ArrayList<File> result = new ArrayList<>();
        if(!fileName.toLowerCase().equals(getApplication().getBaseContext().getString(R.string.fav).toLowerCase())) {
            File parent = new File(path);
            File[] childs = parent.listFiles();
            if (childs != null && childs.length > 0) {
                for (int i = 0; i < childs.length; i++) {
                    if (childs[i].getName().contains(fileName)) {
                        if ((showHiddenFiles && childs[i].getName().startsWith(".")) || !childs[i].getName().startsWith(".")) {
                            result.add(childs[i]);
                        }
                    }
                    if (childs[i].isDirectory()) {
                        if ((showHiddenFiles && childs[i].getName().startsWith(".")) || !childs[i].getName().startsWith(".")) {
                            result.addAll(searchFiles(childs[i].getPath(), fileName));
                        }
                    }
                }
            }
        } else {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplication().getBaseContext());
            ArrayList<String> paths = new ArrayList<>(preferences.getStringSet("fav", new HashSet<>()));
            if(!paths.isEmpty()){
                for(int i=0;i<paths.size();i++){
                    if(paths.get(i).startsWith(path)){
                        result.add(new File(paths.get(i)));
                    }
                }
            }
        }
        return result;
    }
    public static List<String> sortFiles(List<String> filePaths) {
        ArrayList<String> sortedFiles = new ArrayList<>();
        ArrayList<String> originDirs = new ArrayList<>();
        ArrayList<String> originFiles = new ArrayList<>();
        if (filePaths != null && !filePaths.isEmpty()) {
            for (int i = 0; i < filePaths.size(); i++) {
                if (new File(filePaths.get(i)).isFile()) {
                    originFiles.add(filePaths.get(i));
                } else {
                    originDirs.add(filePaths.get(i));
                }
            }
            Collections.sort(originDirs, String.CASE_INSENSITIVE_ORDER);
            Collections.sort(originFiles, String.CASE_INSENSITIVE_ORDER);
            sortedFiles.addAll(originDirs);
            sortedFiles.addAll(originFiles);
        }
        return sortedFiles;
    }
}
