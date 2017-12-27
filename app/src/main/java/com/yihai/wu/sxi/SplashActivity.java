package com.yihai.wu.sxi;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.yihai.wu.appcontext.MyModel;
import com.yihai.wu.util.LogService;

/**
 * Created by ${Wu} on 2016/12/1.
 */

public class SplashActivity extends AppCompatActivity {
    Handler mHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initdb();
        init();
        Intent stateService =  new Intent (this, LogService.class);
        startService( stateService );
    }


    private void initdb() {

        MyModel myModel = MyModel.getMyModelForGivenName("C1");
        MyModel myModel2 = MyModel.getMyModelForGivenName("C2");
        MyModel myModel3 = MyModel.getMyModelForGivenName("C3");
        MyModel myModel4 = MyModel.getMyModelForGivenName("C4");
        MyModel myModel5 = MyModel.getMyModelForGivenName("C5");
        if (myModel == null) {
            MyModel.initMyModel("C1");
        }
        if (myModel2 == null) {
            MyModel.initMyModel("C2");
        }
        if (myModel3 == null) {
            MyModel.initMyModel("C3");
        }
        if (myModel4 == null) {
            MyModel.initMyModel("C4");
        }
        if (myModel5 == null) {
            MyModel.initMyModel("C5");
        }
    }

    private void init() {
        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                finish();
            }
        }, 1000);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
