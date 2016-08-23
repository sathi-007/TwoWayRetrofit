package com.cricbuzz.twoway.sample.app.util;

/**
 * Created by kiran.kumar on 10/6/15.
 */

/**
 * Default base class used for Subscribing.
 * @param <C>
 */
public class DefaultSubscriber<C> extends rx.Subscriber<C> {
    @Override public void onCompleted() {

    }

    @Override public void onError(Throwable e) {

    }

    @Override public void onNext(C c) {

    }
}