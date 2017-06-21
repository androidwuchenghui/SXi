package com.yihai.wu.sxi;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
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

import com.aigestudio.wheelpicker.WheelPicker;
import com.yihai.wu.appcontext.ConnectedBleDevices;
import com.yihai.wu.appcontext.MyModel;
import com.yihai.wu.util.DarkImageButton;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.VISIBLE;
import static com.yihai.wu.util.MyUtils.BinaryToHexString;
import static com.yihai.wu.util.MyUtils.byteMerger;
import static com.yihai.wu.util.MyUtils.bytes2Int;
import static com.yihai.wu.util.MyUtils.bytesToInt;
import static com.yihai.wu.util.MyUtils.intToBytes;
import static java.lang.Character.getNumericValue;


/**
 * Created by ${Wu} on 2016/12/12.
 */

public class SetActivity extends AppCompatActivity {
    private DarkImageButton btn_back;
    private EditText et;
    private int[] check_select = {0, 0, 0, 0, 0};

    private ImageView select_c1, select_c2, select_c3, select_c4, select_c5;
    private ImageView detail_c1, detail_c2, detail_c3, detail_c4, detail_c5, detail_c6;
    //    private View WheelCurvedPicker;//弹出轮子的整个view
    //
    //    private TextView cancle_wheel, ok_wheel;
    private WheelPicker wheel;

    private List<String> list = new ArrayList<>();

    //    private Button set_wallpaper;
    private LinearLayout line_c1, line_c2, line_c3, line_c4, line_c5, line_set_wallpaper;
    private TextView tv_c1, tv_c2, tv_c3, tv_c4, tv_c5, status, myName;
    private final int REQUEST_CODE_1 = 0X001;
    private final int REQUEST_CODE_TO_MAIN = 0X002;


    private Intent toMainIntent;
    int select = 0;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic g_Character_TX;
    private BluetoothGattCharacteristic g_Character_DeviceName;
    private static final String TAG = "SetActivity";
    private String deviceName;
    private String c1_show;
    private String c2_show;
    private String c3_show;
    private String c4_show;
    private String c5_show;
    private String deviceName1 = "";
    private boolean handleReceiveByte = false;

