package com.yihai.wu.sxi;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.yihai.wu.base.BaseActivity;
import com.yihai.wu.util.DarkImageButton;
import com.yihai.wu.widget.refresh_listview.XupListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by ${Wu} on 2016/12/8.
 */

public class DeviceScanActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener, XupListView.IXListViewListener {
    private BluetoothAdapter mBluetoothAdapter;
    private DarkImageButton btn_back;
    String m_MyDeviceName = "TheDevice";
    //dialog
    private ProgressDialog dialog;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothLeService mBluetoothLeService;
    private XupListView device_lv;
    private Handler mHandler;
    private boolean mScanning;
    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000;
    private final static String TAG = "log";
    private boolean mConnected = false;

    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;


    private String mDeviceAddress;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    /**
     * GATT特性:用于发送数据到BLE设备.
     */
    private BluetoothGattCharacteristic g_Character_TX;
    /**
     * GATT特性:用于接收来自BLE设备的数据.
     */
    private BluetoothGattCharacteristic g_Character_RX;
    private BluetoothGattCharacteristic g_Character_DeviceName;
    private BluetoothGattCharacteristic g_Character_Password;
    private BluetoothGattCharacteristic g_Character_Password_Notify;

    public static final int C_SXi_CR_AckPowerValue = 0x06;
    public static final int C_SXi_CR_SetPowerValue = 0x07;
    public static final int C_SXi_CR_SetPower_StepUp = 0x08;
    public static final int C_SXi_CR_SetPower_StepDown = 0x09;
    public static final int C_SXi_CR_CheckParameter = 0x0B;
    public static final int C_SXi_CR_AckParameter = 0x0C;
    public static final int C_SXi_CR_SetBypass = 0x0D;
    public static final int C_SXi_CR_Test_Transmit_TX = 0x10;
    public static final int C_SXi_CR_Test_Transmit_RX = 0x11;

    //密码
    private String password = "000000000000";
    private String mDeviceName;
    private TextView tv_title;

    @Override
    protected int getContentId() {
        return R.layout.activity_devicescan;
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
        /*创建BluetoothLeService并与之绑定.*/
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        scanLeDevice(true);
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
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.e("log", "onReceive: " + "GATT连接成功*************");

                mConnected = true;

                //                updateConnectionState(R.string.connected);
                //                invalidateOptionsMenu();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;

                                Log.e("log", "onReceive: " + "GATT连接断开********");
                //                updateConnectionState(R.string.disconnected);
                //                invalidateOptionsMenu();
                //                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                // Show all the supported services and characteristics on the user interface.
                //                Log.e("log", "onReceive: " + "发现GATT中的服务********");
                displayGattServices(mBluetoothLeService.getSupportedGattServices());

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String string = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                Log.d(TAG, "onReceive: password_back+++++++++" + string);
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
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
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
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
        mHandler = new Handler();
        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);//设置进度条的样式
        dialog.setMessage("搜索中...");

