package com.suslanium.encryptor.ui.messagecrypt;

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
        Fragment fragment = new EncryptFragment();
        Bundle args = new Bundle();
        args.putInt(EncryptFragment.ARG_OBJECT, position + 1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
