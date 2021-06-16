package com.suslanium.encryptor.ui.gdrive;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.suslanium.encryptor.EncryptorService;
import com.suslanium.encryptor.R;
import com.suslanium.encryptor.ui.explorer.ExplorerViewModel;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GDriveUploadSelectorAdapter extends RecyclerView.Adapter<GDriveUploadSelectorAdapter.ViewHolder> {
    private ArrayList<String> localDataSet;
    private String path;
    private final RecyclerView Recview;
    private final Activity activity;
    private final ArrayList<ViewHolder> holders = new ArrayList<>();
    private final ArrayList<String> CheckedId = new ArrayList<>();
    private int thumbnailLoadingCount = 0;
    private final ExecutorService service;
    public boolean isSearching = false;
    private final boolean showPreviews;
    private final String B;
    private final String KB;
    private final String MB;
    private final String GB;
    private final String TB;
    private final String Calc;
    private final String items;
    private final ColorStateList defTint;
    private final ExplorerViewModel viewModel;
    private final AsyncLayoutInflater.OnInflateFinishedListener listener;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;
        private ImageView fileImage;
        private CheckBox fileCheckbox;
        private TextView dateView;
        private TextView sizeView;
        public String realPath;
        public boolean encrypted;
        public int loadingCount = 0;
        private final View parentView;

        public ViewHolder(View view) {
            super(view);
            parentView = view;
        }

        public void setupHolder(View view) {
            fileImage = view.findViewById(R.id.fileImage);
            Button fileButton = view.findViewById(R.id.fileButton);
            fileCheckbox = view.findViewById(R.id.fileCheckbox);
            Button checkBoxButton = view.findViewById(R.id.checkBoxButton);
            dateView = view.findViewById(R.id.modDate);
            sizeView = view.findViewById(R.id.fileSize);
            checkBoxButton.setOnClickListener(v -> {
                if (!isSearching) {
                    if (fileCheckbox.isChecked()) {
                        fileCheckbox.setChecked(false);
                        CheckedId.remove(textView.getText().toString());
                    } else {
                        fileCheckbox.setChecked(true);
                        CheckedId.add(textView.getText().toString());
                    }
                }
            });
            checkBoxButton.setOnLongClickListener(v -> {
                selectAll();
                return true;
            });
            fileButton.setOnClickListener(v -> {
                if (!isSearching) {
                    if (new File(path + File.separator + realPath).isDirectory()) {
                        if (new File(path + File.separator + realPath).canWrite()) {
                            if (((GoogleDriveUploadSelector) activity).currentOperationNumber == 0) {
                                Recview.stopScroll();
                                ((GoogleDriveUploadSelector) activity).currentOperationNumber++;
                                Animation fadeIn = AnimationUtils.loadAnimation(activity.getBaseContext(), android.R.anim.slide_out_right);
                                fadeIn.setDuration(200);
                                fadeIn.setFillAfter(true);
                                Recview.startAnimation(fadeIn);
                                Recview.suppressLayout(true);
                                Thread thread = new Thread(() -> {
                                    viewModel.getFileNames(new File(path + File.separator + realPath));
                                    if (((GoogleDriveUploadSelector) activity).searchEnded) {
                                        ((GoogleDriveUploadSelector) activity).searchEnded = false;
                                    }
                                });
                                thread.start();
                            }
                        } else {
                            Snackbar.make(v, R.string.accessDenied, Snackbar.LENGTH_LONG).show();
                        }
                    } else {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(fileImage.getContext(), R.style.MaterialAlertDialog_rounded)
                                .setTitle(R.string.confirmAction)
                                .setMessage(R.string.uploadSelectorConfirmText)
                                .setPositiveButton(R.string.yes, (dialog, which) -> {
                                    ArrayList<String> paths = new ArrayList<>();
                                    paths.add(path + File.separator + realPath);
                                    Intent intent = new Intent(activity, EncryptorService.class);
                                    intent.putExtra("actionType", "gDriveE");
                                    EncryptorService.uniqueID++;
                                    int i = EncryptorService.uniqueID;
                                    EncryptorService.paths.put(i, paths);
                                    intent.putExtra("index", i);
                                    intent.putExtra("pass", activity.getIntent().getByteArrayExtra("pass"));
                                    intent.putExtra("gDriveFolder", activity.getIntent().getStringExtra("gDriveFolder"));
                                    ContextCompat.startForegroundService(activity, intent);
                                    activity.finish();
                                })
                                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                        builder.show();
                    }
                }
            });
            textView = view.findViewById(R.id.fileName);
        }

        public TextView getTextView() {
            return textView;
        }

        public TextView getDateView() {
            return dateView;
        }

        public TextView getSizeView() {
            return sizeView;
        }

        public void setFile(int id) {
            fileImage.setImageResource(id);
        }

        public void setFileImageAlpha(float v) {
            fileImage.setAlpha(v);
        }

        public void setBitMap(Bitmap thumbnail) {
            fileImage.setImageBitmap(thumbnail);
        }

        public void setTint(ColorStateList list) {
            fileImage.setImageTintList(list);
        }
    }

    public GDriveUploadSelectorAdapter(ArrayList<String> dataSet, String path, RecyclerView view, Activity activity, ExplorerViewModel viewModel) {
        localDataSet = dataSet;
        this.path = path;
        Recview = view;
        this.activity = activity;
        service = Executors.newCachedThreadPool();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
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
        this.viewModel = viewModel;
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

    public void selectAll() {
        if (((GoogleDriveUploadSelector) activity).currentOperationNumber == 0) {
            if (!localDataSet.equals(CheckedId)) {
                if (!holders.isEmpty()) {
                    for (int i = 0; i < holders.size(); i++) {
                        holders.get(i).fileCheckbox.setChecked(true);
                    }
                }
                CheckedId.clear();
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
            }
        }
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

    public ArrayList<String> getCheckedFiles() {
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < CheckedId.size(); i++) {
            files.add(path + File.separator + CheckedId.get(i));
        }
        return files;
    }

    public String getPath() {
        return path;
    }

    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public void setNewData(String path, ArrayList<String> fileNames) {
        int position;
        if (holders.size() > 0) position = holders.get(0).getAdapterPosition();
        else position = 0;
        for (int i = 0; i < holders.size(); i++) {
            holders.get(i).fileCheckbox.setChecked(false);
        }
        holders.clear();
        CheckedId.clear();
        localDataSet = fileNames;
        this.path = path;
        notifyDataSetChanged();
    }
}
