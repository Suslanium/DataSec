package com.suslanium.encryptor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

import static android.content.Context.POWER_SERVICE;

public class WelcomeActivityFragment extends Fragment {
    public static final String ARG_OBJECT = "intType";

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
        switch (position) {
            case 1:
                if(WelcomeActivity.isDeviceRooted()){
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.rootWarning)
                            .setCancelable(false)
                            .setPositiveButton(R.string.cont, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    builder.show();
                }
                //Welcome
                confPass.setVisibility(View.GONE);
                pass.setVisibility(View.GONE);
                grantBattery.setVisibility(View.GONE);
                grantStorage.setVisibility(View.GONE);
                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pager2.setCurrentItem(1, true);
                    }
                });
                break;
            case 2:
                confPass.setVisibility(View.GONE);
                pass.setVisibility(View.GONE);
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
                grantStorage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                        }
                    }
                });
                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
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
                    }
                });
                //Let's setup permissions
                break;
            //Let's setup password
            case 3:
                grantBattery.setVisibility(View.GONE);
                grantStorage.setVisibility(View.GONE);
                topText.setText(R.string.letsSetupPassword);
                bottomText.setText(R.string.letsSetupPasswordText);
                next.setText(R.string.finishSetup);
                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
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
                    }
                });
                break;
            default:
                break;
        }
    }

    private void finishSetup(String password, View v){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
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
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(v, R.string.smthWentWrong, Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
        thread.start();
    }
}
