package com.cricbuzz.twoway.lib;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by sathish-n on 7/6/16.
 */

public class TwoWayHttpClient {

    final OkHttpClient realClient, cacheClient;

    private TwoWayHttpClient(OkHttpClient realClient, OkHttpClient cacheClient) {
        this.realClient = realClient;
        this.cacheClient = cacheClient;
    }

    public OkHttpClient getRealClient() {
        return realClient;
    }

    public OkHttpClient getCacheClient() {
        return cacheClient;
    }

    public static class Builder {
        public Builder(Context context) {
            this.context = context;
        }

        public Builder setCache(String cacheName, int cacheSizeMB) {
            this.cacheName = cacheName;
            this.cacheSizeMB = cacheSizeMB;
            return this;
        }

        public Builder setTimeouts(int timeout) {
            return setConnectTimeout(timeout)
                    .setReadTimeout(timeout)
                    .setWriteTimeout(timeout);
        }

        public Builder setConnectTimeout(int timeout) {
            this.timeoutConnect = timeout;
            return this;
        }

        public Builder setReadTimeout(int timeout) {
            this.timeoutRead = timeout;
            return this;
        }

        public Builder setWriteTimeout(int timeout) {
            this.timeoutWrite = timeout;
            return this;
        }

        public Builder setNetworkInterceptor(Interceptor interceptor) {
            this.networkInterceptor = interceptor;
            return this;
        }

        public Builder setNormalInterceptor(Interceptor interceptor) {
            this.normalInterceptor = interceptor;
            return this;
        }

        class CacheReadInterceptor implements Interceptor {

            public CacheReadInterceptor() {
            }

            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();

                Response originalResponse;
                Request newRequest = request.newBuilder()
                        .removeHeader("Pragma")
                        .removeHeader("Cache-Control")
//                        .header("Cache-Control", "only-if-cached, max-stale=" + maxStale)
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .build();

                originalResponse = chain.proceed(newRequest);
                if (originalResponse != null&&originalResponse.code()==504) {
                    Ln.d(TAG, "CacheReadInterceptor  request " + newRequest.headers().toString() + " response " + originalResponse.headers().toString() + " response code " + originalResponse.code());
                    return originalResponse.newBuilder().build();
                }
                return originalResponse;
            }
        }

        class NWEnforceInterceptor implements Interceptor {


            public NWEnforceInterceptor() {
            }

            @Override
            public Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();
                Request request = originalRequest.newBuilder()
                        .removeHeader("Pragma")
                        .removeHeader("Cache-Control")
                        .cacheControl(CacheControl.FORCE_NETWORK)
                        .build();

                Response response = chain.proceed(request);
                Ln.d(TAG, "NWEnforceInterceptor request " + request.headers().toString() + " response " + response.headers().toString());
                return response;
            }
        }

        class CacheWriteInterceptor implements Interceptor {

            final long cachingTime;

            public CacheWriteInterceptor(long cachingTime) {
                this.cachingTime = cachingTime;
            }

            @Override
            public Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();
                Response response = chain.proceed(originalRequest);
                Ln.d(TAG, "CacheWriteInterceptor request " + originalRequest.headers().toString() + " response " + response.headers().toString());
                return response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control","public, max-age="+cachingTime)
                        .build();
            }
        }

        public TwoWayHttpClient build() {

            OkHttpClient.Builder okClientBuilder = new OkHttpClient.Builder();
            OkHttpClient.Builder okCacheClientBuilder = new OkHttpClient.Builder();


            final File baseDir = context.getCacheDir();
            if (baseDir != null) {
                final File cacheDir = new File(baseDir, cacheName);
                int cacheSize = cacheSizeMB * 1024 * 1024;
                okClientBuilder.cache(new Cache(cacheDir, cacheSize));
                okCacheClientBuilder.cache(new Cache(cacheDir, cacheSize));
            }

            okClientBuilder.connectTimeout(this.timeoutConnect, TimeUnit.SECONDS);
            okClientBuilder.readTimeout(this.timeoutRead, TimeUnit.SECONDS);
            okClientBuilder.writeTimeout(this.timeoutWrite, TimeUnit.SECONDS);

            okCacheClientBuilder.connectTimeout(this.timeoutConnect, TimeUnit.SECONDS);
            okCacheClientBuilder.readTimeout(this.timeoutRead, TimeUnit.SECONDS);
            okCacheClientBuilder.writeTimeout(this.timeoutWrite, TimeUnit.SECONDS);

            HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
            okClientBuilder.addInterceptor(httpLoggingInterceptor);

            HttpLoggingInterceptor cachedLoggingInterceptor = new HttpLoggingInterceptor();
            cachedLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
            okCacheClientBuilder.addInterceptor(cachedLoggingInterceptor);

            if (null != networkInterceptor) {
                okClientBuilder.networkInterceptors().add(networkInterceptor);
                okCacheClientBuilder.networkInterceptors().add(networkInterceptor);
            }

            if (null != normalInterceptor) {
                okClientBuilder.interceptors().add(normalInterceptor);
                okCacheClientBuilder.interceptors().add(normalInterceptor);
            }

            //Adding CacheRead Intereceptor to CacheOkHttpCLient
            okCacheClientBuilder.interceptors().add(new CacheReadInterceptor());

            //Adding Network Enforce Interceptor RealOkHttpClient
            okClientBuilder.interceptors().add(new NWEnforceInterceptor());

            if (responseCachingTime > 0) {
                //Adding CacheWrite Interceptor to enforce response caching
                okClientBuilder.addNetworkInterceptor(new CacheWriteInterceptor(responseCachingTime));
            }



            return new TwoWayHttpClient(okClientBuilder.build(), okCacheClientBuilder.build());
        }

        private int timeoutConnect;
        private int timeoutRead;
        private int timeoutWrite;

        private String cacheName;
        private int cacheSizeMB;
        private long responseCachingTime = 0;

        public Builder setResponseCachingTime(long responseCachingTime) {
            this.responseCachingTime = responseCachingTime;
            return this;
        }

        private Interceptor networkInterceptor, normalInterceptor;

        private final Context context;
        private final String TAG = TwoWayHttpClient.class.getSimpleName();
    }
}
