package com.suslanium.encryptor.ui.notebook;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.suslanium.encryptor.ui.Explorer;
import com.suslanium.encryptor.R;

import java.io.File;
import java.util.ArrayList;

import static com.suslanium.encryptor.ui.explorer.ExplorerFragment.fadeIn;
import static com.suslanium.encryptor.ui.explorer.ExplorerFragment.fadeOut;

public class Notebook extends Fragment {

    private NotebookAdapter adapter;
    private RecyclerView recview;
    private TextView searchText;
    private ProgressBar searchProgress;

    public Notebook() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        adapter = new NotebookAdapter(noteNames,intent, requireActivity());
        FloatingActionButton addButton = requireActivity().findViewById(R.id.addNote);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(requireContext(), NotebookActivity.class);
                intent1.putExtra("newNote", true);
                intent1.putExtra("pass", intent.getByteArrayExtra("pass"));
                requireContext().startActivity(intent1);
            }
        });
        recview.setLayoutManager(new LinearLayoutManager(requireContext()));
        recview.setAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notebook, container, false);
    }

    public void updateUI(){
        if(adapter != null) {
            fadeIn(recview);
            fadeOut(searchProgress);
            fadeOut(searchText);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    File parentFolder = new File(requireContext().getFilesDir().getPath() + File.separator + "Notes");
                    parentFolder.mkdirs();
                    File[] noteFiles = parentFolder.listFiles();
                    ArrayList<String> noteNames = new ArrayList<>();
                    if (noteFiles != null && noteFiles.length > 0) {
                        for (int i = 0; i < noteFiles.length; i++) {
                            noteNames.add(noteFiles[i].getName());
                        }
                    }
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.setNewData(noteNames);
                            fadeIn(searchProgress);
                            fadeIn(searchText);
                            fadeOut(recview);
                        }
                    });
                }
            });
            thread.start();
        }
    }
}