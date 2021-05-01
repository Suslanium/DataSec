package com.suslanium.encryptor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class passwordChange extends AppCompatActivity {
    private String service = "";
    private String loginName = "";
    private String passName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dark_theme = preferences.getBoolean("dark_Theme", true);
        if(dark_theme) setTheme(R.style.Theme_MaterialComponents_NoActionBar);
        else setTheme(R.style.Theme_MaterialComponents_Light_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_change);
        Bundle b = getIntent().getExtras();
        int id = b.getInt("id");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = getIntent();
                    byte[] passEnc = intent.getByteArrayExtra("pass");
                    String password = Encryptor.rsadecrypt(passEnc);
                    SQLiteDatabase database = Encryptor.initDataBase(passwordChange.this, password);
                    HashMap<Integer, ArrayList<String>> listHashMap = Encryptor.readPasswordData(database);
                    ArrayList<String> strings = listHashMap.get(id);
                    Encryptor.closeDataBase(database);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onThreadDone(strings);
                        }
                    });
                } catch (Exception e){
                    e.printStackTrace();
                    finish();
                }
            }
        });
        thread.start();
        FloatingActionButton submit = findViewById(R.id.submit2);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextInputEditText name = findViewById(R.id.nameInput);
                TextInputEditText login = findViewById(R.id.loginInput);
                TextInputEditText pass = findViewById(R.id.passInput);
                if (!name.getText().toString().matches("")) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Intent intent = getIntent();
                                byte[] passEnc = intent.getByteArrayExtra("pass");
                                String password = Encryptor.rsadecrypt(passEnc);
                                SQLiteDatabase database = Encryptor.initDataBase(passwordChange.this, password);
                                Encryptor.updateDataIntoPasswordTable(database, id, name.getText().toString(), login.getText().toString(), pass.getText().toString());
                                Encryptor.closeDataBase(database);
                                finish();
                            } catch (Exception e){
                                e.printStackTrace();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Snackbar.make(v, "Failed to update entry.", Snackbar.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    });
                    thread.start();
                } else {
                    Snackbar.make(v, "Please fill data", Snackbar.LENGTH_LONG).show();
                }
            }
        });
        FloatingActionButton cancel = findViewById(R.id.cancel2);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextInputEditText name = findViewById(R.id.nameInput);
                TextInputEditText login = findViewById(R.id.loginInput);
                TextInputEditText pass = findViewById(R.id.passInput);
                if (!name.getText().toString().matches(service) || !login.getText().toString().matches(loginName) || !pass.getText().toString().matches(passName)) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(passwordChange.this, R.style.MaterialAlertDialog_rounded)
                            .setTitle("Discard changes?")
                            .setMessage("You have some unsaved changes. Do you want to discard them?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    finish();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    builder.show();
                } else finish();
            }
        });
        FloatingActionButton delete = findViewById(R.id.delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(passwordChange.this, R.style.MaterialAlertDialog_rounded)
                        .setTitle("Are you sure?")
                        .setMessage("You are going to delete this entry. Do you wish to proceed?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Intent intent = getIntent();
                                            byte[] passEnc = intent.getByteArrayExtra("pass");
                                            String password = Encryptor.rsadecrypt(passEnc);
                                            SQLiteDatabase database = Encryptor.initDataBase(passwordChange.this, password);
                                            Encryptor.deleteDataFromPasswordTable(database, id);
                                            Encryptor.closeDataBase(database);
                                            finish();
                                        } catch (Exception e){
                                            e.printStackTrace();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Snackbar.make(v, "Failed to delete entry.", Snackbar.LENGTH_LONG).show();
                                                }
                                            });
                                        }
                                    }
                                });
                                thread.start();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
            }
        });
    }

    private void onThreadDone(ArrayList<String> strings) {
        TextInputEditText name = findViewById(R.id.nameInput);
        TextInputEditText login = findViewById(R.id.loginInput);
        TextInputEditText pass = findViewById(R.id.passInput);
        service = strings.get(0);
        name.setText(strings.get(0));
        loginName = strings.get(1);
        login.setText(strings.get(1));
        passName = strings.get(2);
        pass.setText(strings.get(2));
    }

    @Override
    public void onBackPressed() {
        TextInputEditText name = findViewById(R.id.nameInput);
        TextInputEditText login = findViewById(R.id.loginInput);
        TextInputEditText pass = findViewById(R.id.passInput);
        if (!name.getText().toString().matches(service) || !login.getText().toString().matches(loginName) || !pass.getText().toString().matches(passName)) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(passwordChange.this, R.style.MaterialAlertDialog_rounded)
                    .setTitle("Discard changes?")
                    .setMessage("You have some unsaved changes. Do you want to discard them?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.show();
        } else finish();
    }
}