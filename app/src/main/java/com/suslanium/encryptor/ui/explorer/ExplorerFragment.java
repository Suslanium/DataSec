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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.suslanium.encryptor.EncryptorService;
import com.suslanium.encryptor.ui.Explorer;
import com.suslanium.encryptor.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    private ExplorerViewModel viewModel;
    private ImageButton searchButton;
    private boolean tutorialComplete = false;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this.requireActivity().getApplication())).get(ExplorerViewModel.class);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        tutorialComplete = preferences.getBoolean("explorerTutorialComplete", false);
    }

    public View.OnClickListener getUpFolderAction() {
        return upFolderAction;
    }


    @SuppressLint("NonConstantResourceId")
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
        ((Explorer) requireActivity()).setExplorerVisible(true);
        ListIterator<String> pathIterator;
        RecyclerView fileView = requireActivity().findViewById(R.id.fileView);
        SwipeRefreshLayout swipeRefreshLayout = requireActivity().findViewById(R.id.swipeExplorer);
        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#171E21"));
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.parseColor("#90A4AE"));
        storagePaths = viewModel.getStoragePaths();
        pathIterator = storagePaths.listIterator();
        pathIterator.next();
        LiveData<ArrayList<String>> currentNames = viewModel.getCurrentNames();
        createDrawable = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_input_add);
        cancelDrawable = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_delete);
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        if (((Explorer) requireActivity()).getSearchButton() != null)
            toolbar.removeView(((Explorer) requireActivity()).getSearchButton());
        if (((Explorer) requireActivity()).getSearchBar() != null) {
            toolbar.removeView(((Explorer) requireActivity()).getSearchBar());
            ((Explorer) requireActivity()).setSearchBar(null);
            final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        searchButton = new ImageButton(requireContext());
        Drawable drawable;
        drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search);
        searchButton.setImageDrawable(drawable);
        searchButton.setBackgroundColor(Color.parseColor("#00000000"));
        Toolbar.LayoutParams l3 = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
        l3.gravity = Gravity.END;
        searchButton.setLayoutParams(l3);
        ((Explorer) requireActivity()).setSearchButton(searchButton);
        toolbar.addView(searchButton);
        searchButton.setImageDrawable(drawable);
        searchButton.setBackgroundColor(Color.parseColor("#00000000"));
        final TextView[] search = {requireActivity().findViewById(R.id.searchText)};
        final ProgressBar[] bar = {requireActivity().findViewById(R.id.progressBarSearch)};
        search[0].setVisibility(View.INVISIBLE);
        bar[0].setVisibility(View.INVISIBLE);
        final ExplorerAdapter[] adapter = {null};
        LiveData<double[]> freeSpaces = viewModel.getFreeSpace();
        @SuppressLint("SetTextI18n") final Observer<double[]> spaceObserver = doubles -> {
            double freeSpace1 = doubles[1];
            double spaceDivisionCount = doubles[0];
            switch ((int) spaceDivisionCount) {
                case 0:
                    freeSpace.setText(freeSpace1 + " " + getString(R.string.bytes));
                    break;
                case 1:
                    freeSpace.setText(freeSpace1 + " " + getString(R.string.kbytes));
                    break;
                case 2:
                    freeSpace.setText(freeSpace1 + " " + getString(R.string.mbytes));
                    break;
                case 3:
                    freeSpace.setText(freeSpace1 + " " + getString(R.string.gbytes));
                    break;
                case 4:
                    freeSpace.setText(freeSpace1 + " " + getString(R.string.tbytes));
                    break;
                default:
                    break;
            }
        };
        freeSpaces.observe(getViewLifecycleOwner(), spaceObserver);
        viewModel.calculateFreeSpace(viewModel.getPath().getValue());
        final Observer<ArrayList<String>> pathsObserver = strings -> {
            fileList.clear();
            fileList.addAll(strings);
            if (adapter[0] != null) {
                new Handler().postDelayed(() -> {
                    Animation fadeOut1 = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_in_left);
                    fadeOut1.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            ((Explorer) requireActivity()).setCurrentOperationNumber(0);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    fileView.suppressLayout(false);
                    fadeOut1.setDuration(200);
                    fileView.startAnimation(fadeOut1);
                    adapter[0].setNewData(viewModel.getPath().getValue(), fileList);
                    fileView.scrollToPosition(0);
                    setStoragePath(viewModel.getPath().getValue());
                }, 200);
            } else {
                adapter[0] = new ExplorerAdapter(fileList, viewModel.getPath().getValue(), fileView, requireActivity(), bottomBar, ExplorerFragment.this, viewModel);
                LinearLayoutManager manager = new LinearLayoutManager(requireContext());
                manager.setSmoothScrollbarEnabled(true);
                fileView.setLayoutManager(manager);
                fileView.setAdapter(adapter[0]);
                if(!tutorialComplete)showHints(fileView,toolbar);
                else requireActivity().findViewById(R.id.hintDummy1).setVisibility(View.GONE);
            }
        };
        if (currentNames.getValue() != null && !currentNames.getValue().isEmpty()) {
            if (adapter[0] != null) {
                fileList.addAll(currentNames.getValue());
                adapter[0].setNewData(viewModel.getPath().getValue(), fileList);
                fileView.scrollToPosition(0);
                setStoragePath(viewModel.getPath().getValue());
            }
        } else {
            viewModel.getFileNames(new File(viewModel.getPath().getValue()));
        }
        currentNames.observe(getViewLifecycleOwner(), pathsObserver);
        Intent intent2 = ((Explorer) requireActivity()).getIntent2();
        FloatingActionButton confirm = requireActivity().findViewById(R.id.confirmButton);
        FloatingActionButton cancel = requireActivity().findViewById(R.id.cancelButton);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            updateUI(adapter[0], fileView, new File(viewModel.getPath().getValue()));
            cancelSearch();
            if (!adapter[0].getDoingFileOperations() && getAddButtonState() == View.GONE) {
                showAddButton(true);
            }
            swipeRefreshLayout.setRefreshing(false);
        });
        bottomBar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_deleteFiles:
                    ArrayList<String> paths12 = adapter[0].getCheckedFiles();
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
                            adapter[0].deselectAll();
                            adapter[0].closeBottomBar();
                            showAddButton(true);
                            Thread thread = new Thread(() -> {
                                String pathBefore = viewModel.getPath().getValue();
                                Intent intent = new Intent(requireContext(), EncryptorService.class);
                                intent.putExtra(ACTIONTYPE, "delete");
                                EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                                int i = EncryptorService.getUniqueID();
                                EncryptorService.getPaths().put(i, paths12);
                                intent.putExtra(INDEX, i);
                                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                ContextCompat.startForegroundService(requireContext(), intent);
                                adapter[0].addAllDeletedFiles(paths12);
                                while (EncryptorService.getDeletingFiles().get(i) == null) {
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException e) {

                                        Thread.currentThread().interrupt();
                                    }
                                }
                                while (EncryptorService.getDeletingFiles().get(i) != null) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {

                                        Thread.currentThread().interrupt();
                                    }
                                }
                                String pathAfter = viewModel.getPath().getValue();
                                boolean match = false;
                                ArrayList<String> localDataSet;
                                if (pathBefore.matches(Pattern.quote(pathAfter))) {
                                    localDataSet = adapter[0].getLocalDataSet();
                                    ArrayList<String> listToRemove = new ArrayList<>();
                                    if (!localDataSet.isEmpty()) {
                                        for (int j = 0; j < paths12.size(); j++) {
                                            listToRemove.add(paths12.get(j).replaceFirst(Pattern.quote(pathBefore), "").substring(1));
                                        }
                                        localDataSet.removeAll(listToRemove);
                                    }
                                    fileList = localDataSet;
                                    match = true;
                                }
                                adapter[0].removeAllDeletedFiles(paths12);
                                boolean finalMatch = match;
                                requireActivity().runOnUiThread(() -> {
                                    builder1[0].dismiss();
                                    if (finalMatch) {
                                        adapter[0].setNewData(pathAfter, fileList);
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
                        ArrayList<String> checkedFiles1 = adapter[0].getCheckedFiles();
                        ArrayList<String> pathswSub = viewModel.constructFilePaths(checkedFiles1);
                        ArrayList<Uri> uris = new ArrayList<>();
                        if (!pathswSub.isEmpty()) {
                            for (int i = 0; i < pathswSub.size(); i++) {
                                uris.add(FileProvider.getUriForFile(requireContext(), "com.suslanium.encryptor.fileprovider", new File(pathswSub.get(i))));
                            }
                            shareFiles(uris);
                        } else {
                            Snackbar.make(requireView(), R.string.selectFiles, Snackbar.LENGTH_LONG).show();
                        }
                        adapter[0].deselectAll();
                        adapter[0].closeBottomBar();
                        showAddButton(true);
                    } catch (Exception e) {
                        Snackbar.make(requireView(), R.string.smthWentWrong, Snackbar.LENGTH_LONG).show();
                    }
                    break;
                case R.id.action_encryptFiles:
                    ArrayList<String> checkedFiles = adapter[0].getCheckedFiles();
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
                                                    EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                                                    int i = EncryptorService.getUniqueID();
                                                    EncryptorService.getPaths().put(i, checkedFiles);
                                                    intent.putExtra(INDEX, i);
                                                    intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                    ContextCompat.startForegroundService(requireContext(), intent);
                                                    Snackbar.make(requireView(), R.string.encStarted, Snackbar.LENGTH_LONG).show();
                                                    adapter[0].deselectAll();
                                                    adapter[0].closeBottomBar();
                                                    showAddButton(true);
                                                });
                                                dialogBuilder.setNegativeButton(R.string.cancel, (dialog14, which14) -> {
                                                });
                                                dialogBuilder.show();
                                            } else {
                                                Intent intent = new Intent(requireContext(), EncryptorService.class);
                                                intent.putExtra(ACTIONTYPE, "E");
                                                EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                                                int i = EncryptorService.getUniqueID();
                                                EncryptorService.getPaths().put(i, checkedFiles);
                                                intent.putExtra(INDEX, i);
                                                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                ContextCompat.startForegroundService(requireContext(), intent);
                                                Snackbar.make(requireView(), R.string.encStarted, Snackbar.LENGTH_LONG).show();
                                                adapter[0].deselectAll();
                                                adapter[0].closeBottomBar();
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
                                                    EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                                                    int i = EncryptorService.getUniqueID();
                                                    EncryptorService.getPaths().put(i, checkedFiles);
                                                    intent.putExtra(INDEX, i);
                                                    intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                    ContextCompat.startForegroundService(requireContext(), intent);
                                                    Snackbar.make(requireView(), R.string.decStarted, Snackbar.LENGTH_LONG).show();
                                                    adapter[0].deselectAll();
                                                    adapter[0].closeBottomBar();
                                                    showAddButton(true);
                                                });
                                                dialogBuilder.setNegativeButton(R.string.cancel, (dialog12, which12) -> {
                                                });
                                                dialogBuilder.show();
                                            } else {
                                                Intent intent = new Intent(requireContext(), EncryptorService.class);
                                                EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                                                int i = EncryptorService.getUniqueID();
                                                EncryptorService.getPaths().put(i, checkedFiles);
                                                intent.putExtra(INDEX, i);
                                                intent.putExtra(ACTIONTYPE, "D");
                                                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                                                ContextCompat.startForegroundService(requireContext(), intent);
                                                Snackbar.make(requireView(), R.string.decStarted, Snackbar.LENGTH_LONG).show();
                                                adapter[0].deselectAll();
                                                adapter[0].closeBottomBar();
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
                    copyFiles(false, adapter[0], toolbar, searchButton, confirm, cancel);
                    break;
                case R.id.action_moveFiles:
                    copyFiles(true, adapter[0], toolbar, searchButton, confirm, cancel);
                    break;
                default:
                    break;
            }
            return false;
        });
        changeStorage = requireActivity().findViewById(R.id.sdCardButton);
        changeStorageListener = v -> {
            if (((Explorer) requireActivity()).getCurrentOperationNumber() == 0) {
                if (storagePaths.size() > 1) {
                    String path;
                    if (pathIterator.hasNext()) {
                        path = pathIterator.next();
                        if (new File(path).canWrite()) {
                            if (path.equals(requireContext().getFilesDir().getPath())) {
                                viewModel.setCurrentStorageName(getString(R.string.privateFolder));
                                Snackbar.make(v, getString(R.string.swPrivate), Snackbar.LENGTH_LONG).show();
                            } else {
                                viewModel.setCurrentStorageName(getString(R.string.extStorage) + " " + (pathIterator.previousIndex()));
                                Snackbar.make(v, getString(R.string.swExt) + " " + (pathIterator.previousIndex()), Snackbar.LENGTH_LONG).show();
                            }
                            viewModel.setCurrentStoragePath(path);
                            viewModel.calculateFreeSpace(path);
                        } else {
                            changeStorageListener.onClick(v);
                        }
                    } else {
                        while (pathIterator.hasPrevious()) {
                            pathIterator.previous();
                        }
                        path = pathIterator.next();
                        viewModel.setCurrentStorageName(getString(R.string.intStorage));
                        viewModel.setCurrentStoragePath(path);
                        viewModel.calculateFreeSpace(path);
                        Snackbar.make(v, getString(R.string.swInt), Snackbar.LENGTH_LONG).show();
                    }
                    File parent = new File(path);
                    if (parent.canWrite() && ((Explorer) requireActivity()).getCurrentOperationNumber() == 0) {
                        updateUI(adapter[0], fileView, parent);
                    }
                }
            }
        };
        changeStorage.setOnClickListener(changeStorageListener);
        upFolderAction = v -> {
            if (((Explorer) requireActivity()).getCurrentOperationNumber() == 0) {
                fileView.stopScroll();
                String path = viewModel.getPath().getValue();
                File parent = new File(path).getParentFile();
                boolean matches = false;
                for (int i = 0; i < storagePaths.size(); i++) {
                    if (path.matches(Pattern.quote(storagePaths.get(i)))) matches = true;
                }
                if (matches) {
                    ((Explorer) requireActivity()).incrementBackPressedCount();
                    Snackbar snackbar = Snackbar.make(v, R.string.pressToExit, Snackbar.LENGTH_LONG);
                    snackbar.setAction(getString(R.string.swSt), v1 -> {
                        try {
                            changeStorage.performClick();
                        } catch (Exception ignored) {
                        }
                    });
                    snackbar.show();
                } else {
                    updateUI(adapter[0], fileView, parent);
                }
            }
        };
        newFolder = requireActivity().findViewById(R.id.addNewFolder);
        newFolderListener = v -> {
            if (((Explorer) requireActivity()).getCurrentOperationNumber() == 0) {
                final EditText input = new EditText(requireContext());
                Typeface ubuntu = ResourcesCompat.getFont(requireContext(), R.font.ubuntu);
                input.setTypeface(ubuntu);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setSingleLine(true);
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
                    } else if (adapter[0].getLocalDataSet().contains(newName)) {
                        Snackbar.make(v12, R.string.folderExistsErr, Snackbar.LENGTH_LONG).show();
                    } else {
                        Thread thread = new Thread(() -> {
                            try {
                                File testing = new File(viewModel.getPath().getValue() + File.separator + newName);
                                testing.mkdirs();
                                requireActivity().runOnUiThread(() -> {
                                    dialog.dismiss();
                                    ArrayList<String> currentSet = adapter[0].getLocalDataSet();
                                    currentSet.add(newName);
                                    fileList = currentSet;
                                    adapter[0].setNewData(viewModel.getPath().getValue(), fileList);
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
            if (((Explorer) requireActivity()).getCurrentOperationNumber() == 0) {
                String fileName = ((Explorer) requireActivity()).getSearchBar().getText().toString();
                if (!fileName.matches("")) {
                    ((Explorer) requireActivity()).setCurrentOperationNumber(((Explorer) requireActivity()).getCurrentOperationNumber() + 1);
                    searchButton.setEnabled(false);
                    newFolder.setEnabled(false);
                    changeStorage.setEnabled(false);
                    adapter[0].isSearching = true;
                    if (((Explorer) requireActivity()).getSearchBar() != null) {
                        toolbar.removeView(((Explorer) requireActivity()).getSearchBar());
                        ((Explorer) requireActivity()).setSearchBar(null);
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
                    fadeOut(search[0]);
                    fadeOut(bar[0]);
                    String path = viewModel.getPath().getValue();
                    fileView.suppressLayout(true);
                    Thread thread = new Thread(() -> {
                        boolean hasResults = viewModel.searchFile(path, fileName);
                        if (!hasResults) {
                            requireActivity().runOnUiThread(() -> {
                                try {
                                    fileView.suppressLayout(false);
                                    fileView.startAnimation(fadeOut);
                                    fadeIn(search[0]);
                                    fadeIn(bar[0]);
                                    Snackbar.make(requireView(), R.string.noResults, Snackbar.LENGTH_LONG).show();
                                } catch (Exception ignored) {
                                }
                            });
                        } else {
                            requireActivity().runOnUiThread(() -> {
                                fadeIn(search[0]);
                                fadeIn(bar[0]);
                            });
                        }
                        ((Explorer) requireActivity()).setCurrentOperationNumber(((Explorer) requireActivity()).getCurrentOperationNumber() - 1);
                        requireActivity().runOnUiThread(() -> {
                            search[0].setVisibility(View.INVISIBLE);
                            bar[0].setVisibility(View.INVISIBLE);
                            adapter[0].isSearching = false;
                            adapter[0].setSearchEnded();
                            searchButton.setEnabled(true);
                            newFolder.setEnabled(true);
                            changeStorage.setEnabled(true);
                            if (hasResults) {
                                newFolder.setImageDrawable(finalCancelDrawable);
                                newFolder.setOnClickListener(v13 -> {
                                    File parent = new File(viewModel.getPath().getValue());
                                    updateUI(adapter[0], fileView, parent);
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
            }
        };
        searchButton.setOnClickListener(v -> {
            if (((Explorer) requireActivity()).getSearchBar() == null) {
                EditText layout = new EditText(requireContext());
                Typeface ubuntu = ResourcesCompat.getFont(requireContext(), R.font.ubuntu);
                layout.setTypeface(ubuntu);
                Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                layoutParams.gravity = Gravity.START;
                layout.setLayoutParams(l3);
                layout.setTextColor(Color.parseColor("#FFFFFF"));
                layout.setHint(R.string.enterFileNameSearch);
                layout.setSingleLine(true);
                toolbar.addView(layout, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                layout.setFocusableInTouchMode(true);
                layout.setOnKeyListener((v14, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        searchListener.onClick(v14);
                        return true;
                    }
                    return false;
                });
                layout.requestFocus();
                final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(layout, InputMethodManager.SHOW_IMPLICIT);
                ((Explorer) requireActivity()).setSearchBar(layout);
            } else {
                searchListener.onClick(v);
            }
        });
    }

    protected void cancelSearch() {
        newFolder.setOnClickListener(newFolderListener);
        newFolder.setImageDrawable(createDrawable);
    }

    protected void updateUI(ExplorerAdapter adapter, RecyclerView fileView, File parent) {
        if (((Explorer) requireActivity()).getCurrentOperationNumber() == 0) {
            if (adapter.getSearchEnded()) cancelSearch();
            fileView.stopScroll();
            ((Explorer) requireActivity()).setCurrentOperationNumber(((Explorer) requireActivity()).getCurrentOperationNumber() + 1);
            Animation fadeIn1 = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_out_right);
            fadeIn1.setDuration(200);
            fadeIn1.setFillAfter(true);
            fileView.startAnimation(fadeIn1);
            fileView.suppressLayout(true);
            Thread thread1 = new Thread(() -> viewModel.getFileNames(parent));
            thread1.start();
        }
    }

    protected void showAddButton(boolean show) {
        if (show) {
            fadeOut(newFolder);
            fadeOut(changeStorage);
        } else {
            fadeIn(newFolder);
            fadeIn(changeStorage);
        }
    }

    protected int getAddButtonState() {
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
        } catch (Exception ignored) {
        }
    }

    private void copyFiles(boolean cut, ExplorerAdapter adapter, Toolbar t, ImageButton b1, FloatingActionButton confirm, FloatingActionButton cancel) {
        ArrayList<String> checkedFiles2 = adapter.getCheckedFiles();
        String originalPath = viewModel.getPath().getValue();
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
                String path = viewModel.getPath().getValue();
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
                                tempArrayList.addAll(viewModel.getSubFiles(file));
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
                        EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                        int i = EncryptorService.getUniqueID();
                        EncryptorService.getPaths().put(i, checkedFiles2);
                        EncryptorService.getPath().put(i, path);
                        EncryptorService.getOriginalPath().put(i, originalPath);
                        EncryptorService.getFolderReplacements().put(i, folderRenamings);
                        if (!cut) intent.putExtra(ACTIONTYPE, "copyFiles");
                        else {
                            intent.putExtra(ACTIONTYPE, "moveFiles");
                            EncryptorService.getOriginalPaths().put(i, originalSelected);
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
                    .setMessage(getString(R.string.folder) + " " + name + " " + getString(R.string.alreadyExistsSubStr) + " " + substring + getString(R.string.replaceFolderEnding))
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
                        tempArrayLists.addAll(viewModel.getSubFiles(file));
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
                    .setMessage(getString(R.string.file) + " " + fileName + " " + getString(R.string.alreadyExistsSubStr) + " " + folderName + getString(R.string.replaceFileEnding))
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


    public static String fitString(TextView text, String newText) {
        try {
            float textWidth = text.getPaint().measureText(newText);
            int startIndex = 1;
            while (textWidth >= text.getMeasuredWidth()) {
                newText = newText.substring(startIndex);
                textWidth = text.getPaint().measureText(newText);
                startIndex++;
            }
        } catch (Exception ignored) {
        }
        return newText;
    }

    private void setStoragePath(String path) {
        String pathToShow = path.replace(viewModel.getCurrentStoragePath().getValue(), viewModel.getCurrentStorageName().getValue());
        pathToShow = fitString(storagePath, pathToShow);
        storagePath.setText(pathToShow);
    }

    @Override
    public void onDestroyView() {
        ((Explorer) requireActivity()).setExplorerVisible(false);
        super.onDestroyView();
    }

    private void showHints(RecyclerView fileView, Toolbar t) {
        final int[] targetNum = {0};
        Typeface ubuntu = ResourcesCompat.getFont(requireContext(), R.font.ubuntu);
        new TapTargetSequence(requireActivity()).targets(
                getTapTarget(newFolder, getString(R.string.explorerHintTitle1), getString(R.string.explorerHintMessage1), ubuntu),
                getTapTarget(changeStorage, getString(R.string.explorerHintTitle2), getString(R.string.explorerHintMessage2), ubuntu),
                getTapTarget(searchButton, getString(R.string.explorerHintTitle3), getString(R.string.explorerHintMessage3), ubuntu),
                getTapTarget(requireActivity().findViewById(R.id.hintDummy1), getString(R.string.explorerHintTitle4), getString(R.string.explorerHintMessage4), ubuntu),
                getTapTarget(requireActivity().findViewById(R.id.action_encryptFiles), getString(R.string.explorerHintTitle5), getString(R.string.explorerHintMessage5), ubuntu),
                getTapTarget(changeStorage, getString(R.string.explorerHintTitle7), getString(R.string.explorerHintMessage7), ubuntu),
                TapTarget.forToolbarNavigationIcon(t,getString(R.string.explorerHintTitle6), getString(R.string.explorerHintMessage6)).id(1)
                        .cancelable(false)
                        .outerCircleColor(R.color.navTextLight)
                        .outerCircleAlpha(0.9f)
                        .targetCircleColor(R.color.dialogTitleDark)
                        .titleTextSize(20)
                        .descriptionTextSize(15)
                        .textColor(R.color.navLight)
                        .textTypeface(ubuntu)
                        .transparentTarget(true)
                        .dimColor(R.color.navDark)
        ).listener(new TapTargetSequence.Listener() {
            @Override
            public void onSequenceFinish() {
                SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
                preferences.putBoolean("explorerTutorialComplete", true);
                preferences.apply();
            }

            @Override
            public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                targetNum[0]++;
                if(targetNum[0] == 4) {
                    ((ExplorerAdapter.ViewHolder)fileView.findViewHolderForAdapterPosition(0)).checkBoxButton.performClick();
                } else if(targetNum[0] == 5){
                    ((ExplorerAdapter.ViewHolder)fileView.findViewHolderForAdapterPosition(0)).checkBoxButton.performClick();
                }
            }

            @Override
            public void onSequenceCanceled(TapTarget lastTarget) {

            }
        }).start();
    }

    public static TapTarget getTapTarget(View view, String title, String message, Typeface ubuntu) {
        return TapTarget.forView(view, title, message)
                .cancelable(false)
                .outerCircleColor(R.color.navTextLight)
                .outerCircleAlpha(0.9f)
                .targetCircleColor(R.color.dialogTitleDark)
                .titleTextSize(20)
                .descriptionTextSize(15)
                .textColor(R.color.navLight)
                .textTypeface(ubuntu)
                .transparentTarget(true)
                .dimColor(R.color.navDark);
    }
}