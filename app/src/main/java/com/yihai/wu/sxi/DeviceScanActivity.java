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
    private static final long SCAN_PERIOD = 6000;
    private final static String TAG = "DeviceScanActivity";
    private boolean mConnected = false;
    private byte[] merger_bytes;
    //    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
    //            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    /**
     * resultCode
     * GATT特性:用于发送数据到BLE设备.
     */
    private BluetoothGattCharacteristic g_Character_TX;

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
    private boolean get_software_version = false;
    private boolean wait = false;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isGpsEnable(this) == false) {
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
    private byte[] software_Version;
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
                    //                    Toast.makeText(DeviceScanActivity.this, "不是本公司产品!", Toast.LENGTH_SHORT).show();
                    LandDialog.dismiss();

                    break;
                case BluetoothLeService.ACTION_DATA_RX:
                    Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
                    byte[] data = bundle.getByteArray("byteValues");
                    int counts = 0;
                    if (data.length > 3) {
                        counts = (data[2] & 0xff) + 3;
                    }
/*
                    if (wait) {
                        merger_bytes = byteMerger(merger_bytes, data);
                        Sys_YiHi_Protocol_RX_Porc(merger_bytes);
                        merger_bytes = null;
                        wait = false;
                    }

                    if (data[0] == (byte) 0x55 && data[1] == (byte) 0xFF && counts <= 20) {

                        Sys_YiHi_Protocol_RX_Porc(data);

                    } else if (data[0] == (byte) 0x55 && data[1] == (byte) 0xFF && counts > 20) {
                        merger_bytes = data;
                        wait = true;
                    }*/

                    //                    if(get_software_version){
                    //                        if(software_Version==null) {
                    //                            software_Version = data;
                    //                        }else {
                    //                            software_Version = byteMerger(software_Version,data);
                    //                            get_software_version =false;
                    //                            Log.d(TAG, "onReceiveRX: "+BinaryToHexString(software_Version));
                    //                            Sys_YiHi_Protocol_RX_Porc(software_Version);
                    //                        }
                    //
                    //                    }else {
                    //                        Sys_YiHi_Protocol_RX_Porc(data);
                    //                    }
                    break;

                case BluetoothLeService.ACTION_LAND_SUCCESS:
                    LandDialog.setMessage(DeviceScanActivity.this.getString(R.string.connect_successfully));

                    editor.putString("address", mDeviceAddress);
                    mBluetoothLeService.setLastAddress(mDeviceAddress);
                    editor.commit();
                    g_Character_TX = mBluetoothLeService.getG_Character_TX();
                    //                    Log.d(TAG, "commitPassword Ok: 修改登录成功" + " charac  " + g_Character_TX);

                    if (mDeviceAddress == null) {
                        mDeviceAddress = ConnectedBleDevices.getConnectedDevice().deviceAddress;
                    }

                    ConnectedBleDevices connectedBleDevice = ConnectedBleDevices.getConnectInfoByAddress(mDeviceAddress);
                    if (connectedBleDevice == null) {
                        connectedBleDevice = new ConnectedBleDevices();
                        connectedBleDevice.deviceAddress = mDeviceAddress;
                        connectedBleDevice.save();
                    }


                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (LandDialog.isShowing()) {
                                LandDialog.dismiss();
                            }
                            //                        Log.d(TAG, "connectedState :" + mBluetoothLeService.getTheConnectedState());
                            DeviceScanActivity.this.finish();
                            overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                        }
                    }, 800);

//                    getConnectedDeviceRealName();
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
                            .setTitle(R.string.point_out_title)
                            .setMessage(DeviceScanActivity.this.getString(R.string.point_out_information_2))
                            .setCancelable(false)
                            .setNegativeButton(R.string.close_the_tip, new DialogInterface.OnClickListener() {
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
                            .setTitle(R.string.point_out_title)
                            .setMessage(DeviceScanActivity.this.getString(R.string.point_out_information_1))
                            .setCancelable(false)
                            .setNegativeButton(R.string.close_the_tip, null)
                            .create();
                    remindDialog1.show();
                    break;

            }
        }
    };

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

                            //                            Log.d(TAG, "扫描结果  : " + device.getBondState() + "   getuuid： " + device.getUuids() + "   address： " + device.getAddress() + ">>>>>" + scanRecord.toString() + "name: " + device.getName());
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
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
            dialog.show();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                }
            }, 3000);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
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
        dialog.setMessage(DeviceScanActivity.this.getString(R.string.searching));

        LandDialog = new ProgressDialog(this);
        LandDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        LandDialog.setMessage(DeviceScanActivity.this.getString(R.string.connecting));

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


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanLeDevice(false);
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

       /* switch (m_Command) {
            case C_SXi_CR_AskDeviceName:
                String s = BinaryToHexString(m_Data);
//                Log.d(TAG, "scanReceive: " + s + "   data_length " + m_Data.length);
                String usefulData = s.substring(10, m_Data.length * 2);
                String realName = hexStringToString(usefulData);
                ConnectedBleDevices connectedBleDevice = ConnectedBleDevices.getConnectInfoByAddress(mDeviceAddress);
                connectedBleDevice.realName = realName;
                connectedBleDevice.save();
//                Log.d(TAG, "Sys_YiHi_Protocol_RX_Porc: name:  " + s + "  r: " + realName);
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
//                Log.d(TAG, "Sys_YiHi_Protocol_RX_Porc: id:   " + AckDevice_ID + "  r: " + realID);
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
//                Log.d(TAG, "Sys_YiHi_Protocol_RX_Porc:   Protocol:  " + Protocol_Vision + "  b: " + Protocol_Vision_Behind + "   r:  " + realVsion);
                //                mHandler.postDelayed(new Runnable() {
                //                    @Override
                //                    public void run() {

                //                ConnectedBleDevices connectedDevice_sv = ConnectedBleDevices.getConnectedDevice();
                //                connectedDevice_sv.softVision = realVsion;
                //                connectedDevice_sv.save();
                //    ---询问主机支持哪些功能
                //                getConnectedDeviceCapability();
                get_software_version = true;
                getConnectedDeviceSoftVision();

                //                    }
                //                }, 50);
                break;
          *//*  case C_SXi_CR_AckDevCapability:
                String Protocol_Capability = BinaryToHexString(m_Data);
                byte b = m_Data[m_Index + 6];
                //转成2进制
                String tString = Integer.toBinaryString((b & 0xFF) + 0x100).substring(1);
                char c = tString.charAt(1);
                Log.d(TAG, ": cap :   第6位的二进制： " + tString + "      收到的16进制数据：  " + Protocol_Capability + "   ");
//                if (tString.substring(1, 2).equals(1 + "")) {

                //  获得芯片版本
                    getConnectedDeviceSoftVision();
                    get_software_version = true;
//                }
                break;*//*
            case C_SXi_CR_AckUserSoftware_Version:

                String back_Software = BinaryToHexString(m_Data).toString();
                String SoftwareData = back_Software.substring(10);
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
                        if (LandDialog.isShowing()) {
                            LandDialog.dismiss();
                        }
                        //                        Log.d(TAG, "connectedState :" + mBluetoothLeService.getTheConnectedState());
                        DeviceScanActivity.this.finish();
                    }
                }, 600);
                break;
        }*/
    }



}
