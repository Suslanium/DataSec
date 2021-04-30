package com.suslanium.encryptor;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;

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
        ((Explorer) getActivity()).settingsVisible = true;
        updateUI();
    }

    @Override
    public void onDestroyView() {
        ((Explorer) getActivity()).settingsVisible = false;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean dark_theme = preferences.getBoolean("dark_Theme", true);
        boolean autoDelete = preferences.getBoolean("auto_Delete", false);
        boolean autoDelete2 = preferences.getBoolean("auto_Delete2", false);
        Switch darkTheme = getActivity().findViewById(R.id.darkTheme);
        Button changePass = getActivity().findViewById(R.id.changePassword);
        Switch deleteAfter = getActivity().findViewById(R.id.deleteAfter);
        Switch deleteAfter2  = getActivity().findViewById(R.id.deleteAfter2);
        Toolbar t = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if(((Explorer) getActivity()).searchButton != null)t.removeView(((Explorer) getActivity()).searchButton);
        if(((Explorer) getActivity()).searchBar != null) {
            t.removeView(((Explorer) getActivity()).searchBar);
            ((Explorer) getActivity()).searchBar = null;
            final InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
        deleteAfter2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                    editor.putBoolean("auto_Delete2", true);
                    editor.apply();
                } else {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                    editor.putBoolean("auto_Delete2", false);
                    editor.apply();
                }
            }
        });
        deleteAfter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                    editor.putBoolean("auto_Delete", true);
                    editor.apply();
                } else {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                    editor.putBoolean("auto_Delete", false);
                    editor.apply();
                }
            }
        });
        if(dark_theme){
            darkTheme.setChecked(true);
        } else {
            changePass.setTextColor(Color.parseColor("#000000"));
        }
        if(autoDelete){
            deleteAfter.setChecked(true);
        }
        if(autoDelete2){
            deleteAfter2.setChecked(true);
        }
        darkTheme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                    editor.putBoolean("dark_Theme", true);
                    editor.apply();
                    Intent intent = new Intent(getContext(), Explorer.class);
                    intent.putExtra("pass", ((Explorer) getActivity()).getIntent2().getByteArrayExtra("pass"));
                    intent.putExtra("fromSettings", true);
                    startActivity(intent);
                    getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } else {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                    editor.putBoolean("dark_Theme", false);
                    editor.apply();
                    Intent intent = new Intent(getContext(), Explorer.class);
                    intent.putExtra("fromSettings", true);
                    intent.putExtra("pass", ((Explorer) getActivity()).getIntent2().getByteArrayExtra("pass"));
                    startActivity(intent);
                    getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            }
        });
        changePass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialAlertDialogBuilder builder1 = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded);
                builder1.setTitle("Warning!");
                builder1.setMessage("You will lose access to all your encrypted files until you change your password back to the current one! Do you wish to proceed?");
                builder1.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded);
                        builder.setTitle("Enter new password:");
                        final EditText input = new EditText(getContext());
                        input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        builder.setView(input);
                        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String newPass = input.getText().toString();
                                if(!newPass.equals("") && newPass != null) {
                                    MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded);
                                    builder2.setTitle("Moving your passwords to new Database...");
                                    builder2.setMessage("Don't close the application until this window disappears!");
                                    ProgressBar bar = new ProgressBar(getContext());
                                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                    bar.setLayoutParams(lp);
                                    builder2.setView(bar);
                                    builder2.setCancelable(false);
                                    builder2.show();
                                    Intent intent = new Intent(getContext(), EncryptorService.class);
                                    Thread thread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                byte[] newPassEnc = Encryptor.RSAEncrypt(newPass);
                                                intent.putExtra("actionType", "changePass");
                                                intent.putExtra("newPass", newPassEnc);
                                                intent.putExtra("pass", ((Explorer) getActivity()).getIntent2().getByteArrayExtra("pass"));
                                                ContextCompat.startForegroundService(getContext(), intent);
                                                SharedPreferences editor1 = PreferenceManager.getDefaultSharedPreferences(getContext());
                                                while(!EncryptorService.changingPassword){
                                                    Thread.sleep(100);
                                                }
                                                while(EncryptorService.changingPassword){
                                                    Thread.sleep(100);
                                                }
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Intent intent2 = new Intent(getContext(), PasswordActivity.class);
                                                        startActivity(intent2);
                                                    }
                                                });
                                            } catch (Exception e){
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                    thread.start();
                                } else {
                                    Snackbar.make(v, "Please enter new password", Snackbar.LENGTH_LONG).show();
                                }
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.show();
                    }
                });
                builder1.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder1.show();
            }
        });

    }
}