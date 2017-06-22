package com.yihai.wu.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.dinuscxj.progressbar.CircleProgressBar;
import com.yihai.wu.sxi.R;

/**
 * Created by ${Wu} on 2017/5/17.
 */

public class WallpaperDialogView extends Dialog {
    private Context context;
//    private static SquareProgressBar spBar;
private CircleProgressBar mCircleProgressBar;
    public WallpaperDialogView(Context context) {
        super(context,R.style.CustomDialogStyle);
        this.context = context;
    }

    public WallpaperDialogView(Context context, int themeResId) {
        super(context, themeResId);
        this.context = context;
    }

    protected WallpaperDialogView(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        this.context = context;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_wallpaper, null);
        setContentView(view);

//        spBar = (SquareProgressBar) view.findViewById(spBar);
        mCircleProgressBar = (CircleProgressBar) view.findViewById(R.id.mCircleProgressBar);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(params);
    }

    public void setImage(Bitmap bm) {
//        spBar.setImageBitmap(bm);
    }

    public void setProgress(double progress) {
//        spBar.setProgress(progress);
        mCircleProgressBar.setProgress((int) progress);
    }

    public void setDialogStyle() {

//        spBar.setOpacity(true);
//        spBar.setClearOnHundred(true);
//        spBar.showProgress(true);
//        spBar.setPercentStyle(new PercentStyle(Paint.Align.CENTER, 50, true));
//        mCircleProgressBar.setMax(100);

    }
//
//    public SquareProgressBar getBar(){
//        return spBar;
//    }

    public void setSendProgress(int progress) {
//        tv_show_progress.setText(progress);
    }

}
