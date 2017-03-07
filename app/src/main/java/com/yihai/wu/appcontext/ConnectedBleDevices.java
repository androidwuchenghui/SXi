package com.yihai.wu.appcontext;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

/**
 * Created b${Wu} on 2016/12/21.
 */
@Table(name="ConnectedBleDevices")
public class ConnectedBleDevices extends Model{

    public ConnectedBleDevices() {
        super();
    }

    @Column(name = "deviceName")
    public String deviceName;


    @Column(name = "deviceAddress")
    public String deviceAddress;


    @Column(name = "password")
    public String password;

    @Column(name="isFirst")
    public boolean isFirst = true;

    @Column(name = "isConnected")
    public boolean isConnected = false;

    @Column(name = "realName")
    public String realName ;

    @Column(name = "deviceID")
    public String deviceID;

    @Column(name = "softVision")
    public String softVision;

    @Column(name = "lastConnect")
    public boolean lastConnect = false;



    public static ConnectedBleDevices getConnectInfoByName(String name) {
        return new Select().from(ConnectedBleDevices.class).where("deviceName = ?", name).executeSingle();
    }


    public static ConnectedBleDevices getConnectInfoByAddress(String address) {
        return new Select().from(ConnectedBleDevices.class).where("deviceAddress = ?", address).executeSingle();
    }

    public static ConnectedBleDevices getConnectedDevice(){
        return new Select().from(ConnectedBleDevices.class).where("isConnected = ?",true).executeSingle();
    }

    public static ConnectedBleDevices getLastConnectedDevice(){
        return new Select().from(ConnectedBleDevices.class).where("lastConnect = ?",true).executeSingle();
    }
}
