package com.suslanium.encryptor;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.text.SimpleDateFormat;

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
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.suslanium.encryptor.ui.explorer.ExplorerFragment.sortFiles;

public class GDriveUploadSelectorAdapter extends RecyclerView.Adapter<GDriveUploadSelectorAdapter.ViewHolder> {
    private ArrayList<String> localDataSet;
    private String path;
    private RecyclerView Recview;
    private Activity activity;
    private ArrayList<ViewHolder> holders = new ArrayList<>();
    private ArrayList<String> CheckedId = new ArrayList<>();
    private SimpleDateFormat format;
    private Date date;
    private int thumbnailLoadingCount = 0;
    private ExecutorService service;
    public boolean isSearching = false;
    private boolean showPreviews = true;
    private String B = "B";
    private String KB = "KB";
    private String MB = "MB";
    private String GB = "GB";
    private String TB = "TB";
    private String Calc = "Calculating...";
    private String items = "items";
    private ColorStateList defTint;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final Button fileButton;
        private final ImageView fileImage;
        private final CheckBox fileCheckbox;
        private final Button checkBoxButton;
        private final TextView dateView;
        private final TextView sizeView;
        private final ImageView isEncrypted;
        public String realPath;
        public boolean encrypted;
        public int loadingCount = 0;

        //TODO: add progress bar on encryption/decryption
        //TODO: add option to store encrypted/decrypted files in separate folder(ex. /storage/emulated/0/EncryptedFiles/)(на будущее)
        //TODO: checkbox multiple encryption/decryption
        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            fileImage = (ImageView) view.findViewById(R.id.fileImage);
            fileButton = (Button) view.findViewById(R.id.fileButton);
            fileCheckbox = (CheckBox) view.findViewById(R.id.fileCheckbox);
            checkBoxButton = (Button) view.findViewById(R.id.checkBoxButton);
            dateView = (TextView) view.findViewById(R.id.modDate);
            sizeView = (TextView) view.findViewById(R.id.fileSize);
            isEncrypted = (ImageView) view.findViewById(R.id.isEncrypted);
            checkBoxButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isSearching) {
                        if (fileCheckbox.isChecked()) {
                            fileCheckbox.setChecked(false);
                            CheckedId.remove(textView.getText().toString());
                        } else {
                            fileCheckbox.setChecked(true);
                            CheckedId.add(textView.getText().toString());
                        }
                    }
                }
            });
            checkBoxButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    selectAll();
                    return true;
                }
            });
            fileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
                                    Thread thread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            File[] files = new File(path + File.separator + realPath).listFiles();
                                            //if(files != null){}
                                            ArrayList<String> paths = new ArrayList<>();
                                            for (int i = 0; i < files.length; i++) {
                                                paths.add(files[i].getPath());
                                            }
                                            List<String> sorted = sortFiles(paths);
                                            ArrayList<File> filesSorted = new ArrayList<>();
                                            for (int i = 0; i < sorted.size(); i++) {
                                                File toAdd = new File(sorted.get(i));
                                                if ((((GoogleDriveUploadSelector) activity).showHiddenFiles && toAdd.getName().startsWith(".")) || !toAdd.getName().startsWith(".")) {
                                                    filesSorted.add(toAdd);
                                                }
                                            }
                                            ArrayList<String> fileNames = new ArrayList<>();
                                            for (int i = 0; i < filesSorted.size(); i++) {
                                                fileNames.add(filesSorted.get(i).getName());
                                            }
                                            for (int i = 0; i < holders.size(); i++) {
                                                holders.get(i).fileCheckbox.setChecked(false);
                                            }
                                            CheckedId.clear();
                                            holders.clear();
                                            int position = getAdapterPosition();
                                            localDataSet = fileNames;
                                            path = path + File.separator + realPath;
                                            //new ExplorerAdapter(fileNames, path + File.separator + textView.getText(), Recview, activity);
                                            while (!fadeIn.hasEnded()) {
                                                try {
                                                    Thread.sleep(10);
                                                } catch (InterruptedException e) {

                                                }
                                            }
                                            Animation fadeOut = AnimationUtils.loadAnimation(activity.getBaseContext(), android.R.anim.slide_in_left);
                                            fadeOut.setDuration(200);
                                            if (((GoogleDriveUploadSelector) activity).searchEnded) {
                                                ((GoogleDriveUploadSelector) activity).searchEnded = false;
                                            }
                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((GoogleDriveUploadSelector) activity).setStoragePath(path);
                                                    notifyDataSetChanged();
                                                    Recview.scrollToPosition(0);
                                                    Recview.startAnimation(fadeOut);
                                                }
                                            });
                                            while (!fadeOut.hasEnded()) {
                                                try {
                                                    Thread.sleep(10);
                                                } catch (InterruptedException e) {

                                                }
                                            }
                                            ((GoogleDriveUploadSelector) activity).currentOperationNumber--;
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
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
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
                                        }
                                    })
                                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            builder.show();
                        }
                        //Move to next folder if clicked element is folder
                    }
                }
            });
            textView = (TextView) view.findViewById(R.id.fileName);
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

        public ImageView getIsEncrypted() {
            return isEncrypted;
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

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet String[] containing the data to populate views to be used
     *                by RecyclerView.
     */
    public GDriveUploadSelectorAdapter(ArrayList<String> dataSet, String path, RecyclerView view, Activity activity) {
        localDataSet = dataSet;
        this.path = path;
        Recview = view;
        this.activity = activity;
        service = Executors.newCachedThreadPool();
        format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        format.setTimeZone(TimeZone.getDefault());
        date = new Date();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        showPreviews = preferences.getBoolean("showPreviews", true);
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
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.viewholder_folder, viewGroup, false);

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

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        holders.add(viewHolder);
        viewHolder.setFile(android.R.drawable.list_selector_background);
        if (!CheckedId.contains(localDataSet.get(position))) {
            viewHolder.fileCheckbox.setChecked(false);
        } else {
            viewHolder.fileCheckbox.setChecked(true);
        }
        String name;
        if (localDataSet.get(position).contains(File.separator)) {
            name = localDataSet.get(position).substring(localDataSet.get(position).lastIndexOf(File.separator) + 1);
            viewHolder.getTextView().setText(name);
            viewHolder.realPath = localDataSet.get(position);
        } else {
            name = localDataSet.get(position);
            viewHolder.getTextView().setText(name);
            viewHolder.realPath = name;
        }
        if (name.startsWith(".")) {
            viewHolder.setFileImageAlpha(0.5f);
        } else {
            viewHolder.setFileImageAlpha(1f);
        }
        service.submit(() -> {
            // Get element from your dataset at this position and replace the
            // contents of the view with that element
            File file = new File(path + File.separator + localDataSet.get(position));
            long lastModified = file.lastModified();
            date.setTime(lastModified);
            String formattedDate = format.format(date);
            activity.runOnUiThread(() -> viewHolder.getDateView().setText(formattedDate));
            if (file.getName().endsWith(".enc") || file.getName().endsWith("Enc")) {
                activity.runOnUiThread(() -> {
                    viewHolder.getIsEncrypted().setVisibility(View.VISIBLE);
                    viewHolder.encrypted = true;
                });
            } else {
                activity.runOnUiThread(() -> {
                    viewHolder.getIsEncrypted().setVisibility(View.INVISIBLE);
                    viewHolder.encrypted = false;
                });
            }
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
                        //EXE
                        viewHolder.setFile(R.drawable.exefile);
                    } else if (type.contains("vnd.android.package-archive")) {
                        viewHolder.setFile(R.drawable.apk);
                    } else if (type.contains("powerpoint") || type.contains("presentation")) {
                        viewHolder.setFile(R.drawable.presentation);
                    } else if (type.contains("msword") || type.contains("document") || type.contains("pdf") || type.contains("rtf") || type.contains("excel") || type.contains("sheet")) {
                        //DOCX
                        viewHolder.setFile(R.drawable.rtf);
                    } else if (type.contains("rar") || type.contains("zip") || type.contains("7z")) {
                        viewHolder.setFile(R.drawable.zipfile);
                    } else {
                        viewHolder.setFile(R.drawable.ic_file);
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
                            //B
                            break;
                        case 1:
                            viewHolder.getSizeView().setText(finalLength + " " + KB);
                            //KB
                            break;
                        case 2:
                            viewHolder.getSizeView().setText(finalLength + " " + MB);
                            //MB
                            break;
                        case 3:
                            viewHolder.getSizeView().setText(finalLength + " " + GB);
                            //GB
                            break;
                        case 4:
                            viewHolder.getSizeView().setText(finalLength + " " + TB);
                            //TB
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
                            if (viewHolder.getTextView().getText().toString().equals(file.getName())) {
                                activity.runOnUiThread(() -> {
                                    viewHolder.setTint(null);
                                    viewHolder.setBitMap(thumbnail);
                                });
                            }
                            thumbnailLoadingCount--;
                        } catch (Exception e) {

                            thumbnailLoadingCount--;
                        }
                    }
                }
            } else {
                activity.runOnUiThread(() -> {
                    viewHolder.setTint(defTint);
                    viewHolder.setFile(R.drawable.ic_folder);
                    viewHolder.getSizeView().setText(Calc);
                });
                int itemCount = file.list() != null ? file.list().length : 0;
                activity.runOnUiThread(() -> viewHolder.getSizeView().setText(itemCount + " " + items));
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

    public HashMap<Integer, String> getCheckedNames() {
        HashMap<Integer, String> files = new HashMap<>();
        for (int i = 0; i < holders.size(); i++) {
            if (holders.get(i).fileCheckbox.isChecked()) {
                files.put(holders.get(i).getAdapterPosition(), (holders.get(i).getTextView().getText()).toString());
            }
        }
        return files;
    }

    public String getPath() {
        return path;
    }

    // Return the size of your dataset (invoked by the layout manager)
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
