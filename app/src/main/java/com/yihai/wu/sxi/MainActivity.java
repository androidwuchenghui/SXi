package com.yihai.wu.sxi;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.yihai.wu.util.DarkImageButton;
import com.yihai.wu.util.GlideImageLoader;
import com.youth.banner.Banner;
import com.youth.banner.BannerConfig;
import com.youth.banner.listener.OnBannerClickListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Integer[] images = {R.mipmap.a,R.mipmap.b, R.mipmap.c, R.mipmap.d, R.mipmap.e, R.mipmap.f};
    String TAG = "print";
    private DarkImageButton btn_connect;
    private DarkImageButton btn_information;
    private DarkImageButton btn_set;
    //打开蓝牙需要的参数
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();

            // finish();
            return;
        }


        initBanner();
        initButton();
    }

    //弹出提示框提示打开蓝牙
    @Override
    protected void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {

                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void initButton() {
        btn_connect = (DarkImageButton) findViewById(R.id.btn_connect);
        btn_connect.setOnClickListener(this);
        btn_information = (DarkImageButton) findViewById(R.id.btn_information);
        btn_information.setOnClickListener(this);
        btn_set = (DarkImageButton) findViewById(R.id.btn_set);
        btn_set.setOnClickListener(this);
    }

    private void initBanner() {
        Banner banner = (Banner) findViewById(R.id.head_banner);
        banner.setImageLoader(new GlideImageLoader());
        List images_list = new ArrayList();
        for (Integer image : images) {
            images_list.add(image);
        }
        banner.setImages(images_list);
        banner.setIndicatorGravity(BannerConfig.CENTER);
        banner.start();
        banner.setOnBannerClickListener(new OnBannerClickListener() {
            @Override
            public void OnBannerClick(int position) {
                Log.d(TAG, "OnBannerClick: " + position); //position 从1开始
                Intent intent = new Intent(MainActivity.this,WebViewActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_connect:
                startActivity(new Intent(this, DeviceScanActivity.class));
                break;
            case R.id.btn_information:
                startActivity(new Intent(this, DeviceInformationActivity.class));
                break;
            case R.id.btn_set:
                startActivity(new Intent(this, SetActivity.class));
                break;
        }

    }


    /**
     * 点击返回退出APP
     */
    private long end;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            //按的是返回键
            //dialog弹出
            //			dialog.show();

            if(System.currentTimeMillis() - end <= 3000){
                //关闭当前应用
                this.finish();
            } else {
                Toast.makeText(this, "再按一次退出应用", Toast.LENGTH_SHORT).show();
                end = System.currentTimeMillis();
            }
        }

        //表示拦截back事件
        return true;
    }
}
