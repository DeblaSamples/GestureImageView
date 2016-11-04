package com.cocoonshu.cobox.gestureimageview;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Progress Drawable
 * @Auther Cocoonshu
 * @Date   2016-10-24 14:26:55
 */
public class ProgressDrawable extends Drawable {

    public static final float STROKE_WIDTH = 3.0f;

    private Paint   mPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF   mDrawArea  = new RectF();
    private float   mProgress  = 0f;
    private boolean mIsEnabled = true;

    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mIsEnabled) {
            float boundsWidth = getBounds().width();
            float boundsHeight = getBounds().height();
            float minEdge = Math.min(boundsWidth, boundsHeight);

            mDrawArea.set(
                    (boundsWidth - minEdge) * 0.5f + 4 * STROKE_WIDTH,
                    (boundsHeight - minEdge) * 0.5f + 4 * STROKE_WIDTH,
                    (boundsWidth + minEdge) * 0.5f - 4 * STROKE_WIDTH,
                    (boundsHeight + minEdge) * 0.5f - 4 * STROKE_WIDTH);

            mPaint.setColor(0x88FFFFFF);
            mPaint.setStrokeWidth(3.0f);

            mPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(boundsWidth * 0.5f, boundsHeight * 0.5f, minEdge * 0.5f - STROKE_WIDTH, mPaint);

            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawArc(mDrawArea, -90, mProgress * 360, true, mPaint);
        }
    }

    public void setProgress(float progress) {
        mProgress = progress;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        int alpha = mPaint.getAlpha();
        return alpha < 255 && alpha > 0 ? PixelFormat.TRANSLUCENT :
                (alpha == 0 ? PixelFormat.TRANSPARENT : PixelFormat.OPAQUE);
    }
}