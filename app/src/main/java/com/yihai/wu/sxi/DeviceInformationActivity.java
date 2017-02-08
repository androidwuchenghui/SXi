package com.yihai.wu.sxi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.yihai.wu.appcontext.ConnectedBleDevices;
import com.yihai.wu.base.BaseActivity;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ${Wu} on 2016/12/6.
 */

public class DeviceInformationActivity extends BaseActivity {


    @Bind(R.id.name_after)
    TextView nameAfter;
    @Bind(R.id.soft_after)
    TextView softAfter;
    @Bind(R.id.id_after)
    TextView idAfter;
    @Bind(R.id.btn_back)
    TextView btnBack;
    @Bind(R.id.connect_state)
    TextView connectState;
    private static final String TAG = "DeviceInformationActivi";

    @Override
    protected int getContentId() {
        return R.layout.activity_deviceinformation;
    }

    @Override
    protected void init() {


        Intent getIntent = getIntent();
        int state = getIntent.getIntExtra("connectState", 0);
        if (state == 2) {
            connectState.setText("已连接");
            ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
            nameAfter.setText(connectedDevice.realName);
            softAfter.setText(connectedDevice.softVision);
            idAfter.setText(connectedDevice.deviceID);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: add setContentView(...) invocation
        ButterKnife.bind(this);
    }

    @OnClick(R.id.btn_back)
    public void onClick() {
        finish();
    }
}
