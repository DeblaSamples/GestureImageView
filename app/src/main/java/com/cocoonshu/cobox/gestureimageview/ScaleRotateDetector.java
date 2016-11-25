package com.cocoonshu.cobox.gestureimageview;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;

import com.cocoonshu.cobox.animation.Smoother;

/**
 * Gesture detector for scale and rotate mixing
 * @author Cocoonshu
 * @date 2016-11-02 10:21:46
 */
public class ScaleRotateDetector {

    private static final float MIN_SCALE_FACTOR                   = 0.01f;
    private static final float ROTATE_SMOOTH_FACTOR               = 5E-1F;
    private static final float ROTATE_SMOOTH_ERROR                = 5E-2F;
    private static final float SCALE_SMOOTH_FACTOR                = 5E-1F;
    private static final float SCALE_SMOOTH_ERROR                 = 5E-2F;
    private static final int   INVALID_POINTER_POSITION           = -1;
    private static final int   PROCESS_SCALE_ROTATE_BEGIN         = 0x0001;
    private static final int   PROCESS_SCALE_ROTATE               = 0x0002;
    private static final int   PROCESS_SCALE_ROTATE_END           = 0x0003;
    private static final int   HANDLE_REQUEST_INTERPOLATER_EVENT  = 0x0004;

    private Context mContext                 = null;
    private Handler mHandler                 = null;
    private OnScaleRotateGestureListener mListener                = null;

    private int                          mPointerCount            = 0;
    private boolean                      mIsEventBegin            = false;
    private boolean                      mIsRotateSmoothEnabled   = false;
    private boolean                      mIsScaleSmoothEnabled    = false;
    private boolean                      mIsCenterReferedByScale  = false;
    private boolean                      mIsCenterReferedByRotate = false;
    private float                        mInitScaleDistance       = 0;
    private float                        mPrevScaleDistance       = 0;
    private float                        mCurrentScaleDistance    = 0;
    private float                        mInitRotateRadian        = 0;
    private float                        mPrevRotateRadian        = 0;
    private float                        mCurrentRotateRadian     = 0;
    private PointF mPointerCenter           = new PointF();
    private PointF mPointerMain             = new PointF();
    private PointF mPointerSecondery        = new PointF();
    private Smoother mRotateSmoother          = new Smoother(ROTATE_SMOOTH_FACTOR, ROTATE_SMOOTH_ERROR);
    private Smoother                     mScaleSmoother           = new Smoother(SCALE_SMOOTH_FACTOR, SCALE_SMOOTH_ERROR);

//    private Smoother2                    mRotatePivotSmoother     = new Smoother2();
//    private Smoother2                    mScalePivotSmoother      = new Smoother2();

    public interface OnScaleRotateGestureListener {
        boolean onScaleRotateBegin(
                float scalePivotX, float scalePivotY, float angle,
                float rotatePivotX, float rotatePivotY, float scale,
                ScaleRotateDetector detector);
        boolean onScaleRotate(
                float scalePivotX, float scalePivotY, float angle,
                float rotatePivotX, float rotatePivotY, float scale,
                ScaleRotateDetector detector);
        boolean onScaleRotateEnd(
                float scalePivotX, float scalePivotY, float angle,
                float rotatePivotX, float rotatePivotY, float scale,
                ScaleRotateDetector detector);
    }

