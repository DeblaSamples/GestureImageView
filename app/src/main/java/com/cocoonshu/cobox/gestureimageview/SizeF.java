package com.cocoonshu.cobox.gestureimageview;

/**
 * Size
 * @author Cocoonshu
 * @date 2016-11-02 11:22:31
 */
public class SizeF {
    public float width  = 0;
    public float height = 0;

    public void set(float viewWidth, float viewHeight) {
        width = viewWidth;
        height = viewHeight;
    }

    public final float centerX() {
        return width * 0.5f;
    }

    public final float centerY() {
        return height * 0.5f;
    }
}
