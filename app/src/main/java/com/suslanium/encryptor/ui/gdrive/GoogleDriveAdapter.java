package com.suslanium.encryptor.ui.gdrive;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.suslanium.encryptor.EncryptorService;
import com.suslanium.encryptor.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleDriveAdapter extends RecyclerView.Adapter<GoogleDriveAdapter.ViewHolder> {
    private ArrayList<String> localDataSet;
    private ArrayList<String> ids;
    private ArrayList<String> mimeTypes;
    private final Context context;
    private final RecyclerView recyclerView;
    private final ArrayList<String> checkedId = new ArrayList<>();
    private boolean canSelect = true;
    private final ArrayList<ViewHolder> holders = new ArrayList<>();
    private final GoogleDriveViewModel viewModel;
    private final AsyncLayoutInflater.OnInflateFinishedListener listener;
    private final ExecutorService service;
    private final ColorStateList defTint;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;
        private ImageView fileType;
        private CheckBox fileCheckBox;
        private TextView fileModDate;
        private TextView fileSize;
        private boolean isFolder = false;
        private final View parentView;

        public ViewHolder(View view) {
            super(view);
            parentView = view;
        }

        private void setupHolder(View view){
            fileType = view.findViewById(R.id.fileImage);
            Button fileButton = view.findViewById(R.id.fileButton);
            Button checkBoxButton = view.findViewById(R.id.checkBoxButton);
            fileCheckBox = view.findViewById(R.id.fileCheckbox);
            fileSize = view.findViewById(R.id.fileSize);
            fileModDate = view.findViewById(R.id.modDate);
            checkBoxButton.setOnClickListener(v -> {
                if (canSelect) {
                    if (fileCheckBox.isChecked()) {
                        fileCheckBox.setChecked(false);
                        checkedId.remove(textView.getText().toString());
                    } else {
                        fileCheckBox.setChecked(true);
                        checkedId.add(textView.getText().toString());
                    }
                    ((GoogleDriveManager) context).checkFileBar();
                }
            });
            checkBoxButton.setOnLongClickListener(v -> {
                selectAll();
                return true;
            });
            fileButton.setOnClickListener(v -> {
                if (((GoogleDriveManager) context).currentOperationNumber == 0) {
                    if (!isFolder) {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_rounded)
                                .setTitle(R.string.confirmAction)
                                .setMessage(context.getString(R.string.downloadAndDecryptQ) + " " + textView.getText() + "?")
                                .setPositiveButton(R.string.yes, (dialog, which) -> {
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
                                            EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                                            int j = EncryptorService.getUniqueID();
                                            EncryptorService.getPaths().put(j, paths);
                                            EncryptorService.getNames().put(j, names);
                                            EncryptorService.getMimeTypes().put(j, mimes);
                                            intent.putExtra("index", j);
                                            intent.putExtra("pass", ((GoogleDriveManager) context).getIntent().getByteArrayExtra("pass"));
                                            ContextCompat.startForegroundService(context, intent);
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
                                recyclerView.suppressLayout(true);
                                Thread thread = new Thread(() -> {
                                    try {
                                        viewModel.listFilesInFolder(id,false,true,name);
                                        ((GoogleDriveManager) context).runOnUiThread(((GoogleDriveManager) context)::constructAndSetPath);
                                    } catch (Exception ignored) {

                                    }
                                });
                                thread.start();
                            }
                        }
                    }
                }
            });
            textView = view.findViewById(R.id.fileName);
        }

        private ImageView getFileType() {
            return fileType;
        }

        private TextView getTextView() {
            return textView;
        }

        private TextView getFileModDate() {
            return fileModDate;
        }

        private TextView getFileSize() {
            return fileSize;
        }
    }

    public GoogleDriveAdapter(ArrayList<String> dataSet, ArrayList<String> ids, Context context, RecyclerView view, ArrayList<String> mimeTypes, GoogleDriveViewModel viewModel) {
        localDataSet = dataSet;
        this.ids = ids;
        this.context = context;
        recyclerView = view;
        this.mimeTypes = mimeTypes;
        this.viewModel = viewModel;
        listener = (view1, resid, parent) -> parent.addView(view1);
        service = Executors.newCachedThreadPool();
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.explorerIconColor, typedValue, true);
        @ColorInt int color = typedValue.data;
        defTint = ColorStateList.valueOf(color);
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

    @Override
    public void onBindViewHolder(@NotNull ViewHolder viewHolder, final int position) {
        service.submit(() -> {
            while (viewHolder.parentView.findViewById(R.id.fileName) == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
            if (viewHolder.getTextView() == null)  ((GoogleDriveManager) context).runOnUiThread(() -> viewHolder.setupHolder(viewHolder.parentView));
            while (viewHolder.getTextView() == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }
            ((GoogleDriveManager) context).runOnUiThread(() -> {
                viewHolder.fileCheckBox.setChecked(checkedId.contains(localDataSet.get(position)));
                viewHolder.getFileType().setImageResource(R.drawable.ic_folder);
                viewHolder.getFileType().setImageTintList(defTint);
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
            });
        });
    }

    protected void setCanSelect(boolean canSelect) {
        this.canSelect = canSelect;
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (localDataSet == null) return 0;
        else return localDataSet.size();
    }
    protected void selectAll() {
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

    protected ArrayList<String> getCheckedIds() {
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

    protected ArrayList<String> getCheckedMimes() {
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

    protected void deselectAll() {
        checkedId.clear();
        setNewData(localDataSet, ids, mimeTypes);
    }

    protected ArrayList<String> getCheckedNames() {
        return new ArrayList<>(checkedId);
    }

    protected void setNewData(ArrayList<String> dataSet, ArrayList<String> ids, ArrayList<String> mimeTypes) {
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
