package com.example.stripedemo.net;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Factory to generate our Retrofit instance.
 */
public class RetrofitFactory {

    // Put your Base URL here. Unless you customized it, the URL will be something like
    // https://hidden-beach-12345.herokuapp.com/
    private static final String BASE_URL = "http://192.168.34.19:8181/stripe/customer/";
    private static Retrofit mInstance = null;

    public static Retrofit getInstance() {
        if (mInstance == null) {


            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            // Adding Rx so the calls can be Observable, and adding a Gson converter with
            // leniency to make parsing the results simple.
            mInstance = new Retrofit.Builder()
                    .client(genericClient())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .baseUrl(BASE_URL)
                    .build();
        }
        return mInstance;
    }

    private static OkHttpClient genericClient() {
        OkHttpClient okHttpClient;
        X509TrustManager mX509TrustManager;
        try {
            // Create a trust manager that does not validate certificate chains
            mX509TrustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(
                        X509Certificate[] chain,
                        String authType) throws CertificateException {
                    Log.e("test", "checkClientTrusted");
                }

                @Override
                public void checkServerTrusted(
                        X509Certificate[] chain,
                        String authType) throws CertificateException {
                    if (chain == null) {
                        throw new IllegalArgumentException("Check Server X509Certificate is null");
                    }
                    if (chain.length < 0) {
                        throw new IllegalArgumentException("Check Server X509Certificate is empty");
                    }
                    for (X509Certificate cert : chain) {
                        cert.checkValidity();
                    }
                    Log.e("test", "checkServerTrusted");
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            final TrustManager[] trustAllCerts = new TrustManager[]{mX509TrustManager};

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts,
                    new SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext
                    .getSocketFactory();
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, mX509TrustManager)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS);
            // Log信息拦截器

            // Set your desired log level. Use Level.BODY for debugging errors.
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);//包含header，body数据
            //设置 Debug Log 模式
            builder.addInterceptor(loggingInterceptor);


            okHttpClient = builder.build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
