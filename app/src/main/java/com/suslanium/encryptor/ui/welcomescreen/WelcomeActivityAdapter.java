package com.suslanium.encryptor.ui.welcomescreen;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class WelcomeActivityAdapter extends FragmentStateAdapter {
    public WelcomeActivityAdapter(WelcomeActivity fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = new WelcomeActivityFragment();
        Bundle args = new Bundle();
        args.putInt(WelcomeActivityFragment.ARG_OBJECT, position + 1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
