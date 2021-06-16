package com.suslanium.encryptor.ui;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.suslanium.encryptor.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onStart() {
        super.onStart();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.about);
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.backarrow);
        actionBar.setHomeAsUpIndicator(drawable);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dark_theme = preferences.getBoolean("dark_Theme", false);
        if (dark_theme) setTheme(R.style.Theme_Encryptor_Dark_ActionBar);
        else setTheme(R.style.Theme_Encryptor_Light_ActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView version = findViewById(R.id.versionText);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version.setText(getString(R.string.version) + " " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            version.setText(getString(R.string.version) + " N/A");
        }
        TextView sendFeedback = findViewById(R.id.sendFeedBackText);
        sendFeedback.setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:datacryptinfo@gmail.com"));
            email.putExtra(Intent.EXTRA_SUBJECT, "DataCrypt feedback");
            email.putExtra(Intent.EXTRA_TEXT, "");
            startActivity(Intent.createChooser(email, getString(R.string.selectApp)));
        });
        FrameLayout github = findViewById(R.id.githubFrame);
        github.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Suslanium/DataSec"));
            startActivity(browserIntent);
        });
        ImageView itschool = findViewById(R.id.ITSchoolImage);
        itschool.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://myitschool.ru/"));
            startActivity(browserIntent);
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