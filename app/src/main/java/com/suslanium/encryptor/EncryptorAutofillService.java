package com.suslanium.encryptor;

import android.app.PendingIntent;
import android.app.assist.AssistStructure;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveRequest;
import android.view.View;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.suslanium.encryptor.ui.PasswordActivity;
import com.suslanium.encryptor.util.Encryptor;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class EncryptorAutofillService extends AutofillService {
    private final String[] loginKeyWords = new String[]{"phone", "Phone", "телефон", "Телефон", "login", "Login", "логин", "Логин", "name", "Name", "имя", "Имя", "user", "User", "пользовател", "Пользовател", "mail", "Mail", "address", "Address", "почт", "Почт", "Адрес", "адрес"};
    private final String[] passKeyWords = new String[]{"pass", "Pass", "Пароль", "пароль"};
    protected static byte[] pass = null;
    private String password = null;

    @Override
    public void onFillRequest(@NonNull FillRequest request, @NonNull CancellationSignal cancellationSignal, @NonNull FillCallback callback) {
        try {
            List<FillContext> context = request.getFillContexts();
            AssistStructure structure = context.get(context.size() - 1).getStructure();
            String appPackageName = structure.getActivityComponent().getPackageName();
            if (!appPackageName.equals(getPackageName())) {
                String appName;
                PackageManager pm = getApplicationContext().getPackageManager();
                ApplicationInfo ai;
                try {
                    ai = pm.getApplicationInfo(this.getPackageName(), 0);
                } catch (final PackageManager.NameNotFoundException e) {
                    ai = null;
                }
                appName = (String) (ai != null ? pm.getApplicationLabel(ai) : "Null");
                List<AssistStructure.ViewNode> loginNodes = new ArrayList<>();
                List<AssistStructure.ViewNode> passNodes = new ArrayList<>();
                List<String> texts = new ArrayList<>();
                parseAutoFillFields(structure.getWindowNodeAt(0).getRootViewNode(), loginNodes, false);
                parseAutoFillFields(structure.getWindowNodeAt(0).getRootViewNode(), passNodes, true);
                getPossibleAppNames(structure.getWindowNodeAt(0).getRootViewNode(), texts);
                ArrayList<AutofillId> ids = new ArrayList<>();
                int type = 0;
                if (loginNodes.size() > 0 && passNodes.size() > 0) {
                    for (int i = 0; i < loginNodes.size(); i++) {
                        ids.add(loginNodes.get(i).getAutofillId());
                    }
                    for (int i = 0; i < passNodes.size(); i++) {
                        ids.add(passNodes.get(i).getAutofillId());
                    }
                    type = 1;
                } else if (loginNodes.size() > 0) {
                    for (int i = 0; i < loginNodes.size(); i++) {
                        ids.add(loginNodes.get(i).getAutofillId());
                    }
                    type = 2;
                } else if (passNodes.size() > 0) {
                    for (int i = 0; i < passNodes.size(); i++) {
                        ids.add(passNodes.get(i).getAutofillId());
                    }
                    type = 3;
                } else {
                    return;
                }
                if (pass != null) {
                    if (password == null) {
                        try {
                            password = Encryptor.rsadecrypt(pass);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (password != null) {
                    SQLiteDatabase database = Encryptor.initDataBase(getBaseContext(), password);
                    HashMap<Integer, ArrayList<String>> integerArrayListHashMap = Encryptor.readPasswordData(database);
                    Encryptor.closeDataBase(database);
                    ArrayList<Integer> integers = new ArrayList<>();
                    for (Integer i : integerArrayListHashMap.keySet()) {
                        if (appPackageName.toLowerCase().contains(integerArrayListHashMap.get(i).get(0).toLowerCase()) || appPackageName.contains(integerArrayListHashMap.get(i).get(0)) || checkForAppName(texts, integerArrayListHashMap.get(i).get(0)) || appName.contains(integerArrayListHashMap.get(i).get(0)) || appName.toLowerCase().contains(integerArrayListHashMap.get(i).get(0).toLowerCase())) {
                            integers.add(i);
                        }
                    }
                    if (!integers.isEmpty()) {
                        switch (type) {
                            case 2:
                                if (!ids.isEmpty()) {
                                    ArrayList<Dataset.Builder> datasets = new ArrayList<>();
                                    for (Integer i : integers) {
                                        if (integerArrayListHashMap.get(i).get(1) != null && !integerArrayListHashMap.get(i).get(1).matches("")) {
                                            Dataset.Builder builder = new Dataset.Builder();
                                            RemoteViews views = new RemoteViews(getPackageName(), R.layout.autofilllistitem);
                                            views.setTextViewText(R.id.textToSet, integerArrayListHashMap.get(i).get(0) + ": " + integerArrayListHashMap.get(i).get(1));
                                            for (int j = 0; j < ids.size(); j++) {
                                                builder.setValue(ids.get(j), AutofillValue.forText(integerArrayListHashMap.get(i).get(1)), views);
                                            }
                                            datasets.add(builder);
                                        }
                                    }
                                    if (!datasets.isEmpty()) {
                                        FillResponse.Builder builder = new FillResponse.Builder();
                                        for (int i = 0; i < datasets.size(); i++) {
                                            builder.addDataset(datasets.get(i).build());
                                        }
                                        FillResponse response = builder.build();
                                        callback.onSuccess(response);
                                    }
                                } else {
                                    return;
                                }
                                break;
                            case 3:
                                if (!ids.isEmpty()) {
                                    ArrayList<Dataset.Builder> datasets = new ArrayList<>();
                                    for (Integer i : integers) {
                                        if (integerArrayListHashMap.get(i).get(2) != null && !integerArrayListHashMap.get(i).get(2).matches("")) {
                                            Dataset.Builder builder = new Dataset.Builder();
                                            RemoteViews views = new RemoteViews(getPackageName(), R.layout.autofilllistitem);
                                            views.setTextViewText(R.id.textToSet, integerArrayListHashMap.get(i).get(0) + ": " + generateMaskedPass(integerArrayListHashMap.get(i).get(2).length()));
                                            for (int j = 0; j < ids.size(); j++) {
                                                builder.setValue(ids.get(j), AutofillValue.forText(integerArrayListHashMap.get(i).get(2)), views);
                                            }
                                            datasets.add(builder);
                                        }
                                    }
                                    if (!datasets.isEmpty()) {
                                        FillResponse.Builder builder = new FillResponse.Builder();
                                        for (int i = 0; i < datasets.size(); i++) {
                                            builder.addDataset(datasets.get(i).build());
                                        }
                                        FillResponse response = builder.build();
                                        callback.onSuccess(response);
                                    }
                                } else {
                                    return;
                                }
                                break;
                            case 1:
                                if (!ids.isEmpty()) {
                                    ArrayList<Dataset.Builder> datasets = new ArrayList<>();
                                    for (Integer i : integers) {
                                        if (integerArrayListHashMap.get(i).get(1) != null && !integerArrayListHashMap.get(i).get(1).matches("")) {
                                            Dataset.Builder builder = new Dataset.Builder();
                                            RemoteViews views = new RemoteViews(getPackageName(), R.layout.autofilllistitem);
                                            views.setTextViewText(R.id.textToSet, integerArrayListHashMap.get(i).get(0) + ": " + integerArrayListHashMap.get(i).get(1));
                                            for (int j = 0; j < ids.size(); j++) {
                                                builder.setValue(ids.get(j), AutofillValue.forText(integerArrayListHashMap.get(i).get(1)), views);
                                            }
                                            datasets.add(builder);
                                        }
                                        if (integerArrayListHashMap.get(i).get(2) != null && !integerArrayListHashMap.get(i).get(2).matches("")) {
                                            Dataset.Builder builder = new Dataset.Builder();
                                            RemoteViews views = new RemoteViews(getPackageName(), R.layout.autofilllistitem);
                                            views.setTextViewText(R.id.textToSet, integerArrayListHashMap.get(i).get(0) + ": " + generateMaskedPass(integerArrayListHashMap.get(i).get(2).length()));
                                            for (int j = 0; j < ids.size(); j++) {
                                                builder.setValue(ids.get(j), AutofillValue.forText(integerArrayListHashMap.get(i).get(2)), views);
                                            }
                                            datasets.add(builder);
                                        }
                                    }
                                    if (!datasets.isEmpty()) {
                                        FillResponse.Builder builder = new FillResponse.Builder();
                                        for (int i = 0; i < datasets.size(); i++) {
                                            builder.addDataset(datasets.get(i).build());
                                        }
                                        FillResponse response = builder.build();
                                        callback.onSuccess(response);
                                    }
                                } else {
                                    return;
                                }
                                break;
                            default:
                                break;
                        }
                    }
                } else if (!ids.isEmpty()) {
                    AutofillId[] ids1 = new AutofillId[ids.size()];
                    ids1 = ids.toArray(ids1);
                    RemoteViews authRequest = new RemoteViews(getPackageName(), R.layout.autofilllistitem);
                    authRequest.setTextViewText(R.id.textToSet, getString(R.string.authRequired));
                    Intent authIntent = new Intent(this, PasswordActivity.class);
                    ArrayList<String> txt = new ArrayList<>(texts);
                    authIntent.putExtra("TYPE", type);
                    authIntent.putExtra("IDS", ids);
                    authIntent.putExtra("PACKAGE", appPackageName);
                    authIntent.putExtra("TEXTS", txt);
                    IntentSender intentSender = PendingIntent.getActivity(this, 1001, authIntent, PendingIntent.FLAG_CANCEL_CURRENT).getIntentSender();
                    FillResponse response = new FillResponse.Builder()
                            .setAuthentication(ids1, intentSender, authRequest)
                            .build();
                    callback.onSuccess(response);
                }
            }
        } catch (Exception e) {
            if(pass != null){
                try {
                    String password2 = Encryptor.rsadecrypt(pass);
                    if(!password2.equals(password)){
                        password = password2;
                        onFillRequest(request,cancellationSignal,callback);
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onSaveRequest(@NonNull SaveRequest request, @NonNull SaveCallback callback) {

    }

    public void parseAutoFillFields(AssistStructure.ViewNode node, List<AssistStructure.ViewNode> loginNodes, boolean passwords) {
        String[] hints = node.getAutofillHints();
        String viewId = node.getIdEntry();
        String hint = node.getHint();
        boolean hintsContain = false;
        if (hints != null && hints.length > 0) {
            for (int i = 0; i < hints.length; i++) {
                if (!passwords && checkStringForLogin(hints[i])) {
                    hintsContain = true;
                } else if (passwords && checkStringForPassword(hints[i])) {
                    hintsContain = true;
                }
            }
        }
        if (((!passwords && checkStringForLogin(hint)) || (!passwords && checkStringForLogin(viewId)) || hintsContain || (passwords && checkStringForPassword(hint)) || (passwords && checkStringForPassword(viewId))) && node.isFocused() && node.getVisibility() == View.VISIBLE && node.getHeight() > 2 && node.getWidth() > 2 && node.getAlpha() > 0f) {
            loginNodes.add(node);
        }
        if (node.getChildCount() > 0) {
            for (int i = 0; i < node.getChildCount(); i++) {
                parseAutoFillFields(node.getChildAt(i), loginNodes, passwords);
            }
        }
    }

    public void getPossibleAppNames(AssistStructure.ViewNode node, List<String> names) {
        CharSequence text = node.getText();
        String viewId = node.getIdEntry();
        String hint = node.getHint();
        String[] hints = node.getAutofillHints();
        if (text != null) {
            String textString = text.toString();
            names.add(textString);
        }
        if (hints != null && hints.length > 0) {
            names.addAll(Arrays.asList(hints));
        }
        if (viewId != null) names.add(viewId);
        if (hint != null) names.add(hint);
        if (node.getChildCount() > 0) {
            for (int i = 0; i < node.getChildCount(); i++) {
                getPossibleAppNames(node.getChildAt(i), names);
            }
        }
    }

    public static boolean checkForAppName(List<String> texts, String toCheck) {
        boolean contains = false;
        if (texts != null && !texts.isEmpty()) {
            for (int i = 0; i < texts.size(); i++) {
                if (texts.get(i).toLowerCase().contains(toCheck.toLowerCase()) || texts.get(i).contains(toCheck)) {
                    contains = true;
                }
            }
        }
        return contains;
    }

    public boolean checkStringForLogin(String hint) {
        boolean contains = false;
        for (int i = 0; i < loginKeyWords.length; i++) {
            if (hint != null && hint.contains(loginKeyWords[i])) {
                contains = true;
                break;
            }
        }
        return contains;
    }

    public boolean checkStringForPassword(String hint) {
        boolean contains = false;
        for (int i = 0; i < passKeyWords.length; i++) {
            if (hint != null && hint.contains(passKeyWords[i])) {
                contains = true;
                break;
            }
        }
        return contains;
    }

    public static String generateMaskedPass(int length) {
        StringBuilder pass = new StringBuilder();
        for (int i = 0; i < length; i++) {
            pass.append("●");
        }
        return pass.toString();
    }

    public static void setPass(byte[] passEnc){
        pass = passEnc;
    }
}
