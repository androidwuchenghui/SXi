package com.yihai.wu.sxi;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import com.yihai.wu.appcontext.ConnectedBleDevices;
import com.yihai.wu.base.BaseActivity;
import com.yihai.wu.util.DarkImageButton;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.yihai.wu.util.MyUtils.BinaryToHexString;
import static com.yihai.wu.util.MyUtils.byteMerger;
import static com.yihai.wu.util.MyUtils.hexStringToString;

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
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic g_Character_TX;
    private boolean get_software_version = false;
    private byte[] software_Version;
    public static final int C_SXi_CR_AskDeviceName = 0x02;
    public static final int C_SXi_CR_AckDevice_ID = 0x04;
    public static final int C_SXi_CR_AckProtocolVersion = 0x13;
    public static final int C_SXi_CR_AckDevCapability = 0x19;
    public static final int C_SXi_CR_AckUserSoftware_Version = 0x42;
    private boolean getId = false;

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
            ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
            if (connectedDevice != null) {
                nameAfter.setText(connectedDevice.realName);
                softAfter.setText(connectedDevice.softVision);
                idAfter.setText(connectedDevice.deviceID);
            }
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        registerReceiver(informationReceiver, makeFilter());
    }

    private IntentFilter makeFilter() {

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RX);
        intentFilter.addAction(BluetoothLeService.ACTION_LAND_SUCCESS);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(informationReceiver);
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            g_Character_TX = mBluetoothLeService.getG_Character_TX();
            if (!mBluetoothLeService.initialize()) {
                Log.e("service", "Unable to initialize Bluetooth");
                finish();
            }
            if (mBluetoothLeService.getTheConnectedState() == 0) {
                connectState.setText(R.string.disconnected);

            } else if (mBluetoothLeService.getTheConnectedState() == 2) {
                connectState.setText(R.string.connected);
                getConnectedDeviceRealName();
            }
            //            g_Character_TX = mBluetoothLeService.getG_Character_TX();
            //            g_Character_DeviceName = mBluetoothLeService.getG_Character_DeviceName();
            //            Log.d("setActivityInService", "onServiceConnected: " + mBluetoothLeService+"  character_TX:  "+g_Character_TX+"    "+deviceName);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("service", "onServiceDisconnected: " + "---------服务断开-------------");
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver informationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {

                case BluetoothLeService.ACTION_LAND_SUCCESS:
                    connectState.setText(R.string.connected);
                    Log.e("log", "onReceive: " + "GATT连接成功*************");
                    startActivity(new Intent(DeviceInformationActivity.this, MainActivity.class));
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    connectState.setText(R.string.no_connect);
                    Log.e("log", "onReceive: " + "GATT未连接********");
                    startActivity(new Intent(DeviceInformationActivity.this, MainActivity.class));
                    break;
                case BluetoothLeService.ACTION_DATA_RX:
                    Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
                    byte[] data = bundle.getByteArray("byteValues");
                    String s = BinaryToHexString(data);
                    Log.d("DeviceInformation", "onReceive: 收到的数据为：  " + s+"    "+get_software_version);

                    if (get_software_version) {
                        if (software_Version == null) {
                            software_Version = data;
                        } else {
                            software_Version = byteMerger(software_Version, data);
                            get_software_version = false;
                            Log.d(TAG, "onReceiveRX: " + BinaryToHexString(software_Version));
                            Sys_YiHi_Protocol_RX_Porc(software_Version);
                            software_Version=null;
                        }
                    } else if(getId){
                        if (software_Version == null) {
                            software_Version = data;
                        } else {
                            software_Version = byteMerger(software_Version, data);
                            getId = false;
                            Log.d(TAG, "onReceiveRX: " + BinaryToHexString(software_Version));
                            Sys_YiHi_Protocol_RX_Porc(software_Version);
                            software_Version=null;
                        }
                    }else {
                        Sys_YiHi_Protocol_RX_Porc(data);
                    }
                    break;
            }
        }
    };

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

    //获得芯片软件 版本
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
        g_Character_TX.setValue(m_MyData);
        mBluetoothLeService.writeCharacteristic(g_Character_TX);
    }

    private void Sys_YiHi_Protocol_RX_Porc(byte[] m_Data) {
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
                ConnectedBleDevices deviceName = ConnectedBleDevices.getConnectedDevice();
                deviceName.realName = realName;
                deviceName.save();
                Log.d(TAG, "DeviceInformation: name:  " + s + "  r: " + realName);
                //                mHandler.postDelayed(new Runnable() {
                //                    @Override
                //                    public void run() {
                nameAfter.setText(realName);

                getConnectedDeviceID();
                getId = true;
                //                    }
                //                }, 50);
                break;
            case C_SXi_CR_AckDevice_ID:

                String AckDevice_ID = BinaryToHexString(m_Data);
                String AckDevice_ID_Behind = AckDevice_ID.substring(10, m_Data.length * 2);
                String realID = hexStringToString(AckDevice_ID_Behind);
                Log.d(TAG, "handleInfo: "+realID);
                idAfter.setText(realID);
                ConnectedBleDevices deviceId = ConnectedBleDevices.getConnectedDevice();
                deviceId.deviceID = realID;
                deviceId.save();

                get_software_version = true;
                getConnectedDeviceSoftVision();

                break;
            case C_SXi_CR_AckUserSoftware_Version:
                String back_Software = BinaryToHexString(m_Data).toString();
                String SoftwareData = back_Software.substring(10);
                String Software_Version = hexStringToString(SoftwareData);
                Log.d(TAG, "handleInfo: "+Software_Version);
                softAfter.setText(Software_Version);
                ConnectedBleDevices deviceSoftV = ConnectedBleDevices.getConnectedDevice();
                deviceSoftV.deviceID = Software_Version;
                deviceSoftV.save();
                break;
        }
    }


}
