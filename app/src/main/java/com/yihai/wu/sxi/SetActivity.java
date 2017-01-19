package com.yihai.wu.sxi;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yihai.wu.appcontext.ConnectedBleDevices;
import com.yihai.wu.appcontext.MyModel;
import com.yihai.wu.util.DarkImageButton;

/**
 * Created by ${Wu} on 2016/12/12.
 */

public class SetActivity extends AppCompatActivity {
    private DarkImageButton btn_back;
    private EditText et;
    private int[] check_select = {0, 0, 0, 0, 0};

    private ImageView select_c1, select_c2, select_c3, select_c4, select_c5;
    private ImageView detail_c1, detail_c2, detail_c3, detail_c4, detail_c5;
    private LinearLayout line_c1, line_c2, line_c3, line_c4, line_c5;
    private TextView tv_c1, tv_c2, tv_c3, tv_c4, tv_c5,status;
    private final int REQUEST_CODE_1 = 0X001;
    private final int REQUEST_CODE_TO_MAIN = 0X002;

    private Intent intent;
    private Intent toMainIntent;
    int select = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);
        initView();
        status = (TextView) findViewById(R.id.connect_state);
        ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
        if(connectedDevice!=null){
            status.setText("已连接");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        MyModel selectedModel = MyModel.getSelectedModel();
        if (selectedModel != null) {
            switch (selectedModel.model) {
                case "C1":
                    select = 0;
                    break;
                case "C2":
                    select = 1;
                    break;
                case "C3":
                    select = 2;
                    break;
                case "C4":
                    select = 3;
                    break;
                case "C5":
                    select = 4;
                    break;

            }
        }
//                 select = sharedPreferences.getInt("select", 0);
        select_control(select);
    }

    private void select_control(int s) {
//        share_editor.putInt("select", s);
//        share_editor.commit();

        toMainIntent.putExtra("myMode", "C" + (s + 1));

        setResult(REQUEST_CODE_TO_MAIN, toMainIntent);

        for (int i = 0; i < check_select.length; i++) {
            check_select[i] = 0;
        }
        check_select[s] = 1;

        if (check_select[0] == 1) {
            select_c1.setVisibility(View.VISIBLE);
        } else {
            select_c1.setVisibility(View.INVISIBLE);
        }
        if (check_select[1] == 1) {
            select_c2.setVisibility(View.VISIBLE);
        } else {
            select_c2.setVisibility(View.INVISIBLE);
        }
        if (check_select[2] == 1) {
            select_c3.setVisibility(View.VISIBLE);
        } else {
            select_c3.setVisibility(View.INVISIBLE);
        }
        if (check_select[3] == 1) {
            select_c4.setVisibility(View.VISIBLE);
        } else {
            select_c4.setVisibility(View.INVISIBLE);
        }
        if (check_select[4] == 1) {
            select_c5.setVisibility(View.VISIBLE);
        } else {
            select_c5.setVisibility(View.INVISIBLE);
        }
    }

    private void initView() {

        select_c1 = (ImageView) findViewById(R.id.select_c1);
        select_c2 = (ImageView) findViewById(R.id.select_c2);
        select_c3 = (ImageView) findViewById(R.id.select_c3);
        select_c4 = (ImageView) findViewById(R.id.select_c4);
        select_c5 = (ImageView) findViewById(R.id.select_c5);
        detail_c1 = (ImageView) findViewById(R.id.detail_c1);

        detail_c1.setOnClickListener(new clickEvent());
        detail_c2 = (ImageView) findViewById(R.id.detail_c2);
        detail_c2.setOnClickListener(new clickEvent());
        detail_c3 = (ImageView) findViewById(R.id.detail_c3);
        detail_c3.setOnClickListener(new clickEvent());
        detail_c4 = (ImageView) findViewById(R.id.detail_c4);
        detail_c4.setOnClickListener(new clickEvent());
        detail_c5 = (ImageView) findViewById(R.id.detail_c5);
        detail_c5.setOnClickListener(new clickEvent());

        line_c1 = (LinearLayout) findViewById(R.id.line_c1);
        line_c1.setOnClickListener(new clickEvent());
        line_c2 = (LinearLayout) findViewById(R.id.line_c2);
        line_c2.setOnClickListener(new clickEvent());
        line_c3 = (LinearLayout) findViewById(R.id.line_c3);
        line_c3.setOnClickListener(new clickEvent());
        line_c4 = (LinearLayout) findViewById(R.id.line_c4);
        line_c4.setOnClickListener(new clickEvent());
        line_c5 = (LinearLayout) findViewById(R.id.line_c5);
        line_c5.setOnClickListener(new clickEvent());

        tv_c1 = (TextView) findViewById(R.id.tv_c1);
        tv_c1.setOnClickListener(new clickEvent());
        tv_c2 = (TextView) findViewById(R.id.tv_c2);
        tv_c2.setOnClickListener(new clickEvent());
        tv_c3 = (TextView) findViewById(R.id.tv_c3);
        tv_c3.setOnClickListener(new clickEvent());
        tv_c4 = (TextView) findViewById(R.id.tv_c4);
        tv_c4.setOnClickListener(new clickEvent());
        tv_c5 = (TextView) findViewById(R.id.tv_c5);
        tv_c5.setOnClickListener(new clickEvent());
        btn_back = (DarkImageButton) findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new clickEvent());
        et = (EditText) findViewById(R.id.et);
        et.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (MotionEvent.ACTION_DOWN == event.getAction()) {
                    et.setCursorVisible(true);// 再次点击显示光标
                }
                return false;
            }
        });

        //跳转到设置详情页
        intent = new Intent(SetActivity.this, SetDetailsActivity.class);
        toMainIntent = new Intent();
    }

    private class clickEvent implements View.OnClickListener {
        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.btn_back:
                    finish();
                    break;
                case R.id.tv_c1:
                    select_control(0);
                    setSelectedData("C1");
                    break;
                case R.id.tv_c2:
                    select_control(1);
                    setSelectedData("C2");
                    break;
                case R.id.tv_c3:
                    select_control(2);
                    setSelectedData("C3");
                    break;
                case R.id.tv_c4:
                    select_control(3);
                    setSelectedData("C4");
                    break;
                case R.id.tv_c5:
                    select_control(4);
                    setSelectedData("C5");
                    break;
                case R.id.detail_c1:
                    select_control(0);
                    intent.putExtra("detail", "C1");
                    setSelectedData("C1");
                    startActivity(intent);
                    break;
                case R.id.detail_c2:
                    select_control(1);
                    intent.putExtra("detail", "C2");
                    setSelectedData("C2");
                    startActivity(intent);
                    break;
                case R.id.detail_c3:
                    select_control(2);
                    setSelectedData("C3");
                    intent.putExtra("detail", "C3");
                    startActivity(intent);
                    break;
                case R.id.detail_c4:
                    select_control(3);
                    setSelectedData("C4");
                    intent.putExtra("detail", "C4");
                    startActivity(intent);
                    break;
                case R.id.detail_c5:
                    select_control(4);
                    setSelectedData("C5");
                    intent.putExtra("detail", "C5");
                    startActivity(intent);
                    break;


            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    public void setSelectedData(String str){
        for (int i = 0; i <5 ; i++) {
            String name = "C"+(i+1);
            MyModel myModel = MyModel.getMyModelForGivenName(name);
            myModel.modelSelected=0;
            myModel.save();
        }
        MyModel myModelSlected = MyModel.getMyModelForGivenName(str);
        myModelSlected.modelSelected=1;
        myModelSlected.save();
    }
}
