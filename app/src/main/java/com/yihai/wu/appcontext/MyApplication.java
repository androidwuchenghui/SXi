package com.yihai.wu.appcontext;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.app.Application;
import com.crashlytics.android.Crashlytics;
import com.yihai.wu.crashutil.CrashHandler;
import com.yihai.wu.util.Constant;
import com.yihai.wu.util.RetrofitService;

import io.fabric.sdk.android.Fabric;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by ${Wu} on 2016/12/15.
 */

public class MyApplication extends Application {

    public  static RetrofitService mRetrofitService;
    @Override
    public void onCreate() {
        super.onCreate();
        ActiveAndroid.initialize(this);
        Fabric.with(this, new Crashlytics());
        mRetrofitService = initRetrofit();
        CrashHandler.getInstance().init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ActiveAndroid.dispose();
    }

    public RetrofitService initRetrofit(){
        Retrofit retrofit = new Retrofit.Builder()
//                .client(new OkHttpClient())
                .baseUrl(Constant.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())//告诉他用Gson解析数据
                .build();
        return retrofit.create(RetrofitService.class);
    }

}
