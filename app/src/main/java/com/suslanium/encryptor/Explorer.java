package com.suslanium.encryptor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.suslanium.encryptor.ui.password.PasswordFragment;
import com.suslanium.encryptor.ui.explorer.ExplorerFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Explorer extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ExplorerFragment fragment = null;
    private PasswordFragment passwordFragment = null;
    private Intent intent = null;
    public int currentOperationNumber = 0;
    public ImageButton searchButton = null;
    public EditText searchBar = null;
    public boolean explorerVisible = false;
    public boolean passwordVaultVisible = false;
    public boolean messageCryptVisible = false;
    public boolean settingsVisible = false;
    public boolean notesVisible = false;
    private View navExplorer;
    private int backPressedCount = 0;
    private ExecutorService service;
    private ExplorerActivityViewModel viewModel;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 101){
            service.submit(() -> viewModel.deleteFiles(getFilesDir()+File.separator+".temp"));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())).get(ExplorerActivityViewModel.class);
        if(getIntent().getByteArrayExtra("pass") == null){
            Intent pass = new Intent(this, PasswordActivity.class);
            startActivity(pass);
        }
        service = Executors.newCachedThreadPool();
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
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_explorer, R.id.nav_datavault, R.id.nav_keyexchange,R.id.nav_notes, R.id.nav_messagecrypt, R.id.nav_settings)
                .setDrawerLayout(drawer)
                .build();
        LiveData<Integer> id = viewModel.getID();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        if (isFromSettings) {
            navController.navigate(R.id.nav_settings);
            intent.removeExtra("fromSettings");
        }
        if(id.getValue() != 0){navController.navigate(id.getValue());}
        final Observer<Integer> integerObserver = new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if(integer != 0) navController.navigate(integer);
            }
        };
        id.observe(this,integerObserver);
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
                            if (!explorerVisible) viewModel.setCurrentFragmentID(itemID);
                            break;
                        case R.id.nav_datavault:
                            if (!passwordVaultVisible)
                                viewModel.setCurrentFragmentID(itemID);
                            break;
                        case R.id.nav_keyexchange:
                            viewModel.setCurrentFragmentID(itemID);
                            break;
                        case R.id.nav_notes:
                            if(!notesVisible)
                                viewModel.setCurrentFragmentID(itemID);
                            break;
                        case R.id.nav_messagecrypt:
                            if (!messageCryptVisible)
                                viewModel.setCurrentFragmentID(itemID);
                            break;
                        case R.id.nav_settings:
                            if (!settingsVisible) viewModel.setCurrentFragmentID(itemID);
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
        } else if(passwordFragment != null && passwordFragment.isVisible()){
            passwordFragment.backPress();
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

    public void setExplorerFragment(ExplorerFragment fragment) {
        this.fragment = fragment;
    }

    public void setPasswordFragment(PasswordFragment fragment){
        passwordFragment = fragment;}

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