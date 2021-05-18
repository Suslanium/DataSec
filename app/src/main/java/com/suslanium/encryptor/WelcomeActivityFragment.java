package com.suslanium.encryptor;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WelcomeActivityFragment  extends Fragment {
    public static final String ARG_OBJECT = "intType";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome_activity, container, false);
    }

    @SuppressLint("ResourceType")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        int position = args.getInt(ARG_OBJECT);
        switch (position){
            case 1:
                //Welcome
                break;
            case 2:
                //Let's setup permissions
                break;
                //Let's setup password
            case 3:
                break;
            default:
                break;
        }
    }
}
