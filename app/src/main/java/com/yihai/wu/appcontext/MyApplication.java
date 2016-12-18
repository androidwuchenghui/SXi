package com.yihai.wu.appcontext;

import com.activeandroid.app.Application;

import com.activeandroid.ActiveAndroid;

/**
 * Created by ${Wu} on 2016/12/15.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ActiveAndroid.initialize(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ActiveAndroid.dispose();
    }
}
