package com.suslanium.encryptor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class SettingsFragment extends Fragment {

    private GoogleSignInClient mGoogleSignInClient;
    private static final int SIGNIN = 0;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGNIN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
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
        Button backUpDataBase = requireActivity().findViewById(R.id.backUpDatabase);
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
            backUpDataBase.setTextColor(Color.parseColor("#000000"));
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
        backUpDataBase.setOnClickListener(v -> {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
            if(account != null) {
                askForRestoringOrBackup();
            } else {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, SIGNIN);
            }
        });
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            askForRestoringOrBackup();
        } catch (ApiException e) {
            Log.w("GoogleDrive", "signInResult:failed code=" + e.getStatusCode());
        }
    }

    private void askForRestoringOrBackup(){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded)
                .setTitle("Choose action")
                .setItems(new CharSequence[]{"Backup database", "Restore database"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
                                MaterialAlertDialogBuilder builder1 = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded)
                                        .setTitle("WARNING!")
                                        .setMessage("This operation will delete previous backup. You will lose ALL your data from previous backup(if it exists). If you want to save your data - you should restore passwords from previous backup before doing this. Do you want to continue?")
                                        .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent intent = new Intent(requireContext(), EncryptorService.class);
                                                intent.putExtra("actionType", "gDriveDBU");
                                                EncryptorService.uniqueID++;
                                                int i = EncryptorService.uniqueID;
                                                intent.putExtra("index", i);
                                                intent.putExtra("pass", ((Explorer) requireActivity()).getIntent2().getByteArrayExtra("pass"));
                                                ContextCompat.startForegroundService(requireContext(), intent);
                                                MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded);
                                                ProgressBar bar = new ProgressBar(requireContext());
                                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT);
                                                bar.setLayoutParams(lp);
                                                builder2.setTitle("Uploading database...");
                                                builder2.setView(bar);
                                                builder2.setCancelable(false);
                                                AlertDialog alertDialog = builder2.create();
                                                alertDialog.show();
                                                Thread thread = new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            Thread.sleep(1000);
                                                        } catch (InterruptedException e) {
                                                            e.printStackTrace();
                                                            Thread.currentThread().interrupt();
                                                        }
                                                        while(EncryptorService.deletingFiles.containsValue(true)){
                                                            try {
                                                                Thread.sleep(100);
                                                            } catch (InterruptedException e) {
                                                                e.printStackTrace();
                                                                Thread.currentThread().interrupt();
                                                            }
                                                        }
                                                        requireActivity().runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                alertDialog.dismiss();
                                                                switch (EncryptorService.backupRestoreReturn){
                                                                    case 0:
                                                                        Snackbar.make(requireView(), "Database backed up successfully!", Snackbar.LENGTH_LONG).show();
                                                                        break;
                                                                    case 2:
                                                                        Snackbar.make(requireView(), "Database not found.", Snackbar.LENGTH_LONG).show();
                                                                        EncryptorService.backupRestoreReturn = 0;
                                                                        //No database
                                                                        break;
                                                                    case 3:
                                                                        Snackbar.make(requireView(), "Error", Snackbar.LENGTH_LONG).show();
                                                                        EncryptorService.backupRestoreReturn = 0;
                                                                        //Error
                                                                        break;
                                                                    default:
                                                                        break;
                                                                }
                                                            }
                                                        });
                                                    }
                                                });
                                                thread.start();
                                            }
                                        })
                                        .setNegativeButton("Cancel", (dialog1, which1) -> {});
                                builder1.show();
                                break;
                            case 1:
                                MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded)
                                        .setTitle("Select action")
                                        .setMessage("You can either replace all your data or merge passwords from cloud with current database. What would you like to do?")
                                        .setPositiveButton("Merge", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent intent = new Intent(requireContext(), EncryptorService.class);
                                                intent.putExtra("actionType", "gDriveDBD");
                                                EncryptorService.uniqueID++;
                                                int i = EncryptorService.uniqueID;
                                                intent.putExtra("index", i);
                                                intent.putExtra("mergeData", true);
                                                intent.putExtra("pass", ((Explorer) requireActivity()).getIntent2().getByteArrayExtra("pass"));
                                                ContextCompat.startForegroundService(requireContext(), intent);
                                                afterServiceStart();
                                            }
                                        })
                                        .setNegativeButton("Replace", (dialog1, which1) -> {
                                            Intent intent = new Intent(requireContext(), EncryptorService.class);
                                            intent.putExtra("actionType", "gDriveDBD");
                                            EncryptorService.uniqueID++;
                                            int i = EncryptorService.uniqueID;
                                            intent.putExtra("index", i);
                                            intent.putExtra("mergeData", false);
                                            intent.putExtra("pass", ((Explorer) requireActivity()).getIntent2().getByteArrayExtra("pass"));
                                            ContextCompat.startForegroundService(requireContext(), intent);
                                            afterServiceStart();
                                        });
                                builder2.show();
                                break;
                        }
                    }
                });
        builder.show();
    }

    private void afterServiceStart(){
        MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded);
        ProgressBar bar = new ProgressBar(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bar.setLayoutParams(lp);
        builder2.setTitle("Downloading database...");
        builder2.setView(bar);
        builder2.setCancelable(false);
        AlertDialog alertDialog = builder2.create();
        alertDialog.show();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                while(EncryptorService.deletingFiles.containsValue(true)){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        alertDialog.dismiss();
                        switch (EncryptorService.backupRestoreReturn){
                            case 0:
                                Snackbar.make(requireView(), "Database restored successfully!", Snackbar.LENGTH_LONG).show();
                                break;
                            case 1:
                                Snackbar.make(requireView(), "Database password doesn't match current password", Snackbar.LENGTH_LONG).show();
                                EncryptorService.backupRestoreReturn = 0;
                                break;
                            case 2:
                                Snackbar.make(requireView(), "Database not found in cloud.", Snackbar.LENGTH_LONG).show();
                                EncryptorService.backupRestoreReturn = 0;
                                //No database
                                break;
                            case 3:
                                Snackbar.make(requireView(), "Error", Snackbar.LENGTH_LONG).show();
                                EncryptorService.backupRestoreReturn = 0;
                                //Error
                                break;
                            default:
                                break;
                        }
                    }
                });
            }
        });
        thread.start();
    }
}