package com.suslanium.encryptor.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class Encryptor {
    //region File or folder encryption
    private static final int PBKDF2_ITERATION_COUNT = 300000;
    private static final int PBKDF2_SALT_LENGTH = 16;
    private static final int AES_KEY_LENGTH = 256;
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static PublicKey publicKey = null;
    private static PrivateKey privateKey = null;

    public static byte[] encryptBytesAES256(byte[] input, String password) {
        try {
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG", "AndroidOpenSSL");
            SecretKeyFactory factory;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            } else {
                factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            }
            byte[] salt = new byte[PBKDF2_SALT_LENGTH];
            secureRandom.nextBytes(salt);
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATION_COUNT, AES_KEY_LENGTH);
            byte[] secret = factory.generateSecret(keySpec).getEncoded();
            SecretKey key = new SecretKeySpec(secret, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
            byte[] encrypted = cipher.doFinal(input);
            ByteBuffer byteBuffer = ByteBuffer.allocate(salt.length + nonce.length + encrypted.length);
            byteBuffer.put(salt);
            byteBuffer.put(nonce);
            byteBuffer.put(encrypted);
            return byteBuffer.array();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decryptBytesAES256(byte[] encrypted, String password) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(encrypted);
            byte[] salt = new byte[PBKDF2_SALT_LENGTH];
            byteBuffer.get(salt);
            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            byteBuffer.get(nonce);
            byte[] cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);
            SecretKeyFactory factory;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            } else {
                factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            }
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATION_COUNT, AES_KEY_LENGTH);
            byte[] secret = factory.generateSecret(keySpec).getEncoded();
            SecretKey key = new SecretKeySpec(secret, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            try {
                ByteBuffer byteBuffer = ByteBuffer.wrap(encrypted);
                byte[] salt = new byte[PBKDF2_SALT_LENGTH];
                byteBuffer.get(salt);
                byte[] nonce = new byte[GCM_NONCE_LENGTH];
                byteBuffer.get(nonce);
                byte[] cipherBytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(cipherBytes);
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATION_COUNT, AES_KEY_LENGTH);
                byte[] secret = factory.generateSecret(keySpec).getEncoded();
                SecretKey key = new SecretKeySpec(secret, "AES");
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
                cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
                return cipher.doFinal(cipherBytes);
            } catch (Exception e2) {
                e2.printStackTrace();
                throw new RuntimeException(e2);
            }
        }
    }

    public static void encryptFileAES256(File original, String password, File fileToSave) throws Exception {
        if (original.length() > 15 * 1024 * 1024) {
            List<File> split = new LinkedList<>();
            List<File> encryptedSplit = new LinkedList<>();
            try {
                split.addAll(splitFile(original));
                for (int i = 0; i < split.size(); i++) {
                    encryptFileAES256(split.get(i), password, new File(split.get(i).getPath() + ".enc"));
                    encryptedSplit.add(new File(split.get(i).getPath() + ".enc"));
                }
                zip(fileToSave, encryptedSplit);
            } catch (Exception e) {
                throw new Exception(e);
            } finally {
                for (int i = 0; i < split.size(); i++) {
                    wipeFile(split.get(i));
                    split.get(i).delete();
                }
                for (int i = 0; i < encryptedSplit.size(); i++) {
                    wipeFile(encryptedSplit.get(i));
                    encryptedSplit.get(i).delete();
                }
            }
        } else {
            int size = (int) original.length();
            byte[] originalBytes = new byte[size];
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(original));
            buf.read(originalBytes, 0, originalBytes.length);
            buf.close();
            byte[] encryptedBytes = encryptBytesAES256(originalBytes, password);
            FileOutputStream fileOutputStream = new FileOutputStream(fileToSave);
            fileOutputStream.write(encryptedBytes);
            fileOutputStream.flush();
            fileOutputStream.close();
        }
    }

    public static void decryptFileAES256(File original, String password, File fileToSave) throws Exception {
        if (original.length() > 15 * 1024 * 1024) {
            List<File> split = new LinkedList<>();
            List<File> decryptedSplit = new LinkedList<>();
            try {
                unZip(original, split);
                for (int i = 0; i < split.size(); i++) {
                    decryptFileAES256(split.get(i), password, new File(split.get(i).getPath() + "dec"));
                    decryptedSplit.add(new File(split.get(i).getPath() + "dec"));
                }
                mergeFiles(decryptedSplit, fileToSave);
            } catch (Exception e) {
                throw new Exception(e);
            } finally {
                for (int i = 0; i < split.size(); i++) {
                    wipeFile(split.get(i));
                    split.get(i).delete();
                }
                for (int i = 0; i < decryptedSplit.size(); i++) {
                    wipeFile(decryptedSplit.get(i));
                    decryptedSplit.get(i).delete();
                }
            }
        } else {
            int size = (int) original.length();
            byte[] originalBytes = new byte[size];
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(original));
            buf.read(originalBytes, 0, originalBytes.length);
            buf.close();
            byte[] decryptedBytes = decryptBytesAES256(originalBytes, password);
            FileOutputStream fileOutputStream = new FileOutputStream(fileToSave);
            fileOutputStream.write(decryptedBytes);
            fileOutputStream.flush();
            fileOutputStream.close();
        }
    }

    public static void encryptFolderAESGCM(File folder, String password, File folderToSave, Context context) throws Exception {
        boolean autoDelete = false;
        if (context != null) {
            SharedPreferences editor = PreferenceManager.getDefaultSharedPreferences(context);
            autoDelete = editor.getBoolean("auto_Delete", true);
        }
        folderToSave.mkdirs();
        if (folderToSave.isDirectory() && folder.exists() && folder.isDirectory()) {
            File[] filesInFolder = folder.listFiles();
            if (filesInFolder != null) {
                for (int i = 0; i < filesInFolder.length; i++) {
                    if (filesInFolder[i].isFile()) {
                        encryptFileAES256(filesInFolder[i], password, new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName() + ".enc"));
                        if (autoDelete) {
                            wipeFile(filesInFolder[i]);
                            filesInFolder[i].delete();
                        }
                    } else if (filesInFolder[i].isDirectory()) {
                        encryptFolderAESGCM(filesInFolder[i], password, new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName() + "Enc"), context);
                        if (autoDelete) {
                            wipeFile(filesInFolder[i]);
                            filesInFolder[i].delete();
                        }
                    }
                }
            }
        }
    }

    public static void decryptFolderAESGCM(File folder, String password, File folderToSave, Context context, boolean gDrive) throws Exception {
        boolean autoDelete2 = false;
        if (context != null) {
            SharedPreferences editor = PreferenceManager.getDefaultSharedPreferences(context);
            autoDelete2 = editor.getBoolean("auto_Delete2", true);
        }
        folderToSave.mkdirs();
        if (folderToSave.isDirectory() && folder.exists() && folder.isDirectory()) {
            File[] filesInFolder = folder.listFiles();
            if (filesInFolder != null) {
                for (int i = 0; i < filesInFolder.length; i++) {
                    if (filesInFolder[i].isFile()) {
                        decryptFileAES256(filesInFolder[i], password, new File(folderToSave.getPath() + File.separator + filesInFolder[i].getName().substring(0, filesInFolder[i].getName().length() - 4)));
                        if (autoDelete2 || gDrive) filesInFolder[i].delete();
                    } else if (filesInFolder[i].isDirectory()) {
                        decryptFolderAESGCM(filesInFolder[i], password, new File((folderToSave.getPath() + File.separator + filesInFolder[i].getName()).substring(0, (folderToSave.getPath() + File.separator + filesInFolder[i].getName()).length() - 3)), context, gDrive);
                        if (autoDelete2 || gDrive) filesInFolder[i].delete();
                    }
                }
            }
        }
    }

    private static List<File> splitFile(File f) throws IOException {
        int partCounter = 1;
        int sizeOfFiles = 10 * 1024 * 1024;
        byte[] buffer = new byte[sizeOfFiles];
        String fileName = f.getName();
        List<File> splittedFiles = new LinkedList<>();
        try (FileInputStream fis = new FileInputStream(f); BufferedInputStream bis = new BufferedInputStream(fis)) {
            int bytesAmount;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                String filePartName = fileName + partCounter;
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
                try (InputStream in = new FileInputStream(files.get(i))) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        mergingStream.write(buf, 0, len);
                    }
                } catch (IOException e) {
                    throw new IOException(e);
                }
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
                try (FileOutputStream fos = new FileOutputStream(destFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    output.add(destFile);
                } finally {
                    zipEntry = zis.getNextEntry();
                }
            }
            zis.closeEntry();
        }
    }

    private static void zipFile(ZipOutputStream zos, File file) throws IOException {
        zos.putNextEntry(new ZipEntry(file.getName()));
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[512];
            int byteCount;
            while ((byteCount = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, byteCount);
            }
        }
        zos.closeEntry();
    }

    public static String calculateHash(final String s, String function) {
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(function);
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                StringBuilder h = new StringBuilder(Integer.toHexString(0xFF & aMessageDigest));
                while (h.length() < 2) h.insert(0, "0");
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ignored) {

        }
        return "";
    }
    //endregion
    //region Password manager

    public static SQLiteDatabase initDataBase(Context context, String password) {
        SQLiteDatabase.loadLibs(context);
        boolean fileExisted = false;
        File databaseFile = new File(context.getApplicationInfo().dataDir + File.separator + "database.db");
        if (databaseFile.exists()) fileExisted = true;
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
                "\tpass TEXT,\n" +
                "\timage BLOB,\n" +
                "\twebsite TEXT,\n" +
                "\tnotes TEXT,\n" +
                "\tcategory TEXT,\n" +
                "\tisstub INTEGER DEFAULT 0\n" +
                ");");
    }

    public static void insertDataIntoPasswordTable(SQLiteDatabase database, String name, String login, String password, byte[] image, String website, String notes, String category) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("login", login);
        cv.put("pass", password);
        cv.put("image", image);
        cv.put("website", website);
        cv.put("notes", notes);
        cv.put("category", category);
        database.insert("passwordTable", null, cv);
    }

    public static HashMap<Integer, ArrayList<String>> readPasswordData(SQLiteDatabase database) {
        HashMap<Integer, ArrayList<String>> table = new HashMap<>();
        Cursor cursor = database.rawQuery("SELECT * FROM passwordTable", null);
        if (cursor.moveToFirst()) {
            do {
                if (cursor.getInt(8) == 0) {
                    ArrayList<String> strings = new ArrayList<>();
                    strings.add(cursor.getString(1));
                    strings.add(cursor.getString(2));
                    strings.add(cursor.getString(3));
                    strings.add(cursor.getString(5));
                    strings.add(cursor.getString(6));
                    strings.add(cursor.getString(7));
                    Integer id = cursor.getInt(0);
                    table.put(id, strings);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return table;
    }

    public static void createCategoryStub(SQLiteDatabase database, String categoryName) {
        ContentValues cv = new ContentValues();
        cv.put("name", categoryName + "stub");
        cv.put("login", (String) null);
        cv.put("pass", (String) null);
        cv.put("image", (byte[]) null);
        cv.put("website", (String) null);
        cv.put("notes", (String) null);
        cv.put("category", categoryName);
        cv.put("isstub", 1);
        database.insert("passwordTable", null, cv);
    }

    public static ArrayList<String> getCategories(SQLiteDatabase database) {
        ArrayList<String> result = new ArrayList<>();
        Cursor cursor = database.rawQuery("SELECT * FROM passwordTable", null);
        if (cursor.moveToFirst()) {
            do {
                if (cursor.getInt(8) == 1) {
                    result.add(cursor.getString(7));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    public static void deleteCategory(SQLiteDatabase database, String name) {
        Cursor cursor = database.rawQuery("SELECT * FROM passwordTable", null);
        if (cursor.moveToFirst()) {
            do {
                if (cursor.getInt(8) == 1) {
                    if (cursor.getString(7).equals(name)) {
                        deleteDataFromPasswordTable(database, cursor.getInt(0));
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    public static void renameCategory(SQLiteDatabase database, String oldName, String newName) {
        Cursor cursor = database.rawQuery("SELECT * FROM passwordTable", null);
        if (cursor.moveToFirst()) {
            do {
                if (cursor.getString(7) != null && cursor.getString(7).equals(oldName)) {
                    if (cursor.getInt(8) == 0) {
                        updateDataIntoPasswordTable(database, cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getBlob(4), cursor.getString(5), cursor.getString(6), newName);
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    public static HashMap<Integer, byte[]> readPasswordIcons(SQLiteDatabase database) {
        HashMap<Integer, byte[]> table = new HashMap<>();
        Cursor cursor = database.rawQuery("SELECT * FROM passwordTable", null);
        if (cursor.moveToFirst()) {
            do {
                if (cursor.getInt(8) == 0) {
                    Integer id = cursor.getInt(0);
                    table.put(id, cursor.getBlob(4));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return table;
    }

    public static void updateDataIntoPasswordTable(SQLiteDatabase database, int id, String name, String login, String password, byte[] image, String website, String notes, String category) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("login", login);
        cv.put("pass", password);
        cv.put("image", image);
        cv.put("website", website);
        cv.put("notes", notes);
        cv.put("category", category);
        database.update("passwordTable", cv, "id = ?", new String[]{String.valueOf(id)});
    }

    public static void deleteDataFromPasswordTable(SQLiteDatabase database, int id) {
        database.execSQL("DELETE FROM passwordTable WHERE id = " + id + ";");
    }

    public static void deleteDatabase(Context context) {
        File databaseFile = new File(context.getApplicationInfo().dataDir + File.separator + "database.db");
        wipeFile(databaseFile);
        databaseFile.delete();
    }

    public static void closeDataBase(SQLiteDatabase database) {
        database.close();
    }

    public static boolean checkDatabase(File databaseFile, String password, Context context) {
        try {
            SQLiteDatabase.loadLibs(context);
            SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile, password, null);
            readPasswordData(database);
            readPasswordIcons(database);
            database.close();
            return true;
        } catch (Exception | Error e) {

            return false;
        }
    }

    public static SQLiteDatabase openDownloadedTable(File databaseFile, String password, Context context) {
        SQLiteDatabase.loadLibs(context);
        return SQLiteDatabase.openOrCreateDatabase(databaseFile, password, null);
    }

    public static byte[] rsaencrypt(final String plain) throws Exception {
        if (publicKey == null || privateKey == null) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.genKeyPair();
            publicKey = kp.getPublic();
            privateKey = kp.getPrivate();
        }
        Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA-1AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(plain.getBytes());
    }

    public static String rsadecrypt(final byte[] encryptedBytes) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA-1AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes);
    }

    public static void wipeFile(File file) {
        if (file.exists() && file.isFile()) {
            long length = file.length();
            if (length >= 4096) {
                length = 4096;
            }
            try (FileOutputStream stream = new FileOutputStream(file, false)) {
                byte[] emptyData = new byte[(int) length];
                stream.write(emptyData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //endregion
}
