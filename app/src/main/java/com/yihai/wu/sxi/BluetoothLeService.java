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
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@SuppressWarnings("unused")
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

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
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;//连接状态
                broadcastUpdate(intentAction);

                Log.d("ConnectionStateChange", "onConnectionStateChange:连接状态良好------ ");
                // Attempts to discover services after successful connection.
                mBluetoothGatt.discoverServices();
                //                Log.e("log", "Attempting to start service discovery:" +
                //                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;//断开状态
                Log.i("ConnectionStateChange", "Disconnected from GATT server.请注意---断开了----");
                broadcastUpdate(intentAction);
            }

        }

        // New services discovered
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED); //发现服务
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
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
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.d(TAG, "onCharacteristicRead: "+characteristic.getUuid()+">>>："+g_UUID_Charater_CustomerID);
                if(characteristic.getUuid().equals(g_UUID_Charater_CustomerID)){
                    Log.d(TAG, "onCharacteristicRead: 发出广播");
                    broadcastUpdate("com.id",characteristic);
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

            Log.d(TAG, "onCharacteristicChanged: "+characteristic.getUuid()+">>>"+ g_UUID_Charater_Password_C2);
            if(characteristic.getUuid().equals(g_UUID_Charater_Password_C2)){
                Log.d(TAG, "onCharacteristicChanged: 提交返回");
                broadcastUpdate(ACTION_DATA_COMMIT_PASSWORD_RESULT,characteristic);
            }

        }
    };

    /*======================================================================
     *Purpose:向外广播一个字符串信息.
     *======================================================================
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
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

    /*======================================================================
     *Purpose:与这个类进行绑定的窗口调用了bindService()时,就会触发这个函数.
     *Parameter:
     *Return:
     *Remark:绑定时的标准流程.
     *======================================================================
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
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

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            /*必须有过BluetoothDevice的connectGatt()调用,并获得BluetoothGatt的实例对象后,才能调用这个函数.*/
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

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
        Log.d(TAG, "Trying to create a new connection.");
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
        Log.e(TAG, "writeCharacteristic:*****写** " + m_Property);
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

}
