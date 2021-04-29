package com.suslanium.encryptor.ui.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationMenu;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.microsoft.onedrivesdk.picker.IPicker;
import com.microsoft.onedrivesdk.picker.LinkType;
import com.microsoft.onedrivesdk.picker.Picker;
import com.suslanium.encryptor.Encryptor;
import com.suslanium.encryptor.EncryptorService;
import com.suslanium.encryptor.Explorer;
import com.suslanium.encryptor.ExplorerAdapter;
import com.suslanium.encryptor.GoogleDriveManager;
import com.suslanium.encryptor.R;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment {
    private ArrayList<String> fileList = new ArrayList<>();
    //private HomeViewModel homeViewModel;
    public FloatingActionButton upFolder;
    private View.OnClickListener upFolderAction;
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

    public View.OnClickListener getUpFolderAction() {
        return upFolderAction;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BottomNavigationView bottomBar = getActivity().findViewById(R.id.bottomBar);
        //TODO:work with bottombar
        bottomBar.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomBar.getMenu().size(); i++) {
            bottomBar.getMenu().getItem(i).setChecked(false);
        }
        bottomBar.getMenu().setGroupCheckable(0, true, true);
        ((Explorer) getActivity()).explorerVisible = true;
        ListIterator<String> pathIterator;
        RecyclerView fileView = getActivity().findViewById(R.id.fileView);
        ArrayList<String> storagePaths = new ArrayList<>();
        File[] dir = getContext().getExternalFilesDirs(null);
        for (int i = 0; i < dir.length; i++) {
            //Recalculate substring end index after changing package name
            storagePaths.add(dir[i].getPath().substring(0, dir[i].getPath().length() - 43));
        }
        pathIterator = storagePaths.listIterator();
        pathIterator.next();
        File internalStorageDir = Environment.getExternalStorageDirectory();
        File[] files = internalStorageDir.listFiles();
        ArrayList<String> paths = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            paths.add(files[i].getPath());
        }
        ArrayList<String> sorted = sortFiles(paths);
        ArrayList<File> filesSorted = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            filesSorted.add(new File(sorted.get(i)));
        }
        ArrayList<String> fileNames = new ArrayList<>();
        for (int i = 0; i < filesSorted.size(); i++) {
            fileNames.add(filesSorted.get(i).getName());
        }
        fileList.addAll(fileNames);
        Toolbar t = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (((Explorer) getActivity()).searchButton != null)
            t.removeView(((Explorer) getActivity()).searchButton);
        if (((Explorer) getActivity()).searchBar != null) {
            t.removeView(((Explorer) getActivity()).searchBar);
            ((Explorer) getActivity()).searchBar = null;
            final InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        ImageButton b1 = new ImageButton(getContext());
        Drawable drawable = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            drawable = getContext().getDrawable(android.R.drawable.ic_menu_search);
        } else {
            drawable = getResources().getDrawable(android.R.drawable.ic_menu_search);
        }
        b1.setImageDrawable(drawable);
        b1.setBackgroundColor(Color.parseColor("#00000000"));
        Toolbar.LayoutParams l3 = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
        l3.gravity = Gravity.END;
        b1.setLayoutParams(l3);
        ((Explorer) getActivity()).searchButton = b1;
        t.addView(b1);
        final TextView[] search = {getActivity().findViewById(R.id.searchText)};
        final ProgressBar[] bar = {getActivity().findViewById(R.id.progressBarSearch)};
        search[0].setVisibility(View.INVISIBLE);
        bar[0].setVisibility(View.INVISIBLE);
        //-------------
        ExplorerAdapter adapter = new ExplorerAdapter(fileList, Environment.getExternalStorageDirectory().getPath(), fileView, getActivity(), bottomBar);
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        manager.setSmoothScrollbarEnabled(true);
        fileView.setLayoutManager(manager);
        fileView.setAdapter(adapter);
        FloatingActionButton delete = getActivity().findViewById(R.id.deleteFiles);
        Intent intent2 = ((Explorer) getActivity()).getIntent2();
        FloatingActionButton confirm = getActivity().findViewById(R.id.confirmButton);
        FloatingActionButton cancel = getActivity().findViewById(R.id.cancelButton);
        bottomBar.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_deleteFiles:
                        ArrayList<String> paths = adapter.getCheckedFiles();
                        if (paths.isEmpty()) {
                            Snackbar.make(getView(), "Please select files/folders", Snackbar.LENGTH_LONG).show();
                        } else {
                            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded);
                            builder.setTitle("Warning");
                            builder.setMessage("You are going to delete these files. Do you want to proceed?");
                            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final AlertDialog[] builder1 = new AlertDialog[1];
                                    ProgressBar bar = new ProgressBar(getContext());
                                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                    bar.setLayoutParams(lp);
                                    builder1[0] = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded)
                                            .setTitle("Deleting files...")
                                            .setView(bar)
                                            .setCancelable(false)
                                            .create();
                                    builder1[0].show();
                                    Thread thread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Intent intent = new Intent(getContext(), EncryptorService.class);
                                            intent.putExtra("actionType", "delete");
                                            EncryptorService.uniqueID++;
                                            int i = EncryptorService.uniqueID;
                                            EncryptorService.paths.put(i, paths);
                                            intent.putExtra("index", i);
                                            intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                            ContextCompat.startForegroundService(getContext(), intent);
                                            SharedPreferences editor1 = PreferenceManager.getDefaultSharedPreferences(getContext());
                                            while (editor1.getBoolean("deletingFiles", true)) {
                                                try {
                                                    Thread.sleep(100);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    builder1[0].dismiss();
                                                }
                                            });
                                        }
                                    });
                                    thread.start();
                                }
                            });
                            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            builder.show();
                        }
                        break;
                    case R.id.action_shareFiles:
                        ArrayList<String> checkedFiles1 = adapter.getCheckedFiles();
                        ArrayList<String> pathsW_Sub = constructFilePaths(checkedFiles1);
                        ArrayList<Uri> uris = new ArrayList<>();
                        if (pathsW_Sub.size() > 0) {
                            for (int i = 0; i < pathsW_Sub.size(); i++) {
                                uris.add(FileProvider.getUriForFile(getContext(), "com.suslanium.encryptor.fileprovider", new File(pathsW_Sub.get(i))));
                            }
                            shareFiles(uris);
                        } else {
                            Snackbar.make(getView(), "Please select files/folders", Snackbar.LENGTH_LONG).show();
                        }
                        break;
                    case R.id.action_encryptFiles:
                        ArrayList<String> checkedFiles = adapter.getCheckedFiles();
                        if (checkedFiles.size() > 0) {
                            CharSequence[] items = new CharSequence[]{"Encrypt file(s)", "Decrypt file(s)"};
                            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded)
                                    .setTitle("Choose action")
                                    .setItems(items, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case 0:
                                                    boolean alreadyContainsFiles = false;
                                                    for (String path : checkedFiles) {
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
                                                                EncryptorService.uniqueID++;
                                                                int i = EncryptorService.uniqueID;
                                                                EncryptorService.paths.put(i, checkedFiles);
                                                                intent.putExtra("index", i);
                                                                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                                ContextCompat.startForegroundService(getContext(), intent);
                                                                Snackbar.make(getView(), "Encryption started!", Snackbar.LENGTH_LONG).show();
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
                                                        EncryptorService.uniqueID++;
                                                        int i = EncryptorService.uniqueID;
                                                        EncryptorService.paths.put(i, checkedFiles);
                                                        intent.putExtra("index", i);
                                                        intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                        ContextCompat.startForegroundService(getContext(), intent);
                                                        Snackbar.make(getView(), "Encryption started!", Snackbar.LENGTH_LONG).show();
                                                    }
                                                    break;
                                                case 1:
                                                    boolean alreadyContainsFiles2 = false;
                                                    for (String path : checkedFiles) {
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
                                                                EncryptorService.uniqueID++;
                                                                int i = EncryptorService.uniqueID;
                                                                EncryptorService.paths.put(i, checkedFiles);
                                                                intent.putExtra("index", i);
                                                                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                                ContextCompat.startForegroundService(getContext(), intent);
                                                                Snackbar.make(getView(), "Decryption started!", Snackbar.LENGTH_LONG).show();
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
                                                        EncryptorService.uniqueID++;
                                                        int i = EncryptorService.uniqueID;
                                                        EncryptorService.paths.put(i, checkedFiles);
                                                        intent.putExtra("index", i);
                                                        intent.putExtra("actionType", "D");
                                                        //intent.putExtra("paths", checkedFiles);
                                                        intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                        ContextCompat.startForegroundService(getContext(), intent);
                                                        Snackbar.make(getView(), "Decryption started!", Snackbar.LENGTH_LONG).show();
                                                    }
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }
                                    });
                            builder.show();

                        } else {
                            Snackbar.make(getView(), "Please select files/folders", Snackbar.LENGTH_LONG).show();
                        }
                        break;
                    case R.id.action_copyFiles:
                        copyFiles(false, adapter, t, b1, confirm, cancel);
                        break;
                    case R.id.action_moveFiles:
                        copyFiles(true, adapter, t, b1, confirm, cancel);
                        break;
                }
                return false;
            }
        });
        upFolder = getActivity().findViewById(R.id.upFolder);
        FloatingActionButton sdcardButton = getActivity().findViewById(R.id.sdCardButton);
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
                        if (((Explorer) getActivity()).currentOperationNumber == 0) {
                            fileView.stopScroll();
                            ((Explorer) getActivity()).currentOperationNumber++;
                            Animation fadeIn = AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right);
                            fadeIn.setDuration(200);
                            fadeIn.setFillAfter(true);
                            fileView.startAnimation(fadeIn);
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    File[] files2 = parent.listFiles();
                                    ArrayList<String> paths = new ArrayList<>();
                                    for (int i = 0; i < files2.length; i++) {
                                        paths.add(files2[i].getPath());
                                    }
                                    ArrayList<String> sorted = sortFiles(paths);
                                    ArrayList<File> filesSorted = new ArrayList<>();
                                    for (int i = 0; i < sorted.size(); i++) {
                                        filesSorted.add(new File(sorted.get(i)));
                                    }
                                    ArrayList<String> fileNames2 = new ArrayList<>();
                                    for (int i = 0; i < filesSorted.size(); i++) {
                                        fileNames2.add(filesSorted.get(i).getName());
                                    }
                                    fileList.clear();
                                    fileList.addAll(fileNames2);
                                    while (!fadeIn.hasEnded()) {
                                        try {
                                            Thread.sleep(10);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    Animation fadeOut = AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left);
                                    fadeOut.setDuration(200);
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            adapter.setNewData(parent.getPath(), fileList);
                                            fileView.scrollToPosition(0);
                                            fileView.startAnimation(fadeOut);
                                        }
                                    });
                                    while (!fadeOut.hasEnded()) {
                                        try {
                                            Thread.sleep(10);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    ((Explorer) getActivity()).currentOperationNumber--;
                                }
                            });
                            thread.start();
                        }
                    }
                }
            }
        });
        upFolderAction = v -> {
            if (((Explorer) getActivity()).currentOperationNumber == 0) {
                fileView.stopScroll();
                String path = adapter.getPath();
                File parent = new File(path).getParentFile();
                boolean matches = false;
                for (int i = 0; i < storagePaths.size(); i++) {
                    if (path.matches(storagePaths.get(i))) matches = true;
                }
                if (matches) {
                    Snackbar snackbar = Snackbar.make(v, "Sorry, this is the root.", Snackbar.LENGTH_LONG);
                    snackbar.setAction("Exit app", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getActivity().moveTaskToBack(true);
                            getActivity().finishAffinity();
                            //System.exit(0);
                        }
                    });
                    snackbar.show();
                } else {
                    ((Explorer) getActivity()).currentOperationNumber++;
                    Animation fadeIn = AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right);
                    fadeIn.setDuration(200);
                    fadeIn.setFillAfter(true);
                    fileView.startAnimation(fadeIn);
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            File[] files2 = parent.listFiles();
                            ArrayList<String> paths1 = new ArrayList<>();
                            for (int i = 0; i < files2.length; i++) {
                                paths1.add(files2[i].getPath());
                            }
                            ArrayList<String> sorted1 = sortFiles(paths1);
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
                                }
                            }
                            Animation fadeOut = AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left);
                            fadeOut.setDuration(200);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.setNewData(parent.getPath(), fileList);
                                    fileView.scrollToPosition(0);
                                    fileView.startAnimation(fadeOut);
                                }
                            });
                            while (!fadeOut.hasEnded()) {
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            ((Explorer) getActivity()).currentOperationNumber--;
                        }
                    });
                    thread.start();
                }
            }
        };
        FloatingActionButton encryptButton = getActivity().findViewById(R.id.encryptButton);
        FloatingActionButton shareButton = getActivity().findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((Explorer) getActivity()).searchBar == null) {
                    EditText layout = new EditText(getContext());
                    Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                    layoutParams.gravity = Gravity.START;
                    layout.setLayoutParams(l3);
                    layout.setTextColor(Color.parseColor("#FFFFFF"));
                    layout.setHint("Enter file name here...");
                    layout.setSingleLine(true);
                    t.addView(layout, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                    layout.setFocusableInTouchMode(true);
                    layout.requestFocus();
                    final InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.showSoftInput(layout, InputMethodManager.SHOW_IMPLICIT);
                    ((Explorer) getActivity()).searchBar = layout;
                } else {
                    //Search
                    String fileName = ((Explorer) getActivity()).searchBar.getText().toString();
                    if (!fileName.matches("")) {
                        b1.setEnabled(false);
                        shareButton.setEnabled(false);
                        encryptButton.setEnabled(false);
                        sdcardButton.setEnabled(false);
                        upFolder.setEnabled(false);
                        adapter.isSearching = true;
                        if (((Explorer) getActivity()).searchBar != null) {
                            t.removeView(((Explorer) getActivity()).searchBar);
                            ((Explorer) getActivity()).searchBar = null;
                            final InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                        Animation fadeIn = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
                        Animation fadeOut = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
                        fadeOut.setDuration(200);
                        fadeIn.setDuration(200);
                        fadeIn.setFillAfter(true);
                        search[0].setText("Searching...");
                        fileView.startAnimation(fadeIn);
                        fileView.setEnabled(false);
                        search[0] = getActivity().findViewById(R.id.searchText);
                        bar[0] = getActivity().findViewById(R.id.progressBarSearch);
                        search[0].setVisibility(View.VISIBLE);
                        bar[0].setVisibility(View.VISIBLE);
                        search[0].startAnimation(fadeOut);
                        bar[0].startAnimation(fadeOut);
                        String path = adapter.getPath();
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayList<File> searchResult = searchFiles(path, fileName);
                                while (!fadeOut.hasEnded()) {
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (searchResult.isEmpty()) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                fileView.startAnimation(fadeOut);
                                                search[0].startAnimation(fadeIn);
                                                bar[0].startAnimation(fadeIn);
                                                Snackbar.make(v, "No results", Snackbar.LENGTH_LONG).show();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                } else {
                                    ArrayList<String> fileNamesResult = new ArrayList<>();
                                    for (int i = 0; i < searchResult.size(); i++) {
                                        fileNamesResult.add(searchResult.get(i).getPath().substring(adapter.getPath().length() + 1));
                                    }
                                    fileList.clear();
                                    fileList.addAll(fileNamesResult);
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            adapter.setNewData(adapter.getPath(), fileList);
                                            fileView.scrollToPosition(0);
                                            fileView.startAnimation(fadeOut);
                                            search[0].startAnimation(fadeIn);
                                            bar[0].startAnimation(fadeIn);
                                        }
                                    });
                                }
                                while (!fadeIn.hasEnded()) {
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        search[0].setVisibility(View.INVISIBLE);
                                        bar[0].setVisibility(View.INVISIBLE);
                                        adapter.isSearching = false;
                                        shareButton.setEnabled(true);
                                        encryptButton.setEnabled(true);
                                        sdcardButton.setEnabled(true);
                                        upFolder.setEnabled(true);
                                        b1.setEnabled(true);
                                    }
                                });
                            }
                        });
                        thread.start();
                        //fileView.setVisibility(View.INVISIBLE);
                    } else {
                        Snackbar.make(v, "Please enter file name", Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void FadeIn(@NonNull View view) {
        view.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        view.setVisibility(View.GONE);
                    }
                });
    }

    public void FadeOut(@NonNull View view) {
        view.setVisibility(View.VISIBLE);
        view.bringToFront();
        view.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null);
    }

    private void shareFiles(ArrayList<Uri> filePaths) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            shareIntent.putExtra(Intent.EXTRA_STREAM, filePaths);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(shareIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void copyFiles(boolean cut, ExplorerAdapter adapter, Toolbar t, ImageButton b1, FloatingActionButton confirm, FloatingActionButton cancel) {
        ArrayList<String> checkedFiles2 = adapter.getCheckedFiles();
        String originalPath = adapter.getPath();
        if (!checkedFiles2.isEmpty()) {
            if (!cut) t.setTitle("Copy " + checkedFiles2.size() + " items");
            else t.setTitle("Move " + checkedFiles2.size() + " items");
            adapter.setDoingFileOperations(true);
            adapter.deselectAll();
            adapter.closeBottomBar();
            FadeOut(confirm);
            FadeOut(cancel);
            b1.setEnabled(false);
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.setDoingFileOperations(false);
                    t.setTitle("File explorer");
                    FadeIn(confirm);
                    FadeIn(cancel);
                    b1.setEnabled(true);
                }
            });
            confirm.setOnClickListener(v -> {
                ProgressBar bar = new ProgressBar(getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                bar.setLayoutParams(lp);
                AlertDialog prep = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded)
                        .setTitle("Preparing...")
                        .setView(bar)
                        .setCancelable(false)
                        .create();
                prep.show();
                String path = adapter.getPath();
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<String> originalSelected = null;
                        if(cut) {
                            originalSelected = new ArrayList<>(checkedFiles2);
                        }
                        final Integer[] skip = {0};
                        ArrayList<String> tempArrayList = new ArrayList<>();
                        ArrayList<String> toRemoveList = new ArrayList<>();
                        for (int i = 0; i < checkedFiles2.size(); i++) {
                            File file = new File(checkedFiles2.get(i));
                            String copyPath = path + File.separator + file.getName();
                            File copyInto = new File(copyPath);
                            if (file.isFile()) {
                                if (skip[0] == 0) {
                                    int j = 0;
                                    while (copyInto.exists() && copyInto.isDirectory()) {
                                        copyInto = new File(copyPath + "(" + j + ")");
                                        j++;
                                    }
                                    if (copyInto.exists()) {
                                        int dialogResult = showReplacementDialog(file.getName(), path.substring(path.lastIndexOf(File.separator) + 1), lp);
                                        switch (dialogResult) {
                                            case 1:
                                                skip[0] = 3;
                                                break;
                                            case 2:
                                                skip[0] = 2;
                                                toRemoveList.add(checkedFiles2.get(i));
                                                break;
                                            case 3:
                                                break;
                                            case 4:
                                                toRemoveList.add(checkedFiles2.get(i));
                                                break;
                                            case 5:
                                                skip[0] = 5;
                                                toRemoveList.add(checkedFiles2.get(i));
                                                tempArrayList.add("Rename_" + checkedFiles2.get(i));
                                                break;
                                            case 6:
                                                toRemoveList.add(checkedFiles2.get(i));
                                                tempArrayList.add("Rename_" + checkedFiles2.get(i));
                                                break;
                                        }
                                    }
                                } else if (skip[0] == 2) {
                                    if (file.getParent().matches(originalPath)) {
                                        int j = 0;
                                        while (copyInto.exists() && copyInto.isDirectory()) {
                                            copyInto = new File(copyPath + "(" + j + ")");
                                            j++;
                                        }
                                        if (copyInto.exists()) {
                                            toRemoveList.add(checkedFiles2.get(i));
                                        }
                                    }
                                } else if(skip[0] == 5){
                                    if (file.getParent().matches(originalPath)) {
                                        int j = 0;
                                        while (copyInto.exists() && copyInto.isDirectory()) {
                                            copyInto = new File(copyPath + "(" + j + ")");
                                            j++;
                                        }
                                        if (copyInto.exists()) {
                                            toRemoveList.add(checkedFiles2.get(i));
                                            tempArrayList.add("Rename_" + checkedFiles2.get(i));
                                        }
                                    }
                                }
                            } else {
                                //Log.d("Result", file.getName());
                                int j = 0;
                                while (copyInto.exists() && copyInto.isFile()) {
                                    copyInto = new File(copyPath + "(" + j + ")");
                                    j++;
                                }
                                if (copyInto.exists()) {
                                    tempArrayList.addAll(checkReplacements(copyPath, file.getPath(), skip, lp));
                                } else {
                                    tempArrayList.addAll(getSubFiles(file));
                                }
                                //toRemoveList.add(checkedFiles2.get(i));
                            }

                        }
                        checkedFiles2.addAll(tempArrayList);
                        checkedFiles2.removeAll(toRemoveList);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                prep.dismiss();
                                cancel.performClick();
                            }
                        });
                        for (int i = 0; i < checkedFiles2.size(); i++) {
                            Log.d("Result", checkedFiles2.get(i));
                        }
                        Intent intent = new Intent(getContext(), EncryptorService.class);
                        EncryptorService.uniqueID++;
                        int i = EncryptorService.uniqueID;
                        EncryptorService.paths.put(i,checkedFiles2);
                        EncryptorService.path.put(i, path);
                        EncryptorService.originalPath.put(i,originalPath);
                        if (!cut) intent.putExtra("actionType", "copyFiles");
                        else {
                            intent.putExtra("actionType", "moveFiles");
                            EncryptorService.originalPaths.put(i,originalSelected);
                        }
                        //intent.putExtra("paths", checkedFiles2);
                        //intent.putExtra("toCopyPath", path);
                        //intent.putExtra("originalPath", originalPath);
                        intent.putExtra("index", i);
                        ContextCompat.startForegroundService(getContext(), intent);
                    }
                });
                thread.start();
            });
        }
    }

    private ArrayList<String> getSubFiles(File file) {
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

    private ArrayList<String> checkReplacements(String toCopyPath, String path, final Integer[] sk, ViewGroup.LayoutParams lp) {
        File parent = new File(toCopyPath);
        String[] filesInParent = parent.list();
        List<String> names = new ArrayList<>();
        if (filesInParent != null && filesInParent.length > 0) {
            names = Arrays.asList(filesInParent);
        }
        File originParent = new File(path);
        File[] originFiles = originParent.listFiles();
        ArrayList<String> checkedFiles2 = new ArrayList<>();
        if (originFiles != null && originFiles.length > 0) {
            for (int i = 0; i < originFiles.length; i++) {
                checkedFiles2.add(originFiles[i].getPath());
            }
            ArrayList<String> tempArrayLists = new ArrayList<>();
            ArrayList<String> toRemoveList = new ArrayList<>();
            for (int i = 0; i < checkedFiles2.size(); i++) {
                File file = new File(checkedFiles2.get(i));
                //Log.d("Result", file.getName());
                if (file.isFile()) {
                    if (names.contains(file.getName())) {
                        if (sk[0] == 0) {
                            int dialogResult = showReplacementDialog(file.getName(), toCopyPath.substring(toCopyPath.lastIndexOf(File.separator) + 1), lp);
                            switch (dialogResult) {
                                case 1:
                                    sk[0] = 3;
                                    break;
                                case 2:
                                    sk[0] = 2;
                                    toRemoveList.add(checkedFiles2.get(i));
                                    break;
                                case 3:
                                    break;
                                case 4:
                                    toRemoveList.add(checkedFiles2.get(i));
                                    break;
                                case 5:
                                    sk[0] = 5;
                                    //Replace all
                                    toRemoveList.add(checkedFiles2.get(i));
                                    tempArrayLists.add("Rename_" + checkedFiles2.get(i));
                                    break;
                                case 6:
                                    toRemoveList.add(checkedFiles2.get(i));
                                    tempArrayLists.add("Rename_" + checkedFiles2.get(i));
                                    break;
                            }
                        } else if (sk[0] == 2) {
                            toRemoveList.add(checkedFiles2.get(i));
                        } else if(sk[0] == 5){
                            toRemoveList.add(checkedFiles2.get(i));
                            tempArrayLists.add("Rename_" + checkedFiles2.get(i));
                        }
                    } else {
                        tempArrayLists.add(file.getPath());
                    }
                } else {
                    //toRemoveList.add(checkedFiles2.get(i));
                    if (names.contains(file.getName())) {
                        tempArrayLists.addAll(checkReplacements(toCopyPath + File.separator + file.getName(), file.getPath(), sk, lp));
                    } else {
                        //Log.d("Result", file.getName());
                        tempArrayLists.addAll(getSubFiles(file));
                    }
                }

            }
            checkedFiles2.addAll(tempArrayLists);
            checkedFiles2.removeAll(toRemoveList);
        }
        return checkedFiles2;
    }

    private int showReplacementDialog(String fileName, String folderName, ViewGroup.LayoutParams lp) {
        final int[] result = {3};
        final boolean[] dialogVisible = {true};
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CheckBox checkBox = new CheckBox(getContext());
                checkBox.setText("Apply to all files");
                checkBox.setLayoutParams(lp);
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded)
                        .setTitle("Warning")
                        .setView(checkBox)
                        .setCancelable(false)
                        .setMessage("File " + fileName + " already exists in " + folderName + ". Do you want to replace, rename or skip it?")
                        .setPositiveButton("Replace", (dialog, which) -> {
                            if (checkBox.isChecked()) {
                                result[0] = 1;
                            } else {
                                result[0] = 3;
                            }
                            dialogVisible[0] = false;
                            dialog.dismiss();
                        })
                        .setNegativeButton("Skip", (dialog, which) -> {
                            if (checkBox.isChecked()) {
                                result[0] = 2;
                            } else {
                                result[0] = 4;
                            }
                            dialogVisible[0] = false;
                            dialog.dismiss();
                        })
                        .setNeutralButton("Rename", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (checkBox.isChecked()) {
                                    result[0] = 5;
                                } else {
                                    result[0] = 6;
                                }
                                dialogVisible[0] = false;
                                dialog.dismiss();
                            }
                        });
                builder.show();
            }
        });
        while (dialogVisible[0]) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result[0];
    }

    private ArrayList<String> constructFilePaths(ArrayList<String> paths) {
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

    private ArrayList<File> searchFiles(String path, String fileName) {
        ArrayList<File> result = new ArrayList<>();
        File parent = new File(path);
        File[] childs = parent.listFiles();
        if (childs != null && childs.length > 0) {
            for (int i = 0; i < childs.length; i++) {
                //Log.d("a", childs[i].getName());
                if (childs[i].getName().contains(fileName)) {
                    //Log.d("Added", childs[i].getName());
                    result.add(childs[i]);
                }
                if (childs[i].isDirectory())
                    result.addAll(searchFiles(childs[i].getPath(), fileName));
            }
        }
        return result;
    }

    public static ArrayList<String> sortFiles(ArrayList<String> filePaths) {
        ArrayList<String> sortedFiles = new ArrayList<>();
        ArrayList<String> originDirs = new ArrayList<>();
        ArrayList<String> originFiles = new ArrayList<>();
        if (filePaths != null && filePaths.size() > 0) {
            for (int i = 0; i < filePaths.size(); i++) {
                if (new File(filePaths.get(i)).isFile()) {
                    originFiles.add(filePaths.get(i));
                } else {
                    originDirs.add(filePaths.get(i));
                }
            }
            Collections.sort(originDirs, String.CASE_INSENSITIVE_ORDER);
            Collections.sort(originFiles, String.CASE_INSENSITIVE_ORDER);
            //originDirs.sort(String::compareToIgnoreCase);
            //originFiles.sort(String::compareToIgnoreCase);
            sortedFiles.addAll(originDirs);
            sortedFiles.addAll(originFiles);
        }
        return sortedFiles;
    }

    private ArrayList<String> sortFiles(String[] filePaths) {
        ArrayList<String> sortedFiles = new ArrayList<>();
        ArrayList<String> originDirs = new ArrayList<>();
        ArrayList<String> originFiles = new ArrayList<>();
        if (filePaths != null && filePaths.length > 0) {
            for (int i = 0; i < filePaths.length; i++) {
                if (new File(filePaths[i]).isFile()) {
                    originFiles.add(filePaths[i]);
                } else {
                    originDirs.add(filePaths[i]);
                }
            }
            Collections.sort(originDirs, String.CASE_INSENSITIVE_ORDER);
            Collections.sort(originFiles, String.CASE_INSENSITIVE_ORDER);
            //originDirs.sort(String::compareToIgnoreCase);
            //originFiles.sort(String::compareToIgnoreCase);
            sortedFiles.addAll(originDirs);
            sortedFiles.addAll(originFiles);
        }
        return sortedFiles;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        ((Explorer) getActivity()).explorerVisible = false;
        super.onDestroyView();
        //Log.d("Destroyed", "Home");
    }
}