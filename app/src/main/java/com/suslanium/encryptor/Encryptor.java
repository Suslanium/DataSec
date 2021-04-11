package com.suslanium.encryptor;

import android.content.Context;
import android.icu.util.Calendar;
import android.os.Environment;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.Nullable;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SqliteWrapper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

public final class Encryptor {
    //region File or folder encryption
    private static final int PBKDF2_ITERATION_COUNT = 300_000;
    private static final int PBKDF2_SALT_LENGTH = 16; //128 bits
    private static final int AES_KEY_LENGTH = 256; //in bits
    // An initialization vector size
    private static final int GCM_NONCE_LENGTH = 12; //96 bits
    // An authentication tag size
    private static final int GCM_TAG_LENGTH = 128; //in bits
    private static final String alias = "encryptorKey";
    private static PublicKey publicKey;
    private static PrivateKey privateKey;

    //TODO: add buffered encryption/decryption instead of cipher.doFinal(input);(look into MainActivity encrypting/decrypting methods
    private static byte[] encryptBytesAES256(byte[] input, String password) {
        try {
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            // Derive the key, given password and salt
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            // A salt is a unique, randomly generated string
            // that is added to each password as part of the hashing process
            byte[] salt = new byte[PBKDF2_SALT_LENGTH];
            secureRandom.nextBytes(salt);
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATION_COUNT, AES_KEY_LENGTH);
            byte[] secret = factory.generateSecret(keySpec).getEncoded();
            SecretKey key = new SecretKeySpec(secret, "AES");
            // AES-GCM encryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            // A nonce or an initialization vector is a random value chosen at encryption time
            // and meant to be used only once
            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            // An authentication tag
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
            byte[] encrypted = cipher.doFinal(input);
            /*ByteArrayInputStream inputStream = new ByteArrayInputStream(encrypted);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cos = new CipherOutputStream(outputStream, cipher);
            int b;
            byte[] d = new byte[10 * 1024 * 1024];
            while ((b = inputStream.read(d)) != -1) {
                cos.write(d, 0, b);
            }*/
            // Salt and nonce can be stored together with the encrypted data
            // Both salt and nonce have fixed length, so can be prefixed to the encrypted data
            ByteBuffer byteBuffer = ByteBuffer.allocate(salt.length + nonce.length + encrypted.length);
            byteBuffer.put(salt);
            byteBuffer.put(nonce);
            byteBuffer.put(encrypted);
            return byteBuffer.array();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] decryptBytesAES256(byte[] encrypted, String password) {
        try {
            // Salt and nonce have to be extracted
            ByteBuffer byteBuffer = ByteBuffer.wrap(encrypted);
            byte[] salt = new byte[PBKDF2_SALT_LENGTH];
            byteBuffer.get(salt);
            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            byteBuffer.get(nonce);
            byte[] cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATION_COUNT, AES_KEY_LENGTH);
            byte[] secret = factory.generateSecret(keySpec).getEncoded();
            SecretKey key = new SecretKeySpec(secret, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            // If encrypted data is altered, during decryption authentication tag verification will fail
            // resulting in AEADBadTagException
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void encryptFileAES256(File original, String password, File fileToSave) throws Exception {
        if (original.length() > 15 * 1024 * 1024) {
            List<File> split = splitFile(original);
            List<File> encryptedSplit = new LinkedList<File>();
            for (int i = 0; i < split.size(); i++) {
                encryptFileAES256(split.get(i), password, new File(split.get(i).getPath() + ".enc"));
                encryptedSplit.add(new File(split.get(i).getPath() + ".enc"));
            }
            zip(fileToSave, encryptedSplit);
            for (int i = 0; i < split.size(); i++) {
                split.get(i).delete();
                encryptedSplit.get(i).delete();
            }
        } else {
            byte[] originalBytes = Files.readAllBytes(original.toPath());
            byte[] encryptedBytes = encryptBytesAES256(originalBytes, password);
            FileOutputStream fileOutputStream = new FileOutputStream(fileToSave);
            fileOutputStream.write(encryptedBytes);
            fileOutputStream.flush();
            fileOutputStream.close();
        }
    }

    public static void decryptFileAES256(File original, String password, File fileToSave) throws Exception {
        if (original.length() > 15 * 1024 * 1024) {
            List<File> split = new LinkedList<File>();
            unZip(original, split);
            List<File> decryptedSplit = new LinkedList<File>();
            for (int i = 0; i < split.size(); i++) {
                decryptFileAES256(split.get(i), password, new File(split.get(i).getPath() + "dec"));
                decryptedSplit.add(new File(split.get(i).getPath() + "dec"));
            }
            mergeFiles(decryptedSplit, fileToSave);
            for (int i = 0; i < split.size(); i++) {
                split.get(i).delete();
                decryptedSplit.get(i).delete();
            }
        } else {
            byte[] originalBytes = Files.readAllBytes(original.toPath());
            byte[] decryptedBytes = decryptBytesAES256(originalBytes, password);
            FileOutputStream fileOutputStream = new FileOutputStream(fileToSave);
            fileOutputStream.write(decryptedBytes);
            fileOutputStream.flush();
            fileOutputStream.close();
        }
    }

    public static void encryptFolderAES_GCM(File folder, String password, File folderToSave) throws Exception {
        if (folderToSave.exists()) {
            if (folderToSave.isDirectory()) {
                if (folder.exists() && folder.isDirectory()) {
                    File[] filesInFolder = folder.listFiles();
                    for (int i = 0; i < filesInFolder.length; i++) {
                        if (filesInFolder[i].isFile()) {
                            encryptFileAES256(filesInFolder[i], password, new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName() + ".enc"));
                        } else if (filesInFolder[i].isDirectory()) {
                            encryptFolderAES_GCM(filesInFolder[i], password, new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName() + "Enc"));
                        }
                    }
                }
            }
        } else {
            File file = new File(folderToSave.getPath());
            file.mkdir();
            if (folder.exists() && folder.isDirectory()) {
                File[] filesInFolder = folder.listFiles();
                for (int i = 0; i < filesInFolder.length; i++) {
                    if (filesInFolder[i].isFile()) {
                        encryptFileAES256(filesInFolder[i], password, new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName() + ".enc"));
                    } else if (filesInFolder[i].isDirectory()) {
                        encryptFolderAES_GCM(filesInFolder[i], password, new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName() + "Enc"));
                    }
                }
            }
        }
    }

    public static void decryptFolderAES_GCM(File folder, String password, File folderToSave) throws Exception {
        if (folderToSave.exists()) {
            if (folderToSave.isDirectory()) {
                if (folder.exists() && folder.isDirectory()) {
                    File[] filesInFolder = folder.listFiles();
                    for (int i = 0; i < filesInFolder.length; i++) {
                        if (filesInFolder[i].isFile()) {
                            decryptFileAES256(filesInFolder[i], password, new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName().substring(0, filesInFolder[i].getName().length() - 4)));
                        } else if (filesInFolder[i].isDirectory()) {
                            decryptFolderAES_GCM(filesInFolder[i], password, new File((folderToSave.getPath() + File.separator + filesInFolder[i].getName()).substring(0, (folderToSave.getPath() + File.separator + filesInFolder[i].getName()).length() - 3)));
                        }
                    }
                }
            }
        } else {
            File file = new File(folderToSave.getPath());
            file.mkdir();
            if (folder.exists() && folder.isDirectory()) {
                File[] filesInFolder = folder.listFiles();
                for (int i = 0; i < filesInFolder.length; i++) {
                    if (filesInFolder[i].isFile()) {
                        decryptFileAES256(filesInFolder[i], password, new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName().substring(0, filesInFolder[i].getName().length() - 4)));
                    } else if (filesInFolder[i].isDirectory()) {
                        decryptFolderAES_GCM(filesInFolder[i], password, new File((folderToSave.getPath() + File.separator + filesInFolder[i].getName()).substring(0, (folderToSave.getPath() + File.separator + filesInFolder[i].getName()).length() - 3)));
                    }
                }
            }
        }
    }

    public static void encryptFileThreadAES_GCM(File file, String password, File fileToSave) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (file.exists() && file.isFile()) {
                    try {
                        encryptFileAES256(file, password, fileToSave);
                    } catch (Exception e) {

                    }
                }
            }
        });
        thread.start();
    }

    public static void decryptFileThreadAES_GCM(File file, String password, File fileToSave) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (file.exists() && file.isFile()) {
                    try {
                        decryptFileAES256(file, password, fileToSave);
                    } catch (Exception e) {

                    }
                }
            }
        });
        thread.start();
    }

    private static List<File> splitFile(File f) throws IOException {
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

    private static void mergeFiles(List<File> files, File into) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(into); BufferedOutputStream mergingStream = new BufferedOutputStream(fos)) {
            for (int i = 0; i < files.size(); i++) {
                Files.copy(files.get(i).toPath(), mergingStream);
            }
        }
    }

    private static void zip(File output, List<File> sources) throws Exception {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(output))) {
            zipOut.setLevel(Deflater.NO_COMPRESSION);
            for (int i = 0; i < sources.size(); i++) {
                zipFile(zipOut, sources.get(i));
            }
            zipOut.flush();
        }
    }

    private static void unZip(File input, List<File> output) throws Exception {
        String fileZip = input.getPath();
        File destDir = new File(input.getParent());
        byte[] buffer = new byte[512];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip))) {
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

    private static void zipFile(ZipOutputStream zos, File file) throws IOException {
        zos.putNextEntry(new ZipEntry(file.getName()));
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[512];
            int byteCount = 0;
            while ((byteCount = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, byteCount);
            }
        }
        zos.closeEntry();
    }
    //endregion
    //region Password manager
    /*public static void addDataToKeyStorage(String dataToSave, Context context, String fileName) throws Exception {
        File path = context.getFilesDir();
        String encryptedDataFilePath = path.getPath() + File.separator + fileName;
        Cipher inCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
        inCipher.init(Cipher.ENCRYPT_MODE, getPublicKey());
        InputStream fis = new ByteArrayInputStream(dataToSave.getBytes("UTF-8"));
        FileOutputStream fos = new FileOutputStream(new File(encryptedDataFilePath));
        CipherOutputStream cos = new CipherOutputStream(fos, inCipher);
        byte[] block = new byte[32];
        int i;
        while ((i = fis.read(block)) != -1) {
            cos.write(block, 0, i);
        }
        cos.close();
    }*/

    /*public static void loadStorageData(HashMap<String,String> data, Context context) throws Exception {
        Cipher outCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
        outCipher.init(Cipher.DECRYPT_MODE, getPrivateKey());
        File[] files = new File(context.getFilesDir().getPath()).listFiles();
        for(int i = 0; i < files.length; i++){
            FileInputStream fis = new FileInputStream(files[i]);
            CipherInputStream cis = new CipherInputStream(fis, outCipher);
            //Unfinished
        }
        // Decrypt
        String cleartextAgainFile = "cleartextAgainRSA.txt";

        cipher.init(Cipher.DECRYPT_MODE, privKey);

        fis = new FileInputStream(ciphertextFile);
        CipherInputStream cis = new CipherInputStream(fis, cipher);
        fos = new FileOutputStream(cleartextAgainFile);

        while ((i = cis.read(block)) != -1) {
            fos.write(block, 0, i);
        }
        fos.close();
    }*/

    public static SQLiteDatabase initDataBase(Context context, String password) {
        SQLiteDatabase.loadLibs(context);
        boolean fileExisted = false;
        File databaseFile = new File(context.getApplicationInfo().dataDir + File.separator + "database.db");
        if (databaseFile.exists()) fileExisted = true;
        //databaseFile.mkdirs();
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile, password, null);
        if (!fileExisted) createPasswordTable(database);
        return database;
    }

    private static void createPasswordTable(SQLiteDatabase database) {
        database.execSQL("create table passwordTable\n" +
                "(\n" +
                "\tid INTEGER not null\n" +
                "\t\tconstraint passwordTable_pk\n" +
                "\t\t\tprimary key autoincrement,\n" +
                "\tname TEXT not null,\n" +
                "\tlogin TEXT,\n" +
                "\tpass TEXT\n" +
                ");");
    }

    public static void insertDataIntoPasswordTable(SQLiteDatabase database, String name, String login, String password) {
        database.execSQL("INSERT INTO passwordTable (id, name, login, pass) VALUES ($next_id, '" + name + "', '" + login + "', '" + password + "');");
    }

    public static HashMap<Integer, ArrayList<String>> readPasswordData(SQLiteDatabase database) {
        HashMap<Integer, ArrayList<String>> table = new HashMap<>();
        Cursor cursor = database.rawQuery("SELECT * FROM passwordTable", null);
        if (cursor.moveToFirst()) {
            do {
                ArrayList<String> strings = new ArrayList<>();
                strings.add(cursor.getString(1));
                strings.add(cursor.getString(2));
                strings.add(cursor.getString(3));
                Integer id = cursor.getInt(0);
                table.put(id, strings);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return table;
    }

    public static void updateDataIntoPasswordTable(SQLiteDatabase database, int id, String name, String login, String password) {
        database.execSQL("UPDATE passwordTable SET name = '" + name + "', login = '" + login + "', pass = '" + password + "' WHERE id = " + id + ";");
    }

    public static void deleteDataFromPasswordTable(SQLiteDatabase database, int id) {
        database.execSQL("DELETE FROM passwordTable WHERE id = " + id + ";");
    }

    public static void closeDataBase(SQLiteDatabase database) {
        database.close();
    }

    /*private static PublicKey getPublicKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(alias)) {
            KeyStore.Entry entry = keyStore.getEntry(alias, null);
            PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();
            return publicKey;
        } else return null;
    }

    private static PrivateKey getPrivateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        KeyStore.Entry entry = keyStore.getEntry(alias, null);
        PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
        return privateKey;
    }

    private static void generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");

        kpg.initialize(new KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setKeySize(2048)
                .build());

        KeyPair keyPair = kpg.generateKeyPair();
    }

    private static void deleteKeyPair() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            keyStore.deleteEntry(alias);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    public static byte[] RSAEncrypt(final String plain) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.genKeyPair();
        publicKey = kp.getPublic();
        privateKey = kp.getPrivate();
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plain.getBytes());
        //System.out.println("EEncrypted?????" + new String(org.apache.commons.codec.binary.Hex.encodeHex(encryptedBytes)));
        return encryptedBytes;
    }

    public static String RSADecrypt(final byte[] encryptedBytes) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        String decrypted = new String(decryptedBytes);
        //System.out.println("DDecrypted?????" + decrypted);
        return decrypted;
    }
    //endregion
}
