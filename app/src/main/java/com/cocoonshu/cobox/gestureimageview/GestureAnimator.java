package com.cocoonshu.cobox.gestureimageview;

import android.graphics.Matrix;
import android.graphics.RectF;

/**
 * Gesture animator for Gestured image view
 * @author Cocoonshu
 * @date 2015-07-31 15:40:36
 */
public class GestureAnimator {

    public  static final String  TAG          = "GestureAnimator";
    private static final float[] sTempPolygon = new float[8];

    private float   mZoomInScaleTimes      = 2f;

    private RectF   mImageRect             = new RectF();
    private RectF   mDisplayRect           = new RectF();
    private RectF   mDrawingOutBounds      = new RectF();

    private Vector  mCurrentPose           = new Vector();
    private Vector  mFinalPose             = new Vector();
    private Matrix  mCurrentImageTransform = new Matrix();
    private Matrix  mFinalImageTransform   = new Matrix();
    private Matrix  mOperationTransform    = new Matrix();

    private OnInvalidateListener mOnInvalidateListener = null;

    public static interface OnInvalidateListener {
        void onInvaliadate();
    }

    public boolean compute() {
        boolean hasMoreAnimation = false;

        mCurrentPose.reset();
        mCurrentPose.apply(mCurrentImageTransform);
        mFinalPose.reset();
        mFinalPose.apply(mFinalImageTransform);
        if (mCurrentPose.equals(mFinalPose)) {
            hasMoreAnimation = false;
        } else {
            hasMoreAnimation = mCurrentPose.forward(mFinalPose);
            mCurrentPose.affine(mCurrentImageTransform);
        }

        return hasMoreAnimation;
    }

    public void setOnInvalidateListener(OnInvalidateListener listener) {
        mOnInvalidateListener = listener;
    }

    public void setImageRect(float left, float top, float right, float bottom) {
        mImageRect.set(left, top, right, bottom);
    }

    public void setDisplayRect(float left, float top, float right, float bottom) {
        mDisplayRect.set(left, top, right, bottom);
    }

    public final RectF getDisplayRect() {
        return mDisplayRect;
    }

    public final RectF getDrawingOutBound() {
        float minLeft      = Float.POSITIVE_INFINITY;
        float maxRight     = Float.NEGATIVE_INFINITY;
        float minTop       = Float.POSITIVE_INFINITY;
        float maxBottom    = Float.NEGATIVE_INFINITY;

        synchronized (sTempPolygon) {
            sTempPolygon[0] = mImageRect.left;  sTempPolygon[1] = mImageRect.top;
            sTempPolygon[2] = mImageRect.right; sTempPolygon[3] = mImageRect.top;
            sTempPolygon[4] = mImageRect.left;  sTempPolygon[5] = mImageRect.bottom;
            sTempPolygon[6] = mImageRect.right; sTempPolygon[7] = mImageRect.bottom;
            mFinalImageTransform.mapPoints(sTempPolygon);
            for (int i = 0; i < sTempPolygon.length; i += 2) {
                float x = sTempPolygon[i + 0];
                float y = sTempPolygon[i + 1];
                minLeft   = x < minLeft ? x : minLeft;
                maxRight  = x > maxRight ? x : maxRight;
                minTop    = y < minTop ? y : minTop;
                maxBottom = y > maxBottom ? y : maxBottom;
            }
        }
        synchronized (mDrawingOutBounds) {
            mDrawingOutBounds.set(minLeft, minTop, maxRight, maxBottom);
        }
        return mDrawingOutBounds;
    }

    public void reset() {
        mImageRect.set(0, 0, 0, 0);
        mDisplayRect.set(0, 0, 0, 0);
        mCurrentImageTransform.reset();
        mFinalImageTransform.reset();
    }

    public void revert() {
        revert(false);
    }

