package com.suslanium.encryptor.ui.gallery;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
    private String currentCategory;
    private ArrayList<String> categories = new ArrayList<>();
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
    private FloatingActionButton newCategory;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onDestroyView() {
        ((Explorer) requireActivity()).passwordVaultVisible = false;
        super.onDestroyView();
    }

    public void setCategory(String category){
        currentCategory = category;
        updateView(requireView());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        Explorer parent = (Explorer) context;
        parent.setGalleryFragment(this);
        super.onAttach(context);
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
            drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search);
        } else {
            drawable = getResources().getDrawable(R.drawable.ic_search);
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
                Snackbar.make(v, R.string.searchServiceErr, Snackbar.LENGTH_LONG).show();
            }
        };
        fab = requireView().findViewById(R.id.addData);
        b1.setOnClickListener(v -> {
            if(((Explorer) requireActivity()).searchBar == null) {
                EditText layout = new EditText(requireContext());
                Typeface ubuntu = ResourcesCompat.getFont(requireContext(), R.font.ubuntu);
                layout.setTypeface(ubuntu);
                Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                layoutParams.gravity = Gravity.START;
                layout.setLayoutParams(l3);
                layout.setHint(R.string.enterServiceName);
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
            intent.putExtra("category", currentCategory);
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
        newCategory = requireActivity().findViewById(R.id.newCategory);
        updateView(view);
        newCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(requireContext());
                Typeface ubuntu = ResourcesCompat.getFont(requireContext(), R.font.ubuntu);
                input.setTypeface(ubuntu);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setSingleLine(true);
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded)
                        .setTitle(R.string.categoryName)
                        .setView(input)
                        .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String name = input.getText().toString();
                                if(!name.matches("")){
                                    Thread thread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                byte[] pass = intent2.getByteArrayExtra("pass");
                                                String password = Encryptor.rsadecrypt(pass);
                                                SQLiteDatabase database = Encryptor.initDataBase(requireContext(), password);
                                                ArrayList<String> categories = Encryptor.getCategories(database);
                                                if(!categories.contains(name)){
                                                    Encryptor.createCategoryStub(database, name);
                                                    requireActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            updateView(requireView());
                                                        }
                                                    });
                                                } else {
                                                    requireActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Snackbar.make(v,R.string.catExists, Snackbar.LENGTH_LONG);
                                                        }
                                                    });
                                                }
                                            } catch (Exception e) {

                                            }
                                        }
                                    });
                                    thread.start();
                                } else {
                                    Snackbar.make(v,R.string.enterCatName, Snackbar.LENGTH_LONG);
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> { });
                builder.show();
            }
        });
    }

    public void onThreadDone(ArrayList<String> strings2, ArrayList<Integer> id, ArrayList<String> logins){
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        if(adapter == null) {
            adapter = new PasswordAdapter(strings2, id,logins,categories, intent2, requireActivity(), this);
        } else {
            adapter.setNewData(strings2, id,logins,categories);
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

    public void backPress(){
        if(currentSearchQuery != null && !currentSearchQuery.matches("")){
            fab.setImageDrawable(createDrawable);
            currentSearchQuery = "";
            updateView(requireView());
            fab.setOnClickListener(addDataListener);
        } else if(currentCategory != null && !currentCategory.matches("")){
            currentCategory = null;
            updateView(requireView());
        }
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
        newCategory.setEnabled(false);
        ArrayList<String> strings3 = new ArrayList<>();
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<String> logins = new ArrayList<>();
        categories.clear();
        //categories.addAll(Encryptor.getCategories(database));
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
                    String c = strings.get(5);
                    if(currentSearchQuery != null && !currentSearchQuery.matches("")) {
                        if(s.contains(currentSearchQuery)){
                            names.put(i,s);
                            strings3.add(s);
                            logins.add(l);
                            ids.add(i);
                        }
                    } else if(currentCategory != null && !currentCategory.matches("")){
                        if(c != null && c.equals(currentCategory)){
                            names.put(i,s);
                            strings3.add(s);
                            logins.add(l);
                            ids.add(i);
                        }
                    } else {
                        if(c == null || c.matches("")) {
                            names.put(i, s);
                            strings3.add(s);
                            logins.add(l);
                            ids.add(i);
                        }
                    }
                }
                if((currentSearchQuery == null || currentSearchQuery.matches("")) && (currentCategory == null || currentCategory.matches(""))) {
                    categories.addAll(Encryptor.getCategories(database));
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            newCategory.setEnabled(true);
                        }
                    });
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

                        Thread.currentThread().interrupt();
                    }
                }
                adapter.setIcons(bitmapArrayList);
                Encryptor.closeDataBase(database);
            } catch (Exception e){

                requireActivity().runOnUiThread(() -> Snackbar.make(view, "Failed to read database(perhaps your password is wrong?).", Snackbar.LENGTH_LONG).show());
            }
        });
        thread.start();
    }

    public String getCurrentCategory(){
        return currentCategory;
    }
}