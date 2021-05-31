package com.suslanium.encryptor;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.List;

public class GoogleDriveAdapter extends RecyclerView.Adapter<GoogleDriveAdapter.ViewHolder> {
    private ArrayList<String> localDataSet;
    private ArrayList<String> ids;
    private ArrayList<String> mimeTypes;
    private DriveServiceHelper helper;
    private Context context;
    private RecyclerView recyclerView;
    private ArrayList<String> checkedId = new ArrayList<>();
    private boolean canSelect = true;
    private ArrayList<ViewHolder> holders = new ArrayList<>();

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final ImageView fileType;
        private final CheckBox fileCheckBox;
        private final TextView fileModDate;
        private final TextView fileSize;
        private boolean isFolder = false;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            fileType = (ImageView) view.findViewById(R.id.fileImage);
            textView = (TextView) view.findViewById(R.id.fileName);
            Button fileButton = (Button) view.findViewById(R.id.fileButton);
            Button checkBoxButton = (Button) view.findViewById(R.id.checkBoxButton);
            fileCheckBox = (CheckBox) view.findViewById(R.id.fileCheckbox);
            fileSize = (TextView) view.findViewById(R.id.fileSize);
            fileModDate = (TextView) view.findViewById(R.id.modDate);
            checkBoxButton.setOnClickListener(v -> {
                if (canSelect) {
                    if (fileCheckBox.isChecked()) {
                        fileCheckBox.setChecked(false);
                        checkedId.remove(textView.getText().toString());
                        ((GoogleDriveManager) context).checkFileBar();
                    } else {
                        fileCheckBox.setChecked(true);
                        checkedId.add(textView.getText().toString());
                        ((GoogleDriveManager) context).checkFileBar();
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
            fileButton.setOnClickListener(v -> {
                if (((GoogleDriveManager) context).currentOperationNumber == 0) {
                    if (!isFolder) {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_rounded)
                                .setTitle(R.string.confirmAction)
                                .setMessage(context.getString(R.string.downloadAndDecryptQ) + " " + textView.getText() + "?")
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        for (int i = 0; i < localDataSet.size(); i++) {
                                            if (localDataSet.get(i).contentEquals(textView.getText())) {
                                                String id = ids.get(i);
                                                ArrayList<String> paths = new ArrayList<>();
                                                ArrayList<String> names = new ArrayList<>();
                                                ArrayList<String> mimes = new ArrayList<>();
                                                paths.add(id);
                                                names.add(localDataSet.get(i));
                                                mimes.add(mimeTypes.get(i));
                                                Intent intent = new Intent(context, EncryptorService.class);
                                                intent.putExtra("actionType", "gDriveD");
                                                EncryptorService.uniqueID++;
                                                int j = EncryptorService.uniqueID;
                                                EncryptorService.paths.put(j, paths);
                                                EncryptorService.names.put(j, names);
                                                EncryptorService.mimeTypes.put(j, mimes);
                                                intent.putExtra("index", j);
                                                intent.putExtra("pass", ((GoogleDriveManager) context).getIntent().getByteArrayExtra("pass"));
                                                ContextCompat.startForegroundService(context, intent);
                                            }
                                        }
                                    }
                                })
                                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                        builder.show();
                    } else {
                        for (int i = 0; i < localDataSet.size(); i++) {
                            if (localDataSet.get(i).contentEquals(textView.getText())) {
                                String id = ids.get(i);
                                String name = localDataSet.get(i);
                                recyclerView.stopScroll();
                                Animation fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.slide_out_right);
                                fadeIn.setDuration(200);
                                fadeIn.setFillAfter(true);
                                recyclerView.startAnimation(fadeIn);
                                ((GoogleDriveManager) context).currentOperationNumber++;
                                holders.clear();
                                checkedId.clear();
                                ((GoogleDriveManager) context).checkFileBar();
                                Thread thread = new Thread(() -> {
                                    try {
                                        List<File> files = helper.listDriveFiles(id);
                                        ArrayList<String>[] names = new ArrayList[]{null, null, null};
                                        names[0] = new ArrayList<>();
                                        names[1] = new ArrayList<>();
                                        names[2] = new ArrayList<>();
                                        if (files != null) {
                                            for (File file : files) {
                                                names[0].add(file.getName());
                                                names[1].add(file.getId());
                                                names[2].add(file.getMimeType());
                                            }
                                        }
                                        ((GoogleDriveManager) context).lists.put(((GoogleDriveManager) context).lists.size(), new ArrayList[]{localDataSet, ids, mimeTypes});
                                        int position = getAdapterPosition();
                                        localDataSet = names[0];
                                        ids = names[1];
                                        mimeTypes = names[2];
                                        while (!fadeIn.hasEnded()) {
                                            Thread.sleep(10);
                                        }
                                        Animation fadeOut = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left);
                                        fadeOut.setDuration(200);
                                        ((GoogleDriveManager) context).runOnUiThread(() -> {
                                            ((GoogleDriveManager) context).folders.add(name);
                                            ((GoogleDriveManager) context).constructAndSetPath();
                                            notifyDataSetChanged();
                                            recyclerView.scrollToPosition(0);
                                            recyclerView.startAnimation(fadeOut);
                                        });
                                        while (!fadeOut.hasEnded()) {
                                            try {
                                                Thread.sleep(10);
                                            } catch (Exception e) {

                                            }
                                        }
                                        ((GoogleDriveManager) context).currentOperationNumber--;
                                        ((GoogleDriveManager) context).ids.add(((GoogleDriveManager) context).getCurrentFolderID());
                                        ((GoogleDriveManager) context).setCurrentFolderID(id);
                                    } catch (Exception e) {

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

        public TextView getFileModDate() {
            return fileModDate;
        }

        public TextView getFileSize() {
            return fileSize;
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet String[] containing the data to populate views to be used
     *                by RecyclerView.
     */
    public GoogleDriveAdapter(ArrayList<String> dataSet, DriveServiceHelper helper, ArrayList<String> ids, Context context, RecyclerView view, ArrayList<String> mimeTypes) {
        localDataSet = dataSet;
        this.ids = ids;
        this.helper = helper;
        this.context = context;
        recyclerView = view;
        this.mimeTypes = mimeTypes;
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
        if (!checkedId.contains(localDataSet.get(position))) {
            viewHolder.fileCheckBox.setChecked(false);
        } else {
            viewHolder.fileCheckBox.setChecked(true);
        }
        viewHolder.getFileType().setImageResource(R.drawable.ic_folder);
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.getTextView().setText(localDataSet.get(position));
        if (!mimeTypes.get(position).equals("application/vnd.google-apps.folder")) {
            viewHolder.getFileType().setImageResource(R.drawable.ic_file);
            viewHolder.isFolder = false;
        } else {
            viewHolder.isFolder = true;
        }
        viewHolder.getFileSize().setVisibility(View.INVISIBLE);
        viewHolder.getFileModDate().setVisibility(View.INVISIBLE);
        holders.add(viewHolder);
    }

    public void setCanSelect(boolean canSelect) {
        this.canSelect = canSelect;
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (localDataSet == null) return 0;
        else return localDataSet.size();
    }
    public void selectAll() {
        if (((GoogleDriveManager) context).currentOperationNumber == 0) {
            if (!localDataSet.equals(checkedId)) {
                if (!holders.isEmpty()) {
                    for (int i = 0; i < holders.size(); i++) {
                        holders.get(i).fileCheckBox.setChecked(true);
                    }
                }
                checkedId.clear();
                if (localDataSet != null && !localDataSet.isEmpty()) {
                    checkedId.addAll(localDataSet);
                }
                ((GoogleDriveManager) context).checkFileBar();
            } else {
                if (!holders.isEmpty()) {
                    for (int i = 0; i < holders.size(); i++) {
                        holders.get(i).fileCheckBox.setChecked(false);
                    }
                }
                checkedId.clear();
                ((GoogleDriveManager) context).checkFileBar();
            }
        }
    }

    public ArrayList<String> getCheckedIds() {
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < checkedId.size(); i++) {
            for (int j = 0; j < localDataSet.size(); j++) {
                if (localDataSet.get(j).equals(checkedId.get(i))) {
                    String id = ids.get(j);
                    files.add(id);
                }
            }
        }
        return files;
    }

    public ArrayList<String> getCheckedMimes() {
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < checkedId.size(); i++) {
            for (int j = 0; j < localDataSet.size(); j++) {
                if (localDataSet.get(j).equals(checkedId.get(i))) {
                    String id = mimeTypes.get(j);
                    files.add(id);
                }
            }
        }
        return files;
    }

    public void deselectAll() {
        checkedId.clear();
        setNewData(localDataSet, ids, mimeTypes);
    }

    public ArrayList<String> getCheckedNames() {
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < checkedId.size(); i++) {
            String id = checkedId.get(i);
            files.add(id);
        }
        return files;
    }

    public void setNewData(ArrayList<String> dataSet, ArrayList<String> ids, ArrayList<String> mimeTypes) {
        int position = 0;
        localDataSet = dataSet;
        this.ids = ids;
        this.mimeTypes = mimeTypes;
        holders.clear();
        checkedId.clear();
        ((GoogleDriveManager) context).runOnUiThread(() -> {
            notifyDataSetChanged();
            recyclerView.scrollToPosition(0);
        });
    }
}
