package com.yihai.wu.sxi;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.Region;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yihai.wu.appcontext.ConnectedBleDevices;
import com.yihai.wu.appcontext.MyModel;
import com.yihai.wu.appcontext.Textures;
import com.yihai.wu.util.ClickImageView;
import com.yihai.wu.util.DisEnableImageView;
import com.yihai.wu.util.MyUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

import static com.yihai.wu.util.MyUtils.BinaryToHexString;
import static com.yihai.wu.util.MyUtils.byteMerger;
import static com.yihai.wu.util.MyUtils.intToBytes;
import static com.yihai.wu.util.MyUtils.subBytes;

/**
 * Created by ${Wu} on 2017/3/24.
 */

public class HelloActivity extends AppCompatActivity {
    private static final String TAG = "HelloActivity";
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
    @Bind(R.id.dashLayout)
    LinearLayout dashLayout;

    @Bind(R.id.singleUnit)
    TextView singleUnit;
    @Bind(R.id.coupleUnit)
    TextView coupleUnit;
    //创建一个可重用固定线程数的线程池
    ExecutorService pool = Executors.newFixedThreadPool(4);

    String[] axisData = {"0s", "1s", "2s", "3s", "4s", "5s", "6s", "7s", "8s", "9s", "10s"};
    int temperDashValue = 200;
    int jouleDashValue = 50;
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

    //保存数据到设备
    private int saveStyle;
    private static final int SAVE_STYLE_ONE = 0X0A;
    private static final int SAVE_STYLE_TWO = 0X0B;
    private static final int SAVE_STYLE_THREE = 0X0C;
    private static final int SAVE_STYLE_FOUR = 0X0D;


    //集合用来储存操作的历史记录。。。
    int powerIndex = 0;
    List<int[]> powerMoveList = new ArrayList();
    List<Integer> dashListInPower = new ArrayList<>();

    int temperIndex = 0;
    List<int[]> temperDataList = new ArrayList<>();
    List<Integer> dashListInTemper = new ArrayList<>();

    int jouleIndex = 0;
    List<int[]> jouleDataList = new ArrayList<>();
    List<Integer> dashListInJoule = new ArrayList<>();

    //临时中转存储
    int currentIndex;
    List<int[]> currentMoveLineList = new ArrayList<>();
    List<Integer> currentDashLineList = new ArrayList<>();
    private List<AxisValue> currentAxisY_Values = new ArrayList<>();  //当前Y轴坐标
    private Line currentDashLine;
    private Line selectedDashLine;
    private Line currentMainLine;

    private ChartComputator chartComputator;
    private float rate = 0;
    private int clickX;
    private int clickY;
    private PointValue selectedValue;
    private int[] powerData1;
    private int powerDashValue = 50;
    private int[] temperData1;
    private Line powerLine;
    private Line powerDashLine;
    private Line temperLine_back;
    private Line temperLine;
    private Line backPowerLine;
    private Line temperDashLine;


    private Line jouleLine;
    private Line backJouleLine;
    private Line jouleDashline;
    private int jouleOrPower;
    private byte[] oneFirst;

