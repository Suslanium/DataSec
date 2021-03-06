package com.suslanium.encryptor.util;

import androidx.annotation.Nullable;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DriveServiceHelper {

    private final Drive mDriveService;
    private static final String APPDATAFOLDER = "appDataFolder";


    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    public GoogleDriveFileHolder createFolder(String folderName, String folderId) throws Exception {
        GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
        List<String> root;
        if (folderId == null) {
            root = Collections.singletonList(APPDATAFOLDER);
        } else {
            root = Collections.singletonList(folderId);
        }
        File metadata = new File()
                .setParents(root)
                .setMimeType("application/vnd.google-apps.folder")
                .setName(folderName);
        File googleFile = mDriveService.files().create(metadata).execute();
        if (googleFile == null) {
            throw new Exception("Folder hasn't been created");
        }
        googleDriveFileHolder.setId(googleFile.getId());
        return googleDriveFileHolder;
    }


    public void downloadFile(java.io.File targetFile, String fileId) {
        try(OutputStream outputStream = new FileOutputStream(targetFile)) {
            mDriveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
        } catch (IOException ignored) {
        }
    }

    public void deleteFolderFile(String fileId) throws Exception {
        if (fileId != null) {
            mDriveService.files().delete(fileId).execute();
        }
    }

    public List<File> listDriveFiles(String folderID) throws Exception {
        String pageToken = null;
        FileList result;
        List<File> files = new ArrayList<>();
        if(folderID == null) {
            do {
                result = mDriveService.files().list()
                        .setSpaces(APPDATAFOLDER)
                        .setFields("nextPageToken, files(id, name, mimeType)")
                        .setPageToken(pageToken)
                        .setQ("'" + mDriveService.files().get(APPDATAFOLDER).setFields("id").execute().getId() + "' in parents")
                        .execute();
                if(result.getFiles() != null)files.addAll(result.getFiles());
                pageToken = result.getNextPageToken();
            } while (pageToken != null);
        } else {
            do {
                result = mDriveService.files().list()
                        .setSpaces(APPDATAFOLDER)
                        .setFields("nextPageToken, files(id, name, mimeType)")
                        .setPageToken(pageToken)
                        .setQ("'" + folderID + "' in parents")
                        .execute();
                if(result.getFiles() != null)files.addAll(result.getFiles());
                pageToken = result.getNextPageToken();
            } while (pageToken != null);
        }
        return files;
    }

    public void uploadFile(final java.io.File localFile, @Nullable final String folderId) throws Exception {
        List<String> root;
        if (folderId == null) {
            root = Collections.singletonList(APPDATAFOLDER);
        } else {
            root = Collections.singletonList(folderId);
        }
        File metadata = new File()
                .setParents(root)
                .setName(localFile.getName());
        FileContent fileContent = new FileContent(null, localFile);
        File fileMeta = mDriveService.files().create(metadata, fileContent).execute();
        GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
        googleDriveFileHolder.setId(fileMeta.getId());
        googleDriveFileHolder.setName(fileMeta.getName());
    }

}