package com.suslanium.encryptor.ui.notebook;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.suslanium.encryptor.R;
import com.suslanium.encryptor.ui.Explorer;

import java.io.File;
import java.util.ArrayList;

import static com.suslanium.encryptor.ui.explorer.ExplorerFragment.fadeIn;
import static com.suslanium.encryptor.ui.explorer.ExplorerFragment.fadeOut;
import static com.suslanium.encryptor.ui.explorer.ExplorerFragment.getTapTarget;

public class Notebook extends Fragment {

    private NotebookAdapter adapter;
    private RecyclerView recview;
    private TextView searchText;
    private ProgressBar searchProgress;
    private boolean tutorialComplete = false;

    public Notebook() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        tutorialComplete = preferences.getBoolean("notebookTutorialComplete",false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onDestroyView() {
        ((Explorer) requireActivity()).notesVisible = false;
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((Explorer) requireActivity()).notesVisible = true;
        Toolbar t = requireActivity().findViewById(R.id.toolbar);
        if (((Explorer) requireActivity()).searchButton != null)
            t.removeView(((Explorer) requireActivity()).searchButton);
        if (((Explorer) requireActivity()).searchBar != null) {
            t.removeView(((Explorer) requireActivity()).searchBar);
            ((Explorer) requireActivity()).searchBar = null;
            final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(requireView().getWindowToken(), 0);
        }
        File parentFolder = new File(requireContext().getFilesDir().getPath() + File.separator + "Notes");
        parentFolder.mkdirs();
        File[] noteFiles = parentFolder.listFiles();
        ArrayList<String> noteNames = new ArrayList<>();
        if(noteFiles != null && noteFiles.length > 0){
            for(int i=0;i<noteFiles.length;i++){
                noteNames.add(noteFiles[i].getName());
            }
        }
        recview = requireActivity().findViewById(R.id.notesView);
        searchProgress = requireActivity().findViewById(R.id.notesUpdateProgress);
        searchText = requireActivity().findViewById(R.id.notesUpdateText);
        Intent intent = ((Explorer) requireActivity()).getIntent2();
        adapter = new NotebookAdapter(noteNames,intent);
        FloatingActionButton addButton = requireActivity().findViewById(R.id.addNote);
        addButton.setOnClickListener(v -> {
            Intent intent1 = new Intent(requireContext(), NotebookActivity.class);
            intent1.putExtra("newNote", true);
            intent1.putExtra("pass", intent.getByteArrayExtra("pass"));
            requireContext().startActivity(intent1);
        });
        recview.setLayoutManager(new LinearLayoutManager(requireContext()));
        recview.setAdapter(adapter);
        if(!tutorialComplete)showHints(addButton);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notebook, container, false);
    }

    public void updateUI(){
        if(adapter != null) {
            fadeIn(recview);
            fadeOut(searchProgress);
            fadeOut(searchText);
            Thread thread = new Thread(() -> {
                File parentFolder = new File(requireContext().getFilesDir().getPath() + File.separator + "Notes");
                parentFolder.mkdirs();
                File[] noteFiles = parentFolder.listFiles();
                ArrayList<String> noteNames = new ArrayList<>();
                if (noteFiles != null && noteFiles.length > 0) {
                    for (int i = 0; i < noteFiles.length; i++) {
                        noteNames.add(noteFiles[i].getName());
                    }
                }
                requireActivity().runOnUiThread(() -> {
                    adapter.setNewData(noteNames);
                    fadeIn(searchProgress);
                    fadeIn(searchText);
                    fadeOut(recview);
                });
            });
            thread.start();
        }
    }

    private void showHints(FloatingActionButton addButton){
        Typeface ubuntu = ResourcesCompat.getFont(requireContext(), R.font.ubuntu);
        TapTargetView.showFor(requireActivity(), getTapTarget(addButton, getString(R.string.notebookHintTitle), getString(R.string.notebookHintMessage), ubuntu), new TapTargetView.Listener(){
            @Override
            public void onTargetClick(TapTargetView view) {
                super.onTargetClick(view);
                SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
                preferences.putBoolean("notebookTutorialComplete", true);
                preferences.apply();
            }
        });
    }
}