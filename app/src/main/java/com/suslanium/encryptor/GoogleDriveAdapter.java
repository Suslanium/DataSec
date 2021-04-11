package com.suslanium.encryptor;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.List;

public class GoogleDriveAdapter extends RecyclerView.Adapter<GoogleDriveAdapter.ViewHolder> {
    private ArrayList<String> localDataSet;
    private ArrayList<String> ids;
    private DriveServiceHelper helper;
    private Context context;
    private RecyclerView recyclerView;
    private ArrayList<String> CheckedId = new ArrayList<>();

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final ImageView fileType;
        private final Button fileButton;
        private final Button checkBoxButton;
        private final CheckBox fileCheckBox;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            fileType = (ImageView) view.findViewById(R.id.fileImage);
            textView = (TextView) view.findViewById(R.id.fileName);
            fileButton = (Button) view.findViewById(R.id.fileButton);
            checkBoxButton = (Button) view.findViewById(R.id.checkBoxButton);
            fileCheckBox = (CheckBox) view.findViewById(R.id.fileCheckbox);
            checkBoxButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fileCheckBox.isChecked()) {
                        fileCheckBox.setChecked(false);
                        CheckedId.remove(textView.getText().toString());
                    } else {
                        fileCheckBox.setChecked(true);
                        CheckedId.add(textView.getText().toString());
                    }
                }
            });
            fileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (textView.getText().toString().contains(".")) {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_rounded)
                                .setTitle("Confirm action")
                                .setMessage("Do you really want to download and decrypt " + textView.getText() + "?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        for (int i = 0; i < localDataSet.size(); i++) {
                                            if (localDataSet.get(i).contentEquals(textView.getText())) {
                                                String id = ids.get(i);
                                                ArrayList<String> paths = new ArrayList<>();
                                                ArrayList<String> names = new ArrayList<>();
                                                paths.add(id);
                                                names.add(localDataSet.get(i));
                                                Intent intent = new Intent(context, EncryptorService.class);
                                                intent.putExtra("actionType", "gDriveD");
                                                intent.putExtra("paths", paths);
                                                intent.putExtra("names", names);
                                                intent.putExtra("pass", ((GoogleDriveManager)context).getIntent().getByteArrayExtra("pass"));
                                                ContextCompat.startForegroundService(context, intent);
                                            }
                                        }
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        builder.show();
                    } else {
                        for (int i = 0; i < localDataSet.size(); i++) {
                            if (localDataSet.get(i).contentEquals(textView.getText())) {
                                String id = ids.get(i);
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            List<File> files = helper.listDriveFiles(id);
                                            if (files != null) {
                                                ArrayList<String>[] names = new ArrayList[]{null, null};
                                                names[0] = new ArrayList<String>();
                                                names[1] = new ArrayList<String>();
                                                for (File file : files) {
                                                    names[0].add(file.getName());
                                                    names[1].add(file.getId());
                                                }
                                                ((GoogleDriveManager) context).lists.put(((GoogleDriveManager) context).lists.size(), new ArrayList[]{localDataSet, ids});
                                                int position = getAdapterPosition();
                                                localDataSet = names[0];
                                                ids = names[1];
                                                ((GoogleDriveManager) context).runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        notifyDataSetChanged();
                                                        recyclerView.scrollToPosition(0);
                                                    }
                                                });
                                                ((GoogleDriveManager) context).ids.add(((GoogleDriveManager) context).getCurrentFolderID());
                                                ((GoogleDriveManager) context).setCurrentFolderID(id);
                                            } else {
                                                ArrayList<String>[] names = new ArrayList[]{null, null};
                                                names[0] = new ArrayList<String>();
                                                names[1] = new ArrayList<String>();
                                                ((GoogleDriveManager) context).lists.put(((GoogleDriveManager) context).lists.size(), new ArrayList[]{localDataSet, ids});
                                                int position = getAdapterPosition();
                                                localDataSet = names[0];
                                                ids = names[1];
                                                ((GoogleDriveManager) context).runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        notifyDataSetChanged();
                                                        recyclerView.scrollToPosition(0);
                                                    }
                                                });
                                                ((GoogleDriveManager) context).ids.add(((GoogleDriveManager) context).getCurrentFolderID());
                                                ((GoogleDriveManager) context).setCurrentFolderID(id);
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                thread.start();
                            }
                        }
                    }
                }
            });
        }

        public ImageView getFileType() {
            return fileType;
        }

        public TextView getTextView() {
            return textView;
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet String[] containing the data to populate views to be used
     *                by RecyclerView.
     */
    public GoogleDriveAdapter(ArrayList<String> dataSet, DriveServiceHelper helper, ArrayList<String> ids, Context context, RecyclerView view) {
        localDataSet = dataSet;
        this.ids = ids;
        this.helper = helper;
        this.context = context;
        recyclerView = view;
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
        if (!CheckedId.contains(localDataSet.get(position))) {
            viewHolder.fileCheckBox.setChecked(false);
        } else {
            viewHolder.fileCheckBox.setChecked(true);
        }
        viewHolder.getFileType().setImageResource(R.drawable.folder);
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.getTextView().setText(localDataSet.get(position));
        if (localDataSet.get(position).contains("."))
            viewHolder.getFileType().setImageResource(R.drawable.file);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (localDataSet == null) return 0;
        else return localDataSet.size();
    }

    public ArrayList<String> getCheckedIds() {
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < CheckedId.size(); i++) {
            for (int j = 0; j < localDataSet.size(); j++) {
                if (localDataSet.get(j).equals(CheckedId.get(i))) {
                    String ID = ids.get(j);
                    files.add(ID);
                }
            }
            //files.add(CheckedId.get(i));
        }
        return files;
    }

    public ArrayList<String> getCheckedNames() {
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < CheckedId.size(); i++) {
            String ID = CheckedId.get(i);
            files.add(ID);
            //files.add(CheckedId.get(i));
        }
        return files;
    }

    public void setNewData(ArrayList<String> dataSet, ArrayList<String> ids) {
        int position = 0;
        localDataSet = dataSet;
        this.ids = ids;
        CheckedId.clear();
        ((GoogleDriveManager) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
                recyclerView.scrollToPosition(0);
            }
        });
    }
}
