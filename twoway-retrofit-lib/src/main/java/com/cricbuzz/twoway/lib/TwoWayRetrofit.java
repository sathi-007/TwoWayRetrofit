package com.cricbuzz.twoway.lib;


import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Executor;

import okhttp3.Call;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import rx.Observable;

/**
 * Created by sathish-n on 23/4/16.
 */
public class TwoWayRetrofit<T, K> {

    T realClass, cacheClass;
    Retrofit realRetrofit, cacheRetrofit;

    private TwoWayRetrofit(Retrofit retrofit, Retrofit cacheRetrofit) {
        this.realRetrofit = retrofit;
        this.cacheRetrofit = cacheRetrofit;
    }

    public T create(final Class<T> api) {
        realClass = realRetrofit.create(api);
        T tClass = (T) Proxy.newProxyInstance(
                api.getClassLoader(),
                new Class<?>[]{api},
                new InvocationHandler() {
                    @Override
                    public Observable<K> invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Ln.e(TAG, "setting method Name " + method.getName());
                        String baseUrl = realRetrofit.baseUrl().toString();
                        Annotation[] annotations = method.getAnnotations();
                        if (annotations.length > 0) {
                            for (Annotation annotation : annotations) {
                                if (annotation instanceof GET) {
                                    String value = ((GET) annotation).value();
                                    Ln.e(TAG, baseUrl + " GET Method Value " + value);
//                                    Retrofit retrofit1 = cacheRetrofit(baseUrl);
                                    cacheClass = cacheRetrofit.create(api);
                                }
                            }
                        }
                        Method[] methods = cacheClass.getClass().getMethods();
                        Method impMethod = null;
                        for (Method method1 : methods) {
                            if (method1.getName().contentEquals(method.getName()) && method.getParameterTypes().length == method1.getParameterTypes().length) {
                                impMethod = method1;
                            }
                        }
                        return Observable.concat((Observable<K>) impMethod.invoke(cacheClass, args), (Observable<K>) method.invoke(realClass, args))
                                .onErrorResumeNext((Observable<K>) method.invoke(realClass, args));
                    }
                });

        return tClass;
    }

    public static class Builder {

        private TwoWayHttpClient twoWayHttpClient;
        private String httpBaseUrl;
        private Converter.Factory converterFactory;
        private CallAdapter.Factory callAdapter;
        private Executor executor;
        private Call.Factory callFactory;
        private boolean validateEagerly;


        public Builder client(TwoWayHttpClient httpClient) {
            this.twoWayHttpClient = httpClient;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.httpBaseUrl = baseUrl;
            return this;
        }

        public Builder addConverterFactory(Converter.Factory converte) {
            this.converterFactory = converte;
            return this;
        }

        public Builder addCallAdapterFactory(CallAdapter.Factory callAdapter) {
            this.callAdapter = callAdapter;
            return this;
        }

        public Builder callBackExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder callFactory(Call.Factory callFactory) {
            this.callFactory = callFactory;
            return this;
        }

        public Builder validateEagerly(boolean validateEagerly) {
            this.validateEagerly = validateEagerly;
            return this;
        }

        public TwoWayRetrofit build() {
            Retrofit.Builder builder =
                    new Retrofit.Builder();
            Retrofit.Builder cacheRetrofitBuilder =
                    new Retrofit.Builder();

            if (twoWayHttpClient != null) {
                builder.client(twoWayHttpClient.getRealClient());
                cacheRetrofitBuilder.client(twoWayHttpClient.getCacheClient());
            }

            if (this.httpBaseUrl != null && this.httpBaseUrl.length() > 0) {
                builder.baseUrl(httpBaseUrl);
                cacheRetrofitBuilder.baseUrl(httpBaseUrl);
            }

            if (this.converterFactory != null) {
                builder.addConverterFactory(this.converterFactory);
                cacheRetrofitBuilder.addConverterFactory(this.converterFactory);
            }

            if (this.callAdapter != null) {
                builder.addCallAdapterFactory(this.callAdapter);
                cacheRetrofitBuilder.addCallAdapterFactory(this.callAdapter);
            }

            if (this.callFactory != null) {
                builder.callFactory(this.callFactory);
                cacheRetrofitBuilder.callFactory(this.callFactory);
            }

            if(this.executor!=null){
                builder.callbackExecutor(this.executor);
                cacheRetrofitBuilder.callbackExecutor(this.executor);
            }

            builder.validateEagerly(validateEagerly);
            cacheRetrofitBuilder.validateEagerly(validateEagerly);

            //.setErrorHandler(new EndpointErrorHandler(endpoint)) //todo removed in 2.0
            Retrofit retrofit = builder.build();
            return new TwoWayRetrofit(retrofit, cacheRetrofitBuilder.build());
        }

    }

    final String TAG = "TwoWayRetrofit";
}