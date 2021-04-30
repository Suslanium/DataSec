package com.suslanium.encryptor;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.DateFormat;

import java.sql.Time;
import java.text.SimpleDateFormat;

import android.icu.util.Calendar;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.suslanium.encryptor.ui.home.HomeFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static com.suslanium.encryptor.ui.home.HomeFragment.sortFiles;

public class ExplorerAdapter extends RecyclerView.Adapter<ExplorerAdapter.ViewHolder> {
    private ArrayList<String> localDataSet;
    private String path;
    private RecyclerView Recview;
    private Activity activity;
    private ArrayList<ViewHolder> holders = new ArrayList<>();
    private ArrayList<String> CheckedId = new ArrayList<>();
    private SimpleDateFormat format;
    private Date date;
    public boolean isSearching = false;
    private int thumbnailLoadingCount = 0;
    private ExecutorService service;
    private BottomNavigationView bottomBar;
    private boolean isDoingFileOperations = false;
    private ArrayList<String> deletedFilePaths = new ArrayList<>();
    private HomeFragment fragment;

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
        public boolean encrypted = false;
        public String realPath;
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
                    String filePath = path + File.separator + realPath;
                    if (!deletedFilePaths.contains(filePath)) {
                        if (!isDoingFileOperations) {
                            if (fileCheckbox.isChecked()) {
                                fileCheckbox.setChecked(false);
                                CheckedId.remove(realPath);
                                closeBottomBar();
                                if (!isDoingFileOperations && fragment.getAddButtonState() == View.GONE) {
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
            fileButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (!isSearching) {
                        String filePath = path + File.separator + realPath;
                        if (!deletedFilePaths.contains(filePath)) {
                            File file = new File(filePath);
                            if ((file.isDirectory()) || (file.isFile() && !isDoingFileOperations)) {
                                final EditText input = new EditText(activity.getBaseContext());
                                input.setInputType(InputType.TYPE_CLASS_TEXT);
                                input.setSingleLine(true);
                                input.setText(file.getName());
                                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(fileImage.getContext(), R.style.MaterialAlertDialog_rounded)
                                        .setTitle("Rename file/folder")
                                        .setView(input)
                                        .setCancelable(false)
                                        .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        })
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                AlertDialog dialog = builder.create();
                                dialog.show();
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        String newName = input.getText().toString();
                                        String prevName = file.getName();
                                        if (newName.matches("")) {
                                            Snackbar.make(v, "Please enter name", Snackbar.LENGTH_LONG).show();
                                        } else if (newName.contains(File.separator)) {
                                            Snackbar.make(v, "Please enter valid name", Snackbar.LENGTH_LONG).show();
                                        } else if (newName.matches(Pattern.quote(prevName))) {
                                            Snackbar.make(v, "Please enter a NEW name", Snackbar.LENGTH_LONG).show();
                                        } else if (localDataSet.contains(newName)) {
                                            Snackbar.make(v, "File/folder with same name already exists", Snackbar.LENGTH_LONG).show();
                                        } else {
                                            Thread thread = new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        String withoutName = filePath.substring(0, filePath.lastIndexOf(File.separator) + 1);
                                                        //Log.d("Name", withoutName);
                                                        File testing = new File(withoutName + newName);
                                                        testing.createNewFile();
                                                        testing.delete();
                                                        file.renameTo(testing);
                                                        activity.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                dialog.dismiss();
                                                                textView.setText(newName);
                                                                for (int i = 0; i < localDataSet.size(); i++) {
                                                                    if (localDataSet.get(i).matches(Pattern.quote(realPath))) {
                                                                        realPath = testing.getPath().replace(path + File.separator, "");
                                                                        localDataSet.set(i, realPath);
                                                                    }
                                                                }

                                                            }
                                                        });
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                        activity.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Snackbar.make(v, "Please enter valid name", Snackbar.LENGTH_LONG).show();
                                                            }
                                                        });
                                                    }
                                                }
                                            });
                                            thread.start();
                                        }
                                    }
                                });
                            }
                        } else {
                            Snackbar.make(v, "Access denied(file/folder is being deleted", Snackbar.LENGTH_LONG).show();
                        }
                    }
                    return true;
                }
            });
            fileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isSearching) {
                        String filePath = path + File.separator + realPath;
                        if (!deletedFilePaths.contains(filePath)) {
                            if (new File(filePath).isDirectory()) {
                                if (new File(filePath).canWrite()) {
                                    if (((Explorer) activity).currentOperationNumber == 0) {
                                        Recview.stopScroll();
                                        ((Explorer) activity).currentOperationNumber++;
                                        Animation fadeIn = AnimationUtils.loadAnimation(activity.getBaseContext(), android.R.anim.slide_out_right);
                                        fadeIn.setDuration(200);
                                        fadeIn.setFillAfter(true);
                                        Recview.startAnimation(fadeIn);
                                        Thread thread = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                File[] files = new File(filePath).listFiles();
                                                //if(files != null){}
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
                                                for (int i = 0; i < holders.size(); i++) {
                                                    holders.get(i).fileCheckbox.setChecked(false);
                                                }
                                                CheckedId.clear();
                                                holders.clear();
                                                closeBottomBar();
                                                if (!isDoingFileOperations && fragment.getAddButtonState() == View.GONE) {
                                                    activity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            fragment.showAddButton(true);
                                                        }
                                                    });
                                                }
                                                int position = getAdapterPosition();
                                                localDataSet = fileNames;
                                                path = filePath;
                                                //new ExplorerAdapter(fileNames, path + File.separator + textView.getText(), Recview, activity);
                                                while (!fadeIn.hasEnded()) {
                                                    try {
                                                        Thread.sleep(10);
                                                    } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                                Animation fadeOut = AnimationUtils.loadAnimation(activity.getBaseContext(), android.R.anim.slide_in_left);
                                                fadeOut.setDuration(200);
                                                activity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        notifyDataSetChanged();
                                                        Recview.scrollToPosition(0);
                                                        Recview.startAnimation(fadeOut);
                                                    }
                                                });
                                                while (!fadeOut.hasEnded()) {
                                                    try {
                                                        Thread.sleep(10);
                                                    } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                                ((Explorer) activity).currentOperationNumber--;
                                            }
                                        });
                                        thread.start();
                                    }
                                } else {
                                    Snackbar.make(v, "Access denied", Snackbar.LENGTH_LONG).show();
                                }
                            } else {
                                if (!isDoingFileOperations) {
                                    CharSequence[] items = null;
                                    if (encrypted) {
                                        items = new CharSequence[]{"Decrypt file"};
                                    } else {
                                        items = new CharSequence[]{"Encrypt file", "Open file"};
                                    }
                                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(fileImage.getContext(), R.style.MaterialAlertDialog_rounded);
                                    builder.setTitle("Choose action");
                                    builder.setItems(items, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case 0:
                                                    if (!encrypted) {
                                                        if (new File(filePath + ".enc").exists()) {
                                                            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(fileImage.getContext(), R.style.MaterialAlertDialog_rounded);
                                                            dialogBuilder.setTitle("Warning");
                                                            dialogBuilder.setMessage("Encrypted file already exists. Do you want to replace it?");
                                                            dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    Snackbar.make(v, "Encryption started!", Snackbar.LENGTH_LONG).show();
                                                                    ArrayList<String> paths = new ArrayList<>();
                                                                    paths.add(filePath);
                                                                    Intent intent = new Intent(activity.getBaseContext(), EncryptorService.class);
                                                                    intent.putExtra("actionType", "E");
                                                                    EncryptorService.uniqueID++;
                                                                    int i = EncryptorService.uniqueID;
                                                                    EncryptorService.paths.put(i, paths);
                                                                    intent.putExtra("index", i);
                                                                    intent.putExtra("pass", ((Explorer) activity).getIntent2().getByteArrayExtra("pass"));
                                                                    ContextCompat.startForegroundService(activity.getBaseContext(), intent);
                                                                }
                                                            });
                                                            dialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {

                                                                }
                                                            });
                                                            dialogBuilder.show();
                                                        } else {
                                                            Snackbar.make(v, "Encryption started!", Snackbar.LENGTH_LONG).show();
                                                            ArrayList<String> paths = new ArrayList<>();
                                                            paths.add(filePath);
                                                            Intent intent = new Intent(activity.getBaseContext(), EncryptorService.class);
                                                            intent.putExtra("actionType", "E");
                                                            EncryptorService.uniqueID++;
                                                            int i = EncryptorService.uniqueID;
                                                            EncryptorService.paths.put(i, paths);
                                                            intent.putExtra("index", i);
                                                            intent.putExtra("pass", ((Explorer) activity).getIntent2().getByteArrayExtra("pass"));
                                                            ContextCompat.startForegroundService(activity.getBaseContext(), intent);
                                                        }
                                                    } else {
                                                        if (new File((filePath).substring(0, (filePath).length() - 4)).exists()) {
                                                            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(fileImage.getContext(), R.style.MaterialAlertDialog_rounded);
                                                            dialogBuilder.setTitle("Warning");
                                                            dialogBuilder.setMessage("Decrypted file already exists. Do you want to replace it?");
                                                            dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    Snackbar.make(v, "Decryption started!", Snackbar.LENGTH_LONG).show();
                                                                    ArrayList<String> paths = new ArrayList<>();
                                                                    paths.add(filePath);
                                                                    Intent intent = new Intent(activity.getBaseContext(), EncryptorService.class);
                                                                    intent.putExtra("actionType", "D");
                                                                    EncryptorService.uniqueID++;
                                                                    int i = EncryptorService.uniqueID;
                                                                    EncryptorService.paths.put(i, paths);
                                                                    intent.putExtra("index", i);
                                                                    intent.putExtra("pass", ((Explorer) activity).getIntent2().getByteArrayExtra("pass"));
                                                                    ContextCompat.startForegroundService(activity.getBaseContext(), intent);
                                                                }
                                                            });
                                                            dialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {

                                                                }
                                                            });
                                                            dialogBuilder.show();
                                                        } else {
                                                            Snackbar.make(v, "Decryption started!", Snackbar.LENGTH_LONG).show();
                                                            ArrayList<String> paths = new ArrayList<>();
                                                            paths.add(filePath);
                                                            Intent intent = new Intent(activity.getBaseContext(), EncryptorService.class);
                                                            intent.putExtra("actionType", "D");
                                                            EncryptorService.uniqueID++;
                                                            int i = EncryptorService.uniqueID;
                                                            EncryptorService.paths.put(i, paths);
                                                            intent.putExtra("index", i);
                                                            intent.putExtra("pass", ((Explorer) activity).getIntent2().getByteArrayExtra("pass"));
                                                            ContextCompat.startForegroundService(activity.getBaseContext(), intent);
                                                        }
                                                    }
                                                    break;
                                                case 1:
                                                    try {
                                                        String filePath2 = filePath;
                                                        File file = new File(filePath2);
                                                        Uri uriForFile = FileProvider.getUriForFile(activity.getBaseContext(), "com.suslanium.encryptor.fileprovider", file);
                                                        String type = activity.getContentResolver().getType(uriForFile);
                                                        Intent intent = new Intent();
                                                        intent.setAction(Intent.ACTION_VIEW);
                                                        intent.setDataAndType(uriForFile, type);
                                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                        activity.startActivity(intent);
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                        Snackbar.make(v, "Failed to open file", Snackbar.LENGTH_LONG).show();
                                                    }
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }
                                    });
                                    builder.show();
                                }
                            }
                        } else {
                            Snackbar.make(v, "Access denied(file/folder is being deleted)", Snackbar.LENGTH_LONG).show();
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

        public void setBitMap(Bitmap bitMap) {
            fileImage.setImageBitmap(bitMap);
        }

        public void deselectCheckbox() {
            fileCheckbox.setChecked(false);
        }

        public void setFileImageAlpha(float alpha){
            fileImage.setAlpha(alpha);
        }
    }

    public void addDeletedFile(String path) {
        deletedFilePaths.add(path);
    }

    public void addAllDeletedFiles(ArrayList<String> paths) {
        deletedFilePaths.addAll(paths);
    }

    public void removeDeletedFile(String path) {
        deletedFilePaths.remove(path);
    }

    public void removeAllDeletedFiles(ArrayList<String> paths) {
        deletedFilePaths.removeAll(paths);
    }

    public void closeBottomBar() {
        if (CheckedId.isEmpty()) {
            //Log.d("Call","nonempty");
            if (bottomBar.getVisibility() == View.VISIBLE) {
                bottomBar.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                bottomBar.setVisibility(View.GONE);
                            }
                        });
            }
        }
    }

    public void setDoingFileOperations(boolean doingFileOperations) {
        isDoingFileOperations = doingFileOperations;
    }

    public void openBottomBar() {
        if (CheckedId.isEmpty()) {
            //Log.d("Call","empty");
            if (bottomBar.getVisibility() == View.GONE) {
                bottomBar.setVisibility(View.VISIBLE);
                bottomBar.bringToFront();
                bottomBar.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .setListener(null);
                bottomBar.getMenu().setGroupCheckable(0, true, false);
                for (int i = 0; i < bottomBar.getMenu().size(); i++) {
                    bottomBar.getMenu().getItem(i).setChecked(false);
                }
                bottomBar.getMenu().setGroupCheckable(0, true, true);
                //Log.d("Call","empty2");
                fragment.showAddButton(false);
            }

        }
    }

    public void deselectAll() {
        CheckedId.clear();
        if (!holders.isEmpty()) {
            for (int i = 0; i < holders.size(); i++) {
                holders.get(i).deselectCheckbox();
            }
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet String[] containing the data to populate views to be used
     *                by RecyclerView.
     */
    public ExplorerAdapter(ArrayList<String> dataSet, String path, RecyclerView view, Activity activity, BottomNavigationView bottomBar, HomeFragment fragment) {
        localDataSet = dataSet;
        this.path = path;
        Recview = view;
        this.activity = activity;
        format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        format.setTimeZone(TimeZone.getDefault());
        date = new Date();
        service = Executors.newCachedThreadPool();
        this.bottomBar = bottomBar;
        this.fragment = fragment;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.viewholder_folder, viewGroup, false);

        return new ViewHolder(view);
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
        if(name.startsWith(".")){
            viewHolder.setFileImageAlpha(0.5f);
        } else {
            viewHolder.setFileImageAlpha(1f);
        }
        service.submit(new Runnable() {
            @Override
            public void run() {
                // Get element from your dataset at this position and replace the
                // contents of the view with that element
                File file = new File(path + File.separator + localDataSet.get(position));
                long lastModified = file.lastModified();
                date.setTime(lastModified);
                String formattedDate = format.format(date);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewHolder.getDateView().setText(formattedDate);
                    }
                });
                if (file.getName().endsWith(".enc") || file.getName().endsWith("Enc")) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            viewHolder.getIsEncrypted().setVisibility(View.VISIBLE);
                            viewHolder.encrypted = true;
                        }
                    });
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            viewHolder.getIsEncrypted().setVisibility(View.INVISIBLE);
                            viewHolder.encrypted = false;
                        }
                    });
                }
                if (file.isFile()) {
                    Uri uriForFile = FileProvider.getUriForFile(activity.getBaseContext(), "com.suslanium.encryptor.fileprovider", file);
                    String type = activity.getContentResolver().getType(uriForFile);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (type.contains("text")) {
                                viewHolder.setFile(R.drawable.plaintext);
                            } else if (type.contains("audio")) {
                                viewHolder.setFile(R.drawable.music);
                            } else if (type.contains("image")) {
                                viewHolder.setFile(R.drawable.image);
                            } else if (type.contains("video")) {
                                viewHolder.setFile(R.drawable.video);
                            } else if (type.contains("x-msdos-program") || type.contains("vnd.android.package-archive")) {
                                //EXE
                                viewHolder.setFile(R.drawable.app);
                            } else if (type.contains("powerpoint") || type.contains("presentation")) {
                                viewHolder.setFile(R.drawable.powerpoint);
                            } else if (type.contains("msword") || type.contains("document") || type.contains("pdf") || type.contains("rtf") || type.contains("excel") || type.contains("sheet")) {
                                //DOCX
                                viewHolder.setFile(R.drawable.richtext);
                            } else if (type.contains("rar") || type.contains("zip") || type.contains("7z")) {
                                viewHolder.setFile(R.drawable.zip);
                            } else {
                                viewHolder.setFile(R.drawable.file);
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
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            switch (finalUnit) {
                                case 0:
                                    viewHolder.getSizeView().setText(finalLength + " B");
                                    //B
                                    break;
                                case 1:
                                    viewHolder.getSizeView().setText(finalLength + " KB");
                                    //KB
                                    break;
                                case 2:
                                    viewHolder.getSizeView().setText(finalLength + " MB");
                                    //MB
                                    break;
                                case 3:
                                    viewHolder.getSizeView().setText(finalLength + " GB");
                                    //GB
                                    break;
                                case 4:
                                    viewHolder.getSizeView().setText(finalLength + " TB");
                                    //TB
                                    break;
                            }
                        }
                    });
                    if (type.contains("image")) {
                        viewHolder.loadingCount++;
                        final int loadingNum = viewHolder.loadingCount;
                        while (thumbnailLoadingCount > 0) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (loadingNum == viewHolder.loadingCount) {
                            thumbnailLoadingCount++;
                            try {
                                Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(file.getPath()), 128, 128);
                                if (viewHolder.getTextView().getText().toString().equals(file.getName())) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            viewHolder.setBitMap(thumbnail);
                                        }
                                    });
                                }
                                thumbnailLoadingCount--;
                            } catch (Exception e) {
                                e.printStackTrace();
                                thumbnailLoadingCount--;
                            }
                        }
                    }
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            viewHolder.setFile(R.drawable.folder);
                            viewHolder.getSizeView().setText("Calculating...");
                        }
                    });
                    int itemCount = file.list() != null ? file.list().length : 0;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            viewHolder.getSizeView().setText(itemCount + " items");
                        }
                    });
                }
            }
        });
    }

    private ActivityManager.MemoryInfo getAvailableMemory() {
        ActivityManager activityManager = (ActivityManager) activity.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    public ArrayList<String> getLocalDataSet() {
        return localDataSet;
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
                files.put(holders.get(i).getAdapterPosition(), (holders.get(i).realPath));
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
        closeBottomBar();
        localDataSet = fileNames;
        this.path = path;
        notifyDataSetChanged();
    }

}
