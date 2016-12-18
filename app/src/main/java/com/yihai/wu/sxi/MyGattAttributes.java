package com.yihai.wu.sxi;

import java.util.HashMap;

public class MyGattAttributes {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static HashMap<String, String> attributes = new HashMap();

    public static String getC_UUID_Service_Device_Information="";
    public static String C_UUID_Service_SendDataToDevice="0000ffe5-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Service_ReadDataFromDevice="0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Service_Bluetooth_RSSI="0000ffa0-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Service_DeviceConfig="0000ff90-0000-1000-8000-00805f9b34fb";
    //防劫持密钥服务的UUID
    public static String C_UUID_Service_Password="0000ffc0-0000-1000-8000-00805f9b34fb";
    //一些特征值
    public static String C_UUID_Character_SendDataToDevice="0000ffe9-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Character_ReadDataFromDevice="0000ffe4-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Character_RSSI_Read="0000ffa1-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Character_RSSI_Config="0000ffa2-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Character_Device_Name="0000ff91-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Character_Device_CommInterval="0000ff92-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Character_Device_CommBaudrate="0000ff93-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Character_Device_ResetRestore="0000ff94-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Character_Device_BroadcastInterval="0000ff95-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Character_Device_CustomerID="0000ff96-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Character_Bluetooth_Power="0000ff97-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Character_Password_C1="0000ffc1-0000-1000-8000-00805f9b34fb";
    public static String C_UUID_Character_Password_Notify="0000ffc2-0000-1000-8000-00805f9b34fb";


    /*UUID信息说明:XXXXXXXX-0000-1000-8000-00805F9B34FB(一共32个字符,对应二进制数据为128位长度.)
     *前面8个字符就是Profile,Service,Characteristics对应的UUID,
     *而后面的 0000-1000-8000-00805F9B34FB表示这里使用了SIG定义的Profile.
     */
    static {
    	/*这里的Service的UUID有些是GATT强制BLE设备必须提供的,例如UUID=180A就是一个,这个Service封装有设备信息.*/
        // Sample Services.
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put(C_UUID_Service_SendDataToDevice, "Send Data to Device");
        attributes.put(C_UUID_Service_ReadDataFromDevice, "Read Data from Device");
        attributes.put(C_UUID_Service_Bluetooth_RSSI, "Bluetooth RSSI information");
        attributes.put(C_UUID_Service_DeviceConfig, "Device configuration");
        attributes.put(C_UUID_Service_Password, "Connect password");

        /*这里的特性UUID有些也是GATT强制BLE设备必须提供的,例如UUID=2A29就是一个.*/
        // Sample Characteristics.
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put(C_UUID_Character_SendDataToDevice, "Send data to device(Length<=20 bytes)");
        attributes.put(C_UUID_Character_ReadDataFromDevice, "Read data from device(Length<=20 bytes)");
        attributes.put(C_UUID_Character_RSSI_Read, "RSSI Value(1 byte)");
        attributes.put(C_UUID_Character_RSSI_Config, "RSSI update interval(2 bytes,mS)");
        attributes.put(C_UUID_Character_Device_Name, "Device name (Read or write,16 bytes)");
        attributes.put(C_UUID_Character_Device_CommInterval, "Communication interval");
        attributes.put(C_UUID_Character_Device_CommBaudrate, "Device UART Baudrate");
        attributes.put(C_UUID_Character_Device_ResetRestore, "Device firmware reset or restore");
        attributes.put(C_UUID_Character_Device_BroadcastInterval, "Device bluetooth broadcast interval");
        attributes.put(C_UUID_Character_Device_CustomerID, "Device customer's ID read or write");
        attributes.put(C_UUID_Character_Bluetooth_Power, "Device bluetooth power setting");
        attributes.put(C_UUID_Character_Password_C1, "Submit password");
        attributes.put(C_UUID_Character_Password_Notify, "Submit password Notify");

    }

    /*======================================================================
     *Purpose:查表,表格是一个HashMap类变量(这里的名称是attributes),如果在表格中能找到参数1
     *        相等的值,就返回它对应的说明.
     *Parameter:
     *Return:
     *Remark:HashMap表格记录了成对的K和V.K表示键,V表示值.每个数据项都是K,V的信息对.
     *       HashMap的基本操作就是put()=填写信息对到表格,get()=根据K信息来读取V信息.
     *======================================================================
     */
    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
