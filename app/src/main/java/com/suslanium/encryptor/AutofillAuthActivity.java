package com.suslanium.encryptor;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
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
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;

import static android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT;
import static com.suslanium.encryptor.EncryptorAutofillService.checkForAppName;
import static com.suslanium.encryptor.EncryptorAutofillService.generateMaskedPass;

public class AutofillAuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean darkTheme = preferences.getBoolean("dark_Theme", false);
        if (darkTheme) setTheme(R.style.Theme_Encryptor_Dark);
        else setTheme(R.style.Theme_Encryptor_Light);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autofill_auth);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void checkForPermissionsPassword(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            checkPassword(v);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void checkPassword(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!EncryptorService.changingPassword) {
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1002) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!EncryptorService.changingPassword) {
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
                } catch (Exception ignored) {
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void login(String password) {
        byte[] pass;
        try {
            pass = Encryptor.rsaencrypt(password);
            int type = getIntent().getIntExtra("TYPE", 0);
            ArrayList<AutofillId> ids = (ArrayList<AutofillId>) getIntent().getSerializableExtra("IDS");
            String appPackageName = getIntent().getStringExtra("PACKAGE");
            String appName;
            PackageManager pm = getApplicationContext().getPackageManager();
            ApplicationInfo ai;
            try {
                ai = pm.getApplicationInfo( this.getPackageName(), 0);
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
                                if (integerArrayListHashMap.get(i).get(1) != null) {
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
                                if (integerArrayListHashMap.get(i).get(2) != null) {
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
                                if (integerArrayListHashMap.get(i).get(1) != null) {
                                    Dataset.Builder builder = new Dataset.Builder();
                                    RemoteViews views = new RemoteViews(getPackageName(), R.layout.autofilllistitem);
                                    views.setTextViewText(R.id.textToSet, integerArrayListHashMap.get(i).get(0) + ": " + integerArrayListHashMap.get(i).get(1));
                                    for (int j = 0; j < ids.size(); j++) {
                                        builder.setValue(ids.get(j), AutofillValue.forText(integerArrayListHashMap.get(i).get(1)), views);
                                    }
                                    datasets.add(builder);
                                }
                                if (integerArrayListHashMap.get(i).get(2) != null) {
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
            FillResponse response = null;
            try {
                response = builder2.build();
            } catch (Exception e){
                Dataset.Builder builder = new Dataset.Builder();
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.autofilllistitem);
                views.setTextViewText(R.id.textToSet,getString(R.string.noResultsAutofill));
                builder.setValue(ids.get(0), AutofillValue.forText(""),views);
                builder2.addDataset(builder.build());
                response = builder2.build();
            }
            FillResponse finalResponse = response;
            runOnUiThread(() -> {
                Intent replyIntent = new Intent();
                EncryptorAutofillService.pass = pass;
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