    public ScaleRotateDetector(Context context, OnScaleRotateGestureListener listener) {
        mContext = context;
        mListener = listener;
        mHandler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case HANDLE_REQUEST_INTERPOLATER_EVENT:
                    fireEventProcess(PROCESS_SCALE_ROTATE);
                    break;
                }
            }

        };
    }

    public void setRotateSmoothEnabled(boolean enabled) {
        mIsRotateSmoothEnabled = enabled;
    }

    public void setScaleSmoothEnabled(boolean enabled) {
        mIsScaleSmoothEnabled = enabled;
    }

    public void setCenterPointAsScalePivot(boolean enabled) {
        mIsCenterReferedByScale = enabled;
    }

    public void setCenterPointAsRotatePivot(boolean enabled) {
        mIsCenterReferedByRotate = enabled;
    }

    public float getScaleFactor() {
        return computeScaleFactor();
    }

    public float getDeltaScaleFactor() {
        return computeDeltaScaleFactor();
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        boolean state = false;
        mPointerCount = event.getPointerCount();
        confirmPointerPosition(event);

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            state = true;
            break;
        case MotionEvent.ACTION_POINTER_DOWN:
            if (mPointerCount == 2) {
                computeInitializeScaleDistance();
                computeInitializeRotateAngle();
                fireEventProcess(PROCESS_SCALE_ROTATE_BEGIN);
            }
            state = true;
            break;
        case MotionEvent.ACTION_MOVE:
            if (mPointerCount >= 2) {
                computeCurrentScaleDistance();
                computeCurrentRotateAngle();
                fireEventProcess(PROCESS_SCALE_ROTATE);
            }
            state = true;
            break;
        case MotionEvent.ACTION_POINTER_UP:
            if (mPointerCount == 2) {
                computeCurrentScaleDistance();
                computeCurrentRotateAngle();
                fireEventProcess(PROCESS_SCALE_ROTATE_END);
            }
            state = true;
            break;
        case MotionEvent.ACTION_UP:
            resetEvent();
            state = true;
            break;
        }

        return state;
    }

    private final void computeInitializeScaleDistance() {
        float deltaX = mPointerSecondery.x - mPointerMain.x;
        float deltaY = mPointerSecondery.y - mPointerMain.y;
        mInitScaleDistance    = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        mPrevScaleDistance    = mInitScaleDistance;
        mCurrentScaleDistance = mInitScaleDistance;
    }

    private final void computeCurrentScaleDistance() {
        float deltaX = mPointerSecondery.x - mPointerMain.x;
        float deltaY = mPointerSecondery.y - mPointerMain.y;
        mPrevScaleDistance    = mCurrentScaleDistance;
        mCurrentScaleDistance = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private final float computeScaleFactor() {
        float scaleFactor = mCurrentScaleDistance / mInitScaleDistance;
        scaleFactor = scaleFactor < MIN_SCALE_FACTOR ? MIN_SCALE_FACTOR : scaleFactor;
        return scaleFactor;
    }

    private final float computeDeltaScaleFactor() {
        return 0;
    }

    private final void computeInitializeRotateAngle() {
        float deltaX = mPointerSecondery.x - mPointerMain.x;
        float deltaY = mPointerSecondery.y - mPointerMain.y;
        mInitRotateRadian = (float) Math.atan2(deltaY, deltaX);
        mCurrentRotateRadian = mInitRotateRadian;
    }

    private final void computeCurrentRotateAngle() {
        float deltaX = mPointerSecondery.x - mPointerMain.x;
        float deltaY = mPointerSecondery.y - mPointerMain.y;
        mCurrentRotateRadian = (float) Math.atan2(deltaY, deltaX);
    }

    private final float computeDeltaRotateAngle() {
        return (float) Math.toDegrees(computeDeltaRotateRadian());
    }

    private final float computeDeltaRotateRadian() {
        return mCurrentRotateRadian - mInitRotateRadian;
    }

    public final float getDeltaRotateRadian() {
        return computeDeltaRotateRadian();
    }

    public final float getDeltaRotateAngle() {
        return computeDeltaRotateRadian();
    }

    private void confirmPointerPosition(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        if (pointerCount == 1) {
            mPointerMain.x = event.getX(0);
            mPointerMain.y = event.getY(0);
            mPointerSecondery.x = INVALID_POINTER_POSITION;
            mPointerSecondery.y = INVALID_POINTER_POSITION;
            mPointerCenter.x = mPointerMain.x;
            mPointerCenter.y = mPointerMain.y;
        } else if (pointerCount >= 2) {
            mPointerMain.x = event.getX(0);
            mPointerMain.y = event.getY(0);
            mPointerSecondery.x = event.getX(1);
            mPointerSecondery.y = event.getY(1);
            mPointerCenter.x = mPointerMain.x + (mPointerSecondery.x - mPointerMain.x) * 0.5f;
            mPointerCenter.y = mPointerMain.y + (mPointerSecondery.y - mPointerMain.y) * 0.5f;
        } else {
            mPointerMain.x = INVALID_POINTER_POSITION;
            mPointerMain.y = INVALID_POINTER_POSITION;
            mPointerSecondery.x = INVALID_POINTER_POSITION;
            mPointerSecondery.y = INVALID_POINTER_POSITION;
            mPointerCenter.x = INVALID_POINTER_POSITION;
            mPointerCenter.y = INVALID_POINTER_POSITION;
        }
    }

    private void fireEventProcess(int fakeEvent) {
        switch (fakeEvent) {
        case PROCESS_SCALE_ROTATE_BEGIN:{
            if (mListener != null) {
                float rotatePivotX = 0;
                float rotatePivotY = 0;
                float scalePivotX  = 0;
                float scalePivotY  = 0;

                if (mIsCenterReferedByScale) {
                    rotatePivotX = mPointerCenter.x;
                    rotatePivotY = mPointerCenter.y;
                } else {
                    rotatePivotX = mPointerMain.x;
                    rotatePivotY = mPointerMain.y;
                }

                if (mIsCenterReferedByRotate) {
                    scalePivotX  = mPointerCenter.x;
                    scalePivotY  = mPointerCenter.y;
                } else {
                    scalePivotX  = mPointerMain.x;
                    scalePivotY  = mPointerMain.y;
                }

                float scale = computeScaleFactor();
                float rotate = computeDeltaRotateAngle();
                mRotateSmoother.setDestinationValue(rotate);
                mRotateSmoother.forceFinish();
                mScaleSmoother.setDestinationValue(scale);
                mScaleSmoother.forceFinish();
                mListener.onScaleRotateBegin(
                        rotatePivotX, rotatePivotY, mIsRotateSmoothEnabled ? mRotateSmoother.getCurrentValue() : rotate,
                        scalePivotX, scalePivotY, mIsScaleSmoothEnabled ? mScaleSmoother.getCurrentValue() : scale,
                        this);
            }
        }
            break;
        case PROCESS_SCALE_ROTATE:
        {
            if (mListener != null) {
                float rotatePivotX = 0;
                float rotatePivotY = 0;
                float scalePivotX  = 0;
                float scalePivotY  = 0;

                if (mIsCenterReferedByScale) {
                    rotatePivotX = mPointerCenter.x;
                    rotatePivotY = mPointerCenter.y;
                } else {
                    rotatePivotX = mPointerMain.x;
                    rotatePivotY = mPointerMain.y;
                }

                if (mIsCenterReferedByRotate) {
                    scalePivotX  = mPointerCenter.x;
                    scalePivotY  = mPointerCenter.y;
                } else {
                    scalePivotX  = mPointerMain.x;
                    scalePivotY  = mPointerMain.y;
                }

                float scale = computeScaleFactor();
                float rotate = computeDeltaRotateAngle();
                mRotateSmoother.setDestinationValue(rotate);
                mScaleSmoother.setDestinationValue(scale);
                boolean hasMoreFrames = mRotateSmoother.smooth() || mScaleSmoother.smooth();

                mListener.onScaleRotate(
                        rotatePivotX, rotatePivotY, mIsRotateSmoothEnabled ? mRotateSmoother.getCurrentValue() : rotate,
                        scalePivotX, scalePivotY, mIsScaleSmoothEnabled ? mScaleSmoother.getCurrentValue() : scale,
                        this);

                if (hasMoreFrames) {
                    mHandler.sendEmptyMessage(HANDLE_REQUEST_INTERPOLATER_EVENT);
                }
            }
        }
            break;
        case PROCESS_SCALE_ROTATE_END:
        {
            if (mListener != null) {
                float rotatePivotX = 0;
                float rotatePivotY = 0;
                float scalePivotX  = 0;
                float scalePivotY  = 0;

                if (mIsCenterReferedByScale) {
                    rotatePivotX = mPointerCenter.x;
                    rotatePivotY = mPointerCenter.y;
                } else {
                    rotatePivotX = mPointerMain.x;
                    rotatePivotY = mPointerMain.y;
                }

                if (mIsCenterReferedByRotate) {
                    scalePivotX  = mPointerCenter.x;
                    scalePivotY  = mPointerCenter.y;
                } else {
                    scalePivotX  = mPointerMain.x;
                    scalePivotY  = mPointerMain.y;
                }

                float scale = computeScaleFactor();
                float rotate = computeDeltaRotateAngle();
                mRotateSmoother.setDestinationValue(rotate);
                mScaleSmoother.setDestinationValue(scale);
                mRotateSmoother.smooth();
                mScaleSmoother.smooth();

                mListener.onScaleRotateEnd(
                        rotatePivotX, rotatePivotY, mIsRotateSmoothEnabled ? mRotateSmoother.getCurrentValue() : rotate,
                        scalePivotX, scalePivotY, mIsScaleSmoothEnabled ? mScaleSmoother.getCurrentValue() : scale,
                        this);

                mHandler.removeMessages(HANDLE_REQUEST_INTERPOLATER_EVENT);
            }
        }
            break;
        }
    }

    private void resetEvent() {
        mPointerCount       = 0;
        mIsEventBegin       = false;
        mPointerMain.x      = INVALID_POINTER_POSITION;
        mPointerMain.y      = INVALID_POINTER_POSITION;
        mPointerSecondery.x = INVALID_POINTER_POSITION;
        mPointerSecondery.y = INVALID_POINTER_POSITION;
    }

}
