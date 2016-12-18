package com.yihai.wu.image_button;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by ${Wu} on 2016/12/5.
 */

public class DarkImageButton extends ImageView {

    public DarkImageButton(Context context) {
        super(context);
        init(context, null);
    }

    public DarkImageButton(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.imageButtonStyle);
        init(context, attrs);
    }

    public DarkImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setBackgroundDrawable(createStateDrawable(context, getBackground()));//布局里设置background
        //      setImageDrawable(createStateDrawable(context, getBackground()));//布局里设置Src
        setFocusable(true);
    }

    @Override
    protected boolean onSetAlpha(int alpha) {
        return false;
    }

    public StateListDrawable createStateDrawable(Context context,
                                                 Drawable normal) {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(View.PRESSED_ENABLED_STATE_SET,
                createPressDrawable(normal));
        drawable.addState(View.ENABLED_STATE_SET, normal);
        drawable.addState(View.EMPTY_STATE_SET, normal);
        return drawable;
    }

    public Drawable createPressDrawable(Drawable d) {
        Bitmap bitmap = ((BitmapDrawable) d).getBitmap().copy(
                Bitmap.Config.ARGB_8888, true);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(0x60000000);
        new Canvas(bitmap).drawCircle(bitmap.getWidth()/2,bitmap.getWidth()/2, bitmap.getWidth()/2, paint);
        return new BitmapDrawable(bitmap);
    }
}
