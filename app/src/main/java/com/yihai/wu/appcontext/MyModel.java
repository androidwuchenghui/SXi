package com.yihai.wu.appcontext;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

/**
 * Created by ${Wu} on 2016/12/15.
 */

@Table(name = "MyModel")
public class MyModel extends Model {


    @Column(name = "ModelName")
    public String model;
    @Column(name = "StraightModel")
    public int status;
    @Column(name = "DisplayStatus")
    public int display;
    @Column(name = "Material")
    public String material;
    @Column(name = "Texture")
    public String texture;
    @Column(name = "Memory")
    public int memory;
    @Column(name = "TemperatureUnit")
    public int temperatureUnit;
    @Column(name = "JouleOrPower")
    public int JouleOrPower;
    @Column(name = "Operation")
    public int operation;
    @Column(name = "Temperature")
    public int temperature;
    @Column(name = "temperature_c")
    public int temperature_c;
    @Column(name = "TCR")
    public int tcr;
    @Column(name = "power")
    public int power;
    @Column(name ="joule")
    public int joule;

    public MyModel() {
        super();
    }


    public static void
    initMyModel(String name) {
        MyModel myModel = new MyModel();
        myModel.model = name;
        myModel.status = 0;       //直通模式是否打开（0-false,1-true）
        myModel.display = 0;           //显示状态（0-left,1-right,2-auto）
        myModel.material = "镍丝";       //材质选择
        myModel.texture = "标准";
        myModel.memory = 0;
        myModel.temperatureUnit = 0;
        myModel.JouleOrPower = 0;
        myModel.operation = 1;
        myModel.temperature = 291;
        myModel.temperature_c = 25;
        myModel.tcr = 50;
        myModel.power = 100;
        myModel.joule =100;
        myModel.save();
    }
    public static MyModel getMyModelForGivenName(String model) {
        return new Select().from(MyModel.class).where("ModelName = ?", model).executeSingle();
    }



}

