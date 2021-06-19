package com.suslanium.encryptor.ui.password;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.suslanium.encryptor.R;
import com.suslanium.encryptor.ui.PasswordEntry;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.suslanium.encryptor.ui.explorer.ExplorerFragment.fadeIn;
import static com.suslanium.encryptor.ui.explorer.ExplorerFragment.fadeOut;

public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.ViewHolder> {

    private ArrayList<String> localDataSet;
    private ArrayList<Integer> localids;
    private ArrayList<String> localLogins;
    private ArrayList<String> localCategories;
    private final Intent intent;
    private ArrayList<Bitmap> icons = new ArrayList<>();
    private final Activity activity;
    private final PasswordFragment fragment;
    private final boolean showLogins;
    private static ColorStateList defTint;
    private final ExecutorService service;
    private final AsyncLayoutInflater.OnInflateFinishedListener onInflateFinishedListener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;
        private boolean isCategory = false;
        protected int id = 0;
        protected int position = 0;
        protected Intent main_intent;
        private ImageView iconView;
        private TextView loginView;
        private PasswordFragment fragment;
        private final View parentView;

        public ViewHolder(View view) {
            super(view);
            parentView = view;
        }

        private void setupHolder(View view){
            iconView = view.findViewById(R.id.serviceIcon);
            loginView = view.findViewById(R.id.loginText);
            view.setOnClickListener(v -> {
                if (!isCategory) {
                    Intent intent = new Intent(v.getContext(), PasswordEntry.class);
                    intent.putExtra("id", id);
                    intent.putExtra("category", fragment.getCurrentCategory());
                    intent.putExtra("pass", main_intent.getByteArrayExtra("pass"));
                    v.getContext().startActivity(intent);
                } else {
                    fragment.setCategory(textView.getText().toString());
                }
            });
            view.setOnLongClickListener(v -> {
                if(isCategory){
                    final EditText input = new EditText(fragment.requireContext());
                    Typeface ubuntu = ResourcesCompat.getFont(fragment.requireContext(), R.font.ubuntu);
                    input.setTypeface(ubuntu);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setSingleLine(true);
                    input.setText(textView.getText().toString());
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(fragment.requireContext(),R.style.MaterialAlertDialog_rounded)
                            .setTitle(R.string.renameCategory)
                            .setView(input)
                            .setPositiveButton(R.string.rename, (dialog, which) -> {
                                if(fragment.currentOperationNumber == 0) {
                                    fragment.currentOperationNumber++;
                                    fadeIn(fragment.recyclerView);
                                    fadeOut(fragment.searchProgress);
                                    fadeOut(fragment.searchText);
                                    fragment.fab.setEnabled(false);
                                    fragment.viewModel.renameCategory(textView.getText().toString(), input.getText().toString());
                                }
                            })
                            .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                            .setNeutralButton(R.string.delete, (dialog, which) -> {
                                if(fragment.currentOperationNumber == 0) {
                                    fragment.currentOperationNumber++;
                                    fadeIn(fragment.recyclerView);
                                    fadeOut(fragment.searchProgress);
                                    fadeOut(fragment.searchText);
                                    fragment.fab.setEnabled(false);
                                    fragment.viewModel.deleteCategory(textView.getText().toString());
                                }
                            });
                    builder.show();
                    return true;
                }
                return false;
            });
            textView = view.findViewById(R.id.serviceName);
        }

        private TextView getTextView() {
            return textView;
        }

        private TextView getLoginView() {
            return loginView;
        }

        private void setIconBitmap(Bitmap bitmap) {
            iconView.setImageBitmap(bitmap);
        }

        private void setDefaultIcon() {
            iconView.setImageTintList(defTint);
            iconView.setImageResource(R.drawable.managerkey);
        }

        private void setCategory() {
            isCategory = true;
            iconView.setImageTintList(defTint);
            iconView.setImageResource(R.drawable.ic_folder);
        }

        private void setNonCategory(){
            isCategory = false;
        }

        private void removeTint(){
            iconView.setImageTintList(null);
        }
    }

    public PasswordAdapter(ArrayList<String> dataSet, ArrayList<Integer> ids, ArrayList<String> logins, ArrayList<String> categories, Intent intent, Activity activity, PasswordFragment fragment) {
        localCategories = categories;
        localDataSet = dataSet;
        localids = ids;
        localLogins = logins;
        service = Executors.newCachedThreadPool();
        this.intent = intent;
        this.activity = activity;
        this.fragment = fragment;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(fragment.requireContext());
        showLogins = preferences.getBoolean("showLogins", true);
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = activity.getTheme();
        theme.resolveAttribute(R.attr.explorerIconColor, typedValue, true);
        @ColorInt int color = typedValue.data;
        defTint = ColorStateList.valueOf(color);
        onInflateFinishedListener = (view, resid, parent) -> parent.addView(view);
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.viewholder_dummy, viewGroup, false);
        AsyncLayoutInflater inflater = new AsyncLayoutInflater(viewGroup.getContext());
        inflater.inflate(R.layout.viewholder_manager, (ViewGroup) view, onInflateFinishedListener);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder viewHolder, final int position) {
        service.submit(() -> {
            while (viewHolder.parentView.findViewById(R.id.serviceName) == null) {
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
            activity.runOnUiThread(() -> {
                if (position < localCategories.size()) {
                    viewHolder.getTextView().setText(localCategories.get(position));
                    viewHolder.setCategory();
                    viewHolder.getLoginView().setText("");
                } else {
                    if(position - localCategories.size() >= 0) {
                        viewHolder.getTextView().setText(localDataSet.get(position - localCategories.size()));
                        viewHolder.getLoginView().setText("");
                        viewHolder.setNonCategory();
                        if (localLogins.get(position - localCategories.size()) != null && showLogins) {
                            viewHolder.getLoginView().setText(localLogins.get(position - localCategories.size()));
                        }
                        viewHolder.id = localids.get(position - localCategories.size());
                        viewHolder.main_intent = intent;
                        viewHolder.position = position - localCategories.size();
                        try {
                            if (icons.get(position - localCategories.size()) != null) {
                                viewHolder.removeTint();
                                viewHolder.setIconBitmap(icons.get(position - localCategories.size()));
                            } else {
                                viewHolder.setDefaultIcon();
                            }
                        } catch (Exception e) {
                            viewHolder.setDefaultIcon();
                        }
                    }
                }
                viewHolder.fragment = fragment;
            });
        });
    }

    @Override
    public int getItemCount() {
        if (localids.size() > 0) {
            return localDataSet.size() + localCategories.size();
        } else {
            return localCategories.size();
        }
    }

    protected void setNewData(ArrayList<String> dataSet, ArrayList<Integer> ids, ArrayList<String> logins, ArrayList<String> categories) {
        int position = 0;
        localDataSet = dataSet;
        if (dataSet.isEmpty()) {
            dataSet.addAll(localCategories);
        }
        localCategories = categories;
        localids = ids;
        localLogins = logins;
        notifyDataSetChanged();
    }

    protected void setIcons(ArrayList<Bitmap> icons) {
        this.icons = icons;
        activity.runOnUiThread(() -> setNewData(localDataSet, localids, localLogins, localCategories));
    }

}
