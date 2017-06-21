package com.yihai.wu.sxi;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;

import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoActivity;
import com.jph.takephoto.model.CropOptions;
import com.jph.takephoto.model.TResult;
import com.yihai.wu.util.DarkImageButton;
import com.yihai.wu.util.WallpaperDialogView;

import static com.yihai.wu.util.MyUtils.BinaryToHexString;
import static com.yihai.wu.util.MyUtils.byteMerger;
import static com.yihai.wu.util.MyUtils.bytesToInt;
import static com.yihai.wu.util.MyUtils.intToBytes;
import static com.yihai.wu.util.MyUtils.intToBytes2;
import static com.yihai.wu.util.MyUtils.zoomImg;

/**
 * Created by ${Wu} on 2017/5/5.
 */

public class SetWallpaperActivity extends TakePhotoActivity implements View.OnClickListener, NumberPicker.OnValueChangeListener, NumberPicker.OnScrollListener {
    private ImageView show_img;
    private TakePhoto takePhoto;
    private Button selectPicture;
    private DarkImageButton btn_back;
    private int m_dataLength = 115200;
    private byte[] pixel_data;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic g_Character_TX;
    private int count = 0;
    private boolean needMerge = false;
    private boolean sendPicture = false;
    private boolean mergeOnce = false;
    private NumberPicker numberPicker;
    private byte[] merger_datas;
    private int picture_address;
    private Uri imageUri;
    private int wallpaper_order_number;
   private WallpaperDialogView myDialog;
    private boolean supportPreview = false;
    //   sharedPreference
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor spEditor;

    //调用系统相册-选择图片
    private static final int IMAGE = 1;
    private static final int CROP_IMAGE = 2;
    public static final String TMP_PATH = "clip_temp.jpg";

    PowerManager powerManager = null;
    PowerManager.WakeLock wakeLock = null;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setwallpaper);
        Intent startIntent = getIntent();
        supportPreview = startIntent.getBooleanExtra("support",false);
//        takePhoto = getTakePhoto();

        selectPicture = (Button) findViewById(R.id.btn_selectPicture);
        selectPicture.setOnClickListener(this);
        show_img = (ImageView) findViewById(R.id.show_img);
        btn_back = (DarkImageButton) findViewById(R.id.btn_back);
        btn_back.setOnClickListener(this);
        pixel_data = new byte[115200];

        //dialog
        myDialog = new WallpaperDialogView(this);
        myDialog.setCanceledOnTouchOutside(false);
        myDialog.setCancelable(false);


        //NumberPicker
        numberPicker = (NumberPicker) findViewById(R.id.numpicker);

        //        numberPicker.setOnValueChangedListener(this);
        //        numberPicker.setOnScrollListener(this);
        numberPicker.setMaxValue(5);
        numberPicker.setMinValue(1);
        numberPicker.setValue(1);
        //numberPicker  取消编辑模式
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        numberPicker.setOnValueChangedListener(this);
        numberPicker.setOnScrollListener(this);


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        registerReceiver(myReceiver, makeBroadcastFilter());

        sharedPreferences = getSharedPreferences("pixelData",Context.MODE_PRIVATE);
        spEditor = sharedPreferences.edit();

        powerManager = (PowerManager)this.getSystemService(this.POWER_SERVICE);
        wakeLock = this.powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");
    }

    private IntentFilter makeBroadcastFilter() {

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_RX);
        intentFilter.addAction(BluetoothLeService.ACTION_LAND_SUCCESS);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_SEND_PROGRESS);
        return intentFilter;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_selectPicture:
                int value = numberPicker.getValue();
                wallpaper_order_number = value - 1;
//                String pictureName = "picture_" + value;
//                File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + pictureName + ".jpg");
//                if (!file.getParentFile().exists())
//                    file.getParentFile().mkdirs();
//                imageUri = Uri.fromFile(file);
//

                getOneWallPaperInfo(wallpaper_order_number);
