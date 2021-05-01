package com.suslanium.encryptor;

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

public class MessageFragment extends Fragment {
    //#00B3A4
    //00FFFFFF
    MessageCryptCollectionAdapter messageCryptCollectionAdapter;
    ViewPager2 viewPager;
    public MessageFragment() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_message, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((Explorer) requireActivity()).messageCryptVisible = true;
        Toolbar t = requireActivity().findViewById(R.id.toolbar);
        if(((Explorer) requireActivity()).searchButton != null)t.removeView(((Explorer) requireActivity()).searchButton);
        if(((Explorer) requireActivity()).searchBar != null) {
            t.removeView(((Explorer) requireActivity()).searchBar);
            ((Explorer) requireActivity()).searchBar = null;
            final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        messageCryptCollectionAdapter = new MessageCryptCollectionAdapter(this);
        viewPager = view.findViewById(R.id.pager);
        viewPager.setAdapter(messageCryptCollectionAdapter);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position+1){
                case 1:tab.setText("Encrypt");
                break;
                case 2:tab.setText("Decrypt");
                break;
                case 3:tab.setText("Hash");
                break;
                default:tab.setText("");
                break;
            }
        }).attach();
    }

    @Override
    public void onDestroyView() {
        ((Explorer) requireActivity()).messageCryptVisible = false;
        super.onDestroyView();
    }
}

