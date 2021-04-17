package com.suslanium.encryptor;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class EncryptFragment extends Fragment {
    public static final String ARG_OBJECT = "intType";

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
        TextInputEditText key = view.findViewById(R.id.keyEditText);
        TextInputEditText cipher = view.findViewById(R.id.cipherEditText);
        TextInputLayout plainT = view.findViewById(R.id.plainText);
        TextInputLayout keyT = view.findViewById(R.id.keyText);
        TextInputLayout cipherT = view.findViewById(R.id.cipherText);
        Button encryptButton = view.findViewById(R.id.confirmEncrypt);
        RadioGroup group = view.findViewById(R.id.hashGroup);
        RadioButton sha1 = view.findViewById(R.id.sha1);
        RadioButton sha2 = view.findViewById(R.id.sha2);
        RadioButton sha3 = view.findViewById(R.id.sha3);
        RadioButton md5 = view.findViewById(R.id.md5);
        sha1.setId(0);
        sha2.setId(1);
        sha3.setId(2);
        md5.setId(3);
        //MD5, SHA,
        switch (position){
            case 1: //encrypt
                encryptButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String plainText = plain.getText().toString();
                        if(!plainText.matches("")){

                        } else {
                            Snackbar.make(view, "Please enter text to encrypt", Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
                group.setVisibility(View.INVISIBLE);
                break;
            case 2: //decrypt
                //plain.setHint("Ciphertext");
                plainT.setHint("Ciphertext");
                //cipher.setHint("Plain text");
                cipherT.setHint("Plain text");
                encryptButton.setText("Decrypt");
                group.setVisibility(View.INVISIBLE);
                break;
            case 3: //hash
                //cipher.setHint("Hash output");
                cipherT.setHint("Hash output");
                keyT.setVisibility(View.INVISIBLE);
                final String[] hashFunction = {null};
                group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        //Log.d("a", String.valueOf(checkedId));
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
        //((TextView) view.findViewById(R.id.encText))
                //.setText(Integer.toString(args.getInt(ARG_OBJECT)));
    }
}
