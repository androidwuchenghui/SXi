package com.yihai.wu.sxi;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.PathEffect;
import android.graphics.Region;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yihai.wu.appcontext.MyModel;
import com.yihai.wu.appcontext.Textures;
import com.yihai.wu.util.ClickImageView;
import com.yihai.wu.util.DisEnableImageView;
import com.yihai.wu.util.MyUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
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
    ClickImageView btnBack;
    @Bind(R.id.btn_above_disable)
    DisEnableImageView aboveDisable;
    @Bind(R.id.btn_above)
    ClickImageView btnAbove;
    @Bind(R.id.btn_next_disable)
    DisEnableImageView nextDisable;
    @Bind(R.id.btn_next)
    ClickImageView btnNext;
    @Bind(R.id.btn_switch)
    ClickImageView btnSwitch;
    @Bind(R.id.btn_wave)
    ClickImageView btnWave;
    @Bind(R.id.btn_save)
    ClickImageView btnSave;
    @Bind(R.id.btn_waveBehind)
    ClickImageView btn_waveBehind;
    @Bind(R.id.myChart)
    LineChartView myChart;
    @Bind(R.id.valueX)
    TextView valueX;
    @Bind(R.id.valueY)
    TextView valueY;
    @Bind(R.id.showTemper)
    LinearLayout showTemper;

    @Bind(R.id.tv_dashY)
    TextView tvDashY;
    @Bind(R.id.dash)
    LinearLayout dash;

    String[] axisData = {"0s", "1s", "2s", "3s", "4s", "5s", "6s", "7s", "8s", "9s", "10s"};
    int temperDashValue = 200;
    //以下参数用来区分不同曲线的样式
    private static final int DASH = 0;
    private static final int TEMPER_HIGHLIGHT_CURVE = 2;
    private static final int DARK_CURVE = 9;
    private static final int POWER_HIGHLIGHT_CURVE = 1;
    private static final int JOULE_HIGHLIGHT_CURVE = 3;
    //以下参数用来识别touch事件作用的对象chart
    private int touchInChart;
    private static final int TOUCH_FOR_POWER_CHART = 0X01;
    private static final int TOUCH_FOR_TEMPER_CHART = 0X02;
    private static final int TOUCH_FOR_JOULE_CHART = 0X03;
    private boolean dataChanged = false;
    //集合用来储存操作的历史记录。。。
    int powerIndex = 0;
    List<int[]> powerMoveList = new ArrayList();
    List<Integer> dashListInPower = new ArrayList<>();

    int temperIndex = 0;
    List<int[]> temperDataList = new ArrayList<>();
    List<Integer> dashListInTemper = new ArrayList<>();

    //临时中转存储
    int currentIndex;
    List<int[]> currentMoveLineList;
    List<Integer> currentDashLineList;
    private List<AxisValue> currentAxisY_Values;  //当前Y轴坐标
    private Line currentDashLine;
    private Line selectedDashLine;
    private Line currentMainLine;

    private LineChartData lineData;
    private ChartComputator chartComputator;
    private float rate;
    private int clickX;
    private int clickY;
    private PointValue selectedValue;
    private int[] powerData1;
    private int dash1;
    private int[] temperData1;
    private Line powerLine;
    private Line powerDashLine;
    private Textures texture;
    private Axis axisX;
    private Line temperLine_back;
    private Line temperLine;
    private Line backPowerLine;
    private Line temperDashLine;
    private int [] initJouleData;
    private Line jouleLine;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_bezier);
        ButterKnife.bind(this);
        //初始化
        init();
        initDate();
        myListeners();//监听
    }

    private void myListeners() {
        btnBack.setOnClickListener(new myClickListener());
        btnAbove.setOnClickListener(new myClickListener());
        btnNext.setOnClickListener(new myClickListener());
        btnSwitch.setOnClickListener(new myClickListener());
        btnWave.setOnClickListener(new myClickListener());
        btn_waveBehind.setOnClickListener(new myClickListener());
    }

    private void init() {

        Intent intent = getIntent();
        String modelName = intent.getStringExtra("modelName");
        String customName = intent.getStringExtra("custom");
        Log.d(TAG, "init: " + modelName + "   " + customName);
        //查询数据库、
        texture = Textures.getTexture(modelName, customName);

        //        Log.d(TAG, "init: "+arr1);
        //功率曲线的数据
        powerData1 = getLineData(texture.arr1);
        powerMoveList.add(powerData1);
        //虚线1的数据
        dash1 = texture.dash;
        dashListInPower.add(dash1);
        //℃温度曲线的数据
        temperData1 = getLineData(texture.arr3);
        // J  焦耳曲线的数据
        initJouleData = getLineData(texture.arr4);

        //通过查询数据库判断是焦耳还是功率
        MyModel myMode = texture.myModel;
        int jouleOrPower = myMode.JouleOrPower; //0代表温度曲线，1代表焦耳曲线
        Log.d(TAG, "init: " + jouleOrPower);
//        switch (jouleOrPower) {
//            case 0:
//                generateInitialLineData();//生成温度曲线
//                break;
//            case 1:
//                generateJouleChart();
//                break;
//        }
        generateInitialLineData();//生成温度曲线
    }
    //显示为焦耳Chart
    private void generateJouleChart() {
        touchInChart = TOUCH_FOR_POWER_CHART;
        //X轴
        if (axisX == null) {
            List<AxisValue> axisValues_x = new ArrayList<AxisValue>();
            for (int i = 0; i < 11; i++) {
                axisValues_x.add(new AxisValue(i).setLabel(axisData[i]));
            }
            axisX = new Axis(axisValues_x);
            axisX.setHasLines(true);
        }
        //Y轴
        Axis axisY = new Axis();
        axisY.setHasLines(true);
        axisY.setMaxLabelChars(3);
        List<AxisValue> axisValuesY = new ArrayList<>();
        for (int i = 0; i <= 200; i += 40) {
            axisValuesY.add(new AxisValue(i).setLabel(i + ""));
        }
        axisY.setValues(axisValuesY);
        if(jouleLine==null){
            List<PointValue> jouleValues = new ArrayList<>();
            for (float i = 0; i < 11 - 0.5; i += 0.5) {
                jouleValues.add(new PointValue(i, initJouleData[(int) (i * 2)]));
            }
            jouleLine = new Line(jouleValues);
            setLineStyle(jouleLine,JOULE_HIGHLIGHT_CURVE);
        }

    }

    //显示为功率Chart
    private void generateInitialLineData() {
        touchInChart = TOUCH_FOR_POWER_CHART;
        //功率曲线
        if (powerLine == null) {
            List<PointValue> powerValues = new ArrayList<PointValue>();
            for (float i = 0; i < 11 - 0.5; i += 0.5) {
                powerValues.add(new PointValue(i, powerData1[(int) (i * 2)]));
            }
            powerLine = new Line(powerValues);
            setLineStyle(powerLine, POWER_HIGHLIGHT_CURVE);
        }
        //虚线---
        if (powerDashLine == null) {
            List<PointValue> dashValue1 = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                dashValue1.add(new PointValue(i, dash1));
            }
            powerDashLine = new Line(dashValue1);
            setLineStyle(powerDashLine, DASH);
        }
        //初始化背后的温度曲线℃
        if (temperLine_back == null) {
            List<PointValue> temperValue_back = new ArrayList<>();
            for (float i = 0; i < 11 - 0.5; i += 0.5) {
                temperValue_back.add(new PointValue(i, temperData1[(int) (i * 2)]));
            }
            temperLine_back = new Line(temperValue_back);
            setLineStyle(temperLine_back, DARK_CURVE);
        } else {
            //温度曲线由明转暗
            temperLine_back = getLine_TemperChart_To_PowerChart(temperLine);
            setLineStyle(temperLine_back, DARK_CURVE);
        }


        //X轴
        if (axisX == null) {
            List<AxisValue> axisValues_x = new ArrayList<AxisValue>();
            for (int i = 0; i < 11; i++) {
                axisValues_x.add(new AxisValue(i).setLabel(axisData[i]));
            }
            axisX = new Axis(axisValues_x);
            axisX.setHasLines(true);
        }
        //Y轴
        Axis axisY = new Axis();
        axisY.setHasLines(true);
        axisY.setMaxLabelChars(3);
        List<AxisValue> axisValuesY = new ArrayList<>();
        for (int i = 0; i <= 200; i += 40) {
            axisValuesY.add(new AxisValue(i).setLabel(i + ""));
        }
        axisY.setValues(axisValuesY);

        //所有的线的集合
        List<Line> lines = new ArrayList<Line>();
        lines.add(powerDashLine);
        lines.add(temperLine_back);
        lines.add(powerLine);

        lineData = new LineChartData(lines);
        lineData.setAxisXBottom(axisX);
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
        myChart.setOnTouchListener(new myChartTouchListener());

        setAboveAndNextButtonState(powerMoveList, powerIndex);
        currentAxisY_Values = axisValuesY;
        currentMainLine = powerLine;
        currentDashLine = powerDashLine;
        currentIndex = powerIndex;
        currentMoveLineList = powerMoveList;
        currentDashLineList = dashListInPower;
    }

    //显示为温度Chart
    private void generateInitialTemperChart() {
        touchInChart = TOUCH_FOR_TEMPER_CHART;
        //Y轴坐标
        Axis axisY_In_TemperChart = new Axis();
        axisY_In_TemperChart.setHasLines(true);
        axisY_In_TemperChart.setMaxLabelChars(3);
        List<AxisValue> axisValuesY_in_temperChart = new ArrayList<>();
        for (int i = 100; i <= 300; i += 40) {
            axisValuesY_in_temperChart.add(new AxisValue(i).setLabel(i + ""));
        }

        axisY_In_TemperChart.setValues(axisValuesY_in_temperChart);
        axisY_In_TemperChart.setHasLines(true);

        //功率曲线切换成阴暗 w
        backPowerLine = getLine_PowerChart_To_TemperChart(powerLine);
        setLineStyle(backPowerLine, DARK_CURVE);

        //温度曲线由暗转明 c
        temperLine = getLine_PowerChart_To_TemperChart(temperLine_back);
        setLineStyle(temperLine, TEMPER_HIGHLIGHT_CURVE);

        //温度曲线的初始化数据存储
        List<PointValue> temperValues = temperLine.getValues();
        int[] arr = new int[21];
        for (int i = 0; i < temperValues.size(); i++) {
            arr[i] = (int) temperValues.get(i).getY();
        }

        if (temperDataList.size() == 0) {
            temperDataList.add(arr);
            dashListInTemper.add(temperDashValue);
        }
        //TemperChart中的虚线
        temperDashLine = getDashLineByValue(dashListInTemper.get(temperIndex));
        setLineStyle(temperDashLine, DASH);

        //新chart线的集合
        List<Line> lineList = new ArrayList<>();
        lineList.add(temperDashLine);
        lineList.add(backPowerLine);
        lineList.add(temperLine);

        //把曲线和 chart 通过 LineChartData 联系起来
        LineChartData lineChartData = new LineChartData(lineList);
        lineChartData.setAxisYLeft(axisY_In_TemperChart);
        lineChartData.setAxisXBottom(axisX);
        lineChartData.setValueLabelsTextColor(Color.BLACK);
        myChart.setLineChartData(lineChartData);

        myChart.setViewportCalculationEnabled(false);
        Viewport v = new Viewport(0, 300, 10, 100);
        myChart.setMaximumViewport(v);
        myChart.setCurrentViewport(v);
        myChart.setZoomType(ZoomType.HORIZONTAL);
        myChart.setMaxZoom((float) 10);
        Viewport vp = new Viewport(null);
        vp.left = 0;
        vp.right = 4;//显示的点
        vp.bottom = 0;
        vp.top = 300;
        myChart.setCurrentViewport(vp);
        myChart.setZoomEnabled(false);
        myChart.setOnTouchListener(new myChartTouchListener());


        //切换时控制above按钮和next按钮的显示
        setAboveAndNextButtonState(temperDataList, temperIndex);
        currentAxisY_Values = axisValuesY_in_temperChart;
        currentMainLine = temperLine;
        currentDashLine = temperDashLine;
        currentIndex = temperIndex;
        currentMoveLineList = temperDataList;
        currentDashLineList = dashListInTemper;
    }
    private int[] getLineData(String string) {
        String[] splited = string.split(",");
        int[] data = new int[splited.length];
        for (int i = 0; i < splited.length; i++) {
            data[i] = Integer.parseInt(splited[i]);
        }
        return data;
    }

    private void initDate() {
        chartComputator = myChart.getChartComputator();
        PointValue a = new PointValue(0, 100);
        PointValue b = new PointValue(0, 101);
        rate = chartComputator.computeRawY(b.getY()) - chartComputator.computeRawY(a.getY());
        Log.d(TAG, "initDate: rate:  "+rate);
    }
    private class myChartTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    clickX = (int) motionEvent.getX();
                    clickY = (int) motionEvent.getY();
                    selectedValue = selectPoint(currentMainLine.getValues());//选中点
                    Log.d(TAG, "onTouch: " + clickX + "  ---  " + clickY);
                    if (selectedValue == null) {
                        selectedDashLine = selectedDashLine(motionEvent, currentDashLine);    //选中虚线
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    int moveX = (int) motionEvent.getX();
                    int moveY = (int) motionEvent.getY();
                    int move = moveY - clickY;
                    Log.d(TAG, "onTouch: " + moveX + "  -----  " + moveY);

                    if (selectedDashLine != null) {           //一起移动
                        dataChanged = true;
                        //移动虚线
                        List<PointValue> dashLineValues = selectedDashLine.getValues();
                        for (PointValue value : dashLineValues) {
                            float currentY = value.getY() - move * rate;
                            if (currentY < currentAxisY_Values.get(0).getValue()) {
                                currentY = currentAxisY_Values.get(0).getValue();
                            } else if (currentY > currentAxisY_Values.get(5).getValue()) {
                                currentY = currentAxisY_Values.get(5).getValue();
                            }
                            value.set(value.getX(), currentY);
                        }
                        //移动功率线所有的点
                        for (PointValue value : currentMainLine.getValues()) {
                            float currentY = value.getY() - move * rate;
                            if (currentY < currentAxisY_Values.get(0).getValue()) {
                                currentY = currentAxisY_Values.get(0).getValue();
                            } else if (currentY > currentAxisY_Values.get(5).getValue()) {
                                currentY = currentAxisY_Values.get(5).getValue();
                            }
                            value.set(value.getX(), currentY);
                        }
                        float translateY = (float) (Math.round(dashLineValues.get(0).getY() * 10)) / 10;
                        dash.setVisibility(View.VISIBLE);
                        tvDashY.setText(translateY + "");

                    } else if (selectedValue != null) {   //单点移动
                        dataChanged = true;
                        Log.d(TAG, "onTouch: " + moveX + "  -----  " + moveY);
                        float currentX = selectedValue.getX();
                        float currentY = selectedValue.getY() - move * rate;
                        Log.d(TAG, "onTouch: " + "找到点" + selectedValue.toString());
                        //控制范围
                        if (currentY < currentAxisY_Values.get(0).getValue()) {
                            currentY = currentAxisY_Values.get(0).getValue();
                        } else if (currentY > currentAxisY_Values.get(5).getValue()) {
                            currentY = currentAxisY_Values.get(5).getValue();
                        }
                        //        powerLine.getValues().get((int) (selectedValue.getX() * 2)).set(currentX, currentY);
                        selectedValue.set(currentX, currentY);
                        float translateY = (float) (Math.round(currentY * 10)) / 10;
                        valueX.setText(currentX + "");
                        valueY.setText(translateY + "");
                        showTemper.setVisibility(View.VISIBLE);
                    }
                    myChart.invalidate();
                    clickY = moveY;
                    break;
                case MotionEvent.ACTION_UP:
                    showTemper.setVisibility(View.GONE);
                    dash.setVisibility(View.GONE);
                    selectedDashLine = null;
                    if (selectedValue != null) {
                        Log.d(TAG, "onTouch: 抬起的点" + (int) (selectedValue.getY()));

                       /* powerData1[(int) (selectedValue.getX()*2)]= (int) selectedValue.getY();
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < 21; j++) {

                            if (j != 20) {
                                sb = sb.append(powerData1[j] + ",");
                            } else {
                                sb = sb.append(powerData1[j]);
                            }
                        }
                        texture.arr1 = sb.toString();
                        texture.save();*/
                    }
                    //如果曲线有变动,记录存储
                    if (dataChanged) {

                        for (int i = 0; i < currentMoveLineList.size(); i++) {
                            if (i > currentIndex) {
                                currentMoveLineList.remove(currentMoveLineList.get(i));
                                currentDashLineList.remove(currentDashLineList.get(i));
                            }
                        }

                        List<PointValue> values = currentMainLine.getValues();
                        int[] arr = new int[21];
                        for (int i = 0; i < values.size(); i++) {
                            arr[i] = (int) values.get(i).getY();
                        }
                        currentMoveLineList.add(arr);
                        currentDashLineList.add((int) currentDashLine.getValues().get(0).getY());
                        currentIndex++;
                        //设置按钮状态
                        if (touchInChart == TOUCH_FOR_POWER_CHART) {
                            powerIndex = currentIndex;
                        } else if (touchInChart == TOUCH_FOR_TEMPER_CHART) {
                            temperIndex = currentIndex;
                        }
                        setAboveAndNextButtonState(currentMoveLineList, currentIndex);
                        Log.d(TAG, "onTouch: " + currentIndex + "  powerIndex  " + powerIndex);
                        dataChanged = false;
                    }
                    break;
            }
            return false;
        }
    }


    private PointValue selectPoint(List<PointValue> pointValues) {
        int backgroundGridWidth = MyUtils.dip2px(BezierActivity.this, 45);
        int width = backgroundGridWidth / 2;
        Region r = new Region();
        for (PointValue value : pointValues) {
            r.set(clickX - width, clickY - width, clickX + width, clickY + width);
            int pointX = (int) chartComputator.computeRawX(value.getX());
            int pointY = (int) chartComputator.computeRawY(value.getY());
            if (r.contains(pointX, pointY)) {
                return value;
            }
        }
        return null;
    }

    private Line selectedDashLine(MotionEvent motionEvent, Line dashLine) {
        int backgroundGridWidth = MyUtils.dip2px(BezierActivity.this, 40);
        int width = backgroundGridWidth / 2;
        float downY = motionEvent.getY();
        float y = dashLine.getValues().get(0).getY();
        float v = chartComputator.computeRawY(y);

        if (downY > v - width && downY < v + width) {
            return dashLine;

        }
        return null;
    }

    private boolean waveIsTrue = false;

    private class myClickListener implements ClickImageView.OnClickListener {
        @Override
        public void OnClick(ClickImageView view) {
            switch (view.getId()) {
                case R.id.btn_back:
                    finish();
                    break;
                case R.id.btn_above:
                    //                    if(waveIsTrue){
                    //                        btn_waveBehind.performClick();
                    //                        waveIsTrue=false;
                    //                    }

                    if (touchInChart == TOUCH_FOR_POWER_CHART) {
                        powerIndex--;
                        setAboveAndNextButtonState(powerMoveList, powerIndex);
                        startAnimation(powerMoveList, powerIndex, dashListInPower);
                        if (waveIsTrue) {
                            btn_waveBehind.setVisibility(View.GONE);
                            btnWave.setVisibility(View.VISIBLE);
                            powerMoveList.remove(powerMoveList.get(powerMoveList.size() - 1));
                            setAboveAndNextButtonState(powerMoveList, powerIndex);
                            waveIsTrue = false;
                        }
                    } else if (touchInChart == TOUCH_FOR_TEMPER_CHART) {
                        temperIndex--;
                        setAboveAndNextButtonState(temperDataList, temperIndex);
                        startAnimation(temperDataList, temperIndex, dashListInTemper);
                        if (waveIsTrue) {
                            btn_waveBehind.setVisibility(View.GONE);
                            btnWave.setVisibility(View.VISIBLE);
                            temperDataList.remove(temperDataList.get(temperDataList.size() - 1));
                            setAboveAndNextButtonState(temperDataList, temperIndex);
                            waveIsTrue = false;
                        }
                    }
                    break;
                case R.id.btn_next:
                    if (touchInChart == TOUCH_FOR_POWER_CHART) {
                        powerIndex++;
                        setAboveAndNextButtonState(powerMoveList, powerIndex);
                        startAnimation(powerMoveList, powerIndex, dashListInPower);
                    } else if (touchInChart == TOUCH_FOR_TEMPER_CHART) {
                        temperIndex++;
                        setAboveAndNextButtonState(temperDataList, temperIndex);
                        startAnimation(temperDataList, temperIndex, dashListInTemper);
                    }
                    break;
                case R.id.btn_switch:
                    if (touchInChart == TOUCH_FOR_POWER_CHART) {
                        generateInitialTemperChart();
                    } else if (touchInChart == TOUCH_FOR_TEMPER_CHART) {
                        generateInitialLineData();
                    }
                    break;
                case R.id.btn_wave:
                    waveIsTrue = true;
                    btnWave.setVisibility(View.GONE);
                    btn_waveBehind.setVisibility(View.VISIBLE);
                    if (touchInChart == TOUCH_FOR_POWER_CHART) {
                        powerIndex++;
                        changeForWave(powerIndex, powerLine, powerDashLine);
                    } else if (touchInChart == TOUCH_FOR_TEMPER_CHART) {
                        temperIndex++;
                        changeForWave(temperIndex, temperLine, temperDashLine);
                    }
                    //                    Log.d(TAG, "OnClick: pl: "+powerMoveList.size()+" index : "+powerIndex);
                    break;
                case R.id.btn_waveBehind:
                    btn_waveBehind.setVisibility(View.GONE);
                    btnWave.setVisibility(View.VISIBLE);
                    if (touchInChart == TOUCH_FOR_POWER_CHART) {
                        powerIndex--;
                        changeForBehindWave(powerIndex, powerLine);
                    } else if (touchInChart == TOUCH_FOR_TEMPER_CHART) {
                        temperIndex--;
                        changeForBehindWave(temperIndex, temperLine);
                    }
                    Log.d(TAG, "OnClick: powerMoveList: " + powerMoveList.size() + " index " + powerIndex);
                    break;
            }
        }
    }

    private void changeForBehindWave(int index, Line line) {
        currentMoveLineList.remove(currentMoveLineList.size() - 1);
        currentDashLineList.remove(currentDashLineList.size() - 1);
        setAboveAndNextButtonState(currentMoveLineList, index);

        List<PointValue> values = line.getValues();
        for (int i = 0; i < values.size(); i++) {
            values.get(i).setTarget(values.get(i).getX(), currentMoveLineList.get(currentMoveLineList.size() - 1)[i]);
        }
        myChart.startDataAnimation(300);
    }

    private void changeForWave(int index, Line Line, Line dashLine) {

        List<PointValue> values = Line.getValues();
        for (PointValue value : values) {
            value.setTarget(value.getX(), dashLine.getValues().get(0).getY());
        }
        myChart.startDataAnimation(300);

        List<PointValue> vs = Line.getValues();
        int[] arr = new int[21];
        for (int i = 0; i < vs.size(); i++) {
            arr[i] = (int) vs.get(i).getY();
        }
        currentMoveLineList.add(arr);
        currentDashLineList.add((int) dashLine.getValues().get(0).getY());
        setAboveAndNextButtonState(currentMoveLineList, index);
    }


    private void setAboveAndNextButtonState(List<int[]> list, int index) {
        if (index == 0 && list.size() == 1) {
            btnNext.setVisibility(View.GONE);
            nextDisable.setVisibility(View.VISIBLE);
            btnAbove.setVisibility(View.GONE);
            aboveDisable.setVisibility(View.VISIBLE);
        } else if (index < list.size() - 1 && index > 0) {
            aboveDisable.setVisibility(View.GONE);
            btnAbove.setVisibility(View.VISIBLE);
            nextDisable.setVisibility(View.GONE);
            btnNext.setVisibility(View.VISIBLE);
        } else if (index == list.size() - 1) {
            btnNext.setVisibility(View.GONE);
            nextDisable.setVisibility(View.VISIBLE);
            btnAbove.setVisibility(View.VISIBLE);
            aboveDisable.setVisibility(View.GONE);
        } else if (index == 0 && list.size() > 1) {
            btnAbove.setVisibility(View.GONE);
            aboveDisable.setVisibility(View.VISIBLE);
            btnNext.setVisibility(View.VISIBLE);
            nextDisable.setVisibility(View.GONE);
        }


    }

    private void setLineStyle(Line line, int style) {
        switch (style) {
            case DASH:
                line.setHasPoints(false);
                line.setStrokeWidth(1);
                PathEffect effect = new DashPathEffect(new float[]{1, 2, 4, 8}, 1);
                line.setPathEffect(effect);
                line.setColor(getResources().getColor(R.color.colorPrimaryDark));
                break;
            case TEMPER_HIGHLIGHT_CURVE:
                line.setHasPoints(true);
                line.setCubic(true);
                line.setStrokeWidth(4);
                line.setHasLabelsOnlyForSelected(true);
                line.setPointColor(getResources().getColor(R.color.colorWhite));
                line.setColor(getResources().getColor(R.color.temperOrange));
                break;
            case DARK_CURVE:
                line.setHasPoints(false);
                line.setCubic(true);
                line.setStrokeWidth(3);
                line.setColor(getResources().getColor(R.color.gray));
                break;
            case POWER_HIGHLIGHT_CURVE:
                line.setHasPoints(true);
                line.setCubic(true);
                line.setHasLabelsOnlyForSelected(true);
                line.setColor(getResources().getColor(R.color.colorWhite));
                break;
            case JOULE_HIGHLIGHT_CURVE:
                line.setHasPoints(true);
                line.setCubic(true);
                line.setHasLabelsOnlyForSelected(true);
                line.setColor(getResources().getColor(R.color.jouleGreen));
                break;

        }
    }

    //通过一个高度值获得一条虚线
    private Line getDashLineByValue(int valueY) {
        List<PointValue> dashValue = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            dashValue.add(new PointValue(i, valueY));
        }
        return new Line(dashValue);
    }


    //曲线由功率chart转到温度chart后的状态
    private Line getLine_PowerChart_To_TemperChart(Line line) {
        List<PointValue> pointValues = line.getValues();
        List<PointValue> backPointValues = new ArrayList<>();
        for (PointValue pointValue : pointValues) {
            backPointValues.add(new PointValue(pointValue.getX(), pointValue.getY() + 100));
        }
        return new Line(backPointValues);
    }

    //温度chart到功率chart
    private Line getLine_TemperChart_To_PowerChart(Line line) {
        List<PointValue> pointValues = line.getValues();
        List<PointValue> valuesInPower = new ArrayList<>();
        for (PointValue pointValue : pointValues) {
            valuesInPower.add(new PointValue(pointValue.getX(), pointValue.getY() - 100));
        }
        return new Line(valuesInPower);
    }


    //变换的动画
    //    private void startAnimation(int powerIndex) {
    //        myChart.cancelDataAnimation();
    //        int[] currentPower = powerMoveList.get(powerIndex);
    //        int currentDash = dashListInPower.get(powerIndex);
    //        List<PointValue> powerValues = powerLine.getValues();
    //        List<PointValue> dashValues = powerDashLine.getValues();
    //        Log.d(TAG, "OnClick: " + "size:  " + powerValues.size() + "dash  " + dashValues.size());
    //        for (int i = 0; i < powerValues.size(); i++) {
    //            powerValues.get(i).setTarget(powerValues.get(i).getX(), currentPower[i]);
    //        }
    //        for (int i = 0; i < dashValues.size(); i++) {
    //            dashValues.get(i).setTarget(dashValues.get(i).getX(), currentDash);
    //        }
    //        myChart.startDataAnimation(300);
    //    }

    private void startAnimation(List<int[]> dataList, int Index, List<Integer> dashList) {
        myChart.cancelDataAnimation();
        int[] currentPower = dataList.get(Index);
        int currentDash = dashList.get(Index);
        List<PointValue> powerValues = currentMainLine.getValues();
        List<PointValue> dashValues = currentDashLine.getValues();

        for (int i = 0; i < powerValues.size(); i++) {
            powerValues.get(i).setTarget(powerValues.get(i).getX(), currentPower[i]);
        }
        for (int i = 0; i < dashValues.size(); i++) {
            dashValues.get(i).setTarget(dashValues.get(i).getX(), currentDash);
        }
        myChart.startDataAnimation(300);
    }
}
