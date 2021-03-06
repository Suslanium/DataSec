package com.suslanium.encryptor.ui.messagecrypt;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.suslanium.encryptor.ui.Explorer;
import com.suslanium.encryptor.R;

public class MessageFragment extends Fragment {

    public MessageFragment() {}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((Explorer) requireActivity()).setMessageCryptVisible(true);
        Toolbar t = requireActivity().findViewById(R.id.toolbar);
        if(((Explorer) requireActivity()).getSearchButton() != null)t.removeView(((Explorer) requireActivity()).getSearchButton());
        if(((Explorer) requireActivity()).getSearchBar() != null) {
            t.removeView(((Explorer) requireActivity()).getSearchBar());
            ((Explorer) requireActivity()).setSearchBar(null);
            final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        MessageCryptCollectionAdapter messageCryptCollectionAdapter = new MessageCryptCollectionAdapter(this);
        ViewPager2 viewPager = view.findViewById(R.id.pager);
        viewPager.setAdapter(messageCryptCollectionAdapter);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position+1){
                case 1:tab.setText(R.string.encryptMsg);
                break;
                case 2:tab.setText(R.string.decryptMsg);
                break;
                case 3:tab.setText(R.string.hashMsg);
                break;
                default:tab.setText("");
                break;
            }
        }).attach();
    }

    @Override
    public void onDestroyView() {
        ((Explorer) requireActivity()).setMessageCryptVisible(false);
        super.onDestroyView();
    }
}

