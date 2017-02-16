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
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yihai.wu.appcontext.MyModel;
import com.yihai.wu.util.DarkImageButton;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.yihai.wu.util.MyUtils.BinaryToHexString;

/**
 * Created by ${Wu} on 2016/12/19.
 */

public class SetTextureActivity extends AppCompatActivity {

    @Bind(R.id.btn_back)
    DarkImageButton btnBack;
    @Bind(R.id.modelName)
    TextView modelName;
    @Bind(R.id.connect_state)
    TextView connectState;
    @Bind(R.id.check1)
    ImageView check1;
    @Bind(R.id.tv_power_save)
    TextView tvPowerSave;
    @Bind(R.id.line_power_save)
    LinearLayout linePowerSave;
    @Bind(R.id.check2)
    ImageView check2;
    @Bind(R.id.tv_soft)
    TextView tvSoft;
    @Bind(R.id.line_soft)
    LinearLayout lineSoft;
    @Bind(R.id.check3)
    ImageView check3;
    @Bind(R.id.tv_standard)
    TextView tvStandard;
    @Bind(R.id.line_standard)
    LinearLayout lineStandard;
    @Bind(R.id.check4)
    ImageView check4;
    @Bind(R.id.tv_strong)
    TextView tvStrong;
    @Bind(R.id.line_strong)
    LinearLayout lineStrong;
    @Bind(R.id.check5)
    ImageView check5;
    @Bind(R.id.tv_super_strong)
    TextView tvSuperStrong;
    @Bind(R.id.line_super_strong)
    LinearLayout lineSuperStrong;
    @Bind(R.id.check6)
    ImageView check6;
    @Bind(R.id.tv_custom_s1)
    TextView tvCustomS1;
    @Bind(R.id.detail_s1)
    ImageView detailS1;
    @Bind(R.id.line_custom_s1)
    LinearLayout lineCustomS1;
    @Bind(R.id.check7)
    ImageView check7;
    @Bind(R.id.tv_custom_s2)
    TextView tvCustomS2;
    @Bind(R.id.detail_s2)
    ImageView detailS2;
    @Bind(R.id.line_custom_s2)
    LinearLayout lineCustomS2;
    @Bind(R.id.check8)
    ImageView check8;
    @Bind(R.id.tv_custom_s3)
    TextView tvCustomS3;
    @Bind(R.id.detail_s3)
    ImageView detailS3;
    @Bind(R.id.line_custom_s3)
    LinearLayout lineCustomS3;
    @Bind(R.id.check9)
    ImageView check9;
    @Bind(R.id.tv_custom_s4)
    TextView tvCustomS4;
    @Bind(R.id.detail_s4)
    ImageView detailS4;
    @Bind(R.id.line_custom_s4)
    LinearLayout lineCustomS4;
    @Bind(R.id.check10)
    ImageView check10;
    @Bind(R.id.tv_custom_s5)
    TextView tvCustomS5;
    @Bind(R.id.detail_s5)
    ImageView detailS5;
    @Bind(R.id.line_custom_s5)
    LinearLayout lineCustomS5;

    private int[] checked_arr = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private final static String TEXTURE = "texture";

    private String model;