        btn_back = (DarkImageButton) findViewById(R.id.btn_back);
        btn_back.setOnClickListener(this);
        device_lv = (XupListView) findViewById(R.id.device_lv);
        tv_title = (TextView) findViewById(R.id.scan_title);
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
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position - 1);
        mDeviceAddress = device.getAddress();
        mDeviceName = device.getName();

        mBluetoothLeService.connect(mDeviceAddress);

        Log.d(TAG, "onItemClick: "+ mBluetoothLeService.connect(mDeviceAddress));
        if( mBluetoothLeService.connect(mDeviceAddress)){
            //设置名字
            Sys_SetMyDeviceName();
            //提交密码
            setPassword(password);
        }else {
            Toast.makeText(mBluetoothLeService, "未连接", Toast.LENGTH_SHORT).show();
        }

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
        }, 4500);

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

            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

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
    private void displayGattServices(List<BluetoothGattService> gattServices) {
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
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
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
                /*???????????????????.*/
                if (m_b_Check_TX == true) {
                    if (mBluetoothLeService.g_UUID_Charater_SendData.equals(gattCharacteristic.getUuid())) {
                        Log.e(TAG, "Character TX found");
                        g_Character_TX = gattCharacteristic;
                        m_b_Check_TX = false;
                    }
                }
                /*???????????????????.*/
                if (m_b_Check_RX == true) {
                    if (mBluetoothLeService.g_UUID_Charater_ReadData.equals(gattCharacteristic.getUuid())) {
                        g_Character_RX = gattCharacteristic;
                        m_b_Check_RX = false;
                        m_b_ConfigEnable = true;
                    }
                }
                /*????豸?????????.*/
                if (m_b_Check_DeviceName == true) //?ж??????????豸????????????
                {
                    if (mBluetoothLeService.g_UUID_Charater_DeviceName.equals(gattCharacteristic.getUuid())) {
                        g_Character_DeviceName = gattCharacteristic;//执行成功。。。
                        Log.d(TAG, "displayGattServices: deviceName-----uuid" + g_Character_DeviceName.getUuid());
                        m_b_Check_DeviceName = false;
                    }
                }
                if (m_b_Check_Password == true) {
                    if (mBluetoothLeService.g_UUID_Charater_Password.equals(gattCharacteristic.getUuid())) {
                        g_Character_Password = gattCharacteristic;
                    } else if (mBluetoothLeService.g_UUID_Charater_Password_C2.equals(gattCharacteristic.getUuid())) {
                        g_Character_Password_Notify = gattCharacteristic;
                        m_b_Notify_Password = true;
                    }
                }

            }//for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)-----------
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        } //for (BluetoothGattService gattService : gattServices)------------

        /*
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
        */
        if (m_b_ConfigEnable == true) {
            final int charaProp = g_Character_RX.getProperties();
            Log.e(TAG, "displayGattServices: " + g_Character_RX.getUuid());
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mBluetoothLeService.setCharacteristicNotification(
                        g_Character_RX, true);
            }
            m_b_ConfigEnable = false;
        }

        if (m_b_Notify_Password == true) {
            final int charaProp = g_Character_Password_Notify.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mBluetoothLeService.setCharacteristicNotification(
                        g_Character_Password_Notify, true);
            }
        }

    }//displayGattServices()------------------------END

    private void displayData(String data) {
        if (data != null) {
            tv_title.setText(data);
            /*
            int i;
        	int m_Length;
        	m_Length=data.length();
        	byte[] m_MyData=new byte[m_Length];
        	for (i=0;i<m_Length;i++)
        	{
        		m_MyData[i]=(byte)data.charAt(i);
        	}
        	Sys_DEBUG_InfoDisplay(m_MyData,m_Length);
        	Sys_YiHi_Protocol_RX_Porc(m_MyData);
        	*/
            int i;
            int m_Length;
            byte m_Byte_High, m_Byte_Low;
            int m_Counter;
            m_Length = data.length();
            int[] m_MyData = new int[(m_Length / 3)];
            i = 0;
            m_Counter = 0;
            //Log.e(TAG, "Length="+data);
            while (i < m_Length) {
                m_Byte_High = (byte) data.charAt(i);
                i++;
                m_Byte_Low = (byte) data.charAt(i);
                i++;
                if (m_Counter < m_MyData.length) {
                    m_MyData[m_Counter] = Sys_TwoCharToHex(m_Byte_High, m_Byte_Low);
                    //Log.e(TAG, "#"+m_Counter+"="+m_MyData[m_Counter]);
                    m_Counter++;

                }

                i++;
            }
            //Sys_DEBUG_InfoDisplay(m_MyData,m_Counter);
            //Sys_YiHi_Protocol_RX_Porc(m_MyData);
            Sys_YiHi_Protocol_RX_Porc_int(m_MyData);
        }
    }

    private void Sys_YiHi_Protocol_RX_Porc_int(int[] m_Data) {
        int m_Length = 0;
        int i;
        int m_Index = 0xfe;
        int m_ValidData_Length = 0;
        int m_Command = 0;
        int m_iTemp_x10, m_iTemp_x1;
        //byte[] m_Data=new byte[1024];
        m_Length = m_Data.length;
        //m_Length=m_Byte.length;
        //if (g_b_Use_DEBUG) Log.i(LJB_TAG,"RX proc---m_Length="+m_Length);
        //for (i=0;i<m_Length;i++)
        //{
        //	m_Data[i]=m_Byte[i];
        //}
        if (m_Length < 5) {
            //Sys_DEBUG_InfoDisplay(m_Data,m_Length);
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
        //if (g_b_Use_DEBUG) Log.i(LJB_TAG,"RX proc---m_Length="+m_Length+",m_Index="+m_Index);
        if (m_Index == 0xfe) {
            //Sys_DEBUG_InfoDisplay(m_Data,0);
            return;
        }
        if (m_Index > (m_Length - 2)) {
            //Sys_DEBUG_InfoDisplay(m_Data,m_Length);
            return;
        }
        //Get valid data length.
        m_ValidData_Length = m_Data[(m_Index + 2)];
        if ((m_Index + m_ValidData_Length) > m_Length) {
            //Sys_DEBUG_InfoDisplay(m_Data,m_Length);
            return;
        }
        //Get command code.
        m_Command = m_Data[(m_Index + 4)];
        switch (m_Command) {
            case C_SXi_CR_AckPowerValue:

                //i=m_Data[(m_Index+5)];
                //g_PowerValue_x10=((i/16)*10)+(i%10);
                //i=m_Data[(m_Index+6)];
                //g_PowerValue_x1=((i/16)*10)+(i%10);
                //m_str=String.format("AckPowerValue=%1$d.%2$d W", g_PowerValue_x10,g_PowerValue_x1);
                //g_TextView_DebugInfo.setText(m_str);

                break;
            case C_SXi_CR_AckParameter:

                break;
        }//switch (m_Command)--------
        //Sys_DEBUG_InfoDisplay(m_Data,m_Length);

    }//Sys_YiHi_Protocol_RX_Porc()------------end.

    private int Sys_TwoCharToHex(byte m_Byte_High, byte m_Byte_Low) {
        int m_Result;
        byte m_Temp;
        m_Result = 0;
        m_Temp = Sys_CharToHEX(m_Byte_High);
        if (m_Temp < 16) {
            m_Result = (int) ((m_Temp * 0x10) & 0x00ff);
        }
        m_Temp = Sys_CharToHEX(m_Byte_Low);
        if (m_Temp < 16) {
            m_Result = (int) ((m_Result + m_Temp) & 0x00ff);
        }
        return m_Result;
    }

    private byte Sys_CharToHEX(byte m_Char) {
        byte m_Result;
        m_Result = 0x10;
        if ((m_Char >= '0') && (m_Char <= '9')) {
            m_Result = (byte) (m_Char - '0');
        } else if ((m_Char >= 'A') && (m_Char <= 'F')) {
            m_Result = (byte) (m_Char - 'A' + 10);
        } else if ((m_Char >= 'a') && (m_Char <= 'f')) {
            m_Result = (byte) (m_Char - 'a' + 10);
        }
        return m_Result;
    }

    public void setPassword(String password) {
        byte[] m_Data = password.getBytes();
        if (g_Character_Password != null) {
            g_Character_Password.setValue(m_Data);
            mBluetoothLeService.writeCharacteristic(g_Character_Password);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void Sys_SetMyDeviceName() {
        // Send a message using content of the edit text widget
        int m_Length = 0;


        if (m_MyDeviceName.equals(mDeviceName) == true) {
            return;
        }
        m_Length = m_MyDeviceName.length();
        if (m_Length > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] m_Data = m_MyDeviceName.getBytes();
            //Log.d(TAG, "Send_String=" + m_str);
            //Sys_Proc_Charactor_TX_Send(m_Data,m_Length);
            if (g_Character_DeviceName != null) {
                g_Character_DeviceName.setValue(m_Data);
                mBluetoothLeService.writeCharacteristic(g_Character_DeviceName);

            }
        }

    }
}
