package com.suslanium.encryptor;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.fragment.app.FragmentActivity;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.RestClient;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YandexDiskSignIn extends FragmentActivity {
    public static final String CLIENT_ID = "6afb1b69b37f4608ae9f0d74f3d29f92";
    public static final String AUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id="+CLIENT_ID;
    public static final String USERNAME = "encryptor.username";
    public static final String TOKEN = "encryptor.token";
    private static final String TAG = "YaDiSignIn";
    private RestClient client;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yandex_disk_sign_in);
        /*if (getIntent() != null && getIntent().getData() != null) {
            onLogin();
        }*/
        Credentials credentials = loadCredentials();
        if (credentials == null) {
            startLogin();
            return;
        } else {
            client = new RestClient(credentials);
            YaDiLoader loader = new YaDiLoader(this, new YaDiCredentials(loadCredentials().getUser(), loadCredentials().getToken()), "%2F");
            List<YaDiListItem> yaDiListItems = loader.loadInBackground();
            for(YaDiListItem yaDiListItem: yaDiListItems){
                System.out.println(yaDiListItem.toString());
            }
        }
    }
    public void startLogin() {
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
                        onLogin(url[0]);
                    }
                });
            }
        });
        thread.start();
    }
    private void onLogin (String url) {
        //Uri data = getIntent().getData();
        if(url != null) {
            Pattern pattern = Pattern.compile("access_token=(.*?)(&|$)");
            Matcher matcher = pattern.matcher(url);
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
    private Credentials loadCredentials(){
        SharedPreferences editor = PreferenceManager.getDefaultSharedPreferences(this);
        String name = editor.getString(USERNAME, null);
        String token = editor.getString(TOKEN, null);
        if(name != null && token != null)return new Credentials(name,token);
        else return null;
    }
}