package com.cocoonshu.cobox.gestureimageview;

/**
 * Smoother of Value
 * @author Cocoonshu
 * @date 2015-03-26 17:04:39
 */
public class Smoother {

    private float   mFactor           = 1E-2F;
    private float   mError            = 1E-3F;
    private float   mCurrentValue     = 0;
    private float   mDestinationValue = 0;
    private boolean mIsBypassed       = false;

    public Smoother() {

    }

    public Smoother(float factor, float error) {
        mFactor = factor;
        mError = error;
    }

    public final void setBypassed(boolean isBypassed) {
        mIsBypassed = isBypassed;
    }

    public final boolean isBypassed() {
        return mIsBypassed;
    }

    public boolean smooth() {
        if (mIsBypassed) {
            mCurrentValue = mDestinationValue;
            return false;
        }

        float targetValue = mCurrentValue + (mDestinationValue - mCurrentValue) * mFactor;
        if (targetValue == mCurrentValue) {
            mCurrentValue = mDestinationValue;
            return false;
        } else {
            mCurrentValue = targetValue;
            if (Math.abs(mDestinationValue - mCurrentValue) > mError) {
                return true;
            } else {
                mCurrentValue = mDestinationValue;
                return false;
            }
        }
    }

    public void forceFinish() {
        mCurrentValue = mDestinationValue;
    }

    public final float getError() {
        return mError;
    }

    public final float getFactor() {
        return mFactor;
    }

    public float getCurrentValue() {
        return mCurrentValue;
    }

    public float getDestinationValue() {
        return mDestinationValue;
    }

    public void setCurrentValue(float currentValue) {
        mCurrentValue = currentValue;
    }

    public void setDestinationValue(float destinationValue) {
        mDestinationValue = destinationValue;
    }
}