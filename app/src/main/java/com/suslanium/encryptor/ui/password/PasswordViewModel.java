package com.suslanium.encryptor.ui.password;

import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.suslanium.encryptor.util.Encryptor;

import net.sqlcipher.database.SQLiteDatabase;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class PasswordViewModel extends AndroidViewModel {
    private MutableLiveData<String> currentSearchQuery;
    private MutableLiveData<String> currentCategory;
    private final MutableLiveData<ArrayList<String>> names;
    private final MutableLiveData<ArrayList<String>> logins;
    private final MutableLiveData<ArrayList<Integer>> ids;
    private final MutableLiveData<ArrayList<Bitmap>> bitmaps;
    private final MutableLiveData<ArrayList<String>> categories;
    private Intent intent;

    public PasswordViewModel(@NonNull Application application) {
        super(application);
        this.ids = new MutableLiveData<>(new ArrayList<>());
        this.logins = new MutableLiveData<>(new ArrayList<>());
        this.names = new MutableLiveData<>(new ArrayList<>());
        this.bitmaps = new MutableLiveData<>(new ArrayList<>());
        this.categories = new MutableLiveData<>(new ArrayList<>());
        currentSearchQuery = new MutableLiveData<>("");
        currentCategory = new MutableLiveData<>("");
    }

    protected void setCurrentSearchQuery(String query) {
        currentSearchQuery.setValue(query);
    }

    protected void setCurrentCategory(String category) {
        currentCategory.setValue(category);
    }

    protected LiveData<String> getCurrentSearchQuery() {
        if (currentSearchQuery == null) currentSearchQuery = new MutableLiveData<>();
        return currentSearchQuery;
    }

    protected LiveData<String> getCurrentCategory() {
        if (currentCategory == null) currentCategory = new MutableLiveData<>();
        return currentCategory;
    }

    protected boolean updateList() throws Exception {
        ArrayList<String> strings3 = new ArrayList<>();
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<String> logins = new ArrayList<>();
        byte[] pass = intent.getByteArrayExtra("pass");
        String password = Encryptor.rsadecrypt(pass);
        SQLiteDatabase database = Encryptor.initDataBase(getApplication().getBaseContext(), password);
        HashMap<Integer, ArrayList<String>> listHashMap = Encryptor.readPasswordData(database);
        Set<Integer> integers = listHashMap.keySet();
        HashMap<Integer, String> names = new HashMap<>();
        for (Integer i : integers) {
            ArrayList<String> strings = listHashMap.get(i);
            String s = strings.get(0);
            String l = strings.get(1);
            String c = strings.get(5);
            if (currentSearchQuery.getValue() != null && !currentSearchQuery.getValue().matches("")) {
                if (s.contains(currentSearchQuery.getValue())) {
                    names.put(i, s);
                    strings3.add(s);
                    logins.add(l);
                    ids.add(i);
                }
            } else if (currentCategory.getValue() != null && !currentCategory.getValue().matches("")) {
                if (c != null && c.equals(currentCategory.getValue())) {
                    names.put(i, s);
                    strings3.add(s);
                    logins.add(l);
                    ids.add(i);
                }
            } else {
                if (c == null || c.matches("")) {
                    names.put(i, s);
                    strings3.add(s);
                    logins.add(l);
                    ids.add(i);
                }
            }
        }
        HashMap<Integer, byte[]> iconsList = Encryptor.readPasswordIcons(database);
        Set<Integer> integerSet = iconsList.keySet();
        ArrayList<Bitmap> bitmapArrayList = new ArrayList<>();
        for (Integer i : integerSet) {
            if (ids.contains(i)) {
                byte[] image = iconsList.get(i);
                if (image != null) {
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
        boolean b;
        categories.getValue().clear();
        if ((currentSearchQuery.getValue() == null || currentSearchQuery.getValue().matches("")) && (currentCategory.getValue() == null || currentCategory.getValue().matches(""))) {
            categories.getValue().clear();
            categories.getValue().addAll(Encryptor.getCategories(database));
            Encryptor.closeDataBase(database);
            b=true;
        } else {
            Encryptor.closeDataBase(database);
            b=false;
        }
        this.names.postValue(strings3);
        this.logins.postValue(logins);
        this.bitmaps.postValue(bitmapArrayList);
        this.ids.postValue(ids);
        return b;
    }

    protected LiveData<ArrayList<String>> getNames() {
        return names;
    }

    protected LiveData<ArrayList<String>> getLogins() {
        return logins;
    }

    protected LiveData<ArrayList<Integer>> getIds() {
        return ids;
    }

    protected LiveData<ArrayList<Bitmap>> getIcons() {
        return bitmaps;
    }

    protected LiveData<ArrayList<String>> getCategories() {
        return categories;
    }

    protected void setIntent(Intent intent) {
        this.intent = intent;
    }

    protected boolean createCategory(String name) throws Exception {
        byte[] pass = intent.getByteArrayExtra("pass");
        String password = Encryptor.rsadecrypt(pass);
        SQLiteDatabase database = Encryptor.initDataBase(getApplication().getBaseContext(), password);
        ArrayList<String> categories = Encryptor.getCategories(database);
        if (!categories.contains(name)) {
            Encryptor.createCategoryStub(database, name);
            Encryptor.closeDataBase(database);
            return true;
        } else {
            Encryptor.closeDataBase(database);
            return false;
        }
    }

    protected void renameCategory(String oldName,String newName){
        Thread thread = new Thread(() -> {
            try {
                if(!oldName.equals(newName)) {
                    byte[] pass = intent.getByteArrayExtra("pass");
                    String password = Encryptor.rsadecrypt(pass);
                    SQLiteDatabase database = Encryptor.initDataBase(getApplication().getBaseContext(), password);
                    Encryptor.deleteCategory(database, oldName);
                    if(!Encryptor.getCategories(database).contains(newName)) Encryptor.createCategoryStub(database, newName);
                    Encryptor.renameCategory(database, oldName, newName);
                    Encryptor.closeDataBase(database);
                }
            } catch (Exception ignored){
            } finally {
                try {
                    updateList();
                } catch (Exception ignored) {
                }
            }
        });
        thread.start();
    }

    protected void deleteCategory(String name){
        Thread thread = new Thread(() -> {
            try {
                byte[] pass = intent.getByteArrayExtra("pass");
                String password = Encryptor.rsadecrypt(pass);
                SQLiteDatabase database = Encryptor.initDataBase(getApplication().getBaseContext(), password);
                Encryptor.deleteCategory(database, name);
                Encryptor.closeDataBase(database);
            } catch (Exception ignored){
            } finally {
                try {
                    updateList();
                } catch (Exception ignored) {
                }
            }
        });
        thread.start();
    }
}
