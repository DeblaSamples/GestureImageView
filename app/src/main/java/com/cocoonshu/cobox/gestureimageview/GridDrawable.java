package com.cocoonshu.cobox.gestureimageview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

import com.cocoonshu.cobox.animation.Smoother;
import com.cocoonshu.cobox.animation.Smoother4;
import com.cocoonshu.cobox.graphic.SizeF;
import com.cocoonshu.cobox.utils.ImageUtils;
import com.cocoonshu.cobox.utils.MathUtils;

/**
 * Grid frame drawable
 * @author Cocoonshu
 * @date 2015-07-31 15:40:36
 */
public class GridDrawable extends Drawable {

    private static final int          WIDTH_COUNT                  = 3;
    private static final int          HEIGHT_COUNT                 = 3;
    private static final int          FRAME_CORNER_LINE_HEIGHT_DP  = 16;
    private static final int          FRAME_LINE_WIDTH_DP          = 2;
    private static final int          FRAME_GRID_LINE_WIDTH_DP     = 1;
    private static final int          SHOW_ALPHA                   = 255;
    private static final int          HIDE_ALPHA                   = 000;
    private static final float        FREE_RATIO                   = 0f;

    private static final int          FRAME_LINE_COLOR             = 0x4DFFFFFF;
    private static final int          FRAME_CORNER_LINE_HINT_COLOR = 0xFF3FCAB8;
    private static final int          FRAME_CORNER_LINE_COLOR      = 0xFFFFFFFF;

    private static final int          MOTION_ACTION_NONE          = 0x0000; // 0b0000;
    private static final int          MOTION_ACTION_LEFT          = 0x0001; // 0b0001;
    private static final int          MOTION_ACTION_TOP           = 0x0002; // 0b0010;
    private static final int          MOTION_ACTION_RIGHT         = 0x0004; // 0b0100;
    private static final int          MOTION_ACTION_BOTTOM        = 0x0008; // 0b1000;
    private static final int          MOTION_ACTION_LEFT_TOP      = 0x0003; // 0b0011;
    private static final int          MOTION_ACTION_TOP_RIGHT     = 0x0006; // 0b0110;
    private static final int          MOTION_ACTION_RIGHT_BOTTOM  = 0x000C; // 0b1100;
    private static final int          MOTION_ACTION_BOTTOM_LEFT   = 0x0009; // 0b1001;
    private static final int          MOTION_ACTION_MOVE          = 0x000F; // 0b1111;

    private Context                   mContext                    = null;
    private Handler                   mHandler                    = new Handler(Looper.getMainLooper());
    private int                       mWidthCount                 = WIDTH_COUNT;
    private int                       mHeightCount                = HEIGHT_COUNT;
    private int                       mGridLineCount              = (mWidthCount - 1) * (mHeightCount - 1) * 4;
    private int                       mFrameLineCount             = 2 * 2 * 4;
    private int                       mCornerLineCount            = (mWidthCount + mHeightCount) * 2 * 2 * 2;
    private int                       mAlpha                      = 0xFF;
    private float[]                   mGridLines                  = null;
    private float[]                   mFrameLines                 = null;
    private float[]                   mCornerLines                = null;
    private Smoother mAlphaAnimator              = null;
    private Smoother                  mGridAlphaAnimator          = null;
    private Paint                     mFrameLinePaint             = new Paint();
    private Paint                     mGridLinePaint              = new Paint();
    private Paint                     mCornerLinePaint            = new Paint();
    private RectF                     mClipRect                   = new RectF();
    private RectF                     mClipLimitRect              = new RectF();
    private Rect                      mDrawingClipRect            = new Rect();
    private RectF                     mDrawingOutBounds           = new RectF();
    private PointF                    mTransformedPoint           = new PointF();
    private SizeF mMinClipSize                = new SizeF();
    private boolean                   mHasEatTouchEvent           = false;
    private boolean                   mIsActived                  = false;
    private boolean                   mIsEnabled                  = true;
    private float                     mClipRatio                  = FREE_RATIO;
    private float                     mCurrentRotate              = 0;
    private int                       mMotionActions              = MOTION_ACTION_NONE;
    private PointF                    mLastMotionPoint            = new PointF();
    private Smoother4 mClipSmoother               = null;
    private OnClipChangedListener     mOnClipChangedListener      = null;

    public static interface OnClipChangedListener {
        void onClipStart(GridDrawable drawable);
        void onClipChanging(GridDrawable drawable, RectF clipRect);
        void onClipStop(GridDrawable drawable);
    }

    public GridDrawable(Context context) {
        mContext           = context;
        mGridLines         = new float[mGridLineCount];
        mFrameLines        = new float[mFrameLineCount];
        mCornerLines       = new float[mCornerLineCount];
        mAlphaAnimator     = new Smoother(2E-1F, 1E-3F);
        mGridAlphaAnimator = new Smoother(2E-1F, 1E-3F);
        mClipSmoother      = new Smoother4(2E-1F, 1E-3F);
        mAlphaAnimator.setDestinationValue(SHOW_ALPHA);
        mAlphaAnimator.forceFinish();
    }

    public void setOnClipChangedListener(OnClipChangedListener listener) {
        mOnClipChangedListener = listener;
    }

