package com.cocoonshu.cobox.animation;

/**
 * Smoother of Value
 * @author Cocoonshu
 * @date 2015-03-26 17:04:39
 */
public class Smoother4 {

    private float   mFactor                 = 1E-2F;
    private float   mError                  = 1E-3F;
    private float   mCurrentValueLeft       = 0;
    private float   mCurrentValueRight      = 0;
    private float   mCurrentValueTop        = 0;
    private float   mCurrentValueBottom     = 0;
    private float   mDestinationValueLeft   = 0;
    private float   mDestinationValueRight  = 0;
    private float   mDestinationValueTop    = 0;
    private float   mDestinationValueBottom = 0;
    private boolean mIsBypassed             = false;

    public Smoother4() {

    }

    public Smoother4(float factor, float error) {
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
            mCurrentValueLeft   = mDestinationValueLeft;
            mCurrentValueRight  = mDestinationValueRight;
            mCurrentValueTop    = mDestinationValueTop;
            mCurrentValueBottom = mDestinationValueBottom;
            return false;
        }

        float targetLeft   = mCurrentValueLeft + (mDestinationValueLeft - mCurrentValueLeft) * mFactor;
        float targetRight  = mCurrentValueRight + (mDestinationValueRight - mCurrentValueRight) * mFactor;
        float targetTop    = mCurrentValueTop + (mDestinationValueTop - mCurrentValueTop) * mFactor;
        float targetBottom = mCurrentValueBottom + (mDestinationValueBottom - mCurrentValueBottom) * mFactor;
        if (mCurrentValueLeft == targetLeft && mCurrentValueRight == targetRight && mCurrentValueTop == targetTop && mCurrentValueBottom == targetBottom) {
            mCurrentValueLeft   = mDestinationValueLeft;
            mCurrentValueRight  = mDestinationValueRight;
            mCurrentValueTop    = mDestinationValueTop;
            mCurrentValueBottom = mDestinationValueBottom;
            return false;
        } else {
            mCurrentValueLeft   = targetLeft;
            mCurrentValueRight  = targetRight;
            mCurrentValueTop    = targetTop;
            mCurrentValueBottom = targetBottom;
            if (Math.abs(
                    (mDestinationValueLeft + mDestinationValueRight)
                  - (mCurrentValueLeft + mCurrentValueRight)
                  - (mCurrentValueTop + mCurrentValueTop)
                  - (mCurrentValueBottom + mCurrentValueBottom)
                ) > mError) {
                return true;
            } else {
                mCurrentValueLeft   = mDestinationValueLeft;
                mCurrentValueRight  = mDestinationValueRight;
                mCurrentValueTop    = mDestinationValueTop;
                mCurrentValueBottom = mDestinationValueBottom;
                return false;
            }
        }
    }

    public void forceFinish() {
        mCurrentValueLeft   = mDestinationValueLeft;
        mCurrentValueRight  = mDestinationValueRight;
        mCurrentValueTop    = mDestinationValueTop;
        mCurrentValueBottom = mDestinationValueBottom;
    }

    public final float getError() {
        return mError;
    }

    public final float getFactor() {
        return mFactor;
    }

    public float getCurrentLeft() {
        return mCurrentValueLeft;
    }

    public float getCurrentRight() {
        return mCurrentValueRight;
    }

    public float getCurrentTop() {
        return mCurrentValueTop;
    }

    public float getCurrentBottom() {
        return mCurrentValueBottom;
    }

    public float getDestinationLeft() {
        return mDestinationValueLeft;
    }

    public float getDestinationRight() {
        return mDestinationValueRight;
    }

    public float getDestinationTop() {
        return mDestinationValueTop;
    }

    public float getDestinationBottom() {
        return mDestinationValueBottom;
    }

    public void setCurrentValue(float currentValueLeft, float currentValueTop, float currentValueRight, float currentValueBottom) {
        mCurrentValueLeft   = currentValueLeft;
        mCurrentValueRight  = currentValueRight;
        mCurrentValueTop    = currentValueTop;
        mCurrentValueBottom = currentValueBottom;
    }

    public void setDestinationValue(float destinationValueLeft, float destinationValueTop, float destinationValueRight, float destinationValueBottom) {
        mDestinationValueLeft   = destinationValueLeft;
        mDestinationValueRight  = destinationValueRight;
        mDestinationValueTop    = destinationValueTop;
        mDestinationValueBottom = destinationValueBottom;
    }
}