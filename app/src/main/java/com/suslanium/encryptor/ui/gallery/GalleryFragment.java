package com.suslanium.encryptor.ui.gallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
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
    public void onDestroyView() {
        ((Explorer) getActivity()).passwordVaultVisible = false;
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((Explorer) getActivity()).passwordVaultVisible = true;
        //-------------
        Toolbar t = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if(((Explorer) getActivity()).searchButton != null)t.removeView(((Explorer) getActivity()).searchButton);
        if(((Explorer) getActivity()).searchBar != null) {
            t.removeView(((Explorer) getActivity()).searchBar);
            ((Explorer) getActivity()).searchBar = null;
            final InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        ImageButton b1=new ImageButton(getContext());
        Drawable drawable = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            drawable = getContext().getDrawable(android.R.drawable.ic_menu_search);
        } else {
            drawable = getResources().getDrawable(android.R.drawable.ic_menu_search);
        }
        b1.setImageDrawable(drawable);
        b1.setBackgroundColor(Color.parseColor("#00000000"));
        Toolbar.LayoutParams l3=new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
        l3.gravity= Gravity.END;
        b1.setLayoutParams(l3);
        ((Explorer) getActivity()).searchButton = b1;
        t.addView(b1);
        //--------------
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((Explorer) getActivity()).searchBar == null) {
                    EditText layout = new EditText(getContext());
                    Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                    layoutParams.gravity = Gravity.START;
                    layout.setLayoutParams(l3);
                    layout.setHint("Enter service name here...");
                    t.addView(layout, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                    layout.setFocusableInTouchMode(true);
                    layout.requestFocus();
                    final InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.showSoftInput(layout, InputMethodManager.SHOW_IMPLICIT);
                    ((Explorer) getActivity()).searchBar = layout;
                } else {
                    //Search
                }
            }
        });
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