//                 takePhoto.onPickFromGalleryWithCrop(imageUri, getCropOptions());

                break;
            case R.id.btn_back:
                this.finish();
                break;

        }
    }

    private CropOptions getCropOptions() {
        int height = 240;
        int width = 240;
        CropOptions.Builder builder = new CropOptions.Builder();
        builder.setOutputX(width).setOutputY(height);
        builder.setWithOwnCrop(false);
        return builder.create();
    }

    @Override
    public void takeCancel() {
        super.takeCancel();
    }

    @Override
    public void takeFail(TResult result, String msg) {
        super.takeFail(result, msg);
    }

    @Override
    public void takeSuccess(TResult result) {
        super.takeSuccess(result);
//        showImg(result.getImages());
    }


    private void sendImg(Bitmap bm) {

//        final Bitmap bm = BitmapFactory.decodeFile(images.get(images.size() - 1).getOriginalPath());

//        Log.d(TAG, "bmSize: bm  width：" + bm.getWidth() + "   " + bm.getHeight());
        Bitmap bitmap = zoomImg(bm, 240, 240);

        show_img.setImageBitmap(bitmap);
        myDialog.show();
        wakeLock.acquire();
        //        show_tv



      /*  Log.d(TAG, "bmSize: width:  " + width + " height: " + height);
        //单色验证
        int point_pixel = bitmap.getPixel(100, 100);
        int alpha = Color.alpha(point_pixel);

        int point_red = Color.red(point_pixel);
        int point_green = Color.green(point_pixel);
        int point_blue = Color.blue(point_pixel);
        //        point_red=255;
        //        point_green = 0;
        //        point_blue = 255;
        //        short all = RGB8_to_RGB565(pixel);

        Log.d(TAG, "one_point_showImg: " + " alpha  " + alpha + "  red:  " + point_red + "  green:  " + point_green + "  blue:  " + point_blue + "     " + "  " + "  pixel:  " + point_pixel + "  ");
        Log.d(TAG, "one_point_showImg: " + toFullBinaryString(point_red) + "   " + toFullBinaryString(point_green) + "   " + toFullBinaryString(point_blue));

        byte[] bytes = intToBytes2(point_red);

        //该方法验证正确  (  RGB565   )
        int point_r1 = (point_red << 8) & 0xF800;
        int point_g1 = (point_green << 3) & 0x7E0;
        int point_b1 = point_blue >> 3;
        short test1 = (short) (point_r1 | point_g1 | point_b1);
        int all_int = point_r1 | point_g1 | point_b1;
        int qz = test1;
        Log.d(TAG, "one_point_showImg: short " + test1 + "   int: " + all_int + "   test1:  " + qz);
        byte[] bytes1 = intToBytes2(all_int);
        //        byte b2 = bytes1[2];
        //        byte b3 = bytes1[3];

        byte b_high = (byte) (all_int >> 8);
        byte b_low = (byte) all_int;
        int merge = bytes2Int(b_high, b_low);

        Log.d(TAG, "one_point_showImg: 两个byte合并:" + merge + "    " + toFullBinaryString(all_int) + "    short:  " + test1 + "     " + "   " + toFullBinaryString(test1));

        //  rgb565转回rgb888
        byte[] rgb24 = new byte[3];
        short RGB565_MASK_RED = (short) 0xF800;
        short RGB565_MASK_GREEN = 0x07E0;
        short RGB565_MASK_BLUE = 0x001F;

        rgb24[2] = (byte) ((test1 & RGB565_MASK_RED) >> 11);
        rgb24[1] = (byte) ((test1 & RGB565_MASK_GREEN) >> 5);
        rgb24[0] = (byte) (test1 & RGB565_MASK_BLUE);

        rgb24[2] <<= 3;
        rgb24[1] <<= 2;
        rgb24[0] <<= 3;
        int rrr = rgb24[2] & 0xff;
        int ggg = rgb24[1] & 0xff;
        int bbb = rgb24[0] & 0xff;
        int back_RGB24 = byte_3_ToInt(rgb24[2], rgb24[1], rgb24[0]);

        Log.d(TAG, "one_point_showImg:  r:" + rrr + " g: " + ggg + " b: " + bbb + "  " + back_RGB24);
        Log.d(TAG, "one_point_showImg: " + "    " + toFullBinaryString(rrr) + "    ggg:  " + toFullBinaryString(ggg) + "     " + " bbb  " + toFullBinaryString(bbb));
        Log.d(TAG, "one_point_showImg: " + " RGB888:  " + toFullBinaryString(back_RGB24));
        //单色验证完成
        */
        //        BitmapFactory.Options options =new BitmapFactory.Options();
        //        options.inPreferredConfig = Bitmap.Config.RGB_565;   1010001110101000
        //        Bitmap bm2 = BitmapFactory.decodeFile(images.get(images.size() - 1).getOriginalPath(),options);

        //                int p2 = bm2.getPixel(20,50);

        //        int alpha2 = Color.alpha(p2);
        //        byte red2 = (byte) Color.red(p2);
        //        byte green2 = (byte) Color.green(p2);
        //        byte blue2 = (byte) Color.blue(p2);

        //

        //        int alpha = Color.alpha(pixel);
        //        int red = Color.red(pixel);
        //        int green = Color.green(pixel);
        //        int blue = Color.blue(pixel);


        //        rgb[0] = (pixel & 0xff0000) >> 16;
        //        rgb[1] = (pixel & 0xff00) >> 8;
        //        rgb[2] = (pixel & 0xff);
        //
        //        Log.d("showImg", "ARGB:    a:"  + "  r: " + red + " g: " + green + " b: " + blue + "   pixel: " + pixel+" rgb:  "+rgb[0]+" , "+ rgb[1]+" , "+rgb[2]);
        //        Log.d("showImg", "showImg: "+r+"   "+g+"   "+b+"   all: "+all);

        //        int count =0;
        //循环取色
        //        int a=0,b=0,c=0;
        StringBuilder pixelData = new StringBuilder();
        for (int h = 0; h < bitmap.getHeight(); h++) {

            for (int w = 0; w < bitmap.getWidth(); w++) {
                int pixel = bitmap.getPixel(w, h);


                //                int red = Color.red(pixel);
                //                int green = Color.green(pixel);
                //                int blue = Color.blue(pixel);

                //                //      方案一：
                //                                int r1 = (red << 8) & 0xF800;
                //                                int g1 = (green << 3) & 0x7E0;
                //                                int b1 = blue >> 3;
                //-----------------
                //                short red = (short) Color.red(pixel);
                //                short green = (short) Color.green(pixel);
                //                short blue = (short) Color.blue(pixel);
                //                Log.d("showImg", "ARGB:    a:"  + "  r: " + red + " g: " + green + " b: " + blue + "   pixel: " + pixel+" rgb:  ");
                //      方案一：
                //                int r1 = (red << 8) & 0xF800;
                //                int g1 = (green << 3) & 0x7E0;
                //                int b1 = blue >> 3;
                //                 short b1 = (short) ((blue >> 3) & 0x001F);
                //                 short g1 = (short) (((green >> 2) << 5) & 0x07E0);
                //                 short r1 = (short) (((red >> 3) << 11) & 0xF800);
                //
                //                short all = (short) (r1 | g1 | b1);
                //
                //                short all = RGB8_to_RGB565(pixel);
                //                int p2 = RGB565_to_RGB888(all);
                //                Log.d(TAG, "showImg: p1:  " + pixel + "     " + p2);
                //--------------------
                //      方案二：
                int r1 = (pixel & 0x00F80000) >> 8;
                int g1 = (pixel & 0x0000FC00) >> 5;
                int b1 = (pixel & 0x000000F8) >> 3;
                int all = r1 | g1 | b1;
                byte one = (byte) (all >> 8);
                byte two = (byte) all;

               /* //      方案三：彬哥》》》
                int r3 = red & 0xF8;
                r3 <<= 8;
                int all3 = r3 & 0xF800;
                int g3 = green & 0xFC;
                g3 <<= 3;
                g3 &= 0x07E0;
                all3 |= g3;

                int b3 = blue & 0xF8;
                b3 >>= 3;
                b3 &= 0x001F;
                all3 |= b3;
                //                int all3 = r3 | g3 | b3;


                byte one3 = (byte) (all3 >> 8);
                byte two3 = (byte) all3;
                //网络方法   （实质上和彬哥的一样）
                int r2 = (red << 8) & 0xF800;
                int g2 = (green << 3) & 0x7E0;
                int b2 = blue >> 3;

                int all2 = r2 | g2 | b2;
                byte m8_high = (byte) (all2 >> 8);
                byte m8_low = (byte) all2;
*/

                //                Log.d(TAG, "showImg: " + "   --------------------  " + all2);
                //                int all = RGB888toRGB565(red, green, blue);
                //                int all = colorValue_RGB888_To_RGB565(red, green, blue);

                //                byte[] bytes = intToBytes2(all2);
                //
                //                byte one = bytes[2];
                //                byte two = bytes[3];


                //
                //                                byte one3 = (byte) ((all & 0xFF00) >> 8);
                //                                byte two3 = (byte) (all & 0x00FF);
                //                boolean bool_one = one==one3;
                //                boolean bool_two =  two==two3;
                //                Log.d(TAG, "showImg: "+bool_one+"   "+bool_two);
                //
                //                int it = bytes2Int(one, two);
                //                Log.d("showImg", "showImg:     "  +it);
                //                char c_height = (char) ((all & 0xFF00) >> 8);
                //                char c_low = (char) (all & 0x00FF);
                //                int hi = (all & 0xFF00) >> 8;
                //                int lo = all & 0x00FF;
                //                Log.d("showImg", "  "+c_height+"  ***  "+c_low+"   ----  "+hi+" -- "+lo);
                //-----------------------------


/*
                byte[] rgb24 = new byte[3];
                short RGB565_MASK_RED = (short) 0xF800;
                short RGB565_MASK_GREEN = 0x07E0;
                short RGB565_MASK_BLUE = 0x001F;

                rgb24[2] = (byte) ((all & RGB565_MASK_RED) >> 11);
                rgb24[1] = (byte) ((all & RGB565_MASK_GREEN) >> 5);
                rgb24[0] = (byte) (all & RGB565_MASK_BLUE);

                rgb24[2] <<= 3;
                rgb24[1] <<= 2;
                rgb24[0] <<= 3;
                int rrr = rgb24[2] & 0xff;
                int ggg = rgb24[1] & 0xff;
                int bbb = rgb24[0] & 0xff;

                int [] back_RGB = new int[3];
                back_RGB [0] = rrr;
                back_RGB [1] = ggg;
                back_RGB [2] = bbb;*/
                //                rgb_list.add(back_RGB);
                //                int back_RGB24 = byte_3_ToInt(rgb24[2], rgb24[1], rgb24[0]);
                //
                //                color_array[count] = back_RGB24;
                //                count++;
                //-----------------------------

                int m_dataIndex = (h * bitmap.getWidth() * 2) + (w * 2);
                if (m_dataIndex < m_dataLength) {
                    pixel_data[m_dataIndex + 1] = one;
                    pixel_data[m_dataIndex] = two;
                }

                //                pixel_data2[m_dataIndex] = m8_low;
                //                pixel_data2[m_dataIndex + 1] = m8_high;
                //
                //                pixel_data3[m_dataIndex+1] = (byte) (back_RGB24>>8);
                //                pixel_data3[m_dataIndex ] = (byte) back_RGB24;

                if (h == bitmap.getHeight() - 1 && w == bitmap.getWidth() - 1) {



                    //                    Bitmap result = Bitmap.createBitmap(240, 240, Bitmap.Config.RGB_565);
                    //                    result.setPixels(color_array, 0, 240, 0, 0, 240, 240);
                    //
                    //                    color_2.setImageBitmap(result);
                    pixelData.append(pixel);
                    Log.d("showImg", "showImg: over  over  over  over  over over   "+pixelData);
                    if (mBluetoothLeService.getTheConnectedState() == 2) {
                        sendPicture = true;
                        mBluetoothLeService.setPixel_data(pixel_data);

                        spEditor.putString("pixels", String.valueOf(pixelData));
                        Log.d("showImg", "showImg:    "+String.valueOf(pixelData));
                        spEditor.commit();

                        //进入蓝屏模式
                        getWallPaperRequest();
                        //                        sendDataStart();

                    }
                }else {
                    pixelData.append(pixel+",");
                }
            }
        }
        //        show_img.setImageBitmap(bitmap);

    }

    //进入更换壁纸的功能模式（蓝屏）
    private void getWallPaperRequest() {

        byte[] m_Data = new byte[32];
        int m_Length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[2] = 0x04;
        m_Data[3] = 0x01;
        m_Data[4] = 0x6C;
        m_Data[5] = 0x15;
        m_Data[6] = 0x01;

        m_Length = 7;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("service", "Unable to initialize Bluetooth");
                finish();
            }
            if (mBluetoothLeService.getTheConnectedState() == 0) {

            } else if (mBluetoothLeService.getTheConnectedState() == 2) {
                g_Character_TX = mBluetoothLeService.getG_Character_TX();
            }
            Log.d("setActivityInService", "onServiceConnected: " + mBluetoothLeService + "  character_TX:  " + g_Character_TX + "    ");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("service", "onServiceDisconnected: " + "---------服务断开-------------");
            mBluetoothLeService = null;
        }
    };

    private void Sys_Proc_Charactor_TX_Send(byte[] m_Data, int m_Length) {
        byte[] m_MyData = new byte[m_Length];
        for (int i = 0; i < m_Length; i++) {
            m_MyData[i] = m_Data[i];
        }

        if (g_Character_TX == null) {
            Log.e("set", "character TX is null");
            return;
        }

        if (m_Length <= 0) {
            return;
        }
        g_Character_TX.setValue(m_MyData);
        mBluetoothLeService.writeCharacteristic(g_Character_TX);
    }

    private static final String TAG = "SetWallpaperActivity";
    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothLeService.ACTION_DATA_RX:
                    Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
                    byte[] data = bundle.getByteArray("byteValues");
                    String s = BinaryToHexString(data);
                    Log.d(TAG, "myReceive: >>>>>>  " + s);
                    if (needMerge) {
                        needMerge = false;
                        merger_datas = data;
                        mergeOnce = true;
                        break;
                    } else if (mergeOnce) {
                        mergeOnce = false;
                        merger_datas = byteMerger(merger_datas, data);
                        Sys_YiHi_Protocol_RX_Porc(merger_datas);
                        merger_datas = null;
                    } else if (sendPicture) {
                        Sys_YiHi_Protocol_RX_Porc(data);
                    }

                    //                    Sys_YiHi_Protocol_RX_Porc(data);
                    break;
                case BluetoothLeService.ACTION_LAND_SUCCESS:
                    Log.e("log", "onReceive: " + "GATT连接成功*************");
                    startActivity(new Intent(SetWallpaperActivity.this, MainActivity.class));
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    Log.e("log", "onReceive: " + "GATT未连接********");
                    startActivity(new Intent(SetWallpaperActivity.this, MainActivity.class));
                    break;
                case BluetoothLeService.ACTION_SEND_PROGRESS:
                    int sendProgress = intent.getIntExtra("sendProgress", 0);

                        myDialog.setProgress((double) sendProgress * 100 / 2304);
                    if(sendProgress==2304){
                       
                        myDialog.dismiss();
                        mBluetoothLeService.setCanSendPicture(false);
                    }
                    break;

            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
        unbindService(mServiceConnection);
