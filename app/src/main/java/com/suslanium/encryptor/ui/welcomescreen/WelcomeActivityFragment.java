package com.suslanium.encryptor.ui.welcomescreen;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.suslanium.encryptor.R;
import com.suslanium.encryptor.ui.PasswordActivity;
import com.suslanium.encryptor.ui.PasswordEntryViewModel;
import com.suslanium.encryptor.util.Encryptor;

import static android.content.Context.POWER_SERVICE;

public class WelcomeActivityFragment extends Fragment {
    protected static final String ARG_OBJECT = "intType";
    private int colorFrom = Color.parseColor("#FF0000");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome_activity, container, false);
    }

    @SuppressLint("ResourceType")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        int position = args.getInt(ARG_OBJECT);
        TextView topText = view.findViewById(R.id.topText);
        TextView bottomText = view.findViewById(R.id.bottomText);
        TextInputLayout confPass = view.findViewById(R.id.confPass);
        TextInputLayout pass = view.findViewById(R.id.pass);
        TextInputEditText conf = view.findViewById(R.id.confPassLayout);
        TextInputEditText passL = view.findViewById(R.id.passLayout);
        Button grantBattery = view.findViewById(R.id.grantBattery);
        Button grantStorage = view.findViewById(R.id.grantStorage);
        Button next = view.findViewById(R.id.next);
        ViewPager2 pager2 = requireActivity().findViewById(R.id.welcomePager);
        FrameLayout whyPermissions = view.findViewById(R.id.whyPermissionsFrame);
        ProgressBar strength = view.findViewById(R.id.passwordStrengthBar3);
        strength.setMax(1000);
        switch (position) {
            case 1:
                if(WelcomeActivity.isDeviceRooted()){
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.rootWarning)
                            .setCancelable(false)
                            .setPositiveButton(R.string.cont, (dialog, which) -> dialog.dismiss());
                    builder.show();
                }
                confPass.setVisibility(View.GONE);
                pass.setVisibility(View.GONE);
                grantBattery.setVisibility(View.GONE);
                grantStorage.setVisibility(View.GONE);
                whyPermissions.setVisibility(View.GONE);
                strength.setVisibility(View.GONE);
                next.setOnClickListener(v -> pager2.setCurrentItem(1, true));
                break;
            case 2:
                confPass.setVisibility(View.GONE);
                pass.setVisibility(View.GONE);
                strength.setVisibility(View.GONE);
                whyPermissions.setOnClickListener(v -> {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                            .setTitle(R.string.whyPermissions)
                            .setMessage(R.string.whyPermissionsText)
                            .setPositiveButton(R.string.cont, (dialog, which) -> dialog.dismiss());
                    builder.show();
                });
                topText.setText(R.string.letsSetupPermissions);
                bottomText.setText(R.string.letsSetupPermissionsText);
                grantBattery.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        String packageName = requireContext().getPackageName();
                        PowerManager pm = (PowerManager) requireContext().getSystemService(POWER_SERVICE);
                        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + packageName));
                            startActivityForResult(intent, 1002);
                        }
                    }
                });
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    grantBattery.setVisibility(View.GONE);
                }
                grantStorage.setOnClickListener(v -> {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    }
                });
                next.setOnClickListener(v -> {
                    String packageName = requireContext().getPackageName();
                    PowerManager pm = (PowerManager) requireContext().getSystemService(POWER_SERVICE);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                            pager2.setCurrentItem(2, true);
                        } else {
                            Snackbar.make(v, R.string.pleaseGrantPermissions, Snackbar.LENGTH_LONG).show();
                        }
                    } else {
                        if (pm.isIgnoringBatteryOptimizations(packageName) && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            pager2.setCurrentItem(2, true);
                        } else {
                            Snackbar.make(v, R.string.pleaseGrantPermissions, Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
                break;
            case 3:
                String weakPassword = getString(R.string.weakPassword);
                String mediumPassword = getString(R.string.mediumPassword);
                String strongPassword = getString(R.string.strongPassword);
                grantBattery.setVisibility(View.GONE);
                grantStorage.setVisibility(View.GONE);
                whyPermissions.setVisibility(View.GONE);
                topText.setText(R.string.letsSetupPassword);
                bottomText.setText(R.string.letsSetupPasswordText);
                next.setText(R.string.finishSetup);
                next.setOnClickListener(v -> {
                    if(!passL.getText().toString().matches("")){
                        if (passL.getText().toString().equals(conf.getText().toString())){
                            String password = passL.getText().toString();
                            String packageName = requireContext().getPackageName();
                            PowerManager pm = (PowerManager) requireContext().getSystemService(POWER_SERVICE);
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                                    finishSetup(password,v);
                                } else {
                                    Snackbar.make(v, R.string.pleaseGrantPermissions, Snackbar.LENGTH_LONG).show();
                                }
                            } else {
                                if (pm.isIgnoringBatteryOptimizations(packageName) && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                    finishSetup(password,v);
                                } else {
                                    Snackbar.make(v, R.string.pleaseGrantPermissions, Snackbar.LENGTH_LONG).show();
                                }
                            }
                        } else {
                            Snackbar.make(v, R.string.passwsMatchErr, Snackbar.LENGTH_LONG).show();
                        }
                    } else {
                        Snackbar.make(v, R.string.enterPassErr, Snackbar.LENGTH_LONG).show();
                    }
                });
                passL.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        int passStrength = PasswordEntryViewModel.calculatePasswordStrength(s.toString());
                        setPassBarProgress(passStrength * 100, strength);
                        if (passStrength >= 8) {
                            pass.setHelperTextEnabled(true);
                            pass.setHelperText(strongPassword);
                            setPassBarColor(Color.parseColor("#4CAF50"), strength);
                        } else if (passStrength >= 5) {
                            pass.setHelperTextEnabled(true);
                            pass.setHelperText(mediumPassword);
                            setPassBarColor(Color.parseColor("#FBC02D"), strength);
                        } else if (passStrength <= 3) {
                            pass.setHelperTextEnabled(true);
                            pass.setHelperText(weakPassword);
                            setPassBarColor(Color.parseColor("#EF5350"), strength);
                        }
                    }
                });
                break;
            default:
                break;
        }
    }

    private void finishSetup(String password, View v){
        Thread thread = new Thread(() -> {
            try {
                MasterKey mainKey = new MasterKey.Builder(requireContext())
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
                SharedPreferences editor = EncryptedSharedPreferences.create(requireContext(), "encryptor_shared_prefs", mainKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                SharedPreferences.Editor edit = editor.edit();
                edit.putString("passHash", Encryptor.calculateHash(password, "SHA-512"));
                edit.apply();
                SharedPreferences.Editor edit1 = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
                edit1.putBoolean("setupComplete", true);
                edit1.apply();
                Intent intent = new Intent(requireContext(), PasswordActivity.class);
                startActivity(intent);
            } catch (Exception e){
                requireActivity().runOnUiThread(() -> Snackbar.make(v, R.string.smthWentWrong, Snackbar.LENGTH_LONG).show());
            }
        });
        thread.start();
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
