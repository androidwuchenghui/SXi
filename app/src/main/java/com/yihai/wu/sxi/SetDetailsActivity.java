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
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.yihai.wu.appcontext.MyModel;
import com.yihai.wu.util.DarkImageButton;
import com.yihai.wu.util.MyUtils;
import com.yihai.wu.widget.switch_button.SwitchView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.yihai.wu.sxi.DeviceScanActivity.BinaryToHexString;
import static com.yihai.wu.sxi.R.id.rg_joule;


/**
 * Created by ${Wu} on 2016/12/13.
 */

public class SetDetailsActivity extends AppCompatActivity {
    @Bind(R.id.selected_material)
    TextView selectedMaterial;
    @Bind(R.id.btn_back)
    DarkImageButton btnBack;
    @Bind(R.id.modelName)
    TextView modelName;
    @Bind(R.id.connect_state)
    TextView connectState;
    @Bind(R.id.bt_switch)
    SwitchView btSwitch;
    @Bind(R.id.display_status)
    RadioGroup rgDisplayStatus;
    @Bind(R.id.select_material)
    LinearLayout selectMaterial;
    @Bind(R.id.texture)
    TextView texture;
    @Bind(R.id.select_texture)
    LinearLayout selectTexture;
    @Bind(R.id.rg_memories)
    RadioGroup rgMemories;
    @Bind(R.id.rg_unit_temperature)
    RadioGroup rgUnitTemperature;
    @Bind(rg_joule)
    RadioGroup rgJoule;
    @Bind(R.id.rg_operation)
    RadioGroup rgOperation;

    @Bind(R.id.seekBar_adjust_temperature)
    SeekBar seekBarAdjustTemperature;
    @Bind(R.id.show_compensation_temperature)
    TextView showCompensationTemperature;
    @Bind(R.id.seekBar_compensation_temperature)
    SeekBar seekBarCompensationTemperature;
    @Bind(R.id.show_TCR)
    TextView showTCR;
    @Bind(R.id.seekBar_set_TCR)
    SeekBar seekBarSetTCR;
    @Bind(R.id.show_power)
    TextView showPower;
    @Bind(R.id.seekBar_set_power)
    SeekBar seekBarSetPower;


    //请求码requestCode
    private final static int REQUEST_MATERIAL_CODE = 0X010;
    private final static int REQUEST_TEXTURE_CODE = 0x009;
    private final static String MATERIAL = "material";
    private final static String TEXTURE = "texture";


