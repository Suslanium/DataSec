package com.suslanium.encryptor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import net.sqlcipher.database.SQLiteDatabase;

public class passwordAdd extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dark_theme = preferences.getBoolean("dark_Theme", true);
        if(dark_theme) setTheme(R.style.Theme_MaterialComponents_NoActionBar);
        else setTheme(R.style.Theme_MaterialComponents_Light_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_add);
        FloatingActionButton submit = findViewById(R.id.submit);
        FloatingActionButton cancel = findViewById(R.id.cancel);
        cancel.setOnClickListener(v -> {
            TextInputEditText name = findViewById(R.id.nameInput2);
            TextInputEditText login = findViewById(R.id.loginInput2);
            TextInputEditText pass = findViewById(R.id.passInput2);
            if(!name.getText().toString().matches("") || !login.getText().toString().matches("") || !pass.getText().toString().matches("")){
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(passwordAdd.this, R.style.MaterialAlertDialog_rounded)
                        .setTitle("Discard entry?")
                        .setMessage("You have some unsaved changes. Do you want to discard them?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                builder.show();
            } else finish();
        });
        submit.setOnClickListener(v -> {
            TextInputEditText name = findViewById(R.id.nameInput2);
            TextInputEditText login = findViewById(R.id.loginInput2);
            TextInputEditText pass = findViewById(R.id.passInput2);
            if(!name.getText().toString().matches("")) {
                Thread thread = new Thread(() -> {
                    try {
                        Intent intent = getIntent();
                        byte[] passEnc = intent.getByteArrayExtra("pass");
                        String password = Encryptor.rsadecrypt(passEnc);
                        SQLiteDatabase database = Encryptor.initDataBase(passwordAdd.this, password);
                        Encryptor.insertDataIntoPasswordTable(database, name.getText().toString(), login.getText().toString(), pass.getText().toString());
                        Encryptor.closeDataBase(database);
                        finish();
                    } catch (Exception e){
                        e.printStackTrace();
                        runOnUiThread(() -> Snackbar.make(v, "Failed to add entry.", Snackbar.LENGTH_LONG).show());
                    }
                });
                thread.start();
            }
            else {
                Snackbar.make(v, "Please fill data", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        TextInputEditText name = findViewById(R.id.nameInput2);
        TextInputEditText login = findViewById(R.id.loginInput2);
        TextInputEditText pass = findViewById(R.id.passInput2);
        if(!name.getText().toString().matches("") || !login.getText().toString().matches("") || !pass.getText().toString().matches("")){
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(passwordAdd.this, R.style.MaterialAlertDialog_rounded)
                    .setTitle("Discard entry?")
                    .setMessage("You have some unsaved changes. Do you want to discard them?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss());
            builder.show();
        } else finish();
    }
}