package com.suslanium.encryptor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;

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
}