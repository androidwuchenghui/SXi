package com.yihai.wu.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * Created by ${Wu} on 2016/12/5.
 */

public class ClickImageView extends ImageView {
    private OnClickListener onClickListener;

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public ClickImageView(Context context) {
        super(context);
    }

    public ClickImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClickImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private static final String TAG = "ClickImageView";

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.setColorFilter(0x99000000);
                return true;
            case MotionEvent.ACTION_UP:
                this.setColorFilter(null);

                if (onClickListener != null) {
                    onClickListener.OnClick(this);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                this.setColorFilter(null);

                break;
        }
        return super.onTouchEvent(event);
    }

    public interface OnClickListener {
        public void OnClick(ClickImageView view);
    }


}