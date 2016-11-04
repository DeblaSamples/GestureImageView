package com.cocoonshu.cobox.gestureimageview;

import android.content.Context;
import android.view.MotionEvent;

/**
 * Hover gesture detector
 * @author Cocoonshu
 * @date   2015-03-30 15:51:40
 */
public class HoverDetector {

    private static final String TAG = "HoverDetector";

    private Context         mContext         = null;
    private OnHoverListener mOnHoverListener = null;

    public static interface OnHoverListener {
        boolean onHover(float x, float y);
        boolean onHoverLeave(float x, float y);
    }

    public HoverDetector(Context context, OnHoverListener listener) {
        mContext         = context;
        mOnHoverListener = listener;
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (mOnHoverListener != null) {
                mOnHoverListener.onHover(event.getX(), event.getY());
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (mOnHoverListener != null) {
                mOnHoverListener.onHover(event.getX(), event.getY());
            }
            break;
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_OUTSIDE:
            if (mOnHoverListener != null) {
                mOnHoverListener.onHoverLeave(event.getX(), event.getY());
            }
            break;
        }
        return true;
    }

}
