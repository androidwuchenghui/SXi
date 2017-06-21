package com.yihai.wu.widgets;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.yihai.wu.sxi.R;

/**
 * Created by ${Wu} on 2017/5/23.
 */

public class RoundProgressDialog extends Dialog {
    private Context context;
    private RoundProgressBar  rpb;
    public RoundProgressDialog(Context context) {
        super(context);
        this.context = context;
    }

    public RoundProgressDialog(Context context, int themeResId) {
        super(context, themeResId);
        this.context = context;
    }

    protected RoundProgressDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
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
        View view = inflater.inflate(R.layout.dialog_round_progress, null);
        setContentView(view);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(params);
        rpb  = (RoundProgressBar) view.findViewById(R.id.roundProgressBar2);

    }

    public void setProgress(int progress) {
        rpb.setProgress(progress);
    }
}
