package com.suslanium.encryptor.ui.password;

import android.app.Application;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.suslanium.encryptor.Encryptor;

import net.sqlcipher.database.SQLiteDatabase;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class PasswordViewModel extends AndroidViewModel {
    private MutableLiveData<String> currentSearchQuery;
    private MutableLiveData<String> currentCategory;
    private MutableLiveData<ArrayList<String>> names;
    private MutableLiveData<ArrayList<String>> logins;
    private MutableLiveData<ArrayList<Integer>> ids;
    private MutableLiveData<ArrayList<Bitmap>> bitmaps;
    private MutableLiveData<ArrayList<String>> categories;
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

    public void setCurrentSearchQuery(String query) {
        currentSearchQuery.setValue(query);
    }

    public void setCurrentCategory(String category) {
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

    public boolean updateList() throws Exception {
        categories.getValue().clear();
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
        this.names.postValue(strings3);
        this.logins.postValue(logins);
        this.bitmaps.postValue(bitmapArrayList);
        this.ids.postValue(ids);
        if ((currentSearchQuery.getValue() == null || currentSearchQuery.getValue().matches("")) && (currentCategory.getValue() == null || currentCategory.getValue().matches(""))) {
            categories.getValue().clear();
            categories.getValue().addAll(Encryptor.getCategories(database));
            Encryptor.closeDataBase(database);
            return true;
        } else {
            Encryptor.closeDataBase(database);
            return false;
        }
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

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public boolean createCategory(String name) throws Exception {
        byte[] pass = intent.getByteArrayExtra("pass");
        String password = Encryptor.rsadecrypt(pass);
        SQLiteDatabase database = Encryptor.initDataBase(getApplication().getBaseContext(), password);
        ArrayList<String> categories = Encryptor.getCategories(database);
        if (!categories.contains(name)) {
            Encryptor.createCategoryStub(database, name);
            return true;
        } else {
            return false;
        }
    }
}
