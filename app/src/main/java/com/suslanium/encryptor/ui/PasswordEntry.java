package com.suslanium.encryptor.ui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

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
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.suslanium.encryptor.R;
import com.suslanium.encryptor.util.RndPassword;

import java.util.ArrayList;
import java.util.regex.Pattern;


public class PasswordEntry extends AppCompatActivity {
    private String service = "";
    private String loginName = "";
    private String passName = "";
    private String websiteName = "";
    private String notesName = "";
    private ImageView icon;
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
    private PasswordEntryViewModel viewModel;
    private static final int PICK_IMAGE = 1;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return;
            }
            Thread thread = new Thread(() -> {
                Bitmap thumbnail = viewModel.getThumbnail(data);
                if(thumbnail != null){
                    runOnUiThread(() -> icon.setImageBitmap(thumbnail));
                }
            });
            thread.start();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(PasswordEntryViewModel.class);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dark_theme = preferences.getBoolean("dark_Theme", false);
        if(dark_theme) setTheme(R.style.Theme_Encryptor_Dark_Pass);
        else setTheme(R.style.Theme_Encryptor_Light_Pass);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_change);
        Bundle b = getIntent().getExtras();
        int id = b.getInt("id");
        weakPassword = getString(R.string.weakPassword);
        mediumPassword = getString(R.string.mediumPassword);
        strongPassword = getString(R.string.strongPassword);
        icon = findViewById(R.id.serviceChangeIcon);
        Intent intent = getIntent();
        FloatingActionButton delete = findViewById(R.id.delete);
        cancel = findViewById(R.id.cancel2);
        if(!intent.getBooleanExtra("newEntry",false)) {
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
                try {
                    byte[] passEnc = intent.getByteArrayExtra("pass");
                    ArrayList<String> strings = new ArrayList<>(viewModel.readEntryData(passEnc, id));
                    runOnUiThread(() -> onThreadDone(strings));
                    Bitmap image = viewModel.getImage();
                    if(image != null){
                        runOnUiThread(() -> icon.setImageBitmap(image));
                    }
                } catch (Exception e) {
                    finish();
                }
                PasswordEntry.this.runOnUiThread(alertDialog::dismiss);
            });
            thread.start();
            delete.setOnClickListener(v -> {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(PasswordEntry.this, R.style.MaterialAlertDialog_rounded)
                        .setTitle(R.string.areYouSure)
                        .setMessage(R.string.youAreGoingToDeleteEntry)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            Thread thread12 = new Thread(() -> {
                                try {
                                    Intent intent2 = getIntent();
                                    byte[] passEnc = intent2.getByteArrayExtra("pass");
                                    viewModel.deleteEntry(passEnc, id);
                                    finish();
                                } catch (Exception e){
                                    runOnUiThread(() -> Snackbar.make(v, R.string.failedToDeleteEntry, Snackbar.LENGTH_LONG).show());
                                }
                            });
                            thread12.start();
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                builder.show();
            });
            cancel.setOnClickListener(v -> {
                if (!name.getText().toString().matches(Pattern.quote(service)) || !login.getText().toString().matches(Pattern.quote(loginName)) || !pass.getText().toString().matches(Pattern.quote(passName)) || !website.getText().toString().matches(Pattern.quote(websiteName)) || !notes.getText().toString().matches(Pattern.quote(notesName))) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(PasswordEntry.this, R.style.MaterialAlertDialog_rounded)
                            .setTitle(R.string.discardChanges)
                            .setMessage(R.string.discardEntryText)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                dialog.dismiss();
                                finish();
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                    builder.show();
                } else finish();
            });
        } else {
            cancel.setVisibility(View.GONE);
            delete.setImageResource(android.R.drawable.ic_delete);
            delete.setOnClickListener(v -> {
                if (!name.getText().toString().matches(Pattern.quote(service)) || !login.getText().toString().matches(Pattern.quote(loginName)) || !pass.getText().toString().matches(Pattern.quote(passName)) || !website.getText().toString().matches(Pattern.quote(websiteName)) || !notes.getText().toString().matches(Pattern.quote(notesName))) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(PasswordEntry.this, R.style.MaterialAlertDialog_rounded)
                            .setTitle(R.string.discardChanges)
                            .setMessage(R.string.discardEntryText)
                            .setPositiveButton(R.string.yes, (dialog, which) -> {
                                dialog.dismiss();
                                finish();
                            })
                            .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
                    builder.show();
                } else finish();
            });
        }
        icon.setOnClickListener(v -> {
            Intent intent1 = new Intent(Intent.ACTION_GET_CONTENT);
            intent1.setType("image/*");
            startActivityForResult(Intent.createChooser(intent1, getString(R.string.selectIcon)), PICK_IMAGE);
        });
        name = findViewById(R.id.nameInput);
        login = findViewById(R.id.loginInput);
        pass = findViewById(R.id.passInput);
        website = findViewById(R.id.websiteInput);
        notes = findViewById(R.id.notesInput);
        FloatingActionButton submit = findViewById(R.id.submit2);
        submit.setOnClickListener(v -> {
            if (!name.getText().toString().matches("")) {
                MaterialAlertDialogBuilder builder3 = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_rounded);
                ProgressBar bar2 = new ProgressBar(this);
                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                bar2.setLayoutParams(lp2);
                builder3.setTitle(R.string.wait);
                builder3.setView(bar2);
                builder3.setCancelable(false);
                AlertDialog alertDialog2 = builder3.create();
                alertDialog2.show();
                Thread thread1 = new Thread(() -> {
                    try {
                        Intent intent3 = getIntent();
                        byte[] passEnc = intent3.getByteArrayExtra("pass");
                        viewModel.saveEntry(passEnc, id, intent.getBooleanExtra("newEntry",false), name.getText().toString(), login.getText().toString(), pass.getText().toString(), website.getText().toString(), notes.getText().toString(),intent3.getStringExtra("category"));
                        runOnUiThread(alertDialog2::dismiss);
                        finish();
                    } catch (Exception e){
                        e.printStackTrace();
                        runOnUiThread(() -> Snackbar.make(v, R.string.failedToAddEntry, Snackbar.LENGTH_LONG).show());
                    }
                });
                thread1.start();
            } else {
                Snackbar.make(v, R.string.fillDataErr, Snackbar.LENGTH_LONG).show();
            }
        });
        TextInputLayout nameLayout = findViewById(R.id.name1);
        nameLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        nameLayout.setEndIconDrawable(R.drawable.copysmall);
        nameLayout.setEndIconOnClickListener(v -> copyToClipboard(name, v));
        TextInputLayout passLayout = findViewById(R.id.pass1);
        TextInputLayout loginLayout = findViewById(R.id.login1);
        loginLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        loginLayout.setEndIconDrawable(R.drawable.copysmall);
        loginLayout.setEndIconOnClickListener(v -> copyToClipboard(login, v));
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
                    Snackbar.make(v, R.string.invalidURLErr, Snackbar.LENGTH_LONG).show();
                }
            }
        });
        TextInputLayout noteLayout = findViewById(R.id.notes1);
        noteLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        noteLayout.setEndIconDrawable(R.drawable.copysmall);
        noteLayout.setEndIconOnClickListener(v -> copyToClipboard(notes, v));
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
                int passStrength = viewModel.calculatePasswordStrength(s.toString());
                setPassBarProgress(passStrength * 100, strength);
                if (passStrength >= 8) {
                    passLayout.setHelperTextEnabled(true);
                    passLayout.setHelperText(strongPassword);
                    setPassBarColor(Color.parseColor("#4CAF50"), strength);
                } else if (passStrength >= 5) {
                    passLayout.setHelperTextEnabled(true);
                    passLayout.setHelperText(mediumPassword);
                    setPassBarColor(Color.parseColor("#FBC02D"), strength);
                } else if (passStrength <= 3) {
                    passLayout.setHelperTextEnabled(true);
                    passLayout.setHelperText(weakPassword);
                    setPassBarColor(Color.parseColor("#EF5350"), strength);
                }
            }
        });
        Button generatePassword = findViewById(R.id.generatePassword2);
        generatePassword.setOnClickListener(v -> {
            final EditText input = new EditText(PasswordEntry.this);
            Typeface ubuntu = ResourcesCompat.getFont(PasswordEntry.this, R.font.ubuntu);
            input.setTypeface(ubuntu);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setSingleLine(true);
            input.setHint(R.string.passLength);
            final String[] passwordAlphabetMode = {""};
            CharSequence[] items = new CharSequence[]{getString(R.string.uppercase), getString(R.string.lowercase), getString(R.string.numbers), getString(R.string.specialSymbols)};
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(PasswordEntry.this, R.style.MaterialAlertDialog_rounded)
                    .setTitle(R.string.generatePass)
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

    private void copyToClipboard(TextInputEditText textField, View v){
        if(!textField.getText().toString().matches("")) {
            ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Encryptor", textField.getText().toString());
            if (clipboard == null || clip == null) return;
            clipboard.setPrimaryClip(clip);
            Snackbar.make(v, R.string.copied, Snackbar.LENGTH_LONG).show();
        }
    }
}