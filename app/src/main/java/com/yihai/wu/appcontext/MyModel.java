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
    public int  modelSelected;

    @Column(name = "ModelName")
    public String model;

    @Column(name = "StraightModel")
    public int status;
    @Column(name = "DisplayStatus")
    public int display;
    //材料选择
    @Column(name = "Material")
    public String material;
    //口感选择
    @Column(name = "Texture")
    public int texture;
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
        myModel.modelSelected=0;

        myModel.model = name;
        //直通模式是否打开（0-false,1-true）
        myModel.status = 0;
        //显示状态（0-left,1-right,2-auto）
        myModel.display = 0;
        //材质选择
        myModel.material = "镍丝";
        //口感选择
        myModel.texture =2;
        //记忆模式选择
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

        Textures.initTextures(name);
    }
    public static MyModel getMyModelForGivenName(String model) {
        return new Select().from(MyModel.class).where("ModelName = ?", model).executeSingle();
    }

    public static MyModel getSelectedModel(){
        return new Select().from(MyModel.class).where("modelSelected = ?",1).executeSingle();
    }

    public List<Textures> getCurves(){
        return getMany(Textures.class,"MyModel");
    }
    public static List<MyModel> getAllMyModel(){
        return new Select().from(MyModel.class).execute();
    }
}

