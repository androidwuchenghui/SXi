package com.yihai.wu.util;


import android.content.Context;

/**
 * Created by Dacer on 10/8/13.
 */
public class MyUtils {
    private static String hexStr = "0123456789ABCDEF";

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    public static String hexStringToString(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        s = s.replace(" ", "");
        byte[] baKeyword = new byte[s.length() / 2];
        for (int i = 0; i < baKeyword.length; i++) {
            try {
                baKeyword[i] = (byte) (0xff & Integer.parseInt(
                        s.substring(i * 2, i * 2 + 2), 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            s = new String(baKeyword, "gbk");
            new String();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return s;
    }

    //byte[] 转换成16进制字符
    public static String BinaryToHexString(byte[] bytes) {
        String result = "";
        String hex = "";
        for (int i = 0; i < bytes.length; i++) {
            //字节高4位
            hex = String.valueOf(hexStr.charAt((bytes[i] & 0xF0) >> 4));
            //字节低4位
            hex += String.valueOf(hexStr.charAt(bytes[i] & 0x0F));
            result += hex + "";  //这里可以去掉空格，或者添加0x标识符。
        }
        return result;
    }

    public static byte int2OneByte(int num) {
        return (byte) (num & 0x000000ff);
    }

    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        for (int i = begin; i < begin + count; i++)
            bs[i - begin] = src[i];
        return bs;
    }

    //从一百个点中每隔5个选出一个点
    private static int[] getReadedCurveData(String string) {
        int result[] = new int[21];
        int index = 0;
        String[] splited = string.split(",");
        int[] data = new int[splited.length];
        for (int i = 0; i < splited.length; i++) {
            data[i] = Integer.parseInt(splited[i]);
            if (i % 5 == 0) {
                result[index] = data[i];
                index++;
            }
        }
        return result;
    }
    public static class NoDoubleClickUtils {
        private static long lastClickTime;
        private final static int SPACE_TIME = 800;

        public static void initLastClickTime() {
            lastClickTime = 0;
        }

        public synchronized static boolean isDoubleClick() {
            long currentTime = System.currentTimeMillis();
            boolean isClick2;
            if (currentTime - lastClickTime > SPACE_TIME) {
                isClick2 = false;
            } else {
                isClick2 = true;
            }
            lastClickTime = currentTime;
            return isClick2;
        }
    }
}
