package com.yihai.wu.sxi;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yihai.wu.appcontext.ConnectedBleDevices;
import com.yihai.wu.appcontext.MyModel;
import com.yihai.wu.util.DarkImageButton;

import java.util.List;

import static com.yihai.wu.util.MyUtils.BinaryToHexString;


/**
 * Created by ${Wu} on 2016/12/12.
 */

public class SetActivity extends AppCompatActivity {
    private DarkImageButton btn_back;
    private EditText et;
    private int[] check_select = {0, 0, 0, 0, 0};

    private ImageView select_c1, select_c2, select_c3, select_c4, select_c5;
    private ImageView detail_c1, detail_c2, detail_c3, detail_c4, detail_c5;
    private LinearLayout line_c1, line_c2, line_c3, line_c4, line_c5;
    private TextView tv_c1, tv_c2, tv_c3, tv_c4, tv_c5, status;
    private final int REQUEST_CODE_1 = 0X001;
    private final int REQUEST_CODE_TO_MAIN = 0X002;

    private Intent intent;
    private Intent toMainIntent;
    int select = 0;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic g_Character_TX;
    private BluetoothGattCharacteristic g_Character_DeviceName;
    private static final String TAG = "SetActivity";
    private String deviceName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);
        initView();
        status = (TextView) findViewById(R.id.connect_state);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        registerReceiver(mainActivityReceiver, makeMainBroadcastFilter());

        MyModel selectedModel = MyModel.getSelectedModel();
        Log.d(TAG, "onCreate: init" + selectedModel);
        if (selectedModel != null) {
            select_control (selectedModel.model) ;
        } else {
            select_control("C1");
        }
    }

    private final BroadcastReceiver mainActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {

                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    status.setText("已连接");
                    Log.e("log", "onReceive: " + "GATT连接成功*************");
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    status.setText("未连接");
                    Log.e("log", "onReceive: " + "GATT未连接********");
                    break;
                case BluetoothLeService.ACTION_DATA_RX:
                    Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
                    byte[] data = bundle.getByteArray("byteValues");
                    String s = BinaryToHexString(data);
                    Log.d("set", "onReceive: 收到的数据为：  " + s);
                    //                    Sys_YiHi_Protocol_RX_Porc(data);

                    break;
            }
        }
    };
    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("service", "Unable to initialize Bluetooth");
                finish();
            }
            Log.d(TAG, "onServiceConnected: "+mBluetoothLeService);
            if (mBluetoothLeService.getTheConnectedState() == 0) {
                status.setText("未连接");
            } else if (mBluetoothLeService.getTheConnectedState() == 2) {
                status.setText("已连接");
            }
            g_Character_TX = mBluetoothLeService.getG_Character_TX();
            g_Character_DeviceName = mBluetoothLeService.getG_Character_DeviceName();
            Log.d("setActivityInService", "onServiceConnected: " + mBluetoothLeService+"  character_TX:  "+g_Character_TX+"    "+deviceName);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("service", "onServiceDisconnected: " + "---------服务断开-------------");
            mBluetoothLeService = null;
        }
    };

    private static IntentFilter makeMainBroadcastFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RX);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(!deviceName.equals(et.getText().toString())){
            ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
            connectedDevice.deviceName = et.getText().toString();
            connectedDevice.save();
        }
        if(g_Character_DeviceName!=null&&!deviceName.equals(et.getText().toString())){
            Sys_SetMyDeviceName(et.getText().toString());
        }
    }

    private void select_control(String modelC) {
        //        share_editor.putInt("select", s);
        //        share_editor.commit();
        int s ;
        for (int i = 0; i < check_select.length; i++) {
            check_select[i] = 0;
        }
        switch (modelC){
            case "C1":
                s=0;
                break;
            case "C2":
                s=1;
                break;
            case "C3":
                s=2;
                break;
            case "C4":
                s=3;
                break;
            case "C5":
                s=4;
                break;
            default:
                s=0;
                break;
        }
        check_select[s] = 1;

        List<MyModel> allMyModel = MyModel.getAllMyModel();
        for (MyModel myModel : allMyModel) {
            if(myModel.model.equals(modelC)){
                myModel.modelSelected=1;
            }else {
                myModel.modelSelected=0;
            }
            myModel.save();
        }
        if (check_select[0] == 1) {
            select_c1.setVisibility(View.VISIBLE);
        } else {
            select_c1.setVisibility(View.INVISIBLE);
        }
        if (check_select[1] == 1) {
            select_c2.setVisibility(View.VISIBLE);
        } else {
            select_c2.setVisibility(View.INVISIBLE);
        }
        if (check_select[2] == 1) {

            select_c3.setVisibility(View.VISIBLE);
        } else {
            select_c3.setVisibility(View.INVISIBLE);
        }
        if (check_select[3] == 1) {
            select_c4.setVisibility(View.VISIBLE);

        } else {
            select_c4.setVisibility(View.INVISIBLE);
        }
        if (check_select[4] == 1) {
            select_c5.setVisibility(View.VISIBLE);
        } else {
            select_c5.setVisibility(View.INVISIBLE);
        }
    }

    private void initView() {

        select_c1 = (ImageView) findViewById(R.id.select_c1);
        select_c2 = (ImageView) findViewById(R.id.select_c2);
        select_c3 = (ImageView) findViewById(R.id.select_c3);
        select_c4 = (ImageView) findViewById(R.id.select_c4);
        select_c5 = (ImageView) findViewById(R.id.select_c5);
        detail_c1 = (ImageView) findViewById(R.id.detail_c1);

        detail_c1.setOnClickListener(new clickEvent());
        detail_c2 = (ImageView) findViewById(R.id.detail_c2);
        detail_c2.setOnClickListener(new clickEvent());
        detail_c3 = (ImageView) findViewById(R.id.detail_c3);
        detail_c3.setOnClickListener(new clickEvent());
        detail_c4 = (ImageView) findViewById(R.id.detail_c4);
        detail_c4.setOnClickListener(new clickEvent());
        detail_c5 = (ImageView) findViewById(R.id.detail_c5);
        detail_c5.setOnClickListener(new clickEvent());

        line_c1 = (LinearLayout) findViewById(R.id.line_c1);
        line_c1.setOnClickListener(new clickEvent());
        line_c2 = (LinearLayout) findViewById(R.id.line_c2);
        line_c2.setOnClickListener(new clickEvent());
        line_c3 = (LinearLayout) findViewById(R.id.line_c3);
        line_c3.setOnClickListener(new clickEvent());
        line_c4 = (LinearLayout) findViewById(R.id.line_c4);
        line_c4.setOnClickListener(new clickEvent());
        line_c5 = (LinearLayout) findViewById(R.id.line_c5);
        line_c5.setOnClickListener(new clickEvent());

        tv_c1 = (TextView) findViewById(R.id.tv_c1);
        tv_c1.setOnClickListener(new clickEvent());
        tv_c2 = (TextView) findViewById(R.id.tv_c2);
        tv_c2.setOnClickListener(new clickEvent());
        tv_c3 = (TextView) findViewById(R.id.tv_c3);
        tv_c3.setOnClickListener(new clickEvent());
        tv_c4 = (TextView) findViewById(R.id.tv_c4);
        tv_c4.setOnClickListener(new clickEvent());
        tv_c5 = (TextView) findViewById(R.id.tv_c5);
        tv_c5.setOnClickListener(new clickEvent());
        btn_back = (DarkImageButton) findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new clickEvent());
        et = (EditText) findViewById(R.id.et);
        ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
        if(connectedDevice!=null) {
            et.setText(connectedDevice.deviceName);
        }
        deviceName = et.getText().toString();
        et.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (MotionEvent.ACTION_DOWN == event.getAction()) {
                    et.setCursorVisible(true);// 再次点击显示光标
                }
                return false;
            }
        });




        //跳转到设置详情页
        intent = new Intent(SetActivity.this, SetDetailsActivity.class);
        toMainIntent = new Intent();
    }

    private class clickEvent implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_back:
                    finish();
                    break;
                case R.id.tv_c1:
                    select_control("C1");
                    if (g_Character_TX != null) {
//                        setSelectedData("C1");
                        setUserDeviceSettingModel((byte) 0x00);
                    }
                    break;
                case R.id.tv_c2:
                    if (g_Character_TX != null) {
//                        setSelectedData("C2");
                        setUserDeviceSettingModel((byte) 0x01);
                    }
                    select_control("C2");
                    break;
                case R.id.tv_c3:
                    if (g_Character_TX != null) {
//                        setSelectedData("C3");
                        setUserDeviceSettingModel((byte) 0x02);
                    }
                    select_control("C3");

                    break;
                case R.id.tv_c4:
                    if (g_Character_TX != null) {
//                        setSelectedData("C4");
                        setUserDeviceSettingModel((byte) 0x03);
                    }
                    select_control("C4");
                    break;
                case R.id.tv_c5:
                    if (g_Character_TX != null) {
//                        setSelectedData("C5");
                        setUserDeviceSettingModel((byte) 0x04);
                    }
                    select_control("C5");
                    break;
                case R.id.detail_c1:
                    if (g_Character_TX != null) {
                        setSelectedData("C1");
                    }
                    select_control("C1");
                    intent.putExtra("detail", "C1");

                    startActivity(intent);
                    break;
                case R.id.detail_c2:
                    if (g_Character_TX != null) {
                        setSelectedData("C2");
                    }
                    select_control("C2");
                    intent.putExtra("detail", "C2");
                    startActivity(intent);
                    break;
                case R.id.detail_c3:
                    if (g_Character_TX != null) {
                        setSelectedData("C3");
                    }
                    select_control("C3");
                    intent.putExtra("detail", "C3");
                    startActivity(intent);
                    break;
                case R.id.detail_c4:
                    if (g_Character_TX != null) {
                        setSelectedData("C4");
                    }
                    select_control("C4");
                    intent.putExtra("detail", "C4");
                    startActivity(intent);
                    break;
                case R.id.detail_c5:
                    if (g_Character_TX != null) {
                        setSelectedData("C5");
                    }
                    select_control("C5");
                    intent.putExtra("detail", "C5");
                    startActivity(intent);
                    break;


            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("setActivityInService", "onDestroy: ");

        unregisterReceiver(mainActivityReceiver);
        unbindService(mServiceConnection);
        mBluetoothLeService = null;

    }

    public void setSelectedData(String str) {
        Log.d(TAG, "setSelectedData: "+str);
        switch (str) {
            case "C1":
                setUserDeviceSettingModel((byte) 0x00);
                break;
            case "C2":
                setUserDeviceSettingModel((byte) 0x01);
                break;
            case "C3":
                setUserDeviceSettingModel((byte) 0x02);
                break;
            case "C4":
                setUserDeviceSettingModel((byte) 0x03);
                break;
            case "C5":
                setUserDeviceSettingModel((byte) 0x04);
                break;

        }

        for (int i = 0; i < 5; i++) {
            String name = "C" + (i + 1);
            MyModel myModel = MyModel.getMyModelForGivenName(name);
            if (myModel.model.equals(str)) {
                myModel.modelSelected = 1;
            } else {
                myModel.modelSelected = 0;
            }
            myModel.save();
        }
    }

    public void setUserDeviceSettingModel(byte b) {

        byte[] m_Data_DeviceSetting = new byte[32];
        int m_Length = 0;
        m_Data_DeviceSetting[0] = 0x55;
        m_Data_DeviceSetting[1] = (byte) 0xFF;
        m_Data_DeviceSetting[3] = 0x01; //Device ID
        m_Data_DeviceSetting[2] = 0x04;
        m_Data_DeviceSetting[4] = 0x59;
        m_Data_DeviceSetting[5] = 0x11;
        m_Data_DeviceSetting[6] = b;
        m_Length = 7;
        Sys_Proc_Charactor_TX_Send(m_Data_DeviceSetting, m_Length);
    }

    private void Sys_Proc_Charactor_TX_Send(byte[] m_Data, int m_Length) {
        byte[] m_MyData = new byte[m_Length];
        for (int i = 0; i < m_Length; i++) {
            m_MyData[i] = m_Data[i];
        }

        if (g_Character_TX == null) {
            Log.e("set", "character TX is null");
            return;
        }

        if (m_Length <= 0) {
            return;
        }
        g_Character_TX.setValue(m_MyData);
        mBluetoothLeService.writeCharacteristic(g_Character_TX);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        g_Character_TX = mBluetoothLeService.getG_Character_TX();
        Log.d(TAG, "onRestart:----MainActivity---   " + mBluetoothLeService.getTheConnectedState());
        if (mBluetoothLeService.getTheConnectedState() == 2) {
            status.setText("已连接设备");

        } else {
            status.setText("未连接设备");
        }
    }

    //修改设备可见名称
    private void Sys_SetMyDeviceName(String m_MyDeviceName) {
        // Send a message using content of the edit text widget
        int m_Length = 0;
        if (m_MyDeviceName.equals(deviceName) == true) {
            return;
        }
        m_Length = m_MyDeviceName.length();
        if (m_Length > 0) {

            byte[] m_Data = m_MyDeviceName.getBytes();
            if (g_Character_DeviceName != null) {
                g_Character_DeviceName.setValue(m_Data);
                mBluetoothLeService.writeCharacteristic(g_Character_DeviceName);
            }
        }
    }
}
