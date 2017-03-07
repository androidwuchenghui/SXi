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

/**
 * Created by ${Wu} on 2016/12/14.
 */

public class MaterialActivity extends AppCompatActivity {
    private static final String MATERIAL = "material";
    @Bind(R.id.check1)
    ImageView check1;
    @Bind(R.id.nickel)
    TextView nickel;
    @Bind(R.id.line_n)
    LinearLayout lineN;
    @Bind(R.id.check2)
    ImageView check2;
    @Bind(R.id.titanium)
    TextView titanium;
    @Bind(R.id.line_t)
    LinearLayout lineT;
    @Bind(R.id.check3)
    ImageView check3;
    @Bind(R.id.line_b)
    LinearLayout lineB;
    @Bind(R.id.check4)
    ImageView check4;
    @Bind(R.id.alcohol)
    TextView alcohol;
    @Bind(R.id.line_c)
    LinearLayout lineC;
    @Bind(R.id.check5)
    ImageView check5;
    @Bind(R.id.TRC)
    TextView TRC;
    @Bind(R.id.line_s)
    LinearLayout lineS;
    @Bind(R.id.btn_back)
    DarkImageButton btnBack;
    @Bind(R.id.connect_state)
    TextView connectState;
    @Bind(R.id.stainless_steel)
    TextView stainlessSteel;
    private int[] check_select = {0, 0, 0, 0, 0};
    private String title;
    private static final String TAG = "MaterialActivity";
    private BroadcastReceiver materialActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothLeService.ACTION_LAND_SUCCESS:
                    startActivity(new Intent(MaterialActivity.this, MainActivity.class));
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    startActivity(new Intent(MaterialActivity.this, MainActivity.class));
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material);
        ButterKnife.bind(this);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        initView();
        registerReceiver(materialActivityReceiver, makeBroadcastFilter());
    }


    private static IntentFilter makeBroadcastFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RX);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_LAND_SUCCESS);
        return intentFilter;
    }


    private void initView() {
        Intent intent = getIntent();
        title = intent.getStringExtra("title");

        MyModel model = MyModel.getMyModelForGivenName(title);
        int select = model.coilSelect;
        select_control(select);
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
            g_Character_TX = mBluetoothLeService.getG_Character_TX();
            if (mBluetoothLeService.getTheConnectedState() == 0) {
                connectState.setText("未连接");
            } else if (mBluetoothLeService.getTheConnectedState() == 2) {
                connectState.setText("已连接");
            }
            Log.d(TAG, "onServiceConnected:   char:  " + g_Character_TX + "   ser:  " + mBluetoothLeService);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("service", "onServiceDisconnected: " + "---------服务未连接-------------");
            mBluetoothLeService = null;
        }
    };

    @OnClick({R.id.line_n, R.id.line_t, R.id.line_b, R.id.line_c, R.id.line_s, R.id.btn_back})
    public void onClick(View view) {
        MyModel myModel = MyModel.getMyModelForGivenName(title);
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.line_n:
                select_control(0);
                intent.putExtra(MATERIAL, nickel.getText().toString());
                myModel.coilSelect = 0;
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x02, (byte) 0x00);
                }
                myModel.save();
                break;
            case R.id.line_t:
                select_control(1);
                intent.putExtra(MATERIAL, titanium.getText().toString());
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x02, (byte) 0x01);
                }
                myModel.coilSelect = 1;
                myModel.save();
                break;
            case R.id.line_b:
                select_control(2);

                intent.putExtra(MATERIAL, stainlessSteel.getText().toString());
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x02, (byte) 0x02);
                }
                myModel.coilSelect = 2;
                myModel.save();
                break;
            case R.id.line_c:
                select_control(3);
                intent.putExtra(MATERIAL, alcohol.getText().toString());
                myModel.coilSelect = 3;
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x02, (byte) 0x03);
                }
                myModel.save();
                break;
            case R.id.line_s:
                select_control(4);
                intent.putExtra(MATERIAL, TRC.getText().toString());
                if (g_Character_TX != null) {
                    setUserDeviceSetting((byte) 0x02, (byte) 0x04);
                }
                myModel.coilSelect = 4;
                myModel.save();
                break;
            case R.id.btn_back:
                finish();
                break;
        }
        setResult(RESULT_OK, intent);
        finish();
    }


    //控制选择
    private void select_control(int select) {

        for (int i = 0; i < check_select.length; i++) {
            check_select[i] = 0;
        }
        check_select[select] = 1;

        if (check_select[0] == 1) {
            check1.setVisibility(View.VISIBLE);
        } else {
            check1.setVisibility(View.INVISIBLE);
        }
        if (check_select[1] == 1) {
            check2.setVisibility(View.VISIBLE);
        } else {
            check2.setVisibility(View.INVISIBLE);
        }
        if (check_select[2] == 1) {
            check3.setVisibility(View.VISIBLE);
        } else {
            check3.setVisibility(View.INVISIBLE);
        }
        if (check_select[3] == 1) {
            check4.setVisibility(View.VISIBLE);
        } else {
            check4.setVisibility(View.INVISIBLE);
        }
        if (check_select[4] == 1) {
            check5.setVisibility(View.VISIBLE);
        } else {
            check5.setVisibility(View.INVISIBLE);
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        unregisterReceiver(materialActivityReceiver);
        mBluetoothLeService = null;
    }
}
