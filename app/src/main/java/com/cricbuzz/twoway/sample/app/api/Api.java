package com.cricbuzz.twoway.sample.app.api;

import com.cricbuzz.twoway.sample.app.util.Item;

import java.util.List;

import retrofit2.Response;
import retrofit2.http.GET;
import rx.Observable;

/**
 * Created by sathish-n on 20/8/16.
 */

public interface Api {

    @GET("posts")
    Observable<Response<List<Item>>> getSampleJson();
}
