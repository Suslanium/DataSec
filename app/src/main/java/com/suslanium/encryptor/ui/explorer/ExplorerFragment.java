package com.suslanium.encryptor.ui.explorer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.suslanium.encryptor.EncryptorService;
import com.suslanium.encryptor.Explorer;
import com.suslanium.encryptor.ExplorerAdapter;
import com.suslanium.encryptor.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

public class ExplorerFragment extends Fragment {
    private ArrayList<String> fileList = new ArrayList<>();
    private View.OnClickListener upFolderAction;
    private FloatingActionButton newFolder;
    private FloatingActionButton changeStorage;
    private Drawable cancelDrawable;
    private View.OnClickListener newFolderListener;
    private Drawable createDrawable;
    private static final String RENAME = "Rename_";
    private static final String INDEX = "index";
    private static final String ACTIONTYPE = "actionType";
    private View.OnClickListener changeStorageListener;
    private TextView storagePath;
    private TextView freeSpace;
    private ArrayList<String> storagePaths;
    private String currentStorageName;
    private String currentStoragePath;
    public boolean showHiddenFiles = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explorer, container, false);
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

    

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        storagePath = requireActivity().findViewById(R.id.storagePath);
        freeSpace = requireActivity().findViewById(R.id.freeSpace);
        BottomNavigationView bottomBar = requireActivity().findViewById(R.id.bottomBar);
        bottomBar.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomBar.getMenu().size(); i++) {
            bottomBar.getMenu().getItem(i).setChecked(false);
        }
        bottomBar.getMenu().setGroupCheckable(0, true, true);
        ((Explorer) requireActivity()).explorerVisible = true;
        ListIterator<String> pathIterator;
        RecyclerView fileView = requireActivity().findViewById(R.id.fileView);
        SwipeRefreshLayout swipeRefreshLayout = requireActivity().findViewById(R.id.swipeExplorer);
        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#171E21"));
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.parseColor("#90A4AE"));
        storagePaths = new ArrayList<>();
        File[] dir = requireContext().getExternalFilesDirs(null);
        for (int i = 0; i < dir.length; i++) {
            storagePaths.add(dir[i].getPath().substring(0, dir[i].getPath().length() - 43));
        }
        storagePaths.add(requireContext().getFilesDir().getPath());
        currentStorageName = getString(R.string.intStorage);
        currentStoragePath = Environment.getExternalStorageDirectory().getPath();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        showHiddenFiles = preferences.getBoolean("showHidden", false);
        final ViewTreeObserver vto = view.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    setStoragePath(Environment.getExternalStorageDirectory().getPath());
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }
        calculateFreeSpace(Environment.getExternalStorageDirectory().getPath());
        pathIterator = storagePaths.listIterator();
        pathIterator.next();
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
        fileList.addAll(fileNames);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            createDrawable = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_input_add);
        } else {
            createDrawable = getResources().getDrawable(android.R.drawable.ic_input_add);
        }
        cancelDrawable = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cancelDrawable = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_delete);
        } else {
            cancelDrawable = getResources().getDrawable(android.R.drawable.ic_delete);
        }
        Toolbar t = requireActivity().findViewById(R.id.toolbar);
        if (((Explorer) requireActivity()).searchButton != null)
            t.removeView(((Explorer) requireActivity()).searchButton);
        if (((Explorer) requireActivity()).searchBar != null) {
            t.removeView(((Explorer) requireActivity()).searchBar);
            ((Explorer) requireActivity()).searchBar = null;
            final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        ImageButton b1 = new ImageButton(requireContext());
        Drawable drawable;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search);
        } else {
            drawable = getResources().getDrawable(R.drawable.ic_search);
        }
        b1.setImageDrawable(drawable);
        b1.setBackgroundColor(Color.parseColor("#00000000"));
        Toolbar.LayoutParams l3 = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
        l3.gravity = Gravity.END;
        b1.setLayoutParams(l3);
        ((Explorer) requireActivity()).searchButton = b1;
        t.addView(b1);
        b1.setImageDrawable(drawable);
        b1.setBackgroundColor(Color.parseColor("#00000000"));
        final TextView[] search = {requireActivity().findViewById(R.id.searchText)};
        final ProgressBar[] bar = {requireActivity().findViewById(R.id.progressBarSearch)};
        search[0].setVisibility(View.INVISIBLE);
        bar[0].setVisibility(View.INVISIBLE);
        //-------------
        ExplorerAdapter adapter = new ExplorerAdapter(fileList, Environment.getExternalStorageDirectory().getPath(), fileView, requireActivity(), bottomBar, this);
        LinearLayoutManager manager = new LinearLayoutManager(requireContext());
        manager.setSmoothScrollbarEnabled(true);
        fileView.setLayoutManager(manager);
        fileView.setAdapter(adapter);
        Intent intent2 = ((Explorer) requireActivity()).getIntent2();
        FloatingActionButton confirm = requireActivity().findViewById(R.id.confirmButton);
        FloatingActionButton cancel = requireActivity().findViewById(R.id.cancelButton);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateUI(adapter, fileView, new File(adapter.getPath()));
                cancelSearch();
                if (!adapter.getDoingFileOperations() && getAddButtonState() == View.GONE) {
                    showAddButton(true);
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        bottomBar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_deleteFiles:
                    ArrayList<String> paths12 = adapter.getCheckedFiles();
                    if (paths12.isEmpty()) {
                        Snackbar.make(requireView(), R.string.selectFiles, Snackbar.LENGTH_LONG).show();
                    } else {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded);
                        builder.setTitle(R.string.warning);
                        builder.setMessage(R.string.goingToDelete);
                        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                            final AlertDialog[] builder1 = new AlertDialog[1];
                            ProgressBar bar1 = new ProgressBar(requireContext());
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                            bar1.setLayoutParams(lp);
                            builder1[0] = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                                    .setTitle(R.string.deleting)
                                    .setView(bar1)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.bckgnd, (dialog1, which1) -> dialog1.dismiss())
                                    .create();
                            builder1[0].show();
                            adapter.deselectAll();
                            adapter.closeBottomBar();
                            showAddButton(true);
                            Thread thread = new Thread(() -> {
                                String pathBefore = adapter.getPath();
                                Intent intent = new Intent(requireContext(), EncryptorService.class);
                                intent.putExtra(ACTIONTYPE, "delete");
                                EncryptorService.uniqueID++;
                                int i = EncryptorService.uniqueID;
                                EncryptorService.paths.put(i, paths12);
                                intent.putExtra(INDEX, i);
                                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                ContextCompat.startForegroundService(requireContext(), intent);
                                adapter.addAllDeletedFiles(paths12);
                                while (EncryptorService.deletingFiles.get(i) == null) {
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException e) {

                                        Thread.currentThread().interrupt();
                                    }
                                }
                                while (EncryptorService.deletingFiles.get(i) != null) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {

                                        Thread.currentThread().interrupt();
                                    }
                                }
                                String pathAfter = adapter.getPath();
                                boolean match = false;
                                ArrayList<String> localDataSet;
                                if (pathBefore.matches(Pattern.quote(pathAfter))) {
                                    localDataSet = adapter.getLocalDataSet();
                                    ArrayList<String> listToRemove = new ArrayList<>();
                                    if (!localDataSet.isEmpty()) {
                                        for (int j = 0; j < paths12.size(); j++) {
                                            listToRemove.add(paths12.get(j).substring(paths12.get(j).lastIndexOf(File.separator) + 1));
                                        }
                                        localDataSet.removeAll(listToRemove);
                                    }
                                    fileList = localDataSet;
                                    match = true;
                                }
                                adapter.removeAllDeletedFiles(paths12);
                                boolean finalMatch = match;
                                requireActivity().runOnUiThread(() -> {
                                    builder1[0].dismiss();
                                    if (finalMatch) {
                                        adapter.setNewData(pathAfter, fileList);
                                        fileView.smoothScrollToPosition(0);
                                    }
                                });
                            });
                            thread.start();
                        });
                        builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                        builder.show();
                    }
                    break;
                case R.id.action_shareFiles:
                    try {
                        ArrayList<String> checkedFiles1 = adapter.getCheckedFiles();
                        ArrayList<String> pathswSub = constructFilePaths(checkedFiles1);
                        ArrayList<Uri> uris = new ArrayList<>();
                        if (!pathswSub.isEmpty()) {
                            for (int i = 0; i < pathswSub.size(); i++) {
                                uris.add(FileProvider.getUriForFile(requireContext(), "com.suslanium.encryptor.fileprovider", new File(pathswSub.get(i))));
                            }
                            shareFiles(uris);
                        } else {
                            Snackbar.make(requireView(), R.string.selectFiles, Snackbar.LENGTH_LONG).show();
                        }
                        adapter.deselectAll();
                        adapter.closeBottomBar();
                        showAddButton(true);
                    } catch (Exception e){
                        Snackbar.make(requireView(), R.string.smthWentWrong, Snackbar.LENGTH_LONG).show();
                    }
                    break;
                case R.id.action_encryptFiles:
                    ArrayList<String> checkedFiles = adapter.getCheckedFiles();
                    if (!checkedFiles.isEmpty()) {
                        CharSequence[] items = new CharSequence[]{getString(R.string.encFiles), getString(R.string.decFiles)};
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                                .setTitle(R.string.choose)
                                .setItems(items, (dialog, which) -> {
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
                                                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded);
                                                dialogBuilder.setTitle(R.string.warning);
                                                dialogBuilder.setMessage(R.string.encExists);
                                                dialogBuilder.setPositiveButton(R.string.delete, (dialog15, which15) -> {
                                                    Intent intent = new Intent(requireContext(), EncryptorService.class);
                                                    intent.putExtra(ACTIONTYPE, "E");
                                                    EncryptorService.uniqueID++;
                                                    int i = EncryptorService.uniqueID;
                                                    EncryptorService.paths.put(i, checkedFiles);
                                                    intent.putExtra(INDEX, i);
                                                    intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                    ContextCompat.startForegroundService(requireContext(), intent);
                                                    Snackbar.make(requireView(), R.string.encStarted, Snackbar.LENGTH_LONG).show();
                                                    adapter.deselectAll();
                                                    adapter.closeBottomBar();
                                                    showAddButton(true);
                                                });
                                                dialogBuilder.setNegativeButton(R.string.cancel, (dialog14, which14) -> {
                                                });
                                                dialogBuilder.show();
                                            } else {
                                                Intent intent = new Intent(requireContext(), EncryptorService.class);
                                                intent.putExtra(ACTIONTYPE, "E");
                                                EncryptorService.uniqueID++;
                                                int i = EncryptorService.uniqueID;
                                                EncryptorService.paths.put(i, checkedFiles);
                                                intent.putExtra(INDEX, i);
                                                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                ContextCompat.startForegroundService(requireContext(), intent);
                                                Snackbar.make(requireView(), R.string.encStarted, Snackbar.LENGTH_LONG).show();
                                                adapter.deselectAll();
                                                adapter.closeBottomBar();
                                                showAddButton(true);
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
                                                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded);
                                                dialogBuilder.setTitle(R.string.warning);
                                                dialogBuilder.setMessage(R.string.decExists);
                                                dialogBuilder.setPositiveButton(R.string.delete, (dialog13, which13) -> {
                                                    Intent intent = new Intent(requireContext(), EncryptorService.class);
                                                    intent.putExtra(ACTIONTYPE, "D");
                                                    EncryptorService.uniqueID++;
                                                    int i = EncryptorService.uniqueID;
                                                    EncryptorService.paths.put(i, checkedFiles);
                                                    intent.putExtra(INDEX, i);
                                                    intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                    ContextCompat.startForegroundService(requireContext(), intent);
                                                    Snackbar.make(requireView(), R.string.decStarted, Snackbar.LENGTH_LONG).show();
                                                    adapter.deselectAll();
                                                    adapter.closeBottomBar();
                                                    showAddButton(true);
                                                });
                                                dialogBuilder.setNegativeButton(R.string.cancel, (dialog12, which12) -> {
                                                });
                                                dialogBuilder.show();
                                            } else {
                                                Intent intent = new Intent(requireContext(), EncryptorService.class);
                                                EncryptorService.uniqueID++;
                                                int i = EncryptorService.uniqueID;
                                                EncryptorService.paths.put(i, checkedFiles);
                                                intent.putExtra(INDEX, i);
                                                intent.putExtra(ACTIONTYPE, "D");
                                                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                ContextCompat.startForegroundService(requireContext(), intent);
                                                Snackbar.make(requireView(), R.string.decStarted, Snackbar.LENGTH_LONG).show();
                                                adapter.deselectAll();
                                                adapter.closeBottomBar();
                                                showAddButton(true);
                                            }
                                            break;
                                        default:
                                            break;
                                    }
                                });
                        builder.show();
                    } else {
                        Snackbar.make(requireView(), R.string.selectFiles, Snackbar.LENGTH_LONG).show();
                    }
                    break;
                case R.id.action_copyFiles:
                    copyFiles(false, adapter, t, b1, confirm, cancel);
                    break;
                case R.id.action_moveFiles:
                    copyFiles(true, adapter, t, b1, confirm, cancel);
                    break;
                default:
                    break;
            }
            return false;
        });
        changeStorage = requireActivity().findViewById(R.id.sdCardButton);
        changeStorageListener = v -> {
            if (((Explorer) requireActivity()).currentOperationNumber == 0) {
                if (storagePaths.size() > 1) {
                    String path;
                    if (pathIterator.hasNext()) {
                        path = pathIterator.next();
                        if (new File(path).canWrite()) {
                            if (path.equals(requireContext().getFilesDir().getPath())) {
                                currentStorageName = getString(R.string.privateFolder);
                                Snackbar.make(v, getString(R.string.swPrivate), Snackbar.LENGTH_LONG).show();
                            } else {
                                currentStorageName = getString(R.string.extStorage) + " " + (pathIterator.previousIndex());
                                Snackbar.make(v, getString(R.string.swExt) + " " + (pathIterator.previousIndex()), Snackbar.LENGTH_LONG).show();
                            }
                            currentStoragePath = path;
                            setStoragePath(path);
                            calculateFreeSpace(path);
                        } else {
                            changeStorageListener.onClick(v);
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
                        Snackbar.make(v, getString(R.string.swInt), Snackbar.LENGTH_LONG).show();
                    }
                    File parent = new File(path);
                    if (parent.canWrite() && ((Explorer) requireActivity()).currentOperationNumber == 0) {
                        updateUI(adapter, fileView, parent);
                    }
                }
            }
        };
        changeStorage.setOnClickListener(changeStorageListener);
        upFolderAction = v -> {
            if (((Explorer) requireActivity()).currentOperationNumber == 0) {
                fileView.stopScroll();
                String path = adapter.getPath();
                File parent = new File(path).getParentFile();
                boolean matches = false;
                for (int i = 0; i < storagePaths.size(); i++) {
                    if (path.matches(Pattern.quote(storagePaths.get(i)))) matches = true;
                }
                if (matches) {
                    ((Explorer) requireActivity()).incrementBackPressedCount();
                    Snackbar snackbar = Snackbar.make(v, R.string.pressToExit, Snackbar.LENGTH_LONG);
                    snackbar.setAction(getString(R.string.swSt), v1 -> {try{changeStorage.performClick();}catch (Exception e){}});
                    snackbar.show();
                } else {
                    updateUI(adapter, fileView, parent);
                    setStoragePath(parent.getPath());
                }
            }
        };
        newFolder = requireActivity().findViewById(R.id.addNewFolder);
        newFolderListener = v -> {
            if (((Explorer) requireActivity()).currentOperationNumber == 0) {
                final EditText input = new EditText(requireContext());
                Typeface ubuntu = ResourcesCompat.getFont(requireContext(), R.font.ubuntu);
                input.setTypeface(ubuntu);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setSingleLine(true);
                //Translation end
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                        .setTitle(R.string.createFolder)
                        .setView(input)
                        .setPositiveButton(R.string.create, (dialog, which) -> {
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v12 -> {
                    String newName = input.getText().toString();
                    if (newName.matches("")) {
                        Snackbar.make(v12, R.string.enterNameErr, Snackbar.LENGTH_LONG).show();
                    } else if (newName.contains(File.separator)) {
                        Snackbar.make(v12, R.string.enterValidNameErr, Snackbar.LENGTH_LONG).show();
                    } else if (adapter.getLocalDataSet().contains(newName)) {
                        Snackbar.make(v12, R.string.folderExistsErr, Snackbar.LENGTH_LONG).show();
                    } else {
                        Thread thread = new Thread(() -> {
                            try {
                                File testing = new File(adapter.getPath() + File.separator + newName);
                                testing.mkdirs();
                                requireActivity().runOnUiThread(() -> {
                                    dialog.dismiss();
                                    ArrayList<String> currentSet = adapter.getLocalDataSet();
                                    currentSet.add(newName);
                                    fileList = currentSet;
                                    adapter.setNewData(adapter.getPath(), fileList);
                                });
                            } catch (Exception e) {

                                requireActivity().runOnUiThread(() -> Snackbar.make(v12, R.string.enterValidNameErr, Snackbar.LENGTH_LONG).show());
                            }
                        });
                        thread.start();
                    }
                });
            }
        };
        newFolder.setOnClickListener(newFolderListener);
        Drawable finalCancelDrawable = cancelDrawable;
        Drawable finalCreateDrawable = createDrawable;
        View.OnClickListener searchListener = v -> {
            String fileName = ((Explorer) requireActivity()).searchBar.getText().toString();
            if (!fileName.matches("")) {
                b1.setEnabled(false);
                newFolder.setEnabled(false);
                changeStorage.setEnabled(false);
                adapter.isSearching = true;
                if (((Explorer) requireActivity()).searchBar != null) {
                    t.removeView(((Explorer) requireActivity()).searchBar);
                    ((Explorer) requireActivity()).searchBar = null;
                    final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                Animation fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out);
                Animation fadeOut = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in);
                fadeOut.setDuration(200);
                fadeIn.setDuration(200);
                fadeIn.setFillAfter(true);
                search[0].setText(R.string.searching);
                fileView.startAnimation(fadeIn);
                fileView.setEnabled(false);
                search[0] = requireActivity().findViewById(R.id.searchText);
                bar[0] = requireActivity().findViewById(R.id.progressBarSearch);
                search[0].setVisibility(View.VISIBLE);
                bar[0].setVisibility(View.VISIBLE);
                search[0].startAnimation(fadeOut);
                bar[0].startAnimation(fadeOut);
                String path = adapter.getPath();
                Thread thread = new Thread(() -> {
                    ArrayList<File> searchResult = searchFiles(path, fileName);
                    while (!fadeOut.hasEnded()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {

                            Thread.currentThread().interrupt();
                        }
                    }
                    final boolean[] noResults = {false};
                    if (searchResult.isEmpty()) {
                        requireActivity().runOnUiThread(() -> {
                            try {
                                noResults[0] = true;
                                fileView.startAnimation(fadeOut);
                                search[0].startAnimation(fadeIn);
                                bar[0].startAnimation(fadeIn);
                                Snackbar.make(requireView(), R.string.noResults, Snackbar.LENGTH_LONG).show();
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
                        requireActivity().runOnUiThread(() -> {
                            adapter.setNewData(adapter.getPath(), fileList);
                            fileView.scrollToPosition(0);
                            fileView.startAnimation(fadeOut);
                            search[0].startAnimation(fadeIn);
                            bar[0].startAnimation(fadeIn);
                        });
                    }
                    while (!fadeIn.hasEnded()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {

                            Thread.currentThread().interrupt();
                        }
                    }
                    requireActivity().runOnUiThread(() -> {
                        search[0].setVisibility(View.INVISIBLE);
                        bar[0].setVisibility(View.INVISIBLE);
                        adapter.isSearching = false;
                        adapter.setSearchEnded();
                        b1.setEnabled(true);
                        newFolder.setEnabled(true);
                        changeStorage.setEnabled(true);
                        if(!noResults[0]) {
                            newFolder.setImageDrawable(finalCancelDrawable);
                            newFolder.setOnClickListener(v13 -> {
                                File parent = new File(adapter.getPath());
                                updateUI(adapter, fileView, parent);
                                newFolder.setImageDrawable(finalCreateDrawable);
                                newFolder.setOnClickListener(newFolderListener);
                            });
                        }
                    });
                });
                thread.start();
            } else {
                Snackbar.make(v, R.string.enterFileNameErr, Snackbar.LENGTH_LONG).show();
            }
        };
        b1.setOnClickListener(v -> {
            if (((Explorer) requireActivity()).searchBar == null) {
                EditText layout = new EditText(requireContext());
                Typeface ubuntu = ResourcesCompat.getFont(requireContext(), R.font.ubuntu);
                layout.setTypeface(ubuntu);
                Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                layoutParams.gravity = Gravity.START;
                layout.setLayoutParams(l3);
                layout.setTextColor(Color.parseColor("#FFFFFF"));
                layout.setHint(R.string.enterFileNameSearch);
                layout.setSingleLine(true);
                t.addView(layout, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                layout.setFocusableInTouchMode(true);
                layout.setOnKeyListener((v14, keyCode, event) -> {
                    if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER){
                        searchListener.onClick(v14);
                        return true;
                    }
                    return false;
                });
                layout.requestFocus();
                final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(layout, InputMethodManager.SHOW_IMPLICIT);
                ((Explorer) requireActivity()).searchBar = layout;
            } else {
                //Search
                searchListener.onClick(v);
            }
        });
    }

    public void cancelSearch() {
        newFolder.setOnClickListener(newFolderListener);
        newFolder.setImageDrawable(createDrawable);
    }

    public void updateUI(ExplorerAdapter adapter, RecyclerView fileView, File parent) {
        if (((Explorer) requireActivity()).currentOperationNumber == 0) {
            if(adapter.getSearchEnded()) cancelSearch();
            fileView.stopScroll();
            ((Explorer) requireActivity()).currentOperationNumber++;
            Animation fadeIn1 = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_out_right);
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
                Animation fadeOut1 = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_in_left);
                fadeOut1.setDuration(200);
                requireActivity().runOnUiThread(() -> {
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
                ((Explorer) requireActivity()).currentOperationNumber--;
            });
            thread1.start();
        }
    }

    public void showAddButton(boolean show) {
        if (show) {
            fadeOut(newFolder);
            fadeOut(changeStorage);
        } else {
            fadeIn(newFolder);
            fadeIn(changeStorage);
        }
    }

    public int getAddButtonState() {
        return newFolder.getVisibility();
    }

    public static void fadeIn(@NonNull View view) {
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

    public static void fadeOut(@NonNull View view) {
        view.setVisibility(View.VISIBLE);
        view.bringToFront();
        view.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        view.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void shareFiles(ArrayList<Uri> filePaths) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            shareIntent.putExtra(Intent.EXTRA_STREAM, filePaths);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            requireContext().startActivity(shareIntent);
        } catch (Exception e) {

        }
    }

    private void copyFiles(boolean cut, ExplorerAdapter adapter, Toolbar t, ImageButton b1, FloatingActionButton confirm, FloatingActionButton cancel) {
        ArrayList<String> checkedFiles2 = adapter.getCheckedFiles();
        String originalPath = adapter.getPath();
        if (!checkedFiles2.isEmpty()) {
            if (newFolder.getDrawable() == cancelDrawable) newFolder.performClick();
            if (!cut) t.setTitle(R.string.copyTo);
            else t.setTitle(R.string.moveTo);
            adapter.setDoingFileOperations(true);
            adapter.deselectAll();
            adapter.closeBottomBar();
            fadeOut(confirm);
            fadeOut(cancel);
            b1.setEnabled(false);
            cancel.setOnClickListener(v -> {
                adapter.setDoingFileOperations(false);
                t.setTitle(R.string.menu_explorer);
                fadeIn(confirm);
                fadeIn(cancel);
                b1.setEnabled(true);
                adapter.deselectAll();
                adapter.closeBottomBar();
                showAddButton(true);
            });
            confirm.setOnClickListener(v -> {
                ProgressBar bar = new ProgressBar(requireContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                bar.setLayoutParams(lp);
                AlertDialog prep = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                        .setTitle(R.string.preparing)
                        .setView(bar)
                        .setCancelable(false)
                        .create();
                prep.show();
                String path = adapter.getPath();
                Thread thread = new Thread(() -> {
                    ArrayList<String> originalSelected = null;
                    if (cut) {
                        originalSelected = new ArrayList<>(checkedFiles2);
                    }
                    ArrayList<String> whiteList = new ArrayList<>();
                    ArrayList<String> toRemove = new ArrayList<>();
                    HashMap<String, String> folderRenamings = new HashMap<>();
                    String substring = path.substring(path.lastIndexOf(File.separator) + 1);
                    for (int i = 0; i < checkedFiles2.size(); i++) {
                        File origin = new File(checkedFiles2.get(i));
                        if (origin.isDirectory()) {
                            File copy = new File(path + File.separator + origin.getName());
                            if (copy.exists()) {
                                int j = 0;
                                String copyPath = copy.getPath();
                                while (copy.exists() && copy.isFile()) {
                                    copy = new File(copyPath + "(" + j + ")");
                                    j++;
                                }
                                if (copy.exists()) {
                                    int result = showFolderReplacementDialog(origin.getName(), substring);
                                    switch (result) {
                                        case 2:
                                            toRemove.add(checkedFiles2.get(i));
                                            break;
                                        case 3:
                                            while (copy.exists()) {
                                                copy = new File(copyPath + "(" + j + ")");
                                                j++;
                                            }
                                            folderRenamings.put(origin.getName(), copy.getName());
                                            whiteList.add(origin.getPath());
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }

                        }
                    }
                    checkedFiles2.removeAll(toRemove);
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
                                    int dialogResult = showReplacementDialog(file.getName(), substring, lp);
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
                                            tempArrayList.add(RENAME + checkedFiles2.get(i));
                                            break;
                                        case 6:
                                            toRemoveList.add(checkedFiles2.get(i));
                                            tempArrayList.add(RENAME + checkedFiles2.get(i));
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            } else if (skip[0] == 2) {
                                if (file.getParent().matches(Pattern.quote(originalPath))) {
                                    int j = 0;
                                    while (copyInto.exists() && copyInto.isDirectory()) {
                                        copyInto = new File(copyPath + "(" + j + ")");
                                        j++;
                                    }
                                    if (copyInto.exists()) {
                                        toRemoveList.add(checkedFiles2.get(i));
                                    }
                                }
                            } else if (skip[0] == 5 && file.getParent().matches(Pattern.quote(originalPath))) {
                                int j = 0;
                                while (copyInto.exists() && copyInto.isDirectory()) {
                                    copyInto = new File(copyPath + "(" + j + ")");
                                    j++;
                                }
                                if (copyInto.exists()) {
                                    toRemoveList.add(checkedFiles2.get(i));
                                    tempArrayList.add(RENAME + checkedFiles2.get(i));
                                }

                            }
                        } else {
                            int j = 0;
                            while (copyInto.exists() && copyInto.isFile()) {
                                copyInto = new File(copyPath + "(" + j + ")");
                                j++;
                            }
                            if (copyInto.exists() && !whiteList.contains(file.getPath())) {
                                tempArrayList.addAll(checkReplacements(copyPath, file.getPath(), skip, lp));
                            } else {
                                tempArrayList.addAll(getSubFiles(file));
                            }
                        }

                    }
                    checkedFiles2.addAll(tempArrayList);
                    checkedFiles2.removeAll(toRemoveList);
                    requireActivity().runOnUiThread(() -> {
                        prep.dismiss();
                        cancel.performClick();
                    });
                    if (!checkedFiles2.isEmpty()) {
                        Intent intent = new Intent(requireContext(), EncryptorService.class);
                        EncryptorService.uniqueID++;
                        int i = EncryptorService.uniqueID;
                        EncryptorService.paths.put(i, checkedFiles2);
                        EncryptorService.path.put(i, path);
                        EncryptorService.originalPath.put(i, originalPath);
                        EncryptorService.folderReplacements.put(i, folderRenamings);
                        if (!cut) intent.putExtra(ACTIONTYPE, "copyFiles");
                        else {
                            intent.putExtra(ACTIONTYPE, "moveFiles");
                            EncryptorService.originalPaths.put(i, originalSelected);
                        }
                        intent.putExtra(INDEX, i);
                        ContextCompat.startForegroundService(requireContext(), intent);
                    }
                });
                thread.start();
            });
        }
    }

    private int showFolderReplacementDialog(String name, String substring) {
        final int[] result = {0};
        final boolean[] dialogVisible = {true};
        requireActivity().runOnUiThread(() -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                    .setTitle(R.string.warning)
                    .setCancelable(false)
                    .setMessage(getString(R.string.folder)+" " + name + " "+getString(R.string.alreadyExistsSubStr)+" " + substring + getString(R.string.replaceFolderEnding))
                    .setPositiveButton(R.string.merge, (dialog, which) -> {
                        result[0] = 1;
                        dialogVisible[0] = false;
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.skip, (dialog, which) -> {
                        result[0] = 2;
                        dialogVisible[0] = false;
                        dialog.dismiss();
                    })
                    .setNeutralButton(R.string.rename, (dialog, which) -> {
                        result[0] = 3;
                        dialogVisible[0] = false;
                        dialog.dismiss();
                    });
            builder.show();
        });
        while (dialogVisible[0]) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
            }
        }
        return result[0];
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
                                case 4:
                                    toRemoveList.add(checkedFiles2.get(i));
                                    break;
                                case 5:
                                    sk[0] = 5;
                                    toRemoveList.add(checkedFiles2.get(i));
                                    tempArrayLists.add(RENAME + checkedFiles2.get(i));
                                    break;
                                case 6:
                                    toRemoveList.add(checkedFiles2.get(i));
                                    tempArrayLists.add(RENAME + checkedFiles2.get(i));
                                    break;
                                default:
                                    break;
                            }
                        } else if (sk[0] == 2) {
                            toRemoveList.add(checkedFiles2.get(i));
                        } else if (sk[0] == 5) {
                            toRemoveList.add(checkedFiles2.get(i));
                            tempArrayLists.add(RENAME + checkedFiles2.get(i));
                        }
                    } else {
                        tempArrayLists.add(file.getPath());
                    }
                } else {
                    if (names.contains(file.getName())) {
                        tempArrayLists.addAll(checkReplacements(toCopyPath + File.separator + file.getName(), file.getPath(), sk, lp));
                    } else {
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
        requireActivity().runOnUiThread(() -> {
            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setText(R.string.applyToAllFiles);
            checkBox.setLayoutParams(lp);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                    .setTitle(R.string.warning)
                    .setView(checkBox)
                    .setCancelable(false)
                    .setMessage(getString(R.string.file)+" " + fileName +" "+ getString(R.string.alreadyExistsSubStr) +" "+ folderName + getString(R.string.replaceFileEnding))
                    .setPositiveButton(R.string.replace, (dialog, which) -> {
                        if (checkBox.isChecked()) {
                            result[0] = 1;
                        } else {
                            result[0] = 3;
                        }
                        dialogVisible[0] = false;
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.skip, (dialog, which) -> {
                        if (checkBox.isChecked()) {
                            result[0] = 2;
                        } else {
                            result[0] = 4;
                        }
                        dialogVisible[0] = false;
                        dialog.dismiss();
                    })
                    .setNeutralButton(R.string.rename, (dialog, which) -> {
                        if (checkBox.isChecked()) {
                            result[0] = 5;
                        } else {
                            result[0] = 6;
                        }
                        dialogVisible[0] = false;
                        dialog.dismiss();
                    });
            builder.show();
        });
        while (dialogVisible[0]) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
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
        if(!fileName.toLowerCase().equals(getString(R.string.fav).toLowerCase())) {
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
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
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
    public static String fitString (TextView text, String newText) {
        float textWidth = text.getPaint().measureText(newText);
        int startIndex = 1;
        while (textWidth >= text.getMeasuredWidth()){
            newText = newText.substring(startIndex);
            textWidth = text.getPaint().measureText(newText);
            startIndex++;
        }
        return newText;
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

    @Override
    public void onDestroyView() {
        ((Explorer) requireActivity()).explorerVisible = false;
        super.onDestroyView();
    }
}