package com.suslanium.encryptor.ui.notebook;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.recyclerview.widget.RecyclerView;

import com.suslanium.encryptor.R;

import java.util.ArrayList;

public class NotebookAdapter extends RecyclerView.Adapter<NotebookAdapter.ViewHolder> {

    private ArrayList<String> localDataSet;
    private Intent intent;
    private ColorStateList defTint;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final ImageView imageView;
        private String fileName;
        private Intent main_intent;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            imageView = (ImageView) view.findViewById(R.id.serviceIcon);
            textView = (TextView) view.findViewById(R.id.serviceName);
            view.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), NotebookActivity.class);
                intent.putExtra("fileName",fileName);
                intent.putExtra("pass", main_intent.getByteArrayExtra("pass"));
                v.getContext().startActivity(intent);
            });
        }

        public TextView getTextView() {
            return textView;
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet String[] containing the data to populate views to be used
     * by RecyclerView.
     */
    public NotebookAdapter(ArrayList<String> dataSet, Intent intent, Activity activity) {
        localDataSet = dataSet;
        this.intent = intent;
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
                .inflate(R.layout.viewholder_note, viewGroup, false);
        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.getTextView().setText(localDataSet.get(position).substring(0, localDataSet.get(position).indexOf(".")));
        viewHolder.fileName = localDataSet.get(position);
        viewHolder.main_intent = intent;
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public void setNewData(ArrayList<String> dataSet){
        int position = 0;
        localDataSet = dataSet;
        notifyDataSetChanged();
    }
}
