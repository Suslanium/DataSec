package com.suslanium.encryptor.ui.gallery;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.suslanium.encryptor.Encryptor;
import com.suslanium.encryptor.Explorer;
import com.suslanium.encryptor.PasswordAdapter;
import com.suslanium.encryptor.R;
import com.suslanium.encryptor.passwordAdd;
import com.suslanium.encryptor.ui.home.HomeFragment;

import net.sqlcipher.database.SQLiteDatabase;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static com.suslanium.encryptor.ui.home.HomeFragment.fadeIn;
import static com.suslanium.encryptor.ui.home.HomeFragment.fadeOut;

public class GalleryFragment extends Fragment {
    private boolean isOnResume = false;
    private Intent intent2 = null;
    private String currentSearchQuery;
    private ProgressBar searchProgress;
    private TextView searchText;
    private RecyclerView recyclerView;
    private View.OnClickListener addDataListener;
    private View.OnClickListener cancelSearchListener;
    private Drawable createDrawable;
    private Drawable cancelDrawable;
    private ImageButton b1;
    private FloatingActionButton fab;
    private PasswordAdapter adapter = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onDestroyView() {
        ((Explorer) requireActivity()).passwordVaultVisible = false;
        super.onDestroyView();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((Explorer) requireActivity()).passwordVaultVisible = true;
        Toolbar t = requireActivity().findViewById(R.id.toolbar);
        if(((Explorer) requireActivity()).searchButton != null)t.removeView(((Explorer) requireActivity()).searchButton);
        if(((Explorer) requireActivity()).searchBar != null) {
            t.removeView(((Explorer) requireActivity()).searchBar);
            ((Explorer) requireActivity()).searchBar = null;
            final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        b1 = new ImageButton(requireContext());
        Drawable drawable;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            drawable = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_search);
        } else {
            drawable = getResources().getDrawable(android.R.drawable.ic_menu_search);
        }
        b1.setImageDrawable(drawable);
        b1.setBackgroundColor(Color.parseColor("#00000000"));
        Toolbar.LayoutParams l3=new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
        l3.gravity= Gravity.END;
        b1.setLayoutParams(l3);
        ((Explorer) requireActivity()).searchButton = b1;
        t.addView(b1);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            createDrawable = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_input_add);
        } else {
            createDrawable = getResources().getDrawable(android.R.drawable.ic_input_add);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cancelDrawable = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_delete);
        } else {
            cancelDrawable = getResources().getDrawable(android.R.drawable.ic_delete);
        }
        View.OnClickListener searchListener = v -> {
            String searchQuery = ((Explorer) requireActivity()).searchBar.getText().toString();
            if(!searchQuery.matches("")){
                b1.setEnabled(false);
                currentSearchQuery = searchQuery;
                updateView(v);
                if (((Explorer) requireActivity()).searchBar != null) {
                    t.removeView(((Explorer) requireActivity()).searchBar);
                    ((Explorer) requireActivity()).searchBar = null;
                    final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            } else {
                Snackbar.make(v, "Please enter service name to search", Snackbar.LENGTH_LONG).show();
            }
        };
        fab = requireView().findViewById(R.id.addData);
        b1.setOnClickListener(v -> {
            if(((Explorer) requireActivity()).searchBar == null) {
                EditText layout = new EditText(requireContext());
                Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                layoutParams.gravity = Gravity.START;
                layout.setLayoutParams(l3);
                layout.setHint("Enter service name here...");
                layout.setTextColor(Color.parseColor("#FFFFFF"));
                layout.setSingleLine(true);
                t.addView(layout, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                layout.setFocusableInTouchMode(true);
                layout.requestFocus();
                layout.setOnKeyListener((v1, keyCode, event) -> {
                    if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER){
                        searchListener.onClick(v);
                        return true;
                    }
                    return false;
                });
                final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(layout, InputMethodManager.SHOW_IMPLICIT);
                ((Explorer) requireActivity()).searchBar = layout;
            } else {
                searchListener.onClick(v);
            }
        });
        intent2 = ((Explorer) requireActivity()).getIntent2();
        addDataListener = v -> {
            Intent intent = new Intent(requireActivity(), passwordAdd.class);
            intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
            startActivity(intent);
        };
        fab.setOnClickListener(addDataListener);
        cancelSearchListener = v -> {
            fab.setImageDrawable(createDrawable);
            currentSearchQuery = "";
            updateView(v);
            fab.setOnClickListener(addDataListener);
        };
        recyclerView = requireView().findViewById(R.id.passwords);
        searchProgress = requireView().findViewById(R.id.passwordSearchProgress);
        searchText = requireView().findViewById(R.id.passwordSearchText);
        updateView(view);
    }

    public void onThreadDone(ArrayList<String> strings2, ArrayList<Integer> id, ArrayList<String> logins){
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        if(adapter == null) {
            adapter = new PasswordAdapter(strings2, id,logins, intent2, requireActivity());
        } else {
            adapter.setNewData(strings2, id,logins);
        }
        recyclerView.setAdapter(adapter);
        requireActivity().runOnUiThread(() -> {
            fadeIn(searchProgress);
            fadeIn(searchText);
            fadeOut(recyclerView);
            if(currentSearchQuery != null && !currentSearchQuery.matches("")) {
                b1.setEnabled(true);
                fab.setOnClickListener(cancelSearchListener);
                fab.setImageDrawable(cancelDrawable);
            }
            fab.setEnabled(true);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isOnResume) {
            updateView(requireView());
        }
        else {
            isOnResume = true;
        }
    }

    private void updateView(View view){
        fadeIn(recyclerView);
        fadeOut(searchProgress);
        fadeOut(searchText);
        fab.setEnabled(false);
        ArrayList<String> strings3 = new ArrayList<>();
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<String> logins = new ArrayList<>();
        Thread thread = new Thread(() -> {
            try {
                byte[] pass = intent2.getByteArrayExtra("pass");
                String password = Encryptor.rsadecrypt(pass);
                SQLiteDatabase database = Encryptor.initDataBase(requireContext(), password);
                HashMap<Integer, ArrayList<String>> listHashMap = Encryptor.readPasswordData(database);
                Set<Integer> integers = listHashMap.keySet();
                HashMap<Integer, String> names = new HashMap<>();
                for (Integer i : integers) {
                    ArrayList<String> strings = listHashMap.get(i);
                    String s = strings.get(0);
                    String l = strings.get(1);
                    if(currentSearchQuery != null && !currentSearchQuery.matches("")) {
                        if(s.contains(currentSearchQuery)){
                            names.put(i,s);
                            strings3.add(s);
                            logins.add(l);
                            ids.add(i);
                        }
                    } else {
                        names.put(i,s);
                        strings3.add(s);
                        logins.add(l);
                        ids.add(i);
                    }
                }
                requireActivity().runOnUiThread(() -> onThreadDone(strings3, ids, logins));
                HashMap<Integer, byte[]> iconsList = Encryptor.readPasswordIcons(database);
                Set<Integer> integerSet = iconsList.keySet();
                ArrayList<Bitmap> bitmapArrayList = new ArrayList<>();
                for(Integer i: integerSet){
                    if(ids.contains(i)) {
                        byte[] image = iconsList.get(i);
                        if(image != null) {
                            Bitmap.Config config = Bitmap.Config.ARGB_8888;
                            Bitmap bitmap = Bitmap.createBitmap(256, 256, config);
                            ByteBuffer byteBuffer = ByteBuffer.wrap(image);
                            bitmap.copyPixelsFromBuffer(byteBuffer);
                            bitmapArrayList.add(bitmap);
                        } else {
                            bitmapArrayList.add(null);
                        }
                    }
                }
                while (adapter == null){
                    try {
                        Thread.sleep(100);
                    } catch (Exception e){
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
                adapter.setIcons(bitmapArrayList);
                Encryptor.closeDataBase(database);
            } catch (Exception e){
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> Snackbar.make(view, "Failed to read database(perhaps your password is wrong?).", Snackbar.LENGTH_LONG).show());
            }
        });
        thread.start();
    }
}