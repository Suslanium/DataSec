package com.suslanium.encryptor;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MessageCryptCollectionAdapter extends FragmentStateAdapter {
    public MessageCryptCollectionAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return a NEW fragment instance in createFragment(int)
        Fragment fragment = new EncryptFragment();
        Bundle args = new Bundle();
        // Our object is just an integer :-P
        args.putInt(EncryptFragment.ARG_OBJECT, position + 1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}