package com.suslanium.encryptor.ui.welcomescreen;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;

import com.suslanium.encryptor.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class WelcomeActivity extends AppCompatActivity {

    WelcomeActivityAdapter messageCryptCollectionAdapter;
    ViewPager2 viewPager;

    @Override
    public void onBackPressed() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        messageCryptCollectionAdapter = new WelcomeActivityAdapter(this);
        viewPager = findViewById(R.id.welcomePager);
        viewPager.setAdapter(messageCryptCollectionAdapter);
    }
    public static boolean isDeviceRooted() {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3();
    }

    private static boolean checkRootMethod1() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private static boolean checkRootMethod2() {
        String[] paths = { "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su", "/sbin/magisk", "/sbin/magiskhide"};
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean checkRootMethod3() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return in.readLine() != null;
        } catch (Throwable t) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }
}