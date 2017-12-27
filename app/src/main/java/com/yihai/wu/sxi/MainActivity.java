package com.yihai.wu.sxi;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.activeandroid.query.Delete;
import com.yihai.wu.appcontext.ConnectedBleDevices;
import com.yihai.wu.appcontext.MyModel;
import com.yihai.wu.entity.BannerEntity;
import com.yihai.wu.util.DarkImageButton;
import com.yihai.wu.util.GlideImageLoader;
import com.yihai.wu.util.WallpaperDialogView;
import com.youth.banner.Banner;
import com.youth.banner.BannerConfig;
import com.youth.banner.listener.OnBannerClickListener;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.yihai.wu.appcontext.MyApplication.mRetrofitService;
import static com.yihai.wu.util.MyUtils.BinaryToHexString;
import static com.yihai.wu.util.MyUtils.intToBytes;
import static com.yihai.wu.util.MyUtils.intToBytes2;
import static com.yihai.wu.util.MyUtils.isGpsEnable;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private byte[] pixel_data = new byte[115200];
    Integer[] images = {R.mipmap.a, R.mipmap.b, R.mipmap.c, R.mipmap.d, R.mipmap.e, R.mipmap.f};
    private static final String TAG = "MainActivity";
    private DarkImageButton btn_connect;
    private DarkImageButton btn_information;
    private DarkImageButton btn_set;
    private DarkImageButton btn_reset;
    private DarkImageButton btn_upgrade;
    private TextView connectedState;
    private TextView myName;

    //打开蓝牙需要的参数
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECTED = 0X013;     //19
    private static final int REQUST_RESULT = 0X111;        //273
    private static final int REQUST_MODE = 0X222;             //546
    private static final int REQUEST_CODE_TO_MAIN = 0X002;    //2
    private static final int AckUserDeviceSetting = 0X58;    //2
    private ProgressDialog submitDialog;
    private Handler myHandler;
    //服务
    private BluetoothLeService mBluetoothLeService;

    //一些特征值
    private BluetoothGattCharacteristic g_Character_TX;
    private BluetoothGattCharacteristic g_Character_Baud_Rate;

    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            Log.e(TAG, "onServiceConnected: " + mBluetoothLeService.getTheConnectedState() + "  g_TX_char:  " + g_Character_TX + "   lastConnect:  " + lastAddress);
            if (mBluetoothLeService.getTheConnectedState() == 0) {
                connectedState.setText(R.string.have_been_not_connected);
                //              try to connect
                if (lastAddress != null) {
                    Log.e(TAG, "doScan:  绑定成功后    叫后台去搜索   ");
                    mBluetoothLeService.serviceScan();

                }
            } else if (mBluetoothLeService.getTheConnectedState() == 2) {
                connectedState.setText(R.string.have_been_connected);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "onServiceDisconnected: " + "---------服务未连接-------------");
            mBluetoothLeService = null;
        }
    };

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 0x09;
    private String lastAddress;
    private SharedPreferences sp, pixelSp;
    private SharedPreferences.Editor edit, pixelEditor;
    private String deviceName;
    private WallpaperDialogView wallpaperDialogView;


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //        int a = REQUEST_CONNECTED;
        //        int b = REQUST_RESULT;
        //        int c = REQUST_MODE;
        //        int d = REQUEST_CODE_TO_MAIN;
        //        Log.e(TAG, "onCreate: 0x013:" + a + "  0x111:" + b + "   0x222:" + c + "    0x002:" + d);
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        sp = getSharedPreferences("lastConnected", Context.MODE_PRIVATE);
        edit = sp.edit();
        lastAddress = sp.getString("address", null);
        Log.e(TAG, "life :-----onCreate-----     lastAddress： " + lastAddress);

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
        //            mBluetoothAdapter.enable();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            //                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        initButton();
        //android 6.0 权限   还有定位权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, " onCreate:   --打开权限  ");
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
        // 打开gps定位
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isGpsEnable(this) == false) {
            Log.e(TAG, "onCreate:   -- 跳转到定位 --  ");
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            this.startActivityForResult(intent, 0x0A);
        }

        //绑定服务
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        submitDialog = new ProgressDialog(MainActivity.this);
        submitDialog.setTitle(R.string.point_out_title);
        submitDialog.setIcon(R.mipmap.app_icon);
        submitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        submitDialog.setCancelable(true);//设置进度条是否可以按退回键取消
        submitDialog.setIndeterminate(true);
        submitDialog.setCanceledOnTouchOutside(false);
        myHandler = new Handler();

        wallpaperDialogView = new WallpaperDialogView(this);
        wallpaperDialogView.setCanceledOnTouchOutside(false);
        wallpaperDialogView.setCancelable(false);

        //        wallpaperDialogView.show();
        //        walpaperDialogView.setProgress(80);

        Call<BannerEntity> bannerImages = mRetrofitService.getBannerImages();
        bannerImages.enqueue(new Callback<BannerEntity>() {
            @Override
            public void onResponse(Call<BannerEntity> call, Response<BannerEntity> response) {
                List<BannerEntity.ImagesUrlBean> images_url = response.body().getImages_url();
                Log.e(TAG, "onResponse:   下载轮播图成功  " + images_url.size());
                List<String> banner_images = new ArrayList<String>();
                for (BannerEntity.ImagesUrlBean imagesUrlBean : images_url) {
                    //                    Log.e(TAG, "onResponse: "+imagesUrlBean.getLink());
                    banner_images.add(imagesUrlBean.getLink());
                }
                initBanner(banner_images);

            }

            @Override
            public void onFailure(Call<BannerEntity> call, Throwable t) {
                Log.e(TAG, "onFailure: 轮播图 " + "  下载失败  ");
                List images_list = new ArrayList();
                for (Integer image : images) {
                    images_list.add(image);
                }
                initBanner(images_list);
            }
        });

    }

    @Override
    protected void onRestart() {
        super.onRestart();

        g_Character_TX = mBluetoothLeService.getG_Character_TX();
        Log.e(TAG, "life:----onRestart---   " + mBluetoothLeService.getTheConnectedState());
        if (mBluetoothLeService.getTheConnectedState() == 2) {
            myName.setText(deviceName);
            connectedState.setText(R.string.have_been_connected);
            getUserDeviceSetting();
        } else {
            myName.setText("");
            connectedState.setText(R.string.have_been_not_connected);
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
                    Log.e(TAG, "onReceive: MainActivity 收到的数据为：  " + s + "   ");
                    Sys_YiHi_Protocol_RX_Porc(data);

                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    myName.setText("");
                    connectedState.setText(R.string.have_been_not_connected);
                    break;
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    connectedState.setText(R.string.have_been_connected);
                    break;
                case BluetoothLeService.ACTION_LOGIN_FAILED:
                    //删除保存的数据
                    new Delete().from(ConnectedBleDevices.class).execute();
                    mBluetoothLeService.setLastAddress(null);
                    edit.putString("address", null);
                    edit.commit();
                    //提示用户
                    AlertDialog remindDialog = new AlertDialog.Builder(MainActivity.this)
                            .setIcon(R.mipmap.app_icon)
                            .setTitle(R.string.point_out_title)
                            .setMessage(R.string.point_out_information_2)
                            .setCancelable(false)
                            .setNegativeButton(R.string.close_the_tip, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    MainActivity.this.finish();
                                }
                            })
                            .create();
                    remindDialog.show();

                    break;
                case BluetoothLeService.ACTION_LANDING:
                    submitDialog.setMessage(MainActivity.this.getString(R.string.connecting));
                    submitDialog.show();
                    break;
                case BluetoothLeService.ACTION_LAND_SUCCESS:
                    submitDialog.setMessage(MainActivity.this.getString(R.string.connect_successfully));
                    myHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            deviceName = ConnectedBleDevices.getConnectedDevice().deviceName;
                            myName.setText(deviceName);
                            submitDialog.dismiss();
                            submitDialog.setMessage(MainActivity.this.getString(R.string.connecting));
                            Log.e(TAG, "run: mainTest   // 开始询问设备是否处于更换壁纸模式（也就是蓝屏状态）");
                            if (g_Character_TX == null) {
                                g_Character_TX = mBluetoothLeService.getG_Character_TX();
                            }
                            isInWallPaperRequest();
                        }
                    }, 500);

                    break;
                case BluetoothLeService.ACTION_SEND_PROGRESS:

                    if (wallpaperDialogView.isShowing()) {
                        int sendProgress = intent.getIntExtra("sendProgress", 0);
                        wallpaperDialogView.setProgress(sendProgress * 100 / 2304);
                        if (sendProgress == 2304) {
                            wallpaperDialogView.dismiss();
                            mBluetoothLeService.setCanSendPicture(false);
                        }
                    }

                    break;
            }
        }
    };

    private static IntentFilter makeMainBroadcastFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RX);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_LANDING);
        intentFilter.addAction(BluetoothLeService.ACTION_LOGIN_FAILED);
        intentFilter.addAction(BluetoothLeService.ACTION_LAND_SUCCESS);
        intentFilter.addAction(BluetoothLeService.ACTION_SEND_PROGRESS);

        return intentFilter;
    }

    //弹出提示框提示打开蓝牙
    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(mainActivityReceiver, makeMainBroadcastFilter());
        lastAddress = sp.getString("address", null);
        //        if(ConnectedBleDevices.getConnectedDevice()!=null&&mBluetoothLeService!=null&&ConnectedBleDevices.getConnectedDevice().isConnected){
        //            Log.e(TAG, "onStart: "+mBluetoothLeService);
        //            mBluetoothLeService.connect(ConnectedBleDevices.getConnectedDevice().deviceAddress);
        //        }
        if (!mBluetoothAdapter.isEnabled()) {
            //            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothAdapter.enable();
        }

        Log.e(TAG, "handlePasswordCallbacks:---onStart----  adapter  " + mBluetoothAdapter + "   打开蓝牙了：  " + mBluetoothAdapter.isEnabled() + "  service：  " + mBluetoothLeService + "  state " + "  last: " + lastAddress);

        Log.e(TAG, "life: onStart: " + mBluetoothAdapter.isEnabled());
        if (mBluetoothLeService != null && mBluetoothAdapter != null && lastAddress != null && mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "onResume:   state  " + mBluetoothLeService.getTheConnectedState());
            if (mBluetoothLeService.getTheConnectedState() == 0) {
                Log.e(TAG, "doScan:  onStart  -->   后台发起连接 " + mBluetoothAdapter);
                mBluetoothLeService.setDisConnectByMyself(false);
                mBluetoothLeService.serviceScan();
            }
        }

        ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
        if (connectedDevice != null) {
            String deviceName = connectedDevice.deviceName;
            myName.setText(deviceName);
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
        btn_upgrade = (DarkImageButton) findViewById(R.id.btn_upgrade);
        btn_upgrade.setOnClickListener(this);
        myName = (TextView) findViewById(R.id.myName);

    }

    private void initBanner(List<String> images_list) {
        Banner banner = (Banner) findViewById(R.id.head_banner);
        banner.setImageLoader(new GlideImageLoader());

        banner.setImages(images_list);
        banner.setIndicatorGravity(BannerConfig.CENTER);
        banner.start();
        banner.setOnBannerClickListener(new OnBannerClickListener() {
            @Override
            public void OnBannerClick(int position) {
                //                Log.e(TAG, "OnBannerClick: " + position); //position 从1开始
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
                g_Character_Baud_Rate = mBluetoothLeService.getG_Character_Baud_Rate();
                if (g_Character_Baud_Rate != null) {
                    Log.e(TAG, "g_Character_Baud_Rate:  波特率重置");
                    g_Character_Baud_Rate.setValue(intToBytes(5));
                    mBluetoothLeService.writeCharacteristic(g_Character_Baud_Rate);
                }

                break;
            case R.id.btn_upgrade:
                //                goToUpgradeMode();
                //                getAddrRange();

                break;
        }

    }

    private void getUserDeviceSetting() {
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
                //                Toast.makeText(this, R.string.double_kill, Toast.LENGTH_SHORT).show();
                end = System.currentTimeMillis();
            }
        }
        //表示拦截back事件
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "life: ---onDestroy---: ");
        //        unregisterReceiver(mainActivityReceiver);
        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
        if (connectedDevice != null) {
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

        switch (m_Command) {
            case AckUserDeviceSetting:   //查看系统设定的 C

                String s = BinaryToHexString(m_Data);
                String substring = s.substring(12);
                //                Log.e(TAG, "receiveSetting:    s: "+s+"   sub :  "+substring);
                //                int model = Integer.parseInt(substring);
                String selectedModel = null;
                switch (substring) {
                    case "00":
                        selectedModel = "C1";
                        break;
                    case "01":
                        selectedModel = "C2";
                        break;
                    case "02":
                        selectedModel = "C3";
                        break;
                    case "03":
                        selectedModel = "C4";
                        break;
                    case "04":
                        selectedModel = "C5";
                        break;
                }

                //                Log.e(TAG, "receiveSetting:     " + s + "    " + substring + "   m:  "  + "   " + selectedModel);

                List<MyModel> allMyModel = MyModel.getAllMyModel();
                for (MyModel myModel : allMyModel) {
                    if (myModel.model.equals(selectedModel)) {
                        myModel.modelSelected = 1;
                    } else {
                        myModel.modelSelected = 0;
                    }
                    myModel.save();
                }
                break;
            //询问是否蓝屏换壁纸模式
            case 0x6C:
                if (m_Data[5] == 0x16) {
                    //                    Log.e(TAG, " wallpaper: " + BinaryToHexString(m_Data));
                    if (m_Data[6] == 0x01) {
                        //已经处于蓝屏换壁纸模式
                        Log.e(TAG, "sendData: " + "    已经蓝屏  ");

                        wallpaperDialogView.show();
                        new Thread() {
                            @Override
                            public void run() {
                                super.run();

                                pixelSp = getSharedPreferences("pixelData", Context.MODE_PRIVATE);
                                String pixels = pixelSp.getString("pixels", null);

                                String[] split = pixels.split(",");

                                int count = sp.getInt("count", 0);
                               /* for (int j = 0; j < 57600; j++) {

//                                    Log.e(TAG, "pixelData:    " + String.valueOf(pixels) + "   length: " + split.length + "  count: " + count);
                                    Log.e(TAG, "sendData: " + split.length + "  count: " + count+"    j:  "+j);
                                    int getPixel = Integer.parseInt(split[j]);
                                    int r1 = (getPixel & 0x00F80000) >> 8;
                                    int g1 = (getPixel & 0x0000FC00) >> 5;
                                    int b1 = (getPixel & 0x000000F8) >> 3;
                                    int all = r1 | g1 | b1;
                                    byte one = (byte) (all >> 8);
                                    byte two = (byte) all;

                                    int m_dataIndex = j * 2;

                                    pixel_data[m_dataIndex + 1] = one;
                                    pixel_data[m_dataIndex] = two;
                                    if(j==57599){
                                        Log.e(TAG, "sendData: "+"     "+mBluetoothLeService+"   count: "+count);
                                        mBluetoothLeService.setPixel_data(pixel_data);
                                        mBluetoothLeService.setCount(count);
                                        mBluetoothLeService.sendData(count);
                                    }
                                }*/
                                int myIndex = 0;
                                for (int j = 0; j < 240; j++) {
                                    for (int k = 0; k < 240; k++) {

                                        int getPixel = Integer.parseInt(split[myIndex++]);
                                        int r1 = (getPixel & 0x00F80000) >> 8;
                                        int g1 = (getPixel & 0x0000FC00) >> 5;
                                        int b1 = (getPixel & 0x000000F8) >> 3;
                                        int all = r1 | g1 | b1;
                                        byte one = (byte) (all >> 8);
                                        byte two = (byte) all;

                                        int index = j * 240 * 2 + k * 2;
                                        pixel_data[index + 1] = one;
                                        pixel_data[index] = two;

                                        if (j == 239 && k == 239) {
                                            Log.e(TAG, "sendData: >>>>>>  " + myIndex + "  count: " + count + "  service: " + mBluetoothLeService + "    tx: " + mBluetoothLeService.getG_Character_TX());
                                            myIndex = 0;
                                            mBluetoothLeService.setPixel_data(pixel_data);
                                            mBluetoothLeService.setCount(count);
                                            mBluetoothLeService.setCanSendPicture(true);
                                            mBluetoothLeService.sendData(count);
                                        }
                                    }
                                }

                            }
                        }.start();

                        //                        mBluetoothLeService.setCount(count);
                        //                        mBluetoothLeService.sendData(count);
                    }
                }
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "life:-----onStop: ");
        unregisterReceiver(mainActivityReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0x0A) {

            if (isGpsEnable(this)) {
                Log.i("fang", " request location permission success");
                //Android6.0需要动态申请权限
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    //请求权限
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_REQUEST_COARSE_LOCATION);
                }

            } else {
                //若未开启位置信息功能，则退出该应用
                finish();
            }
        }
        //        if (requestCode == REQUEST_ENABLE_BT
        //                && resultCode == Activity.RESULT_CANCELED) {
        //            finish();
        //            return;
        //        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "mainActivity begin :  ---- onResume:       bleService   ---> " + mBluetoothLeService);

    }

    //询问设备是否处于更换壁纸模式
    private void isInWallPaperRequest() {
        byte[] m_Data = new byte[32];
        int m_Length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[2] = 0x04;
        m_Data[3] = 0x01;
        m_Data[4] = 0x6C;
        m_Data[5] = 0x15;
        m_Data[6] = 0x03;

        m_Length = 7;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }


    private void sendData(int num) {
        sendData1(num);
        sendData2(num);
        sendData3(num);
        sendData4(num);
    }

    private void sendData1(int num) {

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] m_Data = new byte[21];
        int m_Length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[2] = 0x3B;
        m_Data[3] = 0x01;
        m_Data[4] = 0x6C;
        m_Data[5] = 0x0F;

        //    序号   M M
        byte[] bytes1 = intToBytes2(num);
        m_Data[6] = bytes1[1];
        m_Data[7] = bytes1[2];
        m_Data[8] = bytes1[3];

        //    本次发送数据包的长度   P P
        byte[] bytes = intToBytes(50);
        m_Data[9] = bytes[0];
        m_Data[10] = bytes[1];

        //有效数据  V V
        for (int i = 0; i < 9; i++) {
            m_Data[11 + i] = pixel_data[i + num * 50];
        }

        //        Log.e(TAG, "sendData1:  "+BinaryToHexString(m_Data));
        m_Length = 20;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);

    }

    private void sendData2(int num) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] m_Data = new byte[32];
        int m_Length = 0;
        for (int i = 0; i < 20; i++) {
            m_Data[i] = pixel_data[i + 9 + num * 50];
        }
        m_Length = 20;

        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

    private void sendData3(int num) {

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] m_Data = new byte[32];
        int m_Length = 0;
        for (int i = 0; i < 20; i++) {
            m_Data[i] = pixel_data[i + 29 + num * 50];
        }
        m_Length = 20;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

    private void sendData4(int num) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] m_Data = new byte[32];
        int m_Length = 0;

        m_Data[0] = pixel_data[num * 50 + 49];
        int sum = 0;
        for (int i = 0; i < 50; i++) {
            int onByte = pixel_data[i + num * 50] & 0xFF;
            sum += onByte;
        }
        //       校验数据---
        Log.e(TAG, "sendData   out:  " + "   校验： " + sum);
        byte yy = (byte) (sum & 0xFF);
        m_Data[1] = yy;
        m_Length = 2;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

    private void getAddrRange() {
        byte[] m_Data_DeviceSetting = new byte[32];
        int m_Length = 0;
        m_Data_DeviceSetting[0] = 0x55;
        m_Data_DeviceSetting[1] = (byte) 0xFF;
        m_Data_DeviceSetting[2] = 0x02;
        m_Data_DeviceSetting[3] = 0x01; //Device ID
        m_Data_DeviceSetting[4] = 0x39;

        m_Length = 5;
        Sys_Proc_Charactor_TX_Send(m_Data_DeviceSetting, m_Length);
    }

    private void goToUpgradeMode() {
        byte[] m_Data_DeviceSetting = new byte[32];
        int m_Length = 0;
        m_Data_DeviceSetting[0] = 0x55;
        m_Data_DeviceSetting[1] = (byte) 0xFF;
        m_Data_DeviceSetting[2] = 0x06;
        m_Data_DeviceSetting[3] = 0x01; //Device ID
        m_Data_DeviceSetting[4] = 0x14;
        m_Data_DeviceSetting[5] = 0x01;
        m_Data_DeviceSetting[6] = 0x00;
        m_Data_DeviceSetting[7] = 0x00;
        m_Data_DeviceSetting[8] = 0x00;

        m_Length = 9;
        Sys_Proc_Charactor_TX_Send(m_Data_DeviceSetting, m_Length);
    }
}
