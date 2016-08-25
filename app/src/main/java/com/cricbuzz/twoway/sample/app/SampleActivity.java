package com.cricbuzz.twoway.sample.app;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cricbuzz.twoway.lib.BuildConfig;
import com.cricbuzz.twoway.lib.Ln;
import com.cricbuzz.twoway.lib.TwoWayHttpClient;
import com.cricbuzz.twoway.lib.TwoWayRetrofit;
import com.cricbuzz.twoway.sample.app.api.Api;
import com.cricbuzz.twoway.sample.app.util.DefaultSubscriber;
import com.cricbuzz.twoway.sample.app.util.HttpClientBuilder;
import com.cricbuzz.twoway.sample.app.util.Item;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import okhttp3.Cache;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.internal.DiskLruCache;
import okhttp3.internal.Util;
import okhttp3.internal.io.FileSystem;
import okio.Buffer;
import okio.Source;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SampleActivity extends AppCompatActivity {
    String baseUrl = "http://jsonplaceholder.typicode.com/";
    String imageUrl = "http://api.cricbuzz.stg/v1/img/1440x808/i1/c197/cms-img.jpg";
    Api api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        picasso = new Picasso.Builder(getApplicationContext())
                .downloader(new OkHttp3Downloader(provideOkHttpClient(getApplicationContext())))
                .build();
        measureSizeParams();
    }

    public void measureSizeParams() {
        ImageView view = (ImageView) findViewById(R.id.img_news);

    }

    public void fetchdata(View v) {
        SampleJsonSubscriber subscriber = new SampleJsonSubscriber();
        subscription = api.getSampleJson().observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriber);
    }

    public void fetchImage(View v) {
        int height = (int) getResources().getDimension(R.dimen.news_thumbnail_height);
        int width = (int) getResources().getDimension(R.dimen.news_thumbnail_width);
        String s = "width " + width + " height " + height;
        TextView textView = (TextView) findViewById(R.id.txt_heading);
        textView.setText(s);
        picasso.load(imageUrl)
                .resize(width, height)
                .centerCrop()
                .into((ImageView) findViewById(R.id.img_news));
        measureSizeParams();
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

    public String getFromCache(String url) throws Exception {
        final File baseDir = getCacheDir();
        StringBuffer  szBuffer = new StringBuffer ();
        if (baseDir != null) {
            final File cacheDir = new File(baseDir, "http");
            DiskLruCache cache = DiskLruCache.create(FileSystem.SYSTEM, cacheDir, 201105, 2, 10 * 1024 * 1024);
            cache.flush();
            HttpUrl parsed = HttpUrl.parse(url);
            String key = Util.md5Hex(parsed.toString());
            final DiskLruCache.Snapshot snapshot;
            try {
                snapshot = cache.get(key);
                if (snapshot == null) {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }

            final Buffer buffer = new Buffer();
            long length = snapshot.getLength(1);
            Source source = snapshot.getSource(1);
            for (long i = 0; i < length; i += 1024) {
                long count = source.read(buffer, 1024);
                if(count<1){
                    break;
                }
            }
            Log.d(TAG, "Buffer Size " + buffer.size());
            GZIPInputStream bodyIn = new GZIPInputStream(buffer.inputStream()) {
                @Override
                public void close() throws IOException {
                    snapshot.close();
                    buffer.close();
                    super.close();

                }
            };

            byte  tByte [] = new byte [1024];

            while (true)
            {
                int  iLength = bodyIn.read(tByte); // <-- Error comes here
                if (iLength < 0)
                    break;
                szBuffer.append (new String (tByte, 0, iLength));
            }

            bodyIn.close();
            cache.close();
        }
        Log.d(TAG, "String read from buffer " + szBuffer.toString());
        return szBuffer.toString();
    }

    private void convertToList(String buffer) {
        Gson gson = new Gson();
        List<Item> logs = gson.fromJson(buffer, new TypeToken<List<Item>>() {
        }.getType());
        Log.d(TAG, "Deserialized list size " + logs.size());
    }

    TwoWayHttpClient provideTwoWayHttpClient(Context context) {
        return new TwoWayHttpClient.Builder(context)
                .setTimeouts(BuildConfig.CB_HTTP_TIMEOUT)
                .setCache("http", BuildConfig.CB_HTTP_CACHE_SIZE)
                .setResponseCachingTime(87640)
                .build();
    }

    OkHttpClient provideOkHttpClient(Context context) {
        return new HttpClientBuilder(context)
                .setTimeouts(BuildConfig.CB_HTTP_TIMEOUT)
                .setCache("http", BuildConfig.CB_HTTP_CACHE_SIZE)
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
            try {
                convertToList(getFromCache(baseUrl + "posts"));
//                getFromCache(imageUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    private Picasso picasso;
    private Subscription subscription;
    private final String TAG = SampleActivity.class.getSimpleName();
}
