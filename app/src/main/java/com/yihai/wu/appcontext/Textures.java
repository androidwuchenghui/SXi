package com.yihai.wu.appcontext;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

@Table(name = "Textures")
public class Textures extends Model {

    @Column(name = "ModelName")
    public String modelName;
    @Column(name = "CustomName")
    public String customName;
    //状态
    @Column(name = "selected")
    public int selected = 0;
    //功率曲线的数据
    @Column(name = "data1")
    public String arr1;
    //功率虚线的数据（可动）
    @Column(name = "data2")
    public int dash = 50;
    //功率虚线的数据（可动）
    @Column(name = "dashInTemper")
    public int dashValueInTemper = 200;
    //℃温度曲线的数据（不可动）
    @Column(name = "data3")
    public String arr3;
    //焦耳数据
    @Column(name = "data4")
    public String arr4;

    @Column(name = "MyModel")
    public MyModel myModel;

    public static Textures getTexture(String modelName,String customName) {
        return new Select()
                .from(Textures.class)
                .where("ModelName=? and CustomName=?", new Object[]{modelName, customName})   //多条件查询
                .executeSingle();
    }


    public static void initTextures(String modelName) {
        for (int i = 0; i < 5; i++) {
            Textures texture = new Textures();
            texture.modelName = modelName;
            texture.customName = "S" + (1 + i);
            //生成初始  W   功率曲线数据
            StringBuilder sb1 = new StringBuilder();
            for (int j = 0; j < 21; j++) {
                int a = 22 + (int) (Math.random() * 60);
                if (j != 20) {
                    sb1 = sb1.append(a + ",");
                } else {
                    sb1 = sb1.append(a);
                }
            }
            texture.arr1 = sb1.toString();
            //生成初始   ℃    温度曲线数据
             StringBuilder sb3 = new StringBuilder();
            for (int j = 0; j < 21; j++) {
                int a = 10 + (int) (Math.random() * 175);
                if (j != 20) {
                    sb3 = sb3.append(a + ",");
                } else {
                    sb3 = sb3.append(a);
                }
            }
            texture.arr3 = sb3.toString();

            //生成初始   J    焦耳曲线数据
            StringBuilder sb4 = new StringBuilder();
            for (int j = 0; j < 21; j++) {
                int a = 10 + (int) (Math.random() * 175);
                if (j != 20) {
                    sb4 = sb4.append(a + ",");
                } else {
                    sb4 = sb4.append(a);
                }
            }
            texture.arr4 = sb4.toString();

            texture.myModel = MyModel.getMyModelForGivenName(modelName);
            texture.save();
        }


    }


}
