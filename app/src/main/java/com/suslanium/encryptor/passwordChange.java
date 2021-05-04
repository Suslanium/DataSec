package com.suslanium.encryptor;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PathEffect;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ImageView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import static com.suslanium.encryptor.passwordAdd.PICK_IMAGE;

public class passwordChange extends AppCompatActivity {
    private String service = "";
    private String loginName = "";
    private String passName = "";
    private String websiteName = "";
    private String notesName = "";
    private byte[] image = null;
    private ImageView icon;
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
                try (InputStream inputStream = passwordChange.this.getContentResolver().openInputStream(data.getData())){
                    Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(inputStream), 256,256);
                    ByteBuffer byteBuffer = ByteBuffer.allocate(thumbnail.getByteCount());
                    thumbnail.copyPixelsToBuffer(byteBuffer);
                    image = byteBuffer.array();
                    passwordChange.this.runOnUiThread(() -> icon.setImageBitmap(thumbnail));
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
        setContentView(R.layout.activity_password_change);
        Bundle b = getIntent().getExtras();
        int id = b.getInt("id");
        icon = findViewById(R.id.serviceChangeIcon);
        Thread thread = new Thread(() -> {
            try {
                Intent intent = getIntent();
                byte[] passEnc = intent.getByteArrayExtra("pass");
                String password = Encryptor.rsadecrypt(passEnc);
                SQLiteDatabase database = Encryptor.initDataBase(passwordChange.this, password);
                HashMap<Integer, ArrayList<String>> listHashMap = Encryptor.readPasswordData(database);
                ArrayList<String> strings = listHashMap.get(id);
                runOnUiThread(() -> onThreadDone(strings));
                HashMap<Integer, byte[]> iconsHashMap = Encryptor.readPasswordIcons(database);
                image = iconsHashMap.get(id);
                if(image != null){
                    Bitmap.Config config = Bitmap.Config.ARGB_8888;
                    Bitmap bitmap = Bitmap.createBitmap(256,256, config);
                    ByteBuffer byteBuffer = ByteBuffer.wrap(image);
                    bitmap.copyPixelsFromBuffer(byteBuffer);
                    passwordChange.this.runOnUiThread(() -> icon.setImageBitmap(bitmap));
                }
                Encryptor.closeDataBase(database);
            } catch (Exception e){
                e.printStackTrace();
                finish();
            }
        });
        thread.start();
        icon.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select icon"), PICK_IMAGE);
        });
        name = findViewById(R.id.nameInput);
        login = findViewById(R.id.loginInput);
        pass = findViewById(R.id.passInput);
        website = findViewById(R.id.websiteInput);
        notes = findViewById(R.id.notesInput);
        FloatingActionButton submit = findViewById(R.id.submit2);
        submit.setOnClickListener(v -> {
            if (!name.getText().toString().matches("")) {
                Thread thread1 = new Thread(() -> {
                    try {
                        Intent intent = getIntent();
                        byte[] passEnc = intent.getByteArrayExtra("pass");
                        String password = Encryptor.rsadecrypt(passEnc);
                        SQLiteDatabase database = Encryptor.initDataBase(passwordChange.this, password);
                        Encryptor.updateDataIntoPasswordTable(database, id, name.getText().toString(), login.getText().toString(), pass.getText().toString(), image,website.getText().toString(),notes.getText().toString());
                        Encryptor.closeDataBase(database);
                        finish();
                    } catch (Exception e){
                        e.printStackTrace();
                        runOnUiThread(() -> Snackbar.make(v, "Failed to update entry.", Snackbar.LENGTH_LONG).show());
                    }
                });
                thread1.start();
            } else {
                Snackbar.make(v, "Please fill data", Snackbar.LENGTH_LONG).show();
            }
        });
        cancel = findViewById(R.id.cancel2);
        cancel.setOnClickListener(v -> {
            if (!name.getText().toString().matches(Pattern.quote(service)) || !login.getText().toString().matches(Pattern.quote(loginName)) || !pass.getText().toString().matches(Pattern.quote(passName)) || !website.getText().toString().matches(Pattern.quote(websiteName)) || !notes.getText().toString().matches(Pattern.quote(notesName))) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(passwordChange.this, R.style.MaterialAlertDialog_rounded)
                        .setTitle("Discard changes?")
                        .setMessage("You have some unsaved changes. Do you want to discard them?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                builder.show();
            } else finish();
        });
        FloatingActionButton delete = findViewById(R.id.delete);
        delete.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(passwordChange.this, R.style.MaterialAlertDialog_rounded)
                    .setTitle("Are you sure?")
                    .setMessage("You are going to delete this entry. Do you wish to proceed?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Thread thread12 = new Thread(() -> {
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
                                runOnUiThread(() -> Snackbar.make(v, "Failed to delete entry.", Snackbar.LENGTH_LONG).show());
                            }
                        });
                        thread12.start();
                        dialog.dismiss();
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss());
            builder.show();
        });
        TextInputLayout nameLayout = findViewById(R.id.name1);
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
        TextInputLayout passLayout = findViewById(R.id.pass1);
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
        TextInputLayout loginLayout = findViewById(R.id.login1);
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
        TextInputLayout websiteLayout = findViewById(R.id.website1);
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
        TextInputLayout noteLayout = findViewById(R.id.notes1);
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

    private void onThreadDone(ArrayList<String> strings) {
        service = strings.get(0);
        name.setText(service);
        loginName = strings.get(1);
        login.setText(loginName);
        passName = strings.get(2);
        pass.setText(passName);
        websiteName = strings.get(3);
        website.setText(websiteName);
        notesName = strings.get(4);
        notes.setText(notesName);
    }

    @Override
    public void onBackPressed() {
        cancel.performClick();
    }
}