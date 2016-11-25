package com.cocoonshu.cobox.graphic;

import android.graphics.Matrix;

/**
 * 3 dimensions homogeneous vector
 * @author Cocoonshu
 * @date 2016-10-26 16:04:11
 */
public class Vector {

    public  static final float   EqualError     = 1E-3F;
    public  static final float   ScaleError     = 1E-3F;
    private static final float   ForwardFactor  = 2E-1F;
    private static final int     PolygonCount   = 3;
    private static final float[] OrignalPolygon = new float[] {0, 0, 0, 1, 1, 0};

    private float[] mDirection = new float[3];
    private float[] mPosition  = new float[3];
    private float[] mPolygon   = new float[6];

    public Vector() {
        reset();
    }

    public void reset() {
        mDirection[0] = 0f;
        mDirection[1] = 1f;
        mDirection[2] = 0f;

        mPosition[0]  = 0f;
        mPosition[1]  = 0f;
        mPosition[2]  = 1f;

        mPolygon[0]   = 0f; // Ox
        mPolygon[1]   = 0f; // Oy
        mPolygon[2]   = 0f; // Tx
        mPolygon[3]   = 1f; // Ty
        mPolygon[4]   = 1f; // Rx
        mPolygon[5]   = 0f; // Ry
    }

    public final void set(Vector src) {
        mDirection[0] = src.mDirection[0];
        mDirection[1] = src.mDirection[1];
        mDirection[2] = src.mDirection[2];

        mPosition[0]  = src.mPosition[0];
        mPosition[1]  = src.mPosition[1];
        mPosition[2]  = src.mPosition[2];

        mPolygon[0]   = src.mPolygon[0]; // Ox
        mPolygon[1]   = src.mPolygon[1]; // Oy
        mPolygon[2]   = src.mPolygon[2]; // Tx
        mPolygon[3]   = src.mPolygon[3]; // Ty
        mPolygon[4]   = src.mPolygon[4]; // Rx
        mPolygon[5]   = src.mPolygon[5]; // Ry
    }

    public final void setX(float x) {
        mPosition[0] = x;
    }

    public final void setY(float y) {
        mPosition[0] = y;
    }

    public final void set(float x, float y) {
        mPosition[0] = x;
        mPosition[1] = y;
    }

    public final float getX() {
        return mPosition[0];
    }

    public final float getY() {
        return mPosition[1];
    }

    public final float getLength() {
        return (float)Math.sqrt(mDirection[0] * mDirection[0] + mDirection[1] * mDirection[1]);
    }

    public final float getScale() {
        return getLength();
    }

    public final float getTranslateX() {
        return mPosition[0];
    }

    public final float getTranslateY() {
        return mPosition[1];
    }

    public final float getRotate() {
        return (float) (Math.atan2(mDirection[1], mDirection[0]) - Math.PI * 0.5);
    }

    public final float getDegree() {
        return (float) Math.toDegrees(Math.atan2(mDirection[1], mDirection[0]) - Math.PI * 0.5);
    }

    public final void apply(Matrix mat) {
        mat.mapVectors(mDirection);
        mat.mapPoints(mPosition);
        mat.mapPoints(mPolygon);
    }

    public final boolean affine(Matrix mat) {
        return mat.setPolyToPoly(OrignalPolygon, 0, mPolygon, 0, PolygonCount);
    }

    public final boolean forward(Vector dest) {
        if (dest == null) {
            return false;
        }

        boolean allElemEqual = true;
        for (int i = 0; i < PolygonCount * 2; i++) {
            mPolygon[i] = mPolygon[i] + (dest.mPolygon[i] - mPolygon[i]) * ForwardFactor;
            allElemEqual &= Math.abs(dest.mPolygon[i] - mPolygon[i]) < EqualError;
        }
        return !allElemEqual;
    }

    public boolean isPolygonValid() {
        boolean hasInvalidElement = false;
        for (int i = 0; i < PolygonCount * 2; i++) {
            hasInvalidElement |= Float.isInfinite(mPolygon[i]) || Float.isNaN(mPolygon[i]);
        }
        return !hasInvalidElement;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector) {
            float[] otherPolygon = ((Vector) obj).mPolygon;
            boolean allElemEqual = true;
            for (int i = 0; i < PolygonCount * 2; i++) {
                allElemEqual &= Math.abs(mPolygon[i] - otherPolygon[i]) < EqualError;
            }
            return allElemEqual;
        } else {
            return super.equals(obj);
        }
    }

}

