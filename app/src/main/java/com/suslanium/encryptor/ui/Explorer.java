package com.suslanium.encryptor.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
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
import com.suslanium.encryptor.R;
import com.suslanium.encryptor.ui.password.PasswordFragment;
import com.suslanium.encryptor.ui.explorer.ExplorerFragment;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Explorer extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ExplorerFragment fragment = null;
    private PasswordFragment passwordFragment = null;
    private Intent intent = null;
    private int currentOperationNumber = 0;
    private ImageButton searchButton = null;
    private EditText searchBar = null;
    private boolean explorerVisible = false;
    private boolean passwordVaultVisible = false;
    private boolean messageCryptVisible = false;
    private boolean settingsVisible = false;
    private boolean notesVisible = false;
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
        final Observer<Integer> integerObserver = integer -> {
            if(integer != 0) navController.navigate(integer);
        };
        id.observe(this,integerObserver);
        //This is for optimization: the fragment is not loaded right after click, so drawer can be closed without lags
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
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
                            if (!isExplorerVisible()) viewModel.setCurrentFragmentID(itemID);
                            break;
                        case R.id.nav_datavault:
                            if (!isPasswordVaultVisible())
                                viewModel.setCurrentFragmentID(itemID);
                            break;
                        case R.id.nav_keyexchange:
                            viewModel.setCurrentFragmentID(itemID);
                            break;
                        case R.id.nav_notes:
                            if(!isNotesVisible())
                                viewModel.setCurrentFragmentID(itemID);
                            break;
                        case R.id.nav_messagecrypt:
                            if (!isMessageCryptVisible())
                                viewModel.setCurrentFragmentID(itemID);
                            break;
                        case R.id.nav_settings:
                            if (!isSettingsVisible()) viewModel.setCurrentFragmentID(itemID);
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
        } else {
            moveTaskToBack(true);
            finishAffinity();
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
                } catch (InterruptedException ignored) {
                }
                backPressedCount--;
            });
            thread.start();
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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


    public int getCurrentOperationNumber() {
        return currentOperationNumber;
    }

    public void setCurrentOperationNumber(int currentOperationNumber) {
        this.currentOperationNumber = currentOperationNumber;
    }

    public ImageButton getSearchButton() {
        return searchButton;
    }

    public void setSearchButton(ImageButton searchButton) {
        this.searchButton = searchButton;
    }

    public EditText getSearchBar() {
        return searchBar;
    }

    public void setSearchBar(EditText searchBar) {
        this.searchBar = searchBar;
    }

    public boolean isExplorerVisible() {
        return explorerVisible;
    }

    public void setExplorerVisible(boolean explorerVisible) {
        this.explorerVisible = explorerVisible;
    }

    public boolean isPasswordVaultVisible() {
        return passwordVaultVisible;
    }

    public void setPasswordVaultVisible(boolean passwordVaultVisible) {
        this.passwordVaultVisible = passwordVaultVisible;
    }

    public boolean isMessageCryptVisible() {
        return messageCryptVisible;
    }

    public void setMessageCryptVisible(boolean messageCryptVisible) {
        this.messageCryptVisible = messageCryptVisible;
    }

    public boolean isSettingsVisible() {
        return settingsVisible;
    }

    public void setSettingsVisible(boolean settingsVisible) {
        this.settingsVisible = settingsVisible;
    }

    public boolean isNotesVisible() {
        return notesVisible;
    }

    public void setNotesVisible(boolean notesVisible) {
        this.notesVisible = notesVisible;
    }
}