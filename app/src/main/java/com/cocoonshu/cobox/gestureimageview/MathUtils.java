package com.cocoonshu.cobox.gestureimageview;

import android.graphics.RectF;

/**
 * Math relative utility tools
 * @author Cocoonshu
 * @date 2016-10-26 15:03:19
 */
public class MathUtils {

    public static final float PI_2       = (float) (Math.PI * 2.0);
    public static final float PI         = (float) Math.PI;
    public static final float PI_1_2     = (float) (Math.PI * 0.5);
    public static final float PI_1_4     = (float) (Math.PI * 0.25);
    public static final float RATIO_4_3  = (float) 4.0f / 3.0f;
    public static final float RATIO_16_9 = (float) 16.0f / 9.0f;
    public static final float RATIO_1_1  = (float) 1.0f;
    public static final float RATIO_FREE = (float) 0.0f;

    /**
     * Clamp value between <tt>min</tt> and <tt>max</tt>
     * @param src
     * @param min
     * @param max
     * @return
     */
    public static float clamp(float src, float min, float max) {
        return src < min ? min : src > max ? max : src;
    }

    // Returns the input value x clamped to the range [min, max].
    public static int clamp(int x, int min, int max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    /**
     * Compute an intersection point what is perpendicularcross with the specified line
     * @param lineSlope the slope of the specified line
     * @param pointX a point outside of the specified line
     * @param pointY a point outside of the specified line
     * @return
     */
    public static float[] perpendicularIntersectionPoint(
            float onlinePointAX, float onlinePointAY,
            float onlinePointBX, float onlinePointBY,
            float offlinePointX, float offlinePointY) {
        float distX = onlinePointBX - onlinePointAX;
        float distY = onlinePointBY - onlinePointAY;

        if (distX == 0f && distY == 0f) {
            return null;
        } else if (distY == 0f) {
            return new float[] {offlinePointX, onlinePointAY};
        } else if (distX == 0f) {
            return new float[] {onlinePointAX, offlinePointY};
        } else {
            float lineSlope     = distY / distX;
            float line          = onlinePointAY - lineSlope * onlinePointAX;
            float perpendicular = offlinePointX + lineSlope * offlinePointY;
            float intersectionX = (perpendicular - lineSlope * line) / (lineSlope * lineSlope + 1f);
            float intersectionY = lineSlope * intersectionX + line;
            return new float[] {intersectionX, intersectionY};
        }
    }

    /**
     * Compare if 2 RectF are almost equal tolerent of error less than precision
     * @param object1
     * @param object2
     * @param precision
     * @return true if 2 RectF are almost equal, otherwise return false
     */
    public static boolean equalApproximately(RectF object1, RectF object2, float precision){
        if(null == object1 || null == object2){
            return true;
        }
        if(Math.abs(object1.left - object2.left) < precision &&
                Math.abs(object1.right - object2.right) < precision &&
                Math.abs(object1.top - object2.top) < precision &&
                Math.abs(object1.bottom - object2.bottom) < precision){
            return true;
        }
        return false;
    }

}