    private boolean needMerge = false;
    private byte[] merger_bytes;
    private boolean getInit = false;
    private Handler handler;
    private boolean supportPreview = false;

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
            select_control(selectedModel.model);
        } else {
            select_control("C1");
        }
        ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
        if (connectedDevice != null) {
            deviceName1 = connectedDevice.deviceName;
        }
    }

    private final BroadcastReceiver mainActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {

                case BluetoothLeService.ACTION_LAND_SUCCESS:
                    status.setText(R.string.connected);
                    Log.e("log", "onReceive: " + "GATT连接成功*************");
                    startActivity(new Intent(SetActivity.this, MainActivity.class));
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    deviceName1 = "";
                    status.setText(R.string.no_connect);
                    Log.e("log", "onReceive: " + "GATT未连接********");
                    startActivity(new Intent(SetActivity.this, MainActivity.class));
                    break;
                case BluetoothLeService.ACTION_DATA_RX:
                    Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
                    byte[] data = bundle.getByteArray("byteValues");
                    String s = BinaryToHexString(data);
                    Log.d("wallpaper", "onReceive: 收到的数据为：  " + s + "  ---   " + needMerge + "    " + getInit);
                    //                      Sys_YiHi_Protocol_RX_Porc(data);
                    //                    if(data.length>3){
                    //                        if(data[0]==0x55&&data[1]==0xFF&&(data[2]&0xFF)>20){
                    //                            Log.d(TAG, "set:  **********  ");
                    //                            merger_bytes = data;
                    //                            wait = true;
                    //                            return;
                    //                        }
                    //                    }

                    //                    if (wait){
                    //                        merger_bytes = byteMerger(merger_bytes, data);
                    //                        wait=false;
                    //                       Sys_YiHi_Protocol_RX_Porc(merger_bytes);
                    //                    }
                    if (getInit) {
                        getInit = false;
                        merger_bytes = byteMerger(merger_bytes, data);
                        Sys_YiHi_Protocol_RX_Porc(merger_bytes);
                        merger_bytes = null;
                    }

                    if (needMerge) {
                        needMerge = false;
                        merger_bytes = data;
                        getInit = true;
                        Log.d(TAG, "wallpaper: needMerge  " + BinaryToHexString(merger_bytes) + "   " + getInit + "   " + needMerge);
                        break;
                    }

                    if (handleReceiveByte) {
                        Sys_YiHi_Protocol_RX_Porc(data);
                        handleReceiveByte = false;
                    }
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
            g_Character_TX = mBluetoothLeService.getG_Character_TX();
            g_Character_DeviceName = mBluetoothLeService.getG_Character_DeviceName();

            if (mBluetoothLeService.getTheConnectedState() == 0) {
                status.setText(R.string.no_connect);
            } else if (mBluetoothLeService.getTheConnectedState() == 2) {
                myName.setText(deviceName1);
                getConnectedDeviceCapability();
                status.setText(R.string.connected);
            }

            Log.d("setActivityInService", "onServiceConnected: " + mBluetoothLeService + "  character_TX:  " + g_Character_TX + "    " + deviceName);
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
        intentFilter.addAction(BluetoothLeService.ACTION_LAND_SUCCESS);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);

        return intentFilter;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!deviceName.equals(et.getText().toString())) {
            ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
            Log.d(TAG, "onPause: connectedDevice   " + connectedDevice);
            if (connectedDevice != null) {
                connectedDevice.deviceName = et.getText().toString();
                connectedDevice.save();
            }
        }
        if (g_Character_DeviceName != null && !deviceName.equals(et.getText().toString()) && mBluetoothLeService.getTheConnectedState() == 2) {
            String name = et.getText().toString();
            //此处为了解决 6.0.1系统改变设备名后搜不到bug（发现是命名不能低于6位）
            boolean name_Ok = true;
            while (name_Ok) {
                int name_length = name.length();
                if (name_length < 6) {
                    name += " ";
                } else {
                    name_Ok = false;
                }
            }
            Sys_SetMyDeviceName(name);
        }
    }

    private void select_control(String modelC) {
        //        share_editor.putInt("select", s);
        //        share_editor.commit();
        int s;
        for (int i = 0; i < check_select.length; i++) {
            check_select[i] = 0;
        }
        switch (modelC) {
            case "C1":
                s = 0;
                break;
            case "C2":
                s = 1;
                break;
            case "C3":
                s = 2;
                break;
            case "C4":
                s = 3;
                break;
            case "C5":
                s = 4;
                break;
            default:
                s = 0;
                break;
        }
        check_select[s] = 1;

        List<MyModel> allMyModel = MyModel.getAllMyModel();
        for (MyModel myModel : allMyModel) {
            if (myModel.model.equals(modelC)) {
                myModel.modelSelected = 1;
            } else {
                myModel.modelSelected = 0;
            }
            myModel.save();
        }
        if (check_select[0] == 1) {
            select_c1.setVisibility(VISIBLE);
        } else {
            select_c1.setVisibility(View.INVISIBLE);
        }
        if (check_select[1] == 1) {
            select_c2.setVisibility(VISIBLE);
        } else {
            select_c2.setVisibility(View.INVISIBLE);
        }
        if (check_select[2] == 1) {

            select_c3.setVisibility(VISIBLE);
        } else {
            select_c3.setVisibility(View.INVISIBLE);
        }
        if (check_select[3] == 1) {
            select_c4.setVisibility(VISIBLE);

        } else {
            select_c4.setVisibility(View.INVISIBLE);
        }
        if (check_select[4] == 1) {
            select_c5.setVisibility(VISIBLE);
        } else {
            select_c5.setVisibility(View.INVISIBLE);
        }
    }

    private void initView() {
        handler = new Handler();
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
        detail_c6 = (ImageView) findViewById(R.id.detail_c6);
        detail_c6.setOnClickListener(new clickEvent());

        //        WheelCurvedPicker = findViewById(R.id.ll_wheel_curved);
        //        wheel = (WheelPicker) findViewById(R.id.wheel);
        //        cancle_wheel = (TextView) findViewById(cancle_wheel);
        //        ok_wheel = (TextView) findViewById(ok_wheel);

        //        toshowWheel();

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
        line_set_wallpaper = (LinearLayout) findViewById(R.id.line_set_wallpaper);
                        line_set_wallpaper.setVisibility(View.GONE);

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
        myName = (TextView) findViewById(R.id.myName);
        btn_back = (DarkImageButton) findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new clickEvent());
        et = (EditText) findViewById(R.id.et);
        ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
        if (connectedDevice != null) {
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

        toMainIntent = new Intent();
        c1_show = MyModel.getMyModelForGivenName("C1").showName;
        c2_show = MyModel.getMyModelForGivenName("C2").showName;
        c3_show = MyModel.getMyModelForGivenName("C3").showName;
        c4_show = MyModel.getMyModelForGivenName("C4").showName;
        c5_show = MyModel.getMyModelForGivenName("C5").showName;
        tv_c1.setText(c1_show);
        tv_c2.setText(c2_show);
        tv_c3.setText(c3_show);
        tv_c4.setText(c4_show);
        tv_c5.setText(c5_show);

        //        NumberPicker np = (NumberPicker) findViewById(R.id.np);
        //        np.setMaxValue(18);

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
                    Intent toDetail = new Intent(SetActivity.this, SetDetailsActivity.class);
                    if (g_Character_TX != null) {
                        setSelectedData("C1");
                    }
                    select_control("C1");
                    toDetail.putExtra("detail", "C1");

                    startActivity(toDetail);
                    break;
                case R.id.detail_c2:
                    Intent toDetail2 = new Intent(SetActivity.this, SetDetailsActivity.class);
                    if (g_Character_TX != null) {
                        setSelectedData("C2");
                    }
                    select_control("C2");
                    toDetail2.putExtra("detail", "C2");
                    startActivity(toDetail2);
                    break;
                case R.id.detail_c3:
                    Intent toDetail3 = new Intent(SetActivity.this, SetDetailsActivity.class);
                    if (g_Character_TX != null) {
                        setSelectedData("C3");
                    }
                    select_control("C3");
                    toDetail3.putExtra("detail", "C3");
                    startActivity(toDetail3);
                    break;
                case R.id.detail_c4:
                    Intent toDetail4 = new Intent(SetActivity.this, SetDetailsActivity.class);
                    if (g_Character_TX != null) {
                        setSelectedData("C4");
                    }
                    select_control("C4");
                    toDetail4.putExtra("detail", "C4");
                    startActivity(toDetail4);
                    break;
                case R.id.detail_c5:
                    Intent toDetail5 = new Intent(SetActivity.this, SetDetailsActivity.class);
                    if (g_Character_TX != null) {
                        setSelectedData("C5");
                    }
                    select_control("C5");
                    toDetail5.putExtra("detail", "C5");
                    startActivity(toDetail5);
                    break;
                case R.id.detail_c6:
                    //                    WheelCurvedPicker.setVisibility(View.VISIBLE);
                    Intent setWallpaperIntent = new Intent(SetActivity.this,SetWallpaperActivity.class);
                    setWallpaperIntent.putExtra("support",supportPreview);

                    startActivity(setWallpaperIntent);
                    //                    getWallPaperRequest();
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
        Log.d(TAG, "setSelectedData: " + str);
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
        m_Data_DeviceSetting[3] = 0x01;
        m_Data_DeviceSetting[2] = 0x04;         // ----
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
            status.setText(R.string.connected);
        } else {
            status.setText(R.string.no_connect);
        }

        c1_show = MyModel.getMyModelForGivenName("C1").showName;
        c2_show = MyModel.getMyModelForGivenName("C2").showName;
        c3_show = MyModel.getMyModelForGivenName("C3").showName;
        c4_show = MyModel.getMyModelForGivenName("C4").showName;
        c5_show = MyModel.getMyModelForGivenName("C5").showName;
        tv_c1.setText(c1_show);
        tv_c2.setText(c2_show);
        tv_c3.setText(c3_show);
        tv_c4.setText(c4_show);
        tv_c5.setText(c5_show);

    }

    //修改设备可见名称
    private void Sys_SetMyDeviceName(String m_MyDeviceName) {
        // Send a message using content of the edit text widget
        int m_Length = 0;
        if (m_MyDeviceName.equals(deviceName)) {
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

    //      GetMainInfo....
    private void getWallpaperInfo() {

        byte[] m_Data = new byte[32];
        int m_Length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[3] = 0x01;
        m_Data[2] = 0x03;         // ----
        m_Data[4] = 0x6c;
        m_Data[5] = 0x01;
        m_Length = 6;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

    // 获得设备特征
    private void getConnectedDeviceCapability() {
        handleReceiveByte = true;
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

    // 主机获取设备上指定序号的壁纸的信息.
    private void getOneWallPaperInfo(int index) {
        needMerge = true;
        byte[] m_Data = new byte[32];
        int m_Length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[2] = 0x05;
        m_Data[3] = 0x01;
        m_Data[4] = 0x6c;
        m_Data[5] = 0x03;
        m_Data[6] = intToBytes(index)[0];
        m_Data[7] = intToBytes(index)[1];
        m_Length = 8;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

    // 对返回的byte[]进行处理
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
            case 0x19:
                String Protocol_Capability = BinaryToHexString(m_Data);
                byte b = m_Data[m_Index + 6];
                //转成2进制
                String tString = Integer.toBinaryString((b & 0xFF) + 0x100).substring(1);

                int num = getNumericValue(tString.charAt(5));
                Log.d(TAG, ": cap :   第6位的二进制： " + tString + "      收到的16进制数据：  " + Protocol_Capability + "    " + num);
                if (num != 1) {
                    line_set_wallpaper.setVisibility(View.GONE);

                } else {
                    line_set_wallpaper.setVisibility(VISIBLE);
                    getWallpaperInfo();
                    needMerge = true;
                }
                break;

            case (byte) 0x6C:
                if (m_Data[5] == 0x02) {
                    Log.d(TAG, "wallpaper:  準備處理 " + BinaryToHexString(m_Data));
                    Log.d(TAG, "wallpaper:  數量 ： " + bytes2Int(m_Data[6], m_Data[7]) + "  寬度  :" + bytes2Int(m_Data[8], m_Data[9]) + "  高度： " + bytes2Int(m_Data[10], m_Data[11]));
                    Log.d(TAG, "wallpaper:   : " + "  缓冲容量 TT:  " + bytesToInt(m_Data, 13) + "  单个数据包最大容量  VV:  " + bytes2Int(m_Data[17], m_Data[18]));
                    Log.d(TAG, "wallpaper:   最小地址 KK:   " + bytesToInt(m_Data, 19) + "  最大地址LL:   " + bytesToInt(m_Data, 23));
                    byte b1 = m_Data[12];
                    String substring = Integer.toBinaryString((b1 & 0xFF) + 0x100).substring(1);

                    char c = substring.charAt(0);
                    char c1 = substring.charAt(1);
                    char c2 = substring.charAt(2);
                    char c3 = substring.charAt(3);
                    char c4 = substring.charAt(4);
                    char c5 = substring.charAt(5);
                    Log.d(TAG, "wallpaper:  "+substring+"  "+c+"  "+c1+" "+c2+" "+c3+" "+c4+" "+c5);
                    int numericValue = getNumericValue((int) c5);
                    Log.d(TAG, "wallpaper:  "+substring+"  "+c+"  "+c1+" "+c2+" "+c3+" "+c4+" "+c5+"  value: "+numericValue);
                    if(numericValue==1){
                        supportPreview = true;
                    }else {
                        supportPreview = false;
                    }
                    //                    handler.postDelayed(new Runnable() {
                    //                        @Override
                    //                        public void run() {
                    //                            getOneWallPaperInfo(1);
                    //                        }
                    //                    }, 50);
                    //                } else if (m_Data[5] == 0x04) {
                    //                    Log.d(TAG, "wallpaper: 准备好数据：  " + BinaryToHexString(m_Data) + "   地址： " + bytesToInt(m_Data, 12));
                    //
                }

                break;

            default:

                break;

        }
    }

    private void toshowWheel() {
        list.clear();

        //每次进行初始化
        for (int i = 0; i < 30; i++) {
            list.add("第 " + i + " 个");
        }

        if (list.size() == 0) {
            return;
        }

        wheel.setData(list);
        wheel.setSelectedItemPosition(8);
    }


    //进入更换壁纸的功能模式
    private void getWallPaperRequest() {

        byte[] m_Data = new byte[32];
        int m_Length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[2] = 0x04;
        m_Data[3] = 0x01;
        m_Data[4] = 0x6C;
        m_Data[5] = 0x15;
        m_Data[6] = 0x00;

        //        m_Data[7] = 0x00;
        //        m_Data[8] = 0x01;
        //        m_Data[9] = (byte) 0xC2;
        //        m_Data[10] = 0x00;
        //
        //        m_Data[11] = 0x01;
        //
        //        m_Data[12] = 0x00;
        //        m_Data[13] = 0x01;

        m_Length = 7;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

}
