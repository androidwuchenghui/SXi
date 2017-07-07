package com.yihai.wu.appcontext;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.app.Application;

/**
 * Created by ${Wu} on 2016/12/15.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ActiveAndroid.initialize(this);
//        Fabric.with(this, new Crashlytics());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ActiveAndroid.dispose();
    }

}
