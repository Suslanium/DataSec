package com.suslanium.encryptor.ui.explorer;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.suslanium.encryptor.util.Encryptor;
import com.suslanium.encryptor.ui.Explorer;
import com.suslanium.encryptor.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class ExplorerViewModel extends AndroidViewModel {
    private final MutableLiveData<ArrayList<String>> paths;
    private final boolean showHiddenFiles;
    private final MutableLiveData<String> currentPath;
    private final MutableLiveData<double[]> freeSpace;
    private final MutableLiveData<String> currentStoragePath;
    private final MutableLiveData<String> currentStorageName;
    private String password = null;

    public ExplorerViewModel(@NonNull Application application) {
        super(application);
        paths = new MutableLiveData<>();
        currentPath = new MutableLiveData<>();
        freeSpace = new MutableLiveData<>();
        currentStoragePath = new MutableLiveData<>();
        currentStorageName = new MutableLiveData<>();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application.getBaseContext());
        showHiddenFiles = preferences.getBoolean("showHidden", false);
    }

    public void getFileNames(File parent){
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
        paths.postValue(fileNames2);
        setPath(parent);
    }

    public LiveData<ArrayList<String>> getCurrentNames(){
        return paths;
    }

    public void setPath(File parent){
        currentPath.postValue(parent.getPath());
    }

    public LiveData<String> getPath(){
        if(currentPath.getValue() == null){
            File internalStorageDir = Environment.getExternalStorageDirectory();
            try {
                currentPath.setValue(internalStorageDir.getPath());
            } catch (Exception e){
                currentPath.postValue(internalStorageDir.getPath());
            }
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
            paths.postValue(fileNamesResult);
            return true;
        } else {
            return false;
        }
    }

    public void setCurrentStoragePath(String path){
        currentStoragePath.setValue(path);
    }
    public void setCurrentStorageName(String name){
        currentStorageName.setValue(name);
    }

    public LiveData<String> getCurrentStorageName(){
        if(currentStorageName.getValue() == null){
            currentStorageName.setValue(getApplication().getBaseContext().getString(R.string.intStorage));
        }
        return currentStorageName;
    }
    public LiveData<String> getCurrentStoragePath(){
        if(currentStoragePath.getValue() == null){
            currentStoragePath.setValue(Environment.getExternalStorageDirectory().getPath());
        }
        return currentStoragePath;
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

    public void calculateFreeSpace(String path){
        StatFs fs = new StatFs(path);
        double freeSpace = fs.getFreeBlocksLong() * fs.getBlockSizeLong();
        int spaceDivisonCount = 0;
        while (freeSpace > 1024){
            freeSpace = freeSpace/1024;
            spaceDivisonCount++;
        }
        freeSpace = (double) Math.round(freeSpace * 100) / 100;
        this.freeSpace.setValue(new double[]{spaceDivisonCount,freeSpace});
    }
    public LiveData<double[]> getFreeSpace(){
        return freeSpace;
    }
    public ArrayList<String> constructFilePaths(ArrayList<String> paths) {
        ArrayList<String> pathsWithFolders = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            if (new File(paths.get(i)).isDirectory()) {
                File[] files = new File(paths.get(i)).listFiles();
                ArrayList<String> subPaths = new ArrayList<>();
                for (int j = 0; j < files.length; j++) {
                    subPaths.add(files[j].getPath());
                }
                pathsWithFolders.addAll(constructFilePaths(subPaths));
            } else {
                pathsWithFolders.add(paths.get(i));
            }
        }
        return pathsWithFolders;
    }
    public ArrayList<String> getSubFiles(File file) {
        ArrayList<String> paths = new ArrayList<>();
        File[] subFiles = file.listFiles();
        int subFolderCount = 0;
        if (subFiles != null && subFiles.length > 0) {
            for (int x = 0; x < subFiles.length; x++) {
                if (subFiles[x].isFile()) {
                    paths.add(subFiles[x].getPath());
                } else {
                    subFolderCount++;
                    paths.addAll(getSubFiles(subFiles[x]));
                }
            }
        }
        if (subFiles != null && (subFiles.length == 0 || subFolderCount == subFiles.length)) {
            paths.add(file.getPath());
        }
        return paths;
    }
    public ArrayList<String> getStoragePaths(){
        ArrayList<String> storagePaths = new ArrayList<>();
        File[] dir = getApplication().getBaseContext().getExternalFilesDirs(null);
        for (int i = 0; i < dir.length; i++) {
            storagePaths.add(dir[i].getPath().substring(0, dir[i].getPath().length() - 43));
        }
        storagePaths.add(getApplication().getBaseContext().getFilesDir().getPath());
        return storagePaths;
    }

    public void decryptTemp(File encrypted, File cached, Activity activity, AlertDialog alertDialog) throws Exception {
        if(password == null) password = Encryptor.rsadecrypt(((Explorer) activity).getIntent2().getByteArrayExtra("pass"));
        Encryptor.decryptFileAES256(encrypted, password,cached);
        Uri uriForFile = FileProvider.getUriForFile(activity.getBaseContext(), "com.suslanium.encryptor.fileprovider", cached);
        String type = activity.getContentResolver().getType(uriForFile);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uriForFile, type);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.runOnUiThread(() -> {
            alertDialog.dismiss();
            activity.startActivityForResult(intent, 101);
        });
    }
}
