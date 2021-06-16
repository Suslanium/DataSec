package com.suslanium.encryptor.ui.notebook;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.suslanium.encryptor.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class NotebookAdapter extends RecyclerView.Adapter<NotebookAdapter.ViewHolder> {

    private ArrayList<String> localDataSet;
    private final Intent intent;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private String fileName;
        private Intent main_intent;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.serviceName);
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

    public NotebookAdapter(ArrayList<String> dataSet, Intent intent) {
        localDataSet = dataSet;
        this.intent = intent;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.viewholder_note, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        viewHolder.getTextView().setText(localDataSet.get(position).substring(0, localDataSet.get(position).indexOf(".")));
        viewHolder.fileName = localDataSet.get(position);
        viewHolder.main_intent = intent;
    }

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