    public void revert(boolean withAnimation) {
        if (mImageRect.isEmpty() || mDisplayRect.isEmpty()) {
            return;
        }

        float displayWidth  = mDisplayRect.width();
        float displayHeight = mDisplayRect.height();
        float imageWidth    = mImageRect.width();
        float imageHeight   = mImageRect.height();
        float suggestScale  = computeSuggestScale(imageWidth, imageHeight, displayWidth, displayHeight);
        float translateX    = mDisplayRect.left + (displayWidth - imageWidth * suggestScale) * 0.5f;
        float translateY    = mDisplayRect.top + (displayHeight - imageHeight * suggestScale) * 0.5f;
        float rotate        = 0;

        if (withAnimation) {
            mFinalImageTransform.reset();
            mFinalPose.reset();
            makeFinalImageTransform(translateX, translateY, rotate, suggestScale);
            mFinalPose.apply(mFinalImageTransform);
        } else {
            mCurrentImageTransform.reset();
            mFinalImageTransform.reset();
            mCurrentPose.reset();
            mFinalPose.reset();
            makeFinalImageTransform(translateX, translateY, rotate, suggestScale);
            mCurrentImageTransform.set(mFinalImageTransform);
            mFinalPose.apply(mFinalImageTransform);
            mCurrentPose.apply(mCurrentImageTransform);
        }

        requestRedraw();
    }

    public void resizeFinalMatrixInRect(RectF limitRect) {
        float minLeft      = Float.POSITIVE_INFINITY;
        float maxRight     = Float.NEGATIVE_INFINITY;
        float minTop       = Float.POSITIVE_INFINITY;
        float maxBottom    = Float.NEGATIVE_INFINITY;
        float finalWidth   = 0;
        float finalHeight  = 0;
        float alignedScale = 1f;

        synchronized (sTempPolygon) {
            sTempPolygon[0] = mImageRect.left;  sTempPolygon[1] = mImageRect.top;
            sTempPolygon[2] = mImageRect.right; sTempPolygon[3] = mImageRect.top;
            sTempPolygon[4] = mImageRect.left;  sTempPolygon[5] = mImageRect.bottom;
            sTempPolygon[6] = mImageRect.right; sTempPolygon[7] = mImageRect.bottom;
            mFinalImageTransform.mapPoints(sTempPolygon);
            for (int i = 0; i < sTempPolygon.length; i += 2) {
                float x = sTempPolygon[i + 0];
                float y = sTempPolygon[i + 1];
                minLeft   = x < minLeft ? x : minLeft;
                maxRight  = x > maxRight ? x : maxRight;
                minTop    = y < minTop ? y : minTop;
                maxBottom = y > maxBottom ? y : maxBottom;
            }
        }

        finalWidth   = maxRight - minLeft;
        finalHeight  = maxBottom - minTop;
        alignedScale = ImageUtils.scaleImage(
                finalWidth, finalHeight,
                limitRect.width(), limitRect.height(),
                ImageUtils.SCALE_MODE_INSIDE);

        { // scale(alignedScale, mDisplayRect.centerX(), mDisplayRect.centerY())
            mOperationTransform.reset();
            mOperationTransform.setScale(
                    alignedScale, alignedScale,
                    limitRect.centerX(), limitRect.centerY());
            mFinalImageTransform.postConcat(mOperationTransform);
            mFinalPose.reset();
            mFinalPose.apply(mFinalImageTransform);
        }
    }

    public void zoomBack() {
        float displayWidth   = mDisplayRect.width();
        float displayHeight  = mDisplayRect.height();
        float displayCenterX = mDisplayRect.centerX();
        float displayCenterY = mDisplayRect.centerY();
        float imageWidth     = mImageRect.width();
        float imageHeight    = mImageRect.height();
        float suggestScale   = getCurrentSuggestScale();
        float translateX     = mDisplayRect.left + (displayWidth - imageWidth * suggestScale) * 0.5f;
        float translateY     = mDisplayRect.top + (displayHeight - imageHeight * suggestScale) * 0.5f;
        float rotate         = getDegree();

        mFinalImageTransform.reset();
        mFinalImageTransform.setScale(suggestScale, suggestScale);
        mFinalImageTransform.postRotate(rotate, displayCenterX, displayCenterY);
        mFinalImageTransform.postTranslate(translateX, translateY);
        mFinalPose.reset();
        mFinalPose.apply(mFinalImageTransform);

        requestRedraw();
    }

