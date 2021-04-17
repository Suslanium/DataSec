package com.suslanium.encryptor;

import com.yandex.disk.rest.ProgressListener;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.json.Link;

import java.io.File;

public class YaDiServiceHelper {
    private RestClient client;
    private final String separator = "%2F";

    public YaDiServiceHelper(RestClient client){
        this.client = client;
    }

    public void uploadFile(File localFile, String serverPath) throws Exception{
        String path = serverPath.replaceAll("/", separator);
        Link uploadLink = client.getUploadLink(path + separator + localFile.getName(), true);
        client.uploadFile(uploadLink, true, localFile, new YaDiProgressListener());
    }

    private class YaDiProgressListener implements ProgressListener{
        @Override
        public void updateProgress(long loaded, long total) {

        }

        @Override
        public boolean hasCancelled() {
            return false;
        }
    }
}
