package com.suslanium.encryptor;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.icu.text.DateFormat;

import java.sql.Time;
import java.text.SimpleDateFormat;

import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.suslanium.encryptor.ui.home.HomeFragment.sortFiles;

public class GDriveUploadSelectorAdapter extends RecyclerView.Adapter<GDriveUploadSelectorAdapter.ViewHolder> {
    private ArrayList<String> localDataSet;
    private String path;
    private RecyclerView Recview;
    private Activity activity;
    private ArrayList<ViewHolder> holders = new ArrayList<>();
    private ArrayList<String> CheckedId = new ArrayList<>();
    private SimpleDateFormat format;
    private Date date;

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
                    if (fileCheckbox.isChecked()) {
                        fileCheckbox.setChecked(false);
                        CheckedId.remove(textView.getText().toString());
                    } else {
                        fileCheckbox.setChecked(true);
                        CheckedId.add(textView.getText().toString());
                    }
                }
            });
            fileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (new File(path + File.separator + textView.getText()).isDirectory()) {
                        if (new File(path + File.separator + textView.getText()).canWrite()) {
                            if(((GoogleDriveUploadSelector) activity).currentOperationNumber == 0) {
                                Recview.stopScroll();
                                ((GoogleDriveUploadSelector) activity).currentOperationNumber++;
                                Animation fadeIn = AnimationUtils.loadAnimation(activity.getBaseContext(), android.R.anim.slide_out_right);
                                fadeIn.setDuration(200);
                                fadeIn.setFillAfter(true);
                                Recview.startAnimation(fadeIn);
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        File[] files = new File(path + File.separator + textView.getText()).listFiles();
                                        //if(files != null){}
                                        ArrayList<String> paths = new ArrayList<>();
                                        for (int i = 0; i < files.length; i++) {
                                            paths.add(files[i].getPath());
                                        }
                                        List<String> sorted = sortFiles(paths);
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
                                        int position = getAdapterPosition();
                                        localDataSet = fileNames;
                                        path = path + File.separator + textView.getText();
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
                                        while (!fadeOut.hasEnded()){
                                            try {
                                                Thread.sleep(10);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        ((GoogleDriveUploadSelector) activity).currentOperationNumber--;
                                    }
                                });
                                thread.start();
                            }
                        } else {
                            Snackbar.make(v, "Access denied", Snackbar.LENGTH_LONG).show();
                        }
                    } else {
                        CharSequence[] items = new CharSequence[]{"Encrypt file", "Decrypt file"};
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(fileImage.getContext(), R.style.MaterialAlertDialog_rounded)
                        .setTitle("Confirm action")
                                .setMessage("Do you want to encrypt & upload this file?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ArrayList<String> paths = new ArrayList<>();
                                        paths.add(path + File.separator + textView.getText());
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
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        builder.show();
                    }
                    //Move to next folder if clicked element is folder
                }
            });
            textView = (TextView) view.findViewById(R.id.fileName);
        }

        public TextView getTextView() {
            return textView;
        }
        public TextView getDateView() {return dateView;}
        public TextView getSizeView() {return sizeView;}
        public ImageView getIsEncrypted() {
            return isEncrypted;
        }

        public void setFile(int id) {
            fileImage.setImageResource(id);
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
        format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        format.setTimeZone(TimeZone.getDefault());
        date = new Date();
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
        viewHolder.setFile(R.drawable.folder);
        if (!CheckedId.contains(localDataSet.get(position))) {
            viewHolder.fileCheckbox.setChecked(false);
        } else {
            viewHolder.fileCheckbox.setChecked(true);
        }
        viewHolder.getTextView().setText(localDataSet.get(position));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Get element from your dataset at this position and replace the
                // contents of the view with that element
                File file = new File(path + File.separator + localDataSet.get(position));
                if (file.getName().endsWith(".enc") || file.getName().endsWith("Enc")) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            viewHolder.getIsEncrypted().setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            viewHolder.getIsEncrypted().setVisibility(View.INVISIBLE);
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
                            } else if (type.contains("msword") || type.contains("document") || type.contains("pdf") || type.contains("rtf") || type.contains("excel") || type.contains("sheet")) {
                                //DOCX
                                viewHolder.setFile(R.drawable.richtext);
                            } else if (type.contains("powerpoint") || type.contains("presentation")) {
                                viewHolder.setFile(R.drawable.powerpoint);
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
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
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
                long lastModified = file.lastModified();
                date.setTime(lastModified);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewHolder.getDateView().setText(format.format(date));
                    }
                });
            }
        });
        thread.start();
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
