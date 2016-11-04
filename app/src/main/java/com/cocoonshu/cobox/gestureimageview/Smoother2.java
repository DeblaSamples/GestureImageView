package com.cocoonshu.cobox.gestureimageview;

/**
 * Smoother of Value
 * @author Cocoonshu
 * @date 2015-03-26 17:04:39
 */
public class Smoother2 {

    private float   mFactor            = 1E-2F;
    private float   mError             = 2E-3F;
    private float   mCurrentValueX     = 0;
    private float   mCurrentValueY     = 0;
    private float   mDestinationValueX = 0;
    private float   mDestinationValueY = 0;
    private boolean mIsBypassed        = false;

    public Smoother2() {

    }

    public Smoother2(float factor, float error) {
        mFactor = factor;
        mError = error;
    }

    public void setBypassed(boolean isBypassed) {
        mIsBypassed = isBypassed;
    }

    public boolean isBypassed() {
        return mIsBypassed;
    }

    public boolean smooth() {
        if (mIsBypassed) {
            mCurrentValueX = mDestinationValueX;
            mCurrentValueY = mDestinationValueY;
            return false;
        }

        float targetValueX = mCurrentValueX + (mDestinationValueX - mCurrentValueX) * mFactor;
        float targetValueY = mCurrentValueY + (mDestinationValueY - mCurrentValueY) * mFactor;
        if (mCurrentValueX == targetValueX && mCurrentValueY == targetValueY) {
            mCurrentValueX = mDestinationValueX;
            mCurrentValueY = mDestinationValueY;
            return false;
        } else {
            mCurrentValueX = targetValueX;
            mCurrentValueY = targetValueY;
            if (Math.abs(mDestinationValueX - mCurrentValueX) + Math.abs(mDestinationValueY - mCurrentValueY) > mError) {
                return true;
            } else {
                mCurrentValueX = mDestinationValueX;
                mCurrentValueY = mDestinationValueY;
                return false;
            }
        }
    }

    public void forceFinish() {
        mCurrentValueX = mDestinationValueX;
        mCurrentValueY = mDestinationValueY;
    }

    public final float getError() {
        return mError;
    }

    public final float getFactor() {
        return mFactor;
    }

    public float getCurrentValueX() {
        return mCurrentValueX;
    }

    public float getCurrentValueY() {
        return mCurrentValueY;
    }

    public float getDestinationValueX() {
        return mDestinationValueX;
    }

    public float getDestinationValueY() {
        return mDestinationValueY;
    }

    public void setCurrentValue(float currentValueX, float currentValueY) {
        mCurrentValueX = currentValueX;
        mCurrentValueY = currentValueY;
    }

    public void setDestinationValue(float destinationValueX, float destinationValueY) {
        mDestinationValueX = destinationValueX;
        mDestinationValueY = destinationValueY;
    }
}