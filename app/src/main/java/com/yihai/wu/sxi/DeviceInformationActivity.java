package com.yihai.wu.sxi;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import com.yihai.wu.appcontext.ConnectedBleDevices;
import com.yihai.wu.base.BaseActivity;
import com.yihai.wu.util.DarkImageButton;

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
    DarkImageButton btnBack;
    @Bind(R.id.connect_state)
    TextView connectState;
    private static final String TAG = "DeviceInfo";
    @Bind(R.id.visionName)
    TextView visionName;

    @Override
    protected int getContentId() {
        return R.layout.activity_deviceinformation;
    }

    @Override
    protected void init() {

        //版本号
        String versionName = getVersionName(this);
        Intent getIntent = getIntent();
        int state = getIntent.getIntExtra("connectState", 0);
        visionName.setText(versionName);
        if (state == 2) {
            connectState.setText(R.string.connected);
            ConnectedBleDevices connectedDevice = ConnectedBleDevices.getLastConnectedDevice();
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

    //版本名
    public static String getVersionName(Context context) {
        return getPackageInfo(context).versionName;
    }

    //版本号
    public static int getVersionCode(Context context) {
        return getPackageInfo(context).versionCode;
    }

    private static PackageInfo getPackageInfo(Context context) {
        PackageInfo pi = null;

        try {
            PackageManager pm = context.getPackageManager();
            pi = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_CONFIGURATIONS);

            return pi;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pi;
    }


}
