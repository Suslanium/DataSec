package com.suslanium.encryptor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

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
        ((Explorer) requireActivity()).settingsVisible = true;
        updateUI();
    }

    @Override
    public void onDestroyView() {
        ((Explorer) requireActivity()).settingsVisible = false;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean dark_theme = preferences.getBoolean("dark_Theme", true);
        boolean autoDelete = preferences.getBoolean("auto_Delete", false);
        boolean autoDelete2 = preferences.getBoolean("auto_Delete2", false);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch darkTheme = requireActivity().findViewById(R.id.darkTheme);
        Button changePass = requireActivity().findViewById(R.id.changePassword);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch deleteAfter = requireActivity().findViewById(R.id.deleteAfter);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch deleteAfter2  = requireActivity().findViewById(R.id.deleteAfter2);
        Toolbar t = requireActivity().findViewById(R.id.toolbar);
        if(((Explorer) requireActivity()).searchButton != null)t.removeView(((Explorer) requireActivity()).searchButton);
        if(((Explorer) requireActivity()).searchBar != null) {
            t.removeView(((Explorer) requireActivity()).searchBar);
            ((Explorer) requireActivity()).searchBar = null;
            final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(requireView().getWindowToken(), 0);
        }
        deleteAfter2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
            editor.putBoolean("auto_Delete2", isChecked);
            editor.apply();
        });
        deleteAfter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
            editor.putBoolean("auto_Delete", isChecked);
            editor.apply();
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
        darkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
            if(isChecked){
                editor.putBoolean("dark_Theme", true);
                editor.apply();
                Intent intent = new Intent(requireContext(), Explorer.class);
                intent.putExtra("pass", ((Explorer) requireActivity()).getIntent2().getByteArrayExtra("pass"));
                intent.putExtra("fromSettings", true);
                startActivity(intent);
            } else {
                editor.putBoolean("dark_Theme", false);
                editor.apply();
                Intent intent = new Intent(requireContext(), Explorer.class);
                intent.putExtra("fromSettings", true);
                intent.putExtra("pass", ((Explorer) requireActivity()).getIntent2().getByteArrayExtra("pass"));
                startActivity(intent);
            }
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        changePass.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder1 = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded);
            builder1.setTitle("Warning!");
            builder1.setMessage("You will lose access to all your encrypted files until you change your password back to the current one! Do you wish to proceed?");
            builder1.setPositiveButton("Yes", (dialog, which) -> {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded);
                builder.setTitle("Enter new password:");
                final EditText input = new EditText(requireContext());
                input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder.setView(input);
                builder.setPositiveButton("Confirm", (dialog1, which1) -> {
                    String newPass = input.getText().toString();
                    if(!newPass.equals("") && newPass != null) {
                        MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded);
                        builder2.setTitle("Moving your passwords to new Database...");
                        builder2.setMessage("Don't close the application until this window disappears!");
                        ProgressBar bar = new ProgressBar(requireContext());
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        bar.setLayoutParams(lp);
                        builder2.setView(bar);
                        builder2.setCancelable(false);
                        builder2.show();
                        Intent intent = new Intent(requireContext(), EncryptorService.class);
                        Thread thread = new Thread(() -> {
                            try {
                                byte[] newPassEnc = Encryptor.rsaencrypt(newPass);
                                intent.putExtra("actionType", "changePass");
                                intent.putExtra("newPass", newPassEnc);
                                intent.putExtra("pass", ((Explorer) requireActivity()).getIntent2().getByteArrayExtra("pass"));
                                ContextCompat.startForegroundService(requireContext(), intent);
                                while(!EncryptorService.changingPassword){
                                    Thread.sleep(100);
                                }
                                while(EncryptorService.changingPassword){
                                    Thread.sleep(100);
                                }
                                requireActivity().runOnUiThread(() -> {
                                    Intent intent2 = new Intent(requireContext(), PasswordActivity.class);
                                    startActivity(intent2);
                                });
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        });
                        thread.start();
                    } else {
                        Snackbar.make(v, "Please enter new password", Snackbar.LENGTH_LONG).show();
                    }
                });
                builder.setNegativeButton("Cancel", (dialog12, which12) -> dialog12.dismiss());
                builder.show();
            });
            builder1.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
            builder1.show();
        });

    }
}