    @Bind(R.id.temp_unit_c)
    RadioButton tempUnitC;
    @Bind(R.id.temp_unit_f)
    RadioButton tempUnitF;
    @Bind(R.id.textView2)
    TextView textView2;
    @Bind(R.id.unit_c)
    TextView unitC;
    @Bind(R.id.mini_sk_at)
    TextView miniSkAt;
    @Bind(R.id.max_sk_at)
    TextView maxSkAt;
    @Bind(R.id.unit_f)
    TextView unitF;
    @Bind(R.id.mini_sk_ct)
    TextView miniSkCt;
    @Bind(R.id.max_sk_ct)
    TextView maxSkCt;
    @Bind(R.id.mini_sk_tcr)
    TextView miniSkTcr;
    @Bind(R.id.max_sk_tcr)
    TextView maxSkTcr;
    @Bind(R.id.power_joule)
    TextView powerJoule;
    @Bind(R.id.mini_sk_pj)
    TextView miniSkPj;
    @Bind(R.id.max_sk_pj)
    TextView maxSkPj;
    @Bind(R.id.show_adjust_temperature)
    TextView showAdjustTemperature;
    @Bind(R.id.line_show_power)
    LinearLayout lineShowPower;
    @Bind(R.id.tv_joule)
    TextView tvJoule;
    @Bind(R.id.show_joule)
    TextView showJoule;
    @Bind(R.id.mini_sk_j)
    TextView miniSkJ;
    @Bind(R.id.max_sk_j)
    TextView maxSkJ;
    @Bind(R.id.seekBar_set_joule)
    SeekBar seekBarSetJoule;
    @Bind(R.id.line_show_joule)
    LinearLayout lineShowJoule;
    private static final String TAG = "SetDetailsActivity";
    private boolean isCentigrade = true;
    private String detail;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic g_Character_TX;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setdetails);
        ButterKnife.bind(this);
        registerReceiver(setDetailsActivityReceiver, makeBroadcastFilter());
        initListener();
        initUI();

        Intent gattServiceIntent = new Intent(SetDetailsActivity.this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private final BroadcastReceiver setDetailsActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case BluetoothLeService.ACTION_DATA_RX:
                    Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
                    byte[] data = bundle.getByteArray("byteValues");
                    String s = BinaryToHexString(data);
                    Log.d(TAG, "onReceiveRX: 设置详情页收到的数据为：  " + s);
                    Sys_YiHi_Protocol_RX_Porc(data);
                    break;
            }
        }
    };

    private static IntentFilter makeBroadcastFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RX);
        return intentFilter;
    }

    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("service", "Unable to initialize Bluetooth");
                finish();
            }

            g_Character_TX = mBluetoothLeService.getG_Character_TX();
            if (g_Character_TX != null) {

                getConnectedDevicePowerModel();
                //                AckUserDeviceSetting();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("service", "onServiceDisconnected: " + "---------服务未连接-------------");
            mBluetoothLeService = null;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

    }

    private void initUI() {
        Intent intent = getIntent();
        detail = intent.getStringExtra("detail");
        modelName.setText(detail);
        MyModel myModel = MyModel.getMyModelForGivenName(detail);
        int status = myModel.status;
        btSwitch.setOpened(status == 0 ? false : true);

        int display = myModel.display;
        Log.d("log", "initUI:取得数据 " + display);
        RadioButton display_rbt = (RadioButton) rgDisplayStatus.getChildAt(display);
        display_rbt.setChecked(true);
        //材料
        String material = myModel.material;
        selectedMaterial.setText(material);
        //口感
        int textured = myModel.texture;
        setShowText(textured);
        //记忆模式选择
        int memory = myModel.memory;
        RadioButton rb_M = (RadioButton) rgMemories.getChildAt(memory);
        rb_M.setChecked(true);
        //温度单位选择
        int temperatureUnit = myModel.temperatureUnit;
        rgUnitTemperature.getChildAt(temperatureUnit).performClick();
        //功率焦耳切换
        int JouleOrPower = myModel.JouleOrPower;
        rgJoule.getChildAt(JouleOrPower).performClick();

        //操作模式
        int operation = myModel.operation;
        RadioButton rb_operation = (RadioButton) rgOperation.getChildAt(operation);
        rb_operation.setChecked(true);

        //seekBar温度调节
        int temperature = myModel.temperature;
        seekBarAdjustTemperature.setProgress(temperature - 100);


        //seekBar补偿温度
        int temperature_c = myModel.temperature_c;
        seekBarCompensationTemperature.setProgress(temperature_c);

        //seekBar设置TCR
        int tcr = myModel.tcr;
        seekBarSetTCR.setProgress(tcr - 50);
        if (tcr < 100) {
            showTCR.setText("0.000" + tcr);
        } else {
            showTCR.setText("0.00" + tcr);
        }
        //seekBar功率调节
        int power = myModel.power;
        seekBarSetPower.setProgress(power - 50);
        showPower.setText(power / 10 + "." + power % 10);
        //seekBar焦耳调节
        int joule = myModel.joule;
        seekBarSetJoule.setProgress(joule - 100);
        showJoule.setText(joule / 10 + "." + joule % 10);
    }

    //口感选择显示
    private void setShowText(int textured) {
        if (textured == 0) {
            texture.setText(R.string.texture_power_save);
        } else if (textured == 1) {
            texture.setText(R.string.texture_soft);
        } else if (textured == 2) {
            texture.setText(R.string.texture_standard);
        } else if (textured == 3) {
            texture.setText(R.string.texture_strong);
        } else if (textured == 4) {
            texture.setText(R.string.texture_super_strong);
        } else if (textured == 5) {
            texture.setText(R.string.texture_custom_s1);
        } else if (textured == 6) {
            texture.setText(R.string.texture_custom_s2);
        } else if (textured == 7) {
            texture.setText(R.string.texture_custom_s3);
        } else if (textured == 8) {
            texture.setText(R.string.texture_custom_s4);
        } else if (textured == 9) {
            texture.setText(R.string.texture_custom_s5);
        }
    }

    private void initListener() {

        rgDisplayStatus.setOnCheckedChangeListener(new OnCheckedChangeListen());
        rgUnitTemperature.setOnCheckedChangeListener(new OnCheckedChangeListen());
        rgJoule.setOnCheckedChangeListener(new OnCheckedChangeListen());
        rgMemories.setOnCheckedChangeListener(new OnCheckedChangeListen());
        rgOperation.setOnCheckedChangeListener(new OnCheckedChangeListen());
        //seekBar的监听
        seekBarAdjustTemperature.setOnSeekBarChangeListener(new onSeekBarChangeListen());
        seekBarCompensationTemperature.setOnSeekBarChangeListener(new onSeekBarChangeListen());
        seekBarSetTCR.setOnSeekBarChangeListener(new onSeekBarChangeListen());
        seekBarSetPower.setOnSeekBarChangeListener(new onSeekBarChangeListen());
        seekBarSetJoule.setOnSeekBarChangeListener(new onSeekBarChangeListen());


    }

    @OnClick({R.id.select_material, R.id.btn_back, R.id.select_texture, R.id.bt_switch})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                finish();
                break;
            case R.id.select_material:
                Intent intent = new Intent(SetDetailsActivity.this, MaterialActivity.class);
                intent.putExtra("title", detail);
                startActivityForResult(intent, REQUEST_MATERIAL_CODE);
                break;
            case R.id.bt_switch:
                MyModel myModel_s = MyModel.getMyModelForGivenName(detail);
                if (btSwitch.isOpened()) {
                    myModel_s.status = 1;
                } else {
                    myModel_s.status = 0;
                }
                myModel_s.save();
                break;
            case R.id.select_texture:
                Intent textureIntent = new Intent(SetDetailsActivity.this, SetTextureActivity.class);
                textureIntent.putExtra("name", detail);
                startActivityForResult(textureIntent, REQUEST_TEXTURE_CODE);
                break;


        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MATERIAL_CODE && resultCode == RESULT_OK) {
            String material = data.getStringExtra(MATERIAL);
            selectedMaterial.setText(material);
        } else if (requestCode == REQUEST_TEXTURE_CODE && resultCode == RESULT_OK) {
            String getTexture = data.getStringExtra(TEXTURE);
            texture.setText(getTexture);
        }

    }

    //seekBar变化时
    private class onSeekBarChangeListen implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            switch (seekBar.getId()) {
                case R.id.seekBar_adjust_temperature:
                    if (isCentigrade) {
                        showAdjustTemperature.setText(i + 100 + "");
                    } else {
                        showAdjustTemperature.setText((i + 100) * 9 / 5 + 32 + "");
                    }
                    break;
                case R.id.seekBar_compensation_temperature:
                    if (isCentigrade) {
                        showCompensationTemperature.setText(i + "");
                    } else {
                        showCompensationTemperature.setText(i * 9 / 5 + 32 + "");
                    }

                    break;
                case R.id.seekBar_set_TCR:
                    if (i + 50 < 100) {
                        showTCR.setText("0.000" + (i + 50));
                    } else {
                        showTCR.setText("0.00" + (i + 50));
                    }
                    break;
                case R.id.seekBar_set_power:
                    showPower.setText((i + 50) / 10 + "." + (i + 50) % 10);

                    break;
                case R.id.seekBar_set_joule:
                    showJoule.setText((i + 100) / 10 + "." + (i + 100) % 10);
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            switch (seekBar.getId()) {
                case R.id.seekBar_adjust_temperature:
                    MyModel myModel = MyModel.getMyModelForGivenName(detail);
                    myModel.temperature = seekBar.getProgress() + 100;
                    myModel.save();
                    break;
                case R.id.seekBar_compensation_temperature:
                    MyModel myModel1 = MyModel.getMyModelForGivenName(detail);
                    myModel1.temperature_c = seekBar.getProgress();
                    myModel1.save();
                    break;
                case R.id.seekBar_set_TCR:
                    MyModel myModel2 = MyModel.getMyModelForGivenName(detail);
                    myModel2.tcr = seekBar.getProgress() + 50;
                    myModel2.save();
                    break;
                case R.id.seekBar_set_power:
                    int sendData = seekBar.getProgress() + 50;
                    MyModel myModel3 = MyModel.getMyModelForGivenName(detail);
                    myModel3.power = sendData;//需要发送的数据
                    myModel3.save();
                    setPowerValue(sendData / 10, sendData % 10);

                case R.id.seekBar_set_joule:
                    MyModel myModel4 = MyModel.getMyModelForGivenName(detail);
                    myModel4.joule = seekBar.getProgress() + 100;
                    myModel4.save();
            }

        }
    }

    //radioGroup变化时
    private class OnCheckedChangeListen implements RadioGroup.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            MyModel myModel_C = MyModel.getMyModelForGivenName(detail);
            switch (radioGroup.getId()) {
                case R.id.rg_unit_temperature:
                    //                    Log.e("log", "onCheckedChanged: >>>>>>>"+i +"*********"+radioGroup.getCheckedRadioButtonId());
                    switch (i) {
                        case R.id.temp_unit_c:
                            isCentigrade = true;
                            myModel_C.temperatureUnit = 0;

                            //温度调节的seekBar
                            unitC.setText(getResources().getString(R.string.temperature_C));
                            miniSkAt.setText("" + 100);
                            maxSkAt.setText("" + 300);
                            showAdjustTemperature.setText(myModel_C.temperature + "");

                            //补偿温度的seekBar
                            unitF.setText(getResources().getString(R.string.temperature_C));
                            miniSkCt.setText("" + 0);
                            maxSkCt.setText("" + 50);
                            showCompensationTemperature.setText(myModel_C.temperature_c + "");
                            break;
                        case R.id.temp_unit_f:
                            isCentigrade = false;
                            myModel_C.temperatureUnit = 1;
                            //温度调节的seekBar
                            unitC.setText(getResources().getString(R.string.temperature_F));
                            miniSkAt.setText(100 * 9 / 5 + 32 + "");
                            maxSkAt.setText(300 * 9 / 5 + 32 + "");

                            showAdjustTemperature.setText(myModel_C.temperature * 9 / 5 + 32 + "");
                            //补偿温度的seekBar
                            unitF.setText(getResources().getString(R.string.temperature_F));
                            miniSkCt.setText(0 * 9 / 5 + 32 + "");
                            maxSkCt.setText(50 * 9 / 5 + 32 + "");
                            showCompensationTemperature.setText(myModel_C.temperature_c * 9 / 5 + 32 + "");

                            break;
                    }

                    break;
                case R.id.rg_joule:
                    switch (i) {
                        case R.id.rb_power:
                            myModel_C.JouleOrPower = 0;
                            lineShowPower.setVisibility(View.VISIBLE);
                            lineShowJoule.setVisibility(View.GONE);
                            setUserDevicePowerOrJoule((byte) 0x01);

                            break;
                        case R.id.rb_joule:
                            myModel_C.JouleOrPower = 1;
                            lineShowPower.setVisibility(View.GONE);
                            lineShowJoule.setVisibility(View.VISIBLE);
                            setUserDevicePowerOrJoule((byte) 0x02);
                            break;


                    }

                    break;
                case R.id.display_status:
                    if (i == R.id.rb_dis_left) {
                        myModel_C.display = 0;
                    } else if (i == R.id.rb_dis_right) {
                        myModel_C.display = 1;
                    } else if (i == R.id.rb_dis_auto) {
                        myModel_C.display = 2;
                    }
                    break;
                case R.id.rg_memories:
                    if (i == R.id.rb_M1) {
                        myModel_C.memory = 0;
                    } else if (i == R.id.rb_M2) {
                        myModel_C.memory = 1;
                    } else if (i == R.id.rb_M3) {
                        myModel_C.memory = 2;
                    } else if (i == R.id.rb_M4) {
                        myModel_C.memory = 3;
                    } else if (i == R.id.rb_M4) {
                        myModel_C.memory = 4;
                    }
                    break;
                case R.id.rg_operation:
                    if (i == R.id.rb_primary) {
                        myModel_C.operation = 0;
                    } else {
                        myModel_C.operation = 1;
                    }
                    break;

            }
            myModel_C.save();
        }
    }

    private void getConnectedDevicePowerModel() {
        byte[] m_Data = new byte[32];
        int m_length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[3] = 0x01; //Device ID
        m_Data[2] = 0x03;
        m_Data[4] = 0x57;
        m_Data[5] = 0x0F;
        m_length = 6;
        Sys_Proc_Charactor_TX_Send(m_Data, m_length);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(setDetailsActivityReceiver);
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
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
        m_SecondCommand = m_Data[(m_Index + 5)];
        Log.d(TAG, "onReceiveMainActivity: " + m_Command);
        if (m_Command == 0x58 && m_SecondCommand == 0x0F)
        //获得功率焦耳切换
        {
            s = BinaryToHexString(m_Data);
            String substring = s.substring(12);
            Log.d(TAG, "处理    : power&Joule--   " + s + "  -:   " + substring);
            int model = Integer.parseInt(substring);
            //1
            MyModel myModel = MyModel.getMyModelForGivenName(detail);
            if (myModel != null) {
                myModel.JouleOrPower = model - 1;

                rgJoule.getChildAt(myModel.JouleOrPower).performClick();
                myModel.save();
            }

        }

    }

    public void setUserDevicePowerOrJoule(byte b) {
        byte[] m_Data = new byte[32];
        int m_length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[3] = 0x01; //Device ID
        m_Data[2] = 0x04;
        m_Data[4] = 0x59;
        m_Data[5] = 0x0F;
        m_Data[6] = b;
        m_length = 7;
        Sys_Proc_Charactor_TX_Send(m_Data, m_length);
    }

    public void setPowerValue(int powerValue_x10, int powerValue_x1) {

        byte[] m_Data = new byte[32];
        int m_length = 0;
        byte temp;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[3] = 0x01; //Device ID

        m_Data[2] = 0x05;
        m_Data[4] = 0x07;
        temp = (byte) (powerValue_x10>>8);
        m_Data[5] = temp;

        temp = (byte) ((powerValue_x10<<8)>>8);
        m_Data[6] = temp;

        temp =  (MyUtils.int2OneByte(powerValue_x1));
        m_Data[7] = temp;
        m_length = 8;
        Sys_Proc_Charactor_TX_Send(m_Data, m_length);
    }

}
