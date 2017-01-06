package com.yihai.wu.sxi;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.yihai.wu.base.BaseActivity;

/**
 * Created by ${Wu} on 2016/12/6.
 */

public class DeviceInformationActivity extends BaseActivity implements View.OnClickListener {
    TextView btn_back;
    TextView connectState;
    @Override
    protected int getContentId() {
        return R.layout.activity_deviceinformation;
    }

    @Override
    protected void init() {
        connectState = (TextView) findViewById(R.id.connect_state);
        btn_back = (TextView) findViewById(R.id.btn_back);
        btn_back.setOnClickListener(this);
        Intent getIntent = getIntent();
       int state= getIntent.getIntExtra("connectState",0);
       if(state==2){
           connectState.setText("已连接");
       }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_back:
                finish();
                break;
        }
    }
}
