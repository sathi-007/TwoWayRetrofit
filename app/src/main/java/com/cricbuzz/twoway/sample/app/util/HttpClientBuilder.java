package com.cricbuzz.twoway.sample.app.util;

import android.content.Context;
import android.util.Log;


import com.cricbuzz.twoway.sample.app.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import static okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS;
import static okhttp3.logging.HttpLoggingInterceptor.Level.NONE;


/**
 * Created by kiran.kumar on 10/9/15.
 */

/**
 * Builds an HTTP Client
 */
public class HttpClientBuilder {
    public HttpClientBuilder(Context context) {
        this.context = context;
    }

    public OkHttpClient build() {

        OkHttpClient.Builder okClientBuilder = new OkHttpClient.Builder();

        //todo check with pravin if this required
        /*okClientBuilder.addInterceptor(headerAuthorizationInterceptor);*/

        //Add Logging Interceptor
        okClientBuilder.addInterceptor(provideHttpLoggingInterceptor());

        final File baseDir = context.getCacheDir();
        if (baseDir != null) {
            final File cacheDir = new File(baseDir, cacheName);
            int cacheSize = cacheSizeMB * 1024 * 1024;
            okClientBuilder.cache(new Cache(cacheDir, cacheSize));
        }

        okClientBuilder.connectTimeout(this.timeoutConnect, TimeUnit.SECONDS);
        okClientBuilder.readTimeout(this.timeoutRead, TimeUnit.SECONDS);
        okClientBuilder.writeTimeout(this.timeoutWrite, TimeUnit.SECONDS);

        okClientBuilder.sslSocketFactory(getUnsafeSSLFactory());

        if (null != networkInterceptor) {
            okClientBuilder.addInterceptor(networkInterceptor);
        }
        if (null != normalInterceptor) {
            okClientBuilder.addInterceptor(normalInterceptor);
        }

        return okClientBuilder.build();
    }

    private static HttpLoggingInterceptor provideHttpLoggingInterceptor() {
        HttpLoggingInterceptor httpLoggingInterceptor =
                new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        Log.d(TAG, message);
                    }
                });
        httpLoggingInterceptor.setLevel(BuildConfig.DEBUG ? HEADERS : NONE);
        return httpLoggingInterceptor;
    }

    public SSLSocketFactory getUnsafeSSLFactory() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            return sslSocketFactory;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public HttpClientBuilder setCache(String cacheName, int cacheSizeMB) {
        this.cacheName = cacheName;
        this.cacheSizeMB = cacheSizeMB;
        return this;
    }

    public HttpClientBuilder setTimeouts(int timeout) {
        return setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .setWriteTimeout(timeout);
    }

    public HttpClientBuilder setConnectTimeout(int timeout) {
        this.timeoutConnect = timeout;
        return this;
    }

    public HttpClientBuilder setReadTimeout(int timeout) {
        this.timeoutRead = timeout;
        return this;
    }

    public HttpClientBuilder setWriteTimeout(int timeout) {
        this.timeoutWrite = timeout;
        return this;
    }

    public HttpClientBuilder setNetworkInterceptor(Interceptor interceptor) {
        this.networkInterceptor = interceptor;
        return this;
    }

    public HttpClientBuilder setNormalInterceptor(Interceptor interceptor) {
        this.normalInterceptor = interceptor;
        return this;
    }

    Interceptor headerAuthorizationInterceptor = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Headers headers = request.headers().newBuilder().add("Authorization", "").build();
            request = request.newBuilder().headers(headers).build();
            return chain.proceed(request);
        }
    };

    private int timeoutConnect;
    private int timeoutRead;
    private int timeoutWrite;

    private String cacheName;
    private int cacheSizeMB;

    private Interceptor networkInterceptor, normalInterceptor;

    private final Context context;

    private final static String TAG = HttpClientBuilder.class.getSimpleName();
}
