package com.suslanium.encryptor;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.spec.KeySpec;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {
/*
    public static final int GCM_IV_LENGTH = 12;
    public static final int GCM_TAG_LENGTH = 16;
    private final String pathToStorage = Environment.getExternalStorageDirectory().getPath() + File.separator;
    private byte[] globalKey = null;
    private byte[] globalIV = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (globalKey == null || globalIV == null) {
            checkPassword();
        }
    }

    public void checkPassword() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            if (new File(pathToStorage + "EncryptedKey.enc").exists() && new File(pathToStorage + "EncryptedKIV.enc").exists()) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog);
                builder.setTitle("Enter password");
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File keyEncrypted = new File(pathToStorage + "EncryptedKey.enc");
                        File IVEncrypted = new File(pathToStorage + "EncryptedKIV.enc");
                        try {
                            String password = input.getText().toString();
                            MessageDigest digest = MessageDigest.getInstance("SHA-256");
                            byte[] pass = password.getBytes(StandardCharsets.UTF_8);
                            digest.update(pass, 0, pass.length);
                            byte[] hash = digest.digest();
                            //toastCall("Hash is: " + hash.toString());
                            decryptFileAES_ECB(keyEncrypted, hash, new File(pathToStorage + "DecryptedKey.enc"));
                            File keyDecrypted = new File(pathToStorage + "DecryptedKey.enc");
                            byte[] keyEncoded = Files.readAllBytes(keyDecrypted.toPath());
                            byte[] key = Base64.decode(keyEncoded, Base64.DEFAULT);
                            globalKey = key;
                            decryptFileAES_ECB(IVEncrypted, hash, new File(pathToStorage + "DecryptedIV.enc"));
                            File IVDecrypted = new File(pathToStorage + "DecryptedIV.enc");
                            byte[] IV = Files.readAllBytes(IVDecrypted.toPath());
                            globalIV = IV;
                            keyDecrypted.delete();
                            IVDecrypted.delete();
                            Toast.makeText(MainActivity.this, "Welcome!", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "That password is incorrect.", Toast.LENGTH_LONG).show();
                            checkPassword();
                        } finally {
                            File keyDecrypted = new File(pathToStorage + "DecryptedKey.enc");
                            File IVDecrypted = new File(pathToStorage + "DecryptedIV.enc");
                            keyDecrypted.delete();
                            IVDecrypted.delete();
                            File file1 = new File(pathToStorage + "EncryptedKey.enc" + ".Base64");
                            file1.delete();
                            File file2 = new File(pathToStorage + "EncryptedIV.enc" + ".Base64");
                            file1.delete();
                        }
                    }
                });
                builder.show();
            }
        }
    }


    public void EncryptUI() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog);
        builder.setTitle("Choose file");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(Environment.getExternalStorageDirectory().getPath() + File.separator);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = input.getText().toString();
                if (!fileName.matches("")) {
                    MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(MainActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog);
                    builder2.setTitle("Enter save name and location");
                    final EditText input1 = new EditText(MainActivity.this);
                    input1.setInputType(InputType.TYPE_CLASS_TEXT);
                    input1.setText(Environment.getExternalStorageDirectory().getPath() + File.separator);
                    builder2.setView(input1);
                    builder2.setPositiveButton("Encrypt", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                String saveLoc = input1.getText().toString();
                                if (!saveLoc.matches("")) {
                                    encryptFileThreadAES_GCM(new File(fileName), new File(saveLoc));
                                } else {
                                    Toast.makeText(MainActivity.this, "Key does not exist or the path to file is incorrect. Try generating or importing key", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception ignored) {

                            }
                        }
                    });
                    builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder2.show();

                } else {
                    Toast.makeText(MainActivity.this, "Please enter file location!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
    public void DecryptUI() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog);
        builder.setTitle("Choose file");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(Environment.getExternalStorageDirectory().getPath() + File.separator);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = input.getText().toString();
                if (!fileName.matches("")) {
                    MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(MainActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog);
                    builder2.setTitle("Enter save name and location");
                    final EditText input2 = new EditText(MainActivity.this);
                    input2.setInputType(InputType.TYPE_CLASS_TEXT);
                    input2.setText(Environment.getExternalStorageDirectory().getPath() + File.separator);
                    builder2.setView(input2);
                    builder2.setPositiveButton("Decrypt", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String saveLoc = input2.getText().toString();
                            if (!saveLoc.matches("")) {
                                try {
                                    decryptFileThreadAES_GCM(new File(fileName), new File(saveLoc));
                                } catch (Exception ignored) {
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Key does not exist or the path to file is incorrect. Try generating or importing key", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder2.show();
                } else {
                    Toast.makeText(MainActivity.this, "Please enter file location!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void DecryptFolderUI() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog);
        builder.setTitle("Choose folder");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(Environment.getExternalStorageDirectory().getPath() + File.separator);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = input.getText().toString();
                if (!fileName.matches("")) {
                    MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(MainActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog);
                    builder2.setTitle("Enter save folder");
                    final EditText input2 = new EditText(MainActivity.this);
                    input2.setInputType(InputType.TYPE_CLASS_TEXT);
                    input2.setText(Environment.getExternalStorageDirectory().getPath() + File.separator);
                    builder2.setView(input2);
                    builder2.setPositiveButton("Decrypt", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String saveLoc = input2.getText().toString();
                            if (!saveLoc.matches("")) {
                                try {
                                    decryptFolderThreadAES_GCM(new File(fileName), new File(saveLoc));
                                } catch (Exception ignored) {
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Key does not exist or the path to file is incorrect. Try generating or importing key", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder2.show();
                } else {
                    Toast.makeText(MainActivity.this, "Please enter folder location!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void EncryptFolderUI() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog);
        builder.setTitle("Choose folder");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(Environment.getExternalStorageDirectory().getPath() + File.separator);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = input.getText().toString();
                if (!fileName.matches("")) {
                    MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(MainActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog);
                    builder2.setTitle("Enter save folder");
                    final EditText input1 = new EditText(MainActivity.this);
                    input1.setInputType(InputType.TYPE_CLASS_TEXT);
                    input1.setText(Environment.getExternalStorageDirectory().getPath() + File.separator);
                    builder2.setView(input1);
                    builder2.setPositiveButton("Encrypt", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                String saveLoc = input1.getText().toString();
                                if (!saveLoc.matches("")) {
                                    encryptFolderThreadAES_GCM(new File(fileName), new File(saveLoc));
                                } else {
                                    Toast.makeText(MainActivity.this, "Key does not exist or the path to file is incorrect. Try generating or importing key", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception ignored) {

                            }
                        }
                    });
                    builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder2.show();
                } else {
                    Toast.makeText(MainActivity.this, "Please enter folder location!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void GenerateKeyUI() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog);
        builder.setTitle("Enter password for key generation");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText("Only_16_or24or32_symbols_allowed");
        builder.setView(input);
        builder.setPositiveButton("Generate", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = input.getText().toString();
                if (!password.matches("")) {
                    generateAES_GCM_Key_And_IV(password);
                } else {
                    Toast.makeText(MainActivity.this, "Please, enter password!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void ImportKeyUI() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog);
        builder.setTitle("Choose folder with key and IV");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(Environment.getExternalStorageDirectory().getPath() + File.separator);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String path = input.getText().toString();
                importAES_GCM_Key_And_IV(path);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void ExportKeyUI() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog);
        builder.setTitle("Choose folder to export");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(Environment.getExternalStorageDirectory().getPath() + File.separator);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String path = input.getText().toString();
                exportAES_GCM_Key_And_IV(path);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void checkForPermissionsEncrypt(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            EncryptUI();
        }
    }

    public void checkForPermissionsDecrypt(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            DecryptUI();
        }
    }

    public void checkForPermissionsDecryptFolder(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            DecryptFolderUI();
        }
    }

    public void checkForPermissionsEncryptFolder(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        } else {
            EncryptFolderUI();
        }
    }

    public void checkForPermissionsGenerateKey(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            GenerateKeyUI();
        }
    }

    public void checkForPermissionsImportKey(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            ImportKeyUI();
        }
    }

    public void checkForPermissionsExportKey(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            ExportKeyUI();
        }
    }

    private void encryptFolderThreadAES_GCM(File folder, File folderToSave) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (folderToSave.exists()) {
                    if (folderToSave.isDirectory()) {
                        if (folder.exists() && folder.isDirectory()) {
                            File[] filesInFolder = folder.listFiles();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    toastCall("Encryption started!");
                                }
                            });
                            for (int i = 0; i < filesInFolder.length; i++) {
                                if (filesInFolder[i].isFile()) {
                                    if (folderToSave.getPath().substring(folderToSave.getPath().length() - 1).matches(File.separator)) {
                                        try {
                                            encryptFileAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + filesInFolder[i].getName() + ".enc"));
                                        } catch (Exception e) {
                                            int finalI1 = i;
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    toastCall(filesInFolder[finalI1].getName() + " encryption error: " + e.toString());
                                                }
                                            });
                                        }
                                    } else {
                                        try {
                                            encryptFileAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName() + ".enc"));
                                        } catch (Exception e) {
                                            int finalI = i;
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    toastCall(filesInFolder[finalI].getName() + " encryption error: " + e.toString());
                                                }
                                            });
                                        }
                                    }
                                } else if (filesInFolder[i].isDirectory()) {
                                    if (!folderToSave.getPath().substring(folderToSave.getPath().length() - 1).matches(File.separator)) {
                                        File file = new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName());
                                        file.mkdir();
                                        encryptFolderThreadAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName()));
                                    } else {
                                        File file = new File(folderToSave.getPath() + filesInFolder[i].getName());
                                        file.mkdir();
                                        encryptFolderThreadAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + filesInFolder[i].getName()));
                                    }
                                }
                            }
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    toastCall("Encryption done!");
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    toastCall("The specified path does not exist or is not a directory.");
                                }
                            });
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall("The specified saving path is not a directory.");
                            }
                        });
                    }
                } else {
                    File file = new File(folderToSave.getPath());
                    file.mkdir();
                    if (folder.exists() && folder.isDirectory()) {
                        File[] filesInFolder = folder.listFiles();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall("Encryption started!");
                            }
                        });
                        for (int i = 0; i < filesInFolder.length; i++) {
                            if (filesInFolder[i].isFile()) {
                                if (folderToSave.getPath().substring(folderToSave.getPath().length() - 1).matches(File.separator)) {
                                    try {
                                        encryptFileAES_GCM(filesInFolder[i],  new File(folderToSave.getPath() + filesInFolder[i].getName() + ".enc"));
                                    } catch (Exception e) {
                                        int finalI1 = i;
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                toastCall(filesInFolder[finalI1].getName() + " encryption error: " + e.toString());
                                            }
                                        });
                                    }
                                } else {
                                    try {
                                        encryptFileAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName() + ".enc"));
                                    } catch (Exception e) {
                                        int finalI = i;
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                toastCall(filesInFolder[finalI].getName() + " encryption error: " + e.toString());
                                            }
                                        });
                                    }
                                }
                            } else if (filesInFolder[i].isDirectory()) {
                                if (!folderToSave.getPath().substring(folderToSave.getPath().length() - 1).matches(File.separator)) {
                                    File file2 = new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName());
                                    file2.mkdir();
                                    encryptFolderThreadAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName()));
                                } else {
                                    File file2 = new File(folderToSave.getPath() + filesInFolder[i].getName());
                                    file2.mkdir();
                                    encryptFolderThreadAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + filesInFolder[i].getName()));
                                }
                            }
                        }
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall("Encryption done!");
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall("The specified path does not exist or is not a directory.");
                            }
                        });
                    }
                }
            }
        });
        thread.start();
    }

    private void decryptFolderThreadAES_GCM(File folder, File folderToSave) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (folderToSave.exists()) {
                    if (folderToSave.isDirectory()) {
                        if (folder.exists() && folder.isDirectory()) {
                            File[] filesInFolder = folder.listFiles();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    toastCall("Decryption started!");
                                }
                            });
                            for (int i = 0; i < filesInFolder.length; i++) {
                                if (filesInFolder[i].isFile()) {
                                    if (folderToSave.getPath().substring(folderToSave.getPath().length() - 1).matches(File.separator)) {
                                        try {
                                            if (filesInFolder[i].getName().substring(filesInFolder[i].getName().length() - 4, filesInFolder[i].getName().length()).matches(".enc")) {
                                                decryptFileAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + filesInFolder[i].getName().substring(0, filesInFolder[i].getName().length() - 4)));
                                            } else {
                                                decryptFileAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + filesInFolder[i].getName()));
                                            }
                                        } catch (Exception e) {
                                            int finalI = i;
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    toastCall(filesInFolder[finalI].getName() + " decryption error: " + e.toString());
                                                }
                                            });
                                        }
                                    } else {
                                        try {
                                            if (filesInFolder[i].getName().substring(filesInFolder[i].getName().length() - 4, filesInFolder[i].getName().length()).matches(".enc")) {
                                                decryptFileAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName().substring(0, filesInFolder[i].getName().length() - 4)));
                                            } else {
                                                decryptFileAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName()));
                                            }
                                        } catch (Exception e) {
                                            int finalI1 = i;
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    toastCall(filesInFolder[finalI1].getName() + " decryption error: " + e.toString());
                                                }
                                            });
                                        }
                                    }
                                } else if (filesInFolder[i].isDirectory()) {
                                    if (!folderToSave.getPath().substring(folderToSave.getPath().length() - 1).matches(File.separator)) {
                                        File file = new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName());
                                        file.mkdir();
                                        decryptFolderThreadAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName()));
                                    } else {
                                        File file = new File(folderToSave.getPath() + filesInFolder[i].getName());
                                        file.mkdir();
                                        decryptFolderThreadAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + filesInFolder[i].getName()));
                                    }
                                }
                            }
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    toastCall("Decryption done!");
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    toastCall("The specified path does not exist or is not a directory.");
                                }
                            });
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall("The specified saving path is not a directory.");
                            }
                        });
                    }
                } else {
                    File file = new File(folderToSave.getPath());
                    file.mkdir();
                    if (folder.exists() && folder.isDirectory()) {
                        File[] filesInFolder = folder.listFiles();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall("Decryption started!");
                            }
                        });
                        for (int i = 0; i < filesInFolder.length; i++) {
                            if (filesInFolder[i].isFile()) {
                                if (folderToSave.getPath().substring(folderToSave.getPath().length() - 1).matches(File.separator)) {
                                    try {
                                        if (filesInFolder[i].getName().substring(filesInFolder[i].getName().length() - 4, filesInFolder[i].getName().length()).matches(".enc")) {
                                            decryptFileAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + filesInFolder[i].getName().substring(0, filesInFolder[i].getName().length() - 4)));
                                        } else {
                                            decryptFileAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + filesInFolder[i].getName()));
                                        }
                                    } catch (Exception e) {
                                        int finalI = i;
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                toastCall(filesInFolder[finalI].getName() + " decryption error: " + e.toString());
                                            }
                                        });
                                    }
                                } else {
                                    try {
                                        if (filesInFolder[i].getName().substring(filesInFolder[i].getName().length() - 4, filesInFolder[i].getName().length()).matches(".enc")) {
                                            decryptFileAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName().substring(0, filesInFolder[i].getName().length() - 4)));
                                        } else {
                                            decryptFileAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName()));
                                        }
                                    } catch (Exception e) {
                                        int finalI1 = i;
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                toastCall(filesInFolder[finalI1].getName() + " decryption error: " + e.toString());
                                            }
                                        });
                                    }
                                }
                            } else if (filesInFolder[i].isDirectory()) {
                                if (!folderToSave.getPath().substring(folderToSave.getPath().length() - 1).matches(File.separator)) {
                                    File file2 = new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName());
                                    file2.mkdir();
                                    decryptFolderThreadAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName()));
                                } else {
                                    File file2 = new File(folderToSave.getPath() + filesInFolder[i].getName());
                                    file2.mkdir();
                                    decryptFolderThreadAES_GCM(filesInFolder[i], new File(folderToSave.getPath() + filesInFolder[i].getName()));
                                }
                            }
                        }
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall("Decryption done!");
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall("The specified path does not exist or is not a directory.");
                            }
                        });
                    }
                }
            }
        });
        thread.start();
    }

    private void encryptFileThreadAES_GCM(File file, File fileToSave) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (file.exists() && file.isFile()) {
                    try {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall("Encryption started for " + file.getName());
                            }
                        });
                        encryptFileAES_GCM(file, fileToSave);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall(file.getName() + " succesfully encrypted!");
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall(file.getName() + " encryption error: " + e.toString());
                            }
                        });
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            toastCall("The specified file does not exist or is not a file.");
                        }
                    });
                }
            }
        });
        thread.start();
    }

    private void decryptFileThreadAES_GCM(File file, File fileToSave) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (file.exists() && file.isFile()) {
                    try {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall("Decryption started for " + file.getName());
                            }
                        });
                        decryptFileAES_GCM(file, fileToSave);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall(file.getName() + " succesfully decrypted!");
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall(file.getName() + " decryption error: " + e.toString());
                            }
                        });
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            toastCall("The specified file does not exist or is not a file.");
                        }
                    });
                }
            }
        });
        thread.start();
    }

    private void decryptFileAES_ECB(File file, byte[] password, File fileToSave) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        FileOutputStream fos = new FileOutputStream(fileToSave);
        byte[] encoded = Files.readAllBytes(file.toPath());
        byte[] decoded = Base64.decode(encoded, Base64.DEFAULT);
        FileOutputStream fos2 = new FileOutputStream(new File(file.getPath() + ".Base64"));
        fos2.write(decoded);
        fos2.flush();
        fos2.close();
        FileInputStream fis2 = new FileInputStream(new File(file.getPath() + ".Base64"));
        SecretKeySpec sks = new SecretKeySpec(password, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, sks);
        CipherInputStream cis = new CipherInputStream(fis2, cipher);
        int b;
        byte[] d = new byte[10 * 1024 * 1024];
        while ((b = cis.read(d)) != -1) {
            //byte[] dBase64 = android.util.Base64.decode(d, android.util.Base64.DEFAULT);
            fos.write(d, 0, b);
        }
        fos.flush();
        fos.close();
        cis.close();
        fis2.close();
        File file1 = new File(file.getPath() + ".Base64");
        file1.delete();
    }

    private void encryptFileAES_ECB(File file, byte[] password, File fileToSave) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        FileOutputStream fos = new FileOutputStream(fileToSave);
        SecretKeySpec sks = new SecretKeySpec(password, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, sks);
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);
        int b;
        byte[] d = new byte[10 * 1024 * 1024];
        while ((b = fis.read(d)) != -1) {
            //byte[] dBase64 = android.util.Base64.encode(d, android.util.Base64.DEFAULT);
            cos.write(d, 0, b);
        }
        cos.flush();
        cos.close();
        fis.close();
        byte[] nonEncoded = Files.readAllBytes(fileToSave.toPath());
        byte[] encoded = Base64.encode(nonEncoded, Base64.DEFAULT);
        fileToSave.delete();
        FileOutputStream fos2 = new FileOutputStream(fileToSave);
        fos2.write(encoded);
        fos2.flush();
        fos2.close();
    }

    private void encryptFileAES_GCM(File file, File fileToSave) throws Exception {
        //---------------Encrypt file----------------------------------
        if (globalIV != null && globalKey != null) {
            if (file.length() > 15 * 1024 * 1024) {
                List<File> split = splitFile(file);
                List<File> encryptedSplit = new LinkedList<File>();
                for (int i = 0; i < split.size(); i++) {
                    encryptFileAES_GCM(split.get(i), new File(split.get(i).getPath() + ".enc"));
                    encryptedSplit.add(new File(split.get(i).getPath() + ".enc"));
                }
                zip(fileToSave, encryptedSplit);
                for (int i = 0; i < split.size(); i++) {
                    split.get(i).delete();
                    encryptedSplit.get(i).delete();
                }
            } else {
                FileInputStream fis = new FileInputStream(file);
                FileOutputStream fos = new FileOutputStream(fileToSave);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                SecretKeySpec keySpec = new SecretKeySpec(globalKey, "AES");
                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, globalIV);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
                CipherOutputStream cos = new CipherOutputStream(fos, cipher);
                int b;
                byte[] d = new byte[10 * 1024 * 1024];
                while ((b = fis.read(d)) != -1) {
                    cos.write(d, 0, b);
                }
                cos.flush();
                cos.close();
                fis.close();
            }
        } else {
            throw new Exception("Key or IV does not exist, encryption can't be done. Please create a key first.");
        }
    }

    private void decryptFileAES_GCM(File file, File fileToSave) throws Exception {
        //---------------Encrypt file----------------------------------
        if (globalIV != null && globalKey != null) {
            if (file.length() > 15 * 1024 * 1024) {
                List<File> split = new LinkedList<File>();
                unZip(file, split);
                List<File> decryptedSplit = new LinkedList<File>();
                for (int i = 0; i < split.size(); i++) {
                    decryptFileAES_GCM(split.get(i), new File(split.get(i).getPath() + "dec"));
                    decryptedSplit.add(new File(split.get(i).getPath() + "dec"));
                }
                mergeFiles(decryptedSplit, fileToSave);
                for (int i = 0; i < split.size(); i++) {
                    split.get(i).delete();
                    decryptedSplit.get(i).delete();
                }
            } else {
                FileInputStream fis = new FileInputStream(file);
                FileOutputStream fos = new FileOutputStream(fileToSave);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                SecretKeySpec keySpec = new SecretKeySpec(globalKey, "AES");
                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, globalIV);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
                CipherInputStream cis = new CipherInputStream(fis, cipher);
                int b;
                byte[] d = new byte[10 * 1024 * 1024];
                while ((b = cis.read(d)) != -1) {
                    fos.write(d, 0, b);
                }
                fos.flush();
                fos.close();
                cis.close();
            }
        } else {
            throw new Exception("Key or IV does not exist, decryption can't be done. Please create a key first.");
        }
    }

    private void generateAES_GCM_Key_And_IV(String password) {
        try {
            Toast.makeText(this, "Generating key...", Toast.LENGTH_SHORT).show();
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try (FileOutputStream fileOutputStream = new FileOutputStream(new File(pathToStorage + "key.enc"));FileOutputStream fileOutputStream1 = new FileOutputStream(new File(pathToStorage + "iv.enc"))){
                        SecureRandom random2 = new SecureRandom();
                        byte[] salt = new byte[16];
                        random2.nextBytes(salt);
                        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                        MessageDigest digest1 = MessageDigest.getInstance("SHA-256");
                        String str = new String(digest1.digest(RndPassword.generateRandomPasswordStr(32).getBytes()), StandardCharsets.UTF_8);
                        char[] ch = new char[str.length()];
                        for (int i = 0; i < str.length(); i++) {
                            ch[i] = str.charAt(i);
                        }
                        KeySpec spec = new PBEKeySpec(ch, salt, 65536, 256);
                        SecretKey tmp = factory.generateSecret(spec);
                        SecretKey key = new SecretKeySpec(tmp.getEncoded(), "AES");
                        byte[] IV = new byte[GCM_IV_LENGTH];
                        SecureRandom random = new SecureRandom();
                        random.nextBytes(IV);
                        fileOutputStream.write(android.util.Base64.encode(key.getEncoded(), android.util.Base64.DEFAULT));
                        fileOutputStream1.write(IV);
                        globalIV = IV;
                        globalKey = key.getEncoded();
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] pass = password.getBytes(StandardCharsets.UTF_8);
                        digest.update(pass, 0, pass.length);
                        byte[] hash = digest.digest();
                        encryptFileAES_ECB(new File(pathToStorage + "key.enc"), hash, new File(pathToStorage + "EncryptedKey.enc"));
                        encryptFileAES_ECB(new File(pathToStorage + "iv.enc"), hash, new File(pathToStorage + "EncryptedKIV.enc"));
                        File keyDecrypted = new File(pathToStorage + "key.enc");
                        keyDecrypted.delete();
                        File ivDecrypted = new File(pathToStorage + "iv.enc");
                        ivDecrypted.delete();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall("Key generated succesfully!");
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                toastCall("Error while generating key:" + e.toString());
                            }
                        });
                    }
                }
            });
            thread.start();
        } catch (Exception e) {
            Toast.makeText(this, "Error while generating key:" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void importAES_GCM_Key_And_IV(String pathToKeys) {
        if (new File(pathToKeys).exists() && new File(pathToKeys).isDirectory()) {
            if (pathToKeys.substring(pathToKeys.length() - 1).matches(File.separator)) {
                if (new File(pathToKeys + "EncryptedKey.enc").exists() && new File(pathToKeys + "EncryptedKIV.enc").exists()) {
                    if (new File(pathToStorage + "EncryptedKey.enc").exists() || new File(pathToStorage + "EncryptedKIV.enc").exists()) {
                        MaterialAlertDialogBuilder builder1 = new MaterialAlertDialogBuilder(MainActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog);
                        builder1.setTitle("Warning!");
                        builder1.setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_launcher_foreground));
                        builder1.setMessage("Already existing keys were found. Would you like to replace them(ALL DATA ENCRYPTED BEFORE MAY BE LOST)?");
                        builder1.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                File prevKey = new File(pathToStorage + "EncryptedKey.enc");
                                File prevIV = new File(pathToStorage + "EncryptedKIV.enc");
                                prevKey.delete();
                                prevIV.delete();
                                try {
                                    copyFile(new File(pathToKeys + "EncryptedKIV.enc"), new File(pathToStorage + "EncryptedKIV.enc"));
                                    copyFile(new File(pathToKeys + "EncryptedKey.enc"), new File(pathToStorage + "EncryptedKey.enc"));
                                    Toast.makeText(MainActivity.this, "Importing successful!", Toast.LENGTH_SHORT).show();
                                    Toast.makeText(MainActivity.this, "Keys have been changed. Please enter password for the new keys.", Toast.LENGTH_SHORT).show();
                                    checkPassword();
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "Importing error: " + e.toString(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        builder1.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        builder1.show();
                    } else {
                        try {
                            copyFile(new File(pathToKeys + "EncryptedKIV.enc"), new File(pathToStorage + "EncryptedKIV.enc"));
                            copyFile(new File(pathToKeys + "EncryptedKey.enc"), new File(pathToStorage + "EncryptedKey.enc"));
                            Toast.makeText(MainActivity.this, "Importing successful!", Toast.LENGTH_SHORT).show();
                            Toast.makeText(MainActivity.this, "Keys have been changed. Please enter password for the new keys.", Toast.LENGTH_SHORT).show();
                            checkPassword();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Importing error: " + e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Sorry, no keys found in specified directory", Toast.LENGTH_LONG).show();
                }
            } else {
                if (new File(pathToKeys + File.separator + "EncryptedKey.enc").exists() && new File(pathToKeys + File.separator + "EncryptedKIV.enc").exists()) {
                    if (new File(pathToStorage + "EncryptedKey.enc").exists() || new File(pathToStorage + "EncryptedKIV.enc").exists()) {
                        MaterialAlertDialogBuilder builder1 = new MaterialAlertDialogBuilder(MainActivity.this, R.style.ThemeOverlay_App_MaterialAlertDialog);
                        builder1.setTitle("Warning!");
                        builder1.setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_launcher_foreground));
                        builder1.setMessage("Already existing keys were found. Would you like to replace them(ALL DATA ENCRYPTED BEFORE MAY BE LOST)?");
                        builder1.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                File prevKey = new File(pathToStorage + "EncryptedKey.enc");
                                File prevIV = new File(pathToStorage + "EncryptedKIV.enc");
                                prevKey.delete();
                                prevIV.delete();
                                try {
                                    copyFile(new File(pathToKeys + File.separator + "EncryptedKIV.enc"), new File(pathToStorage + "EncryptedKIV.enc"));
                                    copyFile(new File(pathToKeys + File.separator + "EncryptedKey.enc"), new File(pathToStorage + "EncryptedKey.enc"));
                                    Toast.makeText(MainActivity.this, "Importing successful!", Toast.LENGTH_SHORT).show();
                                    Toast.makeText(MainActivity.this, "Keys have been changed. Please enter password for the new keys.", Toast.LENGTH_SHORT).show();
                                    checkPassword();
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "Importing error: " + e.toString(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        builder1.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        builder1.show();
                    } else {
                        try {
                            copyFile(new File(pathToKeys + File.separator + "EncryptedKIV.enc"), new File(pathToStorage + "EncryptedKIV.enc"));
                            copyFile(new File(pathToKeys + File.separator + "EncryptedKey.enc"), new File(pathToStorage + "EncryptedKey.enc"));
                            Toast.makeText(MainActivity.this, "Importing successful!", Toast.LENGTH_SHORT).show();
                            Toast.makeText(MainActivity.this, "Keys have been changed. Please enter password for the new keys.", Toast.LENGTH_SHORT).show();
                            checkPassword();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Importing error: " + e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Sorry, no keys found in specified directory", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Toast.makeText(this, "Sorry, the specified path does not exist or is not a directory.", Toast.LENGTH_LONG).show();
        }
    }

    private void exportAES_GCM_Key_And_IV(String pathToBackup) {
        boolean matches = pathToBackup.substring(pathToBackup.length() - 1).matches(File.separator);
        if (new File(pathToBackup).exists() && new File(pathToBackup).isDirectory()) {
            if (matches) {
                if (new File(pathToStorage + "EncryptedKey.enc").exists() && new File(pathToStorage + "EncryptedKIV.enc").exists()) {
                    try {
                        copyFile(new File(pathToStorage + "EncryptedKey.enc"), new File(pathToBackup + "EncryptedKey.enc"));
                        copyFile(new File(pathToStorage + "EncryptedKIV.enc"), new File(pathToBackup + "EncryptedKIV.enc"));
                        Toast.makeText(this, "Exporting finished.", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Exporting error: " + e.toString(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Please, create keys before exporting them.", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (new File(pathToStorage + "EncryptedKey.enc").exists() && new File(pathToStorage + "EncryptedKIV.enc").exists()) {
                    try {
                        copyFile(new File(pathToStorage + "EncryptedKey.enc"), new File(pathToBackup + File.separator + "EncryptedKey.enc"));
                        copyFile(new File(pathToStorage + "EncryptedKIV.enc"), new File(pathToBackup + File.separator + "EncryptedKIV.enc"));
                        Toast.makeText(this, "Exporting finished.", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Exporting error: " + e.toString(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Please, create keys before exporting them.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (new File(pathToBackup).exists() && !new File(pathToBackup).isDirectory()) {
            Toast.makeText(this, "The specified path is not a directory", Toast.LENGTH_SHORT).show();
        } else if (!new File(pathToBackup).exists()) {
            File file = new File(pathToBackup);
            file.mkdir();
            if (file.isDirectory()) {
                if (matches) {
                    if (new File(pathToStorage + "EncryptedKey.enc").exists() && new File(pathToStorage + "EncryptedKIV.enc").exists()) {
                        try {
                            copyFile(new File(pathToStorage + "EncryptedKey.enc"), new File(pathToBackup + "EncryptedKey.enc"));
                            copyFile(new File(pathToStorage + "EncryptedKIV.enc"), new File(pathToBackup + "EncryptedKIV.enc"));
                            Toast.makeText(this, "Exporting finished.", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Exporting error: " + e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Please, create keys before exporting them.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (new File(pathToStorage + "EncryptedKey.enc").exists() && new File(pathToStorage + "EncryptedKIV.enc").exists()) {
                        try {
                            copyFile(new File(pathToStorage + "EncryptedKey.enc"), new File(pathToBackup + File.separator + "EncryptedKey.enc"));
                            copyFile(new File(pathToStorage + "EncryptedKIV.enc"), new File(pathToBackup + File.separator + "EncryptedKIV.enc"));
                            Toast.makeText(this, "Exporting finished.", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Exporting error: " + e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Please, create keys before exporting them.", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "The specified path is not a directory", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    private List<File> splitFile(File f) throws IOException {
        int partCounter = 1;
        int sizeOfFiles = 10 * 1024 * 1024;
        byte[] buffer = new byte[sizeOfFiles];
        String fileName = f.getName();
        List<File> splittedFiles = new LinkedList<File>();
        try (FileInputStream fis = new FileInputStream(f); BufferedInputStream bis = new BufferedInputStream(fis)) {
            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                String filePartName = fileName + Integer.toString(partCounter);
                partCounter++;
                File newFile = new File(f.getParent(), filePartName);
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, bytesAmount);
                    splittedFiles.add(newFile);
                }
            }
        }
        return splittedFiles;
    }

    private void mergeFiles(List<File> files, File into) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(into); BufferedOutputStream mergingStream = new BufferedOutputStream(fos)) {
            for (int i = 0; i < files.size(); i++) {
                Files.copy(files.get(i).toPath(), mergingStream);
            }
        }
    }

    private void zip(File output, List<File> sources) throws Exception {
        try(ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(output))) {
            zipOut.setLevel(Deflater.NO_COMPRESSION);
            for (int i = 0; i < sources.size(); i++) {
                zipFile(zipOut, sources.get(i));
            }
            zipOut.flush();
        }
    }

    private void unZip(File input, List<File> output) throws Exception {
        String fileZip = input.getPath();
        File destDir = new File(input.getParent());
        byte[] buffer = new byte[512];
        try(ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File destFile = new File(destDir, zipEntry.getName());
                FileOutputStream fos = new FileOutputStream(destFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                output.add(destFile);
                fos.close();
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    private void zipFile(ZipOutputStream zos, File file) throws IOException {
        zos.putNextEntry(new ZipEntry(file.getName()));
        try(FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[512];
            int byteCount = 0;
            while ((byteCount = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, byteCount);
            }
        }
        zos.closeEntry();
    }

    private void toastCall(CharSequence text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this,"Please grant permissions to use this app", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }*/
}