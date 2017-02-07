package com.yihai.wu.sxi;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.yihai.wu.appcontext.ConnectedBleDevices;
import com.yihai.wu.appcontext.MyModel;
import com.yihai.wu.util.DarkImageButton;
import com.yihai.wu.util.GlideImageLoader;
import com.youth.banner.Banner;
import com.youth.banner.BannerConfig;
import com.youth.banner.listener.OnBannerClickListener;

import java.util.ArrayList;
import java.util.List;

import static com.yihai.wu.sxi.DeviceScanActivity.BinaryToHexString;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Integer[] images = {R.mipmap.a, R.mipmap.b, R.mipmap.c, R.mipmap.d, R.mipmap.e, R.mipmap.f};
    String TAG = "print";
    private DarkImageButton btn_connect;
    private DarkImageButton btn_information;
    private DarkImageButton btn_set;
    private DarkImageButton btn_reset;
    private TextView connectedState;
    //打开蓝牙需要的参数
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECTED = 0X013;  //19
    private static final int REQUST_RESULT = 0X111;        //273
    private static final int REQUST_MODE = 0X222;             //546
    private static final int REQUEST_CODE_TO_MAIN = 0X002;    //2
    private static final int AckUserDeviceSetting = 0X58;    //2

    //蓝牙
    private BluetoothLeService mBluetoothLeService;

    //一些特征值
    private BluetoothGattCharacteristic g_Character_TX;

    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            if(mBluetoothLeService.getTheConnectedState()==0){
                connectedState.setText("已连接设备");
            }else if(mBluetoothLeService.getTheConnectedState()==2){
                connectedState.setText("设备未连接");
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
        //        int a = REQUEST_CONNECTED;
        //        int b = REQUST_RESULT;
        //        int c = REQUST_MODE;
        //        int d = REQUEST_CODE_TO_MAIN;
        //        Log.d(TAG, "onCreate: 0x013:" + a + "  0x111:" + b + "   0x222:" + c + "    0x002:" + d);
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
        registerReceiver(mainActivityReceiver, makeMainBroadcastFilter());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart:再次打开MainActivity---");
        if(mBluetoothLeService.getTheConnectedState()==2){
            connectedState.setText("已连接设备");
            AckUserDeviceSetting();
        }else {
            connectedState.setText("未连接设备");
        }
    }

    private final BroadcastReceiver mainActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothLeService.ACTION_DATA_RX:
                    Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
                    byte[] data = bundle.getByteArray("byteValues");
                    String s = BinaryToHexString(data);
                    Log.d(TAG, "onReceive: MainActivity 收到的数据为：  " + s);
                    Sys_YiHi_Protocol_RX_Porc(data);

                    break;
            }
        }
    };

    private static IntentFilter makeMainBroadcastFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RX);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);

        return intentFilter;
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
        connectedState = (TextView) findViewById(R.id.device_name);
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
                Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_connect:

                startActivityForResult(new Intent(this, DeviceScanActivity.class), REQUEST_CONNECTED);
                break;
            case R.id.btn_information:
                Intent deviceInfo = new Intent(this, DeviceInformationActivity.class);
                deviceInfo.putExtra("connectState", mBluetoothLeService.getTheConnectedState());

                startActivity(deviceInfo);
                break;
            case R.id.btn_set:

                startActivity(new Intent(this, SetActivity.class));
                break;
            case R.id.btn_reset:
                //                Log.d(TAG, "onClick: "+g_Character_BaudRate+"---状态  "+mBluetoothLeService.getTheConnectedState());
               /* MyModel model = MyModel.getMyModelForGivenName("C1");
                List<Textures> curves = model.getCurves();
                Log.d(TAG, "curve: "+curves);
                for (Textures curve : curves) {
                    Log.d(TAG, "curve: "+curve.name);
                }
                List<Textures> all = Textures.getAll();
                for (Textures textures : all) {
                    Log.d(TAG, " modelName :  "+textures.modelName+"  name : "+textures.name+"  data:  "+textures.arr1+"    "+all.size()+"selected   "+textures.selected);
                }*/
                Log.d(TAG, "onClick: " + mBluetoothLeService);
                break;
        }

    }

    private void AckUserDeviceSetting() {
        byte[] m_Data_DeviceSetting = new byte[32];
        int m_Length = 0;
        m_Data_DeviceSetting[0] = 0x55;
        m_Data_DeviceSetting[1] = (byte) 0xFF;
        m_Data_DeviceSetting[3] = 0x01; //Device ID
        m_Data_DeviceSetting[2] = 0x03;
        m_Data_DeviceSetting[4] = 0x57;
        m_Data_DeviceSetting[5] = 0x11;

        m_Length = 6;
        Sys_Proc_Charactor_TX_Send(m_Data_DeviceSetting, m_Length);
    }

    /**
     * 点击返回退出APP
     */
    private long end;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //按的是返回键
            //dialog弹出
            //			dialog.show();

            if (System.currentTimeMillis() - end <= 3000) {
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
        unregisterReceiver(mainActivityReceiver);
        unbindService(mServiceConnection);
        mBluetoothLeService.close();
        mBluetoothLeService = null;
        ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
        if(connectedDevice!=null) {
            connectedDevice.isConnected = false;
            connectedDevice.save();
        }
    }

    private void Sys_Proc_Charactor_TX_Send(byte[] m_Data, int m_Length) {

        byte[] m_MyData = new byte[m_Length];
        for (int i = 0; i < m_Length; i++) {
            m_MyData[i] = m_Data[i];
        }

        if (g_Character_TX == null) {
            Log.e(TAG, "character TX is null");
            return;
        }

        if (m_Length <= 0) {
            return;
        }
        g_Character_TX.setValue(m_MyData);
        mBluetoothLeService.writeCharacteristic(g_Character_TX);
    }

    private void Sys_YiHi_Protocol_RX_Porc(byte[] m_Data) {
        int m_Length = 0;
        int i;
        int m_Index = 0xfe;
        byte m_ValidData_Length = 0;
        byte m_Command = 0;
        int m_iTemp_x10, m_iTemp_x1;
        m_Length = m_Data.length;
        if (m_Length < 5) {
            return;
        }
        //Get sync code.
        for (i = 0; i < m_Length; i++) {
            //if (i<16)
            //{
            //	if (g_b_Use_DEBUG) Log.i(LJB_TAG,"RX proc---Data["+i+"]="+m_Data[i]);
            //}
            //if ((m_Data[i]==0x55)&&(m_Data[(i+1)]==0xFF))
            if (((m_Data[i] == 85) || (m_Data[i] == 0x55))
                    && ((m_Data[(i + 1)] == -1) || (m_Data[(i + 1)] == 0xFF)
                    || (m_Data[(i + 1)] == -3) || (m_Data[i + 1] == 0xFD))) {
                //if (g_b_Use_DEBUG) Log.i(LJB_TAG,"RX proc---i="+i);
                m_Index = i;
                //i=m_Length;
                break;
            }

        }
        if (m_Index == 0xfe) {
            return;
        }
        if (m_Index > (m_Length - 2)) {
            return;
        }
        //Get valid data length.
        m_ValidData_Length = m_Data[(m_Index + 2)];
        if ((m_Index + m_ValidData_Length) > m_Length) {
            return;
        }
        //Get command code.
        m_Command = m_Data[(m_Index + 4)];
        Log.d(TAG, "onReceiveMainActivity: " + m_Command);
        switch (m_Command) {
            case AckUserDeviceSetting:   //查看系统设定的 C
                String s = BinaryToHexString(m_Data);
                String substring = s.substring(12);

                int model = Integer.parseInt(substring);
                Log.d(TAG, "处理数据:     " + s + "    " + substring + " m:  " + model);
                String selectedModel = "C" + (model + 1);


                MyModel c1 = MyModel.getMyModelForGivenName(selectedModel);
                c1.modelSelected = 1;
                c1.save();
                break;
        }


    }

}