    private int littleOrder;
    private boolean begin = false;
    private boolean waveInPowerIsTrue = true;
    private boolean waveInJouleIsTrue = true;
    private boolean waveInTemperIsTrue = true;
    int count = 0;
    private StringBuilder sb1 = new StringBuilder();
    int curveNum = 0;
    private final BroadcastReceiver setHelloActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothLeService.ACTION_DATA_RX:
                    Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
                    byte[] data = bundle.getByteArray("byteValues");
                    String s = BinaryToHexString(data);
                    Log.e(TAG, "onReceive: allReceive   :    " + s);
                    if (startReadPowerCurveData) {
                        Log.e("ReadPowerCurveData", s);
                        //处理收到的功率曲线的数据
                        handlerCurveData(data);
                    }
                    if (startReadTempCurveData) {
                        Log.e("ReadTempCurveData", s);
                        //处理收到的温度曲线的数据
                        handlerCurveData(data);
                    }
                    // 读取到的虚线
                    if (startReadMiddleLine) {
                        Sys_YiHi_Protocol_RX_Porc(data);
                    }
                    if (saveCurve) {
                        Sys_YiHi_Protocol_RX_Porc(data);
                    }
                    break;
                case BluetoothLeService.ACTION_LAND_SUCCESS:
                    startActivity(new Intent(HelloActivity.this, MainActivity.class));
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    startActivity(new Intent(HelloActivity.this, MainActivity.class));
                    break;
            }
        }
    };

    private boolean startReadTempCurveData = false;
    private int isConnected;
    private ProgressDialog initDialog;
    private boolean startReadMiddleLine = false;
    private boolean tempToPower = false;
    private boolean tempToJoule = false;
    private ProgressDialog waitingDialog;
    private boolean saveCurve = false;
    private int temperatureUnit;


    private void handlerCurveData(byte[] data) {
        Sys_YiHi_Protocol_RX_Porc(data);
        if (begin) {
            littleOrder++;
        }

        if (TAG == "HelloActivity" && littleOrder == 1 && begin == true) {
            oneFirst = subBytes(data, 11, 9);
            //                        Log.e(TAG, "onFirst: "+BinaryToHexString(oneFirst));
        } else if (TAG == "HelloActivity" && littleOrder == 2 && begin == true) {
            oneFirst = byteMerger(oneFirst, data);
            //                        Log.e(TAG, "onFirst: "+BinaryToHexString(oneFirst));
        } else if (TAG == "HelloActivity" && littleOrder == 3 && begin == true) {
            oneFirst = byteMerger(oneFirst, data);
            //                        Log.e(TAG, "onFirst: "+BinaryToHexString(oneFirst));
        } else if (TAG == "HelloActivity" && littleOrder == 4 && begin == true) {
            oneFirst = byteMerger(oneFirst, data);
            Log.e(TAG, "byteMergerData: " + BinaryToHexString(oneFirst));
            //处理取到的50个数据byte，每2个组成一个数据

            for (int i = 0; i < oneFirst.length; i += 2) {

                byte[] bs = new byte[2];
                bs[0] = oneFirst[i];
                bs[1] = oneFirst[i + 1];
                int powerData = ((bs[0] & 0xff) << 8) | (bs[1] & 0xff);
                count++;
                if (count % 2 == 1) {
                    if (startReadPowerCurveData) {
                        sb1.append(powerData / 10 + ",");
                    } else if (startReadTempCurveData) {
                        sb1.append(powerData + ",");
                    }
                } else if (bigOrder == 4 && i == 48) {

                    bigOrder = 0;
                    if (startReadPowerCurveData) {
                        sb1.append(powerData / 10);
                    } else if (startReadTempCurveData) {
                        sb1.append(powerData);
                    }

                    Log.e(TAG, "byteMergerData: " + sb1 + "       curveNum:  " + curveNum);
                    count = 0;
                    if (curveNum == 0) {
                        //                        readTexture = Textures.getTexture(settingPackage, customName);
                        int[] lineData = getLineData(sb1.toString(), 5);
                        //                        readTexture.arr1 = sb1.toString();
                        //                        readTexture.arr4 = sb1.toString();
                        powerMoveList.add(lineData);
                        jouleDataList.add(lineData);
                        count = 0;
                        sb1.setLength(0);

                        startReadPowerCurveData = false;
                        startReadTempCurveData = true;
                        //功率曲线数据保存完毕、       准备读取温度曲线
                        settingPackage_PowerCurve_ReadData(settingPackageOrder, (byte) 0x03, (byte) 0x00, (byte) 0x04, userOrder, 50);
                    } else if (curveNum == 1) {
                        String temperData = sb1.toString();
                        //                        readTexture.arr3 = getTemperData(temperData);
                        int[] lineData = getLineData(sb1.toString(), 5);
                        //-----------------------------------
                        //      进入时背后的温度曲线
                        List<PointValue> temperValue_back = new ArrayList<>();
                        for (float j = 0; j <= 10; j += 0.2) {
                            temperValue_back.add(new PointValue(j, lineData[(int) (j * 5)]));
                        }
                        temperLine_back = new Line(temperValue_back);
                        setLineStyle(temperLine_back, DARK_CURVE);
                        int [] firstData = new int[51];
                        for (int i1 = 0; i1 < lineData.length; i1++) {
                            firstData[i1] = lineData[i1]+100;
                        }

                        temperDataList.add(firstData);
                        //-----------------------------------------

                        count = 0;
                        Log.e(TAG, "ReadTempCurveData: " + sb1);
                        sb1.setLength(0);

                        startReadTempCurveData = false;
                        startReadMiddleLine = true;
                        //温度曲线读完、   读取中线
                        settingPackage_PowerCurve_ReadData(settingPackageOrder, (byte) 0x02, (byte) 0x00, (byte) 0x01, userOrder, 2);
                    } else if (curveNum == 2) {


                    }
                    curveNum++;
                }

            }
            Log.e(TAG, "handlerCurveData onReceive: " + littleOrder + "  " + begin + "   " + bigOrder + "  data--->SB:   " + sb1);
        }
        if (littleOrder == 4 && begin && bigOrder == 1) {
            begin = false;
            littleOrder = 0;
            //取第二组25个点
            Log.e(TAG, "handlerCurveData: 取第二组25个数组");
            if (startReadPowerCurveData) {
                settingPackage_PowerCurve_ReadData(settingPackageOrder, (byte) 0x01, (byte) 0x01, (byte) 0x04, userOrder, 50);
            }
            if (startReadTempCurveData) {
                settingPackage_PowerCurve_ReadData(settingPackageOrder, (byte) 0x03, (byte) 0x01, (byte) 0x04, userOrder, 50);
            }
        } else if (littleOrder == 4 && begin && bigOrder == 2) {
            begin = false;
            littleOrder = 0;
            //取第三组25个点
            Log.e(TAG, "handlerCurveData: 取第三组25个数组");
            if (startReadPowerCurveData) {
                settingPackage_PowerCurve_ReadData(settingPackageOrder, (byte) 0x01, (byte) 0x02, (byte) 0x04, userOrder, 50);
            }
            if (startReadTempCurveData) {
                settingPackage_PowerCurve_ReadData(settingPackageOrder, (byte) 0x03, (byte) 0x02, (byte) 0x04, userOrder, 50);
            }
        } else if (littleOrder == 4 && begin && bigOrder == 3) {
            begin = false;
            littleOrder = 0;
            //取第四组25个点
            Log.e(TAG, "handlerCurveData: 取第四组25个数组");
            if (startReadPowerCurveData) {
                settingPackage_PowerCurve_ReadData(settingPackageOrder, (byte) 0x01, (byte) 0x03, (byte) 0x04, userOrder, 50);
            }
            if (startReadTempCurveData) {
                settingPackage_PowerCurve_ReadData(settingPackageOrder, (byte) 0x03, (byte) 0x03, (byte) 0x04, userOrder, 50);
            }
        }
    }

    private String settingPackage;
    private String customName;
    private byte settingPackageOrder;
    private byte userOrder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_bezier);
        ButterKnife.bind(this);

        waitingDialog = new ProgressDialog(HelloActivity.this);
        waitingDialog.setTitle(R.string.point_out_title);
        waitingDialog.setIcon(R.mipmap.app_icon);
        waitingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        waitingDialog.setMessage(this.getString(R.string.Modifying));
        waitingDialog.setIndeterminate(true);
        waitingDialog.setCancelable(false);

        Intent intent = getIntent();
        int state = intent.getIntExtra("state", 0);
        MyModel selectedModel = MyModel.getSelectedModel();
        settingPackage = selectedModel.model;
        switch (settingPackage) {
            case "C1":
                settingPackageOrder = 0x00;
                break;
            case "C2":
                settingPackageOrder = 0x01;
                break;
            case "C3":
                settingPackageOrder = 0x02;
                break;
            case "C4":
                settingPackageOrder = 0x03;
                break;
            case "C5":
                settingPackageOrder = 0x04;
                break;
        }
        customName = intent.getStringExtra("custom");
        switch (customName) {
            case "S1":
                userOrder = 0x00;
                break;
            case "S2":
                userOrder = 0x01;
                break;
            case "S3":
                userOrder = 0x02;
                break;
            case "S4":
                userOrder = 0x03;
                break;
            case "S5":
                userOrder = 0x04;
                break;
        }
        ConnectedBleDevices connectedDevice = ConnectedBleDevices.getConnectedDevice();
        Log.e(TAG, "onCreate:   获取连接的设备： " + connectedDevice);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        registerReceiver(setHelloActivityReceiver, makeBroadcastFilter());
        chartComputator = myChart.getChartComputator();
        //初始化
        if (state == 2) {
            initDialog = new ProgressDialog(HelloActivity.this);
            initDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            initDialog.setMessage(this.getString(R.string.reading));
            initDialog.show();
        } else {
            init();
        }
        myListeners();//监听
    }

    private static IntentFilter makeBroadcastFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RX);
        intentFilter.addAction(BluetoothLeService.ACTION_LAND_SUCCESS);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);

        return intentFilter;
    }

    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic g_Character_TX;
    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("service", "Unable to initialize Bluetooth");
                finish();
            }
            Log.e(TAG, "onServiceConnected: HelloActivity service connect");
            g_Character_TX = mBluetoothLeService.getG_Character_TX();
            if (g_Character_TX != null && mBluetoothLeService.getTheConnectedState() == 2) {

                startReadPowerCurveData = true;
                Log.e(TAG, "onServiceConnected: " + "开始查询数据   " + settingPackageOrder + "   " + userOrder);
                //   功率曲线上的点的数据   序号00     第一组的25个点
                settingPackage_PowerCurve_ReadData(settingPackageOrder, (byte) 0x01, (byte) 0x00, (byte) 0x04, userOrder, 50);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("service", "onServiceDisconnected: " + "---------服务未连接-------------");
            mBluetoothLeService = null;
        }
    };

    private void myListeners() {
        btnBack.setOnClickListener(new HelloActivity.myClickListener());
        btnAbove.setOnClickListener(new HelloActivity.myClickListener());
        btnNext.setOnClickListener(new HelloActivity.myClickListener());
        btnSwitch.setOnClickListener(new HelloActivity.myClickListener());
        btnWave.setOnClickListener(new HelloActivity.myClickListener());
        btn_waveBehind.setOnClickListener(new HelloActivity.myClickListener());
        btnSave.setOnClickListener(new HelloActivity.myClickListener());
    }

    private void init() {
        Log.e(TAG, "init: " + settingPackage + "   " + customName);
        //查询数据库、
        Textures texture = Textures.getTexture(settingPackage, customName);

        //        Log.e(TAG, "init: " + texture.arr1 + "    " + texture.arr3 + "    " + texture.dash + "    " + dashListInTemper);
        //功率曲线的数据
        //        powerData1 = getLineData(texture.arr1, 5);
        //        powerMoveList.add(powerData1);
        //虚线1的数据
        //        powerDashValue = texture.dash;
        if (powerDashValue > 200 || powerDashValue < 0) {
            powerDashValue = 5;
        }


        //        jouleDashValue = texture.dash;
        if (jouleDashValue > 200 || jouleDashValue < 0) {
            jouleDashValue = 5;
        }

        //虚线2的数据
        //        temperDashValue = texture.dashValueInTemper;
        if (temperDashValue > 300 || temperDashValue < 100) {
            temperDashValue = 105;
        }

        //℃温度曲线的数据

        //        temperData1 = getLineData(texture.arr3, 5);


        // J  焦耳曲线的数据

        //        initJouleData = getLineData(texture.arr4, 5);
        //通过查询数据库判断是焦耳还是功率
        MyModel myMode = texture.myModel;
        //0代表功率曲线，1代表焦耳曲线
        jouleOrPower = myMode.JouleOrPower;
        //单位
        temperatureUnit = myMode.temperatureUnit;
        Log.e(TAG, "temperatureUnit init: " + temperatureUnit);
        //        Log.e(TAG, "init: powerDashValue    j/p: " + jouleOrPower + "   powerDashValue  " + powerDashValue + "  temperDashValue   " + temperDashValue);
        switch (jouleOrPower) {
            case 0:
                generateInitialLineData();//生成功率曲线
                break;
            case 1:
                generateJouleChart();  //生成焦耳曲线
                break;
        }

    }

    //显示为焦耳Chart
    private void generateJouleChart() {
        touchInChart = TOUCH_FOR_JOULE_CHART;
        //X轴
        List<AxisValue> axisValues_x = new ArrayList<AxisValue>();
        for (int i = 0; i < 11; i++) {
            axisValues_x.add(new AxisValue(i).setLabel(axisData[i]));
        }
        Axis axisX = new Axis(axisValues_x);
        axisX.setHasLines(true);
        axisX.setValues(axisValues_x);

        //Y轴
        Axis axisY = new Axis();
        axisY.setHasLines(true);
        axisY.setMaxLabelChars(3);
        List<AxisValue> axisValuesY = new ArrayList<>();
        for (int i = 0; i <= 200; i += 40) {
            axisValuesY.add(new AxisValue(i).setLabel(i + ""));
        }
        axisY.setValues(axisValuesY);

        //高亮显示的joule曲线
        if (jouleLine == null && jouleDataList.size() == 0) {
            List<PointValue> jouleValues = new ArrayList<>();
            int[] initJouleData = new int[51];
            for (float i = 0; i <= 10; i += 0.2) {
                int a = 10 + (int) (Math.random() * 175);
                initJouleData[(int) (i * 5)] = a;
                jouleValues.add(new PointValue(i, a));
            }
            jouleDataList.add(initJouleData);
            jouleLine = new Line(jouleValues);
            setLineStyle(jouleLine, JOULE_HIGHLIGHT_CURVE);
        } else if (jouleLine == null && jouleDataList.size() == 1) {
            List<PointValue> jouleValues = new ArrayList<>();
            int[] data = jouleDataList.get(0);
            for (float i = 0; i <= 10; i += 0.2) {
                jouleValues.add(new PointValue(i, data[(int) (i * 5)]));
            }
            jouleLine = new Line(jouleValues);
            setLineStyle(jouleLine, JOULE_HIGHLIGHT_CURVE);
        }


        //虚线
        if (jouleDashline == null && dashListInJoule.size() == 0) {
            List<PointValue> dashValue = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                dashValue.add(new PointValue(i, jouleDashValue));
            }
            dashListInJoule.add(jouleDashValue);
            jouleDashline = new Line(dashValue);
            setLineStyle(jouleDashline, DASH);
        }

        //初始化背后的温度曲线℃
        if (temperLine_back == null) {
            List<PointValue> temperValue_back = new ArrayList<>();
            for (float i = 0; i <= 10; i += 0.2) {
                int a = 10 + (int) (Math.random() * 175);
                temperValue_back.add(new PointValue(i, a));
            }
            temperLine_back = new Line(temperValue_back);
            setLineStyle(temperLine_back, DARK_CURVE);
        }
        else if (tempToJoule) {
            //            温度曲线由明转暗
            temperLine_back = getLine_TemperChart_To_PowerChart(temperLine);
            setLineStyle(temperLine_back, DARK_CURVE);
        }

        List<Line> lines = new ArrayList<>();
        lines.add(jouleDashline);
        lines.add(temperLine_back);
        lines.add(jouleLine);


        LineChartData data = new LineChartData(lines);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        data.setValueLabelsTextColor(Color.BLACK);
        data.setValueLabelTextSize(20);
        myChart.setLineChartData(data);
        myChart.setViewportCalculationEnabled(false);
        //坐标的视图范围
        Viewport v = new Viewport(0, 210, 10, 0);
        myChart.setMaximumViewport(v);
        myChart.setCurrentViewport(v);
        myChart.setZoomType(ZoomType.HORIZONTAL);
        myChart.setMaxZoom((float) 10);
        Viewport vp = new Viewport(null);
        vp.left = 0;
        vp.right = 2;//显示的点
        vp.bottom = 0;
        vp.top = 210;
        myChart.setCurrentViewport(vp);
        myChart.setZoomEnabled(false);
        myChart.setOnTouchListener(new HelloActivity.myChartTouchListener());

        setAboveAndNextButtonState(jouleDataList, jouleIndex);
        currentAxisY_Values = axisValuesY;
        currentMainLine = jouleLine;
        currentDashLine = jouleDashline;
        currentIndex = jouleIndex;
        currentMoveLineList = jouleDataList;
        currentDashLineList = dashListInJoule;
        if (waveInJouleIsTrue) {
            btnWave.setVisibility(View.VISIBLE);
            btn_waveBehind.setVisibility(View.GONE);
        } else {
            btnWave.setVisibility(View.GONE);
            btn_waveBehind.setVisibility(View.VISIBLE);
        }

    }

    //显示为功率Chart
    private void generateInitialLineData() {
        touchInChart = TOUCH_FOR_POWER_CHART;
        //功率曲线
        if (powerLine == null && powerMoveList.size() == 0) {
            List<PointValue> powerValues = new ArrayList<PointValue>();
            //            for (float i = 0; i < 11 - 0.5; i += 0.5) {
            //                powerValues.add(new PointValue(i, powerData1[(int) (i * 2)]));
            //            }
            int[] initPowerData = new int[51];
            for (float i = 0; i <= 10; i += 0.2) {
                int a = 22 + (int) (Math.random() * 80);
                initPowerData[(int) (i * 5)] = a;
                powerValues.add(new PointValue(i, a));
            }
            powerMoveList.add(initPowerData);

            powerLine = new Line(powerValues);
            setLineStyle(powerLine, POWER_HIGHLIGHT_CURVE);
        } else if (powerLine == null && powerMoveList.size() == 1) {
            List<PointValue> powerValues = new ArrayList<PointValue>();
            int[] data = powerMoveList.get(0);
            for (float i = 0; i <= 10; i += 0.2) {
                powerValues.add(new PointValue(i, data[(int) (i * 5)]));
            }
            powerLine = new Line(powerValues);
            setLineStyle(powerLine, POWER_HIGHLIGHT_CURVE);
        }


        //  功率虚线----
        if (powerDashLine == null && dashListInPower.size() == 0) {
            List<PointValue> dashValue1 = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                dashValue1.add(new PointValue(i, powerDashValue));
            }
            // 用来存储当前功率虚线的集合
            dashListInPower.add(powerDashValue);
            powerDashLine = new Line(dashValue1);
            setLineStyle(powerDashLine, DASH);
        }
        //初始化背后的温度曲线℃
        if (temperLine_back == null) {
            List<PointValue> temperValue_back = new ArrayList<>();
            for (float i = 0; i <= 10; i += 0.2) {
                int a = 10 + (int) (Math.random() * 175);
                temperValue_back.add(new PointValue(i, a));
            }
            temperLine_back = new Line(temperValue_back);
            setLineStyle(temperLine_back, DARK_CURVE);
        }
        else if (tempToPower) {
            //            温度曲线由明转暗
            temperLine_back = getLine_TemperChart_To_PowerChart(temperLine);
            setLineStyle(temperLine_back, DARK_CURVE);
        }

        //X轴
        List<AxisValue> axisValues_x = new ArrayList<AxisValue>();
        for (int i = 0; i < 11; i++) {
            axisValues_x.add(new AxisValue(i).setLabel(axisData[i]));
        }
        Axis axisX = new Axis(axisValues_x);
        axisX.setHasLines(true);
        axisX.setValues(axisValues_x);
        //Y轴
        Axis axisY = new Axis();
        axisY.setHasLines(true);
        axisY.setMaxLabelChars(3);
        List<AxisValue> axisValuesY = new ArrayList<>();
        for (int i = 0; i <= 200; i += 40) {
            axisValuesY.add(new AxisValue(i).setLabel(i + ""));
        }
        axisY.setValues(axisValuesY);
        int maxLabelChars = axisY.getMaxLabelChars();


        //此刻所有的线的集合
        List<Line> lines = new ArrayList<Line>();
        lines.add(powerDashLine);
        lines.add(temperLine_back);
        lines.add(powerLine);

        LineChartData lineData = new LineChartData(lines);
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
        Viewport v = new Viewport(0, 210, 10, 0);
        myChart.setMaximumViewport(v);
        myChart.setCurrentViewport(v);
        myChart.setZoomType(ZoomType.HORIZONTAL);
        myChart.setMaxZoom((float) 10);
        Viewport vp = new Viewport(null);
        vp.left = 0;
        vp.right = 2;//显示的点
        vp.bottom = 0;
        vp.top = 210;
        myChart.setCurrentViewport(vp);
        myChart.setZoomEnabled(false);
        myChart.setOnTouchListener(new HelloActivity.myChartTouchListener());

        setAboveAndNextButtonState(powerMoveList, powerIndex);
        currentAxisY_Values = axisValuesY;
        currentMainLine = powerLine;
        currentDashLine = powerDashLine;
        currentIndex = powerIndex;
        currentMoveLineList = powerMoveList;
        currentDashLineList = dashListInPower;

        if (waveInPowerIsTrue) {
            btnWave.setVisibility(View.VISIBLE);
            btn_waveBehind.setVisibility(View.GONE);
        } else {
            btnWave.setVisibility(View.GONE);
            btn_waveBehind.setVisibility(View.VISIBLE);
        }

    }

    //显示为温度Chart
    private void generateInitialTemperChart() {
        touchInChart = TOUCH_FOR_TEMPER_CHART;
        //X轴
        List<AxisValue> axisValues_x = new ArrayList<AxisValue>();
        for (int i = 0; i < 11; i++) {
            axisValues_x.add(new AxisValue(i).setLabel(axisData[i]));
        }
        Axis axisX = new Axis(axisValues_x);
        axisX.setHasLines(true);
        axisX.setValues(axisValues_x);
        //Y轴
        Axis axisY_In_TemperChart = new Axis();
        axisY_In_TemperChart.setHasLines(true);
        axisY_In_TemperChart.setMaxLabelChars(3);
        List<AxisValue> axisValuesY_in_temperChart = new ArrayList<>();
        for (int i = 100; i <= 300; i += 40) {
            switch (temperatureUnit) {
                case 0:
                    axisValuesY_in_temperChart.add(new AxisValue(i).setLabel(i + ""));
                    break;
                case 1:
                    axisValuesY_in_temperChart.add(new AxisValue(i).setLabel((i * 9 / 5 + 32) + ""));
                    break;
            }

        }

        axisY_In_TemperChart.setValues(axisValuesY_in_temperChart);
        axisY_In_TemperChart.setHasLines(true);

        //温度曲线由暗转明 c
        temperLine = getLine_PowerChart_To_TemperChart(temperLine_back);
        setLineStyle(temperLine, TEMPER_HIGHLIGHT_CURVE);
        //温度曲线的初始化数据存储
        if (temperDataList.size() == 0) {
            List<PointValue> temperValues = temperLine.getValues();
            int[] arr = new int[51];
            for (int i = 0; i < temperValues.size(); i++) {
                arr[i] = (int) temperValues.get(i).getY();
            }
            temperDataList.add(arr);
        }

        if (dashListInTemper.size() == 0) {
            dashListInTemper.add(temperDashValue);
        }

        //TemperChart中的虚线
        temperDashLine = getDashLineByValue(dashListInTemper.get(temperIndex));
        setLineStyle(temperDashLine, DASH);

        //新chart线的集合
        List<Line> lineList = new ArrayList<>();
        lineList.add(temperDashLine);

        switch (jouleOrPower) {
            case 0:
                //功率曲线切换成阴暗   w
                backPowerLine = getLine_PowerChart_To_TemperChart(powerLine);
                setLineStyle(backPowerLine, DARK_CURVE);
                lineList.add(backPowerLine);
                break;
            case 1:
                //焦耳曲线切换成阴暗   j
                backJouleLine = getLine_PowerChart_To_TemperChart(jouleLine);
                setLineStyle(backJouleLine, DARK_CURVE);
                lineList.add(backJouleLine);
                break;
        }
        lineList.add(temperLine);

        //把曲线和 chart 通过 LineChartData 联系起来
        LineChartData lineChartData = new LineChartData(lineList);
        lineChartData.setAxisYLeft(axisY_In_TemperChart);
        lineChartData.setAxisXBottom(axisX);
        lineChartData.setValueLabelsTextColor(Color.BLACK);
        myChart.setLineChartData(lineChartData);

        myChart.setViewportCalculationEnabled(false);
        Viewport v = new Viewport(0, 310, 10, 100);
        myChart.setMaximumViewport(v);
        myChart.setCurrentViewport(v);
        myChart.setZoomType(ZoomType.HORIZONTAL);
        myChart.setMaxZoom((float) 10);
        Viewport vp = new Viewport(null);
        vp.left = 0;
        vp.right = 2;//显示的点
        vp.bottom = 0;
        vp.top = 310;
        myChart.setCurrentViewport(vp);
        myChart.setZoomEnabled(false);
        myChart.setOnTouchListener(new HelloActivity.myChartTouchListener());


        //切换时  above按钮和next按钮 的显示
        setAboveAndNextButtonState(temperDataList, temperIndex);
        currentAxisY_Values = axisValuesY_in_temperChart;
        currentMainLine = temperLine;
        currentDashLine = temperDashLine;
        currentIndex = temperIndex;
        currentMoveLineList = temperDataList;
        currentDashLineList = dashListInTemper;
        if (waveInTemperIsTrue) {
            btnWave.setVisibility(View.VISIBLE);
            btn_waveBehind.setVisibility(View.GONE);
        } else {
            btnWave.setVisibility(View.GONE);
            btn_waveBehind.setVisibility(View.VISIBLE);
        }
    }

    private int[] getLineData(String string, int limit) {
        String[] splited = string.split(",");
        int[] data = new int[splited.length];
        for (int i = 0; i < splited.length; i++) {
            data[i] = Integer.parseInt(splited[i]);
            if (data[i] > 200 || data[i] < 0) {
                for (int j = 0; j < splited.length; j++) {
                    data[j] = limit;
                }
                return data;
            }
        }
        return data;
    }

    //读取的温度曲线数据放在功率曲线，需要减100
    private String getTemperData(String string) {
        StringBuilder sb1 = new StringBuilder();
        String[] splited = string.split(",");
        int[] data = new int[splited.length];
        for (int i = 0; i < splited.length; i++) {
            data[i] = Integer.parseInt(splited[i]) - 100;
            if (i != 20) {
                sb1 = sb1.append(data[i] + ",");
            } else {
                sb1 = sb1.append(data[i]);
            }
        }
        return sb1.toString();
    }

    private class myChartTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.e(TAG, "actionDown getRate: " + chartComputator.computeRawY((float) 100) + "  " + chartComputator.computeRawY((float) 140));
                    if (rate == 0) {
                        rate = (chartComputator.computeRawY((float) 100) - chartComputator.computeRawY((float) 140)) / (float) 40;
                    }
                    clickX = (int) motionEvent.getX();
                    clickY = (int) motionEvent.getY();
                    selectedValue = selectPoint(currentMainLine.getValues());//选中点
                    Log.e(TAG, "selectPoint onTouch:  X: " + clickX + "  ---  Y: " + clickY + "   selectedValue " + selectedValue);
                    if (selectedValue == null) {
                        selectedDashLine = selectedDashLine(motionEvent, currentDashLine);    //选中虚线
                    }
                    switch (touchInChart) {
                        case TOUCH_FOR_POWER_CHART:
                            singleUnit.setText("W");
                            coupleUnit.setText("W");
                            break;
                        case TOUCH_FOR_JOULE_CHART:
                            singleUnit.setText("J");
                            coupleUnit.setText("J");
                            break;
                        case TOUCH_FOR_TEMPER_CHART:
                            if (temperatureUnit == 0) {
                                singleUnit.setText("℃");
                                coupleUnit.setText("℃");
                            } else if (temperatureUnit == 1) {
                                singleUnit.setText("℉");
                                coupleUnit.setText("℉");
                            }
                            break;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    int moveX = (int) motionEvent.getX();
                    int moveY = (int) motionEvent.getY();
                    int move = moveY - clickY;
                    Log.e(TAG, "onTouch: " + moveX + "  -----  " + moveY);

                    if (selectedDashLine != null) {           //一起移动
                        dataChanged = true;
                        //移动虚线
                        List<PointValue> dashLineValues = selectedDashLine.getValues();
                        for (PointValue value : dashLineValues) {
                            float currentY = value.getY() - move / rate;

                            if (currentY < currentAxisY_Values.get(0).getValue() + 5) {
                                currentY = currentAxisY_Values.get(0).getValue() + 5;
                            } else if (currentY > currentAxisY_Values.get(5).getValue()) {
                                currentY = currentAxisY_Values.get(5).getValue();
                            }
                            value.set(value.getX(), currentY);
                        }
                        //移动功率线所有的点
                        for (PointValue value : currentMainLine.getValues()) {
                            float currentY = value.getY() - move / rate;
                            if (currentY < currentAxisY_Values.get(0).getValue()) {
                                currentY = currentAxisY_Values.get(0).getValue();
                            } else if (currentY > currentAxisY_Values.get(5).getValue()) {
                                currentY = currentAxisY_Values.get(5).getValue();
                            }
                            value.set(value.getX(), currentY);
                        }

                        float translateY = (float) (Math.round(dashLineValues.get(0).getY() * 10)) / 10;
                        dashLayout.setVisibility(View.VISIBLE);
                        switch (temperatureUnit) {
                            case 0:
                                tvDashY.setText(translateY + "");
                                break;
                            case 1:
                                String result = "";
                                if (touchInChart == TOUCH_FOR_TEMPER_CHART) {
                                    result = String.format("%.1f", translateY * 9 / 5 + 32);
                                } else {
                                    result = translateY + "";
                                }

                                tvDashY.setText(result);
                                break;
                        }
                    } else if (selectedValue != null) {   //单点移动
                        dataChanged = true;
                        Log.e(TAG, "onTouch: " + moveX + "  -----  " + moveY);
                        float currentX = selectedValue.getX();
                        float currentY = selectedValue.getY() - move / rate;
                        Log.e(TAG, "onTouch: " + "找到点" + selectedValue.toString());
                        //控制范围
                        if (currentY < currentAxisY_Values.get(0).getValue()) {
                            currentY = currentAxisY_Values.get(0).getValue();
                        } else if (currentY > currentAxisY_Values.get(5).getValue()) {
                            currentY = currentAxisY_Values.get(5).getValue();
                        }
                        //        powerLine.getValues().get((int) (selectedValue.getX() * 2)).set(currentX, currentY);
                        selectedValue.set(currentX, currentY);
                        float translateY = (float) (Math.round(currentY * 10)) / 10;
                        String result_x = String.format("%.1f", currentX);
                        valueX.setText(result_x);
                        switch (temperatureUnit) {
                            case 0:
                                valueY.setText(translateY + "");
                                break;
                            case 1:
                                String result = "";
                                if (touchInChart == TOUCH_FOR_TEMPER_CHART) {
                                    result = String.format("%.1f", translateY * 9 / 5 + 32);
                                } else {
                                    result = translateY + "";
                                }
                                valueY.setText(result);
                                break;
                        }
                        showTemper.setVisibility(View.VISIBLE);
                    }
                    myChart.invalidate();
                    clickY = moveY;
                    break;
                case MotionEvent.ACTION_UP:
                    showTemper.setVisibility(View.GONE);
                    dashLayout.setVisibility(View.GONE);
                    switch (touchInChart) {
                        case TOUCH_FOR_POWER_CHART:
                            powerDashValue = (int) currentDashLine.getValues().get(0).getY();
                            //                            jouleDashValue = (int) currentDashLine.getValues().get(0).getY();
                            break;
                        case TOUCH_FOR_JOULE_CHART:
                            //                            powerDashValue = (int) currentDashLine.getValues().get(0).getY();
                            jouleDashValue = (int) currentDashLine.getValues().get(0).getY();
                            break;
                        case TOUCH_FOR_TEMPER_CHART:
                            temperDashValue = (int) currentDashLine.getValues().get(0).getY();
                            break;
                    }
                    Log.e(TAG, "dashValues: powerDashValue: " + powerDashValue + "  jouleDashValue:  " + jouleDashValue + "  temperDashValue:  " + temperDashValue);
                    selectedDashLine = null;
                    if (selectedValue != null) {
                        //                        Log.e(TAG, "onTouch: 抬起的点" + (int) (selectedValue.getY()));

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
                        int[] arr = new int[51];
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
                        } else if (touchInChart == TOUCH_FOR_JOULE_CHART) {
                            jouleIndex = currentIndex;
                        }
                        setAboveAndNextButtonState(currentMoveLineList, currentIndex);
                        Log.e(TAG, "onTouch: " + currentIndex + "  powerIndex  " + powerIndex);
                        dataChanged = false;
                    }
                    break;
            }
            return false;
        }
    }


    private PointValue selectPoint(List<PointValue> pointValues) {
        int backgroundGridWidth = MyUtils.dip2px(HelloActivity.this, 45);
        int width = backgroundGridWidth / 2;
        Region r = new Region();

        for (PointValue value : pointValues) {
            r.set(clickX - width, clickY - width, clickX + width, clickY + width);
            int pointX = (int) chartComputator.computeRawX(value.getX());
            int pointY = (int) chartComputator.computeRawY(value.getY());
            Log.e(TAG, "selectPoint: x:  " + pointX + "  y: " + pointY);
            if (r.contains(pointX, pointY)) {
                return value;
            }
        }
        return null;
    }

    private Line selectedDashLine(MotionEvent motionEvent, Line dashLine) {
        int backgroundGridWidth = MyUtils.dip2px(HelloActivity.this, 40);
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
                    } else if (touchInChart == TOUCH_FOR_JOULE_CHART) {
                        jouleIndex--;
                        setAboveAndNextButtonState(jouleDataList, jouleIndex);
                        startAnimation(jouleDataList, jouleIndex, dashListInJoule);
                        if (waveIsTrue) {
                            btn_waveBehind.setVisibility(View.GONE);
                            btnWave.setVisibility(View.VISIBLE);
                            jouleDataList.remove(jouleDataList.get(jouleDataList.size() - 1));
                            setAboveAndNextButtonState(jouleDataList, jouleIndex);
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
                    } else if (touchInChart == TOUCH_FOR_JOULE_CHART) {
                        jouleIndex++;
                        setAboveAndNextButtonState(jouleDataList, jouleIndex);
                        startAnimation(jouleDataList, jouleIndex, dashListInJoule);
                    }
                    break;
                // 切换按钮
                case R.id.btn_switch:
                    if (touchInChart == TOUCH_FOR_POWER_CHART) {                               //功率--->温度
                        generateInitialTemperChart();
                    } else if (touchInChart == TOUCH_FOR_TEMPER_CHART && jouleOrPower == 0) {   //温度--->功率
                        tempToPower = true;
                        generateInitialLineData();
                    } else if (touchInChart == TOUCH_FOR_TEMPER_CHART && jouleOrPower == 1) {   //温度--->焦耳
                        tempToJoule = true;
                        generateJouleChart();
                    } else if (touchInChart == TOUCH_FOR_JOULE_CHART) {                     // 焦耳--->温度
                        generateInitialTemperChart();
                    }
                    break;
                case R.id.btn_wave:

                    waveIsTrue = true;
                    btnWave.setVisibility(View.GONE);
                    btn_waveBehind.setVisibility(View.VISIBLE);
                    if (touchInChart == TOUCH_FOR_POWER_CHART) {
                        waveInPowerIsTrue = false;
                        powerIndex++;
                        changeForWave(powerIndex, powerLine, powerDashLine);
                    } else if (touchInChart == TOUCH_FOR_TEMPER_CHART) {
                        waveInTemperIsTrue = false;
                        temperIndex++;
                        changeForWave(temperIndex, temperLine, temperDashLine);
                    } else if (touchInChart == TOUCH_FOR_JOULE_CHART) {
                        waveInJouleIsTrue = false;
                        jouleIndex++;
                        changeForWave(jouleIndex, jouleLine, jouleDashline);
                    }
                    //                    Log.e(TAG, "OnClick: pl: "+powerMoveList.size()+" index : "+powerIndex);
                    break;
                case R.id.btn_waveBehind:

                    btn_waveBehind.setVisibility(View.GONE);
                    btnWave.setVisibility(View.VISIBLE);
                    if (touchInChart == TOUCH_FOR_POWER_CHART) {
                        waveInPowerIsTrue = true;
                        powerIndex--;
                        changeForBehindWave(powerIndex, powerLine);
                    } else if (touchInChart == TOUCH_FOR_TEMPER_CHART) {
                        waveInTemperIsTrue = true;
                        temperIndex--;
                        changeForBehindWave(temperIndex, temperLine);
                    } else if (touchInChart == TOUCH_FOR_JOULE_CHART) {
                        waveInJouleIsTrue = true;
                        jouleIndex--;
                        changeForBehindWave(jouleIndex, jouleLine);
                    }
                    Log.e(TAG, "OnClick: powerMoveList: " + powerMoveList.size() + " index " + powerIndex);
                    break;
                case R.id.btn_save:
                    if (!MyUtils.NoDoubleClickUtils.isDoubleClick()) {
                        saveCurve = true;
                        waitingDialog.show();
                        tempToPower = false;
                        tempToJoule = false;
                        if (touchInChart == TOUCH_FOR_POWER_CHART) {
                            saveStyle = SAVE_STYLE_ONE;
                            //                            Toast.makeText(mBluetoothLeService, "  功率  曲线下保存  ", Toast.LENGTH_SHORT).show();
                            saveCurveData();
                        } else if (touchInChart == TOUCH_FOR_JOULE_CHART) {
                            saveStyle = SAVE_STYLE_TWO;
                            //                            Toast.makeText(mBluetoothLeService, "  焦耳  曲线下保存   ", Toast.LENGTH_SHORT).show();
                            saveCurveData();
                        } else if (touchInChart == TOUCH_FOR_TEMPER_CHART && jouleOrPower == 0) {
                            //                            Toast.makeText(mBluetoothLeService, "  温度-功率  曲线下保存   ", Toast.LENGTH_SHORT).show();
                            saveStyle = SAVE_STYLE_THREE;
                            tempToPower = true;
                            generateInitialLineData();
                            saveCurveData();

                        } else if (touchInChart == TOUCH_FOR_TEMPER_CHART && jouleOrPower == 1) {
                            //                            Toast.makeText(mBluetoothLeService, "  温度-焦耳  曲线下保存   ", Toast.LENGTH_SHORT).show();
                            saveStyle = SAVE_STYLE_FOUR;
                            tempToJoule = true;
                            generateJouleChart();
                            saveCurveData();
                        }
                        break;

                    }
            }
        }

        private void saveCurveData() {
            final int[] arrPower = new int[100];
            final int[] arrTemp = new int[100];
            // X轴
            List<AxisValue> axisValues_x = new ArrayList<AxisValue>();
            for (int i = 0; i < 11; i++) {
                axisValues_x.add(new AxisValue(i).setLabel(axisData[i]));
            }
            Axis axisX = new Axis(axisValues_x);
            axisX.setHasLines(false);
            axisX.setValues(axisValues_x);
            // Y轴
            Axis axisY = new Axis();
            axisY.setHasLines(false);
            axisY.setMaxLabelChars(3);
            List<AxisValue> axisValuesY = new ArrayList<>();
            for (int i = 0; i <= 200; i += 40) {
                axisValuesY.add(new AxisValue(i).setLabel(i + ""));
            }
            axisY.setValues(axisValuesY);
            axisY.setHasLines(false);

            LineChartData lineChartData = myChart.getLineChartData();
            List<Line> lines = lineChartData.getLines();

            //  最上层可移动曲线线(power/joule)
            final Line line1 = lines.get(2);
            line1.setColor(Color.parseColor("#ffffff"));
            line1.setStrokeWidth(1);
            line1.setHasLabelsOnlyForSelected(false);
            line1.setHasPoints(false);
            final int line1Color = line1.getColor();

            //  阴暗曲线 (temper)
            final Line line3 = lines.get(1);
            line3.setStrokeWidth(1);
            line3.setColor(Color.parseColor("#000000"));
            final int line3Color = line3.getColor();
            //  虚线
            final Line line2 = lines.get(0);
            line2.setColor(getResources().getColor(R.color.bezierBack));

            lineChartData.setAxisYLeft(axisY);
            lineChartData.setAxisXBottom(axisX);
            myChart.setLineChartData(lineChartData);
            Log.e("myBitmap", "btn_save: line1color:  " + line1Color + "  line3Color  " + line3Color);
            //                    myChart.setViewportCalculationEnabled(false);
            Viewport v = new Viewport(0, 210, 10, 0);
            myChart.setMaximumViewport(v);
            myChart.setCurrentViewport(v);
            myChart.setZoomType(ZoomType.HORIZONTAL);
            myChart.setZoomEnabled(false);
            myChart.setOnTouchListener(null);
            myChart.setDrawingCacheEnabled(true);
            myChart.buildDrawingCache();
            final Bitmap bmp = myChart.getDrawingCache();
            Log.e("myBitmap", "with:  " + bmp.getWidth() + "  height:  " + bmp.getHeight());
            int bmpWidth = bmp.getWidth();
            final int bmpHeight = bmp.getHeight();
            //                    myChart.invalidate();

            //bitmap已经生成，可以开始计算100个点的坐标，并让视图回归

            Thread powerThred = new Thread() {

                private byte[] powerBytes;
                private byte[] temperBytes;

                @Override
                public void run() {
                    int a = 0;
                    int b = 0;
                    int k = 0;
        //                发送中线
                    if (temperatureUnit == 0 && jouleOrPower == 0) {
                        settingPackage_sendMidlleLineData(userOrder, (byte) 0x02, (byte) 0x01, powerDashValue * 10);
                        try {
                            sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        settingPackage_sendMidlleLineData(userOrder, (byte) 0x04, (byte) 0x00, temperDashValue);
                    } else if (temperatureUnit == 1 && jouleOrPower == 0) {
                        settingPackage_sendMidlleLineData(userOrder, (byte) 0x02, (byte) 0x01, powerDashValue * 10);
                        try {
                            sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        settingPackage_sendMidlleLineData(userOrder, (byte) 0x04, (byte) 0x01, temperDashValue);
                    } else if (temperatureUnit == 0 && jouleOrPower == 1) {
                        settingPackage_sendMidlleLineData(userOrder, (byte) 0x02, (byte) 0x02, powerDashValue * 10);
                        try {
                            sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        settingPackage_sendMidlleLineData(userOrder, (byte) 0x04, (byte) 0x00, temperDashValue);
                    } else if (temperatureUnit == 1 && jouleOrPower == 1) {
                        settingPackage_sendMidlleLineData(userOrder, (byte) 0x02, (byte) 0x02, powerDashValue * 10);
                        try {
                            sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        settingPackage_sendMidlleLineData(userOrder, (byte) 0x04, (byte) 0x01, temperDashValue);
                    }
                    //      计算100个点
                    for (int i = 0; i < 100; i++) {
                        if (i % 2 != 0) {
                            int valueX = (int) chartComputator.computeRawX((float) (i / 10.0));
                            boolean findline1 = true;
                            boolean findline3 = true;
                            for (int j = 0; j < bmpHeight; j++) {
                                //     获取颜色
                                int pixel = bmp.getPixel(valueX, j);
                                if (pixel == line1Color && findline1) {
                                    k++;
                                    findline1 = false;
                                    for (int l = 0; l < 200; l++) {
                                        PointF f = new PointF(valueX, l);
                                        if (chartComputator.rawPixelsToDataPoint(valueX, j, f)) {
                                            DecimalFormat df = new DecimalFormat("#0.0");
                                                          Log.e("bmp", "run:  功率   point   " + f + "   f.x:  " + df.format(f.x) + "   f.y:  " + Math.round(f.y));
                                            arrPower[i] = Math.round(f.y);
                                            break;
                                        }
                                    }
                                } else if (pixel == line3Color && findline3) {
                                    a++;
                                    findline3 = false;
                                    for (int l = 0; l < 200; l++) {
                                        PointF f = new PointF(valueX, l);
                                        if (chartComputator.rawPixelsToDataPoint(valueX, j, f)) {
                                            DecimalFormat df = new DecimalFormat("#0.0");
                                            Log.e("bmp", "run:  温度  point   " + f + "   f.x:  " + df.format(f.x) + "   f.y:  " + Math.round(f.y) + "    数量:  " + a);
                                            arrTemp[i] = Math.round(f.y);
                                            break;
                                        }
                                    }
                                }

                                if (!findline1 && !findline3) {
                                    break;
                                }
                            }
                            //        当后面曲线的点被覆盖
                            if (findline1 != findline3) {
                                a++;
                                arrTemp[i] = arrPower[i];
                            }
                        } else if (i % 2 == 0) {
                            Log.e("bmp", "run: " + "     原始数据：      "+(int) line1.getValues().get(b).getY());
                            arrPower[i] = (int) line1.getValues().get(b).getY();
                            arrTemp[i] = (int) line3.getValues().get(b).getY();
                            b++;
                        }
                    }
                    arrPower[99] = (int) line1.getValues().get(50).getY();
                    arrTemp[99] = (int) line3.getValues().get(50).getY();

                    Log.e(TAG, "runInThread: k:  " + k + "   a:  " + a + "   b:  " + b + "   功率数据：  " + arrPower.length + "   温度数据：  " + arrTemp.length + "   powerDashValue  " + powerDashValue);
                    b = 0;

                    for (int i = 0; i < arrPower.length; i++) {
                        byte[] getBytes = intToBytes(arrPower[i] * 10);
                        if (i == 0) {
                            powerBytes = getBytes;
                        } else {
                            powerBytes = byteMerger(powerBytes, getBytes);
                        }
                    }
                    //把Temper曲线的坐标值转换成  byte[]
                    for (int i = 0; i < arrTemp.length; i++) {
                        byte[] getBytes = intToBytes(arrTemp[i] + 100);
                        if (i == 0) {
                            temperBytes = getBytes;
                        } else {
                            temperBytes = byteMerger(temperBytes, getBytes);
                        }
                    }

                    sb1.setLength(0);
                    //      发送数据-----
                    if (g_Character_TX != null) {
                        sendCurveDataToDevice((byte) 0x4E, powerBytes);
                        sendCurveDataToDevice((byte) 0x6A, temperBytes);
                        Log.e(TAG, "powerDashValue: ----------------------------- ");
                        //                        sendMiddleLineDataToDevice();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            switch (saveStyle) {
                                case SAVE_STYLE_ONE:

                                    setLineStyle(line1, POWER_HIGHLIGHT_CURVE);
                                    setLineStyle(line3, DARK_CURVE);
                                    setLineStyle(line2, DASH);
                                    generateInitialLineData();
                                    break;
                                case SAVE_STYLE_TWO:

                                    setLineStyle(line1, JOULE_HIGHLIGHT_CURVE);
                                    setLineStyle(line3, DARK_CURVE);
                                    setLineStyle(line2, DASH);
                                    generateJouleChart();
                                    break;
                                case SAVE_STYLE_THREE:

                                    setLineStyle(line1, POWER_HIGHLIGHT_CURVE);
                                    setLineStyle(line3, DARK_CURVE);
                                    setLineStyle(line2, DASH);
                                    generateInitialTemperChart();
                                    break;
                                case SAVE_STYLE_FOUR:

                                    setLineStyle(line1, JOULE_HIGHLIGHT_CURVE);
                                    setLineStyle(line3, DARK_CURVE);
                                    setLineStyle(line2, DASH);
                                    generateInitialTemperChart();
                                    break;
                            }
                            waitingDialog.dismiss();
                        }
                    });
                    super.run();

                }


            };
            pool.execute(powerThred);
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
        int[] arr = new int[51];
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
                line.setStrokeWidth(3);
                line.setHasLabelsOnlyForSelected(true);
                line.setColor(getResources().getColor(R.color.colorWhite));
                break;
            case JOULE_HIGHLIGHT_CURVE:
                line.setHasPoints(true);
                line.setCubic(true);
                line.setStrokeWidth(3);
                line.setHasLabelsOnlyForSelected(true);
                line.setPointColor(getResources().getColor(R.color.colorWhite));
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
    //        Log.e(TAG, "OnClick: " + "size:  " + powerValues.size() + "dash  " + dashValues.size());
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(setHelloActivityReceiver);
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private boolean startReadPowerCurveData = false;

    //读取曲线数据                         Setting 包的序号     曲线      序号    数量     编号
    public void settingPackage_PowerCurve_ReadData(byte pp, byte tt, byte mm, byte rr, byte ll, int length) {
        Log.e(TAG, "口感选择发出数据: ");
        byte[] m_Data_DeviceSetting = new byte[32];
        int m_Length = 0;
        m_Data_DeviceSetting[0] = 0x55;
        m_Data_DeviceSetting[1] = (byte) 0xFF;
        m_Data_DeviceSetting[3] = 0x01; //Device ID
        m_Data_DeviceSetting[2] = 0x09;
        m_Data_DeviceSetting[4] = 0x66;
        m_Data_DeviceSetting[5] = pp;           //C1 ~ C5
        m_Data_DeviceSetting[6] = tt;           //1-W 点,2-W 线，3-temp 点，4-temp线
        m_Data_DeviceSetting[7] = mm;           //      序号
        m_Data_DeviceSetting[8] = rr;           //一条曲线分为4个数据包,虚线1个数据包
        m_Data_DeviceSetting[9] = ll;           //用户编号 S1 ~ S5
        m_Data_DeviceSetting[10] = (byte) ((byte) (length >> 8) & 0xff);
        m_Data_DeviceSetting[11] = (byte) ((byte) length & 0xff);

        m_Length = 12;
        Sys_Proc_Charactor_TX_Send(m_Data_DeviceSetting, m_Length);
    }

    private void Sys_Proc_Charactor_TX_Send(byte[] m_Data, int m_Length) {

        byte[] m_MyData = new byte[m_Length];
        for (int i = 0; i < m_Length; i++) {
            m_MyData[i] = m_Data[i];
        }

        if (g_Character_TX == null) {
            Log.e("SetDetailsActivity", "character TX is null");
            return;
        }

        if (m_Length <= 0) {
            return;
        }
        String s = BinaryToHexString(m_MyData);
        Log.e(TAG, "sendData:   write into:    " + s + "    length:   " + m_Length);
        g_Character_TX.setValue(m_MyData);
        mBluetoothLeService.writeCharacteristic(g_Character_TX);
    }

    private void Sys_YiHi_Protocol_RX_Porc(byte[] m_Data) {
        int m_Length = 0;
        int i;
        int m_Index = 0xfe;
        byte m_ValidData_Length = 0;
        byte m_Command = 0;
        byte m_SecondCommand = 0;
        String s = null;
        int m_iTemp_x10, m_iTemp_x1;
        m_Length = m_Data.length;
        if (m_Length < 5) {
            return;
        }
        //Get sync code.
        for (i = 0; i < m_Length; i++) {
            //if (i<16)
            //{
            //	if (g_b_Use_DEBUG) Log.i(LJB_TAG,"RX proc---Data["+i+"]="+m_Data[i]);
            //}
            //if ((m_Data[i]==0x55)&&(m_Data[(i+1)]==0xFF))
            if (((m_Data[i] == 85) || (m_Data[i] == 0x55))
                    && ((m_Data[(i + 1)] == -1) || (m_Data[(i + 1)] == 0xFF)
                    || (m_Data[(i + 1)] == -3) || (m_Data[i + 1] == 0xFD))) {
                //if (g_b_Use_DEBUG) Log.i(LJB_TAG,"RX proc---i="+i);
                m_Index = i;
                //i=m_Length;
                break;
            }

        }
        if (m_Index == 0xfe) {
            return;
        }
        if (m_Index > (m_Length - 2)) {
            return;
        }
        //Get valid data length.
        m_ValidData_Length = m_Data[(m_Index + 2)];
        if ((m_Index + m_ValidData_Length) > m_Length) {
            return;
        }
        //Get command code.
        m_Command = m_Data[(m_Index + 4)];
        switch (m_Command) {
            case 0x67:
                Log.e(TAG, "powerCurveReadData 口感选择准备处理数据: " + BinaryToHexString(m_Data));
                int height = (m_Data[6] & 0xf0) >> 4;
                int low = m_Data[6] & 0x0f;
                if (height == 2 && low == 2) {      //      功率曲线的虚线
                    int i1 = ((m_Data[11] & 0xff) << 8) | (m_Data[12] & 0xff);
                    Log.e(TAG, " powerDashValue  功率曲线的中线的数据****  :  结果： " + i1);
                    //                    readTexture.dash = i1 / 10;
                    powerDashValue = i1/10;
                    jouleDashValue = powerDashValue;
                    //读取温度中线
                    settingPackage_PowerCurve_ReadData(settingPackageOrder, (byte) 0x04, (byte) 0x00, (byte) 0x01, userOrder, 2);

                } else if (height == 2 && low == 4) {       //      温度曲线的虚线
                    int i1 = ((m_Data[11] & 0xff) << 8) | (m_Data[12] & 0xff);
                    //                    readTexture.dashValueInTemper = i1;
                    Log.e(TAG, " powerDashValue  温度曲线的中线的数据----   "  + "   结果：  " + i1);
                    startReadMiddleLine = false;
                    //                    readTexture.save();
                    temperDashValue = i1;

                    init();
                    initDialog.dismiss();
                }

                if (height == 1) {
                    final int waitTime = ((m_Data[9] & 0xff) << 8) | (m_Data[10] & 0xff);
                    Log.e(TAG, "等待时间: " + waitTime);

                    waitToGetData(waitTime);
                }
                break;
            case 0x4F:
                Log.e(TAG, "sendDataBack:   " + " " + BinaryToHexString(m_Data));
                break;
            case 0x6B:
                Log.e(TAG, "powerDashValue: 发送温度曲线数据后返回的数据：" + BinaryToHexString(m_Data));
                break;
            case 0x58:
                Log.e(TAG, "powerDashValue:   收到要处理的虚线数据   " + BinaryToHexString(m_Data));
                break;
        }
    }

    private void waitToGetData(final int waitTime) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(waitTime);
                    getSettingPackage_ReadData_GetResult();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private int bigOrder = 0;

    private void getSettingPackage_ReadData_GetResult() {
        bigOrder++;
        begin = true;
        littleOrder = 0;
        byte[] m_Data = new byte[32];
        int m_length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[3] = 0x01; //Device ID
        m_Data[2] = 0x03;
        m_Data[4] = 0x69;
        m_Data[5] = 0x02;
        m_length = 6;
        Sys_Proc_Charactor_TX_Send(m_Data, m_length);
    }

    //发送曲线数据      手机  ---->  设备
    private void curve_SendData(byte function, byte mm, byte rr, byte ll, byte[] qq) {

        byte[] m_Data_DeviceSetting = new byte[32];
        int m_Length = 0;
        m_Data_DeviceSetting[0] = 0x55;
        m_Data_DeviceSetting[1] = (byte) 0xFF;
        m_Data_DeviceSetting[3] = 0x01;
        m_Data_DeviceSetting[2] = 0x37;           //从3开始的个数   55
        m_Data_DeviceSetting[4] = function;
        m_Data_DeviceSetting[5] = mm;           //  本次发送的数据块在整个功率曲线数据包中的序号
        m_Data_DeviceSetting[6] = rr;           //  本次要发送的整个功率曲线数据包含的数据块数量
        m_Data_DeviceSetting[7] = ll;           //  功率曲线的用户编号.(设备可能存储多条用户曲线.)  S1 ~ S5
        for (int i = 0; i < 12; i++) {
            m_Data_DeviceSetting[8 + i] = qq[i];
        }
        m_Length = 20;
        Sys_Proc_Charactor_TX_Send(m_Data_DeviceSetting, m_Length);
    }

    //曲线数据包2
    private void sendQ2(byte[] qq) {
        int m_Length = 20;
        byte[] m_Data_DeviceSetting = new byte[32];
        for (int i = 0; i < 20; i++) {
            m_Data_DeviceSetting[i] = qq[i + 12];
        }
        Sys_Proc_Charactor_TX_Send(m_Data_DeviceSetting, m_Length);
    }

    //曲线数据包3
    private void sendQ3(byte[] qq) {
        int m_Length = 18;
        byte[] m_Data_DeviceSetting = new byte[32];
        for (int i = 0; i < 18; i++) {
            m_Data_DeviceSetting[i] = qq[i + 32];
        }
        Sys_Proc_Charactor_TX_Send(m_Data_DeviceSetting, m_Length);
    }

    private void sendCurveDataToDevice(byte function, byte[] data) {
        for (int i = 0; i < 4; i++) {
            byte[] bt = subBytes(data, i * 50, 50);
            Log.e(TAG, "run:send50Data  Prepare    i:    " + i + "     " + BinaryToHexString(bt) + "       " + bt.length);
            //  开始发送
            curve_SendData(function, (byte) i, (byte) 0x04, userOrder, bt);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //  第二个数据包
            sendQ2(bt);

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //  第三个数据包
            sendQ3(bt);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    //发送中线数值    手机  ---->  设备
    public void settingPackage_sendMidlleLineData(byte p1, byte p2, byte p3, int value) {
        byte[] m_Data_DeviceSetting = new byte[32];
        int m_Length = 0;
        m_Data_DeviceSetting[0] = 0x55;
        m_Data_DeviceSetting[1] = (byte) 0xFF;
        m_Data_DeviceSetting[3] = 0x01;
        m_Data_DeviceSetting[2] = 0x08;
        m_Data_DeviceSetting[4] = 0x59;
        m_Data_DeviceSetting[5] = 0x16;
        m_Data_DeviceSetting[6] = p1;            //曲线序号   S1 ~ S5
        m_Data_DeviceSetting[7] = p2;
        m_Data_DeviceSetting[8] = p3;
        m_Data_DeviceSetting[9] = (byte) ((byte) (value >> 8) & 0xff);
        m_Data_DeviceSetting[10] = (byte) ((byte) value & 0xff);
        m_Length = 11;
        Sys_Proc_Charactor_TX_Send(m_Data_DeviceSetting, m_Length);
    }

    private void sendMiddleLineDataToDevice() {

        Log.e(TAG, "sendMiddleLineDataToDevice: " + temperatureUnit + "   " + jouleOrPower + "  userOrder  " + userOrder + "   powerDashValue  " + powerDashValue);
        if (temperatureUnit == 0 && jouleOrPower == 0) {
            settingPackage_sendMidlleLineData(userOrder, (byte) 0x02, (byte) 0x01, powerDashValue * 10);
            settingPackage_sendMidlleLineData(userOrder, (byte) 0x04, (byte) 0x00, temperDashValue);
        } else if (temperatureUnit == 1 && jouleOrPower == 0) {
            settingPackage_sendMidlleLineData(userOrder, (byte) 0x02, (byte) 0x01, powerDashValue * 10);
            settingPackage_sendMidlleLineData(userOrder, (byte) 0x04, (byte) 0x01, temperDashValue);
        } else if (temperatureUnit == 0 && jouleOrPower == 1) {
            settingPackage_sendMidlleLineData(userOrder, (byte) 0x02, (byte) 0x02, powerDashValue * 10);
            settingPackage_sendMidlleLineData(userOrder, (byte) 0x04, (byte) 0x00, temperDashValue);
        } else if (temperatureUnit == 1 && jouleOrPower == 1) {
            settingPackage_sendMidlleLineData(userOrder, (byte) 0x02, (byte) 0x02, powerDashValue * 10);
            settingPackage_sendMidlleLineData(userOrder, (byte) 0x04, (byte) 0x01, temperDashValue);
        }
    }
}
