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
import androidx.annotation.Nullable;
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

import java.io.File;
import java.util.ArrayList;

public class Explorer extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private HomeFragment fragment = null;
    private GalleryFragment galleryFragment = null;
    private Intent intent = null;
    public int currentOperationNumber = 0;
    public ImageButton searchButton = null;
    public EditText searchBar = null;
    public boolean explorerVisible = false;
    public boolean passwordVaultVisible = false;
    public boolean messageCryptVisible = false;
    public boolean settingsVisible = false;
    private View navExplorer;
    private int backPressedCount = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 101){
            deleteFiles(getFilesDir()+File.separator+".temp");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(getIntent().getByteArrayExtra("pass") == null){
            Intent pass = new Intent(this, PasswordActivity.class);
            startActivity(pass);
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean darkTheme = preferences.getBoolean("dark_Theme", false);
        if (darkTheme) setTheme(R.style.Theme_Encryptor_Dark);
        else setTheme(R.style.Theme_Encryptor_Light);
        super.onCreate(savedInstanceState);
        intent = getIntent();
        boolean isFromSettings = intent.getBooleanExtra("fromSettings", false);
        setContentView(R.layout.activity_explorer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextAppearance(this,R.style.Ubuntu);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setItemTextAppearance(R.style.Theme_Encryptor_Dark_Nav);
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_explorer, R.id.nav_datavault, R.id.nav_keyexchange, R.id.nav_messagecrypt, R.id.nav_settings)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        if (isFromSettings) {
            navController.navigate(R.id.nav_settings);
        }
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
                new Handler().postDelayed(() -> {
                    switch (itemID) {
                        case R.id.nav_explorer:
                            if (!explorerVisible) navController.navigate(R.id.nav_explorer);
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
                        default:
                            break;
                    }
                    navHost.setVisibility(View.VISIBLE);
                    navHost.bringToFront();
                    navHost.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .setListener(null);
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
        } else if(galleryFragment != null && galleryFragment.isVisible()){
            galleryFragment.backPress();
        }
    }

    public void incrementBackPressedCount() {
        backPressedCount++;
        if (backPressedCount > 1) {
            moveTaskToBack(true);
            finishAffinity();
        } else {
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
                backPressedCount--;
            });
            thread.start();
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

    public void setGalleryFragment(GalleryFragment fragment){galleryFragment = fragment;}

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public Intent getIntent2() {
        return intent;
    }

    private void deleteFiles(ArrayList<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            File file = new File(paths.get(i));
            if (!file.isFile()) {
                File[] files = file.listFiles();
                if (files != null && files.length > 0) {
                    ArrayList<String> subPaths = new ArrayList<>();
                    for (int j = 0; j < files.length; j++) {
                        subPaths.add(files[j].getPath());
                    }
                    deleteFiles(subPaths);
                }
            }
            file.delete();
        }
    }

    private void deleteFiles(String path) {
        File file = new File(path);
        if (!file.isFile()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                ArrayList<String> subPaths = new ArrayList<>();
                for (int j = 0; j < files.length; j++) {
                    subPaths.add(files[j].getPath());
                }
                deleteFiles(subPaths);
            }
        }
        file.delete();
    }
}