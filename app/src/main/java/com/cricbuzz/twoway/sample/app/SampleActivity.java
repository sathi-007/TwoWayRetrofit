package com.cricbuzz.twoway.sample.app;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.cricbuzz.twoway.lib.BuildConfig;
import com.cricbuzz.twoway.lib.Ln;
import com.cricbuzz.twoway.lib.TwoWayHttpClient;
import com.cricbuzz.twoway.lib.TwoWayRetrofit;
import com.cricbuzz.twoway.sample.app.api.Api;
import com.cricbuzz.twoway.sample.app.util.DefaultSubscriber;
import com.cricbuzz.twoway.sample.app.util.Item;

import java.util.List;

import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SampleActivity extends AppCompatActivity {
    String baseUrl = "http://jsonplaceholder.typicode.com/";
    Api api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
    }

    public void fetchdata(View v) {
        SampleJsonSubscriber subscriber = new SampleJsonSubscriber();
        subscription = api.getSampleJson().observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriber);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initialize();
    }

    public void initialize() {
        TwoWayHttpClient twoWayHttpClient = provideTwoWayHttpClient(this);
        TwoWayRetrofit retrofit = createTwoWayAdapter(baseUrl, twoWayHttpClient, GsonConverterFactory.create());
        api = (Api) retrofit.create(Api.class);
    }

    TwoWayHttpClient provideTwoWayHttpClient(Context context) {
        return new TwoWayHttpClient.Builder(context)
                .setTimeouts(BuildConfig.CB_HTTP_TIMEOUT)
                .setCache("http", BuildConfig.CB_HTTP_CACHE_SIZE)
                .setResponseCachingTime(87640)
                .build();
    }

    TwoWayRetrofit createTwoWayAdapter(String url,
                                       TwoWayHttpClient client,
                                       Converter.Factory factory) {
        return new TwoWayRetrofit.Builder()
                .client(client)
                .baseUrl(url)
                .addConverterFactory(factory)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build();
    }

    private final class SampleJsonSubscriber extends DefaultSubscriber<Response<List<Item>>> {

        @Override
        public void onCompleted() {

            Ln.i(TAG, "SampleJsonSubscriber Completed!");
            if (subscription != null && !subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
            initialize();
        }

        @Override
        public void onError(Throwable e) {
            Ln.e(TAG, "SampleJsonSubscriber: Error in fetching containers: " + e.getMessage());
        }

        @Override
        public void onNext(Response<List<Item>> response) {
            //todo need to handle Error Handling based in response, discuss with Harsha/Pravin for figuring the retry mechanism
            Ln.d(TAG, "Response Json List:" + response.isSuccessful() + "---" + response.code());


        }
    }

    private Subscription subscription;
    private final String TAG = SampleActivity.class.getSimpleName();
}
