package com.suslanium.encryptor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import net.sqlcipher.database.SQLiteDatabase;

public class passwordAdd extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_add);
        FloatingActionButton submit = findViewById(R.id.submit);
        FloatingActionButton cancel = findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextInputEditText name = findViewById(R.id.nameInput2);
                TextInputEditText login = findViewById(R.id.loginInput2);
                TextInputEditText pass = findViewById(R.id.passInput2);
                if(!name.getText().toString().matches("") || !login.getText().toString().matches("") || !pass.getText().toString().matches("")){
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(passwordAdd.this, R.style.MaterialAlertDialog_rounded)
                            .setTitle("Discard entry?")
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
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextInputEditText name = findViewById(R.id.nameInput2);
                TextInputEditText login = findViewById(R.id.loginInput2);
                TextInputEditText pass = findViewById(R.id.passInput2);
                if(!name.getText().toString().matches("")) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SQLiteDatabase database = Encryptor.initDataBase(passwordAdd.this, "password");
                            Encryptor.insertDataIntoPasswordTable(database, name.getText().toString(), login.getText().toString(), pass.getText().toString());
                            Encryptor.closeDataBase(database);
                        }
                    });
                    thread.start();
                    finish();
                }
                else {
                    Snackbar.make(v, "Please fill data", Snackbar.LENGTH_LONG).show();
                }
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