    private static final String TAG = "SetTextureActivity";
    private String send = null;
    private final BroadcastReceiver setDetailsActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothLeService.ACTION_DATA_RX:
                    Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
                    byte[] data = bundle.getByteArray("byteValues");
                    String s = BinaryToHexString(data);

//                    if(begin){
//                        r++;
//                    }
                    Log.d(TAG, "口感选择收到数据: " + s+"   R: " );
                    /*if(TAG=="SetTextureActivity"&&r==1&&begin ==true){
                        oneFirst = subBytes(data, 11, 9);
                        Log.d(TAG, "onFirst: "+BinaryToHexString(oneFirst));
                    }else if(TAG=="SetTextureActivity"&&r==2&&begin ==true){
                        oneFirst = byteMerger(oneFirst, data);
                        Log.d(TAG, "onFirst: "+BinaryToHexString(oneFirst));
                    }else if(TAG=="SetTextureActivity"&&r==3&&begin ==true){
                        oneFirst = byteMerger(oneFirst, data);
                        Log.d(TAG, "onFirst: "+BinaryToHexString(oneFirst));
                    }else if(TAG=="SetTextureActivity"&&r==4&&begin ==true){
                        oneFirst = byteMerger(oneFirst, data);
                        Log.d(TAG, "onFirst: "+BinaryToHexString(oneFirst));
                        //处理取到的50个数据
//                        for (int i = 0; i < oneFirst.length; i+=2) {
//                            byte[] bs = new byte[2];
//                            bs[0] = oneFirst[i];
//                            bs[1] = oneFirst[i+1];
//                            int powerData = ((bs[0]&0xff)<<8)|(bs[1] & 0xff);
//                            Log.d(TAG, "onReceive: "+powerData);
//                        }
                        Log.d(TAG, "onReceive: "+r+"  "+begin+"   " +order);
                        if (order==4){
                            order=0;
                        }

                    }
                    if(r==4&&begin&&order==1){
                        begin=false;
                        r=0;
                        //取第二组25个点
                        settingPackage_PowerCurve_ReadData((byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x04, (byte) 0x00);
                    }else if(r==4&&begin&&order==2){
                        begin=false;
                        r=0;
                        //取第三组25个点
                        settingPackage_PowerCurve_ReadData((byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x04, (byte) 0x00);
                    }else if(r==4&&begin&&order==3){
                        begin=false;
                        r=0;
                        //取第四组25个点
                        settingPackage_PowerCurve_ReadData((byte) 0x00, (byte) 0x01, (byte) 0x03, (byte) 0x04, (byte) 0x00);
                    }*/

//                    if (send.equals(TAG)) {
//                        Sys_YiHi_Protocol_RX_Porc(data);
//                    }

                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                        connectState.setText("未连接");
                    break;
            }
        }
    };
    private byte[] oneFirst;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture);
        ButterKnife.bind(this);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        registerReceiver(setDetailsActivityReceiver, makeBroadcastFilter());
        initUI();
        Log.d(TAG, "SetTextureActivityonCreate: ");
    }

    //界面初始化
    private void initUI() {
        Intent intent = getIntent();
        model = intent.getStringExtra("name");
        MyModel myModel = MyModel.getMyModelForGivenName(model);
        int texture = myModel.texture;
        select_control(texture);

    }

    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic g_Character_TX;
    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("service", "Unable to initialize Bluetooth");
                finish();
            }
            if (mBluetoothLeService.getTheConnectedState() == 0) {
                connectState.setText("未连接");
            } else if (mBluetoothLeService.getTheConnectedState() == 2) {
                connectState.setText("已连接");
            }