    public Rect getDrawingClipRect() {
        return mDrawingClipRect;
    }

    void updateGridLines() {
        float density                 = mContext.getResources().getDisplayMetrics().density;
        float acturalCornerLineHeight = FRAME_CORNER_LINE_HEIGHT_DP * density;
        float acturalLineWidth        = FRAME_LINE_WIDTH_DP * density;
        float acturalGridLineWidth    = FRAME_GRID_LINE_WIDTH_DP * density;
        float halfLineWidth           = acturalLineWidth * 0.5f;
        mFrameLinePaint.setStrokeWidth(acturalLineWidth);
        mGridLinePaint.setStrokeWidth(acturalGridLineWidth);
        mCornerLinePaint.setStrokeWidth(acturalLineWidth);

        mMinClipSize.set(
                acturalCornerLineHeight * 3f + acturalLineWidth,
                acturalCornerLineHeight * 3f + acturalLineWidth);

        float clipLeft    = mClipSmoother.getCurrentLeft() + halfLineWidth;
        float clipRight   = mClipSmoother.getCurrentRight() - halfLineWidth;
        float clipTop     = mClipSmoother.getCurrentTop() + halfLineWidth;
        float clipBottom  = mClipSmoother.getCurrentBottom() - halfLineWidth;
        float unitWidth   = (clipRight - clipLeft) / mWidthCount;
        float unitHeight  = (clipBottom - clipTop) / mHeightCount;
        int   pointOffset = 0;

        mDrawingClipRect.set(
                (int) (clipLeft - halfLineWidth), (int) (clipTop - halfLineWidth),
                (int) (clipRight + halfLineWidth), (int) (clipBottom + halfLineWidth));

        // Column frame line
        for (int width = 0; width < 2; width++) {
            float top     = clipTop - halfLineWidth;
            float bottom  = clipBottom + halfLineWidth;
            float xOffset = (clipRight - clipLeft) * width + clipLeft;
            mFrameLines[4 * width + 0] = xOffset;
            mFrameLines[4 * width + 1] = top;
            mFrameLines[4 * width + 2] = xOffset;
            mFrameLines[4 * width + 3] = bottom;
        }
        // Row frame line
        pointOffset = 2 * 4;
        for (int height = 0; height < 2; height++) {
            float left    = clipLeft - halfLineWidth;
            float right   = clipRight + halfLineWidth;
            float yOffset = (clipBottom - clipTop) * height + clipTop;
            mFrameLines[4 * height + 0 + pointOffset] = left;
            mFrameLines[4 * height + 1 + pointOffset] = yOffset;
            mFrameLines[4 * height + 2 + pointOffset] = right;
            mFrameLines[4 * height + 3 + pointOffset] = yOffset;
        }
        // Column grid line
        pointOffset = 0;
        for (int width = 1; width < mWidthCount; width++) {
            halfLineWidth = acturalLineWidth * 0.5f;
            float top     = clipTop + halfLineWidth;
            float bottom  = clipBottom - halfLineWidth;
            float xOffset = unitWidth * width + clipLeft;
            mGridLines[4 * (width - 1) + 0] = xOffset;
            mGridLines[4 * (width - 1) + 1] = top;
            mGridLines[4 * (width - 1) + 2] = xOffset;
            mGridLines[4 * (width - 1) + 3] = bottom;
        }
        // Row grid line
        pointOffset = (mWidthCount - 1) * (mHeightCount - 1) * 2;
        for (int height = 1; height < mHeightCount; height++) {
            halfLineWidth = acturalLineWidth * 0.5f;
            float left    = clipLeft + halfLineWidth;
            float right   = clipRight - halfLineWidth;
            float yOffset = unitHeight * height + clipTop;
            mGridLines[4 * (height - 1) + 0 + pointOffset] = left;
            mGridLines[4 * (height - 1) + 1 + pointOffset] = yOffset;
            mGridLines[4 * (height - 1) + 2 + pointOffset] = right;
            mGridLines[4 * (height - 1) + 3 + pointOffset] = yOffset;
        }
        // Corner column line
        int lineOffset = 0;
        for (int width = 0; width < 2; width++) {
            float xOffset = clipLeft + width * (clipRight - clipLeft);
            for (int section = 0; section < mHeightCount; section++) {
                if (section == 0) {
                    float top    = clipTop - halfLineWidth;
                    float bottom = clipTop + acturalCornerLineHeight + halfLineWidth;
                    mCornerLines[lineOffset * 4 + 0] = xOffset;
                    mCornerLines[lineOffset * 4 + 1] = top;
                    mCornerLines[lineOffset * 4 + 2] = xOffset;
                    mCornerLines[lineOffset * 4 + 3] = bottom;
                    lineOffset++;
                } else if (section == mHeightCount - 1) {
                    float top    = clipBottom - acturalCornerLineHeight - halfLineWidth;
                    float bottom = clipBottom + halfLineWidth;
                    mCornerLines[lineOffset * 4 + 0] = xOffset;
                    mCornerLines[lineOffset * 4 + 1] = top;
                    mCornerLines[lineOffset * 4 + 2] = xOffset;
                    mCornerLines[lineOffset * 4 + 3] = bottom;
                    lineOffset++;
                } else {
                    float center = clipTop + section * (clipBottom - clipTop) / (mHeightCount - 1);
                    float top    = center - acturalCornerLineHeight * 0.5f;
                    float bottom = center + acturalCornerLineHeight * 0.5f;
                    mCornerLines[lineOffset * 4 + 0] = xOffset;
                    mCornerLines[lineOffset * 4 + 1] = top;
                    mCornerLines[lineOffset * 4 + 2] = xOffset;
                    mCornerLines[lineOffset * 4 + 3] = bottom;
                    lineOffset++;
                }
            }
        }
        // Corner row line
        for (int height = 0; height < 2; height++) {
            float yOffset = clipTop + height * (clipBottom - clipTop);
            for (int section = 0; section < mWidthCount; section++) {
                if (section == 0) {
                    float left  = clipLeft - halfLineWidth;
                    float right = clipLeft + acturalCornerLineHeight + halfLineWidth;
                    mCornerLines[lineOffset * 4 + 0] = left;
                    mCornerLines[lineOffset * 4 + 1] = yOffset;
                    mCornerLines[lineOffset * 4 + 2] = right;
                    mCornerLines[lineOffset * 4 + 3] = yOffset;
                    lineOffset++;
                } else if (section == mWidthCount - 1) {
                    float left  = clipRight - acturalCornerLineHeight - halfLineWidth;
                    float right = clipRight + halfLineWidth;
                    mCornerLines[lineOffset * 4 + 0] = left;
                    mCornerLines[lineOffset * 4 + 1] = yOffset;
                    mCornerLines[lineOffset * 4 + 2] = right;
                    mCornerLines[lineOffset * 4 + 3] = yOffset;
                    lineOffset++;
                } else {
                    float center = clipLeft + section * (clipRight - clipLeft) / (mWidthCount - 1);
                    float left   = center - acturalCornerLineHeight * 0.5f;
                    float right  = center + acturalCornerLineHeight * 0.5f;
                    mCornerLines[lineOffset * 4 + 0] = left;
                    mCornerLines[lineOffset * 4 + 1] = yOffset;
                    mCornerLines[lineOffset * 4 + 2] = right;
                    mCornerLines[lineOffset * 4 + 3] = yOffset;
                    lineOffset++;
                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        boolean hasMoreFrames = false;
        hasMoreFrames |= mAlphaAnimator.smooth();
        hasMoreFrames |= mGridAlphaAnimator.smooth();
        hasMoreFrames |= mClipSmoother.smooth();
        if (hasMoreFrames) {
            invalidateSelf();
        }

        updateGridLines();

        int currentFrameAlpha = (int) mAlphaAnimator.getCurrentValue();
        int currentGridAlpha  = (int) mGridAlphaAnimator.getCurrentValue();
        int frameLineAlpha    = ImageUtils.mixAlphaToColor(FRAME_LINE_COLOR, currentFrameAlpha);
        int gridLineAlpha     = ImageUtils.mixAlphaToColor(frameLineAlpha, currentGridAlpha);
        mFrameLinePaint.setColor(frameLineAlpha);
        mGridLinePaint.setColor(gridLineAlpha);
        mCornerLinePaint.setColor(ImageUtils.mixAlphaToColor(
                mIsActived ? FRAME_CORNER_LINE_HINT_COLOR : FRAME_CORNER_LINE_COLOR, currentFrameAlpha));

        if (mIsEnabled) {
            canvas.drawLines(mGridLines, 0, mGridLines.length, mGridLinePaint);
            canvas.drawLines(mFrameLines, 0, mFrameLines.length, mFrameLinePaint);
            canvas.drawLines(mCornerLines, 0, mCornerLines.length, mCornerLinePaint);
        }
    }

    public void setClipRect(float left, float top, float right, float bottom) {
        mClipRect.set(left, top, right, bottom);
        mClipSmoother.setDestinationValue(left, top, right, bottom);
    }

    public float getClipRatio() {
        return mClipRatio;
    }

    public float getClipRectRatio() {
        return mClipRect.isEmpty() ? 0 : mClipRect.width() / mClipRect.height();
    }

    public RectF getClipRect() {
        return mClipRect;
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mCornerLinePaint.setColorFilter(colorFilter);
        mFrameLinePaint.setColorFilter(colorFilter);
        mGridLinePaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    public void showGrid() {
        mGridAlphaAnimator.setDestinationValue(SHOW_ALPHA);
        invalidateSelf();
    }

    public void hideGrid() {
        mGridAlphaAnimator.setDestinationValue(HIDE_ALPHA);
        invalidateSelf();
    }

    public void setCurrentImageRotate(float degree) {
        mCurrentRotate = degree;
    }

    public void setClipLimitRect(RectF clipLimitRect) {
        if (clipLimitRect != null) {
            mClipLimitRect.set(clipLimitRect);
        }
    }

    public void setImageDrawingOutBounds(RectF outBounds) {
        if (outBounds != null) {
            mDrawingOutBounds.set(outBounds);
        }
    }

    public void zoomTo(RectF dest) {
        mClipSmoother.setDestinationValue(dest.left, dest.top, dest.right, dest.bottom);
        setClipRect(dest.left, dest.top, dest.right, dest.bottom);
        invalidateSelf();
    }

    public void resizeToLimitRect() {
        float left   = mDrawingOutBounds.left < mClipLimitRect.left ? mClipLimitRect.left : mDrawingOutBounds.left;
        float right  = mDrawingOutBounds.right > mClipLimitRect.right ? mClipLimitRect.right : mDrawingOutBounds.right;
        float top    = mDrawingOutBounds.top < mClipLimitRect.top ? mClipLimitRect.top : mDrawingOutBounds.top;
        float bottom = mDrawingOutBounds.bottom > mClipLimitRect.bottom ? mClipLimitRect.bottom : mDrawingOutBounds.bottom;
        mClipRect.set(left, top, right, bottom);
        mClipSmoother.setDestinationValue(left, top, right, bottom);
    }

    public void forceFinish() {
        mClipSmoother.forceFinish();
    }

    private final boolean detectMotionAction(float x, float y) {
        if (mClipRect == null) {
            return false;
        }

        float   currentClipWidth        = mClipRect.width();
        float   currentClipHeight       = mClipRect.height();
        int     verticalAction          = MOTION_ACTION_NONE;
        int     horizontalAction        = MOTION_ACTION_NONE;
        float   density                 = mContext.getResources().getDisplayMetrics().density;
        float   acturalCornerLineHeight = FRAME_CORNER_LINE_HEIGHT_DP * density;
        float   cornerBlockWidth        = acturalCornerLineHeight;
        float   cornerBlockHeight       = acturalCornerLineHeight;
        float   touchLeftBorder         = mClipRect.left - cornerBlockWidth;
        float   touchRightBorder        = mClipRect.right + cornerBlockWidth;
        float   touchTopBorder          = mClipRect.top - cornerBlockHeight;
        float   touchBottomBorder       = mClipRect.bottom + cornerBlockHeight;
        float   moveLeftBorder          = mClipRect.left + cornerBlockWidth;
        float   moveRightBorder         = mClipRect.right - cornerBlockWidth;
        float   moveTopBorder           = mClipRect.top + cornerBlockHeight;
        float   moveBottomBorder        = mClipRect.bottom - cornerBlockHeight;

        mTransformedPoint.set(x, y);
        if (mTransformedPoint.x >= touchLeftBorder && mTransformedPoint.x <= moveLeftBorder) {
            verticalAction = MOTION_ACTION_LEFT;
        } else if (mTransformedPoint.x >= moveRightBorder && mTransformedPoint.x <= touchRightBorder) {
            verticalAction = MOTION_ACTION_RIGHT;
        } else {
            verticalAction = MOTION_ACTION_NONE;
        }
        if (mTransformedPoint.y >= touchTopBorder && mTransformedPoint.y <= moveTopBorder) {
            horizontalAction = MOTION_ACTION_TOP;
        } else if (mTransformedPoint.y >= moveBottomBorder && mTransformedPoint.y <= touchBottomBorder) {
            horizontalAction = MOTION_ACTION_BOTTOM;
        } else {
            horizontalAction = MOTION_ACTION_NONE;
        }

        mMotionActions = verticalAction | horizontalAction;
        return mMotionActions != MOTION_ACTION_NONE;
    }

    private final void changeClipFrame(float distanceX, float distanceY) {
        if (mClipRect == null) {
            return;
        }

        float density                 = mContext.getResources().getDisplayMetrics().density;
        float acturalCornerLineHeight = FRAME_CORNER_LINE_HEIGHT_DP * density;
        float horizontalSpacing       = 3 * acturalCornerLineHeight;
        float verticalSpacing         = 3 * acturalCornerLineHeight;
        float destRotate              = mCurrentRotate;
        float currentRatio            = (destRotate % 180f == 0f) ? mClipRatio : 1f / mClipRatio;
        RectF drawingRect             = mDrawingOutBounds;

        if (mClipRatio == FREE_RATIO) {
            // Limit with limition rectangle under free size ratio
            if ((mMotionActions & MOTION_ACTION_LEFT) == MOTION_ACTION_LEFT) {
                mClipRect.left -= distanceX;
                mClipRect.left = MathUtils.clamp(mClipRect.left, mClipLimitRect.left, mClipRect.right - horizontalSpacing);
            }
            if ((mMotionActions & MOTION_ACTION_RIGHT) == MOTION_ACTION_RIGHT) {
                mClipRect.right -= distanceX;
                mClipRect.right = MathUtils.clamp(mClipRect.right, mClipRect.left + horizontalSpacing, mClipLimitRect.right);
            }
            if ((mMotionActions & MOTION_ACTION_TOP) == MOTION_ACTION_TOP) {
                mClipRect.top -= distanceY;
                mClipRect.top = MathUtils.clamp(mClipRect.top, mClipLimitRect.top, mClipRect.bottom - verticalSpacing);
            }
            if ((mMotionActions & MOTION_ACTION_BOTTOM) == MOTION_ACTION_BOTTOM) {
                mClipRect.bottom -= distanceY;
                mClipRect.bottom = MathUtils.clamp(mClipRect.bottom, mClipRect.top + verticalSpacing, mClipLimitRect.bottom);
            }
        } else {
            // Limit under fixed size ratio
            if ((mMotionActions & MOTION_ACTION_LEFT_TOP) == MOTION_ACTION_LEFT_TOP) {
                float[] intersection = MathUtils.perpendicularIntersectionPoint(
                        mClipRect.right, mClipRect.bottom, mClipRect.left, mClipRect.top,
                        mClipRect.left - distanceX, mClipRect.top - distanceY);
                if (intersection != null) {
                    mClipRect.left = intersection[0];
                    mClipRect.top  = intersection[1];

                    RectF drawingArea = mDrawingOutBounds;
                    float roomLeft    = mClipRect.left < drawingArea.left ? drawingArea.left : mClipRect.left;
                    float roomRight   = mClipRect.right > drawingArea.right ? drawingArea.right : mClipRect.right;
                    float roomTop     = mClipRect.top < drawingArea.top ? drawingArea.top : mClipRect.top;
                    float roomBottom  = mClipRect.bottom > drawingArea.bottom ? drawingArea.bottom : mClipRect.bottom;
                    float roomWidth   = roomRight - roomLeft;
                    float roomHeight  = roomBottom - roomTop;
                    float roomScale   = ImageUtils.scaleImage(currentRatio, 1.0f, roomWidth, roomHeight, ImageUtils.SCALE_MODE_INSIDE);
                    float minWidth    = currentRatio > 1f ? verticalSpacing * mClipRatio : horizontalSpacing;
                    float minHeight   = currentRatio > 1f ? verticalSpacing : horizontalSpacing * mClipRatio;
                    float maxWidth    = currentRatio * roomScale;
                    float maxHeight   = 1.0f * roomScale;
                    float clipWidth   = mClipRect.width();
                    float clipHeight  = mClipRect.height();

                    if (clipWidth > maxWidth || clipHeight > maxHeight) {
                        mClipRect.left = mClipRect.right - maxWidth;
                        mClipRect.top  = mClipRect.bottom - maxHeight;
                    }
                    if (clipWidth < minWidth || clipHeight < minHeight) {
                        mClipRect.left = mClipRect.right - minWidth;
                        mClipRect.top  = mClipRect.bottom - minHeight;
                    }
                }

            } else if ((mMotionActions & MOTION_ACTION_BOTTOM_LEFT) == MOTION_ACTION_BOTTOM_LEFT) {
                float[] intersection = MathUtils.perpendicularIntersectionPoint(
                        mClipRect.right, mClipRect.top, mClipRect.left, mClipRect.bottom,
                        mClipRect.left - distanceX, mClipRect.bottom - distanceY);
                if (intersection != null) {
                    mClipRect.left   = intersection[0];
                    mClipRect.bottom = intersection[1];

                    RectF drawingArea = mDrawingOutBounds;
                    float roomLeft    = mClipRect.left < drawingArea.left ? drawingArea.left : mClipRect.left;
                    float roomRight   = mClipRect.right > drawingArea.right ? drawingArea.right : mClipRect.right;
                    float roomTop     = mClipRect.top < drawingArea.top ? drawingArea.top : mClipRect.top;
                    float roomBottom  = mClipRect.bottom > drawingArea.bottom ? drawingArea.bottom : mClipRect.bottom;
                    float roomWidth   = roomRight - roomLeft;
                    float roomHeight  = roomBottom - roomTop;
                    float roomScale   = ImageUtils.scaleImage(currentRatio, 1.0f, roomWidth, roomHeight, ImageUtils.SCALE_MODE_INSIDE);
                    float minWidth    = currentRatio > 1f ? verticalSpacing * mClipRatio : horizontalSpacing;
                    float minHeight   = currentRatio > 1f ? verticalSpacing : horizontalSpacing * mClipRatio;
                    float maxWidth    = currentRatio * roomScale;
                    float maxHeight   = 1.0f * roomScale;
                    float clipWidth   = mClipRect.width();
                    float clipHeight  = mClipRect.height();

                    if (clipWidth > maxWidth || clipHeight > maxHeight) {
                        mClipRect.left   = mClipRect.right - maxWidth;
                        mClipRect.bottom = mClipRect.top + maxHeight;
                    }
                    if (clipWidth < minWidth || clipHeight < minHeight) {
                        mClipRect.left   = mClipRect.right - minWidth;
                        mClipRect.bottom = mClipRect.top + minHeight;
                    }
                }

            } else if ((mMotionActions & MOTION_ACTION_RIGHT_BOTTOM) == MOTION_ACTION_RIGHT_BOTTOM) {
                float[] intersection = MathUtils.perpendicularIntersectionPoint(
                        mClipRect.left, mClipRect.top, mClipRect.right, mClipRect.bottom,
                        mClipRect.right - distanceX, mClipRect.bottom - distanceY);
                if (intersection != null) {
                    mClipRect.right  = intersection[0];
                    mClipRect.bottom = intersection[1];

                    RectF drawingArea = mDrawingOutBounds;
                    float roomLeft    = mClipRect.left < drawingArea.left ? drawingArea.left : mClipRect.left;
                    float roomRight   = mClipRect.right > drawingArea.right ? drawingArea.right : mClipRect.right;
                    float roomTop     = mClipRect.top < drawingArea.top ? drawingArea.top : mClipRect.top;
                    float roomBottom  = mClipRect.bottom > drawingArea.bottom ? drawingArea.bottom : mClipRect.bottom;
                    float roomWidth   = roomRight - roomLeft;
                    float roomHeight  = roomBottom - roomTop;
                    float roomScale   = ImageUtils.scaleImage(currentRatio, 1.0f, roomWidth, roomHeight, ImageUtils.SCALE_MODE_INSIDE);
                    float minWidth    = currentRatio > 1f ? verticalSpacing * mClipRatio : horizontalSpacing;
                    float minHeight   = currentRatio > 1f ? verticalSpacing : horizontalSpacing * mClipRatio;
                    float maxWidth    = currentRatio * roomScale;
                    float maxHeight   = 1.0f * roomScale;
                    float clipWidth   = mClipRect.width();
                    float clipHeight  = mClipRect.height();

                    if (clipWidth > maxWidth || clipHeight > maxHeight) {
                        mClipRect.right  = mClipRect.left + maxWidth;
                        mClipRect.bottom = mClipRect.top + maxHeight;
                    }
                    if (clipWidth < minWidth || clipHeight < minHeight) {
                        mClipRect.right  = mClipRect.left + minWidth;
                        mClipRect.bottom = mClipRect.top + minHeight;
                    }
                }

            } else if ((mMotionActions & MOTION_ACTION_TOP_RIGHT) == MOTION_ACTION_TOP_RIGHT) {
                float[] intersection = MathUtils.perpendicularIntersectionPoint(
                        mClipRect.left, mClipRect.bottom, mClipRect.right, mClipRect.top,
                        mClipRect.right - distanceX, mClipRect.top - distanceY);
                if (intersection != null) {
                    mClipRect.right = intersection[0];
                    mClipRect.top   = intersection[1];

                    RectF drawingArea = mDrawingOutBounds;
                    float roomLeft    = mClipRect.left < drawingArea.left ? drawingArea.left : mClipRect.left;
                    float roomRight   = mClipRect.right > drawingArea.right ? drawingArea.right : mClipRect.right;
                    float roomTop     = mClipRect.top < drawingArea.top ? drawingArea.top : mClipRect.top;
                    float roomBottom  = mClipRect.bottom > drawingArea.bottom ? drawingArea.bottom : mClipRect.bottom;
                    float roomWidth   = roomRight - roomLeft;
                    float roomHeight  = roomBottom - roomTop;
                    float roomScale   = ImageUtils.scaleImage(currentRatio, 1.0f, roomWidth, roomHeight, ImageUtils.SCALE_MODE_INSIDE);
                    float minWidth    = currentRatio > 1f ? verticalSpacing * mClipRatio : horizontalSpacing;
                    float minHeight   = currentRatio > 1f ? verticalSpacing : horizontalSpacing * mClipRatio;
                    float maxWidth    = currentRatio * roomScale;
                    float maxHeight   = 1.0f * roomScale;
                    float clipWidth   = mClipRect.width();
                    float clipHeight  = mClipRect.height();

                    if (clipWidth > maxWidth || clipHeight > maxHeight) {
                        mClipRect.right = mClipRect.left + maxWidth;
                        mClipRect.top   = mClipRect.bottom - maxHeight;
                    }
                    if (clipWidth < minWidth || clipHeight < minHeight) {
                        mClipRect.right = mClipRect.left + minWidth;
                        mClipRect.top   = mClipRect.bottom - minHeight;
                    }
                }

            } else {
                if ((mMotionActions & MOTION_ACTION_LEFT) == MOTION_ACTION_LEFT) {
                    mClipRect.left -= distanceX;
                    mClipRect.left  = MathUtils.clamp(mClipRect.left, mClipLimitRect.left, mClipRect.right - horizontalSpacing);

                    float exceptWidth  = mClipRect.width();
                    float exceptHeight = exceptWidth / currentRatio;
                    float limitWidth   = Math.min(Math.min(getBounds().width(), mClipLimitRect.width()), drawingRect.width());
                    float limitHeight  = Math.min(Math.min(getBounds().height(), mClipLimitRect.height()), drawingRect.height());
                    float adjustScale  = ImageUtils.scaleImage(exceptWidth, exceptHeight, limitWidth, limitHeight, ImageUtils.SCALE_MODE_INSIDE);
                    float finalWidth   = exceptWidth * (adjustScale > 1f ? 1f : adjustScale);
                    float finalHeight  = exceptHeight * (adjustScale > 1f ? 1f : adjustScale);
                    float deltaWidth   = finalWidth - mClipRect.width();
                    float deltaHeight  = finalHeight - mClipRect.height();

                    if (exceptHeight > finalHeight) {
                        mClipRect.right -= distanceX;
                    } else if (exceptHeight < verticalSpacing) {
                        mClipRect.left += distanceX;
                    } else {
                        mClipRect.top    -= deltaHeight * 0.5f;
                        mClipRect.bottom += deltaHeight * 0.5f;
                    }
                } else if ((mMotionActions & MOTION_ACTION_RIGHT) == MOTION_ACTION_RIGHT) {
                    mClipRect.right -= distanceX;
                    mClipRect.right  = MathUtils.clamp(mClipRect.right, mClipRect.left + horizontalSpacing, mClipLimitRect.right);

                    float exceptWidth  = mClipRect.width();
                    float exceptHeight = exceptWidth / currentRatio;
                    float limitWidth   = Math.min(Math.min(getBounds().width(), mClipLimitRect.width()), drawingRect.width());
                    float limitHeight  = Math.min(Math.min(getBounds().height(), mClipLimitRect.height()), drawingRect.height());
                    float adjustScale  = ImageUtils.scaleImage(exceptWidth, exceptHeight, limitWidth, limitHeight, ImageUtils.SCALE_MODE_INSIDE);
                    float finalWidth   = exceptWidth * (adjustScale > 1f ? 1f : adjustScale);
                    float finalHeight  = exceptHeight * (adjustScale > 1f ? 1f : adjustScale);
                    float deltaWidth   = finalWidth - mClipRect.width();
                    float deltaHeight  = finalHeight - mClipRect.height();

                    if (exceptHeight > finalHeight) {
                        mClipRect.left -= distanceX;
                    } else if (exceptHeight < verticalSpacing) {
                        mClipRect.right += distanceX;
                    } else {
                        mClipRect.top    -= deltaHeight * 0.5f;
                        mClipRect.bottom += deltaHeight * 0.5f;
                    }
                } else if ((mMotionActions & MOTION_ACTION_TOP) == MOTION_ACTION_TOP) {
                    mClipRect.top -= distanceY;
                    mClipRect.top = MathUtils.clamp(mClipRect.top, mClipLimitRect.top, mClipRect.bottom - verticalSpacing);

                    float exceptHeight = mClipRect.height();
                    float exceptWidth  = exceptHeight * currentRatio;
                    float limitWidth   = Math.min(Math.min(getBounds().width(), mClipLimitRect.width()), drawingRect.width());
                    float limitHeight  = Math.min(Math.min(getBounds().height(), mClipLimitRect.height()), drawingRect.height());
                    float adjustScale  = ImageUtils.scaleImage(exceptWidth, exceptHeight, limitWidth, limitHeight, ImageUtils.SCALE_MODE_INSIDE);
                    float finalWidth   = exceptWidth * (adjustScale > 1f ? 1f : adjustScale);
                    float finalHeight  = exceptHeight * (adjustScale > 1f ? 1f : adjustScale);
                    float deltaWidth   = finalWidth - mClipRect.width();
                    float deltaHeight  = finalHeight - mClipRect.height();

                    if (exceptWidth > finalWidth) {
                        mClipRect.bottom -= distanceY;
                    } else if (exceptWidth < horizontalSpacing) {
                        mClipRect.top += distanceY;
                    } else {
                        mClipRect.left  -= deltaWidth * 0.5f;
                        mClipRect.right += deltaWidth * 0.5f;
                    }
                } else if ((mMotionActions & MOTION_ACTION_BOTTOM) == MOTION_ACTION_BOTTOM) {
                    mClipRect.bottom -= distanceY;
                    mClipRect.bottom  = MathUtils.clamp(mClipRect.bottom, mClipRect.top + verticalSpacing, mClipLimitRect.bottom);

                    float exceptHeight = mClipRect.height();
                    float exceptWidth  = exceptHeight * currentRatio;
                    float limitWidth   = Math.min(Math.min(getBounds().width(), mClipLimitRect.width()), drawingRect.width());
                    float limitHeight  = Math.min(Math.min(getBounds().height(), mClipLimitRect.height()), drawingRect.height());
                    float adjustScale  = ImageUtils.scaleImage(exceptWidth, exceptHeight, limitWidth, limitHeight, ImageUtils.SCALE_MODE_INSIDE);
                    float finalWidth   = exceptWidth * (adjustScale > 1f ? 1f : adjustScale);
                    float finalHeight  = exceptHeight * (adjustScale > 1f ? 1f : adjustScale);
                    float deltaWidth   = finalWidth - mClipRect.width();
                    float deltaHeight  = finalHeight - mClipRect.height();

                    if (exceptWidth > finalWidth) {
                        mClipRect.top -= distanceY;
                    } else if (exceptWidth < horizontalSpacing) {
                        mClipRect.bottom += distanceY;
                    } else {
                        mClipRect.left  -= deltaWidth * 0.5f;
                        mClipRect.right += deltaWidth * 0.5f;
                    }
                }
            }
        }

        if (drawingRect.width() < mClipRect.width()) {
            mClipRect.left  = drawingRect.left;
            mClipRect.right = drawingRect.right;
        }
        if (drawingRect.height() < mClipRect.height()) {
            mClipRect.top    = drawingRect.top;
            mClipRect.bottom = drawingRect.bottom;
        }
        mClipSmoother.setDestinationValue(mClipRect.left, mClipRect.top, mClipRect.right, mClipRect.bottom);
        mClipSmoother.forceFinish();
    }

    public final void setFrameRatio(float ratio) {
        if (ratio == FREE_RATIO) {
            mClipRatio = ratio;
            return;
        } else {
            mClipRatio = ratio;
        }

        float currentRotate       = mCurrentRotate % 180;
        float clipLimitRectWidth  = mClipLimitRect.width();
        float clipLimitRectHeight = mClipLimitRect.height();
        float clipViewRectWidth   = getBounds().width();
        float clipViewRectHeight  = getBounds().height();
        float clipRectCenterX     = mClipRect.centerX();
        float clopRectCenterY     = mClipRect.centerY();
        RectF drawingOutBound     = mDrawingOutBounds;
        float drawingRectWidth    = drawingOutBound.width();
        float drawingRectHeight   = drawingOutBound.height();
        float scaleFactor         = 1f;
        float drawingCacheScale   = 1f;
        float destLimitWidth      = 0f;
        float destLimitHeight     = 0f;
        float destWidth           = 0f;
        float destHeight          = 0f;
        float destHalfWidth       = 0f;
        float destHalfHeight      = 0f;
        float destClipLeft        = 0f;
        float destClipRight       = 0f;
        float destClipTop         = 0f;
        float destClipBottom      = 0f;
        float minClipWidth        = 0f;
        float minClipHeight       = 0f;
        float clipRatio           = mClipRect.width() / mClipRect.height();

        if (currentRotate == 0) {
            if (ratio < clipRatio) {
                destWidth  = mClipRect.width();
                destHeight = destWidth / ratio;
            } else {
                destHeight = mClipRect.height();
                destWidth  = destHeight * ratio;
            }
            scaleFactor   = ImageUtils.scaleImage(
                    destWidth, destHeight,
                    drawingRectWidth, drawingRectHeight,
                    ImageUtils.SCALE_MODE_INSIDE);
            scaleFactor   = scaleFactor > 1f ? 1f : scaleFactor;
            destWidth     = destWidth * scaleFactor;
            destHeight    = destHeight * scaleFactor;
            minClipWidth  = mClipRatio * mMinClipSize.height;
            minClipHeight = mMinClipSize.height;
            if (destWidth < minClipWidth || destHeight < minClipHeight) {
                destWidth  = minClipWidth;
                destHeight = minClipHeight;
            }
        } else {
            ratio = 1f / ratio;
            if (ratio > clipRatio) {
                destHeight = mClipRect.height();
                destWidth  = destHeight * ratio;
            } else {
                destWidth  = mClipRect.width();
                destHeight = destWidth / ratio;
            }
            scaleFactor   = ImageUtils.scaleImage(
                    destWidth, destHeight,
                    drawingRectWidth, drawingRectHeight,
                    ImageUtils.SCALE_MODE_INSIDE);
            scaleFactor   = scaleFactor > 1f ? 1f : scaleFactor;
            destWidth     = destWidth * scaleFactor;
            destHeight    = destHeight * scaleFactor;
            minClipWidth  = mMinClipSize.width;
            minClipHeight = mClipRatio * mMinClipSize.width;
            if (destWidth < minClipWidth || destHeight < minClipHeight) {
                destWidth  = minClipWidth;
                destHeight = minClipHeight;
            }
        }

        destHalfWidth  = destWidth * 0.5f;
        destHalfHeight = destHeight * 0.5f;
        destClipLeft   = clipRectCenterX - destHalfWidth;
        destClipRight  = clipRectCenterX + destHalfWidth;
        destClipTop    = clopRectCenterY - destHalfHeight;
        destClipBottom = clopRectCenterY + destHalfHeight;
        setClipRect(destClipLeft, destClipTop, destClipRight, destClipBottom);

        // Invoke animator to resize ClipFrame
        if (mOnClipChangedListener != null) {
            mOnClipChangedListener.onClipStart(GridDrawable.this);
            mOnClipChangedListener.onClipStop(GridDrawable.this);
        }
        invalidateSelf();
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean state = false;
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                state = detectMotionAction(event.getX(), event.getY());
                mHasEatTouchEvent = state;
                mIsActived = mHasEatTouchEvent;
                mLastMotionPoint.set(x, y);
                if (mHasEatTouchEvent) {
                    showGrid();
                    if (mOnClipChangedListener != null) {
                        mOnClipChangedListener.onClipStart(GridDrawable.this);
                    }
                }
            } break;

            case MotionEvent.ACTION_MOVE: {
                if (mHasEatTouchEvent) {
                    float distanceX = mLastMotionPoint.x - x;
                    float distanceY = mLastMotionPoint.y - y;
                    changeClipFrame(distanceX, distanceY);
                    mLastMotionPoint.set(x, y);
                    if (mOnClipChangedListener != null) {
                        mOnClipChangedListener.onClipChanging(GridDrawable.this, getClipRect());
                    }
                }
            } break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                mIsActived = false;
                if (mHasEatTouchEvent) {
                    hideGrid();
                    if (mOnClipChangedListener != null) {
                        mOnClipChangedListener.onClipChanging(GridDrawable.this, getClipRect());
                        mOnClipChangedListener.onClipStop(GridDrawable.this);
                    }
                }
            } break;
        }
        return mHasEatTouchEvent;
    }

}
