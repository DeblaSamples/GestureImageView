package com.cocoonshu.cobox.gestureimageview;

import android.graphics.Rect;
import android.graphics.RectF;

/**
 * Image relative utility tools
 * @author Cocoonshu
 * @date 2014-12-12 12:01:26
 */
public class ImageUtils {

    public static final int SCALE_MODE_FILL   = 0x0001;
    public static final int SCALE_MODE_INSIDE = 0x0002;

    /**
     * detect image nine-patch attribute by giving image file name
     * @param imageFileName
     * @return
     */
    public static boolean isNinePatchImage(String imageFileName) {
        if (imageFileName == null || imageFileName.isEmpty()) {
            return false;
        }

        return imageFileName.lastIndexOf(".9.") != -1;
    }

    /**
     * Give a advised sample size to setup Bitmap Decoder for giving size
     * @param srcWidth
     * @param srcHeight
     * @param destWidth
     * @param destHeight
     * @return
     */
    public static int adviseSamplaSize(int srcWidth, int srcHeight, int destWidth, int destHeight) {
        float widthScale = (float) srcWidth / (float) destWidth;
        float heightScale = (float) srcHeight / (float) destHeight;
        float referenceScale = 1f;

        referenceScale = Math.min(widthScale, heightScale);
        float referenceScale2 = Math.min(
                referenceScale,
                Math.max((float) srcWidth / (float) destHeight, (float) srcHeight
                        / (float) destWidth));
        // if widthScale < 1 or heightScale < 1,
        // we do not downsample bitmap.
        if (referenceScale2 < 1f) {
            return 1;
        }

        return (int) Math.floor(referenceScale2);
    }

    /**
     * Give a advised scale factor to setup Bitmap Decoder for giving size
     * @param srcWidth
     * @param srcHeight
     * @param destWidth
     * @param destHeight
     * @return
     */
    public static float adviseImageScale(int srcWidth, int srcHeight, int destWidth, int destHeight) {
        float widthScale = (float) destWidth / (float) srcWidth;
        float heightScale = (float) destHeight / (float) srcHeight;
        float referenceScale = Math.min(widthScale, heightScale);
        float referenceScale2 = Math.max(
                referenceScale,
                Math.min((float) destWidth / (float) srcHeight, (float) destHeight
                        / (float) srcHeight));
        return referenceScale2;
    }

    /**
     * Give a fit scale to fit limited scale
     * @param srcWidth
     * @param srcHeight
     * @param limitWidth
     * @param limitHeight
     * @return
     */
    public static float fitLimitScale(float srcWidth, float srcHeight, float limitWidth, float limitHeight) {
        float srcRatio = srcWidth / srcHeight;
        float limitRatio = limitWidth / limitHeight;

        if (srcRatio > limitRatio) {
            // limit height
            return limitHeight / srcHeight;
        } else {
            // limit width
            return limitWidth / srcWidth;
        }
    }

    /**
     * Give a advised scale rectangle to setup Bitmap Decoder for giving size
     * @param srcWidth
     * @param srcHeight
     * @param destWidth
     * @param destHeight
     * @return
     */
    public static Rect adviseImageScaleRect(int srcWidth, int srcHeight, int destWidth, int destHeight) {
        float widthScale = destWidth / srcWidth;
        float heightScale = destHeight / srcHeight;
        float referenceScale = Math.min(widthScale, heightScale);
        return new Rect(0, 0, (int) (srcWidth * referenceScale), (int) (srcHeight * referenceScale));
    }

    /**
     * Compute a scale value to fit image to limit size
     * @param srcWidth
     * @param srcHeight
     * @param limitWidth
     * @param limitHeight
     * @return
     */
    public static float computeFitScale(float srcWidth, float srcHeight, float limitWidth, float limitHeight) {
        float scale = 1f;
        float srcRatio = srcWidth / srcHeight;
        float limitRatio = limitWidth / limitHeight;

        if (srcRatio > limitRatio) {
            // fit Height
            scale = limitHeight / srcHeight;
        } else {
            // fit Width
            scale = limitWidth / srcWidth;
        }

        return scale;
    }

