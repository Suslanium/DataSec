package com.suslanium.encryptor.ui.home;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.microsoft.onedrivesdk.picker.IPicker;
import com.microsoft.onedrivesdk.picker.LinkType;
import com.microsoft.onedrivesdk.picker.Picker;
import com.suslanium.encryptor.Encryptor;
import com.suslanium.encryptor.EncryptorService;
import com.suslanium.encryptor.Explorer;
import com.suslanium.encryptor.ExplorerAdapter;
import com.suslanium.encryptor.R;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.ListIterator;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment {
    private ArrayList<String> fileList = new ArrayList<>();
    //private HomeViewModel homeViewModel;
    public FloatingActionButton upFolder;
    //private IPicker mPicker;
    //private String ONEDRIVE_APP_ID = "4a85af0e-df80-4f4f-a172-625d168df915";
    //private ArrayList<DocumentFile> sdCards = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        return root;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        Explorer parent = (Explorer) context;
        parent.setExplorerFragment(this);
        super.onAttach(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView fileView = getActivity().findViewById(R.id.fileView);
        File[] dir = getContext().getExternalFilesDirs(null);
        ArrayList<String> storagePaths = new ArrayList<>();
        for (int i = 0; i < dir.length; i++) {
            //Recaclulate substring end index after changing package name
            storagePaths.add(dir[i].getPath().substring(0, dir[i].getPath().length() - 43));
        }
        ListIterator<String> pathIterator = storagePaths.listIterator();
        pathIterator.next();
        File internalStorageDir = Environment.getExternalStorageDirectory();
        File[] files = internalStorageDir.listFiles();
        ArrayList<String> fileNames = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            fileNames.add(files[i].getName());
        }
        fileList.addAll(fileNames);
        ExplorerAdapter adapter = new ExplorerAdapter(fileList, Environment.getExternalStorageDirectory().getPath(), fileView, getActivity());
        fileView.setLayoutManager(new LinearLayoutManager(getContext()));
        fileView.setAdapter(adapter);
        upFolder = getActivity().findViewById(R.id.upFolder);
        FloatingActionButton sdcardButton = getActivity().findViewById(R.id.sdCardButton);
        Intent intent2 = ((Explorer) getActivity()).getIntent2();
        sdcardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mPicker = Picker.createPicker(ONEDRIVE_APP_ID);
                //mPicker.startPicking((Activity)v.getContext(), LinkType.WebViewLink);
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
                        File[] files2 = parent.listFiles();
                        ArrayList<String> fileNames2 = new ArrayList<>();
                        for (int i = 0; i < files2.length; i++) {
                            fileNames2.add(files2[i].getName());
                        }
                        fileList.clear();
                        fileList.addAll(fileNames2);
                        adapter.setNewData(parent.getPath(), fileList);
                        fileView.scrollToPosition(0);
                    }
                }
            }
        });
        upFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = adapter.getPath();
                File parent = new File(path).getParentFile();
                boolean matches = false;
                for (int i = 0; i < storagePaths.size(); i++) {
                    if (path.matches(storagePaths.get(i))) matches = true;
                }
                if (matches) {
                    Snackbar.make(v, "Sorry, this is the root.", Snackbar.LENGTH_LONG).show();
                } else {
                    File[] files2 = parent.listFiles();
                    ArrayList<String> fileNames2 = new ArrayList<>();
                    for (int i = 0; i < files2.length; i++) {
                        fileNames2.add(files2[i].getName());
                    }
                    fileList.clear();
                    fileList.addAll(fileNames2);
                    adapter.setNewData(parent.getPath(), fileList);
                    fileView.scrollToPosition(0);
                }
            }
        });
        FloatingActionButton encryptButton = getActivity().findViewById(R.id.encryptButton);
        encryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> paths = adapter.getCheckedFiles();
                if (paths.size() > 0) {
                    CharSequence[] items = new CharSequence[]{"Encrypt file(s)", "Decrypt file(s)"};
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded)
                            .setTitle("Choose action")
                            .setItems(items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0:
                                            boolean alreadyContainsFiles = false;
                                            for (String path : paths) {
                                                File file = new File(path);
                                                if (file.isDirectory()) {
                                                    if (new File(path + "Enc").exists()) {
                                                        alreadyContainsFiles = true;
                                                    }
                                                } else {
                                                    if (new File(path + ".enc").exists()) {
                                                        alreadyContainsFiles = true;
                                                    }
                                                }
                                            }
                                            if (alreadyContainsFiles) {
                                                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded);
                                                dialogBuilder.setTitle("Warning");
                                                dialogBuilder.setMessage("One or more encrypted files already exist. Do you want to delete them or not encrypt files?");
                                                dialogBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent intent = new Intent(getContext(), EncryptorService.class);
                                                        intent.putExtra("actionType", "E");
                                                        intent.putExtra("paths", paths);
                                                        intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                        ContextCompat.startForegroundService(getContext(), intent);
                                                        Snackbar.make(v, "Encryption started!", Snackbar.LENGTH_LONG).show();
                                                    }
                                                });
                                                dialogBuilder.setNegativeButton("Stop", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                    }
                                                });
                                                dialogBuilder.show();
                                            } else {
                                                Intent intent = new Intent(getContext(), EncryptorService.class);
                                                intent.putExtra("actionType", "E");
                                                intent.putExtra("paths", paths);
                                                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                ContextCompat.startForegroundService(getContext(), intent);
                                                Snackbar.make(v, "Encryption started!", Snackbar.LENGTH_LONG).show();
                                            }
                                            break;
                                        case 1:
                                            boolean alreadyContainsFiles2 = false;
                                            for (String path : paths) {
                                                File file = new File(path);
                                                if (file.isDirectory()) {
                                                    if (new File((path).substring(0, (path).length() - 3)).exists()) {
                                                        alreadyContainsFiles2 = true;
                                                    }
                                                } else {
                                                    if (new File((path).substring(0, (path).length() - 4)).exists()) {
                                                        alreadyContainsFiles2 = true;
                                                    }
                                                }
                                            }
                                            if (alreadyContainsFiles2) {
                                                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded);
                                                dialogBuilder.setTitle("Warning");
                                                dialogBuilder.setMessage("One or more decrypted files already exist. Do you want to delete them or not decrypt files?");
                                                dialogBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent intent = new Intent(getContext(), EncryptorService.class);
                                                        intent.putExtra("actionType", "D");
                                                        intent.putExtra("paths", paths);
                                                        intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                        ContextCompat.startForegroundService(getContext(), intent);
                                                        Snackbar.make(v, "Decryption started!", Snackbar.LENGTH_LONG).show();
                                                    }
                                                });
                                                dialogBuilder.setNegativeButton("Stop", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                    }
                                                });
                                                dialogBuilder.show();
                                            } else {
                                                Intent intent = new Intent(getContext(), EncryptorService.class);
                                                intent.putExtra("actionType", "D");
                                                intent.putExtra("paths", paths);
                                                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                ContextCompat.startForegroundService(getContext(), intent);
                                                Snackbar.make(v, "Decryption started!", Snackbar.LENGTH_LONG).show();
                                            }
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            });
                    builder.show();

                } else {
                    Snackbar.make(v, "Please select files/folders", Snackbar.LENGTH_LONG).show();
                }
            }
        });
        FloatingActionButton shareButton = getActivity().findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> paths = adapter.getCheckedFiles();
                ArrayList<String> pathsW_Sub = constructFilePaths(paths);
                ArrayList<Uri> uris = new ArrayList<>();
                if(pathsW_Sub.size() > 0){
                    for(int i = 0; i<pathsW_Sub.size(); i++) {
                        uris.add(FileProvider.getUriForFile(getContext(), "com.suslanium.encryptor.fileprovider", new File(pathsW_Sub.get(i))));
                    }
                    shareFiles(uris);
                } else {
                    Snackbar.make(v, "Please select files/folders", Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void shareFiles(ArrayList<Uri> filePaths){
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            shareIntent.putExtra(Intent.EXTRA_STREAM, filePaths);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(shareIntent);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private ArrayList<String> constructFilePaths(ArrayList<String> paths){
        ArrayList<String> pathsWithFolders = new ArrayList<>();
        for(int i=0;i<paths.size();i++){
            if(new File(paths.get(i)).isDirectory()){
                File[] files = new File(paths.get(i)).listFiles();
                ArrayList<String> subPaths = new ArrayList<>();
                for(int j=0;j<files.length;j++){
                    subPaths.add(files[j].getPath());
                }
                pathsWithFolders.addAll(constructFilePaths(subPaths));
            } else {
                pathsWithFolders.add(paths.get(i));
            }
        }
        return pathsWithFolders;
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}