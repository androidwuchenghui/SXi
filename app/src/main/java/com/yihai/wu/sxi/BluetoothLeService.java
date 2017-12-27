/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yihai.wu.sxi;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.activeandroid.query.Delete;
import com.yihai.wu.appcontext.ConnectedBleDevices;
import com.yihai.wu.util.LogService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.yihai.wu.util.MyUtils.BinaryToHexString;
import static com.yihai.wu.util.MyUtils.HexToInt;
import static com.yihai.wu.util.MyUtils.Sys_BCD_To_HEX;
import static com.yihai.wu.util.MyUtils.byteMerger;
import static com.yihai.wu.util.MyUtils.hexStringToString;
import static com.yihai.wu.util.MyUtils.intToBytes;
import static com.yihai.wu.util.MyUtils.intToBytes2;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@SuppressWarnings("unused")
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    ExecutorService pool = Executors.newFixedThreadPool(4);
    /*mBluetoothManager=蓝牙管理器.在initialize()创建.*/
    private BluetoothManager mBluetoothManager;
    /*mBluetoothAdapter=蓝牙适配器,在initialize()创建.*/
    private BluetoothAdapter mBluetoothAdapter;
    /*mBluetoothDeviceAddress=当前已经与本机建立了连接的BLE设备地址.*/
    private String mBluetoothDeviceAddress;
    /*mBluetoothGatt=管理BLE内核中的GATT协议栈.用到BluetoothGatt,相应的也要用到BluetoothGattCallback.*/
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public void setCount(int count) {
        this.count = count;
    }

    private int count = 0;

    // -----

    public void setCanSendPicture(boolean canSendPicture) {
        this.canSendPicture = canSendPicture;
    }

    private boolean canSendPicture = false;

    /*用于广播的消息.*/

    public final static String ACTION_SEND_PROGRESS =
            "com.example.bluetooth.le.PICTURE_PROGRESS";

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String ACTION_DATA_COMMIT_PASSWORD_RESULT =
            "com.example.bluetooth.le.PASSWORD";
    public final static String ACTION_DATA_RX =
            "com.example.bluetooth.le.RX";
    public final static String ACTION_LAND_SUCCESS =
            "com.example.bluetooth.le.LandingSuccess";
    public final static String ACTION_LANDING =
            "com.example.bluetooth.le.LANDING";

    public final static String ACTION_LOGIN_FAILED =
            "com.example.bluetooth.le.LoginFailed";
    public final static String ACTION_NOT_BELONG =
            "com.example.bluetooth.le.Does not belong to the company";
    public final static String ACTION_THREE_SUBMISSION_FAILED =
            "com.example.bluetooth.le.Three submission failed";


    /*自定义广播消息:一个特性的数据发送完成.*/
    public final static String ACTION_BLE_DATA_TX_OK =
            "com.example.bluetooth.le.ACTION_BLE_DATA_TX_OK";

    //public final static UUID UUID_HEART_RATE_MEASUREMENT =
    //        UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    /*指定收发数据的服务和特性的UUID.*/
    public final UUID g_UUID_Service_SendData =
            UUID.fromString(MyGattAttributes.C_UUID_Service_SendDataToDevice);
    public final UUID g_UUID_Charater_SendData =
            UUID.fromString(MyGattAttributes.C_UUID_Character_SendDataToDevice);
    public final UUID g_UUID_Service_ReadData =
            UUID.fromString(MyGattAttributes.C_UUID_Service_ReadDataFromDevice);
    public final UUID g_UUID_Charater_ReadData =
            UUID.fromString(MyGattAttributes.C_UUID_Character_ReadDataFromDevice);
    public final UUID g_UUID_Service_DeviceConfig =
            UUID.fromString(MyGattAttributes.C_UUID_Service_DeviceConfig);
    public final UUID g_UUID_Charater_DeviceName =
            UUID.fromString(MyGattAttributes.C_UUID_Character_Device_Name);
    public final UUID g_UUID_Charater_CustomerID =
            UUID.fromString(MyGattAttributes.C_UUID_Character_Device_CustomerID);
    public final UUID g_UUID_Service_Password =
            UUID.fromString(MyGattAttributes.C_UUID_Service_Password);
    public final UUID g_UUID_Charater_Password =
            UUID.fromString(MyGattAttributes.C_UUID_Character_Password_C1);
    public final UUID g_UUID_Charater_Password_C2 =
            UUID.fromString(MyGattAttributes.C_UUID_Character_Password_Notify);
    public final UUID g_UUID_Charater_Baud_Rate =
            UUID.fromString(MyGattAttributes.C_UUID_Character_Device_CommBaudRate);

    private BluetoothGattCharacteristic g_Character_TX;
    private BluetoothGattCharacteristic g_Character_DeviceName;
    private BluetoothDevice device;

    // Device scan callback. 扫描回掉
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
                    Log.e(TAG, "onLeScan: " + device.getAddress() + "    lastAddress:  " + lastAddress);
                    if (device.getAddress().toString().equals(lastAddress)) {
                        Log.e(TAG, "onLeScan: stopLeScan ");
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        keepSearch = false;
                        connect(lastAddress);
                    }
                }
            };

    private Thread scanThread = new Thread() {
        @Override
        public void run() {
            super.run();
            Log.e(TAG, "ServiceOnConnectionStateChange:   执行   后台搜索。。。   " + mBluetoothAdapter);

            mBluetoothAdapter.startLeScan(mLeScanCallback);

        }
    };

    private Thread serviceThread = new Thread() {
        @Override
        public void run() {
            List<BluetoothGattService> supportedGattServices = getSupportedGattServices();
            displayGattServices(supportedGattServices);
            super.run();
        }
    };

    private Thread sendPixelThread = new Thread(){
        @Override
        public void run() {
            super.run();
            sendData(count);
        }
    };

    private boolean keepSearch = true;
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    //连接回掉
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        /*======================================================================
         *Purpose:本机与外部BLE设备连接或者断开时的处理.
         *Remark:使用了BluetoothGatt类时,必须重写的BluetoothGattCallback函数.
         *======================================================================
         */
        //连接状态的改变
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            String intentAction = null;
            Log.e(TAG, "  连接回调，连接状态的变化  ServiceOnConnectionStateChange:   gatt" + gatt + "   status:  " + status + "   newState:   " + newState + " = 2");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;//连接状态
                broadcastUpdate(intentAction);
                // Attempts to discover services after successful connection.
                Log.e(TAG, "ServiceOnConnectionStateChange: " + "     连接成功   ");
                mBluetoothGatt.discoverServices();
                // 提交密码
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "ServiceOnConnectionStateChange:     断开连接    " + disConnectByMyself);
                close();

                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;//断开状态
                ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
                if (connectedDevice != null) {
                    connectedDevice.isConnected = false;
                    connectedDevice.save();
                }
                //
                if (status == 19) {
                    disConnectByMyself = true;
                    broadcastUpdate(ACTION_LOGIN_FAILED);
                    return;
                }
                //出现133 错误
                if (status == 133) {
                    Log.e(TAG, "onConnectionStateChange: 133 出现了    " + connectedAddress);
                    connect(connectedAddress);
                    return;
                }
                broadcastUpdate(intentAction);

                if (!disConnectByMyself) {
                    keepSearch = true;
                    serviceScan();
                } else {
                    disConnectByMyself = true;
                    broadcastUpdate(ACTION_LOGIN_FAILED);
                }

            }

        }

        // New services discovered
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.e(TAG, "onServicesDiscovered:  发现服务   status:  " + status + "    services:    " + gatt.getServices());
            disconStatus = status;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED); //发现服务

                pool.execute(serviceThread);
            } else if (status == 129) {
                                Log.e(TAG, "status = 129: -----出现129错误 ");
                //                mBluetoothAdapter.disable();
                //                Timer single_timer = new Timer();
                //                single_timer.schedule(new TimerTask() {
                //                    @Override
                //                    public void run() {
                //                        mBluetoothAdapter.enable();
                //                        broadcastUpdate("129");
                //                    }
                //                }, 2500);
                //            }
                //            if (status == 129) {
                //                                broadcastUpdate(ACTION_LOGIN_FAILED);
            }
        }

        /*
         *Remark:当BluetoothLeService执行了BluetoothGatt类的readCharacteristic(),会触发这个函数.
         */
        // Result of a characteristic read operation
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.e(TAG, "onCharacteristicRead: " + characteristic.getUuid() + ">>>：" + g_UUID_Charater_CustomerID);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                //                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

                if (characteristic.getUuid().equals(g_UUID_Charater_CustomerID)) {

                    byte[] data = characteristic.getValue();
                    readID = BinaryToHexString(data);
                    //     产品识别码
                    Log.e(TAG, "onCharacteristicRead: "+" 识别码："+readID );
                    if (readID.equals("0601")) {
                        //确认为本公司产品之后，产生6位随机密码
                        sb = new StringBuilder();
                        for (int i = 0; i < 6; i++) {
                            int random = (int) (Math.random() * 100);
                            if (random % 3 == 0) {
                                //number
                                int n = (int) (Math.random() * 10);
                                sb.append(n);
                            } else if (random % 3 == 1) {
                                //big letter
                                int bl = (int) (Math.random() * 26) + 65;
                                sb.append((char) bl);
                            } else {
                                // small letter
                                int sl = (int) (Math.random() * 26) + 97;
                                sb.append((char) sl);
                            }
                        }
                        changeTo = DEFAULT_PASSWORD + sb.toString();
                        Log.e(TAG, "onReceive:  changeTo " + changeTo);
                        commit_amount = 2;
                        reChange++;
                        commitPassword(changeTo);
                    } else {
                        //断开连接(流程结束)
                        disConnectByMyself = true;
                        disconnect();
                        new Delete().from(ConnectedBleDevices.class).execute();
                        broadcastUpdate(BluetoothLeService.ACTION_NOT_BELONG);
                    }
                }
            }
        }

        /*
         *Remark:当BluetoothLeService执行了BluetoothGatt类的writeCharacteristic(),就
         *       会触发这个函数.
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            // TODO Auto-generated method stub
            if (g_UUID_Charater_SendData.equals(characteristic.getUuid())) {
                Log.e(TAG, "write characteristic=Done");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    broadcastUpdate(ACTION_BLE_DATA_TX_OK);
                }
            }
            //super.onCharacteristicWrite(gatt, characteristic, status);
        }

        /*======================================================================
         *Purpose:某个特性的状态改变了,就会触发这个函数.
         *Remark:如果接收到BLE设备发送过来的数据,并且接收数据的特性的属性已经设置为通知的话,接收完成后
         *       也会触发这个函数.
         *======================================================================
         *  如果notificaiton方式对于某个Characteristic是enable的，那么当设备上的这个Characteristic改变时，
         *  手机上的onCharacteristicChanged() 回调就会被促发。如下所示：
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

                        Log.e(TAG, "onCharacteristicChanged: " + characteristic.getUuid() + "  >>>  " + g_UUID_Charater_Password_C2);
            if (characteristic.getUuid().equals(g_UUID_Charater_Password_C2)) {
                //                broadcastUpdate(ACTION_DATA_COMMIT_PASSWORD_RESULT, characteristic);

                byte[] data = characteristic.getValue();
                final String reply = BinaryToHexString(data);
                //                Log.e(TAG, "onCharacteristicChanged:  reply : " + reply);
                pool.execute(new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (commit_amount == 0) {
                            //第一次提交密码的处理
                            handlePasswordCallbacks(reply);
                        } else if (commit_amount == 1) {
                            //      处理提交亿海产品密码后的返回
                            handleSecondPasswordCallback(reply);
                        } else if (commit_amount == 2) {
                            //         处理修改密码的返回

                            handleChangePassword(reply);
                        } else if (commit_amount == 3) {
                            handleLastPassword(reply);
                        }
                    }
                });

            } else if (characteristic.getUuid().equals(g_UUID_Charater_ReadData)) {

                Log.e(TAG, "onCharacteristicChanged:  RX    " + BinaryToHexString(characteristic.getValue()));
                broadcastRxUpdate(ACTION_DATA_RX, characteristic);
//                if (canSendPicture) {
//                    byte[] value = characteristic.getValue();
//                    Log.e(TAG, "sendPicture: "+BinaryToHexString(value));
//                    Sys_YiHi_Protocol_RX_Porc(value);
//                }
            }

        }
    };
    private String readID;
    private int disconStatus;

    public void setLastAddress(String lastAddress) {
        this.lastAddress = lastAddress;
    }

    private String lastAddress;

    public BluetoothGattCharacteristic getG_Character_Baud_Rate() {
        return g_Character_Baud_Rate;
    }

    private BluetoothGattCharacteristic g_Character_Baud_Rate;


    public void setDisConnectByMyself(boolean disConnectByMyself) {
        this.disConnectByMyself = disConnectByMyself;
    }

    private boolean disConnectByMyself;


    /*======================================================================
     *Purpose:向外广播一个字符串信息.
     *======================================================================
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastRxUpdate(final String action,
                                   final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        Bundle bundle = new Bundle();
        bundle.putByteArray("byteValues", data);

        intent.putExtra(EXTRA_DATA, bundle);
        sendBroadcast(intent);
    }

    private void broadcastUpdateProgress( String action) {
        final Intent intent = new Intent(action);
        intent.putExtra("sendProgress",count);
        sendBroadcast(intent);
    }
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);


        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        /*
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.e(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.e(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.e(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else
        */

        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X", byteChar));

            intent.putExtra(EXTRA_DATA, stringBuilder.toString());
            //intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
        }
        sendBroadcast(intent);
    }

    /*======================================================================
     *Purpose:把这个类的引用传递给与这个类绑定的窗口.
     *Parameter:
     *Return:
     *Remark:绑定时的标准流程.
     *======================================================================
     */
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(bluetoothLeServiceReceiver, makeMainBroadcastFilter());
        sharedPreferences = getSharedPreferences("lastConnected", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        lastAddress = sharedPreferences.getString("address", null);
        initialize();
        registerReceiver(serviceReceiver, makeBroadcastFilter());
        Log.e(TAG, "serviceOnCreate:     lastAddress  " + lastAddress);
    }

    private IntentFilter makeBroadcastFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RX);
        return intentFilter;
    }
    /****************************************接受广播***********************************************************/
    private boolean wait = false;
    private byte[] merger_bytes;
    private boolean get_software_version = false;
    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){
                case ACTION_DATA_RX:
                    Bundle bundle = intent.getBundleExtra(EXTRA_DATA);
                    byte[] data = bundle.getByteArray("byteValues");
                    String s = BinaryToHexString(data);
                    Log.e(TAG, "sendPicture: "+s);
                    if(canSendPicture) {
                        Sys_YiHi_Protocol_RX_Porc(data);
                    }

                    int counts = 0;
                    if (data.length > 3) {
                        counts = (data[2] & 0xff) + 3;
                    }

                    if (wait) {
                        merger_bytes = byteMerger(merger_bytes, data);
                        Sys_YiHi_Protocol_RX_Porc_Do(merger_bytes);
                        merger_bytes = null;
                        wait = false;
                    }

                    if (data[0] == (byte) 0x55 && data[1] == (byte) 0xFF && counts <= 20) {

                        Sys_YiHi_Protocol_RX_Porc_Do(data);

                    } else if (data[0] == (byte) 0x55 && data[1] == (byte) 0xFF && counts > 20) {
                        merger_bytes = data;
                        wait = true;
                    }

                    break;
            }
        }
    };

    /*======================================================================
         *Purpose:与这个类进行绑定的窗口调用了bindService()时,就会触发这个函数.
         *Parameter:
         *Return:
         *Remark:绑定时的标准流程.
         *======================================================================
         */
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind: " + "BindService");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind: service 取消绑定");
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        unregisterReceiver(bluetoothLeServiceReceiver);
        close();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        keepSearch = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        unregisterReceiver(serviceReceiver);
        Log.e(TAG, "onServiceDestroy: 后台服务关闭 ");
        Intent stateService =  new Intent (this, LogService.class);
        stopService(stateService);
    }

    /*mBinder=要交给与这个类绑定的ServiceConnection.*/
    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {

        connectedAddress = address;
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.

        /*如果用户现在制定的BLE设备地址与先前的不一样,就会到这里执行.*/
        /*获取指定了MAC地址的外部蓝牙设备.*/
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        /*如果参数2为true,指定的设备一旦进入有效连接范围就自动连接.*/
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.e(TAG, "Trying to create a new connection.    发起连接   getBluetoothGatt:    " + mBluetoothGatt);
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        broadcastUpdate(ACTION_LANDING);
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        Log.e(TAG, "disconnect: 断开连接");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {

        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;

    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        Log.e(TAG, "readCharacteristic: 读特征值    gatt:  " + mBluetoothGatt + "    " + characteristic.getUuid());
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * 功能:执行指定的特性的写操作.
     * 备注:这个函数会触发BluetoothGattCallback的
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        //String m_str;
        int m_Property = characteristic.getProperties();
        Log.e(TAG, "writeCharacteristic:  进行了写的动作-- " + m_Property);
        //    	String m_str=String.format("%1$08x", m_Property);
        //    	Log.e(TAG, "wc property=***********"+m_str);
        if ((m_Property | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            mBluetoothGatt.writeCharacteristic(characteristic);
            //Log.e(TAG, "write characteristic");

        }
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     *                       如果设备主动给手机发信息，则可以通过notification的方式，这种方式不用手机去轮询地读设备上的数据。
     *                       手机可以用如下方式给设备设置notification功能。
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);


        //FFC2特征值，获得通知
        //        if(g_UUID_Charater_Password_C2.equals(characteristic.getUuid())){
        //            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
        //                    UUID.fromString(MyGattAttributes.C_UUID_Character_Password_Notify));
        //            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        //            mBluetoothGatt.writeDescriptor(descriptor);
        //        }


        // This is specific to Heart Rate Measurement.
        /*
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        */
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    public int getTheConnectedState() {
        return mConnectionState;
    }

    public BluetoothGattCharacteristic getG_Character_TX() {
        return g_Character_TX;
    }

    public BluetoothGattCharacteristic getG_Character_DeviceName() {
        return g_Character_DeviceName;
    }

    private final BroadcastReceiver bluetoothLeServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e("serviceBroadReceiver", "onReceive: " + action);
            switch (action) {
                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    Log.e(TAG, "reConnectBluetooth: ");
                    BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    Log.e(TAG, "screenLock:    关闭屏幕  ");   //屏幕熄灭后需要断开连接，关闭蓝牙
                    disConnectByMyself = true;
                    disconnect();
                    break;
                case Intent.ACTION_SCREEN_ON:
                    Log.e(TAG, "screenLock:    屏幕亮起  ");
                    //                    mBluetoothAdapter.enable();
                    break;


            }
        }
    };

    private static IntentFilter makeMainBroadcastFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);//  clock the screen
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);//  clock the screen
        return intentFilter;
    }

    public void setBluetoothDevice(BluetoothDevice connectedDevice) {
        device = connectedDevice;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private BluetoothGattCharacteristic g_Character_RX;
    private BluetoothGattCharacteristic g_Character_CustomerID;
    private BluetoothGattCharacteristic g_Character_Password;
    private BluetoothGattCharacteristic g_Character_Password_Notify;
    private String connectedAddress;
    private StringBuilder sb;
    private String changeTo;
    private int reChange = 0;

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
        //        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            if (g_UUID_Service_SendData.equals(gattService.getUuid())) {
                Log.e(TAG, "Service TX found");
                m_b_Check_TX = true;
            } else {
                m_b_Check_TX = false;
            }
            if (g_UUID_Service_ReadData.equals(gattService.getUuid())) {
                m_b_Check_RX = true;
            } else {
                m_b_Check_RX = false;
            }
            if (g_UUID_Service_DeviceConfig.equals(gattService.getUuid())) {
                m_b_Check_DeviceName = true;
            } else {
                m_b_Check_DeviceName = false;
            }

            if (g_UUID_Service_Password.equals(gattService.getUuid())) {
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
                    if (g_UUID_Charater_SendData.equals(gattCharacteristic.getUuid())) {
                        Log.e(TAG, "displayGattServices   TX found :  " + gattCharacteristic.getUuid());
                        g_Character_TX = gattCharacteristic;
                        m_b_Check_TX = false;
                    }
                }
                /*???????????????????.*/
                if (m_b_Check_RX == true) {
                    if (g_UUID_Charater_ReadData.equals(gattCharacteristic.getUuid())) {
                        g_Character_RX = gattCharacteristic;
                        m_b_Check_RX = false;
                        m_b_ConfigEnable = true;
                    }
                }
                /*????豸?????????.*/
                if (m_b_Check_DeviceName == true) //?ж??????????豸????????????
                {
                    if (g_UUID_Charater_DeviceName.equals(gattCharacteristic.getUuid())) {
                        g_Character_DeviceName = gattCharacteristic;//执行功。获得FF91特征。。
                        Log.e(TAG, "displayGattServices: 设备名特征值----" + g_Character_DeviceName.getUuid());
                        //                        Sys_SetMyDeviceName("BleDevice");
                    } else if (g_UUID_Charater_CustomerID.equals(gattCharacteristic.getUuid())) {
                        g_Character_CustomerID = gattCharacteristic;
                        Log.e(TAG, "displayGattServices: 产品识别码特征值：  " + g_Character_CustomerID.getUuid());
                    } else if (g_UUID_Charater_Baud_Rate.equals(gattCharacteristic.getUuid())) {
                        g_Character_Baud_Rate = gattCharacteristic;
                        Log.e(TAG, "displayGattServices: 波特率特征值：" + g_Character_Baud_Rate.getUuid());
                    }

                }
                if (m_b_Check_Password == true) {
                    if (g_UUID_Charater_Password.equals(gattCharacteristic.getUuid())) {
                        g_Character_Password = gattCharacteristic;
                    } else if (g_UUID_Charater_Password_C2.equals(gattCharacteristic.getUuid())) {
                        g_Character_Password_Notify = gattCharacteristic;
                    }

                }

            }//for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)-----------
            //            mGattCharacteristics.add(charas);
            //            gattCharacteristicData.add(gattCharacteristicGroupData);
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
            Log.e(TAG, "displayGattServices:  RX: " + g_Character_RX.getUuid());
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                setCharacteristicNotification(
                        g_Character_RX, true);
            }
        }

        if (g_Character_Password_Notify != null) {
            final int charaProp = g_Character_Password_Notify.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                setCharacteristicNotification(
                        g_Character_Password_Notify, true);
            }
        }
        //提交密码
        ConnectedBleDevices usedDevice = ConnectedBleDevices.getConnectInfoByAddress(connectedAddress);


        //判断之前是否记录过设备A的密码
        if (usedDevice != null) {
            String password = usedDevice.password;
            Log.e(TAG, "password: 提交保存的密码:  " + password);
            commit_amount = 0;
            commitPassword(password + password);

        } else {
            Log.e(TAG, "password: 提交默认密码---  000000");
            commit_amount = 0;
            ConnectedBleDevices current = new ConnectedBleDevices();
            current.deviceName = mDeviceName;
            current.deviceAddress = connectedAddress;
            current.password = DEFAULT_PASSWORD;
            current.save();
            Log.e(TAG, "displayGattServices: " + ConnectedBleDevices.getConnectInfoByAddress(connectedAddress).deviceName);
            commitPassword(DEFAULT_PASSWORD + DEFAULT_PASSWORD);
        }

    }//displayGattServices()------------------------END

    private int commit_amount = 100;
    private String mDeviceName;
    private static final String DEFAULT_PASSWORD = "000000";
    private static final String YIHI_DEFAULT_PASSWORD = "135246";

    public void setmDeviceName(String mDeviceName) {
        this.mDeviceName = mDeviceName;
    }

    public void commitPassword(String password) {
        Log.e(TAG, "password: 提交密码： 特征值为：   " + g_Character_Password + "   密码为：  " + password);
        byte[] m_Data = password.getBytes();
        if (g_Character_Password != null) {
            g_Character_Password.setValue(m_Data);
            writeCharacteristic(g_Character_Password);
        }
    }

    //首次连接的判断
    private void isFirst() {

        ConnectedBleDevices theDevice = ConnectedBleDevices.getConnectInfoByAddress(connectedAddress);
        Log.e(TAG, "passWordisFirst: " + theDevice.isFirst + "  ***");
        boolean isFirstRun = theDevice.isFirst;
        if (isFirstRun || (step3 || step5) && commit_amount != 3) {
            //      获取识别码
            Log.e(TAG, " passWord    是第一次登陆 获取识别码: ");

            readCharacteristic(g_Character_CustomerID);//读取产品识别码

        } else {
            //通过用户自己保存的密码，并且不是第一次进入，允许执行正常通信￥￥（流程OK）
            Log.e(TAG, "handleChangePassword: 用户通过保存的密码进入      over");
            theDevice.isConnected = true;
            theDevice.save();
            //发出登陆成功的广播
            broadcastUpdate(ACTION_LAND_SUCCESS);

            getConnectedDeviceRealName();
        }

    }

    private boolean step3 = false;
    private boolean step5 = false;

    //      ----首次连接提交第一次密码
    private void handlePasswordCallbacks(String reply) {
        ConnectedBleDevices devices = ConnectedBleDevices.getConnectInfoByAddress(connectedAddress);
        Log.e(TAG, "handlePasswordCallbacks: " + "处理返回的密码状态:" + reply);
        if (reply != null) {
            //提交密码后判断回馈的信息
            switch (reply) {
                case "00":
                    ////////    此处因之前芯片6.0 以上修改密码后无法连接
  /*

                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        broadcastUpdate(ACTION_LAND_SUCCESS);
                        devices.isConnected = true;
                        devices.save();

                        getConnectedDeviceRealName();
                        break;
                    }
*/
                    Log.e(TAG, "handlePasswordCallbacks: 密码提交---正确---");
                    if (devices.password.equals(DEFAULT_PASSWORD)) {
                        step3 = true;
                    }
                    //判断是不是第一次连接
                    isFirst();
                    break;
                case "01":
                    ////////////////////////////
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        broadcastUpdate(ACTION_LOGIN_FAILED);
                        break;
                    }
                    Log.e(TAG, "displayData: 默认设备密码提交错误--- 开始提交产品密码");
                    step3 = false;
                    commit_amount = 1;
                    devices.password = YIHI_DEFAULT_PASSWORD;
                    devices.save();
                    commitPassword(YIHI_DEFAULT_PASSWORD + YIHI_DEFAULT_PASSWORD);

                    break;

            }
        }
    }

    //再次提交亿海产品默认密码返回结果的处理   5.
    private void handleSecondPasswordCallback(String str) {
        Log.e(TAG, "handleSecondPasswordCallback: " + "提交默认产品密码返回处理  " + str);
        ConnectedBleDevices devices = ConnectedBleDevices.getConnectInfoByAddress(connectedAddress);
        switch (str) {
            case "00"://正确
                if (devices.password.equals(YIHI_DEFAULT_PASSWORD)) {
                    step5 = true;
                }
                isFirst();

                break;
            case "01"://连接失败，删除数据，断开连接(流程结束)
                disConnectByMyself = true;
                disconnect();
                broadcastUpdate(ACTION_LOGIN_FAILED);

                break;
        }
    }

    //修改密码的处理
    private void handleChangePassword(String str) {
        switch (str) {
            case "01":
                //提交错误，修改失败
                Log.e(TAG, "handleChangePassword:修改失败 " + reChange);
                if (reChange == 3) {
                    disConnectByMyself = true;
                    disconnect();
                    broadcastUpdate(BluetoothLeService.ACTION_THREE_SUBMISSION_FAILED);

                } else {
                    reChange++;
                    commit_amount = 2;
                    commitPassword(changeTo);
                }
                break;
            case "02":
                //修改成功,保存密码，并进行最终连接
                ConnectedBleDevices changedPassword = ConnectedBleDevices.getConnectInfoByAddress(connectedAddress);
                String lastPassword = sb.toString();
                changedPassword.password = lastPassword;
                changedPassword.isConnected = true;
                changedPassword.isFirst = false;
                changedPassword.save();

                commit_amount = 3;
                step3 = false;
                step5 = false;
                //连接成功，执行正常通信￥￥ （流程OK）
                Log.e(TAG, "handleChangePassword: 修改密码成功     提交最终正确密码。。。");

                commitPassword(lastPassword + lastPassword);

                break;
        }

    }

    private void handleLastPassword(String reply) {
        switch (reply) {
            case "00":
                Log.e(TAG, "handleLastPassword: " + "OK  -->  isFirst");
                isFirst();
                break;
            case "01":

                break;
        }
    }


    public void serviceScan() {
        pool.execute(scanThread);
    }

    //设置图片数据
    private byte[] pixel_data;

    public void setPixel_data(byte[] pixel_data) {
        this.pixel_data = pixel_data;
    }

    public void sendData(int num) {
//        Log.e(TAG, "sendData: begin~~~~~  ");
        sendData1(num);
        sendData2(num);
        sendData3(num);
        sendData4(num);
    }


    //发送壁纸的动作
    private void sendData1(int num) {

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] m_Data = new byte[21];
        int m_Length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[2] = 0x3B;
        m_Data[3] = 0x01;
        m_Data[4] = 0x6C;
        m_Data[5] = 0x0F;

        //    序号   M M
        byte[] bytes1 = intToBytes2(num);
        m_Data[6] = bytes1[1];
        m_Data[7] = bytes1[2];
        m_Data[8] = bytes1[3];

        //    本次发送数据包的长度   P P
        byte[] bytes = intToBytes(50);
        m_Data[9] = bytes[0];
        m_Data[10] = bytes[1];

        //有效数据  V V
        for (int i = 0; i < 9; i++) {
            m_Data[11 + i] = pixel_data[i + num * 50];
        }

