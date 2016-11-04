package com.cocoonshu.cobox.gestureimageview;

import android.content.Context;
import android.view.MotionEvent;

/**
 * Gesture detector for ACTION_UP
 * @author Cocoonshu
 * @date   2015-03-30 15:13:54
 */
public class UpDetector {

    private Context      mContext      = null;
    private OnUpListener mOnUpListener = null;

    public static interface OnUpListener {
        boolean onUp(MotionEvent event);
    }

    public UpDetector(Context context, OnUpListener listener) {
        mContext      = context;
        mOnUpListener = listener;
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
            if (mOnUpListener != null) {
                mOnUpListener.onUp(event);
            }
            return true;
        }
        return false;
    }

}
