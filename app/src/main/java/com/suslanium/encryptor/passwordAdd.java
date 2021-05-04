package com.suslanium.encryptor;

import android.R.attr;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class passwordAdd extends AppCompatActivity {

    private byte[] image = null;
    public static final int PICK_IMAGE = 1;
    private ImageView addServiceIcon;
    private TextInputEditText name;
    private TextInputEditText login;
    private TextInputEditText pass;
    private TextInputEditText website;
    private TextInputEditText notes;
    private FloatingActionButton cancel;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return;
            }
            Thread thread = new Thread(() -> {
                try (InputStream inputStream = passwordAdd.this.getContentResolver().openInputStream(data.getData())){
                    Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(inputStream), 256,256);
                    ByteBuffer byteBuffer = ByteBuffer.allocate(thumbnail.getByteCount());
                    thumbnail.copyPixelsToBuffer(byteBuffer);
                    image = byteBuffer.array();
                    passwordAdd.this.runOnUiThread(() -> addServiceIcon.setImageBitmap(thumbnail));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dark_theme = preferences.getBoolean("dark_Theme", true);
        if(dark_theme) setTheme(R.style.Theme_MaterialComponents_NoActionBar);
        else setTheme(R.style.Theme_MaterialComponents_Light_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_add);
        addServiceIcon = findViewById(R.id.serviceAddIcon);
        addServiceIcon.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select icon"), PICK_IMAGE);
        });
        name = findViewById(R.id.nameInput2);
        login = findViewById(R.id.loginInput2);
        pass = findViewById(R.id.passInput2);
        website = findViewById(R.id.websiteInput2);
        notes = findViewById(R.id.noteInput2);
        FloatingActionButton submit = findViewById(R.id.submit);
        cancel = findViewById(R.id.cancel);
        cancel.setOnClickListener(v -> {
            if(!name.getText().toString().matches("") || !login.getText().toString().matches("") || !pass.getText().toString().matches("") || !website.getText().toString().matches("") || !notes.getText().toString().matches("")){
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
            if(!name.getText().toString().matches("")) {
                Thread thread = new Thread(() -> {
                    try {
                        Intent intent = getIntent();
                        byte[] passEnc = intent.getByteArrayExtra("pass");
                        String password = Encryptor.rsadecrypt(passEnc);
                        SQLiteDatabase database = Encryptor.initDataBase(passwordAdd.this, password);
                        Encryptor.insertDataIntoPasswordTable(database, name.getText().toString(), login.getText().toString(), pass.getText().toString(), image, website.getText().toString(), notes.getText().toString());
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
        TextInputLayout nameLayout = findViewById(R.id.name);
        nameLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        nameLayout.setEndIconDrawable(R.drawable.copysmall);
        nameLayout.setEndIconOnClickListener(v -> {
            if(!name.getText().toString().matches("")) {
                ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Encryptor", name.getText().toString());
                if (clipboard == null || clip == null) return;
                clipboard.setPrimaryClip(clip);
                Snackbar.make(v, "Title copied to clipboard!", Snackbar.LENGTH_LONG).show();
            }
        });
        TextInputLayout passLayout = findViewById(R.id.password);
        passLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        passLayout.setEndIconDrawable(R.drawable.copysmall);
        passLayout.setEndIconOnClickListener(v -> {
            if(!pass.getText().toString().matches("")) {
                ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Encryptor", pass.getText().toString());
                if (clipboard == null || clip == null) return;
                clipboard.setPrimaryClip(clip);
                Snackbar.make(v, "Password copied to clipboard!", Snackbar.LENGTH_LONG).show();
            }
        });
        TextInputLayout loginLayout = findViewById(R.id.login);
        loginLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        loginLayout.setEndIconDrawable(R.drawable.copysmall);
        loginLayout.setEndIconOnClickListener(v -> {
            if(!login.getText().toString().matches("")) {
                ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Encryptor", login.getText().toString());
                if (clipboard == null || clip == null) return;
                clipboard.setPrimaryClip(clip);
                Snackbar.make(v, "Login copied to clipboard!", Snackbar.LENGTH_LONG).show();
            }
        });
        TextInputLayout websiteLayout = findViewById(R.id.website);
        websiteLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        websiteLayout.setEndIconDrawable(R.drawable.browsericon);
        websiteLayout.setEndIconOnClickListener(v -> {
            if(!website.getText().toString().matches("")) {
                try {
                    String s = URLUtil.guessUrl(website.getText().toString());
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Snackbar.make(v, "Enter a valid URL, please.", Snackbar.LENGTH_LONG).show();
                }
            }
        });
        TextInputLayout noteLayout = findViewById(R.id.notes);
        noteLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        noteLayout.setEndIconDrawable(R.drawable.copysmall);
        noteLayout.setEndIconOnClickListener(v -> {
            if(!notes.getText().toString().matches("")) {
                ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Encryptor", notes.getText().toString());
                if (clipboard == null || clip == null) return;
                clipboard.setPrimaryClip(clip);
                Snackbar.make(v, "Note copied to clipboard!", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        cancel.performClick();
    }

    private static int calculatePasswordStrength(String password){

        int iPasswordScore = 0;

        if( password.length() < 8 )
            return 0;
        else if( password.length() >= 10 )
            iPasswordScore += 2;
        else
            iPasswordScore += 1;

        /*
         * if password contains 2 digits, add 2 to score.
         * if contains 1 digit add 1 to score
         */
        if( password.matches("(?=.*[0-9].*[0-9]).*") )
            iPasswordScore += 2;
        else if ( password.matches("(?=.*[0-9]).*") )
            iPasswordScore += 1;

        //if password contains 1 lower case letter, add 2 to score
        if( password.matches("(?=.*[a-z]).*") )
            iPasswordScore += 2;

        /*
         * if password contains 2 upper case letters, add 2 to score.
         * if contains only 1 then add 1 to score.
         */
        if( password.matches("(?=.*[A-Z].*[A-Z]).*") )
            iPasswordScore += 2;
        else if( password.matches("(?=.*[A-Z]).*") )
            iPasswordScore += 1;

        /*
         * if password contains 2 special characters, add 2 to score.
         * if contains only 1 special character then add 1 to score.
         */
        if( password.matches("(?=.*[~!@#$%^&*()_-].*[~!@#$%^&*()_-]).*") )
            iPasswordScore += 2;
        else if( password.matches("(?=.*[~!@#$%^&*()_-]).*") )
            iPasswordScore += 1;

        return iPasswordScore;

    }
}