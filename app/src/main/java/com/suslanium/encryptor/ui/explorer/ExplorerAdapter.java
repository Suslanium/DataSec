package com.suslanium.encryptor.ui.explorer;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.text.SimpleDateFormat;

import android.graphics.Typeface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.suslanium.encryptor.EncryptorService;
import com.suslanium.encryptor.ui.Explorer;
import com.suslanium.encryptor.R;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class ExplorerAdapter extends RecyclerView.Adapter<ExplorerAdapter.ViewHolder> {
    private ArrayList<String> localDataSet;
    private String path;
    private final RecyclerView recyclerView;
    private final Activity activity;
    private final ArrayList<ViewHolder> holders = new ArrayList<>();
    private final ArrayList<String> CheckedId = new ArrayList<>();
    protected boolean isSearching = false;
    private int thumbnailLoadingCount = 0;
    private final ExecutorService service;
    private final BottomNavigationView bottomBar;
    private boolean isDoingFileOperations = false;
    private final ArrayList<String> deletedFilePaths = new ArrayList<>();
    private final ExplorerFragment fragment;
    private static final String ACTIONTYPE = "actionType";
    private static final String INDEX = "index";
    private boolean searchEnded = false;
    private final boolean showPreviews;
    private boolean canSelect = true;
    private final String B;
    private final String KB;
    private final String MB;
    private final String GB;
    private final String TB;
    private final String Calc;
    private final String items;
    private final ColorStateList defTint;
    private final Set<String> favorites = new HashSet<>();
    private final ExplorerViewModel viewModel;
    private final AsyncLayoutInflater.OnInflateFinishedListener listener;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;
        private ImageView fileImage;
        private CheckBox fileCheckbox;
        private TextView dateView;
        private TextView sizeView;
        private final View parentView;
        private boolean encrypted = false;
        private String realPath;
        private int loadingCount = 0;
        protected Button checkBoxButton;

        public ViewHolder(View view) {
            super(view);
            parentView = view;
        }

        private TextView getTextView() {
            return textView;
        }

        private void setupHolder(View view) {
            fileImage = view.findViewById(R.id.fileImage);
            Button fileButton = view.findViewById(R.id.fileButton);
            fileCheckbox = view.findViewById(R.id.fileCheckbox);
            checkBoxButton = view.findViewById(R.id.checkBoxButton);
            dateView = view.findViewById(R.id.modDate);
            sizeView = view.findViewById(R.id.fileSize);
            checkBoxButton.setOnClickListener(v -> {
                String filePath = path + File.separator + realPath;
                if (!deletedFilePaths.contains(filePath)) {
                    if (!isDoingFileOperations) {
                        if (canSelect) {
                            if (fileCheckbox.isChecked()) {
                                fileCheckbox.setChecked(false);
                                CheckedId.remove(realPath);
                                closeBottomBar();
                                if (!isDoingFileOperations && fragment.getAddButtonState() == View.GONE && CheckedId.isEmpty()) {
                                    fragment.showAddButton(true);
                                }
                            } else {
                                openBottomBar();
                                fileCheckbox.setChecked(true);
                                CheckedId.add(realPath);
                            }
                        }
                    }
                }
            });
            checkBoxButton.setOnLongClickListener(v -> {
                selectAll();
                return true;
            });
            fileButton.setOnLongClickListener(v -> {
                if (!isSearching) {
                    String filePath = path + File.separator + realPath;
                    if (!deletedFilePaths.contains(filePath)) {
                        File file = new File(filePath);
                        if ((file.isDirectory()) || (file.isFile() && !isDoingFileOperations)) {
                            final EditText input = new EditText(fragment.requireContext());
                            Typeface ubuntu = ResourcesCompat.getFont(fragment.requireContext(), R.font.ubuntu);
                            input.setTypeface(ubuntu);
                            input.setInputType(InputType.TYPE_CLASS_TEXT);
                            input.setSingleLine(true);
                            input.setText(file.getName());
                            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(fileImage.getContext(), R.style.MaterialAlertDialog_rounded)
                                    .setTitle(R.string.renameFileFolder)
                                    .setView(input)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.rename, (dialog, which) -> {
                                    })
                                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                                String newName = input.getText().toString();
                                String prevName = file.getName();
                                if (newName.matches("")) {
                                    Snackbar.make(v1, R.string.enterAdapterNameErr, Snackbar.LENGTH_LONG).show();
                                } else if (newName.contains(File.separator)) {
                                    Snackbar.make(v1, R.string.enterValidNameErr, Snackbar.LENGTH_LONG).show();
                                } else if (newName.matches(Pattern.quote(prevName))) {
                                    Snackbar.make(v1, R.string.enterNewName, Snackbar.LENGTH_LONG).show();
                                } else if (localDataSet.contains(newName)) {
                                    Snackbar.make(v1, R.string.folderExistsErr, Snackbar.LENGTH_LONG).show();
                                } else {
                                    Thread thread = new Thread(() -> {
                                        try {
                                            String withoutName = filePath.substring(0, filePath.lastIndexOf(File.separator) + 1);
                                            File testing = new File(withoutName + newName);
                                            testing.createNewFile();
                                            testing.delete();
                                            file.renameTo(testing);
                                            activity.runOnUiThread(() -> {
                                                dialog.dismiss();
                                                textView.setText(newName);
                                                for (int i = 0; i < localDataSet.size(); i++) {
                                                    if (localDataSet.get(i).matches(Pattern.quote(realPath))) {
                                                        realPath = testing.getPath().replace(path + File.separator, "");
                                                        localDataSet.set(i, realPath);
                                                    }
                                                }

                                            });
                                        } catch (Exception e) {
                                            activity.runOnUiThread(() -> Snackbar.make(v1, R.string.enterValidNameErr, Snackbar.LENGTH_LONG).show());
                                        }
                                    });
                                    thread.start();
                                }
                            });
                        }
                    } else {
                        Snackbar.make(v, R.string.accessDeniedDelete, Snackbar.LENGTH_LONG).show();
                    }
                }
                return true;
            });
            fileButton.setOnClickListener(v -> {
                if (!isSearching) {
                    String filePath = path + File.separator + realPath;
                    if (!deletedFilePaths.contains(filePath)) {
                        if (new File(filePath).isDirectory()) {
                            if (new File(filePath).canWrite()) {
                                if (((Explorer) activity).getCurrentOperationNumber() == 0) {
                                    recyclerView.stopScroll();
                                    ((Explorer) activity).setCurrentOperationNumber(((Explorer) activity).getCurrentOperationNumber() + 1);
                                    Animation fadeIn = AnimationUtils.loadAnimation(activity.getBaseContext(), android.R.anim.slide_out_right);
                                    fadeIn.setDuration(200);
                                    fadeIn.setFillAfter(true);
                                    recyclerView.startAnimation(fadeIn);
                                    recyclerView.suppressLayout(true);
                                    Thread thread = new Thread(() -> {
                                        viewModel.getFileNames(new File(filePath));
                                        if (searchEnded) {
                                            activity.runOnUiThread(fragment::cancelSearch);
                                            searchEnded = false;
                                        }
                                        if (!isDoingFileOperations && fragment.getAddButtonState() == View.GONE) {
                                            activity.runOnUiThread(() -> fragment.showAddButton(true));
                                        }
                                    });
                                    thread.start();
                                }
                            } else {
                                Snackbar.make(v, R.string.accessDenied, Snackbar.LENGTH_LONG).show();
                            }
                        } else {
                            if (!isDoingFileOperations) {
                                CharSequence[] items;
                                if (encrypted) {
                                    if (!favorites.contains(filePath)) {
                                        items = new CharSequence[]{activity.getString(R.string.decryptFile), activity.getString(R.string.openFile), activity.getString(R.string.addToFav)};
                                    } else {
                                        items = new CharSequence[]{activity.getString(R.string.decryptFile), activity.getString(R.string.openFile), activity.getString(R.string.rmFromFav)};
                                    }
                                } else {
                                    if (!favorites.contains(filePath)) {
                                        items = new CharSequence[]{activity.getString(R.string.encryptFile), activity.getString(R.string.openFile), activity.getString(R.string.addToFav)};
                                    } else {
                                        items = new CharSequence[]{activity.getString(R.string.encryptFile), activity.getString(R.string.openFile), activity.getString(R.string.rmFromFav)};
                                    }
                                }
                                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(fileImage.getContext(), R.style.MaterialAlertDialog_rounded);
                                builder.setTitle(R.string.choose);
                                builder.setItems(items, (dialog, which) -> {
                                    switch (which) {
                                        case 0:
                                            if (!encrypted) {
                                                if (new File(filePath + ".enc").exists()) {
                                                    MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(fileImage.getContext(), R.style.MaterialAlertDialog_rounded);
                                                    dialogBuilder.setTitle(R.string.warning);
                                                    dialogBuilder.setMessage(R.string.encFileExists);
                                                    dialogBuilder.setPositiveButton(R.string.yes, (dialog1, which1) -> {
                                                        Snackbar.make(v, R.string.encStarted, Snackbar.LENGTH_LONG).show();
                                                        ArrayList<String> paths = new ArrayList<>();
                                                        paths.add(filePath);
                                                        Intent intent = new Intent(activity.getBaseContext(), EncryptorService.class);
                                                        intent.putExtra(ACTIONTYPE, "E");
                                                        EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                                                        int i = EncryptorService.getUniqueID();
                                                        EncryptorService.getPaths().put(i, paths);
                                                        intent.putExtra(INDEX, i);
                                                        intent.putExtra("pass", ((Explorer) activity).getIntent2().getByteArrayExtra("pass"));
                                                        ContextCompat.startForegroundService(activity.getBaseContext(), intent);
                                                    });
                                                    dialogBuilder.setNegativeButton(R.string.no, (dialog12, which12) -> {

                                                    });
                                                    dialogBuilder.show();
                                                } else {
                                                    Snackbar.make(v, R.string.encStarted, Snackbar.LENGTH_LONG).show();
                                                    ArrayList<String> paths = new ArrayList<>();
                                                    paths.add(filePath);
                                                    Intent intent = new Intent(activity.getBaseContext(), EncryptorService.class);
                                                    intent.putExtra(ACTIONTYPE, "E");
                                                    EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                                                    int i = EncryptorService.getUniqueID();
                                                    EncryptorService.getPaths().put(i, paths);
                                                    intent.putExtra(INDEX, i);
                                                    intent.putExtra("pass", ((Explorer) activity).getIntent2().getByteArrayExtra("pass"));
                                                    ContextCompat.startForegroundService(activity.getBaseContext(), intent);
                                                }
                                            } else {
                                                if (new File((filePath).substring(0, (filePath).length() - 4)).exists()) {
                                                    MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(fileImage.getContext(), R.style.MaterialAlertDialog_rounded);
                                                    dialogBuilder.setTitle(R.string.warning);
                                                    dialogBuilder.setMessage(R.string.decFileExists);
                                                    dialogBuilder.setPositiveButton(R.string.yes, (dialog13, which13) -> {
                                                        Snackbar.make(v, R.string.decStarted, Snackbar.LENGTH_LONG).show();
                                                        ArrayList<String> paths = new ArrayList<>();
                                                        paths.add(filePath);
                                                        Intent intent = new Intent(activity.getBaseContext(), EncryptorService.class);
                                                        intent.putExtra(ACTIONTYPE, "D");
                                                        EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                                                        int i = EncryptorService.getUniqueID();
                                                        EncryptorService.getPaths().put(i, paths);
                                                        intent.putExtra(INDEX, i);
                                                        intent.putExtra("pass", ((Explorer) activity).getIntent2().getByteArrayExtra("pass"));
                                                        ContextCompat.startForegroundService(activity.getBaseContext(), intent);
                                                    });
                                                    dialogBuilder.setNegativeButton(R.string.no, (dialog14, which14) -> {
                                                    });
                                                    dialogBuilder.show();
                                                } else {
                                                    Snackbar.make(v, R.string.decStarted, Snackbar.LENGTH_LONG).show();
                                                    ArrayList<String> paths = new ArrayList<>();
                                                    paths.add(filePath);
                                                    Intent intent = new Intent(activity.getBaseContext(), EncryptorService.class);
                                                    intent.putExtra(ACTIONTYPE, "D");
                                                    EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                                                    int i = EncryptorService.getUniqueID();
                                                    EncryptorService.getPaths().put(i, paths);
                                                    intent.putExtra(INDEX, i);
                                                    intent.putExtra("pass", ((Explorer) activity).getIntent2().getByteArrayExtra("pass"));
                                                    ContextCompat.startForegroundService(activity.getBaseContext(), intent);
                                                }
                                            }
                                            break;
                                        case 1:
                                            if (!encrypted) {
                                                try {
                                                    File file = new File(filePath);
                                                    Uri uriForFile = FileProvider.getUriForFile(activity.getBaseContext(), "com.suslanium.encryptor.fileprovider", file);
                                                    String type = activity.getContentResolver().getType(uriForFile);
                                                    Intent intent = new Intent();
                                                    intent.setAction(Intent.ACTION_VIEW);
                                                    intent.setDataAndType(uriForFile, type);
                                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                    activity.startActivity(intent);
                                                } catch (Exception e) {
                                                    Snackbar.make(v, R.string.failedToOpenFile, Snackbar.LENGTH_LONG).show();
                                                }
                                            } else {
                                                File encrypted = new File(filePath);
                                                if (encrypted.length() <= 50 * 1024 * 1024) {
                                                    MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(fragment.requireContext(), R.style.MaterialAlertDialog_rounded);
                                                    ProgressBar bar = new ProgressBar(fragment.requireContext());
                                                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                                    bar.setLayoutParams(lp);
                                                    builder2.setTitle(R.string.wait);
                                                    builder2.setMessage(R.string.decryptionRunning);
                                                    builder2.setView(bar);
                                                    builder2.setCancelable(false);
                                                    builder2.setPositiveButton(R.string.bckgnd, (dialog15, which15) -> dialog15.dismiss());
                                                    AlertDialog alertDialog = builder2.create();
                                                    alertDialog.show();
                                                    File cached = new File((activity.getFilesDir().getPath() + File.separator + ".temp" + File.separator + encrypted.getName()).substring(0, (activity.getFilesDir().getPath() + File.separator + ".temp" + File.separator + encrypted.getName()).length() - 4));
                                                    Thread thread = new Thread(() -> {
                                                        try {
                                                            cached.getParentFile().mkdirs();
                                                            cached.delete();
                                                            viewModel.decryptTemp(encrypted, cached, activity, alertDialog);
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                            cached.delete();
                                                            activity.runOnUiThread(() -> {
                                                                alertDialog.dismiss();
                                                                Snackbar.make(v, R.string.failedToOpenFile, Snackbar.LENGTH_LONG).show();
                                                            });
                                                        }
                                                    });
                                                    thread.start();
                                                } else {
                                                    Snackbar.make(v, R.string.fileTooBig, Snackbar.LENGTH_LONG).show();
                                                }
                                            }
                                            break;
                                        case 2:
                                            SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(fragment.requireContext()).edit();
                                            if (favorites.contains(filePath)) {
                                                favorites.remove(filePath);
                                            } else {
                                                favorites.add(filePath);
                                            }
                                            preferences.putStringSet("fav", favorites);
                                            preferences.apply();
                                            break;
                                        default:
                                            break;
                                    }
                                });
                                builder.show();
                            }
                        }
                    } else {
                        Snackbar.make(v, R.string.accessDeniedDelete, Snackbar.LENGTH_LONG).show();
                    }
                }
            });
            textView = view.findViewById(R.id.fileName);
        }

        private TextView getDateView() {
            return dateView;
        }

        private TextView getSizeView() {
            return sizeView;
        }

        private void setFile(int id) {
            fileImage.setImageResource(id);
        }

        private void setBitMap(Bitmap bitMap) {
            fileImage.setImageBitmap(bitMap);
        }

        private void deselectCheckbox() {
            fileCheckbox.setChecked(false);
        }

        private void setFileImageAlpha(float alpha) {
            fileImage.setAlpha(alpha);
        }

        private void setTint(ColorStateList list) {
            fileImage.setImageTintList(list);
        }
    }

    protected void addAllDeletedFiles(ArrayList<String> paths) {
        deletedFilePaths.addAll(paths);
    }

    protected void removeAllDeletedFiles(ArrayList<String> paths) {
        deletedFilePaths.removeAll(paths);
    }

    protected void setSearchEnded() {
        searchEnded = true;
    }

    protected void closeBottomBar() {
        if (CheckedId.isEmpty() && bottomBar.getVisibility() == View.VISIBLE) {
            canSelect = false;
            bottomBar.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            canSelect = true;
                            bottomBar.setVisibility(View.GONE);
                        }
                    });
        }
    }

    protected void setDoingFileOperations(boolean doingFileOperations) {
        isDoingFileOperations = doingFileOperations;
    }

    protected boolean getDoingFileOperations() {
        return isDoingFileOperations;
    }

    protected void openBottomBar() {
        if (CheckedId.isEmpty() && bottomBar.getVisibility() == View.GONE) {
            canSelect = false;
            bottomBar.setVisibility(View.VISIBLE);
            bottomBar.bringToFront();
            bottomBar.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            canSelect = true;
                        }
                    });
            bottomBar.getMenu().setGroupCheckable(0, true, false);
            for (int i = 0; i < bottomBar.getMenu().size(); i++) {
                bottomBar.getMenu().getItem(i).setChecked(false);
            }
            bottomBar.getMenu().setGroupCheckable(0, true, true);
            fragment.showAddButton(false);
        }
    }

    protected boolean getSearchEnded() {
        return searchEnded;
    }

    protected void deselectAll() {
        CheckedId.clear();
        if (!holders.isEmpty()) {
            for (int i = 0; i < holders.size(); i++) {
                holders.get(i).deselectCheckbox();
            }
        }
    }

    public ExplorerAdapter(ArrayList<String> dataSet, String path, RecyclerView view, Activity activity, BottomNavigationView bottomBar, ExplorerFragment fragment, ExplorerViewModel model) {
        localDataSet = dataSet;
        this.path = path;
        recyclerView = view;
        this.activity = activity;
        service = Executors.newCachedThreadPool();
        this.bottomBar = bottomBar;
        this.fragment = fragment;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(fragment.requireContext());
        showPreviews = preferences.getBoolean("showPreviews", false);
        B = activity.getString(R.string.b);
        KB = activity.getString(R.string.kb);
        MB = activity.getString(R.string.mb);
        GB = activity.getString(R.string.gb);
        TB = activity.getString(R.string.tb);
        Calc = activity.getString(R.string.calculating);
        items = activity.getString(R.string.items);
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = activity.getTheme();
        theme.resolveAttribute(R.attr.explorerIconColor, typedValue, true);
        @ColorInt int color = typedValue.data;
        defTint = ColorStateList.valueOf(color);
        favorites.addAll(preferences.getStringSet("fav", new HashSet<>()));
        viewModel = model;
        listener = (view1, resid, parent) -> parent.addView(view1);
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.viewholder_dummy, viewGroup, false);
        AsyncLayoutInflater asyncLayoutInflater = new AsyncLayoutInflater(viewGroup.getContext());
        asyncLayoutInflater.inflate(R.layout.viewholder_explorer, (ViewGroup) view, listener);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NotNull ViewHolder viewHolder, final int position) {
        service.submit(() -> {
            while (viewHolder.parentView.findViewById(R.id.fileName) == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
            if (viewHolder.getTextView() == null)
                activity.runOnUiThread(() -> viewHolder.setupHolder(viewHolder.parentView));
            while (viewHolder.getTextView() == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
            holders.add(viewHolder);
            activity.runOnUiThread(() -> viewHolder.setFile(android.R.drawable.list_selector_background));
            if (!CheckedId.contains(localDataSet.get(position))) {
                activity.runOnUiThread(() -> viewHolder.fileCheckbox.setChecked(false));
            } else {
                activity.runOnUiThread(() -> viewHolder.fileCheckbox.setChecked(true));
            }
            String name;
            if (localDataSet.get(position).contains(File.separator)) {
                name = localDataSet.get(position).substring(localDataSet.get(position).lastIndexOf(File.separator) + 1);
                activity.runOnUiThread(() -> viewHolder.getTextView().setText(name));
                viewHolder.realPath = localDataSet.get(position);
            } else {
                name = localDataSet.get(position);
                activity.runOnUiThread(() -> viewHolder.getTextView().setText(name));
                viewHolder.realPath = name;
            }
            if (name.startsWith(".")) {
                activity.runOnUiThread(() -> viewHolder.setFileImageAlpha(0.5f));
            } else {
                activity.runOnUiThread(() -> viewHolder.setFileImageAlpha(1f));
            }
            File file = new File(path + File.separator + localDataSet.get(position));
            long lastModified = file.lastModified();
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            format.setTimeZone(TimeZone.getDefault());
            String formattedDate = format.format(new Date(lastModified));
            activity.runOnUiThread(() -> {if (viewHolder.getTextView().getText().toString().equals(file.getName()))viewHolder.getDateView().setText(formattedDate);});
            viewHolder.encrypted = file.getName().endsWith(".enc") || file.getName().endsWith("Enc");
            if (file.isFile()) {
                Uri uriForFile = FileProvider.getUriForFile(activity.getBaseContext(), "com.suslanium.encryptor.fileprovider", file);
                String type = activity.getContentResolver().getType(uriForFile);
                activity.runOnUiThread(() -> {
                    viewHolder.setTint(defTint);
                    if (type.contains("text")) {
                        viewHolder.setFile(R.drawable.ic_text);
                    } else if (type.contains("audio")) {
                        viewHolder.setFile(R.drawable.audiofile);
                    } else if (type.contains("image")) {
                        viewHolder.setFile(R.drawable.ic_image);
                    } else if (type.contains("video")) {
                        viewHolder.setFile(R.drawable.ic_movie);
                    } else if (type.contains("x-msdos-program")) {
                        viewHolder.setFile(R.drawable.exefile);
                    } else if (type.contains("vnd.android.package-archive")) {
                        viewHolder.setFile(R.drawable.apk);
                    } else if (type.contains("powerpoint") || type.contains("presentation")) {
                        viewHolder.setFile(R.drawable.presentation);
                    } else if (type.contains("msword") || type.contains("document") || type.contains("pdf") || type.contains("rtf") || type.contains("excel") || type.contains("sheet")) {
                        viewHolder.setFile(R.drawable.rtf);
                    } else if (type.contains("rar") || type.contains("zip") || type.contains("7z")) {
                        viewHolder.setFile(R.drawable.zipfile);
                    } else {
                        if (viewHolder.encrypted) {
                            viewHolder.setFile(R.drawable.encfile);
                        } else {
                            viewHolder.setFile(R.drawable.ic_file);
                        }
                    }
                });
                double length = file.length();
                int unit = 0;
                while (length > 1024) {
                    length = length / 1024;
                    unit++;
                }
                length = (double) Math.round(length * 100) / 100;
                double finalLength = length;
                int finalUnit = unit;
                activity.runOnUiThread(() -> {
                    switch (finalUnit) {
                        case 0:
                            viewHolder.getSizeView().setText(finalLength + " " + B);
                            break;
                        case 1:
                            viewHolder.getSizeView().setText(finalLength + " " + KB);
                            break;
                        case 2:
                            viewHolder.getSizeView().setText(finalLength + " " + MB);
                            break;
                        case 3:
                            viewHolder.getSizeView().setText(finalLength + " " + GB);
                            break;
                        case 4:
                            viewHolder.getSizeView().setText(finalLength + " " + TB);
                            break;
                        default:
                            break;
                    }
                });
                if (type.contains("image") && showPreviews) {
                    viewHolder.loadingCount++;
                    final int loadingNum = viewHolder.loadingCount;
                    while (thumbnailLoadingCount > 0) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {

                            Thread.currentThread().interrupt();
                        }
                    }
                    if (loadingNum == viewHolder.loadingCount) {
                        thumbnailLoadingCount++;
                        try {
                            Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(file.getPath()), 128, 128);
                            activity.runOnUiThread(() -> {
                                if (viewHolder.getTextView().getText().toString().equals(file.getName())) {
                                    viewHolder.setTint(null);
                                    viewHolder.setBitMap(thumbnail);
                                }
                            });
                            thumbnailLoadingCount--;
                        } catch (Exception e) {

                            thumbnailLoadingCount--;
                        }
                    }
                }
            } else {
                viewHolder.loadingCount++;
                final int loadingNum = viewHolder.loadingCount;
                activity.runOnUiThread(() -> {
                    viewHolder.setTint(defTint);
                    if(viewHolder.encrypted){
                        viewHolder.setFile(R.drawable.encfolder);
                    } else {
                        viewHolder.setFile(R.drawable.ic_folder);
                    }
                    viewHolder.getSizeView().setText(Calc);
                });
                int itemCount = file.list() != null ? file.list().length : 0;
                if (loadingNum == viewHolder.loadingCount) {
                    activity.runOnUiThread(() -> viewHolder.getSizeView().setText(itemCount + " " + items));
                }
            }
        });
    }

    protected void selectAll() {
        if (!isDoingFileOperations) {
            if (canSelect) {
                if (!localDataSet.equals(CheckedId)) {
                    if (!holders.isEmpty()) {
                        for (int i = 0; i < holders.size(); i++) {
                            holders.get(i).fileCheckbox.setChecked(true);
                        }
                    }
                    CheckedId.clear();
                    openBottomBar();
                    if (localDataSet != null && !localDataSet.isEmpty()) {
                        CheckedId.addAll(localDataSet);
                    }
                } else {
                    if (!holders.isEmpty()) {
                        for (int i = 0; i < holders.size(); i++) {
                            holders.get(i).fileCheckbox.setChecked(false);
                        }
                    }
                    CheckedId.clear();
                    closeBottomBar();
                    if (!isDoingFileOperations && fragment.getAddButtonState() == View.GONE && CheckedId.isEmpty()) {
                        fragment.showAddButton(true);
                    }
                }
            }
        }
    }

    protected ArrayList<String> getLocalDataSet() {
        return localDataSet;
    }

    protected ArrayList<String> getCheckedFiles() {
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < CheckedId.size(); i++) {
            files.add(path + File.separator + CheckedId.get(i));
        }
        return files;
    }

    protected String getPath() {
        return path;
    }

    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    protected void setNewData(String path, ArrayList<String> fileNames) {
        int position;
        if (holders.size() > 0) position = holders.get(0).getAdapterPosition();
        else position = 0;
        for (int i = 0; i < holders.size(); i++) {
            holders.get(i).fileCheckbox.setChecked(false);
        }
        holders.clear();
        CheckedId.clear();
        closeBottomBar();
        localDataSet = fileNames;
        this.path = path;
        notifyDataSetChanged();
    }

}
