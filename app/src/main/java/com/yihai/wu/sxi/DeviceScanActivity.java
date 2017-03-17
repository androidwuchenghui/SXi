package com.yihai.wu.sxi;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.activeandroid.query.Delete;
import com.yihai.wu.appcontext.ConnectedBleDevices;
import com.yihai.wu.base.BaseActivity;
import com.yihai.wu.util.DarkImageButton;
import com.yihai.wu.widget.refresh_listview.XupListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static com.yihai.wu.util.MyUtils.BinaryToHexString;
import static com.yihai.wu.util.MyUtils.hexStringToString;
import static com.yihai.wu.util.MyUtils.isGpsEnable;

/**
 * Created by ${Wu} on 2016/12/8.
 */

public class DeviceScanActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener, XupListView.IXListViewListener {
    //share
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    private BluetoothAdapter mBluetoothAdapter;
    private DarkImageButton btn_back;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 0x09;
    //dialog
    private ProgressDialog dialog;

    private ProgressDialog LandDialog;
    //    private ProgressDialog successDialog;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothLeService mBluetoothLeService;
    private XupListView device_lv;
    private Handler mHandler;
    private boolean mScanning;
    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000;
    private final static String TAG = "DeviceScanActivity";
    private boolean mConnected = false;

    //    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
    //            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    /**
     * resultCode
     * GATT特性:用于发送数据到BLE设备.
     */
    private BluetoothGattCharacteristic g_Character_TX;
    /**
     * GATT特性:用于接收来自BLE设备的数据.
     */
    //    private BluetoothGattCharacteristic g_Character_RX;
    //    private BluetoothGattCharacteristic g_Character_DeviceName;
    //    private BluetoothGattCharacteristic g_Character_CustomerID;
    //
    //    private BluetoothGattCharacteristic g_Character_Password;
    //    private BluetoothGattCharacteristic g_Character_Password_Notify;

    //    public static final int C_SXi_CR_AckPowerValue = 0x06;
    //    public static final int C_SXi_CR_SetPowerValue = 0x07;
    //    public static final int C_SXi_CR_SetPower_StepUp = 0x08;
    //    public static final int C_SXi_CR_SetPower_StepDown = 0x09;
    //    public static final int C_SXi_CR_CheckParameter = 0x0B;
    //    public static final int C_SXi_CR_AckParameter = 0x0C;
    //    public static final int C_SXi_CR_SetBypass = 0x0D;
    //    public static final int C_SXi_CR_Test_Transmit_RX = 0x11;
    //    //-----------
    //    public static final int C_SXi_CR_GetDeviceName = 0x01;
    public static final int C_SXi_CR_AskDeviceName = 0x02;
    public static final int C_SXi_CR_AckDevice_ID = 0x04;
    public static final int C_SXi_CR_AckProtocolVersion = 0x13;
    public static final int C_SXi_CR_AckDevCapability = 0x19;
    public static final int C_SXi_CR_AckUserSoftware_Version = 0x42;

    //密码
    private static final String DEFAULT_PASSWORD = "000000";
    private static final String YIHI_DEFAULT_PASSWORD = "135246";
    private int commit_amount = 100;
    private int reChange = 0;
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean step3 = false;
    private boolean step5 = false;
    //    private TextView tv_title;
    //    private Message message;
    private StringBuilder sb;
    private String changeTo;
    private BluetoothDevice device;
    //    private ScanCallback mScanCallback;

    //    private static final int REQUEST_FINE_LOCATION = 0;

    //    private void mayRequestLocation() {
    //        if (Build.VERSION.SDK_INT >= 23) {
    //            int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
    //            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
    //                //判断是否需要 向用户解释，为什么要申请该权限
    //                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
    //                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_FINE_LOCATION);
    //                return;
    //            } else {
    //
    //            }
    //        } else {
    //
    //        }
    //    }

    @Override
    protected int getContentId() {
        return R.layout.activity_devicescan;
    }

    @Override
    public boolean isOpenStatus() {
        return false;
    }

    @Override
    protected void init() {
        Log.d(TAG, "init: " + "初始化");

        initView();


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
            finish();
            return;
        }

