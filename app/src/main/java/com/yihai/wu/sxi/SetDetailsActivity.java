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

import static com.yihai.wu.sxi.R.id.rg_joule;
import static com.yihai.wu.util.MyUtils.BinaryToHexString;
import static com.yihai.wu.util.MyUtils.byteMerger;


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
    private byte pack;
    private boolean mergerData = false;
    private int jouleOrPower;
    private boolean mergerDataOver = false;

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

    private String first = "";
    private byte[] firstByteArray;
    private int receiveCount = 0;
    private final BroadcastReceiver setDetailsActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case BluetoothLeService.ACTION_DATA_RX:
                    Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
                    byte[] data = bundle.getByteArray("byteValues");
                    String s = BinaryToHexString(data);
                    Log.d(TAG, "receiveInfo: " + s);

                    if (mergerData) {
                        if (receiveCount == 1) {
                            mergerDataOver = true;
                            byte[] mergerBytes = byteMerger(firstByteArray, data);
                            String result = first + s;
                            Log.d(TAG, "temperInfo: 合并：" + BinaryToHexString(mergerBytes));
                            mergerData = false;

                            receiveCount = 0;
                            first = "";
                            Sys_YiHi_Protocol_RX_Porc(mergerBytes);

                        } else {
                            mergerDataOver = false;
                            first += s;
                            Log.d(TAG, "mergerBytes: 一段：" + first);
                            firstByteArray = new byte[data.length];
                            firstByteArray = data;
                            receiveCount++;
                        }
                    } else {
                        Sys_YiHi_Protocol_RX_Porc(data);
                    }

                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    connectState.setText("未连接");
                    break;

            }
        }
    };

    private static IntentFilter makeBroadcastFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RX);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
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

            if (mBluetoothLeService.getTheConnectedState() == 0) {
                connectState.setText("未连接");
            } else if (mBluetoothLeService.getTheConnectedState() == 2) {
                connectState.setText("已连接");
            }
            g_Character_TX = mBluetoothLeService.getG_Character_TX();
            if (g_Character_TX != null) {
                //                getConnectedDevicePowerModel();
                //                AckUserDeviceSetting();

                switch (detail) {
                    case "C1":
                        pack = 0x00;
                        break;
                    case "C2":
                        pack = 0x01;
                        break;
                    case "C3":
                        pack = 0x02;
                        break;
                    case "C4":
                        pack = 0x03;
                        break;
                    case "C5":
                        pack = 0x04;
                        break;
                }
                getSettingPackage_ReadData_Exe(pack);

            }

            Log.d(TAG, "onServiceConnected:   char:  " + g_Character_TX + "   ser:  " + mBluetoothLeService);
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
        int status = myModel.bypass;
        btSwitch.setOpened(status == 0 ? false : true);

        int display = myModel.display;
        Log.d("log", "initUI:取得数据 " + display);
        RadioButton display_rbt = (RadioButton) rgDisplayStatus.getChildAt(display);
        display_rbt.setChecked(true);
        //材料
        int material = myModel.coilSelect;
        String coil = "";
        switch (material) {
            case 0:
                coil = "镍丝";
                break;
            case 1:
                coil = "钛丝";
                break;
            case 2:
                coil = "不锈钢";
                break;
            case 3:
                coil = "百醇控制";
                break;
            case 4:
                coil = "手动TCR";
                break;
        }
        selectedMaterial.setText(coil);
        //口感
        int textured = myModel.texture;
        setShowText(textured);
        //记忆模式选择
        int memory = myModel.memory;
        RadioButton rb_M = (RadioButton) rgMemories.getChildAt(memory);
        rb_M.setChecked(true);
        //温度单位选择
        int temperatureUnit = myModel.temperatureUnit;
        RadioButton rb_TempUnit = (RadioButton) rgUnitTemperature.getChildAt(temperatureUnit);
        rb_TempUnit.setChecked(true);
        //功率焦耳切换
        jouleOrPower = myModel.JouleOrPower;
        RadioButton rb_jouleOrPower = (RadioButton) rgJoule.getChildAt(jouleOrPower);
        rb_jouleOrPower.setChecked(true);
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
        //
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
                    myModel_s.bypass = 1;
                    if (g_Character_TX != null) {
                        setUserDeviceSetting((byte) 0x0C, (byte) 0x01);
                        //                        setBypass((byte) 0x01);
                    }
                } else {
                    myModel_s.bypass = 0;
                    //                    setBypass((byte) 0x00);
                    setUserDeviceSetting((byte) 0x0C, (byte) 0x00);
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

                     String str = showAdjustTemperature.getText().toString();
                    int num = Integer.parseInt(str);
                    Log.d(TAG, "onStopTrackingTouch: "+str);
                    if (g_Character_TX != null) {
                        setUserDeviceSetting((byte) 0x06, num);
                    }
                    myModel.temperature = num;
                    myModel.save();
                    break;
                case R.id.seekBar_compensation_temperature:
                    MyModel myModel1 = MyModel.getMyModelForGivenName(detail);
                    String show_C = showCompensationTemperature.getText().toString();
                    int compensation_temperature_num = Integer.parseInt(show_C);
                    if (g_Character_TX != null) {
                        setUserDeviceSetting((byte) 0x07, compensation_temperature_num);
                    }
                    myModel1.temperature_c = compensation_temperature_num;
                    myModel1.save();
                    break;
                case R.id.seekBar_set_TCR:
                    MyModel myModel2 = MyModel.getMyModelForGivenName(detail);
                    int TCR_num = seekBar.getProgress() + 50;
                    myModel2.tcr = TCR_num;
                    if (g_Character_TX != null) {
                        Log.d(TAG, "onStopTrackingTouch: " + TCR_num);
                        setUserDeviceSetting((byte) 0x08, TCR_num);
                    }
                    myModel2.save();
                    break;
                case R.id.seekBar_set_power:
                    MyModel myModel3 = MyModel.getMyModelForGivenName(detail);
                    int power_num = seekBar.getProgress() + 50;
                    myModel3.power = power_num;//需要发送的数据
                    myModel3.save();
                    if (g_Character_TX != null) {
                        setPowerValueIn_Watts_Joule((byte) 0x0E, (byte) 0x01, power_num);
                    }
                    //                    setPowerValue(power_num / 10, power_num % 10);

                case R.id.seekBar_set_joule:
                    MyModel myModel4 = MyModel.getMyModelForGivenName(detail);

                    int joule_num = seekBar.getProgress() + 100;
                    if (g_Character_TX != null) {
                        setPowerValueIn_Watts_Joule((byte) 0x0E, (byte) 0x02, joule_num);
                    }
                    myModel4.joule = joule_num;
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

                            if (g_Character_TX != null) {
                                setUserDeviceSetting((byte) 0x03, (byte) 0x00);
                            }
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
                            if (g_Character_TX != null) {
                                setUserDeviceSetting((byte) 0x03, (byte) 0x01);
                            }
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
                        if (g_Character_TX != null) {
                            setUserDeviceSetting((byte) 0x01, (byte) 0x00);
                        }
                    } else if (i == R.id.rb_dis_right) {
                        myModel_C.display = 1;
                        if (g_Character_TX != null) {
                            setUserDeviceSetting((byte) 0x01, (byte) 0x01);
                        }
                    } else if (i == R.id.rb_dis_auto) {
                        myModel_C.display = 2;
                        if (g_Character_TX != null) {
                            setUserDeviceSetting((byte) 0x01, (byte) 0x02);
                        }
                    }
                    break;
                case R.id.rg_memories:
                    if (i == R.id.rb_M1) {
                        myModel_C.memory = 0;
                        setUserDeviceSetting((byte) 0x13, (byte) 0x00);
                    } else if (i == R.id.rb_M2) {
                        myModel_C.memory = 1;
                        setUserDeviceSetting((byte) 0x13, (byte) 0x01);
                    } else if (i == R.id.rb_M3) {
                        myModel_C.memory = 2;
                        setUserDeviceSetting((byte) 0x13, (byte) 0x02);
                    } else if (i == R.id.rb_M4) {
                        myModel_C.memory = 3;
                        setUserDeviceSetting((byte) 0x13, (byte) 0x03);
                    } else if (i == R.id.rb_M5) {
                        myModel_C.memory = 4;
                        setUserDeviceSetting((byte) 0x13, (byte) 0x04);
                    }
                    break;
                case R.id.rg_operation:     //操作模式设置
                    if (i == R.id.rb_primary) {
                        myModel_C.operation = 1;
                        setUserDeviceSetting((byte) 0x10, (byte) 0x01);
                    } else if (i == R.id.rb_senior) {
                        myModel_C.operation = 1;
                        setUserDeviceSetting((byte) 0x10, (byte) 0x01);
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
        if (m_Command == 0x58 && m_SecondCommand == 0x0F && m_Data.length == 7)
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
        switch (m_Command) {
            case 0x60://SettingPackage_AckReadData
                if (m_Data[5] == 0x01) {

                    final int waitTime = ((m_Data[8] & 0xff) << 8) | (m_Data[9] & 0xff);
                    Log.d(TAG, "Sys_YiHi_Protocol_RX_Porc: 等待时间：" + BinaryToHexString(m_Data) + "   " + waitTime);

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

                } else if (m_Data[5] == 0x04) {
                    //得到数据包  - SettingPackage        ---此处为合并的数据包
                    Log.d(TAG, "Sys_YiHi_Protocol_RX_Porc: 处理数据包:  " + BinaryToHexString(m_Data) + "  int:  " + (int) m_Data[8]);
                    MyModel configPackage = MyModel.getMyModelForGivenName(detail);
                    configPackage.bypass = (int) m_Data[8];
                    configPackage.JouleOrPower = (int) m_Data[9] - 1;

                    configPackage.operation = (int) m_Data[10];
                    configPackage.display = (int) m_Data[12];
                    configPackage.coilSelect = (int) m_Data[13];
                    configPackage.temperatureUnit = (int) m_Data[14];
                    //   功率值
                    int powerValue = (m_Data[15] & 0xff) << 24 | (m_Data[16] & 0xff) << 16 | (m_Data[17] & 0xff) << 8 | m_Data[18] & 0xff;
                    configPackage.power = powerValue;
                    //   焦耳值
                    int jouleValue = (m_Data[19] & 0xff) << 24 | (m_Data[20] & 0xff) << 16 | (m_Data[21] & 0xff) << 8 | m_Data[22] & 0xff;
                    configPackage.joule = jouleValue;
                    //   温度值
                    int tempValue = (m_Data[23] & 0xff) << 24 | (m_Data[24] & 0xff) << 16 | (m_Data[25] & 0xff) << 8 | m_Data[26] & 0xff;
                    configPackage.temperature = tempValue;
                    //   温度补偿值
                    int compensateTempValue = (m_Data[27] & 0xff) << 24 | (m_Data[28] & 0xff) << 16 | (m_Data[29] & 0xff) << 8 | m_Data[30] & 0xff;
                    configPackage.temperature_c = compensateTempValue;
                    //   TCR_Value值
                    int TCR_Value = (m_Data[31] & 0xff) << 24 | (m_Data[32] & 0xff) << 16 | (m_Data[33] & 0xff) << 8 | m_Data[34] & 0xff;
                    configPackage.tcr = TCR_Value;
                    //口感
                    configPackage.texture = (int) m_Data[35];
                    //记忆模式
                    configPackage.memory = (int) m_Data[36];
                    configPackage.save();
                    initUI();
                    Log.d(TAG, "powerValue: " + powerValue + "  jouleValue: " + jouleValue + "  tempValue:  " + tempValue + "  compensateTempValue:  " + compensateTempValue + "  TCR_Value:  " + TCR_Value);

                    //获得温度调节范围
                    getUserDeviceSetting((byte) 0x09);
                    mergerData = true;
                }
                break;
            case 0x58:

                if (m_Data[5] == 0x13 && jouleOrPower == 0) {
                    int setPowerNum = (m_Data[7] & 0xff) << 24 | (m_Data[8] & 0xff) << 16 | (m_Data[9] & 0xff) << 8 | m_Data[10] & 0xff;
                    seekBarSetPower.setProgress(setPowerNum - 50);
                } else if (m_Data[5] == 0x13 && jouleOrPower == 1) {
                    int setJouleNum = (m_Data[7] & 0xff) << 24 | (m_Data[8] & 0xff) << 16 | (m_Data[9] & 0xff) << 8 | m_Data[10] & 0xff;
                    seekBarSetJoule.setProgress(setJouleNum - 100);
                } else if (m_Data[5] == 0x09 && mergerDataOver) {
                    mergerDataOver = false;
                    Log.d(TAG, "temperInfo: 温度范围合并后： " + m_Data.length);
                    int tempMax_C = (m_Data[6] & 0xff) << 24 | (m_Data[7] & 0xff) << 16 | (m_Data[8] & 0xff) << 8 | m_Data[9] & 0xff;
                    int tempMin_C = (m_Data[10] & 0xff) << 24 | (m_Data[11] & 0xff) << 16 | (m_Data[12] & 0xff) << 8 | m_Data[13] & 0xff;
                    int tempMax_F = (m_Data[14] & 0xff) << 24 | (m_Data[15] & 0xff) << 16 | (m_Data[16] & 0xff) << 8 | m_Data[17] & 0xff;
                    int tempMin_F = (m_Data[18] & 0xff) << 24 | (m_Data[19] & 0xff) << 16 | (m_Data[20] & 0xff) << 8 | m_Data[21] & 0xff;
                    Log.d(TAG, "获得温度调节范围： temperInfo   "+tempMax_C+"  "+tempMin_C+"  "+tempMax_F+"  "+tempMin_F);
                    getUserDeviceSetting((byte) 0x0A);
                    mergerData = true;
                }else if(m_Data[5]==0x0A&&mergerDataOver){
                    int compensateTempMax_C = (m_Data[6] & 0xff) << 24 | (m_Data[7] & 0xff) << 16 | (m_Data[8] & 0xff) << 8 | m_Data[9] & 0xff;
                    int compensateTempMin_C = (m_Data[10] & 0xff) << 24 | (m_Data[11] & 0xff) << 16 | (m_Data[12] & 0xff) << 8 | m_Data[13] & 0xff;
                    int compensateTempMax_F = (m_Data[14] & 0xff) << 24 | (m_Data[15] & 0xff) << 16 | (m_Data[16] & 0xff) << 8 | m_Data[17] & 0xff;
                    int compensateTempMin_F = (m_Data[18] & 0xff) << 24 | (m_Data[19] & 0xff) << 16 | (m_Data[20] & 0xff) << 8 | m_Data[21] & 0xff;
                    Log.d(TAG, "获得补偿温度调节范围： temperInfo   "+compensateTempMax_C+"   "+compensateTempMin_C+"   "+compensateTempMax_F+"   "+compensateTempMin_F);
                    getUserDeviceSetting((byte) 0x0D);
                    mergerData = true;
                }else if(m_Data[5]==0x0D&&mergerDataOver){
                    int a = (m_Data[6] & 0xff) << 24 | (m_Data[7] & 0xff) << 16 | (m_Data[8] & 0xff) << 8 | m_Data[9] & 0xff;
                    int b = (m_Data[10] & 0xff) << 24 | (m_Data[11] & 0xff) << 16 | (m_Data[12] & 0xff) << 8 | m_Data[13] & 0xff;
                    int c = (m_Data[14] & 0xff) << 24 | (m_Data[15] & 0xff) << 16 | (m_Data[16] & 0xff) << 8 | m_Data[17] & 0xff;
                    int d = (m_Data[18] & 0xff) << 24 | (m_Data[19] & 0xff) << 16 | (m_Data[20] & 0xff) << 8 | m_Data[21] & 0xff;
                    Log.d(TAG, "功率焦耳切换时范围  temperInfo: "+ a+"   "+b+ "   "+"   "+c+"    "+d);
                }
                break;
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
        temp = (byte) (powerValue_x10 >> 8);
        m_Data[5] = temp;

        temp = (byte) ((powerValue_x10 << 8) >> 8);
        m_Data[6] = temp;

        temp = (MyUtils.int2OneByte(powerValue_x1));
        m_Data[7] = temp;
        m_length = 8;
        Sys_Proc_Charactor_TX_Send(m_Data, m_length);
    }

    //获得数据包
    private void getSettingPackage_ReadData_Exe(byte packNumber) {
        byte[] m_Data = new byte[32];
        int m_length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[3] = 0x01; //Device ID
        m_Data[2] = 0x04;
        m_Data[4] = 0x5F;
        m_Data[5] = packNumber;
        m_Data[6] = 0x01;
        m_length = 7;
        Sys_Proc_Charactor_TX_Send(m_Data, m_length);
    }

    //读取setting包的处理结果
    private void getSettingPackage_ReadData_GetResult() {
        mergerData = true;
        byte[] m_Data = new byte[32];
        int m_length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[3] = 0x01; //Device ID
        m_Data[2] = 0x02;
        m_Data[4] = 0x61;
        m_length = 5;
        Sys_Proc_Charactor_TX_Send(m_Data, m_length);
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

    //seekBar的调节，发送给设备
    public void setUserDeviceSetting(byte nn, int num) {
        byte[] m_Data_DeviceSetting = new byte[32];
        int m_Length = 0;
        m_Data_DeviceSetting[0] = 0x55;
        m_Data_DeviceSetting[1] = (byte) 0xFF;
        m_Data_DeviceSetting[3] = 0x01; //Device ID
        m_Data_DeviceSetting[2] = 0x07;
        m_Data_DeviceSetting[4] = 0x59;
        m_Data_DeviceSetting[5] = nn;
        byte p1 = (byte) (num >> 24 & 0xff);
        byte p2 = (byte) (num >> 16 & 0xff);
        byte p3 = (byte) (num >> 8 & 0xff);
        byte p4 = (byte) (num & 0xff);
        m_Data_DeviceSetting[6] = p1;
        m_Data_DeviceSetting[7] = p2;
        m_Data_DeviceSetting[8] = p3;
        m_Data_DeviceSetting[9] = p4;

        m_Length = 10;
        Sys_Proc_Charactor_TX_Send(m_Data_DeviceSetting, m_Length);
    }

    //设置功率或者焦耳模式下的功率值
    public void setPowerValueIn_Watts_Joule(byte nn, byte model, int num) {
        byte[] m_Data_DeviceSetting = new byte[32];
        int m_Length = 0;
        m_Data_DeviceSetting[0] = 0x55;
        m_Data_DeviceSetting[1] = (byte) 0xFF;
        m_Data_DeviceSetting[3] = 0x01; //Device ID
        m_Data_DeviceSetting[2] = 0x08;
        m_Data_DeviceSetting[4] = 0x59;
        m_Data_DeviceSetting[5] = nn;
        m_Data_DeviceSetting[6] = model;
        byte p1 = (byte) (num >> 24 & 0xff);
        byte p2 = (byte) (num >> 16 & 0xff);
        byte p3 = (byte) (num >> 8 & 0xff);
        byte p4 = (byte) (num & 0xff);
        m_Data_DeviceSetting[7] = p1;
        m_Data_DeviceSetting[8] = p2;
        m_Data_DeviceSetting[9] = p3;
        m_Data_DeviceSetting[10] = p4;
        m_Length = 11;
        Sys_Proc_Charactor_TX_Send(m_Data_DeviceSetting, m_Length);
    }

    //获取数据
    public void getUserDeviceSetting(byte nn) {
        //        mergerData = true;
        byte[] m_Data = new byte[32];
        int m_length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[3] = 0x01; //Device ID
        m_Data[2] = 0x03;
        m_Data[4] = 0x57;
        m_Data[5] = nn;
        m_length = 6;
        Sys_Proc_Charactor_TX_Send(m_Data, m_length);
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
}
