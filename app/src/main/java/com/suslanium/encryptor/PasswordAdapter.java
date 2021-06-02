package com.suslanium.encryptor;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.recyclerview.widget.RecyclerView;

import com.suslanium.encryptor.ui.password.PasswordFragment;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.ViewHolder> {

    private ArrayList<String> localDataSet;
    private ArrayList<Integer> localids;
    private ArrayList<String> localLogins;
    private ArrayList<String> localCategories;
    private Intent intent;
    private ArrayList<Bitmap> icons = new ArrayList<>();
    private HashMap<Integer, ViewHolder> holders = new HashMap<>();
    private Activity activity;
    private PasswordFragment fragment;
    private boolean showLogins = true;
    private static ColorStateList defTint;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private boolean isCategory = false;
        protected int id = 0;
        protected int position = 0;
        protected Intent main_intent;
        private final ImageView iconView;
        private final TextView loginView;
        private PasswordFragment fragment;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.serviceName);
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
        }

        public TextView getTextView() {
            return textView;
        }

        public TextView getLoginView() {
            return loginView;
        }

        public void setIconBitmap(Bitmap bitmap) {
            iconView.setImageBitmap(bitmap);
        }

        public void setDefaultIcon() {
            iconView.setImageTintList(defTint);
            iconView.setImageResource(R.drawable.managerkey);
        }

        public void setCategory() {
            isCategory = true;
            iconView.setImageTintList(defTint);
            iconView.setImageResource(R.drawable.ic_folder);
        }

        public void setNonCategory(){
            isCategory = false;
        }

        public void removeTint(){
            iconView.setImageTintList(null);
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet String[] containing the data to populate views to be used
     *                by RecyclerView.
     */
    public PasswordAdapter(ArrayList<String> dataSet, ArrayList<Integer> ids, ArrayList<String> logins, ArrayList<String> categories, Intent intent, Activity activity, PasswordFragment fragment) {
        localDataSet = dataSet;
        localids = ids;
        localLogins = logins;
        localCategories = categories;
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
    }

    // Create new views (invoked by the layout manager)
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.viewholder_manager, viewGroup, false);
        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        if (position < localCategories.size()) {
            viewHolder.getTextView().setText(localCategories.get(position));
            viewHolder.setCategory();
            viewHolder.getLoginView().setText("");
        } else {
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
        viewHolder.fragment = fragment;
        holders.put(position, viewHolder);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (localids.size() > 0) {
            return localDataSet.size() + localCategories.size();
        } else {
            return localCategories.size();
        }
    }

    public void setNewData(ArrayList<String> dataSet, ArrayList<Integer> ids, ArrayList<String> logins, ArrayList<String> categories) {
        int position = 0;
        localDataSet = dataSet;
        if (dataSet.isEmpty()) {
            dataSet.addAll(localCategories);
        }
        localids = ids;
        localLogins = logins;
        localCategories = categories;
        holders.clear();
        notifyDataSetChanged();
    }

    public void setIcons(ArrayList<Bitmap> icons) {
        this.icons = icons;
        activity.runOnUiThread(() -> setNewData(localDataSet, localids, localLogins, localCategories));
    }

    public HashMap<Integer, ViewHolder> getHolders() {
        return holders;
    }
}
