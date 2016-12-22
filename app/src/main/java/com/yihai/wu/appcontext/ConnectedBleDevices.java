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



    public static ConnectedBleDevices getConnectInfo(String name) {
        return new Select().from(ConnectedBleDevices.class).where("deviceName = ?", name).executeSingle();
    }
}
