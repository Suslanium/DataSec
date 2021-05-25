package com.suslanium.encryptor;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.autofill.AutofillManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

public class PasswordActivity extends AppCompatActivity {

    @Override
    public void onBackPressed() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(!preferences.getBoolean("setupComplete", false)){
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
        }
        boolean darkTheme = preferences.getBoolean("dark_Theme", false);
        if (darkTheme) setTheme(R.style.Theme_Encryptor_Dark);
        else setTheme(R.style.Theme_Encryptor_Light);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
    }

    public void checkForPermissionsPassword(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            checkPassword(v);
        }
    }

    public void checkPassword(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if(!EncryptorService.changingPassword) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + packageName));
                        startActivityForResult(intent, 1002);
                    } else {
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
                                        runOnUiThread(() -> Snackbar.make(v, R.string.wrongPass, Snackbar.LENGTH_LONG).show());
                                    }
                                }
                            } catch (Exception e) {
                                runOnUiThread(() -> Snackbar.make(v, R.string.wentWrongPass, Snackbar.LENGTH_LONG).show());
                            }
                        });
                        thread.start();
                    }
                } else {
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
                                    runOnUiThread(() -> Snackbar.make(v, R.string.wrongPass, Snackbar.LENGTH_LONG).show());
                                }
                            }
                        } catch (Exception e) {

                            runOnUiThread(() -> Snackbar.make(v, R.string.wentWrongPass, Snackbar.LENGTH_LONG).show());
                        }
                    });
                    thread.start();
                }
            } else {
                Snackbar.make(v, R.string.cannotUseWhileChanging, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1002) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if(!EncryptorService.changingPassword) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + packageName));
                        startActivityForResult(intent, 1002);
                    } else {
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
                                        runOnUiThread(() -> Snackbar.make(getCurrentFocus(), R.string.wrongPass, Snackbar.LENGTH_LONG).show());
                                    }
                                }
                            } catch (Exception e) {

                                runOnUiThread(() -> Snackbar.make(getCurrentFocus(), R.string.wentWrongPass, Snackbar.LENGTH_LONG).show());
                            }
                        });
                        thread.start();
                    }
                } else {
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
                                    runOnUiThread(() -> Snackbar.make(getCurrentFocus(), R.string.wrongPass, Snackbar.LENGTH_LONG).show());
                                }
                            }
                        } catch (Exception e) {

                            runOnUiThread(() -> Snackbar.make(getCurrentFocus(), R.string.wentWrongPass, Snackbar.LENGTH_LONG).show());
                        }
                    });
                    thread.start();
                }
            } else {
                try {
                    Snackbar.make(findViewById(R.id.welcomeText), R.string.cannotUseWhileChanging, Snackbar.LENGTH_LONG).show();
                } catch (Exception ignored){}
            }
        }
    }

    private void login(String password){
        byte[] pass;
        try {
            pass = Encryptor.rsaencrypt(password);
            runOnUiThread(() -> {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getSystemService(AutofillManager.class).isAutofillSupported() && getSystemService(AutofillManager.class).hasEnabledAutofillServices()){
                    EncryptorAutofillService.pass = pass;
                }
                Intent intent = new Intent(PasswordActivity.this, Explorer.class);
                intent.putExtra("pass", pass);
                startActivity(intent);
            });
        } catch (Exception e) {

        }
    }
}