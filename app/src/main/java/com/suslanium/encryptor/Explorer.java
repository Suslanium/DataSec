package com.suslanium.encryptor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.suslanium.encryptor.ui.gallery.GalleryFragment;
import com.suslanium.encryptor.ui.home.HomeFragment;
import com.suslanium.encryptor.ui.slideshow.SlideshowFragment;

public class Explorer extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private HomeFragment fragment = null;
    private Intent intent = null;
    public int currentOperationNumber = 0;
    public ImageButton searchButton = null;
    public EditText searchBar = null;
    public boolean explorerVisible = false;
    public boolean passwordVaultVisible = false;
    public boolean messageCryptVisible = false;
    public boolean settingsVisible = false;
    private View navExplorer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dark_theme = preferences.getBoolean("dark_Theme", true);
        if (dark_theme) setTheme(R.style.Theme_MaterialComponents_NoActionBar);
        else setTheme(R.style.Theme_MaterialComponents_Light_NoActionBar);
        super.onCreate(savedInstanceState);
        intent = getIntent();
        boolean isFromSettings = intent.getBooleanExtra("fromSettings", false);
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
        if (isFromSettings) {
            navController.navigate(R.id.nav_settings);
        }
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                /*drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
                    @Override
                    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) { }

                    @Override
                    public void onDrawerOpened(@NonNull View drawerView) { }

                    @Override
                    public void onDrawerClosed(@NonNull View drawerView) {
                        switch (item.getItemId()){
                            case R.id.nav_explorer:
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.nav_host_fragment, new HomeFragment())
                                        .addToBackStack(null)
                                        .commit();
                                break;
                            case R.id.nav_datavault:
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.nav_host_fragment, new GalleryFragment())
                                        .addToBackStack(null)
                                        .commit();
                                break;
                            case R.id.nav_keyexchange:
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.nav_host_fragment, new SlideshowFragment())
                                        .addToBackStack(null)
                                        .commit();
                                break;
                            case R.id.nav_messagecrypt:
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.nav_host_fragment, new MessageFragment())
                                        .addToBackStack(null)
                                        .commit();
                                 break;
                            case R.id.nav_settings:
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.nav_host_fragment, new SettingsFragment())
                                        .addToBackStack(null)
                                        .commit();
                                break;
                        }
                    }

                    @Override
                    public void onDrawerStateChanged(int newState) { }
                });
                drawer.closeDrawer(GravityCompat.START);
                return true;*/
                int millis = 400;
                int itemID = item.getItemId();
                View navHost = findViewById(R.id.nav_host_fragment);
                navHost.animate()
                        .alpha(0f)
                        .setDuration(0)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                navHost.setVisibility(View.GONE);
                            }
                        });
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        switch (itemID) {
                            case R.id.nav_explorer:
                                if (!explorerVisible) navController.navigate(R.id.nav_explorer);
                                //Log.d("Call", getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).getName());
                                break;
                            case R.id.nav_datavault:
                                if (!passwordVaultVisible)
                                    navController.navigate(R.id.nav_datavault);
                                break;
                            case R.id.nav_keyexchange:
                                navController.navigate(R.id.nav_keyexchange);
                                break;
                            case R.id.nav_messagecrypt:
                                if (!messageCryptVisible)
                                    navController.navigate(R.id.nav_messagecrypt);
                                break;
                            case R.id.nav_settings:
                                if (!settingsVisible) navController.navigate(R.id.nav_settings);
                                break;
                        }
                        navHost.setVisibility(View.VISIBLE);
                        navHost.bringToFront();
                        navHost.animate()
                                .alpha(1f)
                                .setDuration(200)
                                .setListener(null);
                    }
                }, millis);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        });
        navExplorer = findViewById(R.id.drawer_layout);
    }

    @Override
    public void onBackPressed() {
        if (fragment != null && fragment.isVisible()) {
            fragment.getUpFolderAction().onClick(navExplorer);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.explorer, menu);
        return true;
    }

    public void setExplorerFragment(HomeFragment fragment) {
        this.fragment = fragment;
    }


    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public Intent getIntent2() {
        return intent;
    }
}