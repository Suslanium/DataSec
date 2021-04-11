package com.suslanium.encryptor;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.material.snackbar.Snackbar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YandexDiskSignIn extends FragmentActivity {
    public static final String CLIENT_ID = "6afb1b69b37f4608ae9f0d74f3d29f92";
    public static final String AUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id="+CLIENT_ID;
    public static final String USERNAME = "encryptor.username";
    public static final String TOKEN = "encryptor.token";
    private static final String TAG = "YaDiSignIn";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yandex_disk_sign_in);
        if (getIntent() != null && getIntent().getData() != null) {
            onLogin();
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String token = preferences.getString(TOKEN, null);
        if (token == null) {
            startLogin();
            return;
        }
    }
    public void startLogin() {
        new AuthDialogFragment().show(getSupportFragmentManager(), "auth");
    }
    private void onLogin () {
        Uri data = getIntent().getData();
        if(data != null) {
            setIntent(null);
            Pattern pattern = Pattern.compile("access_token=(.*?)(&|$)");
            Matcher matcher = pattern.matcher(data.toString());
            if (matcher.find()) {
                final String token = matcher.group(1);
                if (!TextUtils.isEmpty(token)) {
                    Log.d(TAG, "onLogin: token: " + token);
                    saveToken(token);
                } else {
                    Log.w(TAG, "onRegistrationSuccess: empty token");
                }
            } else {
                Log.w(TAG, "onRegistrationSuccess: token not found in return url");
            }
        }
    }
    private void saveToken(String token) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(USERNAME, "");
        editor.putString(TOKEN, token);
        editor.apply();
    }
    public void openLogin(){
        WebView webView = (WebView) findViewById(R.id.loginWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(AUTH_URL);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final String[] url = {null};
                do {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                url[0] = webView.getUrl();
                            }
                        });
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (!url[0].contains("https://yx6afb1b69b37f4608ae9f0d74f3d29f92.oauth.yandex.ru/"));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String token = url[0].substring(101, 140);
                        Snackbar.make(webView, token, Snackbar.LENGTH_LONG).show();
                        saveToken(token);
                    }
                });
            }
        });
        thread.start();
    }

    public static class AuthDialogFragment extends DialogFragment {

        public AuthDialogFragment () {
            super();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity(), R.style.MaterialAlertDialog_rounded)
                    .setTitle("Login?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick (DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ((YandexDiskSignIn)getActivity()).openLogin();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick (DialogInterface dialog, int which) {
                            dialog.dismiss();
                            getActivity().finish();
                        }
                    })
                    .create();
        }
    }
}