    /**
     * Compute a scale value to fit image to limit size
     * @param srcWidth
     * @param srcHeight
     * @param limitWidth
     * @param limitHeight
     * @return
     */
    public static double computeFitScale(double srcWidth, double srcHeight, double limitWidth, double limitHeight) {
        double scale = 1f;
        double srcRatio = srcWidth / srcHeight;
        double limitRatio = limitWidth / limitHeight;

        if (srcRatio > limitRatio) {
            // fit Height
            scale = limitHeight / srcHeight;
        } else {
            // fit Width
            scale = limitWidth / srcWidth;
        }

        return scale;
    }

    /**
     * Compute a scale value to let all image inside the limit size
     * @param srcWidth
     * @param srcHeight
     * @param limitWidth
     * @param limitHeight
     * @return fitable image scale factor
     */
    private static float computeInsideScale(float srcWidth, float srcHeight, float limitWidth, float limitHeight) {
        float scale = 1f;
        float srcRatio = srcWidth / srcHeight;
        float limitRatio = limitWidth / limitHeight;

        if (srcRatio > limitRatio) {
            // fit Width
            scale = limitWidth / srcWidth;
        } else {
            // fit Height
            scale = limitHeight / srcHeight;
        }

        return scale;
    }

    /**
     * Compute a scale value to let all image inside the limit size
     * @param srcWidth
     * @param srcHeight
     * @param limitWidth
     * @param limitHeight
     * @return fitable image scale factor
     */
    private static double computeInsideScale(double srcWidth, double srcHeight, double limitWidth, double limitHeight) {
        double scale = 1f;
        double srcRatio = srcWidth / srcHeight;
        double limitRatio = limitWidth / limitHeight;

        if (srcRatio > limitRatio) {
            // fit Width
            scale = limitWidth / srcWidth;
        } else {
            // fit Height
            scale = limitHeight / srcHeight;
        }

        return scale;
    }

    /**
     * Compute scale factor with specified method
     * @param imageWidth
     * @param imageHeight
     * @param limitWidth
     * @param limitHeight
     * @param mode SCALE_MODE_FILL to fill full of limit size
     *             SCALE_MODE_INSIDE to scale image fit limit size
     * @return scale factor
     */
    public static float scaleImage(float imageWidth, float imageHeight, float limitWidth, float limitHeight, int mode) {
        float scaleFactor = 1f;
        switch(mode) {
        case SCALE_MODE_FILL:
            scaleFactor = computeFitScale(imageWidth, imageHeight, limitWidth, limitHeight);
            break;
        case SCALE_MODE_INSIDE:
            scaleFactor = computeInsideScale(imageWidth, imageHeight, limitWidth, limitHeight);
            break;
        }
        return scaleFactor;
    }

    /**
     * Compute scale factor with specified method
     * @param imageWidth
     * @param imageHeight
     * @param limitWidth
     * @param limitHeight
     * @param mode SCALE_MODE_FILL to fill full of limit size
     *             SCALE_MODE_INSIDE to scale image fit limit size
     * @return scale factor
     */
    public static double scaleImage(double imageWidth, double imageHeight, double limitWidth, double limitHeight, int mode) {
        double scaleFactor = 1f;
        switch(mode) {
        case SCALE_MODE_FILL:
            scaleFactor = computeFitScale(imageWidth, imageHeight, limitWidth, limitHeight);
            break;
        case SCALE_MODE_INSIDE:
            scaleFactor = computeInsideScale(imageWidth, imageHeight, limitWidth, limitHeight);
            break;
        }
        return scaleFactor;
    }

    public static boolean isEmptyRect(float left, float top, float right, float bottom) {
        return Float.isNaN(left) || Float.isNaN(right)
                || Float.isNaN(top) || Float.isNaN(bottom);
    }

    public static boolean isEmptyRect(Rect rect) {
        return rect == null || rect.isEmpty();
    }

    public static boolean isEmptyRect(RectF rect) {
        return rect == null || rect.isEmpty()
                || Float.isNaN(rect.left) || Float.isNaN(rect.right)
                || Float.isNaN(rect.top) || Float.isNaN(rect.bottom);
    }

    public static int mixAlphaToColor(int color, int alpha) {
        int colorAlpha = color >>> 24;
        int destAlpha  = (int)(((float)colorAlpha / 255f) * ((float)alpha / 255f) * 255f);
        return (destAlpha << 24)
             | (color & 0x00FFFFFF);
    }

}
