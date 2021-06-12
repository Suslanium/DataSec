package com.suslanium.encryptor.ui.notebook;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.suslanium.encryptor.util.Encryptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class NotebookViewModel extends AndroidViewModel {

    private File originEncNote;
    public NotebookViewModel(@NonNull Application application) {
        super(application);
    }

    private String readFromFile(String path) throws Exception {
        String ret = "";
        InputStream inputStream = new FileInputStream(new File(path));
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString = "";
            StringBuilder stringBuilder = new StringBuilder();
            boolean first = true;
            while ((receiveString = bufferedReader.readLine()) != null) {
                if(!first)stringBuilder.append("\n").append(receiveString);
                else {
                    stringBuilder.append(receiveString);
                    first = false;
                }
            }
            inputStream.close();
            ret = stringBuilder.toString();
        }
        return ret;
    }

    protected String readText(String fileName, byte[] pass) throws Exception {
        String password = Encryptor.rsadecrypt(pass);
        File encNote = new File(getApplication().getBaseContext().getFilesDir().getPath() + File.separator + "Notes" + File.separator + fileName);
        originEncNote = encNote;
        File tempNote = new File(getApplication().getBaseContext().getApplicationInfo().dataDir + File.separator + "noteTemp" +File.separator+ encNote.getName());
        tempNote.delete();
        tempNote.getParentFile().mkdirs();
        Encryptor.decryptFileAES256(encNote, password, tempNote);
        String text = readFromFile(tempNote.getPath());
        Encryptor.wipeFile(tempNote);
        tempNote.delete();
        return text;
    }

    protected File getOriginEncNote(){
        return originEncNote;
    }

    protected void checkFileValidName(File file) throws Exception{
        file.getParentFile().mkdirs();
        file.createNewFile();
        file.delete();
    }

    protected void saveNote(File tempNote, File encNote, byte[] pass, String text,boolean deleteOrigin) throws Exception{
        String password = Encryptor.rsadecrypt(pass);
        tempNote.delete();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(tempNote));
        outputStreamWriter.write(text);
        outputStreamWriter.close();
        if (originEncNote != null && deleteOrigin) originEncNote.delete();
        encNote.delete();
        Encryptor.encryptFileAES256(tempNote, password, encNote);
        Encryptor.wipeFile(tempNote);
        tempNote.delete();
    }
}