    public void setZoomInScale(float times) {
        mZoomInScaleTimes = times;
    }

    public RectF zoomIn(RectF zoomInRect) {
        float translateX   = zoomInRect.centerX() - mDisplayRect.centerX();
        float translateY   = zoomInRect.centerY() - mDisplayRect.centerY();
        float suggestScale = ImageUtils.scaleImage(
                zoomInRect.width(), zoomInRect.height(),
                mDisplayRect.width(), mDisplayRect.height(),
                ImageUtils.SCALE_MODE_INSIDE
        );

        scale(suggestScale, zoomInRect.centerX(), zoomInRect.centerY());
        scroll(translateX, translateY);

        // Compute an new clip rect area
        float displayCenterX = mDisplayRect.centerX();
        float displayCenterY = mDisplayRect.centerY();
        float outRectWidth   = zoomInRect.width() * suggestScale;
        float outRectHeight  = zoomInRect.height() * suggestScale;
        return new RectF(
                displayCenterX - outRectWidth * 0.5f,
                displayCenterY - outRectHeight * 0.5f,
                displayCenterX + outRectWidth * 0.5f,
                displayCenterY + outRectHeight * 0.5f
        );
    }

    public void zoomOut() {
        float displayCenterX = mDisplayRect.centerX();
        float displayCenterY = mDisplayRect.centerY();
        float suggestScale   = getCurrentSuggestScale() * mZoomInScaleTimes;
        float currentScale   = getScale();
        scale(suggestScale / currentScale, displayCenterX, displayCenterY);
        requestRedraw();
    }

    public void toggleZoom() {
        float currentScale  = getScale();
        float suggestScale  = getCurrentSuggestScale();
        if (Math.abs(currentScale - suggestScale) < Vector.ScaleError) {
            zoomOut();
        } else {
            zoomBack();
        }
    }

    public void scroll(float dx, float dy) {
        mOperationTransform.reset();
        mOperationTransform.setTranslate(-dx, -dy);
        mFinalImageTransform.postConcat(mOperationTransform);
        mFinalPose.apply(mFinalImageTransform);
        requestRedraw();
    }

    public float getRotate() {
        return mFinalPose.getRotate();
    }

    public float getDegree() {
        return mFinalPose.getDegree();
    }

    public void rotate(float degree, float pivotX, float pivotY) {
        mOperationTransform.reset();
        mOperationTransform.setRotate(degree, pivotX, pivotY);
        mFinalImageTransform.postConcat(mOperationTransform);
        mFinalPose.apply(mFinalImageTransform);
        requestRedraw();
    }

    public float getScale() {
        return mFinalPose.getScale();
    }

    public void scale(float scale, float pivotX, float pivotY) {
        mOperationTransform.reset();
        mOperationTransform.setScale(scale, scale, pivotX, pivotY);
        mFinalImageTransform.postConcat(mOperationTransform);
        mFinalPose.apply(mFinalImageTransform);
        requestRedraw();
    }

    public void setScale(float scale, float pivotX, float pivotY) {

    }

    public Matrix getImageTransform() {
        return mCurrentImageTransform;
    }

    public float getImageWidth() {
        return mImageRect == null ? 0 : mImageRect.width();
    }

    public float getImageHeight() {
        return mImageRect == null ? 0 : mImageRect.height();
    }

    public float getDisplayCenterX() {
        return mDisplayRect != null ? mDisplayRect.centerX() : 0;
    }

    public float getDisplayCenterY() {
        return mDisplayRect != null ? mDisplayRect.centerY() : 0;
    }

    public float getCurrentSuggestScale() {
        return getCurrentSuggestScale(mDisplayRect);
    }