        //--------
        Log.d(TAG, "permission6.0: adapter :" + mBluetoothAdapter.enable() + "   gps:  " + isGpsEnable(this) + "  -sdk:  " + Build.VERSION.SDK_INT);
        //android6.0动态权限

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isGpsEnable(this)) {
            scanLeDevice(true);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isGpsEnable(this) == false) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            this.startActivityForResult(intent, 0x0A);
        }
        scanLeDevice(true);

        sharedPreferences = getSharedPreferences("lastConnected", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                Log.d(TAG, "permission6.0:   result " + permissions);
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted!
                    scanLeDevice(true);
                }
                return;
            }
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "allBroadcast : " + action);
            switch (action) {
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    mConnected = true;
                    Log.e("DeviceScanConnect", "onReceive: " + "GATT连接成功");

                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    mConnected = false;
                    Log.e("DeviceScanConnect", "onReceive: " + "GATT连接断开********");
                    break;
                //                case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                //                    Log.e("discoveryService", "onReceive: " + "发现GATT中的服务********" + "  count:  " + mBluetoothLeService.getSupportedGattServices().size());
                //                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                //                    break;
                case BluetoothLeService.ACTION_DATA_AVAILABLE:
                    String string = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                    Log.d(TAG, "onReceive: 收到的一些信息：" + string);

                    break;
                case BluetoothLeService.ACTION_NOT_BELONG://产品识别码的返回  FF96
                    Toast.makeText(DeviceScanActivity.this, "不是本公司产品!", Toast.LENGTH_SHORT).show();
                    LandDialog.dismiss();

                    break;
                case BluetoothLeService.ACTION_DATA_RX:
                    Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
                    byte[] data = bundle.getByteArray("byteValues");

                    String s = BinaryToHexString(data);
                    Log.d(TAG, "onReceiveRX: " + s);
                    Sys_YiHi_Protocol_RX_Porc(data);

                    break;

                case BluetoothLeService.ACTION_LAND_SUCCESS:
                    LandDialog.setMessage("登录成功~");

                    editor.putString("address", mDeviceAddress);
                    mBluetoothLeService.setLastAddress(mDeviceAddress);
                    editor.commit();
                    g_Character_TX = mBluetoothLeService.getG_Character_TX();
                    Log.d(TAG, "commitPassword Ok: 修改登录成功" + " charac  " + g_Character_TX);
                    getConnectedDeviceRealName();
                    break;
                case BluetoothLeService.ACTION_LOGIN_FAILED:

                    LandDialog.dismiss();
                    //删除保存的数据
                    new Delete().from(ConnectedBleDevices.class).execute();
                    mBluetoothLeService.setLastAddress(null);
                    step5 = false;
                    //提示用户
                    AlertDialog remindDialog = new AlertDialog.Builder(DeviceScanActivity.this)
                            .setIcon(R.mipmap.app_icon)
                            .setTitle("提示")
                            .setMessage("您的连接失败，请尝试以下操作\n1.退出本程序\n2.手动操作设备A,进入\"设备配对\"菜单\n3.长按ENTER键,直到设备A显示蓝牙配对画面\n4.重新启动本程序,重新搜索,并点击连接搜索到的设备A,来完成配对.")
                            .setCancelable(false)
                            .setNegativeButton("关闭提示", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    DeviceScanActivity.this.finish();
                                }
                            })
                            .create();
                    remindDialog.show();
                    break;
                case BluetoothLeService.ACTION_THREE_SUBMISSION_FAILED:
                    Log.d(TAG, "handleChangePassword: 提交3次后失败，结束");

                    //登录失败(流程结束)
                    LandDialog.dismiss();
                    reChange = 0;
                    AlertDialog remindDialog1 = new AlertDialog.Builder(DeviceScanActivity.this)
                            .setIcon(R.mipmap.app_icon)
                            .setTitle("提示")
                            .setMessage("登录失败！\n" + "这个设备可能不是我们的产品,\n" +
                                    "也可能蓝牙模块的密码修改功能\n" +
                                    "已经出现异常")
                            .setCancelable(false)
                            .setNegativeButton("关闭提示", null)
                            .create();
                    remindDialog1.show();
                    break;

            }
        }
    };


    //修改密码的处理
    //    private void handleChangePassword(String str) {
    //        switch (str) {
    //            case "01":
    //                //提交错误，修改失败
    //                Log.d(TAG, "handleChangePassword:修改失败 " + reChange);
    //                if (reChange == 3) {
    //                    Log.d(TAG, "handleChangePassword: 提交3次后失败，结束");
    //                    mBluetoothLeService.disconnect();
    //                    //登录失败(流程结束)
    //                    LandDialog.dismiss();
    //                    reChange = 0;
    //                    AlertDialog remindDialog = new AlertDialog.Builder(this)
    //                            .setIcon(R.mipmap.app_icon)
    //                            .setTitle("提示")
    //                            .setMessage("登录失败！\n" + "这个设备可能不是我们的产品,\n" +
    //                                    "也可能蓝牙模块的密码修改功能\n" +
    //                                    "已经出现异常")
    //                            .setCancelable(false)
    //                            .setNegativeButton("关闭提示", null)
    //                            .create();
    //                    remindDialog.show();
    //
    //                } else {
    //                    reChange++;
    //                    commit_amount = 2;
    //                    commitPassword(changeTo);
    //                }
    //                break;
    //            case "02":
    //                //修改成功,保存密码
    //                ConnectedBleDevices changedPassword = ConnectedBleDevices.getConnectInfoByAddress(mDeviceAddress);
    //                String lastPassword = sb.toString();
    //                changedPassword.password = lastPassword;
    //                changedPassword.isConnected = true;
    //                changedPassword.isFirst = false;
    //                changedPassword.save();
    //
    //                commit_amount = 3;
    //                step3 = false;
    //                step5 = false;
    //                commitPassword(lastPassword + lastPassword);
    //                //连接成功，执行正常通信￥￥ （流程OK）
    //                Log.d(TAG, "handleChangePassword: 修改密码并保存     正常连接。。。");
    //                //                getConnectedDeviceRealName();
    //                //                LandDialog.cancel();
    //                //                successDialog.show();
    //
    //                break;
    //        }
    //
    //    }

    //再次提交亿海产品默认密码返回结果的处理   5.
   /* private void handleSecondPasswordCallback(String str) {
        Log.d(TAG, "handleSecondPasswordCallback: " + "提交默认产品密码返回处理");
        ConnectedBleDevices devices = ConnectedBleDevices.getConnectInfoByAddress(mDeviceAddress);
        switch (str) {
            case "00"://正确
                if (devices.password.equals(YIHI_DEFAULT_PASSWORD)) {
                    step5 = true;
                }
                isFirst();

                break;
            case "01"://连接失败，删除数据，断开连接(流程结束)
                LandDialog.dismiss();
                //删除保存的数据
                new Delete().from(ConnectedBleDevices.class).execute();
                step5 = false;
                mBluetoothLeService.disconnect();
                //提示用户
                AlertDialog remindDialog = new AlertDialog.Builder(this)
                        .setIcon(R.mipmap.app_icon)
                        .setTitle("提示")
                        .setMessage("您的连接失败，请尝试以下操作\n1.退出本程序\n2.手动操作设备A,进入\"设备配对\"菜单\n3.长按ENTER键,直到设备A显示蓝牙配对画面\n4.重新启动本程序,重新搜索,并点击连接搜索到的设备A,来完成配对.")
                        .setCancelable(false)
                        .setNegativeButton("关闭提示", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                DeviceScanActivity.this.finish();
                            }
                        })
                        .create();
                remindDialog.show();

                break;
        }
    }

    //提交密码返回结果处理   3.
    private void handlePasswordCallbacks(String reply) {
        ConnectedBleDevices devices = ConnectedBleDevices.getConnectInfoByAddress(mDeviceAddress);
        Log.d(TAG, "handlePasswordCallbacks: " + "处理返回的密码状态:" + reply);
        if (reply != null) {
            //提交密码后判断回馈的信息
            switch (reply) {
                case "00":
                    Log.d(TAG, "handlePasswordCallbacks: 密码提交---正确---");
                    if (devices.password.equals(DEFAULT_PASSWORD)) {
                        step3 = true;
                    }
                    //判断是不是第一次连接
                    isFirst();

                    break;
                case "01":
                    Log.d(TAG, "displayData: 默认设备密码提交错误--- 开始提交产品密码");
                    step3 = false;
                    commit_amount = 1;
                    devices.password = YIHI_DEFAULT_PASSWORD;
                    devices.save();
                    commitPassword(YIHI_DEFAULT_PASSWORD + YIHI_DEFAULT_PASSWORD);

                    break;

            }

        }
    }
*/
    //首次连接的判断
   /* private void isFirst() {

        ConnectedBleDevices theDevice = ConnectedBleDevices.getConnectInfoByAddress(mDeviceAddress);
        Log.d(TAG, "passWordisFirst: " + theDevice.isFirst + "***");
        boolean isFirstRun = theDevice.isFirst;
        if (isFirstRun || (step3 || step5) && commit_amount != 3) {
            //      获取识别码
            Log.d(TAG, " passWord    是第一次登陆 获取识别码: ");
            mBluetoothLeService.readCharacteristic(g_Character_CustomerID);//读取产品识别码

        } else {
            //通过用户自己保存的密码，并且不是第一次进入，允许执行正常通信￥￥（流程OK）
            Log.d(TAG, "handleChangePassword: 用户通过保存的密码进入      over");

            mBluetoothLeService.setBluetoothDevice(device);
        }

    }*/


    // Code to manage Service lifecycle.
    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "onServiceConnected: **********************连接服务");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            //              mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected: " + "---------服务未连接-------------");
            mBluetoothLeService = null;
        }
    };
    // Device scan callback. 扫描回掉
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Log.d(TAG, "扫描结果  : " + device.getBondState() + "   getuuid： " + device.getUuids() + "   address： " + device.getAddress() + ">>>>>" + scanRecord.toString() + "name: " + device.getName());
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    //扫描方法
    private void scanLeDevice(final boolean enable) {
        mLeDeviceListAdapter.clear();
        if (enable) {

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    mScanning = false;

                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
            dialog.show();
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            dialog.dismiss();
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }


    private void initView() {
         /*创建BluetoothLeService并与之绑定.*/
        Intent gattServiceIntent = new Intent(DeviceScanActivity.this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        //注册广播
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        mHandler = new Handler();
        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);//设置进度条的样式
        dialog.setMessage("搜索中...");

        LandDialog = new ProgressDialog(this);
        LandDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        LandDialog.setMessage("登录中~");

        Log.d(TAG, "bbbbbb: 初始化: initView ");

        //        successDialog = new ProgressDialog(this);
        //        successDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        //        successDialog.setMessage("登录成功~");

        btn_back = (DarkImageButton) findViewById(R.id.btn_back);
        btn_back.setOnClickListener(this);
        device_lv = (XupListView) findViewById(R.id.device_lv);
        //        tv_title = (TextView) findViewById(R.id.scan_title);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        device_lv.setAdapter(mLeDeviceListAdapter);
        device_lv.setOnItemClickListener(this);
        device_lv.setPullRefreshEnable(true);
        device_lv.setPullLoadEnable(true);
        device_lv.setAutoLoadEnable(true);
        device_lv.setXListViewListener(this);
        device_lv.setRefreshTime(getTime());
        device_lv.setOnItemClickListener(this);

    }

    private String getTime() {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date());
    }

    //点击事件
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                finish();
                break;
            default:
                break;
        }
    }


    //设备选择列表的点击事件
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        device = mLeDeviceListAdapter.getDevice(position - 1);
        mBluetoothLeService.setBluetoothDevice(device);
        mDeviceAddress = device.getAddress();
        mDeviceName = device.getName();
        mBluetoothLeService.setmDeviceName(mDeviceName);
        Log.d(TAG, "onItemClick: " + "address: " + mDeviceAddress + "  service：  " + mBluetoothLeService);
        //点击了一个想要连接的设备

        mBluetoothLeService.connect(mDeviceAddress);
        LandDialog.show();
