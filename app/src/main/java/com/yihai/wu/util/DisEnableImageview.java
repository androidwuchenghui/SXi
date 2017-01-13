package com.yihai.wu.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by ${Wu} on 2017/1/11.
 */

public class DisEnableImageView extends ImageView {

    public DisEnableImageView(Context context) {
        this(context,null);
    }

    public DisEnableImageView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public DisEnableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.setColorFilter(0x50000000);
    }
}
