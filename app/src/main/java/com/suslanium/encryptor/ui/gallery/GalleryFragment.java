package com.suslanium.encryptor.ui.gallery;

import android.annotation.SuppressLint;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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
    private Intent intent2 = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onDestroyView() {
        ((Explorer) requireActivity()).passwordVaultVisible = false;
        super.onDestroyView();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((Explorer) requireActivity()).passwordVaultVisible = true;
        Toolbar t = requireActivity().findViewById(R.id.toolbar);
        if(((Explorer) requireActivity()).searchButton != null)t.removeView(((Explorer) requireActivity()).searchButton);
        if(((Explorer) requireActivity()).searchBar != null) {
            t.removeView(((Explorer) requireActivity()).searchBar);
            ((Explorer) requireActivity()).searchBar = null;
            final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        ImageButton b1=new ImageButton(requireContext());
        Drawable drawable;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            drawable = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_search);
        } else {
            drawable = getResources().getDrawable(android.R.drawable.ic_menu_search);
        }
        b1.setImageDrawable(drawable);
        b1.setBackgroundColor(Color.parseColor("#00000000"));
        Toolbar.LayoutParams l3=new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
        l3.gravity= Gravity.END;
        b1.setLayoutParams(l3);
        ((Explorer) requireActivity()).searchButton = b1;
        t.addView(b1);
        b1.setOnClickListener(v -> {
            if(((Explorer) requireActivity()).searchBar == null) {
                EditText layout = new EditText(requireContext());
                Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                layoutParams.gravity = Gravity.START;
                layout.setLayoutParams(l3);
                layout.setHint("Enter service name here...");
                t.addView(layout, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                layout.setFocusableInTouchMode(true);
                layout.requestFocus();
                final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(layout, InputMethodManager.SHOW_IMPLICIT);
                ((Explorer) requireActivity()).searchBar = layout;
            }

        });
        FloatingActionButton fab = requireView().findViewById(R.id.addData);
        intent2 = ((Explorer) requireActivity()).getIntent2();
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), passwordAdd.class);
            intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
            startActivity(intent);
        });
        updateView(view);
    }

    public void onThreadDone(ArrayList<String> strings2, ArrayList<Integer> id){
        RecyclerView recyclerView = requireView().findViewById(R.id.passwords);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        PasswordAdapter adapter = new PasswordAdapter(strings2, id, intent2);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isOnResume) {
            updateView(requireView());
        }
        else {
            isOnResume = true;
        }
    }

    private void updateView(View view){
        ArrayList<String> strings3 = new ArrayList<>();
        ArrayList<Integer> ids = new ArrayList<>();
        Thread thread = new Thread(() -> {
            try {
                byte[] pass = intent2.getByteArrayExtra("pass");
                String password = Encryptor.rsadecrypt(pass);
                SQLiteDatabase database = Encryptor.initDataBase(requireContext(), password);
                HashMap<Integer, ArrayList<String>> listHashMap = Encryptor.readPasswordData(database);
                Set<Integer> integers = listHashMap.keySet();
                for (Integer i : integers) {
                    ArrayList<String> strings = listHashMap.get(i);
                    String s = strings.get(0);
                    strings3.add(s);
                    ids.add(i);
                }
                Encryptor.closeDataBase(database);
                requireActivity().runOnUiThread(() -> onThreadDone(strings3, ids));
            } catch (Exception e){
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> Snackbar.make(view, "Failed to read database(perhaps your password is wrong?).", Snackbar.LENGTH_LONG).show());
            }
        });
        thread.start();
    }
}