package com.suslanium.encryptor.ui.password;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.suslanium.encryptor.ui.Explorer;
import com.suslanium.encryptor.ui.PasswordEntry;
import com.suslanium.encryptor.R;

import java.util.ArrayList;

import static com.suslanium.encryptor.ui.explorer.ExplorerFragment.fadeIn;
import static com.suslanium.encryptor.ui.explorer.ExplorerFragment.fadeOut;
import static com.suslanium.encryptor.ui.explorer.ExplorerFragment.getTapTarget;

public class PasswordFragment extends Fragment {
    private Intent intent2 = null;
    protected ProgressBar searchProgress;
    protected TextView searchText;
    protected RecyclerView recyclerView;
    private View.OnClickListener addDataListener;
    private View.OnClickListener cancelSearchListener;
    private Drawable createDrawable;
    private Drawable cancelDrawable;
    private ImageButton searchButton;
    protected FloatingActionButton fab;
    private PasswordAdapter adapter = null;
    protected FloatingActionButton newCategory;
    protected PasswordViewModel viewModel;
    protected int currentOperationNumber = 0;
    private boolean tutorialComplete = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())).get(PasswordViewModel.class);
        viewModel.setIntent(((Explorer) requireActivity()).getIntent2());
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        tutorialComplete = preferences.getBoolean("passwordTutorialComplete",false);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_password, container, false);
    }

    @Override
    public void onDestroyView() {
        ((Explorer) requireActivity()).setPasswordVaultVisible(false);
        super.onDestroyView();
    }

    protected void setCategory(String category) {
        if(currentOperationNumber == 0) {
            viewModel.setCurrentCategory(category);
            updateView(requireView());
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        Explorer parent = (Explorer) context;
        parent.setPasswordFragment(this);
        super.onAttach(context);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((Explorer) requireActivity()).setPasswordVaultVisible(true);
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        if (((Explorer) requireActivity()).getSearchButton() != null)
            toolbar.removeView(((Explorer) requireActivity()).getSearchButton());
        if (((Explorer) requireActivity()).getSearchBar() != null) {
            toolbar.removeView(((Explorer) requireActivity()).getSearchBar());
            ((Explorer) requireActivity()).setSearchBar(null);
            final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        searchButton = new ImageButton(requireContext());
        Drawable drawable;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search);
        } else {
            drawable = getResources().getDrawable(R.drawable.ic_search);
        }
        searchButton.setImageDrawable(drawable);
        searchButton.setBackgroundColor(Color.parseColor("#00000000"));
        Toolbar.LayoutParams l3 = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
        l3.gravity = Gravity.END;
        searchButton.setLayoutParams(l3);
        ((Explorer) requireActivity()).setSearchButton(searchButton);
        toolbar.addView(searchButton);
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
            if(currentOperationNumber == 0) {
                String searchQuery = ((Explorer) requireActivity()).getSearchBar().getText().toString();
                if (!searchQuery.matches("")) {
                    searchButton.setEnabled(false);
                    viewModel.setCurrentSearchQuery(searchQuery);
                    updateView(v);
                    if (((Explorer) requireActivity()).getSearchBar() != null) {
                        toolbar.removeView(((Explorer) requireActivity()).getSearchBar());
                        ((Explorer) requireActivity()).setSearchBar(null);
                        final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                } else {
                    Snackbar.make(v, R.string.searchServiceErr, Snackbar.LENGTH_LONG).show();
                }
            }
        };
        fab = requireView().findViewById(R.id.addData);
        searchButton.setOnClickListener(v -> {
            if (((Explorer) requireActivity()).getSearchBar() == null) {
                EditText layout = new EditText(requireContext());
                Typeface ubuntu = ResourcesCompat.getFont(requireContext(), R.font.ubuntu);
                layout.setTypeface(ubuntu);
                Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                layoutParams.gravity = Gravity.START;
                layout.setLayoutParams(l3);
                layout.setHint(R.string.enterServiceName);
                layout.setTextColor(Color.parseColor("#FFFFFF"));
                layout.setSingleLine(true);
                toolbar.addView(layout, Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
                layout.setFocusableInTouchMode(true);
                layout.requestFocus();
                layout.setOnKeyListener((v1, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        searchListener.onClick(v);
                        return true;
                    }
                    return false;
                });
                final InputMethodManager inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(layout, InputMethodManager.SHOW_IMPLICIT);
                ((Explorer) requireActivity()).setSearchBar(layout);
            } else {
                searchListener.onClick(v);
            }
        });
        intent2 = ((Explorer) requireActivity()).getIntent2();
        addDataListener = v -> {
            if(currentOperationNumber == 0) {
                Intent intent = new Intent(requireActivity(), PasswordEntry.class);
                intent.putExtra("pass", intent2.getByteArrayExtra("pass"));
                intent.putExtra("category", viewModel.getCurrentCategory().getValue());
                intent.putExtra("newEntry", true);
                startActivity(intent);
            }
        };
        fab.setOnClickListener(addDataListener);
        cancelSearchListener = v -> {
            if(currentOperationNumber == 0) {
                fab.setImageDrawable(createDrawable);
                viewModel.setCurrentSearchQuery("");
                updateView(v);
                fab.setOnClickListener(addDataListener);
            }
        };
        recyclerView = requireView().findViewById(R.id.passwords);
        searchProgress = requireView().findViewById(R.id.passwordSearchProgress);
        searchText = requireView().findViewById(R.id.passwordSearchText);
        newCategory = requireActivity().findViewById(R.id.newCategory);
        LiveData<ArrayList<Integer>> ids = viewModel.getIds();
        final Observer<ArrayList<Integer>> idObserver = integers -> onThreadDone(viewModel.getNames().getValue(), integers, viewModel.getLogins().getValue(), viewModel.getIcons().getValue());
        ids.observe(getViewLifecycleOwner(), idObserver);
        newCategory.setOnClickListener(v -> {
            if (currentOperationNumber == 0) {
                final EditText input = new EditText(requireContext());
                Typeface ubuntu = ResourcesCompat.getFont(requireContext(), R.font.ubuntu);
                input.setTypeface(ubuntu);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setSingleLine(true);
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_rounded);
                builder.setTitle(R.string.categoryName);
                builder.setView(input);
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.create, (dialog, which) -> {
                });
                builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
                });
                AlertDialog dialog2 = builder.create();
                dialog2.show();
                dialog2.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v12 -> {
                    String name = input.getText().toString();
                    if (!name.matches("")) {
                        Thread thread = new Thread(() -> {
                            try {
                                boolean created = viewModel.createCategory(name);
                                if (created) {
                                    requireActivity().runOnUiThread(() -> {
                                        dialog2.dismiss();
                                        updateView(requireView());
                                    });
                                } else {
                                    requireActivity().runOnUiThread(() -> Snackbar.make(v12, R.string.catExists, Snackbar.LENGTH_LONG));
                                }
                            } catch (Exception ignored) {
                            }
                        });
                        thread.start();
                    } else {
                        Snackbar.make(v12, R.string.enterCatName, Snackbar.LENGTH_LONG);
                    }
                });
            }
        });
    }

    private void onThreadDone(ArrayList<String> strings2, ArrayList<Integer> id, ArrayList<String> logins, ArrayList<Bitmap> icons) {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        if (adapter == null) {
            adapter = new PasswordAdapter(strings2, id, logins, viewModel.getCategories().getValue(), intent2, requireActivity(), this);
            if(!tutorialComplete)showHints();
        } else {
            adapter.setNewData(strings2, id, logins, viewModel.getCategories().getValue());
        }
        adapter.setIcons(icons);
        recyclerView.setAdapter(adapter);
        requireActivity().runOnUiThread(() -> {
            fadeIn(searchProgress);
            fadeIn(searchText);
            fadeOut(recyclerView);
            if (viewModel.getCurrentSearchQuery().getValue() != null && !viewModel.getCurrentSearchQuery().getValue().matches("")) {
                searchButton.setEnabled(true);
                fab.setOnClickListener(cancelSearchListener);
                fab.setImageDrawable(cancelDrawable);
            }
            fab.setEnabled(true);
            currentOperationNumber = 0;
        });
    }

    public void backPress() {
        if(currentOperationNumber == 0) {
            if (viewModel.getCurrentSearchQuery().getValue() != null && !viewModel.getCurrentSearchQuery().getValue().matches("")) {
                fab.setImageDrawable(createDrawable);
                viewModel.setCurrentSearchQuery("");
                updateView(requireView());
                fab.setOnClickListener(addDataListener);
            } else if (viewModel.getCurrentCategory().getValue() != null && !viewModel.getCurrentCategory().getValue().matches("")) {
                viewModel.setCurrentCategory(null);
                updateView(requireView());
            } else {
                requireActivity().moveTaskToBack(true);
                requireActivity().finishAffinity();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView(requireView());
    }

    private void updateView(View view) {
        if(currentOperationNumber == 0) {
            currentOperationNumber++;
            fadeIn(recyclerView);
            fadeOut(searchProgress);
            fadeOut(searchText);
            fab.setEnabled(false);
            newCategory.setEnabled(false);
            Thread thread = new Thread(() -> {
                try {
                    boolean b = viewModel.updateList();
                    if (b) {
                        requireActivity().runOnUiThread(() -> newCategory.setEnabled(true));
                    }
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> Snackbar.make(view, getString(R.string.failedToReadDB), Snackbar.LENGTH_LONG).show());
                    e.printStackTrace();
                }
            });
            thread.start();
        }
    }

    protected String getCurrentCategory() {
        return viewModel.getCurrentCategory().getValue();
    }

    private void showHints(){
        Typeface ubuntu = ResourcesCompat.getFont(requireContext(), R.font.ubuntu);
        new TapTargetSequence(requireActivity()).targets(
                getTapTarget(fab,getString(R.string.passwordHintTitle1),getString(R.string.passwordHintMessage1),ubuntu),
                getTapTarget(newCategory, getString(R.string.passwordHintTitle2),getString(R.string.passwordHintMessage2),ubuntu),
                getTapTarget(searchButton, getString(R.string.explorerHintTitle3), getString(R.string.passwordHintMessage3),ubuntu)
        ).listener(new TapTargetSequence.Listener() {
            @Override
            public void onSequenceFinish() {
                SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
                preferences.putBoolean("passwordTutorialComplete", true);
                preferences.apply();
            }

            @Override
            public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {

            }

            @Override
            public void onSequenceCanceled(TapTarget lastTarget) {

            }
        }).start();
    }
}