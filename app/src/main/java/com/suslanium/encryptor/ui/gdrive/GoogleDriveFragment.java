package com.suslanium.encryptor.ui.gdrive;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.google.android.material.snackbar.Snackbar;
import com.suslanium.encryptor.Explorer;
import com.suslanium.encryptor.GoogleDriveManager;
import com.suslanium.encryptor.R;

public class GoogleDriveFragment extends Fragment {
    private GoogleSignInClient mGoogleSignInClient;
    SignInButton signInButton;
    private static final int SIGNIN = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gdrive, container, false);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGNIN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar t = requireActivity().findViewById(R.id.toolbar);
        if(((Explorer) requireActivity()).searchButton != null)t.removeView(((Explorer) requireActivity()).searchButton);
        if(((Explorer) requireActivity()).searchBar != null) {
            t.removeView(((Explorer) requireActivity()).searchBar);
            ((Explorer) requireActivity()).searchBar = null;
            final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        ConnectivityManager cm =
                (ConnectivityManager)requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if(activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            signInButton = requireActivity().findViewById(R.id.sign_in_button);
            signInButton.setSize(SignInButton.SIZE_STANDARD);
            signInButton.setOnClickListener(v -> {
                if (v.getId() == R.id.sign_in_button) {
                    signIn();
                }
            });
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
        } else {
            Snackbar.make(view, R.string.noInternet, Snackbar.LENGTH_LONG).show();
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        ConnectivityManager cm =
                (ConnectivityManager)requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if(activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
            if (account != null) {
                Intent intent = new Intent(requireActivity(), GoogleDriveManager.class);
                intent.putExtra("pass", ((Explorer) requireActivity()).getIntent2().getByteArrayExtra("pass"));
                startActivity(intent);
            }
        } else {
            Snackbar.make(requireView(), R.string.noInternet, Snackbar.LENGTH_LONG).show();
        }
    }
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, SIGNIN);
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Intent intent = new Intent(requireActivity(), GoogleDriveManager.class);
            intent.putExtra("pass", ((Explorer) requireActivity()).getIntent2().getByteArrayExtra("pass"));
            startActivity(intent);
        } catch (ApiException e) {
            Log.w("GoogleDrive", "signInResult:failed code=" + e.getStatusCode());
        }
    }
}