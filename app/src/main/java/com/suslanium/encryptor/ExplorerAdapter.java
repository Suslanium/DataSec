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
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.suslanium.encryptor.ui.home.HomeFragment.sortFiles;

public class ExplorerAdapter extends RecyclerView.Adapter<ExplorerAdapter.ViewHolder> {
    private ArrayList<String> localDataSet;
    private String path;
    private RecyclerView Recview;
    private Activity activity;
    private ArrayList<ViewHolder> holders = new ArrayList<>();
    private ArrayList<String> CheckedId = new ArrayList<>();
    private DateFormat format;
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
                            File[] files = new File(path + File.separator + textView.getText()).listFiles();
                            //if(files != null){}
                            ArrayList<String> paths = new ArrayList<>();
                            for(int i=0; i<files.length;i++){
                                paths.add(files[i].getPath());
                            }
                            ArrayList<String> sorted = sortFiles(paths);
                            ArrayList<File> filesSorted = new ArrayList<>();
                            for(int i=0;i<sorted.size();i++){
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
                            notifyDataSetChanged();
                            Recview.scrollToPosition(0);
                        } else {
                            Snackbar.make(v, "Access denied", Snackbar.LENGTH_LONG).show();
                        }
                    } else {
                        CharSequence[] items = new CharSequence[]{"Encrypt file", "Decrypt file"};
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(fileImage.getContext(), R.style.MaterialAlertDialog_rounded);
                        builder.setTitle("Choose action");
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        if (new File(path + File.separator + textView.getText() + ".enc").exists()) {
                                            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(fileImage.getContext(), R.style.MaterialAlertDialog_rounded);
                                            dialogBuilder.setTitle("Warning");
                                            dialogBuilder.setMessage("Encrypted file already exists. Do you want to replace it?");
                                            dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Snackbar.make(v, "Encryption started!", Snackbar.LENGTH_LONG).show();
                                                    ArrayList<String> paths = new ArrayList<>();
                                                    paths.add(path + File.separator + textView.getText());
                                                    Intent intent = new Intent(activity.getBaseContext(), EncryptorService.class);
                                                    intent.putExtra("actionType", "E");
                                                    intent.putExtra("paths", paths);
                                                    intent.putExtra("pass", ((Explorer)activity).getIntent2().getByteArrayExtra("pass"));
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
                                            paths.add(path + File.separator + textView.getText());
                                            Intent intent = new Intent(activity.getBaseContext(), EncryptorService.class);
                                            intent.putExtra("actionType", "E");
                                            intent.putExtra("paths", paths);
                                            intent.putExtra("pass", ((Explorer)activity).getIntent2().getByteArrayExtra("pass"));
                                            ContextCompat.startForegroundService(activity.getBaseContext(), intent);
                                        }
                                        break;
                                    case 1:
                                        if (new File((path + File.separator + textView.getText()).substring(0, (path + File.separator + textView.getText()).length() - 4)).exists()) {
                                            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(fileImage.getContext(), R.style.MaterialAlertDialog_rounded);
                                            dialogBuilder.setTitle("Warning");
                                            dialogBuilder.setMessage("Decrypted file already exists. Do you want to replace it?");
                                            dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Snackbar.make(v, "Decryption started!", Snackbar.LENGTH_LONG).show();
                                                    ArrayList<String> paths = new ArrayList<>();
                                                    paths.add(path + File.separator + textView.getText());
                                                    Intent intent = new Intent(activity.getBaseContext(), EncryptorService.class);
                                                    intent.putExtra("actionType", "D");
                                                    intent.putExtra("paths", paths);
                                                    intent.putExtra("pass", ((Explorer)activity).getIntent2().getByteArrayExtra("pass"));
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
                                            paths.add(path + File.separator + textView.getText());
                                            Intent intent = new Intent(activity.getBaseContext(), EncryptorService.class);
                                            intent.putExtra("actionType", "D");
                                            intent.putExtra("paths", paths);
                                            intent.putExtra("pass", ((Explorer)activity).getIntent2().getByteArrayExtra("pass"));
                                            ContextCompat.startForegroundService(activity.getBaseContext(), intent);
                                        }
                                        break;
                                    default:
                                        break;
                                }
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
    public ExplorerAdapter(ArrayList<String> dataSet, String path, RecyclerView view, Activity activity) {
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
        viewHolder.getSizeView().setVisibility(View.VISIBLE);
        if (!CheckedId.contains(localDataSet.get(position))) {
            viewHolder.fileCheckbox.setChecked(false);
        } else {
            viewHolder.fileCheckbox.setChecked(true);
        }
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.getTextView().setText(localDataSet.get(position));
        File file  = new File(path + File.separator + localDataSet.get(position));
        if (file.isFile()) {
            viewHolder.setFile(R.drawable.file);
            double length = file.length();
            int unit = 0;
            while(length>1024){
                length = length/1024;
                unit++;
            }
            length = (double)Math.round(length * 100)/100;
            switch (unit){
                case 0:
                    viewHolder.getSizeView().setText(length + " B");
                    //B
                    break;
                case 1:
                    viewHolder.getSizeView().setText(length + " KB");
                    //KB
                    break;
                case 2:
                    viewHolder.getSizeView().setText(length + " MB");
                    //MB
                    break;
                case 3:
                    viewHolder.getSizeView().setText(length + " GB");
                    //GB
                    break;
                case 4:
                    viewHolder.getSizeView().setText(length + " TB");
                    //TB
                    break;
            }
        } else {
            viewHolder.getSizeView().setVisibility(View.INVISIBLE);
        }
        long lastModified = file.lastModified();
        date.setTime(lastModified);
        viewHolder.getDateView().setText(format.format(date));
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
