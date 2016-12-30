package com.yihai.wu.sxi;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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

import static com.yihai.wu.sxi.R.mipmap.f;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Integer[] images = {R.mipmap.a,R.mipmap.b, R.mipmap.c, R.mipmap.d, R.mipmap.e, f};
    String TAG = "print";
    private DarkImageButton btn_connect;
    private DarkImageButton btn_information;
    private DarkImageButton btn_set;
    private DarkImageButton btn_reset;
    //打开蓝牙需要的参数
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECTED = 0X013;
    private static final int REQUST_RESULT=0X111;
    private static final int REQUST_MODE=0X222;
    private static final int REQUEST_CODE_TO_MAIN=0X002;

    //蓝牙
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic g_Character_BaudRate;

    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected: " + "---------服务未连接-------------");
            mBluetoothLeService = null;
        }
    };



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
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
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
        btn_reset = (DarkImageButton) findViewById(R.id.btn_reset);
        btn_reset.setOnClickListener(this);
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
                startActivityForResult(new Intent(this, DeviceScanActivity.class),REQUEST_CONNECTED);
                break;
            case R.id.btn_information:

                startActivity(new Intent(this, DeviceInformationActivity.class));
                break;
            case R.id.btn_set:
                startActivityForResult(new Intent(this, SetActivity.class),REQUST_MODE);
                break;
            case R.id.btn_reset:
                Log.d(TAG, "onClick: "+g_Character_BaudRate);
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: "+requestCode+">>>>"+resultCode);
        boolean m_b_Check_BaudRate = false;
        if(requestCode==REQUEST_CONNECTED&&resultCode==REQUST_RESULT){
            List<BluetoothGattService> supportedGattServices = mBluetoothLeService.getSupportedGattServices();
            Log.d(TAG, "onActivityResult: "+supportedGattServices.size()+">>>>");
            for (BluetoothGattService supportedGattService : supportedGattServices) {

                if (mBluetoothLeService.g_UUID_Service_DeviceConfig.equals(supportedGattService.getUuid())) {
                    m_b_Check_BaudRate = true;
                } else {
                    m_b_Check_BaudRate = false;
                }

                List<BluetoothGattCharacteristic> characteristics = supportedGattService.getCharacteristics();

                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    if(m_b_Check_BaudRate==true&&characteristic.getUuid().equals(mBluetoothLeService.g_UUID_Charater_Baud_Rate)){
                        g_Character_BaudRate = characteristic;
                    }

                }
            }

        }else if (requestCode==REQUST_MODE&&resultCode==REQUEST_CODE_TO_MAIN){
            Log.d(TAG, "onActivityResult: "+data.getStringExtra("myMode"));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
}
