package com.suslanium.encryptor;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.Base64;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;

public class PasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.setNavigationBarColor(Color.parseColor("#1976D2"));
        window.setStatusBarColor(Color.parseColor("#6200EA"));
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
            SharedPreferences editor1 = PreferenceManager.getDefaultSharedPreferences(this);
            if(!editor1.getBoolean("changingPassword", false)) {
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivityForResult(intent, 1002);
                } else {
                    TextInputLayout text = findViewById(R.id.textInputLayout);
                    String password = text.getEditText().getText().toString();
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String keyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                                SharedPreferences editor = EncryptedSharedPreferences.create("encryptor_shared_prefs", keyAlias, PasswordActivity.this, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
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
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Snackbar.make(v, "Wrong password!", Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Snackbar.make(v, "Oops, something went wrong! Try again.", Snackbar.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    });
                    thread.start();
                }
            } else {
                Snackbar.make(v, "Cannot use app while changing password", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1002) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivityForResult(intent, 1002);
            } else {
                TextInputLayout text = findViewById(R.id.textInputLayout);
                String password = text.getEditText().getText().toString();
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String keyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                            SharedPreferences editor = EncryptedSharedPreferences.create("encryptor_shared_prefs", keyAlias, PasswordActivity.this, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
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
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Snackbar.make(getCurrentFocus(), "Wrong password!", Snackbar.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            }
                        } catch (Exception e){
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Snackbar.make(getCurrentFocus(), "Oops, something went wrong! Try again.", Snackbar.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
                thread.start();
            }
        }
    }

    private void login(String password){
        byte[] pass;
        try {
            pass = Encryptor.RSAEncrypt(password);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(PasswordActivity.this, Explorer.class);
                    intent.putExtra("pass", pass);
                    startActivity(intent);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}