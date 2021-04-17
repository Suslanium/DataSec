package com.suslanium.encryptor;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class EncryptFragment extends Fragment {
    public static final String ARG_OBJECT = "intType";
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_encrypt, container, false);
    }

    @SuppressLint("ResourceType")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        //getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        int position = args.getInt(ARG_OBJECT);
        TextInputEditText plain = view.findViewById(R.id.plainEditText);
        plain.setSingleLine(true);
        TextInputEditText key = view.findViewById(R.id.keyEditText);
        key.setSingleLine(true);
        TextInputEditText cipher = view.findViewById(R.id.cipherEditText);
        cipher.setSingleLine(true);
        TextInputLayout plainT = view.findViewById(R.id.plainText);
        TextInputLayout keyT = view.findViewById(R.id.keyText);
        TextInputLayout cipherT = view.findViewById(R.id.cipherText);
        Button encryptButton = view.findViewById(R.id.confirmEncrypt);
        RadioGroup group = view.findViewById(R.id.hashGroup);
        RadioButton sha1 = view.findViewById(R.id.sha1);
        RadioButton sha2 = view.findViewById(R.id.sha2);
        RadioButton sha3 = view.findViewById(R.id.sha3);
        RadioButton md5 = view.findViewById(R.id.md5);
        ProgressBar bar = view.findViewById(R.id.encProgressBar);
        bar.setVisibility(View.INVISIBLE);
        sha1.setId(0);
        sha2.setId(1);
        sha3.setId(2);
        md5.setId(3);
        FloatingActionButton copy = view.findViewById(R.id.copyButton);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Encryptor", cipher.getText().toString());
                if (clipboard == null || clip == null) return;
                clipboard.setPrimaryClip(clip);
                Snackbar.make(view, "Copied to clipboard!", Snackbar.LENGTH_LONG).show();
            }
        });
        switch (position){
            case 1:
                encryptButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String plainText = plain.getText().toString();
                        if(!plainText.matches("")){
                            String keyText = key.getText().toString();
                            if(!keyText.matches("")){
                                if(!isLoading) {
                                    bar.setVisibility(View.VISIBLE);
                                    isLoading = true;
                                    Thread thread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            byte[] plain = plainText.getBytes();
                                            byte[] encrypted = Encryptor.encryptBytesAES256(plain, keyText);
                                            try {
                                                String enc = Base64.encodeToString(encrypted, Base64.DEFAULT);
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        cipher.setText(enc);
                                                        isLoading = false;
                                                        bar.setVisibility(View.INVISIBLE);
                                                    }
                                                });
                                            } catch (Exception e) {
                                                try {
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Snackbar.make(view, "Something went wrong...", Snackbar.LENGTH_LONG).show();
                                                        }
                                                    });
                                                } catch (Exception e2) {
                                                    e2.printStackTrace();
                                                }
                                                e.printStackTrace();
                                                isLoading = false;
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        bar.setVisibility(View.INVISIBLE);
                                                    }
                                                });
                                            }
                                        }
                                    });
                                    thread.start();
                                } else {
                                    Snackbar.make(view, "Please wait...", Snackbar.LENGTH_LONG).show();
                                }
                            } else {
                                Snackbar.make(view, "Please enter key for encryption", Snackbar.LENGTH_LONG).show();
                            }
                        } else {
                            Snackbar.make(view, "Please enter text to encrypt", Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
                group.setVisibility(View.INVISIBLE);
                break;
            case 2:
                plainT.setHint("Ciphertext");
                cipherT.setHint("Plain text");
                encryptButton.setText("Decrypt");
                encryptButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String plainText = plain.getText().toString();
                        if(!plainText.matches("")){
                            String keyText = key.getText().toString();
                            if(!keyText.matches("")){
                                if(!isLoading) {
                                    isLoading = true;
                                    bar.setVisibility(View.VISIBLE);
                                    Thread thread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            byte[] encrypted = Base64.decode(plainText, Base64.DEFAULT);
                                            byte[] decrypted = Encryptor.decryptBytesAES256(encrypted, keyText);
                                            try {
                                                String dec = new String(decrypted);
                                                isLoading = false;
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        cipher.setText(dec);
                                                        bar.setVisibility(View.INVISIBLE);
                                                    }
                                                });
                                            } catch (Exception e) {
                                                try {
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Snackbar.make(view, "Wrong key or bad ciphertext", Snackbar.LENGTH_LONG).show();
                                                        }
                                                    });
                                                } catch (Exception e2) {
                                                    e2.printStackTrace();
                                                }
                                                e.printStackTrace();
                                                isLoading = false;
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        bar.setVisibility(View.INVISIBLE);
                                                    }
                                                });
                                            }
                                        }
                                    });
                                    thread.start();
                                } else {
                                    Snackbar.make(view, "Please wait...", Snackbar.LENGTH_LONG).show();
                                }
                            } else {
                                Snackbar.make(view, "Please enter key for decryption", Snackbar.LENGTH_LONG).show();
                            }
                        } else {
                            Snackbar.make(view, "Please enter text for decryption", Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
                group.setVisibility(View.INVISIBLE);
                break;
            case 3:
                cipherT.setHint("Hash output");
                keyT.setVisibility(View.INVISIBLE);
                final String[] hashFunction = {null};
                group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        switch (checkedId){
                            case 0: hashFunction[0] = "SHA"; break;
                            case 1: hashFunction[0] = "SHA-256"; break;
                            case 2: hashFunction[0] = "SHA-512"; break;
                            case 3: hashFunction[0] = "MD5"; break;
                            default:break;
                        }
                    }
                });
                encryptButton.setText("Calculate hash");
                encryptButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String plainText = plain.getText().toString();
                        if(!plainText.matches("")){
                            String s = Encryptor.calculateHash(plainText, hashFunction[0]);
                            cipher.setText(s);
                        } else {
                            Snackbar.make(view, "Please enter text", Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
                break;
        }
    }
}