//        if(dialogView.isShowing()){
//            dialogView.dismiss();
//        }
        mBluetoothLeService = null;
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
        m_Command = m_Data[(m_Index + 5)];
        switch (m_Command) {
            //     getOneWallpaper返回的信息：
            case 0x04:

                Log.d(TAG, "getOneWallpaper:  " + BinaryToHexString(m_Data) + "   地址： " + bytesToInt(m_Data, 12));
                picture_address = bytesToInt(m_Data, 12);
//                takePhoto.onPickFromGalleryWithCrop(imageUri, getCropOptions());
//                Intent intent = new Intent(Intent.ACTION_PICK,
//                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, IMAGE);

                break;
            case 0x16:
                final int waitTime = ((m_Data[7] & 0xff) << 8) | (m_Data[8] & 0xff);
                Log.d(TAG, "Sys_YiHi_Protocol_RX_Porc: " + BinaryToHexString(m_Data) + "   ***   " + waitTime);
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            Thread.sleep(waitTime);
                            mBluetoothLeService.setCanSendPicture(true);
                            sendDataStart();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }.start();
                break;
            case 0x0E:
                //     sendDataStart();
                Log.d(TAG, "Sys_YiHi_Protocol_RX_Porc: llllllllllllllllllllllllllllllllllllll");
