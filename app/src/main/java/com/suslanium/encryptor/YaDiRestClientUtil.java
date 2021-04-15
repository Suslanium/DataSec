package com.suslanium.encryptor;


import com.squareup.okhttp.OkHttpClient;
import com.yandex.disk.rest.OkHttpClientFactory;
import com.yandex.disk.rest.RestClient;

public class YaDiRestClientUtil {

    public static RestClient getInstance(final YaDiCredentials credentials) {
        OkHttpClient client = OkHttpClientFactory.makeClient();
        return new RestClient(credentials, client);
    }
}