/*
        ConnectedBleDevices usedDevice = ConnectedBleDevices.getConnectInfo(mDeviceName);
        Log.d(TAG, "onItemClick:usedDevice "+usedDevice);
        //判断之前是否登录过
        if(usedDevice!=null){
            String password = usedDevice.password;
        }else {
            commitPassword(password);
        }*/

        //        Sys_SetMyDeviceName("BleDevice");
    }

    //下拉刷新
    @Override
    public void onRefresh() {
        scanLeDevice(true);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                onLoad();
            }
        }, 5000);

    }

    @Override
    public void onLoadMore() {

    }

    private void onLoad() {
        device_lv.stopRefresh();
        device_lv.stopLoadMore();
        device_lv.setRefreshTime(getTime());
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();

            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
                viewHolder.deviceAddress.setText(device.getAddress());
            } else {
                viewHolder.deviceName.setText(R.string.unknown_device);
                viewHolder.deviceAddress.setText(device.getAddress());
            }
            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_DATA_TX_OK);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_COMMIT_PASSWORD_RESULT);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RX);
        intentFilter.addAction(BluetoothLeService.ACTION_LAND_SUCCESS);
        intentFilter.addAction(BluetoothLeService.ACTION_LOGIN_FAILED);
        intentFilter.addAction(BluetoothLeService.ACTION_NOT_BELONG);
        intentFilter.addAction(BluetoothLeService.ACTION_THREE_SUBMISSION_FAILED);

        return intentFilter;
    }

    /*======================================================================
     *Purpose:列出已经连接到本机的BLE设备的所有服务,以及每个服务中的所有特性.
     *Parameter:
     *Return:
     *Remark:
     *======================================================================
     */
    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
   /* private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid = null;
        boolean m_b_Check_TX = false;
        boolean m_b_Check_RX = false;
        boolean m_b_ConfigEnable = false;
        boolean m_b_Check_DeviceName = false;
        boolean m_b_Check_Password = false;
        boolean m_b_Notify_Password = false;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        //        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            if (mBluetoothLeService.g_UUID_Service_SendData.equals(gattService.getUuid())) {
                Log.e(TAG, "Service TX found");
                m_b_Check_TX = true;
            } else {
                m_b_Check_TX = false;
            }
            if (mBluetoothLeService.g_UUID_Service_ReadData.equals(gattService.getUuid())) {
                m_b_Check_RX = true;
            } else {
                m_b_Check_RX = false;
            }
            if (mBluetoothLeService.g_UUID_Service_DeviceConfig.equals(gattService.getUuid())) {
                m_b_Check_DeviceName = true;
            } else {
                m_b_Check_DeviceName = false;
            }

            if (mBluetoothLeService.g_UUID_Service_Password.equals(gattService.getUuid())) {
                m_b_Check_Password = true;
            } else {
                m_b_Check_Password = false;
            }
            //currentServiceData.put(
            //        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(
                    LIST_NAME, MyGattAttributes.lookup(uuid, unknownServiceString));

            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                //currentCharaData.put(
                //        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(
                        LIST_NAME, MyGattAttributes.lookup(uuid, unknownCharaString));

                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
                *//*???????????????????.*//*
                if (m_b_Check_TX == true) {
                    if (mBluetoothLeService.g_UUID_Charater_SendData.equals(gattCharacteristic.getUuid())) {
                        Log.e(TAG, "Character TX found");
                        g_Character_TX = gattCharacteristic;
                        m_b_Check_TX = false;
                    }
                }
                *//*???????????????????.*//*
                if (m_b_Check_RX == true) {
                    if (mBluetoothLeService.g_UUID_Charater_ReadData.equals(gattCharacteristic.getUuid())) {
                        g_Character_RX = gattCharacteristic;
                        m_b_Check_RX = false;
                        m_b_ConfigEnable = true;
                    }
                }
                *//*????豸?????????.*//*
                if (m_b_Check_DeviceName == true) //?ж??????????豸????????????
                {
                    if (mBluetoothLeService.g_UUID_Charater_DeviceName.equals(gattCharacteristic.getUuid())) {
                        g_Character_DeviceName = gattCharacteristic;//执行功。获得FF91特征。。
                        Log.d(TAG, "displayGattServices: 设备名称----" + g_Character_DeviceName.getUuid());
                        //                        Sys_SetMyDeviceName("BleDevice");
                    } else if (mBluetoothLeService.g_UUID_Charater_CustomerID.equals(gattCharacteristic.getUuid())) {
                        g_Character_CustomerID = gattCharacteristic;
                        Log.d(TAG, "displayGattServices: 产品识别码：  " + g_Character_CustomerID.getUuid());
                    }

                }
                if (m_b_Check_Password == true) {
                    if (mBluetoothLeService.g_UUID_Charater_Password.equals(gattCharacteristic.getUuid())) {
                        g_Character_Password = gattCharacteristic;
                    } else if (mBluetoothLeService.g_UUID_Charater_Password_C2.equals(gattCharacteristic.getUuid())) {
                        g_Character_Password_Notify = gattCharacteristic;
                    }

                }

            }//for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)-----------
            //            mGattCharacteristics.add(charas);
            //            gattCharacteristicData.add(gattCharacteristicGroupData);
        } //for (BluetoothGattService gattService : gattServices)------------

        *//*
        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
        *//*
        if (m_b_ConfigEnable == true) {
            final int charaProp = g_Character_RX.getProperties();
            Log.e(TAG, "displayGattServices:  RX " + g_Character_RX.getUuid());
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mBluetoothLeService.setCharacteristicNotification(
                        g_Character_RX, true);
            }
        }

        if (g_Character_Password_Notify != null) {
            final int charaProp = g_Character_Password_Notify.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mBluetoothLeService.setCharacteristicNotification(
                        g_Character_Password_Notify, true);
            }
        }

        //提交密码
        ConnectedBleDevices usedDevice = ConnectedBleDevices.getConnectInfoByAddress(mDeviceAddress);

        //判断之前是否记录过设备A的密码
        if (usedDevice != null) {
            String password = usedDevice.password;
            Log.d(TAG, "password: 提交保存的密码:  " + password);
            commit_amount = 0;
            commitPassword(password + password);

        } else {
            Log.d(TAG, "password: 提交默认密码---  000000");
            commit_amount = 0;
            ConnectedBleDevices current = new ConnectedBleDevices();
            current.deviceName = mDeviceName;
            current.deviceAddress = mDeviceAddress;
            current.password = DEFAULT_PASSWORD;
            current.save();
            Log.d(TAG, "displayGattServices: " + ConnectedBleDevices.getConnectInfoByAddress(mDeviceAddress).deviceName);
            commitPassword(DEFAULT_PASSWORD + DEFAULT_PASSWORD);
        }
    }*///displayGattServices()------------------------END


  /*  public void commitPassword(String password) {
        Log.d(TAG, "password: 提交密码： 特征值为：   " + g_Character_Password + "   密码为：  " + password);
        byte[] m_Data = password.getBytes();
        if (g_Character_Password != null) {
            g_Character_Password.setValue(m_Data);
            mBluetoothLeService.writeCharacteristic(g_Character_Password);
        }
    }*/

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy DeviceScanActivity: " + "--解绑service--");
        unbindService(mServiceConnection);
        unregisterReceiver(mGattUpdateReceiver);
        mBluetoothLeService = null;
    }


    //TX传值
    //获得产品名称
    private void getConnectedDeviceRealName() {
        Log.d(TAG, "getConnectedDeviceRealName: " + " 获得产品名称");
        byte[] m_Data_GetDeviceName = new byte[32];
        //GetDeviceName
        int m_NameLength = 0;
        m_Data_GetDeviceName[0] = 0x55;
        m_Data_GetDeviceName[1] = (byte) 0xFF;
        m_Data_GetDeviceName[3] = 0x01; //Device ID
        m_Data_GetDeviceName[2] = 0x02;
        m_Data_GetDeviceName[4] = 0x01;
        m_NameLength = 5;
        Sys_Proc_Charactor_TX_Send(m_Data_GetDeviceName, m_NameLength);

    }

    //获得id
    private void getConnectedDeviceID() {
        byte[] m_Data_GetDeviceID = new byte[32];
        int m_IDLength = 0;
        m_Data_GetDeviceID[0] = 0x55;
        m_Data_GetDeviceID[1] = (byte) 0xFF;
        m_Data_GetDeviceID[3] = 0x01; //Device ID
        m_Data_GetDeviceID[2] = 0x02;
        m_Data_GetDeviceID[4] = 0x03;
        m_IDLength = 5;
        Sys_Proc_Charactor_TX_Send(m_Data_GetDeviceID, m_IDLength);
    }

    //获得SXi版本
    private void getConnectedDeviceProtocol_Version() {
        byte[] m_Data_GetProtocol_Version = new byte[32];
        int m_VersionLength = 0;
        m_Data_GetProtocol_Version[0] = 0x55;
        m_Data_GetProtocol_Version[1] = (byte) 0xFF;
        m_Data_GetProtocol_Version[3] = 0x01; //Device ID
        m_Data_GetProtocol_Version[2] = 0x02;
        m_Data_GetProtocol_Version[4] = 0x12;
        m_VersionLength = 5;
        Sys_Proc_Charactor_TX_Send(m_Data_GetProtocol_Version, m_VersionLength);
    }

    //获得设备特征
    private void getConnectedDeviceCapability() {
        byte[] m_Data_GetDevCapability = new byte[32];
        int m_CapLength = 0;
        m_Data_GetDevCapability[0] = 0x55;
        m_Data_GetDevCapability[1] = (byte) 0xFF;
        m_Data_GetDevCapability[3] = 0x01; //Device ID
        m_Data_GetDevCapability[2] = 0x02;
        m_Data_GetDevCapability[4] = 0x18;
        m_CapLength = 5;
        Sys_Proc_Charactor_TX_Send(m_Data_GetDevCapability, m_CapLength);
    }

    private void getConnectedDeviceSoftVision() {
        byte[] m_Data_GetDevSoftVision = new byte[32];
        int m_SoftVision = 0;
        m_Data_GetDevSoftVision[0] = 0x55;
        m_Data_GetDevSoftVision[1] = (byte) 0xFF;
        m_Data_GetDevSoftVision[3] = 0x01; //Device ID
        m_Data_GetDevSoftVision[2] = 0x02;
        m_Data_GetDevSoftVision[4] = 0x41;
        m_SoftVision = 5;
        Sys_Proc_Charactor_TX_Send(m_Data_GetDevSoftVision, m_SoftVision);
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
        /*
        m_MyData[0]=0x55;      //Sync byte#0.
		m_MyData[1]=(byte)0xFF;//Sync byte#1.
		m_MyData[2]=(byte)((m_Length+2)&0xff); //Data Length.
		m_MyData[3]=0x01; //Device ID.
		m_MyData[4]=0x10; //Command.
		for (i=0;i<m_Length;i++)
		{
			if ((i+5)<20)
			{
				m_MyData[i+5]=m_Data[i];
			}
		}
		*/
        g_Character_TX.setValue(m_MyData);
        mBluetoothLeService.writeCharacteristic(g_Character_TX);
    }

    //对返回的byte[]进行处理
    private void
    Sys_YiHi_Protocol_RX_Porc(byte[] m_Data) {
        int m_Length = 0;
        int i;
        int m_Index = 0xfe;
        byte m_ValidData_Length = 0;
        byte m_Command;
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
            case C_SXi_CR_AskDeviceName:
                String s = BinaryToHexString(m_Data);
                String usefulData = s.substring(10, m_Data.length * 2);
                String realName = hexStringToString(usefulData);
                ConnectedBleDevices connectedBleDevice = ConnectedBleDevices.getConnectInfoByAddress(mDeviceAddress);
                connectedBleDevice.realName = realName;
                connectedBleDevice.save();
                Log.d(TAG, "Sys_YiHi_Protocol_RX_Porc: name:  " + s + "  r: " + realName);
                //                mHandler.postDelayed(new Runnable() {
                //                    @Override
                //                    public void run() {
                getConnectedDeviceID();
                //                    }
                //                }, 50);
                break;
            case C_SXi_CR_AckDevice_ID:

                String AckDevice_ID = BinaryToHexString(m_Data);
                String AckDevice_ID_Behind = AckDevice_ID.substring(10, m_Data.length * 2);
                String realID = hexStringToString(AckDevice_ID_Behind);
                Log.d(TAG, "Sys_YiHi_Protocol_RX_Porc: id:   " + AckDevice_ID + "  r: " + realID);
                ConnectedBleDevices deviceID = ConnectedBleDevices.getConnectInfoByAddress(mDeviceAddress);
                deviceID.deviceID = realID;
                deviceID.save();

                //                mHandler.postDelayed(new Runnable() {
                //                    @Override
                //                    public void run() {
                getConnectedDeviceProtocol_Version();
                //                    }
                //                }, 50);
                break;

            case C_SXi_CR_AckProtocolVersion:

                int g_PowerValue_x10 = Sys_BCD_To_HEX(m_Data[(m_Index + 5)]);
                int g_PowerValue_x1 = Sys_BCD_To_HEX(m_Data[(m_Index + 6)]);
                String Protocol_Vision = BinaryToHexString(m_Data);
                String Protocol_Vision_Behind = Protocol_Vision.substring(10, m_Data.length * 2);
                String realVsion = hexStringToString(Protocol_Vision_Behind);
                //                Log.d(TAG, "Sys_YiHi_Protocol_RX_Porc: x10: "+g_PowerValue_x10+"  x1:  "+g_PowerValue_x1);
                Log.d(TAG, "Sys_YiHi_Protocol_RX_Porc:   Protocol:  " + Protocol_Vision + "  b: " + Protocol_Vision_Behind + "   r:  " + realVsion);
                //                mHandler.postDelayed(new Runnable() {
                //                    @Override
                //                    public void run() {
                getConnectedDeviceCapability();
                //                    }
                //                }, 50);
                break;
            case C_SXi_CR_AckDevCapability:
                String Protocol_Capability = BinaryToHexString(m_Data);
                byte b = m_Data[m_Index + 6];
                //转成2进制
                String tString = Integer.toBinaryString((b & 0xFF) + 0x100).substring(1);
                char c = tString.charAt(1);
                Log.d(TAG, ": cap :   " + tString + "    " + Protocol_Capability + "   ");
                if (tString.substring(1, 2).equals(1 + "")) {
                    getConnectedDeviceSoftVision();
                }
                break;
            case C_SXi_CR_AckUserSoftware_Version:

                String back_Software = BinaryToHexString(m_Data);
                String SoftwareData = back_Software.substring(10, m_Data.length * 2);
                String Software_Version = hexStringToString(SoftwareData);

                ConnectedBleDevices deviceSoftVision = ConnectedBleDevices.getConnectInfoByAddress(mDeviceAddress);
                deviceSoftVision.softVision = Software_Version;
                deviceSoftVision.isConnected = true;
                deviceSoftVision.lastConnect = true;
                deviceSoftVision.save();
                Log.d(TAG, "Sys_YiHi_Protocol_RX_Porc: softvision:   " + back_Software + "  vision:  " + Software_Version + "  ---> 数据获取完毕    connectedState:  " + mBluetoothLeService.getTheConnectedState());
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        LandDialog.dismiss();
                        Log.d(TAG, "connectedState :" + mBluetoothLeService.getTheConnectedState());
                        DeviceScanActivity.this.finish();
                    }
                }, 1000);
                break;

        }
    }

    private byte Sys_BCD_To_HEX(byte m_BCD) {
        byte m_Return;
        byte m_Temp;
        m_Return = (byte) ((m_BCD / 16) * 10);
        m_Temp = (byte) (m_BCD & 0x0F);
        //m_Return+=(byte)(m_BCD%10);
        m_Return += (byte) (m_Temp % 10);
        return m_Return;
    }


}
