package com.suslanium.encryptor.ui;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.suslanium.encryptor.EncryptorService;
import com.suslanium.encryptor.R;
import com.suslanium.encryptor.ui.gdrive.GoogleDriveManager;
import com.suslanium.encryptor.util.Encryptor;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;

public class SettingsFragment extends Fragment {

    private GoogleSignInClient mGoogleSignInClient;
    private static final int SIGNIN = 0;
    private final Scope SCOPEEMAIL = new Scope(Scopes.EMAIL);
    private final Scope SCOPEAPP = new Scope(Scopes.DRIVE_APPFOLDER);

    public SettingsFragment() {
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
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((Explorer) requireActivity()).setSettingsVisible(true);
        updateUI();
    }

    @Override
    public void onDestroyView() {
        ((Explorer) requireActivity()).setSettingsVisible(false);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        //TODO: Fix issue: dark theme doesn't work on some devices + dark theme crashes the app in some activities if it's is not working after it was enabled
        boolean dark_theme = preferences.getBoolean("dark_Theme", false);
        boolean autoDelete = preferences.getBoolean("auto_Delete", true);
        boolean autoDelete2 = preferences.getBoolean("auto_Delete2", true);
        boolean showLogin = preferences.getBoolean("showLogins", true);
        boolean showPreview = preferences.getBoolean("showPreviews", false);
        boolean showHide = preferences.getBoolean("showHidden", false);
        boolean canUseBioAuth = false;
        BiometricManager biometricManager = BiometricManager.from(requireContext());
        switch (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                canUseBioAuth = true;
                break;
            default:
                break;
        }
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch darkTheme = requireActivity().findViewById(R.id.darkTheme);
        Button changePass = requireActivity().findViewById(R.id.changePassword);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch enableAutofill = requireActivity().findViewById(R.id.autofillSwitch);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch deleteAfter = requireActivity().findViewById(R.id.deleteAfter);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch deleteAfter2 = requireActivity().findViewById(R.id.deleteAfter2);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch showPreviews = requireActivity().findViewById(R.id.previewSwitch);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch showLogins = requireActivity().findViewById(R.id.showLoginsSwitch);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch showHidden = requireActivity().findViewById(R.id.hiddenFilesSwitch);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch bioSwitch = requireActivity().findViewById(R.id.biometricSwitch);
        Button backUpDataBase = requireActivity().findViewById(R.id.backUpDatabase);
        Button signOut = requireActivity().findViewById(R.id.signOut);
        Button changeIc = requireActivity().findViewById(R.id.changeIc);
        Button showTutorial = requireActivity().findViewById(R.id.showTutorialAgain);
        showTutorial.setOnClickListener(v -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("explorerTutorialComplete", false);
            editor.putBoolean("gDriveTutorialComplete", false);
            editor.putBoolean("messageCryptTutorialComplete", false);
            editor.putBoolean("notebookTutorialComplete", false);
            editor.putBoolean("passwordTutorialComplete", false);
            editor.apply();
            Snackbar.make(v,getString(R.string.applied),Snackbar.LENGTH_LONG).show();
        });
        changeIc.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                    .setTitle(R.string.selectIcon)
                    .setItems(new CharSequence[]{getString(R.string.defApp), getString(R.string.calcApp), getString(R.string.playStoreApp), getString(R.string.cameraApp)}, (dialog, which) -> {
                        switch (which){
                            case 0:
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.DefaultIc"), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.CalcIc"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.PlayStoreIc"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.CameraIc"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                break;
                            case 1:
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.CalcIc"), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.DefaultIc"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.PlayStoreIc"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.CameraIc"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                break;
                            case 2:
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.PlayStoreIc"), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.CalcIc"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.DefaultIc"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.CameraIc"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                break;
                            case 3:
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.CameraIc"), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.PlayStoreIc"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.CalcIc"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                requireContext().getPackageManager().setComponentEnabledSetting(new ComponentName("com.suslanium.encryptor", "com.suslanium.encryptor.DefaultIc"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                                break;
                            default:
                                break;
                        }
                        Snackbar.make(v, R.string.appWillClose,Snackbar.LENGTH_LONG).show();
                    });
            builder.show();
        });
        Toolbar t = requireActivity().findViewById(R.id.toolbar);
        if (((Explorer) requireActivity()).getSearchButton() != null)
            t.removeView(((Explorer) requireActivity()).getSearchButton());
        if (((Explorer) requireActivity()).getSearchBar() != null) {
            t.removeView(((Explorer) requireActivity()).getSearchBar());
            ((Explorer) requireActivity()).setSearchBar(null);
            final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(requireView().getWindowToken(), 0);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillManager autofillManager = requireContext().getSystemService(AutofillManager.class);
            if (autofillManager.isAutofillSupported()) {
                enableAutofill.setChecked(autofillManager.hasEnabledAutofillServices());
            } else {
                enableAutofill.setEnabled(false);
            }
        } else {
            enableAutofill.setText(getString(R.string.enableAutofill) + "(Android 8+)");
            enableAutofill.setEnabled(false);
        }
        if(!canUseBioAuth){
            bioSwitch.setEnabled(false);
        } else {
            boolean usesBioAuth = preferences.getBoolean("usesBioAuth", false);
            if(usesBioAuth){
                bioSwitch.setChecked(true);
            }
            bioSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(isChecked){
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                            .setTitle(R.string.warning)
                            .setCancelable(false)
                            .setMessage(R.string.bioWarning)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                SharedPreferences.Editor prefEdit = preferences.edit();
                                prefEdit.putBoolean("usesBioAuth", isChecked);
                                prefEdit.apply();
                                Thread thread = new Thread(() -> {
                                    try {
                                        String pass = Encryptor.rsadecrypt(((Explorer) requireActivity()).getIntent2().getByteArrayExtra("pass"));
                                        MasterKey mainKey = new MasterKey.Builder(requireContext())
                                                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                                .build();
                                        SharedPreferences editor = EncryptedSharedPreferences.create(requireContext(), "encryptor_shared_prefs", mainKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                                        SharedPreferences.Editor edit = editor.edit();
                                        edit.putString("pass", pass);
                                        edit.apply();
                                    } catch (Exception e) {
                                        requireActivity().runOnUiThread(() -> {
                                            try {
                                                Snackbar.make(requireView(), R.string.authEnableFail, Snackbar.LENGTH_LONG).show();
                                                bioSwitch.setChecked(false);
                                            } catch (Exception ignored){}
                                            prefEdit.putBoolean("usesBioAuth", false);
                                            prefEdit.apply();
                                        });
                                    }
                                });
                                thread.start();
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> {bioSwitch.setChecked(false);dialog.dismiss();});
                    builder.show();
                } else {
                    SharedPreferences.Editor prefEdit = preferences.edit();
                    prefEdit.putBoolean("usesBioAuth", isChecked);
                    prefEdit.apply();
                    Thread thread = new Thread(() -> {
                        try {
                            MasterKey mainKey = new MasterKey.Builder(requireContext())
                                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                    .build();
                            SharedPreferences editor = EncryptedSharedPreferences.create(requireContext(), "encryptor_shared_prefs", mainKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                            SharedPreferences.Editor edit = editor.edit();
                            edit.remove("pass");
                            edit.apply();
                        } catch (Exception ignored) {}
                    });
                    thread.start();
                }
            });
        }
        enableAutofill.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isChecked) {
                    AutofillManager autofillManager = requireContext().getSystemService(AutofillManager.class);
                    if (autofillManager.isAutofillSupported()) {
                        if (!autofillManager.hasEnabledAutofillServices()) {
                            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                                    .setTitle(R.string.warning)
                                    .setCancelable(false)
                                    .setMessage(R.string.autofillWarning)
                                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                                        Uri uri = Uri.parse("package:" + requireContext().getPackageName());
                                        Intent i = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE, uri);
                                        startActivityForResult(i, 111);
                                    })
                                    .setNegativeButton(R.string.no, (dialog, which) -> {enableAutofill.setChecked(false);dialog.dismiss();});
                            builder.show();
                        }
                    }
                } else {
                    AutofillManager autofillManager = requireContext().getSystemService(AutofillManager.class);
                    if (autofillManager.isAutofillSupported()) {
                        if (autofillManager.hasEnabledAutofillServices()) {
                            autofillManager.disableAutofillServices();
                        }
                    }
                }
            }
        });
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
        showPreviews.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
            editor.putBoolean("showPreviews", isChecked);
            editor.apply();
        });
        showLogins.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
            editor.putBoolean("showLogins", isChecked);
            editor.apply();
        });
        showHidden.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
            editor.putBoolean("showHidden", isChecked);
            editor.apply();
        });
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            darkTheme.setChecked(false);
            darkTheme.setEnabled(false);
            darkTheme.setText(getString(R.string.darkTheme)+"(Android 10+)");
        }
        if (dark_theme)
            darkTheme.setChecked(true);
        deleteAfter.setChecked(autoDelete);
        deleteAfter2.setChecked(autoDelete2);
        showPreviews.setChecked(showPreview);
        showLogins.setChecked(showLogin);
        showHidden.setChecked(showHide);
        darkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
            if (isChecked) {
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
            builder1.setTitle(R.string.warning);
            builder1.setMessage(R.string.changePassWarning);
            builder1.setPositiveButton(R.string.yes, (dialog, which) -> {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded);
                builder.setTitle(R.string.enterNewPass);
                final EditText input = new EditText(requireContext());
                Typeface ubuntu = ResourcesCompat.getFont(requireContext(), R.font.ubuntu);
                input.setTypeface(ubuntu);
                input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder.setView(input);
                builder.setPositiveButton(R.string.confirm, (dialog1, which1) -> {
                    String newPass = input.getText().toString();
                    if (!newPass.equals("")) {
                        MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded);
                        builder2.setTitle(R.string.wait);
                        builder2.setMessage(R.string.movingPass);
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
                                Thread.sleep(1000);
                                while (EncryptorService.isChangingPassword()) {
                                    Thread.sleep(100);
                                }
                                requireActivity().runOnUiThread(() -> {
                                    Intent intent2 = new Intent(requireContext(), PasswordActivity.class);
                                    startActivity(intent2);
                                });
                            } catch (Exception ignored) {
                            }
                        });
                        thread.start();
                    } else {
                        Snackbar.make(v, R.string.enterNewPassErr, Snackbar.LENGTH_LONG).show();
                    }
                });
                builder.setNegativeButton(R.string.cancel, (dialog12, which12) -> dialog12.dismiss());
                builder.show();
            });
            builder1.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
            builder1.show();
        });
        backUpDataBase.setOnClickListener(v -> {
            ConnectivityManager cm =
                    (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build();
                mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
                if (account != null) {
                    checkForGooglePermissions();
                } else {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, SIGNIN);
                }
            } else {
                Snackbar.make(v, R.string.noInternet, Snackbar.LENGTH_LONG).show();
            }
        });
        signOut.setOnClickListener(v -> {
            ConnectivityManager cm =
                    (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build();
                mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
                if (account != null) {
                    mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> Snackbar.make(v, R.string.signOutSuccess, Snackbar.LENGTH_LONG).show());
                } else {
                    Snackbar.make(v, R.string.signOutErr, Snackbar.LENGTH_LONG).show();
                }
            } else {
                Snackbar.make(v, R.string.noInternet, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            checkForGooglePermissions();
        } catch (ApiException e) {
            Log.w("GoogleDrive", "signInResult:failed code=" + e.getStatusCode());
        }
    }
    private void checkForGooglePermissions() {
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(requireContext()), SCOPEAPP, SCOPEEMAIL)) {
            GoogleSignIn.requestPermissions(requireActivity(), 1, GoogleSignIn.getLastSignedInAccount(requireContext()), SCOPEEMAIL, SCOPEAPP);
        } else {
            askForRestoringOrBackup();
        }
    }
    private void askForRestoringOrBackup() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                .setTitle(R.string.choose)
                .setItems(new CharSequence[]{getString(R.string.backup), getString(R.string.restoreDatabase)}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            MaterialAlertDialogBuilder builder1 = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded)
                                    .setTitle(R.string.warning)
                                    .setMessage(R.string.overwriteBackup)
                                    .setPositiveButton(R.string.cont, (dialog13, which13) -> {
                                        Intent intent = new Intent(requireContext(), EncryptorService.class);
                                        intent.putExtra("actionType", "gDriveDBU");
                                        EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                                        int i = EncryptorService.getUniqueID();
                                        intent.putExtra("index", i);
                                        intent.putExtra("pass", ((Explorer) requireActivity()).getIntent2().getByteArrayExtra("pass"));
                                        ContextCompat.startForegroundService(requireContext(), intent);
                                        MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_rounded);
                                        ProgressBar bar = new ProgressBar(requireContext());
                                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT);
                                        bar.setLayoutParams(lp);
                                        builder2.setTitle(R.string.wait);
                                        builder2.setMessage(R.string.uploadingDB);
                                        builder2.setView(bar);
                                        builder2.setCancelable(false);
                                        AlertDialog alertDialog = builder2.create();
                                        alertDialog.show();
                                        Thread thread = new Thread(() -> {
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {

                                                Thread.currentThread().interrupt();
                                            }
                                            while (EncryptorService.getDeletingFiles().containsValue(true)) {
                                                try {
                                                    Thread.sleep(100);
                                                } catch (InterruptedException e) {

                                                    Thread.currentThread().interrupt();
                                                }
                                            }
                                            requireActivity().runOnUiThread(() -> {
                                                alertDialog.dismiss();
                                                switch (EncryptorService.getBackupRestoreReturn()) {
                                                    case 0:
                                                        Snackbar.make(requireView(), R.string.backupSuccess, Snackbar.LENGTH_LONG).show();
                                                        break;
                                                    case 2:
                                                        Snackbar.make(requireView(), R.string.databaseNotFound, Snackbar.LENGTH_LONG).show();
                                                        EncryptorService.setBackupRestoreReturn(0);
                                                        break;
                                                    case 3:
                                                        Snackbar.make(requireView(), R.string.smthWentWrong, Snackbar.LENGTH_LONG).show();
                                                        EncryptorService.setBackupRestoreReturn(0);
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            });
                                        });
                                        thread.start();
                                    })
                                    .setNegativeButton(R.string.cancel, (dialog1, which1) -> {
                                    });
                            builder1.show();
                            break;
                        case 1:
                            MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                                    .setTitle(R.string.choose)
                                    .setMessage(R.string.replaceDatabaseQ)
                                    .setPositiveButton(R.string.merge, (dialog12, which12) -> {
                                        Intent intent = new Intent(requireContext(), EncryptorService.class);
                                        intent.putExtra("actionType", "gDriveDBD");
                                        EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                                        int i = EncryptorService.getUniqueID();
                                        intent.putExtra("index", i);
                                        intent.putExtra("mergeData", true);
                                        intent.putExtra("pass", ((Explorer) requireActivity()).getIntent2().getByteArrayExtra("pass"));
                                        ContextCompat.startForegroundService(requireContext(), intent);
                                        afterServiceStart();
                                    })
                                    .setNegativeButton(R.string.replace, (dialog1, which1) -> {
                                        Intent intent = new Intent(requireContext(), EncryptorService.class);
                                        intent.putExtra("actionType", "gDriveDBD");
                                        EncryptorService.setUniqueID(EncryptorService.getUniqueID() + 1);
                                        int i = EncryptorService.getUniqueID();
                                        intent.putExtra("index", i);
                                        intent.putExtra("mergeData", false);
                                        intent.putExtra("pass", ((Explorer) requireActivity()).getIntent2().getByteArrayExtra("pass"));
                                        ContextCompat.startForegroundService(requireContext(), intent);
                                        afterServiceStart();
                                    });
                            builder2.show();
                            break;
                    }
                });
        builder.show();
    }

    private void afterServiceStart() {
        MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded);
        ProgressBar bar = new ProgressBar(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bar.setLayoutParams(lp);
        builder2.setTitle(R.string.wait);
        builder2.setMessage(R.string.downloadingDataBase);
        builder2.setView(bar);
        builder2.setCancelable(false);
        AlertDialog alertDialog = builder2.create();
        alertDialog.show();
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
            }
            while (EncryptorService.getDeletingFiles().containsValue(true)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();
                }
            }
            requireActivity().runOnUiThread(() -> {
                alertDialog.dismiss();
                switch (EncryptorService.getBackupRestoreReturn()) {
                    case 0:
                        Snackbar.make(requireView(), R.string.databaseRestoreSuccess, Snackbar.LENGTH_LONG).show();
                        break;
                    case 1:
                        Snackbar.make(requireView(), R.string.databasePassNotMatch, Snackbar.LENGTH_LONG).show();
                        EncryptorService.setBackupRestoreReturn(0);
                        break;
                    case 2:
                        Snackbar.make(requireView(), R.string.noDBfoundInCloud, Snackbar.LENGTH_LONG).show();
                        EncryptorService.setBackupRestoreReturn(0);
                        break;
                    case 3:
                        Snackbar.make(requireView(), R.string.smthWentWrong, Snackbar.LENGTH_LONG).show();
                        EncryptorService.setBackupRestoreReturn(0);
                        break;
                    default:
                        break;
                }
            });
        });
        thread.start();
    }
}