//                Log.e(TAG, "sendData1:  "+BinaryToHexString(m_Data));
        m_Length = 20;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);

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
//        Log.e(TAG, "sendData:  "+BinaryToHexString(m_MyData));
        g_Character_TX.setValue(m_MyData);
        writeCharacteristic(g_Character_TX);
    }


    private void sendData2(int num) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] m_Data = new byte[32];
        int m_Length = 0;
        for (int i = 0; i < 20; i++) {
            m_Data[i] = pixel_data[i + 9 + num * 50];
        }
        m_Length = 20;
//        Log.e(TAG, "sendData2:  "+BinaryToHexString(m_Data));
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

    private void sendData3(int num) {

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] m_Data = new byte[32];
        int m_Length = 0;
        for (int i = 0; i < 20; i++) {
            m_Data[i] = pixel_data[i + 29 + num * 50];
        }
        m_Length = 20;
//        Log.e(TAG, "sendData3:  "+BinaryToHexString(m_Data));
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

    private void sendData4(int num) {

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        count++;

        byte[] m_Data = new byte[32];
        int m_Length = 0;
        m_Data[0] = pixel_data[num * 50 + 49];
        int sum = 0;
        for (int i = 0; i < 50; i++) {
            int onByte = pixel_data[i + num * 50] & 0xFF;
            sum += onByte;
        }
        //       校验数据---
//        Log.e(TAG, "sendData   out:  " + "   校验： " + sum);
        byte yy = (byte) (sum & 0xFF);
        m_Data[1] = yy;
        m_Length = 2;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
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
        m_Command = m_Data[(m_Index + 5)];
        switch (m_Command) {
            case 0x0E:
                Log.e(TAG, "sendPicture: "+BinaryToHexString(m_Data)  +"    "+g_Character_TX+"  count: "+count+"  piex: "+pixel_data.length);
                pool.execute(sendPixelThread);
                break;
            case 0x10:
                Log.e(TAG, "sendData:   back " + BinaryToHexString(m_Data) + "     count :  " + count);
                int num = count-1;
                editor.putInt("count",num);
                editor.commit();
               /* dialogView.setProgress((double) count * 100 / 2304);

                if (count >= 2304) {
                    Log.e(TAG, "sendData: " + "   over     over   over   over   over   over   over   over   over");
                    dialogView.dismiss();
                    count = 0;
                    sendPicture = false;
                    return;
                }*/

                broadcastUpdateProgress(ACTION_SEND_PROGRESS);

                if(count>=2304){
                    count =0;
                    canSendPicture = false;
                    return;
                }
//                sendData(count);
                pool.execute(sendPixelThread);
                break;
        }
    }


    //TX传值
    //获得产品名称
    private void getConnectedDeviceRealName() {
        Log.e(TAG, "getConnectedDeviceRealName: " + " 获得产品名称");
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

    //对返回的byte[]进行处理
    private void
    Sys_YiHi_Protocol_RX_Porc_Do(byte[] m_Data) {
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
            case 0x02:
                String s = BinaryToHexString(m_Data);
                //                Log.e(TAG, "scanReceive: " + s + "   data_length " + m_Data.length);
                String usefulData = s.substring(10, m_Data.length * 2);
                String realName = hexStringToString(usefulData);
                ConnectedBleDevices connectedBleDevice = ConnectedBleDevices.getConnectInfoByAddress(connectedAddress);
                connectedBleDevice.realName = realName;
                connectedBleDevice.save();
                //                Log.e(TAG, "Sys_YiHi_Protocol_RX_Porc: name:  " + s + "  r: " + realName);
                //                mHandler.postDelayed(new Runnable() {
                //                    @Override
                //                    public void run() {
                getConnectedDeviceID();

                //                    }
                //                }, 50);
                break;
            case 0x04:

                String AckDevice_ID = BinaryToHexString(m_Data);
                String count = AckDevice_ID.substring(4, 6);
                int i1 = HexToInt(count);
                String AckDevice_ID_Behind = AckDevice_ID.substring(10);
                String realID = hexStringToString(AckDevice_ID_Behind);
                Log.e(TAG, "Sys_YiHi_Protocol_RX_Porc: id:    "+count+" int:   "+i1 +"  behind: "+AckDevice_ID_Behind+"   "+ AckDevice_ID + "  r: " + realID+"   length: "+m_Data.length);

                ConnectedBleDevices deviceID = ConnectedBleDevices.getConnectInfoByAddress(connectedAddress);
                deviceID.deviceID = realID;
                deviceID.save();

                //                mHandler.postDelayed(new Runnable() {
                //                    @Override
                //                    public void run() {
                getConnectedDeviceProtocol_Version();
                //                    }
                //                }, 50);
                break;

            case 0x13:

                int g_PowerValue_x10 = Sys_BCD_To_HEX(m_Data[(m_Index + 5)]);
                int g_PowerValue_x1 = Sys_BCD_To_HEX(m_Data[(m_Index + 6)]);
                String Protocol_Vision = BinaryToHexString(m_Data);
                String Protocol_Vision_Behind = Protocol_Vision.substring(10, m_Data.length * 2);
                String realVsion = hexStringToString(Protocol_Vision_Behind);
                //                Log.e(TAG, "Sys_YiHi_Protocol_RX_Porc: x10: "+g_PowerValue_x10+"  x1:  "+g_PowerValue_x1);
                //                Log.e(TAG, "Sys_YiHi_Protocol_RX_Porc:   Protocol:  " + Protocol_Vision + "  b: " + Protocol_Vision_Behind + "   r:  " + realVsion);
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
          /*  case C_SXi_CR_AckDevCapability:
                String Protocol_Capability = BinaryToHexString(m_Data);
                byte b = m_Data[m_Index + 6];
                //转成2进制
                String tString = Integer.toBinaryString((b & 0xFF) + 0x100).substring(1);
                char c = tString.charAt(1);
                Log.e(TAG, ": cap :   第6位的二进制： " + tString + "      收到的16进制数据：  " + Protocol_Capability + "   ");
//                if (tString.substring(1, 2).equals(1 + "")) {

                //  获得芯片版本
                    getConnectedDeviceSoftVision();
                    get_software_version = true;
//                }
                break;*/
            case 0x42:

                String back_Software = BinaryToHexString(m_Data).toString();
                String SoftwareData = back_Software.substring(10);
                String Software_Version = hexStringToString(SoftwareData);

                ConnectedBleDevices deviceSoftVision = ConnectedBleDevices.getConnectInfoByAddress(connectedAddress);
                deviceSoftVision.softVision = Software_Version;
                deviceSoftVision.isConnected = true;
                deviceSoftVision.lastConnect = true;
                deviceSoftVision.save();
                get_software_version = false;
                Log.e(TAG, "Sys_YiHi_Protocol_RX_Porc: softvision:   " + back_Software + "  vision:  " + Software_Version + "  ---> 数据获取完毕    connectedState:  " );

                break;
        }
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
}
