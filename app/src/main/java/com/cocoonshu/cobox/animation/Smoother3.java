package com.cocoonshu.cobox.animation;

/**
 * Smoother of Value
 * @author Cocoonshu
 * @date 2015-03-26 17:04:39
 */
public class Smoother3 {

    private float   mFactor            = 1E-2F;
    private float   mError             = 1E-3F;
    private float   mCurrentValueX     = 0;
    private float   mCurrentValueY     = 0;
    private float   mCurrentValueZ     = 0;
    private float   mDestinationValueX = 0;
    private float   mDestinationValueY = 0;
    private float   mDestinationValueZ = 0;
    private boolean mIsBypassed        = false;

    public Smoother3() {

    }

    public Smoother3(float factor, float error) {
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
            mCurrentValueZ = mDestinationValueZ;
            return false;
        }

        float targetX = mCurrentValueX + (mDestinationValueX - mCurrentValueX) * mFactor;
        float targetY = mCurrentValueY + (mDestinationValueY - mCurrentValueY) * mFactor;
        float targetZ = mCurrentValueZ + (mDestinationValueZ - mCurrentValueZ) * mFactor;
        if (mCurrentValueX == targetX && mCurrentValueY == targetY && mCurrentValueZ == targetZ) {
            mCurrentValueX = mDestinationValueX;
            mCurrentValueY = mDestinationValueY;
            mCurrentValueZ = mDestinationValueZ;
            return false;
        } else {
            mCurrentValueX = targetX;
            mCurrentValueY = targetY;
            mCurrentValueZ = targetZ;
            if (Math.abs((mDestinationValueX + mDestinationValueY) - (mCurrentValueX + mCurrentValueY) - (mCurrentValueZ + mCurrentValueZ)) > mError) {
                return true;
            } else {
                mCurrentValueX = mDestinationValueX;
                mCurrentValueY = mDestinationValueY;
                mCurrentValueZ = mDestinationValueZ;
                return false;
            }
        }
    }

    public void forceFinish() {
        mCurrentValueX = mDestinationValueX;
        mCurrentValueY = mDestinationValueY;
        mCurrentValueZ = mDestinationValueZ;
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

    public float getCurrentValueZ() {
        return mCurrentValueZ;
    }

    public float getDestinationValueX() {
        return mDestinationValueX;
    }

    public float getDestinationValueY() {
        return mDestinationValueY;
    }

    public float getDestinationValueZ() {
        return mDestinationValueZ;
    }

    public void setCurrentValue(float currentValueX, float currentValueY, float currentValueZ) {
        mCurrentValueX = currentValueX;
        mCurrentValueY = currentValueY;
        mCurrentValueZ = currentValueZ;
    }

    public void setDestinationValue(float destinationValueX, float destinationValueY, float destinationValueZ) {
        mDestinationValueX = destinationValueX;
        mDestinationValueY = destinationValueY;
        mDestinationValueZ = destinationValueZ;
    }
}