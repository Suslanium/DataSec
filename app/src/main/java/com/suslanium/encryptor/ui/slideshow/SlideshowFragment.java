package com.suslanium.encryptor.ui.slideshow;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.suslanium.encryptor.Explorer;
import com.suslanium.encryptor.GoogleDriveManager;
import com.suslanium.encryptor.R;

public class SlideshowFragment extends Fragment {
    private GoogleSignInClient mGoogleSignInClient;
    SignInButton signInButton;
    int RC_SIGN_IN = 0;
    private SlideshowViewModel slideshowViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_slideshow, container, false);
        return root;
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar t = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if(((Explorer) getActivity()).searchButton != null)t.removeView(((Explorer) getActivity()).searchButton);
        if(((Explorer) getActivity()).searchBar != null) {
            t.removeView(((Explorer) getActivity()).searchBar);
            ((Explorer) getActivity()).searchBar = null;
            final InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        Intent intent2 = ((Explorer) getActivity()).getIntent2();
        /*Button oneDriveButton = (Button)getActivity().findViewById(R.id.oneDriveButton);
        Button googleDriveButtton = (Button)getActivity().findViewById(R.id.googleDriveButton);
        Button yadiskButton = (Button)getActivity().findViewById(R.id.yadiskButton);
        oneDriveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*try {
                    android.content.pm.PackageInfo info = getContext().getPackageManager().getPackageInfo(
                            "com.suslanium.encryptor",
                            android.content.pm.PackageManager.GET_SIGNATURES);
                    for (android.content.pm.Signature signature : info.signatures) {
                        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA");
                        md.update(signature.toByteArray());
                        android.util.Log.d("KeyHash", "KeyHash:" + android.util.Base64.encodeToString(md.digest(),
                                android.util.Base64.DEFAULT));

                    }
                } catch (android.content.pm.PackageManager.NameNotFoundException e) {

                } catch (java.security.NoSuchAlgorithmException e) {

                }
                Intent intent = new Intent(getActivity(), oneDriveActivity.class);
                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                startActivity(intent);
                //Intent intent = new Intent(getActivity(), OneDriveSelector.class);
                //startActivity(intent);
            }
        });
        googleDriveButtton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), GoogleDrive.class);
                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                startActivity(intent);
            }
        });
        yadiskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), YandexDiskSignIn.class);
                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                startActivity(intent);
            }
        });*/
        signInButton = getActivity().findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.sign_in_button:
                        signIn();
                        break;
                }
            }
        });
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);
    }
    @Override
    public void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
        if(account != null) {
            Intent intent = new Intent(getActivity(), GoogleDriveManager.class);
            intent.putExtra("pass", ((Explorer) getActivity()).getIntent2().getByteArrayExtra("pass"));
            startActivity(intent);
        }
    }
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Intent intent = new Intent(getActivity(), GoogleDriveManager.class);
            intent.putExtra("pass", ((Explorer) getActivity()).getIntent2().getByteArrayExtra("pass"));
            startActivity(intent);
        } catch (ApiException e) {
            Log.w("GoogleDrive", "signInResult:failed code=" + e.getStatusCode());
        }
    }
}