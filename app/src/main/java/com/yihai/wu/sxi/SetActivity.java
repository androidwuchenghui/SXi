package com.yihai.wu.sxi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yihai.wu.util.DarkImageButton;

/**
 * Created by ${Wu} on 2016/12/12.
 */

public class SetActivity extends AppCompatActivity {
    private DarkImageButton btn_back;
    private EditText et;
    private int[] check_select = {0, 0, 0, 0, 0};
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor share_editor;
    private ImageView select_c1, select_c2, select_c3, select_c4, select_c5;
    private ImageView detail_c1, detail_c2, detail_c3, detail_c4, detail_c5;
    private LinearLayout line_c1, line_c2, line_c3, line_c4, line_c5;
    private TextView tv_c1,tv_c2,tv_c3,tv_c4,tv_c5;
    private final int REQUEST_CODE_1=0X001;
    private Intent intent;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);
        initView();

    }

    @Override
    protected void onStart() {
        super.onStart();
        int select = sharedPreferences.getInt("select", 0);
        select_control(select);
    }

    private void select_control(int s) {

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
        sharedPreferences = getSharedPreferences("YiHi_select", MODE_PRIVATE);
        share_editor = sharedPreferences.edit();
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
        intent = new Intent(SetActivity.this,SetDetailsActivity.class);
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
                    share_editor.putInt("select",0);
                    share_editor.commit();
                    break;
                case R.id.tv_c2:
                    select_control(1);
                    share_editor.putInt("select",1);
                    share_editor.commit();
                    break;
                case R.id.tv_c3:
                    select_control(2);
                    share_editor.putInt("select",2);
                    share_editor.commit();
                    break;
                case R.id.tv_c4:
                    select_control(3);
                    share_editor.putInt("select",3);
                    share_editor.commit();
                    break;
                case R.id.tv_c5:
                    share_editor.putInt("select",4);
                    select_control(4);
                    share_editor.commit();
                    break;
                case R.id.detail_c1:
                    intent.putExtra("detail","C1");
                    startActivity(intent);
                    break;
                case R.id.detail_c2:
                    intent.putExtra("detail","C2");
                    startActivity(intent);
                    break;
                case R.id.detail_c3:
                    intent.putExtra("detail","C3");
                    startActivity(intent);
                    break;
                case R.id.detail_c4:
                    intent.putExtra("detail","C4");
                    startActivity(intent);
                    break;
                case R.id.detail_c5:

                    intent.putExtra("detail","C5");
                    startActivity(intent);
                    break;


            }
        }
    }


}
