package com.yihai.wu.sxi;

import android.graphics.Color;
import android.graphics.Region;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yihai.wu.util.MyUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lecho.lib.hellocharts.computator.ChartComputator;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by ${Wu} on 2016/12/23.
 */

public class BezierActivity extends AppCompatActivity {
    private static final String TAG = "BezierActivity";
    @Bind(R.id.btn_back)
    ImageView btnBack;
    @Bind(R.id.btn_above)
    ImageView btnAbove;
    @Bind(R.id.btn_next)
    ImageView btnNext;
    @Bind(R.id.btn_switch)
    ImageView btnSwitch;
    @Bind(R.id.btn_wave)
    ImageView btnWave;
    @Bind(R.id.btn_save)
    ImageView btnSave;
    @Bind(R.id.myChart)
    LineChartView myChart;
    @Bind(R.id.valueX)
    TextView valueX;
    @Bind(R.id.valueY)
    TextView valueY;
    @Bind(R.id.show)
    LinearLayout show;
    @Bind(R.id.btn_waveBehind)
    ImageView btn_waveBehind;

    String[] date = {"0s", "1s", "2s", "3s", "4s", "5s", "6s", "7s", "8s", "9s", "10s"};
    int[] score = {40, 42, 80, 33, 25, 74, 22, 18, 79, 20, 45};//图表的数据点

    private List<PointValue> values;
    private LineChartData lineData;
    private ChartComputator chartComputator;
    private float rate;
    private int clickX;
    private int clickY;
    private PointValue selectedValue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_bezier);
        ButterKnife.bind(this);
        //初始化
        generateInitialLineData();
        initDate();
        myChart.setOnTouchListener(new myChartTouchListener());

    }

    private void initDate() {
        chartComputator = myChart.getChartComputator();
        PointValue a = new PointValue(0, 0);
        PointValue b = new PointValue(0, 1);
        rate = chartComputator.computeRawY(b.getY()) - chartComputator.computeRawY(a.getY());
    }

    private void generateInitialLineData() {
        int numValues = 11;

        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        values = new ArrayList<PointValue>();
        for (int i = 0; i < numValues; ++i) {
            values.add(new PointValue(i, score[i]));
            values.add(new PointValue(i + 0.5f, score[i] + 8.5f));
            axisValues.add(new AxisValue(i).setLabel(date[i]));
        }

        Line line = new Line(values);
        line.setColor(getResources().getColor(R.color.colorWhite)).setCubic(true);
        line.setHasLabelsOnlyForSelected(true);

        List<Line> lines = new ArrayList<Line>();
        lines.add(line);

        PointValue value = line.getValues().get(5);

        lineData = new LineChartData(lines);
        lineData.setAxisXBottom(new Axis(axisValues).setHasLines(true));
        Axis axisY = new Axis();
        axisY.setHasLines(true);
        axisY.setMaxLabelChars(3);
        List<AxisValue> axisValues1 = new ArrayList<>();

        for (int i = 0; i <= 200; i += 40) {
            axisValues1.add(new AxisValue(i).setLabel(i + ""));
        }
        axisY.setValues(axisValues1);
        lineData.setAxisYLeft(axisY);
        lineData.setValueLabelsTextColor(Color.BLACK);
        lineData.setValueLabelTextSize(20);
        //        topLineData.setAxisYLeft(new Axis().setHasLines(true).setMaxLabelChars(3));

        myChart.setLineChartData(lineData);

        // For build-up animation you have to disable viewport recalculation.
        myChart.setViewportCalculationEnabled(false);

        // And set initial max viewport and current viewport- remember to set viewports after data.
        //坐标的视图范围
        Viewport v = new Viewport(0, 200, 10, 0);
        myChart.setMaximumViewport(v);
        myChart.setCurrentViewport(v);

        myChart.setZoomType(ZoomType.HORIZONTAL);
        myChart.setMaxZoom((float) 10);
        Viewport vp = new Viewport(null);
        vp.left = 0;
        vp.right = 4;//显示的点
        vp.bottom = 0;
        vp.top = 200;
        myChart.setCurrentViewport(vp);
        myChart.setZoomEnabled(false);
    }

    @OnClick({R.id.btn_back, R.id.btn_above, R.id.btn_next, R.id.btn_switch, R.id.btn_wave, R.id.btn_save,R.id.btn_waveBehind})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                finish();
                break;
            case R.id.btn_above:
                break;
            case R.id.btn_next:
                break;
            case R.id.btn_switch:
                break;
            case R.id.btn_wave:
                btn_waveBehind.setVisibility(View.VISIBLE);
                btnWave.setVisibility(View.GONE);
                break;
            case R.id.btn_save:

                break;
            case R.id.btn_waveBehind:
                btn_waveBehind.setVisibility(View.GONE);
                btnWave.setVisibility(View.VISIBLE);
                break;
        }
    }


    private class myChartTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    selectedValue = selectPoint(motionEvent);
                    Log.d(TAG, "onTouch: " + clickX + "  ---  " + clickY);
                    break;
                case MotionEvent.ACTION_MOVE:
                    int moveX = (int) motionEvent.getX();
                    int moveY = (int) motionEvent.getY();
                    int move = moveY - clickY;
                    Log.d(TAG, "onTouch: " + moveX + "  -----  " + moveY);
                    if (selectedValue != null) {
                        float currentX = selectedValue.getX();
                        float currentY = selectedValue.getY() - move * rate;
                        Log.d(TAG, "onTouch: " + "找到点" + selectedValue.toString());
                        Line line = lineData.getLines().get(0);// line.
                        if (currentY < 0) {
                            currentY = 0;
                        } else if (currentY > 200) {
                            currentY = 200;
                        }
                        line.getValues().get((int) (selectedValue.getX() * 2)).set(currentX, currentY);
                        float translateY = (float) (Math.round(currentY * 10)) / 10;
                        valueX.setText(currentX + "");
                        valueY.setText(translateY + "");
                        show.setVisibility(View.VISIBLE);
                        myChart.invalidate();
                        clickY = moveY;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    show.setVisibility(View.INVISIBLE);
                    break;
            }

            return false;
        }
    }

    private PointValue selectPoint(MotionEvent motionEvent) {
        int backgroundGridWidth = MyUtils.dip2px(BezierActivity.this, 45);
        int width = backgroundGridWidth / 2;
        Region r = new Region();
        for (PointValue value : values) {
            clickX = (int) motionEvent.getX();
            clickY = (int) motionEvent.getY();
            r.set(clickX - width, clickY - width, clickX + width, clickY + width);
            int pointX = (int) chartComputator.computeRawX(value.getX());
            int pointY = (int) chartComputator.computeRawY(value.getY());
            if (r.contains(pointX, pointY)) {
                return value;
            }
        }
        return null;
    }
}