//                sendData(count);
//                count++;
                //--------------------------


                break;
            case 0x10:
//                Log.d(TAG, "sendData:   back " + BinaryToHexString(m_Data) + "     count " + count);
//                dialogView.setProgress((double) count * 100 / 2304);
//
//                if (count >= 2304) {
//                    Log.d(TAG, "sendData: " + "   over     over   over   over   over   over   over   over   over");
//                    dialogView.dismiss();
//                    count = 0;
//                    sendPicture = false;
//                    return;
//                }
//                sendData(count);
//                count++;
                break;

        }
    }

    public void sendDataStart() {
        byte[] m_Data = new byte[32];
        int m_Length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[2] = 0x0E;
        m_Data[3] = 0x01;
        m_Data[4] = 0x6C;
        m_Data[5] = 0x0D;
        // 起始地址  （已获得）   测试数据：  33030144
        byte[] byte_address = intToBytes2(picture_address);

        m_Data[6] = byte_address[0];
        m_Data[7] = byte_address[1];
        m_Data[8] = byte_address[2];
        m_Data[9] = byte_address[3];
        // 数据流长度  115200
        byte[] bytes = intToBytes2(115200);
        m_Data[10] = bytes[0];
        m_Data[11] = bytes[1];
        m_Data[12] = bytes[2];
        m_Data[13] = bytes[3];
        // 0xSS
        m_Data[14] = 0x01;

        m_Data[15] = (byte) (wallpaper_order_number >> 8);
        m_Data[16] = (byte) wallpaper_order_number;

        m_Length = 17;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

    private void sendData(int num) {
        sendData1(num);
        sendData2(num);
        sendData3(num);
        sendData4(num);
    }

    private void sendData1(int num) {

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] m_Data = new byte[21];
        int m_Length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[2] = 0x3B;
        m_Data[3] = 0x01;
        m_Data[4] = 0x6C;
        m_Data[5] = 0x0F;

        //    序号   M M
        byte[] bytes1 = intToBytes2(num);
        m_Data[6] = bytes1[1];
        m_Data[7] = bytes1[2];
        m_Data[8] = bytes1[3];

        //    本次发送数据包的长度   P P
        byte[] bytes = intToBytes(50);
        m_Data[9] = bytes[0];
        m_Data[10] = bytes[1];

        //有效数据  V V
        for (int i = 0; i < 9; i++) {
            m_Data[11 + i] = pixel_data[i + num * 50];
        }

        //        Log.d(TAG, "sendData1:  "+BinaryToHexString(m_Data));
        m_Length = 20;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);

    }

    private void sendData2(int num) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] m_Data = new byte[32];
        int m_Length = 0;
        for (int i = 0; i < 20; i++) {
            m_Data[i] = pixel_data[i + 9 + num * 50];
        }
        m_Length = 20;

        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

    private void sendData3(int num) {

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] m_Data = new byte[32];
        int m_Length = 0;
        for (int i = 0; i < 20; i++) {
            m_Data[i] = pixel_data[i + 29 + num * 50];
        }
        m_Length = 20;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

    private void sendData4(int num) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] m_Data = new byte[32];
        int m_Length = 0;
        m_Data[0] = pixel_data[num * 50 + 49];
        int sum = 0;
        for (int i = 0; i < 50; i++) {
            int onByte = pixel_data[i + num * 50] & 0xFF;
            sum += onByte;
        }
        //       校验数据---
                Log.d(TAG, "sendData   out:  " +"   校验： "+sum);
        byte yy = (byte) (sum & 0xFF);
        m_Data[1] = yy;
        m_Length = 2;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

    //主机获取设备上指定序号的壁纸的信息.
    private void getOneWallPaperInfo(int index) {
        needMerge = true;
        byte[] m_Data = new byte[32];
        int m_Length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[2] = 0x05;
        m_Data[3] = 0x01;
        m_Data[4] = 0x6c;
        m_Data[5] = 0x03;
        m_Data[6] = intToBytes(index)[0];
        m_Data[7] = intToBytes(index)[1];
        m_Length = 8;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }

    private void previewWallPaper(int index) {
        needMerge = true;
        byte[] m_Data = new byte[32];
        int m_Length = 0;
        m_Data[0] = 0x55;
        m_Data[1] = (byte) 0xFF;
        m_Data[2] = 0x05;
        m_Data[3] = 0x01;
        m_Data[4] = 0x6c;
        m_Data[5] = 0x17;
        m_Data[6] = intToBytes(index)[0];
        m_Data[7] = intToBytes(index)[1];
        m_Length = 8;
        Sys_Proc_Charactor_TX_Send(m_Data, m_Length);
    }


    @Override
    public void onValueChange(NumberPicker numberPicker, int oldVal,
                              int newVal) {
        Log.d(TAG, "onValueChange: id： " + numberPicker.getId() + "  oldVal: " + oldVal + "  newVal: " + newVal);
    }

    @Override
    public void onScrollStateChange(NumberPicker numberPicker, int scrollState) {
        Log.d(TAG, "onValueChange  onScrollStateChange: "+scrollState+"  fling   "+ NumberPicker.OnScrollListener.SCROLL_STATE_FLING+"   idle   "+NumberPicker.OnScrollListener.SCROLL_STATE_IDLE+"  TOUCH_SCROLL:  "+NumberPicker.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
        switch (scrollState){
            case NumberPicker.OnScrollListener.SCROLL_STATE_IDLE:
                int value = numberPicker.getValue()-1;
                Log.d(TAG, "stopValue: "+value+"    "+supportPreview);
                if(supportPreview){
                    previewWallPaper(value);
                }

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //获取图片路径
        if (requestCode == IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumns = {MediaStore.Images.Media.DATA};
            Cursor c = getContentResolver().query(selectedImage, filePathColumns, null, null, null);
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePathColumns[0]);
            String imagePath = c.getString(columnIndex);

            Log.d(TAG, "onActivityResult: "+imagePath);
            Intent myCropIntent = new Intent(SetWallpaperActivity.this,CropActivity.class);
            myCropIntent.putExtra("path",imagePath);
            startActivityForResult(myCropIntent, CROP_IMAGE);

//            Intent pickIntent = new Intent(Intent.ACTION_PICK, null);
//            // 如果限制上传到服务器的图片类型时可以直接写如："image/jpeg 、 image/png等的类型"
//            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
//            startActivityForResult(pickIntent, CROP_IMAGE);
//            showImage(imagePath);
            c.close();
        }else if(requestCode == CROP_IMAGE && resultCode == Activity.RESULT_OK && data != null){
            String path = data.getStringExtra(CropActivity.RESULT_PATH);
            Bitmap photo = BitmapFactory.decodeFile(path);


            if (photo != null)
            {
//                show_img.setImageBitmap(photo);
                sendImg(photo);
            }
        }
    }

    //加载图片
    private void showImage(String imaePath){
        Bitmap bm = BitmapFactory.decodeFile(imaePath);
//        ((ImageView)findViewById(R.id.ge)).setImageBitmap(bm);
        show_img.setImageBitmap(bm);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");

    }
}

