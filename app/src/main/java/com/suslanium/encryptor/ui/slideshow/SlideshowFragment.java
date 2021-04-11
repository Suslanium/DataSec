package com.suslanium.encryptor.ui.slideshow;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.microsoft.onedrivesdk.picker.IPicker;
import com.microsoft.onedrivesdk.picker.IPickerResult;
import com.microsoft.onedrivesdk.picker.LinkType;
import com.microsoft.onedrivesdk.picker.Picker;
import com.suslanium.encryptor.Explorer;
import com.suslanium.encryptor.GoogleDrive;
import com.suslanium.encryptor.R;
import com.suslanium.encryptor.YandexDiskSignIn;
import com.suslanium.encryptor.oneDriveActivity;
import com.suslanium.encryptor.passwordAdd;

import java.security.Signature;

public class SlideshowFragment extends Fragment {

    private SlideshowViewModel slideshowViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_slideshow, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Intent intent2 = ((Explorer) getActivity()).getIntent2();
        Button oneDriveButton = (Button)getActivity().findViewById(R.id.oneDriveButton);
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

                }*/
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
        });
    }
}