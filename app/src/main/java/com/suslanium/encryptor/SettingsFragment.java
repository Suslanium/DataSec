package com.suslanium.encryptor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean dark_theme = preferences.getBoolean("dark_Theme", true);
        Switch darkTheme = getActivity().findViewById(R.id.darkTheme);
        if(dark_theme){
            darkTheme.setChecked(true);
        }
        //TODO:change fragment to settings on restart
        darkTheme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                    editor.putBoolean("dark_Theme", true);
                    editor.apply();
                    Intent intent = new Intent(getContext(), Explorer.class);
                    intent.putExtra("pass", ((Explorer) getActivity()).getIntent2().getByteArrayExtra("pass"));
                    startActivity(intent);
                    getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } else {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                    editor.putBoolean("dark_Theme", false);
                    editor.apply();
                    Intent intent = new Intent(getContext(), Explorer.class);
                    intent.putExtra("pass", ((Explorer) getActivity()).getIntent2().getByteArrayExtra("pass"));
                    startActivity(intent);
                    getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            }
        });
    }
}