//            g_Character_TX = mBluetoothLeService.getG_Character_TX();
//            if (g_Character_TX != null) {             //C1 、S1   功率曲线上的点的数据   序号00     第一组的25个点
//                settingPackage_PowerCurve_ReadData((byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00);
//            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("service", "onServiceDisconnected: " + "---------服务未连接-------------");
            mBluetoothLeService = null;
        }
    };

    //点击事件
    @OnClick({R.id.btn_back, R.id.tv_power_save, R.id.tv_soft, R.id.tv_standard, R.id.tv_strong, R.id.tv_super_strong, R.id.tv_custom_s1, R.id.detail_s1, R.id.tv_custom_s2, R.id.detail_s2, R.id.tv_custom_s3, R.id.detail_s3, R.id.tv_custom_s4, R.id.detail_s4, R.id.tv_custom_s5, R.id.detail_s5})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                finish();
                break;
            case R.id.tv_power_save:
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x12, (byte) 0x00);
                }
                pressed(0, getResources().getString(R.string.texture_power_save));
                break;
            case R.id.tv_soft:
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x12, (byte) 0x01);
                }
                pressed(1, getResources().getString(R.string.texture_soft));
                break;
            case R.id.tv_standard:
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x12, (byte) 0x02);
                }
                pressed(2, getResources().getString(R.string.texture_standard));
                break;
            case R.id.tv_strong:
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x12, (byte) 0x03);
                }
                pressed(3, getResources().getString(R.string.texture_strong));
                break;
            case R.id.tv_super_strong:
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x12, (byte) 0x04);
                }
                pressed(4, getResources().getString(R.string.texture_super_strong));
                break;
            case R.id.tv_custom_s1:
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x12, (byte) 0x05);
                }
                pressed(5, getResources().getString(R.string.texture_custom_s1));
                break;
            case R.id.detail_s1:
                Intent s1ToCurve = new Intent(SetTextureActivity.this,BezierActivity.class);
                s1ToCurve.putExtra("custom", "S1");
                startActivity(s1ToCurve);//进入曲线界面
                break;
            case R.id.tv_custom_s2:
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x12, (byte) 0x06);
                }
                pressed(6, getResources().getString(R.string.texture_custom_s2));
                break;
            case R.id.detail_s2:

                Intent s2ToCurve = new Intent(SetTextureActivity.this,BezierActivity.class);
                s2ToCurve.putExtra("custom", "S2");
                startActivity(s2ToCurve);//进入曲线界面
                break;
            case R.id.tv_custom_s3:
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x12, (byte) 0x07);
                }
                pressed(7, getResources().getString(R.string.texture_custom_s3));
                break;
            case R.id.detail_s3:
                Intent s3ToCurve = new Intent(SetTextureActivity.this,BezierActivity.class);
                s3ToCurve.putExtra("custom", "S3");
                startActivity(s3ToCurve);//进入曲线界面
                break;
            case R.id.tv_custom_s4:
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x12, (byte) 0x08);
                }
                pressed(8, getResources().getString(R.string.texture_custom_s4));
                break;
            case R.id.detail_s4:
                Intent s4ToCurve = new Intent(SetTextureActivity.this,BezierActivity.class);
                s4ToCurve.putExtra("custom", "S4");
                startActivity(s4ToCurve);//进入曲线界面
                break;
            case R.id.tv_custom_s5:
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x12, (byte) 0x09);
                }
                pressed(9, getResources().getString(R.string.texture_custom_s5));
                break;
            case R.id.detail_s5:
                Intent s5ToCurve = new Intent(SetTextureActivity.this,BezierActivity.class);
                s5ToCurve.putExtra("custom", "S5");
                startActivity(s5ToCurve);//进入曲线界面
                break;
        }
    }

    //控制选择
    private void select_control(int texture) {

        for (int i = 0; i < checked_arr.length; i++) {
            checked_arr[i] = 0;
        }
        checked_arr[texture] = 1;

        if (checked_arr[0] == 1) {
            check1.setVisibility(View.VISIBLE);
        } else {
            check1.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[1] == 1) {
            check2.setVisibility(View.VISIBLE);
        } else {
            check2.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[2] == 1) {
            check3.setVisibility(View.VISIBLE);
        } else {
            check3.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[3] == 1) {
            check4.setVisibility(View.VISIBLE);
        } else {
            check4.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[4] == 1) {
            check5.setVisibility(View.VISIBLE);
        } else {
            check5.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[5] == 1) {
            check6.setVisibility(View.VISIBLE);
        } else {
            check6.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[6] == 1) {
            check7.setVisibility(View.VISIBLE);
        } else {
            check7.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[7] == 1) {
            check8.setVisibility(View.VISIBLE);
        } else {
            check8.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[8] == 1) {
            check9.setVisibility(View.VISIBLE);
        } else {
            check9.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[9] == 1) {
            check10.setVisibility(View.VISIBLE);
        } else {
            check10.setVisibility(View.INVISIBLE);
        }

    }

    //选择点击
    public void pressed(int i, String name) {
        Intent intent = new Intent();
        select_control(i);
        MyModel myModel = MyModel.getMyModelForGivenName(model);
        myModel.texture = i;
        myModel.save();

        intent.putExtra(TEXTURE, name);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void setUserDeviceSetting(byte nn, byte pp) {
        byte[] m_Data_DeviceSetting = new byte[32];
        int m_Length = 0;
        m_Data_DeviceSetting[0] = 0x55;
        m_Data_DeviceSetting[1] = (byte) 0xFF;
        m_Data_DeviceSetting[3] = 0x01; //Device ID
        m_Data_DeviceSetting[2] = 0x04;
        m_Data_DeviceSetting[4] = 0x59;
        m_Data_DeviceSetting[5] = nn;
        m_Data_DeviceSetting[6] = pp;

        m_Length = 7;
        Sys_Proc_Charactor_TX_Send(m_Data_DeviceSetting, m_Length);
    }

    private void Sys_Proc_Charactor_TX_Send(byte[] m_Data, int m_Length) {

        byte[] m_MyData = new byte[m_Length];
        for (int i = 0; i < m_Length; i++) {
            m_MyData[i] = m_Data[i];
        }

        if (g_Character_TX == null) {
            Log.e("SetDetailsActivity", "character TX is null");
            return;
        }

        if (m_Length <= 0) {
            return;
        }
        g_Character_TX.setValue(m_MyData);
        mBluetoothLeService.writeCharacteristic(g_Character_TX);
    }

    private static IntentFilter makeBroadcastFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RX);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);

        return intentFilter;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        g_Character_TX = mBluetoothLeService.getG_Character_TX();
        Log.d(TAG, "onRestart:----MainActivity---   " + mBluetoothLeService.getTheConnectedState());
        if (mBluetoothLeService.getTheConnectedState() == 2) {
            connectState.setText("已连接");
        } else {
            connectState.setText("未连接");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(setDetailsActivityReceiver);
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

   /* //读取曲线数据                         Setting 包的序号     曲线      序号    数量    编号
    public void settingPackage_PowerCurve_ReadData(byte pp, byte tt, byte mm, byte rr, byte ll) {
        Log.d(TAG, "口感选择发出数据: ");
        send = TAG;
        byte[] m_Data_DeviceSetting = new byte[32];
        int m_Length = 0;
        m_Data_DeviceSetting[0] = 0x55;
        m_Data_DeviceSetting[1] = (byte) 0xFF;
        m_Data_DeviceSetting[3] = 0x01; //Device ID
        m_Data_DeviceSetting[2] = 0x09;
        m_Data_DeviceSetting[4] = 0x66;
        m_Data_DeviceSetting[5] = pp;           //C1 ~ C5
        m_Data_DeviceSetting[6] = tt;           //1-W 点,2-W 线，3-temp 点，4-temp线
        m_Data_DeviceSetting[7] = mm;           //      序号
        m_Data_DeviceSetting[8] = rr;           //一条曲线分为4个数据包
        m_Data_DeviceSetting[9] = ll;           //S1 ~ S5
        m_Data_DeviceSetting[10] = (byte) (50 >> 8) & 0xff;
        m_Data_DeviceSetting[11] = (byte) 50 & 0xff;

        m_Length = 12;
        Sys_Proc_Charactor_TX_Send(m_Data_DeviceSetting, m_Length);
    }

    private void Sys_YiHi_Protocol_RX_Porc(byte[] m_Data) {
        int m_Length = 0;
        int i;
        int m_Index = 0xfe;
        byte m_ValidData_Length = 0;
        byte m_Command = 0;
        byte m_SecondCommand = 0;
        String s = null;
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
            case 0x67:
//                Log.d(TAG, "口感选择准备处理数据: " + BinaryToHexString(m_Data));
                int height = (m_Data[6] & 0xf0) >> 4;
                int low = m_Data[6] & 0x0f;
//                Log.d(TAG, "口感选择准备处理数据: "+"低四位： "+low);
                if (height == 1) {
                    final int waitTime = ((m_Data[9] & 0xff) << 8) | (m_Data[10] & 0xff);
//                    Log.d(TAG, "等待时间: " + waitTime);
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            try {
                                Thread.sleep(waitTime);
                                getSettingPackage_ReadData_GetResult();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }

                break;
        }
    }
    private byte first25;
    private int r ;
    private int order = 0;
    private boolean begin = false;
    private void getSettingPackage_ReadData_GetResult() {
        order++;
        begin = true;
        r=0;
        byte[] m_Data = new byte[32];
        int m_length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[3] = 0x01; //Device ID
        m_Data[2] = 0x03;
        m_Data[4] = 0x69;
        m_Data[5] = 0x02;
        m_length = 6;
        Sys_Proc_Charactor_TX_Send(m_Data, m_length);
    }*/
}
