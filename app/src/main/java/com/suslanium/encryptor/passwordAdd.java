package com.suslanium.encryptor;

import android.R.attr;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

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
import java.net.HttpURLConnection;
import java.net.URL;
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
    private int colorFrom = Color.parseColor("#FF0000");
    private String weakPassword = "Weak password";
    private String mediumPassword = "Medium password";
    private String strongPassword = "Strong password";

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return;
            }
            Thread thread = new Thread(() -> {
                try (InputStream inputStream = passwordAdd.this.getContentResolver().openInputStream(data.getData())) {
                    Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(inputStream), 256, 256);
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
        if (dark_theme) setTheme(R.style.Theme_MaterialComponents_NoActionBar);
        else setTheme(R.style.Theme_MaterialComponents_Light_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_add);
        addServiceIcon = findViewById(R.id.serviceAddIcon);
        addServiceIcon.setOnClickListener(v -> {
            //Translation end 2
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.selectIcon)), PICK_IMAGE);
        });
        name = findViewById(R.id.nameInput2);
        login = findViewById(R.id.loginInput2);
        pass = findViewById(R.id.passInput2);
        website = findViewById(R.id.websiteInput2);
        notes = findViewById(R.id.noteInput2);
        FloatingActionButton submit = findViewById(R.id.submit);
        cancel = findViewById(R.id.cancel);
        weakPassword = getString(R.string.weakPassword);
        mediumPassword = getString(R.string.mediumPassword);
        strongPassword = getString(R.string.strongPassword);
        cancel.setOnClickListener(v -> {
            if (!name.getText().toString().matches("") || !login.getText().toString().matches("") || !pass.getText().toString().matches("") || !website.getText().toString().matches("") || !notes.getText().toString().matches("")) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(passwordAdd.this, R.style.MaterialAlertDialog_rounded)
                        .setTitle(R.string.discardEntry)
                        .setMessage(R.string.discardEntryText)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        })
                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                builder.show();
            } else finish();
        });
        submit.setOnClickListener(v -> {
            if (!name.getText().toString().matches("")) {
                Thread thread = new Thread(() -> {
                    try {
                        Intent intent = getIntent();
                        byte[] passEnc = intent.getByteArrayExtra("pass");
                        String password = Encryptor.rsadecrypt(passEnc);
                        SQLiteDatabase database = Encryptor.initDataBase(passwordAdd.this, password);
                        if(image == null){
                            try {
                                URL imageURL = new URL("https://logo.clearbit.com/" + URLUtil.guessUrl(website.getText().toString()));
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
                        Encryptor.insertDataIntoPasswordTable(database, name.getText().toString(), login.getText().toString(), pass.getText().toString(), image, website.getText().toString(), notes.getText().toString(), intent.getStringExtra("category"));
                        Encryptor.closeDataBase(database);
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Snackbar.make(v, R.string.failedToAddEntry, Snackbar.LENGTH_LONG).show());
                    }
                });
                thread.start();
            } else {
                Snackbar.make(v, R.string.fillDataErr, Snackbar.LENGTH_LONG).show();
            }
        });
        TextInputLayout nameLayout = findViewById(R.id.name);
        nameLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        nameLayout.setEndIconDrawable(R.drawable.copysmall);
        nameLayout.setEndIconOnClickListener(v -> {
            if (!name.getText().toString().matches("")) {
                ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Encryptor", name.getText().toString());
                if (clipboard == null || clip == null) return;
                clipboard.setPrimaryClip(clip);
                Snackbar.make(v, R.string.copied, Snackbar.LENGTH_LONG).show();
            }
        });
        TextInputLayout passLayout = findViewById(R.id.password);
        /*passLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        passLayout.setEndIconDrawable(R.drawable.copysmall);
        passLayout.setEndIconOnClickListener(v -> {
            if (!pass.getText().toString().matches("")) {
                ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Encryptor", pass.getText().toString());
                if (clipboard == null || clip == null) return;
                clipboard.setPrimaryClip(clip);
                Snackbar.make(v, "Password copied to clipboard!", Snackbar.LENGTH_LONG).show();
            }
        });*/
        TextInputLayout loginLayout = findViewById(R.id.login);
        loginLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        loginLayout.setEndIconDrawable(R.drawable.copysmall);
        loginLayout.setEndIconOnClickListener(v -> {
            if (!login.getText().toString().matches("")) {
                ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Encryptor", login.getText().toString());
                if (clipboard == null || clip == null) return;
                clipboard.setPrimaryClip(clip);
                Snackbar.make(v, R.string.copied, Snackbar.LENGTH_LONG).show();
            }
        });
        TextInputLayout websiteLayout = findViewById(R.id.website);
        websiteLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        websiteLayout.setEndIconDrawable(R.drawable.browsericon);
        websiteLayout.setEndIconOnClickListener(v -> {
            if (!website.getText().toString().matches("")) {
                try {
                    String s = URLUtil.guessUrl(website.getText().toString());
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Snackbar.make(v, R.string.invalidURLErr, Snackbar.LENGTH_LONG).show();
                }
            }
        });
        TextInputLayout noteLayout = findViewById(R.id.notes);
        noteLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        noteLayout.setEndIconDrawable(R.drawable.copysmall);
        noteLayout.setEndIconOnClickListener(v -> {
            if (!notes.getText().toString().matches("")) {
                ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Encryptor", notes.getText().toString());
                if (clipboard == null || clip == null) return;
                clipboard.setPrimaryClip(clip);
                Snackbar.make(v, R.string.copied, Snackbar.LENGTH_LONG).show();
            }
        });
        ProgressBar strength = findViewById(R.id.passwordStrengthBar);
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
                    passLayout.setHelperText(strongPassword);
                    setPassBarColor(Color.parseColor("#00FF00"), strength);
                } else if (passStrength >= 5) {
                    passLayout.setHelperTextEnabled(true);
                    passLayout.setHelperText(mediumPassword);
                    setPassBarColor(Color.parseColor("#FFFF00"), strength);
                } else if (passStrength <= 3) {
                    passLayout.setHelperTextEnabled(true);
                    passLayout.setHelperText(weakPassword);
                    setPassBarColor(Color.parseColor("#FF0000"), strength);
                }
            }
        });
        Button generatePassword = findViewById(R.id.generatePassword);
        generatePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(passwordAdd.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setSingleLine(true);
                input.setHint(R.string.passLength);
                final String[] passwordAlphabetMode = {""};
                CharSequence[] items = new CharSequence[]{getString(R.string.uppercase), getString(R.string.lowercase), getString(R.string.numbers), getString(R.string.specialSymbols)};
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(passwordAdd.this, R.style.MaterialAlertDialog_rounded)
                        .setTitle(getString(R.string.generatePass))
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
                        .setPositiveButton(R.string.generate, (dialog, which) -> {
                            try {
                                String lengthString = input.getText().toString();
                                if (!lengthString.matches("") && !passwordAlphabetMode[0].matches("")) {
                                    int length = Integer.parseInt(lengthString);
                                    String password = RndPassword.generateRandomPasswordStr(length, passwordAlphabetMode[0]);
                                    pass.setText(password);
                                } else {
                                    Snackbar.make(v, R.string.selectPassOptions, Snackbar.LENGTH_LONG).show();
                                }
                            } catch (Exception e){
                                Snackbar.make(v, R.string.passwordTooLong, Snackbar.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {});
                builder.show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        cancel.performClick();
    }

    public static int calculatePasswordStrength(String password) {

        int passwordScore = 0;

        if (password.length() >= 8 && password.length() <= 10)
            passwordScore += 1;
        else if (password.length() >= 10)
            passwordScore += 2;
        else if(password.length() == 0)
            return 0;
        else
            return 1;

        if (password.matches("(?=.*[0-9].*[0-9]).*"))
            passwordScore += 2;
        else if (password.matches("(?=.*[0-9]).*"))
            passwordScore += 1;

        if (password.matches("(?=.*[a-z].*[a-z]).*"))
            passwordScore += 2;
        else if(password.matches("(?=.*[a-z]).*"))
            passwordScore += 1;

        if (password.matches("(?=.*[A-Z].*[A-Z]).*"))
            passwordScore += 2;
        else if (password.matches("(?=.*[A-Z]).*"))
            passwordScore += 1;

        if (password.matches("(?=.*[~!@#$%^&*()_-].*[~!@#$%^&*()_-]).*"))
            passwordScore += 2;
        else if (password.matches("(?=.*[~!@#$%^&*()_-]).*"))
            passwordScore += 1;

        return passwordScore;

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