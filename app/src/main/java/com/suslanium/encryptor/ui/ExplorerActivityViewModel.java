package com.suslanium.encryptor.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.suslanium.encryptor.util.Encryptor;

import java.io.File;
import java.util.ArrayList;

public class ExplorerActivityViewModel extends AndroidViewModel {
    private MutableLiveData<Integer> currentFragmentID;
    public ExplorerActivityViewModel(@NonNull Application application) {
        super(application);
        currentFragmentID = new MutableLiveData<>(0);
    }

    public void setCurrentFragmentID(int id){
        currentFragmentID.setValue(id);
    }

    public LiveData<Integer> getID(){
        return currentFragmentID;
    }

    public void deleteFiles(ArrayList<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            File file = new File(paths.get(i));
            if (!file.isFile()) {
                File[] files = file.listFiles();
                if (files != null && files.length > 0) {
                    ArrayList<String> subPaths = new ArrayList<>();
                    for (int j = 0; j < files.length; j++) {
                        subPaths.add(files[j].getPath());
                    }
                    deleteFiles(subPaths);
                }
            }
            Encryptor.wipeFile(file);
            file.delete();
        }
    }

    public void deleteFiles(String path) {
        File file = new File(path);
        if (!file.isFile()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                ArrayList<String> subPaths = new ArrayList<>();
                for (int j = 0; j < files.length; j++) {
                    subPaths.add(files[j].getPath());
                }
                deleteFiles(subPaths);
            }
        }
        Encryptor.wipeFile(file);
        file.delete();
    }
}
