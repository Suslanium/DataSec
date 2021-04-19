package com.suslanium.encryptor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.suslanium.encryptor.ui.home.HomeFragment;

public class Explorer extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private HomeFragment fragment = null;
    private Intent intent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dark_theme = preferences.getBoolean("dark_Theme", true);
        if(dark_theme) setTheme(R.style.Theme_MaterialComponents_NoActionBar);
        else setTheme(R.style.Theme_MaterialComponents_Light_NoActionBar);
        super.onCreate(savedInstanceState);
        intent = getIntent();
        setContentView(R.layout.activity_explorer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_explorer, R.id.nav_datavault, R.id.nav_keyexchange, R.id.nav_messagecrypt, R.id.nav_settings)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        float radius = getResources().getDimension(R.dimen.drawer_round);
        MaterialShapeDrawable navViewBackground = (MaterialShapeDrawable) navigationView.getBackground();
        navViewBackground.setShapeAppearanceModel(
                navViewBackground.getShapeAppearanceModel()
                        .toBuilder()
                        .setTopRightCorner(CornerFamily.ROUNDED, getResources().getDimension(R.dimen.drawer_round2))
                        .setBottomRightCorner(CornerFamily.ROUNDED, radius)
                        .build());
    }

    @Override
    public void onBackPressed() {
        if(fragment != null && fragment.isVisible()){
            fragment.upFolder.performClick();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.explorer, menu);
        return true;
    }
    public void setExplorerFragment(HomeFragment fragment){
        this.fragment = fragment;
    }


    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
    public Intent getIntent2(){
        return intent;
    }
}