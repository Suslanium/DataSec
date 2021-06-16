package com.suslanium.encryptor.ui.notebook;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.suslanium.encryptor.R;

import java.io.File;

public class NotebookActivity extends AppCompatActivity {

    private String originText = "";
    private String originName = "";
    private TextInputEditText txt;
    private TextInputEditText name;
    private NotebookViewModel viewModel;

    @Override
    protected void onStart() {
        super.onStart();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.note);
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.backarrow);
        actionBar.setHomeAsUpIndicator(drawable);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onBackPressed() {
        if(txt != null && name != null){
            if(!txt.getText().toString().equals(originText) || !name.getText().toString().equals(originName)){
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.discardEntryText)
                        .setPositiveButton(R.string.yes, (dialog, which) -> NotebookActivity.super.onBackPressed())
                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                builder.show();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())).get(NotebookViewModel.class);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dark_theme = preferences.getBoolean("dark_Theme", false);
        if (dark_theme) setTheme(R.style.Theme_Encryptor_Dark_ActionBar);
        else setTheme(R.style.Theme_Encryptor_Light_ActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notebook);
        Intent intent = getIntent();
        FloatingActionButton delete = findViewById(R.id.deleteNote);
        FloatingActionButton save = findViewById(R.id.saveNote);
        txt = findViewById(R.id.noteTextField);
        name = findViewById(R.id.noteNameField);
        if (!intent.getBooleanExtra("newNote", false)) {
            MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded);
            ProgressBar bar = new ProgressBar(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            bar.setLayoutParams(lp);
            builder2.setTitle(R.string.wait);
            builder2.setView(bar);
            builder2.setCancelable(false);
            AlertDialog alertDialog = builder2.create();
            alertDialog.show();
            Thread thread = new Thread(() -> {
                String fileName = intent.getStringExtra("fileName");
                byte[] pass = intent.getByteArrayExtra("pass");
                try {
                    String text = viewModel.readText(fileName, pass);
                    runOnUiThread(() -> {
                        alertDialog.dismiss();
                        txt.setText(text);
                        originText = txt.getText().toString();
                        name.setText(fileName.substring(0, fileName.indexOf(".")));
                        originName = name.getText().toString();
                        delete.setOnClickListener(v -> {
                            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(NotebookActivity.this, R.style.MaterialAlertDialog_rounded)
                                    .setTitle(R.string.warning)
                                    .setMessage(R.string.deleteNoteMsg)
                                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                                        viewModel.getOriginEncNote().delete();
                                        finish();
                                    })
                                    .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                            builder.show();
                        });
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    alertDialog.dismiss();
                    finish();
                }
            });
            thread.start();
        } else {
            delete.setVisibility(View.GONE);
        }
        save.setOnClickListener(v -> {
            if(!txt.getText().toString().matches("") && !name.getText().toString().matches("")) {
                MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(NotebookActivity.this, R.style.MaterialAlertDialog_rounded);
                ProgressBar bar = new ProgressBar(NotebookActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                bar.setLayoutParams(lp);
                builder2.setTitle(R.string.wait);
                builder2.setView(bar);
                builder2.setCancelable(false);
                AlertDialog alertDialog = builder2.create();
                alertDialog.show();
                File tempNote = new File(getApplicationInfo().dataDir + File.separator + "noteTemp" + File.separator + name.getText().toString() + ".txt");
                try {
                    viewModel.checkFileValidName(tempNote);
                    Thread thread = new Thread(() -> {
                        try {
                            byte[] pass = intent.getByteArrayExtra("pass");
                            tempNote.getParentFile().mkdirs();
                            File encNote = new File(getFilesDir().getPath() + File.separator + "Notes" + File.separator + name.getText().toString() + ".txt.enc");
                            if (encNote.exists() && intent.getBooleanExtra("newNote", false)) {
                                runOnUiThread(() -> {
                                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(NotebookActivity.this, R.style.MaterialAlertDialog_rounded);
                                    builder.setTitle(R.string.warning);
                                    builder.setMessage(R.string.noteAlreadyExists);
                                    builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                                        Thread thread1 = new Thread(() -> {
                                            try {
                                                viewModel.saveNote(tempNote,encNote, pass, txt.getText().toString(), false);
                                                runOnUiThread(() -> {
                                                    alertDialog.dismiss();
                                                    finish();
                                                });
                                            } catch (Exception e) {
                                                runOnUiThread(() -> {
                                                    alertDialog.dismiss();
                                                    Snackbar.make(v, R.string.failedToSaveNote, Snackbar.LENGTH_LONG).show();
                                                });
                                            }
                                        });
                                        thread1.start();
                                    });
                                    builder.setNegativeButton(R.string.no, (dialog, which) -> {
                                        alertDialog.dismiss();
                                        dialog.dismiss();
                                    });
                                    builder.show();
                                });
                            } else {
                                viewModel.saveNote(tempNote,encNote, pass, txt.getText().toString(), true);
                                runOnUiThread(() -> {
                                    alertDialog.dismiss();
                                    finish();
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                alertDialog.dismiss();
                                Snackbar.make(v, R.string.failedToSaveNote, Snackbar.LENGTH_LONG).show();
                            });
                        }
                    });
                    thread.start();
                } catch (Exception e){
                    alertDialog.dismiss();
                    Snackbar.make(v, R.string.noteInvalidName, Snackbar.LENGTH_LONG).show();
                }
            } else {
                Snackbar.make(v, R.string.enterNote, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}