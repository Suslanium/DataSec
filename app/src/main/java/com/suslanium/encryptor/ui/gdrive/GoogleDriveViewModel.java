package com.suslanium.encryptor.ui.gdrive;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.suslanium.encryptor.util.DriveServiceHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class GoogleDriveViewModel extends AndroidViewModel {
    private final MutableLiveData<ArrayList<String>[]> names;
    private final HashMap<Integer, ArrayList<String>[]> prevLists = new HashMap<>();
    private Drive googleDriveService = null;
    private DriveServiceHelper mDriveServiceHelper = null;
    private final MutableLiveData<String> currentFolderID;
    private final ArrayList<String> ids = new ArrayList<>();
    private final ArrayList<String> folders = new ArrayList<>();

    public GoogleDriveViewModel(@NonNull Application application) {
        super(application);
        names = new MutableLiveData<>();
        currentFolderID = new MutableLiveData<>();
    }

    public boolean setDrive(){
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getApplication().getBaseContext(), gso);
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplication().getBaseContext());
        return acct != null;
    }

    public void setUpDrive(){
        GoogleSignInAccount mAccount = GoogleSignIn.getLastSignedInAccount(getApplication().getBaseContext());
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(getApplication().getBaseContext(), Collections.singleton(Scopes.DRIVE_APPFOLDER));
        if (mAccount != null) {
            credential.setSelectedAccount(mAccount.getAccount());
        }
        googleDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).setApplicationName("Encryptor").build();
        mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
    }

    protected double[] getFreeSpace(){
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
        } catch (Exception ignored) {

        }
        long free;
        if (total != 0 && used != 0) {
            free = total - used;
            double freeSpace = free;
            int spaceDivisonCount = 0;
            while (freeSpace > 1024) {
                freeSpace = freeSpace / 1024;
                spaceDivisonCount++;
            }
            freeSpace = (double) Math.round(freeSpace * 100) / 100;
            return new double[]{freeSpace,spaceDivisonCount};
        } else {
            return null;
        }
    }

    protected void listFilesInFolder(String currentFolderID, boolean backPress, boolean addFolderAndID, @Nullable String folderName) throws Exception{
        if(!backPress) {
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
                }
            }
            if(addFolderAndID){
                ids.add(currentFolderID);
                folders.add(folderName);
                prevLists.put(prevLists.size(), names);
            }
            this.currentFolderID.postValue(currentFolderID);
            this.names.postValue(names);
        } else {
            if(prevLists.size() > 1){
                ArrayList<String>[] list = prevLists.get(prevLists.size() - 2);
                this.currentFolderID.postValue(ids.get(ids.size() - 2));
                ids.remove(ids.size() - 1);
                folders.remove(folders.get(folders.size() - 1));
                this.names.postValue(list);
                prevLists.remove(prevLists.size() - 1);
            }
        }
    }

    protected LiveData<ArrayList<String>[]> getFileList(){
        return names;
    }

    protected LiveData<String> getCurrentFolderID(){
        return currentFolderID;
    }

    protected ArrayList<String> getFolders(){
        return folders;
    }

    protected HashMap<Integer,ArrayList<String>[]> getPrevLists(){
        return prevLists;
    }

    public int createFolder(String name){
        final int[] result = new int[]{3};
        java.io.File file = new java.io.File(getApplication().getBaseContext().getCacheDir() + java.io.File.separator + name);
        try {
            file.createNewFile();
            file.delete();
                try {
                    mDriveServiceHelper.createFolder(name, getCurrentFolderID().getValue());
                    result[0] = 0;
                } catch (Exception e) {
                    result[0] = 1;
                }
        } catch (Exception e) {
            result[0] = 2;
        }
        return result[0];
    }
}