    public float getCurrentSuggestScale(RectF displayRect) {
        float  minLeft      = Float.POSITIVE_INFINITY;
        float  maxRight     = Float.NEGATIVE_INFINITY;
        float  minTop       = Float.POSITIVE_INFINITY;
        float  maxBottom    = Float.NEGATIVE_INFINITY;
        Matrix matrix       = new Matrix();

        matrix.setRotate(getDegree(), displayRect.centerX(), displayRect.centerY());
        synchronized (sTempPolygon) {
            sTempPolygon[0] = mImageRect.left;  sTempPolygon[1] = mImageRect.top;
            sTempPolygon[2] = mImageRect.right; sTempPolygon[3] = mImageRect.top;
            sTempPolygon[4] = mImageRect.left;  sTempPolygon[5] = mImageRect.bottom;
            sTempPolygon[6] = mImageRect.right; sTempPolygon[7] = mImageRect.bottom;
            matrix.mapPoints(sTempPolygon);
            for (int i = 0; i < sTempPolygon.length; i += 2) {
                float x = sTempPolygon[i + 0];
                float y = sTempPolygon[i + 1];
                minLeft   = x < minLeft ? x : minLeft;
                maxRight  = x > maxRight ? x : maxRight;
                minTop    = y < minTop ? y : minTop;
                maxBottom = y > maxBottom ? y : maxBottom;
            }
        }

        return ImageUtils.scaleImage(
                maxRight - minLeft, maxBottom - minTop,
                displayRect.width(), displayRect.height(),
                ImageUtils.SCALE_MODE_INSIDE);
    }

    /**
     * Compute a pass-rate of the specified distance
     * We use [f(x) = 0.1 ^ x] as damping function.
     * The valid value range is f(x) = [1, 0.01] when x defined in [0, 2],
     * so we mapping the DAMPING_DISTANCE to the definition of the X,
     * that is said [0, DAMPING_DISTANCE] is mapped to [0, 2].
     */
    public static float computeScrollPassrate(float distance, float dampingDistance) {
        final float nearNormDistance   = 0;
        final float farNormDistance    = 2;
        final float normDistanceScalar = farNormDistance - nearNormDistance;

        float absDistance  = Math.abs(distance);
        float normDistance = (absDistance * normDistanceScalar) / dampingDistance + nearNormDistance;
        return (float) Math.pow(0.1, normDistance);
    }

    /**
     * Compute a pass-rate of the specified distance
     * We use [f(x) = 0.1 ^ x] as damping function.
     * The valid value range is f(x) = [1, 0.01] when x defined in [0, 2],
     * so we mapping the DAMPING_SCALE to the definition of the X,
     * that is said [1, 1 + DAMPING_SCALE] and [1 - DAMPING_SCALE, 1] is mapped to [0, 2].
     */
    public static float computeScalePassrate(float scale, float dampingScale) {
        final float nearNormScale   = 0;
        final float farNormScale    = 2;
        final float normScaleScalar = farNormScale - nearNormScale;

        float absScale  = scale > 1f ? scale : 1f / scale;
        float normScale = (absScale - 1) / (1 + dampingScale) * normScaleScalar + nearNormScale;
        return (float) Math.pow(0.1, normScale);
    }

    ///
    /// Internal computing
    ///

    private void makeFinalImageTransform(float translateX, float translateY, float rotate, float scale) {
        /**
         * MF = MT * MR * MS;
         */
        mFinalImageTransform.reset();
        mFinalImageTransform.setScale(scale, scale);
        mFinalImageTransform.postRotate(rotate, mDisplayRect.centerX(), mDisplayRect.centerY()); // Degree
        mFinalImageTransform.postTranslate(translateX, translateY);
    }

    private float computeSuggestScale(float imageWidth, float imageHeight, float displayWidth, float displayHeight) {
        return ImageUtils.scaleImage(
                imageWidth, imageHeight,
                displayWidth, displayHeight,
                ImageUtils.SCALE_MODE_INSIDE);
    }

    private void requestRedraw() {
        if (mOnInvalidateListener != null) {
            mOnInvalidateListener.onInvaliadate();
        }
    }
}

