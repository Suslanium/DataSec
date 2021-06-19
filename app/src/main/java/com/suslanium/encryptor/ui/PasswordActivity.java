package com.suslanium.encryptor.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.view.View;
import android.view.WindowManager;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.suslanium.encryptor.EncryptorAutofillService;
import com.suslanium.encryptor.EncryptorService;
import com.suslanium.encryptor.R;
import com.suslanium.encryptor.ui.welcomescreen.WelcomeActivity;
import com.suslanium.encryptor.util.Encryptor;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executor;

import static android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT;
import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static com.suslanium.encryptor.EncryptorAutofillService.checkForAppName;
import static com.suslanium.encryptor.EncryptorAutofillService.generateMaskedPass;

public class PasswordActivity extends AppCompatActivity {

    private boolean isAuthActivity = false;
    private boolean usesBioAuth = false;

    @Override
    public void onBackPressed() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean("setupComplete", false)) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
        }
        if(getIntent().getIntExtra("TYPE", 5) != 5) isAuthActivity = true;
        boolean darkTheme = preferences.getBoolean("dark_Theme", false);
        if (darkTheme) setTheme(R.style.Theme_Encryptor_Dark);
        else setTheme(R.style.Theme_Encryptor_Light);
        usesBioAuth = preferences.getBoolean("usesBioAuth", false);
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                break;
            default:
                usesBioAuth = false;
                break;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
    }

    @Override
    protected void onStart() {
        super.onStart();
        TextView bottom = findViewById(R.id.welcomeBottomText);
        Button loginButton = findViewById(R.id.loginButton);
        if(isAuthActivity){ bottom.setText(R.string.enterPassAuth); loginButton.setText(R.string.cont);}
        if(usesBioAuth){
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || pm.isIgnoringBatteryOptimizations(packageName)) {
                    BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                            .setTitle(getString(R.string.bioAuth))
                            .setSubtitle(getString(R.string.loginBio))
                            .setNegativeButtonText(getString(R.string.usePass))
                            .build();
                    Executor executor = ContextCompat.getMainExecutor(this);
                    BiometricPrompt biometricPrompt = new BiometricPrompt(PasswordActivity.this,
                            executor, new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                        }

                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                            try {
                                MasterKey mainKey = new MasterKey.Builder(getBaseContext())
                                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                        .build();
                                SharedPreferences editor = EncryptedSharedPreferences.create(getBaseContext(), "encryptor_shared_prefs", mainKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                                String pass = editor.getString("pass", null);
                                if(pass != null){
                                    login(pass);
                                } else {
                                    Snackbar.make(getCurrentFocus(), R.string.smthWentWrong, Snackbar.LENGTH_LONG).show();
                                }
                            } catch (Exception e){
                                Snackbar.make(getCurrentFocus(), R.string.smthWentWrong, Snackbar.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                        }
                    });
                    biometricPrompt.authenticate(promptInfo);
                }
            }
        }
    }

    public void checkForPermissionsPassword(View v) {
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(packageName)) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivityForResult(intent, 1002);
        } else {
            checkPassword(v);
        }
    }

    private void checkPassword(View v) {
        if (!EncryptorService.isChangingPassword()) {
            startLogin();
        } else {
            Snackbar.make(v, R.string.cannotUseWhileChanging, Snackbar.LENGTH_LONG).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1002) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!EncryptorService.isChangingPassword()) {
                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                        startLogin();
                    }
            } else {
                try {
                    Snackbar.make(findViewById(R.id.welcomeText), R.string.cannotUseWhileChanging, Snackbar.LENGTH_LONG).show();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void startLogin(){
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        TextInputLayout text = findViewById(R.id.textInputLayout);
        String password = text.getEditText().getText().toString();
        Thread thread = new Thread(() -> {
            try {
                MasterKey mainKey = new MasterKey.Builder(getBaseContext())
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
                SharedPreferences editor = EncryptedSharedPreferences.create(getBaseContext(), "encryptor_shared_prefs", mainKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                String passHash = editor.getString("passHash", null);
                if (passHash == null) {
                    SharedPreferences.Editor edit = editor.edit();
                    edit.putString("passHash", Encryptor.calculateHash(password, "SHA-512"));
                    edit.apply();
                    login(password);
                } else {
                    String passwordHash = Encryptor.calculateHash(password, "SHA-512");
                    if (passwordHash.equals(passHash)) {
                        login(password);
                    } else {
                        runOnUiThread(() -> {Snackbar.make(getCurrentFocus(), R.string.wrongPass, Snackbar.LENGTH_LONG).show();getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);});
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> {Snackbar.make(getCurrentFocus(), R.string.wentWrongPass, Snackbar.LENGTH_LONG).show();getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);});
            }
        });
        thread.start();
    }

    private void login(String password) {
        byte[] pass;
        runOnUiThread(() -> getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE));
        if(!isAuthActivity) {
            try {
                pass = Encryptor.rsaencrypt(password);
                runOnUiThread(() -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getSystemService(AutofillManager.class).isAutofillSupported() && getSystemService(AutofillManager.class).hasEnabledAutofillServices()) {
                        EncryptorAutofillService.setPass(pass);
                    }
                    Intent intent = new Intent(PasswordActivity.this, Explorer.class);
                    intent.putExtra("pass", pass);
                    startActivity(intent);
                });
            } catch (Exception ignored) {
            }
        } else {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    pass = Encryptor.rsaencrypt(password);
                    int type = getIntent().getIntExtra("TYPE", 0);
                    ArrayList<AutofillId> ids = (ArrayList<AutofillId>) getIntent().getSerializableExtra("IDS");
                    String appPackageName = getIntent().getStringExtra("PACKAGE");
                    String appName;
                    PackageManager pm = getApplicationContext().getPackageManager();
                    ApplicationInfo ai;
                    try {
                        ai = pm.getApplicationInfo(this.getPackageName(), 0);
                    } catch (final PackageManager.NameNotFoundException e) {
                        ai = null;
                    }
                    appName = (String) (ai != null ? pm.getApplicationLabel(ai) : "Null");
                    ArrayList<String> texts = getIntent().getStringArrayListExtra("TEXTS");
                    SQLiteDatabase database = Encryptor.initDataBase(getBaseContext(), password);
                    HashMap<Integer, ArrayList<String>> integerArrayListHashMap = Encryptor.readPasswordData(database);
                    Encryptor.closeDataBase(database);
                    ArrayList<Integer> integers = new ArrayList<>();
                    for (Integer i : integerArrayListHashMap.keySet()) {
                        if (appPackageName.toLowerCase().contains(integerArrayListHashMap.get(i).get(0).toLowerCase()) || checkForAppName(texts, integerArrayListHashMap.get(i).get(0)) || appName.contains(integerArrayListHashMap.get(i).get(0))) {
                            integers.add(i);
                        }
                    }
                    FillResponse.Builder builder2 = new FillResponse.Builder();
                    if (!integers.isEmpty()) {
                        switch (type) {
                            case 2:
                                if (!ids.isEmpty()) {
                                    ArrayList<Dataset.Builder> datasets = new ArrayList<>();
                                    for (Integer i : integers) {
                                        if (integerArrayListHashMap.get(i).get(1) != null && !integerArrayListHashMap.get(i).get(1).matches("")) {
                                            Dataset.Builder builder = new Dataset.Builder();
                                            RemoteViews views = new RemoteViews(getPackageName(), R.layout.autofilllistitem);
                                            views.setTextViewText(R.id.textToSet, integerArrayListHashMap.get(i).get(0) + ": " + integerArrayListHashMap.get(i).get(1));
                                            for (int j = 0; j < ids.size(); j++) {
                                                builder.setValue(ids.get(j), AutofillValue.forText(integerArrayListHashMap.get(i).get(1)), views);
                                            }
                                            datasets.add(builder);
                                        }
                                    }
                                    if (!datasets.isEmpty()) {
                                        for (int i = 0; i < datasets.size(); i++) {
                                            builder2.addDataset(datasets.get(i).build());
                                        }
                                    }
                                } else {
                                    return;
                                }
                                break;
                            case 3:
                                if (!ids.isEmpty()) {
                                    ArrayList<Dataset.Builder> datasets = new ArrayList<>();
                                    for (Integer i : integers) {
                                        if (integerArrayListHashMap.get(i).get(2) != null && !integerArrayListHashMap.get(i).get(2).matches("")) {
                                            Dataset.Builder builder = new Dataset.Builder();
                                            RemoteViews views = new RemoteViews(getPackageName(), R.layout.autofilllistitem);
                                            views.setTextViewText(R.id.textToSet, integerArrayListHashMap.get(i).get(0) + ": " + generateMaskedPass(integerArrayListHashMap.get(i).get(2).length()));
                                            for (int j = 0; j < ids.size(); j++) {
                                                builder.setValue(ids.get(j), AutofillValue.forText(integerArrayListHashMap.get(i).get(2)), views);
                                            }
                                            datasets.add(builder);
                                        }
                                    }
                                    if (!datasets.isEmpty()) {
                                        for (int i = 0; i < datasets.size(); i++) {
                                            builder2.addDataset(datasets.get(i).build());
                                        }
                                    }
                                } else {
                                    return;
                                }
                                break;
                            case 1:
                                if (!ids.isEmpty()) {
                                    ArrayList<Dataset.Builder> datasets = new ArrayList<>();
                                    for (Integer i : integers) {
                                        if (integerArrayListHashMap.get(i).get(1) != null && !integerArrayListHashMap.get(i).get(1).matches("")) {
                                            Dataset.Builder builder = new Dataset.Builder();
                                            RemoteViews views = new RemoteViews(getPackageName(), R.layout.autofilllistitem);
                                            views.setTextViewText(R.id.textToSet, integerArrayListHashMap.get(i).get(0) + ": " + integerArrayListHashMap.get(i).get(1));
                                            for (int j = 0; j < ids.size(); j++) {
                                                builder.setValue(ids.get(j), AutofillValue.forText(integerArrayListHashMap.get(i).get(1)), views);
                                            }
                                            datasets.add(builder);
                                        }
                                        if (integerArrayListHashMap.get(i).get(2) != null && !integerArrayListHashMap.get(i).get(2).matches("")) {
                                            Dataset.Builder builder = new Dataset.Builder();
                                            RemoteViews views = new RemoteViews(getPackageName(), R.layout.autofilllistitem);
                                            views.setTextViewText(R.id.textToSet, integerArrayListHashMap.get(i).get(0) + ": " + generateMaskedPass(integerArrayListHashMap.get(i).get(2).length()));
                                            for (int j = 0; j < ids.size(); j++) {
                                                builder.setValue(ids.get(j), AutofillValue.forText(integerArrayListHashMap.get(i).get(2)), views);
                                            }
                                            datasets.add(builder);
                                        }
                                    }
                                    if (!datasets.isEmpty()) {
                                        for (int i = 0; i < datasets.size(); i++) {
                                            builder2.addDataset(datasets.get(i).build());
                                        }
                                    }
                                } else {
                                    return;
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    FillResponse response;
                    try {
                        response = builder2.build();
                    } catch (Exception e) {
                        Dataset.Builder builder = new Dataset.Builder();
                        RemoteViews views = new RemoteViews(getPackageName(), R.layout.autofilllistitem);
                        views.setTextViewText(R.id.textToSet, getString(R.string.noResultsAutofill));
                        builder.setValue(ids.get(0), AutofillValue.forText(""), views);
                        builder2.addDataset(builder.build());
                        response = builder2.build();
                    }
                    FillResponse finalResponse = response;
                    runOnUiThread(() -> {
                        Intent replyIntent = new Intent();
                        EncryptorAutofillService.setPass(pass);
                        replyIntent.putExtra(EXTRA_AUTHENTICATION_RESULT, finalResponse);
                        setResult(RESULT_OK, replyIntent);
                        finish();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    finish();
                }
            }
        }
    }
}