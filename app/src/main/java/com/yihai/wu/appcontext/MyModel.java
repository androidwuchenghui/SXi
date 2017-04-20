package com.yihai.wu.appcontext;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.List;

/**
 * Created by ${Wu} on 2016/12/15.
 */

@Table(name = "MyModel")
public class MyModel extends Model {

    @Column(name = "ModelSelected")
    public int modelSelected;

    @Column(name = "ModelName")
    public String model;

    @Column(name = "StraightModel")
    public int bypass;
    @Column(name = "DisplayStatus")
    public int display;
    //材料选择
    @Column(name = "Material")
    public int coilSelect;
    //口感选择
    @Column(name = "Texture")
    public int texture;
    @Column(name = "Memory")
    public int memory;
    //温度单位
    @Column(name = "TemperatureUnit")
    public int temperatureUnit;
    //功率焦耳切换（1-功率，2-焦耳）
    @Column(name = "JouleOrPower")
    public int JouleOrPower;
    //操作模式
    @Column(name = "Operation")
    public int operation;
    //温度调节
    @Column(name = "Temperature")
    public int temperature;
    //补偿温度
    @Column(name = "temperature_c")
    public int temperature_c;
    //TCR设置
    @Column(name = "TCR")
    public int tcr;
    //功率调节
    @Column(name = "power")
    public int power;
    //焦耳调节
    @Column(name = "joule")
    public int joule;
    //功率调节最大值
    @Column(name = "powerRange_max")
    public int powerRange_max;
    //功率调节最小值
    @Column(name = "powerRange_min")
    public int powerRange_min;
    //功率调节最大值
    @Column(name = "jouleRange_max")
    public int jouleRange_max;
    //功率调节最小值
    @Column(name = "jouleRange_min")
    public int jouleRange_min;
    //显示C1~C5
    @Column(name = "showName")
    public String showName;

    public MyModel() {
        super();
    }


    public static void
    initMyModel(String name) {
        MyModel myModel = new MyModel();
        myModel.modelSelected = 0;
        myModel.showName = name;
        myModel.model = name;
        //直通模式是否打开（0-false,1-true）
        myModel.bypass = 0;
        //显示状态（0-left,1-right,2-auto）
        myModel.display = 0;
        //材质选择
        myModel.coilSelect = 0;
        //口感选择
        myModel.texture = 2;
        //记忆模式选择
        myModel.memory = 0;
        //温度单位
        myModel.temperatureUnit = 0;
        //功率焦耳切换
        myModel.JouleOrPower = 0;
        //操作模式
        myModel.operation = 1;

        myModel.temperature = 291;
        myModel.temperature_c = 25;
        myModel.tcr = 50;
        myModel.power = 100;
        myModel.joule = 100;
        myModel.powerRange_max = 2000;
        myModel.powerRange_min = 50;
        myModel.jouleRange_max = 1200;
        myModel.jouleRange_min = 100;

        myModel.save();

        Textures.initTextures(name);
    }

    public static MyModel getMyModelForGivenName(String model) {
        return new Select().from(MyModel.class).where("ModelName = ?", model).executeSingle();
    }

    public static MyModel getSelectedModel() {
        return new Select().from(MyModel.class).where("modelSelected = ?", 1).executeSingle();
    }

    public List<Textures> getCurves() {
        return getMany(Textures.class, "MyModel");
    }

    public static List<MyModel> getAllMyModel() {
        return new Select().from(MyModel.class).execute();
    }
}

