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
import java.util.HashMap;
import java.util.Set;

import static android.content.Context.NOTIFICATION_SERVICE;

public class GDriveUploadSelectorAdapter extends RecyclerView.Adapter<GDriveUploadSelectorAdapter.ViewHolder> {
    private ArrayList<String> localDataSet;
    private String path;
    private RecyclerView Recview;
    private Activity activity;
    private ArrayList<ViewHolder> holders = new ArrayList<>();
    private ArrayList<String> CheckedId = new ArrayList<>();

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
                            ArrayList<String> fileNames = new ArrayList<>();
                            for (int i = 0; i < files.length; i++) {
                                fileNames.add(files[i].getName());
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
                                        intent.putExtra("paths", paths);
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
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.getTextView().setText(localDataSet.get(position));
        if (new File(path + File.separator + localDataSet.get(position)).isFile())
            viewHolder.setFile(R.drawable.file);
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
