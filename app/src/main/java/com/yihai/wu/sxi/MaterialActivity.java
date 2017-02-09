package com.yihai.wu.sxi;

import android.content.Intent;
import android.content.SharedPreferences;
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

import static com.yihai.wu.sxi.R.id.stainless_steel;

/**
 * Created by ${Wu} on 2016/12/14.
 */

public class MaterialActivity extends AppCompatActivity {
    private static final String MATERIAL = "material";
    @Bind(R.id.check1)
    ImageView check1;
    @Bind(R.id.nickel)
    TextView nickel;
    @Bind(R.id.line_n)
    LinearLayout lineN;
    @Bind(R.id.check2)
    ImageView check2;
    @Bind(R.id.titanium)
    TextView titanium;
    @Bind(R.id.line_t)
    LinearLayout lineT;
    @Bind(R.id.check3)
    ImageView check3;
    @Bind(stainless_steel)
    TextView stainlessSteel;
    @Bind(R.id.line_b)
    LinearLayout lineB;
    @Bind(R.id.check4)
    ImageView check4;
    @Bind(R.id.alcohol)
    TextView alcohol;
    @Bind(R.id.line_c)
    LinearLayout lineC;
    @Bind(R.id.check5)
    ImageView check5;
    @Bind(R.id.TRC)
    TextView TRC;
    @Bind(R.id.line_s)
    LinearLayout lineS;
    @Bind(R.id.btn_back)
    DarkImageButton btnBack;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor share_editor;
    private int[] check_select = {0, 0, 0, 0, 0};
    private String title;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material);
        ButterKnife.bind(this);
        initView();

    }

    private void initView() {
        Intent intent = getIntent();
        title = intent.getStringExtra("title");
        sharedPreferences = getSharedPreferences("YiHi_material_UI", MODE_PRIVATE);
        share_editor = sharedPreferences.edit();
        int select = sharedPreferences.getInt("material_select", 0);
        select_control(select);
    }


    @OnClick({R.id.line_n, R.id.line_t, R.id.line_b, R.id.line_c, R.id.line_s,R.id.btn_back})
    public void onClick(View view) {
        MyModel myModel = MyModel.getMyModelForGivenName(title);
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.line_n:
                select_control(0);
                share_editor.putInt("material_select", 0);
                share_editor.commit();
                intent.putExtra(MATERIAL, nickel.getText().toString());
                myModel.coilSelect = 0;
//                getResources().getString(R.string.material_nickel_wire);
                myModel.save();
                break;
            case R.id.line_t:
                select_control(1);
                share_editor.putInt("material_select", 1);
                share_editor.commit();
                intent.putExtra(MATERIAL, titanium.getText().toString());
                myModel.coilSelect = 1;
                myModel.save();
                break;
            case R.id.line_b:
                select_control(2);
                share_editor.putInt("material_select", 2);
                share_editor.commit();
                intent.putExtra(MATERIAL, stainlessSteel.getText().toString());
                myModel.coilSelect = 2;
                myModel.save();
                break;
            case R.id.line_c:
                select_control(3);
                share_editor.putInt("material_select", 3);
                share_editor.commit();
                intent.putExtra(MATERIAL, alcohol.getText().toString());
                myModel.coilSelect = 3;
                myModel.save();
                break;
            case R.id.line_s:
                select_control(4);
                share_editor.putInt("material_select", 4);
                share_editor.commit();
                intent.putExtra(MATERIAL, TRC.getText().toString());
                myModel.coilSelect = 4;
                myModel.save();
                break;
            case R.id.btn_back:
                finish();
                break;
        }
        setResult(RESULT_OK, intent);
        finish();
    }


    //控制选择
    private void select_control(int select) {

        for (int i = 0; i < check_select.length; i++) {
            check_select[i] = 0;
        }
        check_select[select] = 1;

        if (check_select[0] == 1) {
            check1.setVisibility(View.VISIBLE);
        } else {
            check1.setVisibility(View.INVISIBLE);
        }
        if (check_select[1] == 1) {
            check2.setVisibility(View.VISIBLE);
        } else {
            check2.setVisibility(View.INVISIBLE);
        }
        if (check_select[2] == 1) {
            check3.setVisibility(View.VISIBLE);
        } else {
            check3.setVisibility(View.INVISIBLE);
        }
        if (check_select[3] == 1) {
            check4.setVisibility(View.VISIBLE);
        } else {
            check4.setVisibility(View.INVISIBLE);
        }
        if (check_select[4] == 1) {
            check5.setVisibility(View.VISIBLE);
        } else {
            check5.setVisibility(View.INVISIBLE);
        }
    }
}
