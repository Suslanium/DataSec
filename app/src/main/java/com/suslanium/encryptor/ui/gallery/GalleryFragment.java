package com.suslanium.encryptor.ui.gallery;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.suslanium.encryptor.Encryptor;
import com.suslanium.encryptor.Explorer;
import com.suslanium.encryptor.PasswordAdapter;
import com.suslanium.encryptor.R;
import com.suslanium.encryptor.passwordAdd;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class GalleryFragment extends Fragment {
    private boolean isOnResume = false;
    private GalleryViewModel galleryViewModel;
    private Intent intent2 = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //galleryViewModel = new ViewModelProvider(this).get(GalleryViewModel.class);
        View root = inflater.inflate(R.layout.fragment_gallery, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FloatingActionButton fab = getView().findViewById(R.id.addData);
        intent2 = ((Explorer) getActivity()).getIntent2();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), passwordAdd.class);
                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                startActivity(intent);
            }
        });
        updateView(view);
    }

    public void onThreadDone(ArrayList<String> strings2, ArrayList<Integer> id){
        RecyclerView recyclerView = getView().findViewById(R.id.passwords);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        PasswordAdapter adapter = new PasswordAdapter(strings2, id, intent2);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isOnResume) {
            updateView(getView());
        }
        else {
            isOnResume = true;
        }
    }

    private void updateView(View view){
        ArrayList<String> strings3 = new ArrayList<>();
        ArrayList<Integer> ids = new ArrayList<>();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] pass = intent2.getByteArrayExtra("pass");
                    String password = Encryptor.RSADecrypt(pass);
                    SQLiteDatabase database = Encryptor.initDataBase(getContext(), password);
                    HashMap<Integer, ArrayList<String>> listHashMap = Encryptor.readPasswordData(database);
                    Set<Integer> integers = listHashMap.keySet();
                    for (Integer i : integers) {
                        ArrayList<String> strings = listHashMap.get(i);
                        String s = strings.get(0);
                        strings3.add(s);
                        ids.add(i);
                    }
                    Encryptor.closeDataBase(database);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onThreadDone(strings3, ids);
                        }
                    });
                } catch (Exception e){
                    e.printStackTrace();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(view, "Failed to read database(perhaps your password is wrong?).", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
        thread.start();
    }
}