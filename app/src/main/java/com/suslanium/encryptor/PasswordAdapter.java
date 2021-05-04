package com.suslanium.encryptor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.ViewHolder> {

    private ArrayList<String> localDataSet;
    private ArrayList<Integer> localids;
    private Intent intent;
    private ArrayList<Bitmap> icons = new ArrayList<>();
    private HashMap<Integer, ViewHolder> holders = new HashMap<>();
    private Activity activity;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        protected int id = 0;
        protected int position = 0;
        protected Intent main_intent;
        private final ImageView iconView;

        public ViewHolder(View view) {
            super(view);
            view.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), passwordChange.class);
                intent.putExtra("id", id);
                intent.putExtra("pass", main_intent.getByteArrayExtra("pass"));
                v.getContext().startActivity(intent);
            });

            textView = view.findViewById(R.id.serviceName);
            iconView = view.findViewById(R.id.serviceIcon);
        }

        public TextView getTextView() {
            return textView;
        }

        public void setIconBitmap(Bitmap bitmap){
            iconView.setImageBitmap(bitmap);
        }

        public void setDefaultIcon(){
            iconView.setImageResource(R.drawable.managerkey);
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet String[] containing the data to populate views to be used
     *                by RecyclerView.
     */
    public PasswordAdapter(ArrayList<String> dataSet, ArrayList<Integer> ids, Intent intent, Activity activity) {
        localDataSet = dataSet;
        localids = ids;
        this.intent = intent;
        this.activity = activity;
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
        viewHolder.getTextView().setText(localDataSet.get(position));
        viewHolder.id = localids.get(position);
        viewHolder.main_intent = intent;
        viewHolder.position = position;
        try {
            if (icons.get(position) != null) {
                viewHolder.setIconBitmap(icons.get(position));
            } else {
                viewHolder.setDefaultIcon();
            }
        } catch (Exception e){
            viewHolder.setDefaultIcon();
        }
        holders.put(position, viewHolder);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public void setNewData(ArrayList<String> dataSet, ArrayList<Integer> ids) {
        int position = 0;
        localDataSet = dataSet;
        localids = ids;
        holders.clear();
        notifyDataSetChanged();
    }

    public void setIcons(ArrayList<Bitmap> icons){
        this.icons = icons;
        activity.runOnUiThread(() ->setNewData(localDataSet, localids));
    }

    public HashMap<Integer, ViewHolder> getHolders(){
        return holders;
    }
}
