package com.yihai.wu.sxi;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yihai.wu.appcontext.MyModel;
import com.yihai.wu.util.DarkImageButton;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ${Wu} on 2016/12/19.
 */

public class SetTextureActivity extends AppCompatActivity {

    @Bind(R.id.btn_back)
    DarkImageButton btnBack;
    @Bind(R.id.modelName)
    TextView modelName;
    @Bind(R.id.connect_state)
    TextView connectState;
    @Bind(R.id.check1)
    ImageView check1;
    @Bind(R.id.tv_power_save)
    TextView tvPowerSave;
    @Bind(R.id.line_power_save)
    LinearLayout linePowerSave;
    @Bind(R.id.check2)
    ImageView check2;
    @Bind(R.id.tv_soft)
    TextView tvSoft;
    @Bind(R.id.line_soft)
    LinearLayout lineSoft;
    @Bind(R.id.check3)
    ImageView check3;
    @Bind(R.id.tv_standard)
    TextView tvStandard;
    @Bind(R.id.line_standard)
    LinearLayout lineStandard;
    @Bind(R.id.check4)
    ImageView check4;
    @Bind(R.id.tv_strong)
    TextView tvStrong;
    @Bind(R.id.line_strong)
    LinearLayout lineStrong;
    @Bind(R.id.check5)
    ImageView check5;
    @Bind(R.id.tv_super_strong)
    TextView tvSuperStrong;
    @Bind(R.id.line_super_strong)
    LinearLayout lineSuperStrong;
    @Bind(R.id.check6)
    ImageView check6;
    @Bind(R.id.tv_custom_s1)
    TextView tvCustomS1;
    @Bind(R.id.detail_s1)
    ImageView detailS1;
    @Bind(R.id.line_custom_s1)
    LinearLayout lineCustomS1;
    @Bind(R.id.check7)
    ImageView check7;
    @Bind(R.id.tv_custom_s2)
    TextView tvCustomS2;
    @Bind(R.id.detail_s2)
    ImageView detailS2;
    @Bind(R.id.line_custom_s2)
    LinearLayout lineCustomS2;
    @Bind(R.id.check8)
    ImageView check8;
    @Bind(R.id.tv_custom_s3)
    TextView tvCustomS3;
    @Bind(R.id.detail_s3)
    ImageView detailS3;
    @Bind(R.id.line_custom_s3)
    LinearLayout lineCustomS3;
    @Bind(R.id.check9)
    ImageView check9;
    @Bind(R.id.tv_custom_s4)
    TextView tvCustomS4;
    @Bind(R.id.detail_s4)
    ImageView detailS4;
    @Bind(R.id.line_custom_s4)
    LinearLayout lineCustomS4;
    @Bind(R.id.check10)
    ImageView check10;
    @Bind(R.id.tv_custom_s5)
    TextView tvCustomS5;
    @Bind(R.id.detail_s5)
    ImageView detailS5;
    @Bind(R.id.line_custom_s5)
    LinearLayout lineCustomS5;

    private int[] checked_arr = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private final static String TEXTURE = "texture";
    private MyModel myModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture);
        ButterKnife.bind(this);
        initUI();
    }

    //界面初始化
    private void initUI() {
        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        myModel = MyModel.getMyModelForGivenName(name);
        int texture = myModel.texture;
        select_control(texture);
    }

    //点击事件
    @OnClick({R.id.btn_back, R.id.tv_power_save, R.id.tv_soft, R.id.tv_standard, R.id.tv_strong, R.id.tv_super_strong, R.id.tv_custom_s1, R.id.detail_s1, R.id.tv_custom_s2, R.id.detail_s2, R.id.tv_custom_s3, R.id.detail_s3, R.id.tv_custom_s4, R.id.detail_s4, R.id.tv_custom_s5, R.id.detail_s5})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                finish();
                break;
            case R.id.tv_power_save:
                pressed(0,getResources().getString(R.string.texture_power_save));
                break;
            case R.id.tv_soft:
                pressed(1,getResources().getString(R.string.texture_soft));
                break;
            case R.id.tv_standard:
                pressed(2,getResources().getString(R.string.texture_standard));
                break;
            case R.id.tv_strong:
                pressed(3,getResources().getString(R.string.texture_strong));
                break;
            case R.id.tv_super_strong:
                pressed(4,getResources().getString(R.string.texture_super_strong));
                break;
            case R.id.tv_custom_s1:
                pressed(5,getResources().getString(R.string.texture_custom_s1));
                break;
            case R.id.detail_s1:
                break;
            case R.id.tv_custom_s2:
                pressed(6,getResources().getString(R.string.texture_custom_s2));
                break;
            case R.id.detail_s2:
                break;
            case R.id.tv_custom_s3:
                pressed(7,getResources().getString(R.string.texture_custom_s3));
                break;
            case R.id.detail_s3:
                break;
            case R.id.tv_custom_s4:
                pressed(8,getResources().getString(R.string.texture_custom_s4));
                break;
            case R.id.detail_s4:
                break;
            case R.id.tv_custom_s5:
                pressed(9,getResources().getString(R.string.texture_custom_s5));
                break;
            case R.id.detail_s5:
                break;
        }
    }

    //控制选择
    private void select_control(int texture) {

        for (int i = 0; i < checked_arr.length; i++) {
            checked_arr[i] = 0;
        }
        checked_arr[texture] = 1;

        if (checked_arr[0] == 1) {
            check1.setVisibility(View.VISIBLE);
        } else {
            check1.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[1] == 1) {
            check2.setVisibility(View.VISIBLE);
        } else {
            check2.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[2] == 1) {
            check3.setVisibility(View.VISIBLE);
        } else {
            check3.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[3] == 1) {
            check4.setVisibility(View.VISIBLE);
        } else {
            check4.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[4] == 1) {
            check5.setVisibility(View.VISIBLE);
        } else {
            check5.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[5] == 1) {
            check6.setVisibility(View.VISIBLE);
        } else {
            check6.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[6] == 1) {
            check7.setVisibility(View.VISIBLE);
        } else {
            check7.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[7] == 1) {
            check8.setVisibility(View.VISIBLE);
        } else {
            check8.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[8] == 1) {
            check9.setVisibility(View.VISIBLE);
        } else {
            check9.setVisibility(View.INVISIBLE);
        }
        if (checked_arr[9] == 1) {
            check10.setVisibility(View.VISIBLE);
        } else {
            check10.setVisibility(View.INVISIBLE);
        }

    }

    //选择点击
    public void pressed (int i,String name){
        Intent intent = new Intent();
        select_control(i);
        myModel.texture=i;
        myModel.save();
        intent.putExtra(TEXTURE,name);
        setResult(RESULT_OK,intent);
        finish();
    }
}
