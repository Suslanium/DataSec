package com.suslanium.encryptor.ui;

import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.view.autofill.AutofillManager;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.suslanium.encryptor.EncryptorAutofillService;
import com.suslanium.encryptor.util.Encryptor;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PasswordEntryViewModel extends AndroidViewModel {
    private byte[] image = null;
    public PasswordEntryViewModel(@NonNull Application application) {
        super(application);
    }

    protected List<String> readEntryData(byte[] pass,int id) throws Exception {
        String password = Encryptor.rsadecrypt(pass);
        SQLiteDatabase database = Encryptor.initDataBase(getApplication().getBaseContext(), password);
        HashMap<Integer, ArrayList<String>> listHashMap = Encryptor.readPasswordData(database);
        HashMap<Integer, byte[]> iconsHashMap = Encryptor.readPasswordIcons(database);
        image = iconsHashMap.get(id);
        Encryptor.closeDataBase(database);
        return listHashMap.get(id);
    }

    protected Bitmap getImage(){
        if (image != null) {
            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = Bitmap.createBitmap(256, 256, config);
            ByteBuffer byteBuffer = ByteBuffer.wrap(image);
            bitmap.copyPixelsFromBuffer(byteBuffer);
            return bitmap;
        }
        return null;
    }

    protected void deleteEntry(byte [] pass, int id) throws Exception {
        String password = Encryptor.rsadecrypt(pass);
        SQLiteDatabase database = Encryptor.initDataBase(getApplication().getBaseContext(), password);
        Encryptor.deleteDataFromPasswordTable(database, id);
        Encryptor.closeDataBase(database);
    }

    protected void saveEntry(byte[] passEnc,int id, boolean newEntry, String name, String login, String pass, String website, String notes, String category) throws Exception{
        String password = Encryptor.rsadecrypt(passEnc);
        SQLiteDatabase database = Encryptor.initDataBase(getApplication().getBaseContext(), password);
        if(image == null){
            try {
                URL imageURL = new URL("https://logo.clearbit.com/" + URLUtil.guessUrl(website));
                HttpURLConnection connection = (HttpURLConnection) imageURL.openConnection();
                connection.setDoInput(true);
                connection.connect();
                Bitmap imageBitmap = BitmapFactory.decodeStream(connection.getInputStream());
                Bitmap thumbnail = Bitmap.createScaledBitmap(imageBitmap, 256,256, false);
                ByteBuffer byteBuffer = ByteBuffer.allocate(thumbnail.getByteCount());
                thumbnail.copyPixelsToBuffer(byteBuffer);
                image = byteBuffer.array();
            } catch (Exception e){
                image = null;
            }
        }
        if(!newEntry) {
            Encryptor.updateDataIntoPasswordTable(database, id, name, login, pass, image, website, notes, category);
        } else {
            Encryptor.insertDataIntoPasswordTable(database, name, login, pass, image, website, notes, category);
        }
        Encryptor.closeDataBase(database);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getApplication().getBaseContext().getSystemService(AutofillManager.class).isAutofillSupported() && getApplication().getBaseContext().getSystemService(AutofillManager.class).hasEnabledAutofillServices()){
            EncryptorAutofillService.setPass(passEnc);
        }
    }

    protected Bitmap getThumbnail(Intent data){
        try (InputStream inputStream = getApplication().getBaseContext().getContentResolver().openInputStream(data.getData())){
            Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(inputStream), 256,256);
            ByteBuffer byteBuffer = ByteBuffer.allocate(thumbnail.getByteCount());
            thumbnail.copyPixelsToBuffer(byteBuffer);
            image = byteBuffer.array();
            return thumbnail;
        } catch (IOException e) {

        }
        return null;
    }

    protected int calculatePasswordStrength(String password) {

        int passwordScore = 0;

        if (password.length() >= 8 && password.length() <= 10)
            passwordScore += 1;
        else if (password.length() >= 10)
            passwordScore += 2;
        else if(password.length() == 0)
            return 0;
        else
            return 1;

        if (password.matches("(?=.*[0-9].*[0-9]).*"))
            passwordScore += 2;
        else if (password.matches("(?=.*[0-9]).*"))
            passwordScore += 1;

        if (password.matches("(?=.*[a-z].*[a-z]).*"))
            passwordScore += 2;
        else if(password.matches("(?=.*[a-z]).*"))
            passwordScore += 1;

        if (password.matches("(?=.*[A-Z].*[A-Z]).*"))
            passwordScore += 2;
        else if (password.matches("(?=.*[A-Z]).*"))
            passwordScore += 1;

        if (password.matches("(?=.*[~!@#$%^&*()_-].*[~!@#$%^&*()_-]).*"))
            passwordScore += 2;
        else if (password.matches("(?=.*[~!@#$%^&*()_-]).*"))
            passwordScore += 1;

        return passwordScore;

    }
}
