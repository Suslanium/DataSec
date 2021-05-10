package com.suslanium.encryptor;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PathEffect;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import static com.suslanium.encryptor.passwordAdd.PICK_IMAGE;
import static com.suslanium.encryptor.passwordAdd.calculatePasswordStrength;

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
    private int colorFrom = Color.parseColor("#FF0000");


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
                        if(image == null){
                            try {
                                URL imageURL = new URL("http://logo.clearbit.com/" + URLUtil.guessUrl(website.getText().toString()));
                                HttpURLConnection connection = (HttpURLConnection) imageURL.openConnection();
                                connection.setDoInput(true);
                                connection.connect();
                                Bitmap imageBitmap = BitmapFactory.decodeStream(connection.getInputStream());
                                Bitmap thumbnail = Bitmap.createScaledBitmap(imageBitmap, 256,256, false);
                                ByteBuffer byteBuffer = ByteBuffer.allocate(thumbnail.getByteCount());
                                thumbnail.copyPixelsToBuffer(byteBuffer);
                                image = byteBuffer.array();
                            } catch (Exception e){
                                e.printStackTrace();
                                image = null;
                            }
                        }
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
        /*passLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        passLayout.setEndIconDrawable(R.drawable.copysmall);
        passLayout.setEndIconOnClickListener(v -> {
            if(!pass.getText().toString().matches("")) {
                ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Encryptor", pass.getText().toString());
                if (clipboard == null || clip == null) return;
                clipboard.setPrimaryClip(clip);
                Snackbar.make(v, "Password copied to clipboard!", Snackbar.LENGTH_LONG).show();
            }
        });*/
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
        ProgressBar strength = findViewById(R.id.passwordStrengthBar2);
        strength.setMax(1000);
        strength.setProgressTintList(ColorStateList.valueOf(colorFrom));
        pass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                int passStrength = calculatePasswordStrength(s.toString());
                setPassBarProgress(passStrength * 100, strength);
                if (passStrength >= 8) {
                    passLayout.setHelperTextEnabled(true);
                    passLayout.setHelperText("Strong password");
                    setPassBarColor(Color.parseColor("#00FF00"), strength);
                } else if (passStrength >= 5) {
                    passLayout.setHelperTextEnabled(true);
                    passLayout.setHelperText("Medium password");
                    setPassBarColor(Color.parseColor("#FFFF00"), strength);
                } else if (passStrength <= 3) {
                    passLayout.setHelperTextEnabled(true);
                    passLayout.setHelperText("Weak password");
                    setPassBarColor(Color.parseColor("#FF0000"), strength);
                }
            }
        });
        Button generatePassword = findViewById(R.id.generatePassword2);
        generatePassword.setOnClickListener(v -> {
            final EditText input = new EditText(passwordChange.this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setSingleLine(true);
            input.setHint("Password length");
            final String[] passwordAlphabetMode = {""};
            CharSequence[] items = new CharSequence[]{"Uppercase letters", "Lowercase letters", "Numbers", "Special symbols"};
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(passwordChange.this, R.style.MaterialAlertDialog_rounded)
                    .setTitle("Generate password")
                    .setMultiChoiceItems(items, null, (dialog, which, isChecked) -> {
                        switch (which){
                            case 0:
                                if(isChecked){
                                    if(!passwordAlphabetMode[0].contains("U")){
                                        passwordAlphabetMode[0] = passwordAlphabetMode[0] +"U";
                                    }
                                } else {
                                    if(passwordAlphabetMode[0].contains("U")){
                                        passwordAlphabetMode[0] = passwordAlphabetMode[0].replace("U", "");
                                    }
                                }
                                break;
                            case 1:
                                if(isChecked){
                                    if(!passwordAlphabetMode[0].contains("L")){
                                        passwordAlphabetMode[0] = passwordAlphabetMode[0] +"L";
                                    }
                                } else {
                                    if(passwordAlphabetMode[0].contains("L")){
                                        passwordAlphabetMode[0] = passwordAlphabetMode[0].replace("L", "");
                                    }
                                }
                                break;
                            case 2:
                                if(isChecked){
                                    if(!passwordAlphabetMode[0].contains("N")){
                                        passwordAlphabetMode[0] = passwordAlphabetMode[0] +"N";
                                    }
                                } else {
                                    if(passwordAlphabetMode[0].contains("N")){
                                        passwordAlphabetMode[0] = passwordAlphabetMode[0].replace("N", "");
                                    }
                                }
                                break;
                            case 3:
                                if(isChecked){
                                    if(!passwordAlphabetMode[0].contains("S")){
                                        passwordAlphabetMode[0] = passwordAlphabetMode[0] +"S";
                                    }
                                } else {
                                    if(passwordAlphabetMode[0].contains("S")){
                                        passwordAlphabetMode[0] = passwordAlphabetMode[0].replace("S", "");
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    })
                    .setView(input)
                    .setPositiveButton("Generate", (dialog, which) -> {
                        try {
                            String lengthString = input.getText().toString();
                            if (!lengthString.matches("") && !passwordAlphabetMode[0].matches("")) {
                                int length = Integer.parseInt(lengthString);
                                String password = RndPassword.generateRandomPasswordStr(length, passwordAlphabetMode[0]);
                                pass.setText(password);
                            } else {
                                Snackbar.make(v, "Select options for password generation first.", Snackbar.LENGTH_LONG).show();
                            }
                        } catch (Exception e){
                            Snackbar.make(v, "Length is too big.", Snackbar.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {});
            builder.show();
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

    private void setPassBarProgress(int progress, ProgressBar strength) {
        ObjectAnimator animation = ObjectAnimator.ofInt(strength, "progress", strength.getProgress(), progress);
        animation.setDuration(400);
        animation.setAutoCancel(true);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    private void setPassBarColor(int color, ProgressBar strength) {
        int finalColorFrom = colorFrom;
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), finalColorFrom, color);
        colorAnimation.setDuration(400);
        colorAnimation.addUpdateListener(animator -> strength.setProgressTintList(ColorStateList.valueOf((int) animator.getAnimatedValue())));
        colorAnimation.start();
        colorFrom = color;
    }
}