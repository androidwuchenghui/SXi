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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.yihai.wu.util.MyUtils.BinaryToHexString;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@SuppressWarnings("unused")
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private SharedPreferences sharedPreferences;
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

    /*用于广播的消息.*/
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
                    Log.d(TAG, "onLeScan: " + device.getAddress());
                    if (device.getAddress().toString().equals(lastAddress)) {
                        keepSearch = false;
                        Log.d(TAG, "onLeScan: 通过servive自己搜索并连接");
                        connect(lastAddress);
                    }
                }
            };


    private boolean keepSearch;
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
            Log.d(TAG, "ServiceOnConnectionStateChange:   gatt" + gatt + "   status:  " + status + "   newState:   " + newState + " = 2");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;//连接状态
                broadcastUpdate(intentAction);
                // Attempts to discover services after successful connection.
                                Log.d(TAG, "ServiceOnConnectionStateChange: " + "     连接成功   gatt.discoveryService   " );
                mBluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                close();
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;//断开状态
                ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
                if (connectedDevice != null) {
                    connectedDevice.isConnected = false;
                    connectedDevice.save();
                }
                broadcastUpdate(intentAction);
                Log.d(TAG, "ServiceOnConnectionStateChange:     断开连接   adapter :  " + mBluetoothAdapter);
                if (!disConnectByMyself) {
                    keepSearch = true;
                    pool.execute(new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            while (keepSearch) {
                                mBluetoothAdapter.startLeScan(mLeScanCallback);
                                Log.d(TAG, "ServiceOnConnectionStateChange   run: 循环连接 ");
                                try {
                                    sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            }

                        }
                    });
                }else {
                    broadcastUpdate(ACTION_LOGIN_FAILED);
                }

            }

        }

        // New services discovered
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered:   status:  " + status + "    services:    " + gatt.getServices());
            disconStatus = status;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED); //发现服务
                Thread serviceThread = new Thread() {
                    @Override
                    public void run() {
                        List<BluetoothGattService> supportedGattServices = getSupportedGattServices();
                        displayGattServices(supportedGattServices);
                        super.run();
                    }
                };
                pool.execute(serviceThread);
            }
                        else if (status == 129) {
            //                Log.d(TAG, "status = 129: -----发出129");
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
                broadcastUpdate(ACTION_LOGIN_FAILED);
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
            Log.d(TAG, "onCharacteristicRead: " + characteristic.getUuid() + ">>>：" + g_UUID_Charater_CustomerID);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

                if (characteristic.getUuid().equals(g_UUID_Charater_CustomerID)) {
                    Log.d(TAG, "onCharacteristicRead: 发出广播");

                    byte[] data = characteristic.getValue();
                    readID = BinaryToHexString(data);

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
                        Log.d(TAG, "onReceive: " + changeTo);
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
                //Log.d(TAG, "write characteristic=Done");
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

            Log.d(TAG, "onCharacteristicChanged: " + characteristic.getUuid() + "  >>>  " + g_UUID_Charater_Password_C2);
            if (characteristic.getUuid().equals(g_UUID_Charater_Password_C2)) {
                //                broadcastUpdate(ACTION_DATA_COMMIT_PASSWORD_RESULT, characteristic);

                byte[] data = characteristic.getValue();
                final String reply = BinaryToHexString(data);
                Log.d(TAG, "onCharacteristicChanged:  reply : " + reply);
                pool.execute(new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            sleep(0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (commit_amount == 0) {
                            handlePasswordCallbacks(reply);
                        } else if (commit_amount == 1) {
                            //      处理提交亿海产品密码后的返回
                            handleSecondPasswordCallback(reply);
                        } else if (commit_amount == 2) {
                            //         处理修改密码的返回

                            handleChangePassword(reply);
                        } else if (commit_amount == 3) {
                            Log.d(TAG, "handleChangePassword: " + "修改密码后再提交一次");
                            handleLastPassword(reply);
                        }
                    }
                });

            } else if (characteristic.getUuid().equals(g_UUID_Charater_ReadData)) {
                Log.d(TAG, "onCharacteristicChanged:  RX" + BinaryToHexString(characteristic.getValue()));
                broadcastRxUpdate(ACTION_DATA_RX, characteristic);
            }

        }
    };
    private String readID;
    private int disconStatus;

    public void setLastAddress(String lastAddress) {
        this.lastAddress = lastAddress;
    }

    private String lastAddress;
    private BluetoothGattCharacteristic g_Character_Baud_Rate;
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
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
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
        lastAddress = sharedPreferences.getString("address", null);
        Log.d(TAG, "serviceOnCreate:     lastAddress  "+lastAddress);
    }

    /*======================================================================
         *Purpose:与这个类进行绑定的窗口调用了bindService()时,就会触发这个函数.
         *Parameter:
         *Return:
         *Remark:绑定时的标准流程.
         *======================================================================
         */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: " + "BindService");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: service 取消绑定");
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
        Log.d(TAG, "onServiceDestroy: 后台服务关闭 ");
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
        Log.d(TAG, "Trying to create a new connection.     getBluetoothGatt:    " + mBluetoothGatt);
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;

        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        Log.d(TAG, "disconnect: 断开连接");
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
        Log.d(TAG, "readCharacteristic: 读特征值    gatt:  " + mBluetoothGatt + "    " + characteristic.getUuid());
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
            //Log.d(TAG, "write characteristic");

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
            Log.d("serviceBroadReceiver", "onReceive: " + action);
            switch (action) {
                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    Log.d(TAG, "reConnectBluetooth: ");
                    BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    break;

            }
        }
    };

    private static IntentFilter makeMainBroadcastFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

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
                        Log.d(TAG, "displayGattServices: 设备名特征值----" + g_Character_DeviceName.getUuid());
                        //                        Sys_SetMyDeviceName("BleDevice");
                    } else if (g_UUID_Charater_CustomerID.equals(gattCharacteristic.getUuid())) {
                        g_Character_CustomerID = gattCharacteristic;
                        Log.d(TAG, "displayGattServices: 产品识别码特征值：  " + g_Character_CustomerID.getUuid());
                    } else if (g_UUID_Charater_Baud_Rate.equals(gattCharacteristic.getUuid())) {
                        g_Character_Baud_Rate = gattCharacteristic;
                        Log.d(TAG, "displayGattServices: 波特率特征值：" + g_Character_Baud_Rate.getUuid());
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
            Log.d(TAG, "password: 提交保存的密码:  " + password);
            commit_amount = 0;
            commitPassword(password + password);

        } else {
            Log.d(TAG, "password: 提交默认密码---  000000");
            commit_amount = 0;
            ConnectedBleDevices current = new ConnectedBleDevices();
            current.deviceName = mDeviceName;
            current.deviceAddress = connectedAddress;
            current.password = DEFAULT_PASSWORD;
            current.save();
            Log.d(TAG, "displayGattServices: " + ConnectedBleDevices.getConnectInfoByAddress(connectedAddress).deviceName);
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
        Log.d(TAG, "password: 提交密码： 特征值为：   " + g_Character_Password + "   密码为：  " + password);
        byte[] m_Data = password.getBytes();
        if (g_Character_Password != null) {
            g_Character_Password.setValue(m_Data);
            writeCharacteristic(g_Character_Password);
        }
    }

    //首次连接的判断
    private void isFirst() {

        ConnectedBleDevices theDevice = ConnectedBleDevices.getConnectInfoByAddress(connectedAddress);
        Log.d(TAG, "passWordisFirst: " + theDevice.isFirst + "  ***");
        boolean isFirstRun = theDevice.isFirst;
        if (isFirstRun || (step3 || step5) && commit_amount != 3) {
            //      获取识别码
            Log.d(TAG, " passWord    是第一次登陆 获取识别码: ");

            readCharacteristic(g_Character_CustomerID);//读取产品识别码

        } else {
            //通过用户自己保存的密码，并且不是第一次进入，允许执行正常通信￥￥（流程OK）
            Log.d(TAG, "handleChangePassword: 用户通过保存的密码进入      over");
            //发出登陆成功的广播
            broadcastUpdate(ACTION_LAND_SUCCESS);
            theDevice.isConnected = true;
            theDevice.save();
        }

    }

    private boolean step3 = false;
    private boolean step5 = false;

    private void handlePasswordCallbacks(String reply) {
        ConnectedBleDevices devices = ConnectedBleDevices.getConnectInfoByAddress(connectedAddress);
        Log.d(TAG, "handlePasswordCallbacks: " + "处理返回的密码状态:" + reply);
        if (reply != null) {
            //提交密码后判断回馈的信息
            switch (reply) {
                case "00":
                    //判断是不是第一次连接
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        broadcastUpdate(ACTION_LAND_SUCCESS);
                        devices.isConnected = true;
                        devices.save();
                        break;
                    }
                    Log.d(TAG, "handlePasswordCallbacks: 密码提交---正确---");
                    if (devices.password.equals(DEFAULT_PASSWORD)) {
                        step3 = true;
                    }
                    isFirst();
                    break;
                case "01":
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        broadcastUpdate(ACTION_LOGIN_FAILED);
                        break;
                    }
                    Log.d(TAG, "displayData: 默认设备密码提交错误--- 开始提交产品密码");
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
        Log.d(TAG, "handleSecondPasswordCallback: " + "提交默认产品密码返回处理  " + str);
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
                Log.d(TAG, "handleChangePassword:修改失败 " + reChange);
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

                commitPassword(lastPassword + lastPassword);
                //连接成功，执行正常通信￥￥ （流程OK）
                Log.d(TAG, "handleChangePassword: 修改密码并保存     正常连接。。。");

                break;
        }

    }

    private void handleLastPassword(String reply) {
        switch (reply) {
            case "00":
                Log.d(TAG, "handleLastPassword: " + "OK");
                isFirst();
                break;
            case "01":

                break;
        }
    }
}
