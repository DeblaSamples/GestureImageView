package com.cocoonshu.cobox.gestureimageview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.PaletteAsyncListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Gestured image view
 * @Author Cocoonshu
 * @Date   2016-10-24 11:15:24
 */
public class GestureImageView extends View implements GestureDetector.OnGestureListener,
                                                      ScaleRotateDetector.OnScaleRotateGestureListener,
                                                      HoverDetector.OnHoverListener,
                                                      UpDetector.OnUpListener,
                                                      GestureAnimator.OnInvalidateListener, GestureDetector.OnDoubleTapListener, GridDrawable.OnClipChangedListener {
    public static final String TAG = "GestureImageView";
    public static final int    PROGRESS_MIN_WIDTH_DP  = 50;
    public static final int    PROGRESS_MIN_HEIGHT_DP = 50;
    public static final float  DAMPING_DISTANCE_DP    = 100;
    public static final float  DAMPING_SCALE          = 0.2f;
    public static final float  DOUBLE_ZOOM_IN_SCALE   = 5f;

    private Uri                  mImageSource             = null;
    private Drawable             mImageDrawable           = null;
    private ProgressDrawable     mProgressDrawable        = null;
    private GridDrawable         mGridDrawable            = null;
    private Drawable.Callback    mDrawableCallback        = null;
    private Handler              mHandler                 = new Handler();
    private PaletteAsyncListener mPaletteAsyncListener    = null;

    private float                mLastScale               = 1f;
    private float                mLastRotate              = 0f;
    private float                mDampingDistance         = 0f;
    private GestureAnimator      mAnimator                = null;
    private UpDetector           mUpDetector              = null;
    private HoverDetector        mHoverDetector           = null;
    private GestureDetector      mGestureDetector         = null;
    private ScaleRotateDetector  mScaleRotateDetector     = null;

    private boolean              mIsUnderTouch            = false;
    private boolean              mEnabledRotateGesture    = false;
    private boolean              mEnabledScaleGesture     = true;
    private boolean              mEnabledTranslateGesture = true;

    public GestureImageView(Context context) {
        this(context, null);
    }

    public GestureImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseFromXML(context, attrs, defStyleAttr, 0);
        setupGestureDetector();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GestureImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        parseFromXML(context, attrs, defStyleAttr, defStyleRes);
        setupGestureDetector();
    }

    private void setupGestureDetector() {
        Context context = getContext();
        float   density = getResources().getDisplayMetrics().density;
        mAnimator            = new GestureAnimator();
        mUpDetector          = new UpDetector(context, this);
        mHoverDetector       = new HoverDetector(context, this);
        mGestureDetector     = new GestureDetector(context, this);
        mScaleRotateDetector = new ScaleRotateDetector(context, this);
        mDampingDistance     = DAMPING_DISTANCE_DP * density;
        mGestureDetector.setOnDoubleTapListener(this);
        mAnimator.setOnInvalidateListener(this);
        mAnimator.setZoomInScale(DOUBLE_ZOOM_IN_SCALE);
    }

    public void setPaletteAsyncListener(PaletteAsyncListener listener) {
        mPaletteAsyncListener = listener;
    }

    private void parseFromXML(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mProgressDrawable = new ProgressDrawable();
        mGridDrawable     = new GridDrawable(context);
        mDrawableCallback = new Drawable.Callback() {
            @Override
            public void invalidateDrawable(Drawable who) {
                Rect rect = who.getDirtyBounds();
                postInvalidateOnAnimation(rect.left, rect.top, rect.right, rect.bottom);
            }

            @Override
            public void scheduleDrawable(Drawable who, Runnable what, long when) {
                mHandler.postAtTime(what, when);
            }

            @Override
            public void unscheduleDrawable(Drawable who, Runnable what) {
                mHandler.removeCallbacks(what);
            }
        };
        mGridDrawable.setOnClipChangedListener(this);
        mProgressDrawable.setCallback(mDrawableCallback);
        mGridDrawable.setCallback(mDrawableCallback);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.GestureImageView, defStyleAttr, defStyleRes);
        int keyCount = array.getIndexCount();
        for (int i = 0; i < keyCount; i++) {
            int key = array.getIndex(i);
            switch (key) {
                case R.styleable.GestureImageView_image: {
                    if (mImageDrawable == null) {
                        mImageDrawable = array.getDrawable(key);
                        if (mImageDrawable == null) {
                            mImageDrawable.setBounds(
                                    0, 0,
                                    mImageDrawable.getIntrinsicWidth(),
                                    mImageDrawable.getIntrinsicHeight());
                        }
                    }
                } break;

                case R.styleable.GestureImageView_imageUrl: {
                    String url = array.getString(key);
                    if (url != null) {
                        decodeImage(url);
                    }
                } break;
            }
        }
        array.recycle();
    }

    private void decodeImage(String url) {
        if (url == null) {
            return;
        }

        mImageSource = Uri.parse(url);
        if (mImageSource != null) {
            mProgressDrawable.setEnabled(true);
            mProgressDrawable.setProgress(0);
            ImageLoader.decodeImage(getContext(), mImageSource, new ImageLoader.OnFinishedListener() {

                @Override
                public void onProgress(float progress) {
                    mProgressDrawable.setProgress(progress);
                    postInvalidate();
                }

                @Override
                public void onSuccessed(final Bitmap bitmap) {
                    BitmapDrawable image = new BitmapDrawable(bitmap);
                    image.setTargetDensity(bitmap.getDensity());
                    setImage(image);
                }

                @Override
                public void onFailed() {

                }

            });
        }
    }

    private void updatePalette(Bitmap bitmap) {
        Palette.Builder builder = Palette.from(bitmap);
        builder.generate(new Palette.PaletteAsyncListener() {

            @Override
            public void onGenerated(Palette palette) {
                if (mPaletteAsyncListener != null) {
                    mPaletteAsyncListener.onGenerated(palette);
                }
            }

        });
    }

    public final Drawable getImage() {
        return mImageDrawable;
    }

    public void setImage(Drawable image) {
        image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
        mProgressDrawable.setEnabled(false);
        if (mImageDrawable != null) {
            synchronized (mImageDrawable) {
                mImageDrawable = image;
            }
        } else {
            mImageDrawable = image;
        }

        if (image instanceof BitmapDrawable) {
            updatePalette(((BitmapDrawable) image).getBitmap());
        }

        mHandler.post(new Runnable() {

            @Override
            public void run() {
                float viewWidth         = getWidth();
                float viewHeight        = getHeight();
                float paddingLeft       = getPaddingLeft();
                float paddingRight      = getPaddingRight();
                float paddingTop        = getPaddingTop();
                float paddingBottom     = getPaddingBottom();
                float paddingHorizontal = paddingLeft + paddingRight;
                float paddingVertical   = paddingTop + paddingBottom;
                float roomWidth         = viewWidth - paddingHorizontal;
                float roomHeight        = viewHeight - paddingVertical;
                float drawableWidth     = 0;
                float drawableHeight    = 0;

                if (mImageDrawable != null) {
                    synchronized (mImageDrawable) {
                        if (mImageDrawable != null) {
                            drawableWidth = mImageDrawable.getIntrinsicWidth();
                            drawableHeight = mImageDrawable.getIntrinsicHeight();
                            mGridDrawable.setClipRect(0, 0, drawableWidth, drawableHeight);
                            mAnimator.setDisplayRect(paddingLeft, paddingTop, paddingLeft + roomWidth, paddingTop + roomHeight);
                            mAnimator.setImageRect(0, 0, drawableWidth, drawableHeight);
                            revert(false);

                            mGridDrawable.forceFinish();
                        }
                    }
                }
                requestLayout();
                invalidate();
            }

        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wantedWidth      = 0;
        int wantedHeight     = 0;
        int widthSpecMode    = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode   = MeasureSpec.getMode(heightMeasureSpec);
        int widthSpecSize    = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize   = MeasureSpec.getSize(heightMeasureSpec);
        int measuredWidth    = 0;
        int measuredHeight   = 0;

        float density   = getResources().getDisplayMetrics().density;
        int   minWidth  = (int)(density * PROGRESS_MIN_WIDTH_DP + 0.5f);
        int   minHeight = (int)(density * PROGRESS_MIN_HEIGHT_DP + 0.5f);
        mProgressDrawable.setBounds(0, 0, minWidth, minHeight);
        mGridDrawable.setBounds(0, 0, minWidth, minHeight);

        if (mImageDrawable != null) {
            synchronized (mImageDrawable) {
                wantedWidth  += mImageDrawable == null ? 0 : mImageDrawable.getIntrinsicWidth();
                wantedHeight += mImageDrawable == null ? 0 : mImageDrawable.getIntrinsicHeight();
            }
        } else {
            wantedWidth  = minWidth;
            wantedHeight = minHeight;
        }

        // Width
        switch (widthSpecMode) {
            case MeasureSpec.UNSPECIFIED:
                measuredWidth = wantedWidth;
                break;
            case MeasureSpec.AT_MOST:
                measuredWidth = wantedWidth < widthSpecSize ? wantedWidth : widthSpecSize;
                break;
            case MeasureSpec.EXACTLY:
                measuredWidth = widthSpecSize;
                break;
        }

        // Height
        switch (heightSpecMode) {
            case MeasureSpec.UNSPECIFIED:
                measuredHeight = wantedHeight;
                break;
            case MeasureSpec.AT_MOST:
                measuredHeight = wantedHeight < heightSpecSize ? wantedHeight : heightSpecSize;
                break;
            case MeasureSpec.EXACTLY:
                measuredHeight = heightSpecSize;
                break;
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean hasMoreAnimation  = false;
        float   viewWidth         = getWidth();
        float   viewHeight        = getHeight();
        float   paddingLeft       = getPaddingLeft();
        float   paddingRight      = getPaddingRight();
        float   paddingTop        = getPaddingTop();
        float   paddingBottom     = getPaddingBottom();
        float   paddingHorizontal = paddingLeft + paddingRight;
        float   paddingVertical   = paddingTop + paddingBottom;
        float   roomWidth         = viewWidth - paddingHorizontal;
        float   roomHeight        = viewHeight - paddingVertical;
        float   drawableWidth     = 0;
        float   drawableHeight    = 0;

        // Draw transformed image
        if (mImageDrawable != null) {
            synchronized (mImageDrawable) {
                if (mImageDrawable != null) {
                    drawableWidth  = mImageDrawable.getIntrinsicWidth();
                    drawableHeight = mImageDrawable.getIntrinsicHeight();
                    android.util.Log.e(TAG, "[onDraw] imageDrawable = (" + drawableWidth + ", " + drawableHeight + ")");

                    // Update animator
                    mAnimator.setDisplayRect(paddingLeft, paddingTop, paddingLeft + roomWidth, paddingTop + roomHeight);
                    hasMoreAnimation = mAnimator.compute();

                    // Draw image
                    canvas.save();
                    mImageDrawable.setBounds(0, 0, (int)drawableWidth, (int)drawableHeight);
                    canvas.concat(mAnimator.getImageTransform());
                    mImageDrawable.draw(canvas);
                    canvas.restore();
                }
            }

            // Draw background cover
            canvas.drawColor(0xCC000000);
            synchronized (mImageDrawable) {
                if (mImageDrawable != null) {
                    // Draw image
                    canvas.save();
                    canvas.clipRect(mGridDrawable.getDrawingClipRect());
                    canvas.concat(mAnimator.getImageTransform());
                    mImageDrawable.draw(canvas);
                    canvas.restore();
                }
            }

            // Draw clip grid
            mGridDrawable.setBounds(0, 0, (int) (viewWidth), (int) (viewHeight));
            mGridDrawable.setImageDrawingOutBounds(mAnimator.getDrawingOutBound());
            mGridDrawable.setClipLimitRect(mAnimator.getDisplayRect());
            mGridDrawable.setCurrentImageRotate(mAnimator.getDegree());
            mGridDrawable.setEnabled(mImageDrawable != null);
            mGridDrawable.draw(canvas);
        }

        // Draw loading progress
        Rect progressDrawableBounds = mProgressDrawable.getBounds();
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.translate((viewWidth - progressDrawableBounds.width()) * 0.5f, (viewHeight - progressDrawableBounds.height()) * 0.5f);
        mProgressDrawable.draw(canvas);
        canvas.restore();

        // Perform more animation frames
        if (hasMoreAnimation) {
            postInvalidateOnAnimation();
        }
    }

    private void scaleImageBack() {
        RectF drawingOutBound = mAnimator.getDrawingOutBound();
        RectF clipRect        = mGridDrawable.getClipRect();
        float clipRectWidth   = clipRect.width();
        float clipRectHeight  = clipRect.height();
        float outBoundWidth   = drawingOutBound.width();
        float outBoundHeight  = drawingOutBound.height();
        float centerX         = mAnimator.getDisplayCenterX();
        float centerY         = mAnimator.getDisplayCenterY();
        if (clipRectWidth > outBoundWidth
                || clipRectHeight > outBoundHeight) {
            float scaleX       = clipRectWidth / outBoundWidth;
            float scaleY       = clipRectHeight / outBoundHeight;
            float suggestScale = Math.max(1.0f, Math.max(scaleX, scaleY));
            if (suggestScale != 1f) {
                mAnimator.scale(suggestScale, centerX, centerY);
            }
        } else {
            float scaleX       = outBoundWidth / clipRectWidth;
            float scaleY       = outBoundHeight / clipRectHeight;
            float currentScale = Math.min(scaleX, scaleY);
            float suggestScale = DOUBLE_ZOOM_IN_SCALE / currentScale;
            if (currentScale > DOUBLE_ZOOM_IN_SCALE) {
                if (suggestScale < 1f) {
                    mAnimator.scale(suggestScale, centerX, centerY);
                }
            }
        }
    }

    private void scrollImageBack() {
        RectF drawingOutBound = mAnimator.getDrawingOutBound();
        RectF clipRect        = mGridDrawable.getClipRect();
        if (!drawingOutBound.contains(clipRect)) {
            float deltaX         = 0;
            float deltaY         = 0;

            // Horizontal adjusting
            float centerXOffset = drawingOutBound.centerX() - clipRect.centerX();
            if (centerXOffset > 0) {
                // Move to left
                deltaX = clipRect.left - drawingOutBound.left;
                deltaX = deltaX < 0 ? deltaX : 0;
            } else {
                // Move to right
                deltaX = clipRect.right - drawingOutBound.right;
                deltaX = deltaX > 0 ? deltaX : 0;
            }

            // Vertical adjusting
            float centerYOffset = drawingOutBound.centerY() - clipRect.centerY();
            if (centerYOffset > 0) {
                // Move to top
                deltaY = clipRect.top - drawingOutBound.top;
                deltaY = deltaY < 0 ? deltaY : 0;
            } else {
                // Move to bottom
                deltaY = clipRect.bottom - drawingOutBound.bottom;
                deltaY = deltaY > 0 ? deltaY : 0;
            }

            if (deltaX != 0 || deltaY != 0) {
                mAnimator.scroll(-deltaX, -deltaY);
            }
        }
    }

    private void dampingScroll(float distanceX, float distanceY) {
        RectF drawingOutBound = mAnimator.getDrawingOutBound();
        RectF clipRect        = mGridDrawable.getClipRect();
        if (!drawingOutBound.contains(clipRect)) {
            // Horizontal damping
            float centerXOffset      = drawingOutBound.centerX() - clipRect.centerX();
            float horizontalDistance = 0;
            float horizontalPassrate = 1f;
            if (centerXOffset > 0) {
                // Look up the left edge damping
                horizontalDistance = clipRect.left - drawingOutBound.left;
                horizontalDistance = horizontalDistance < 0 ? horizontalDistance : 0;
            } else {
                // Look up the right edge damping
                horizontalDistance = clipRect.right - drawingOutBound.right;
                horizontalDistance = horizontalDistance > 0 ? horizontalDistance : 0;
            }
            horizontalPassrate = GestureAnimator.computeScrollPassrate(horizontalDistance, mDampingDistance);
            distanceX *= horizontalPassrate;

            // Vertical damping
            float centerYOffset    = drawingOutBound.centerY() - clipRect.centerY();
            float verticalDistance = 0;
            float verticalPassrate = 1f;
            if (centerYOffset > 0) {
                // Look up the up edge damping
                verticalDistance = clipRect.top - drawingOutBound.top;
                verticalDistance = verticalDistance < 0 ? verticalDistance : 0;
            } else {
                // Look up the bottom edge damping
                verticalDistance = clipRect.bottom - drawingOutBound.bottom;
                verticalDistance = verticalDistance > 0 ? verticalDistance : 0;
            }
            verticalPassrate = GestureAnimator.computeScrollPassrate(verticalDistance, mDampingDistance);
            distanceY *= verticalPassrate;
        }

        if (distanceX != 0 || distanceY != 0) {
            mAnimator.scroll(distanceX, distanceY);
        }
    }

    private void dampingScale(float scale) {
        float displayCenterX = mAnimator.getDisplayCenterX();
        float displayCenterY = mAnimator.getDisplayCenterY();
        float currentScale   = mAnimator.getScale();
        float minScale       = mAnimator.getCurrentSuggestScale();
        float maxScale       = minScale * DOUBLE_ZOOM_IN_SCALE * 10f;
        float overScale      = 1f;
        float passrate       = 1f;
        float deltaScale     = scale;

        if (currentScale > maxScale) {
            // Over large
            // It is seems that no need to do this anymore
        } else if (currentScale < minScale) {
            // Over small
            overScale  = minScale / currentScale;
            passrate   = GestureAnimator.computeScalePassrate(overScale, DAMPING_SCALE);
            deltaScale = (scale - 1f) * passrate + 1f;
        }

        if (scale != 1f) {
            mAnimator.scale(deltaScale, displayCenterX, displayCenterY);
        }
    }

    ///
    /// Gesture detection
    ///

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean state = false;
        postInvalidateOnAnimation();
        state = mGridDrawable.onTouchEvent(event);
        if (!state) {
            return mGestureDetector.onTouchEvent(event)
                    | mScaleRotateDetector.onTouchEvent(event)
                    | mHoverDetector.onTouchEvent(event)
                    | mUpDetector.onTouchEvent(event);
        } else {
            return state;
        }
    }

    @Override
    public boolean onDown(MotionEvent event) {
        Log.i(TAG, "[onDown]");
        mIsUnderTouch = true;
        return true;
    }

    @Override
    public boolean onUp(MotionEvent event) {
        Log.i(TAG, "[onUp]");
        mIsUnderTouch = false;
        scaleImageBack();
        scrollImageBack();
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.i(TAG, "[onShowPress]");
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.i(TAG, "[onLongPress]");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.i(TAG, "[onSingleTapUp]");
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.i(TAG, "[onSingleTapConfirmed]");
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Log.i(TAG, "[onDoubleTap]");
        mAnimator.toggleZoom();
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        Log.i(TAG, "[onDoubleTapEvent]");
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent eventStart, MotionEvent eventEnd, float distanceX, float distanceY) {
        Log.i(TAG, "[onScroll] distance = (" + distanceX + ", " + distanceY + ")");
        if (mEnabledTranslateGesture) {
            dampingScroll(distanceX, distanceY);
        }
        return true;
    }

    @Override
    public boolean onFling(MotionEvent eventStart, MotionEvent eventEnd, float velocityX, float velocityY) {
        Log.i(TAG, "[onScroll] velocity = (" + velocityX + ", " + velocityY + ")");
        return true;
    }

    @Override
    public boolean onScaleRotateBegin(float scalePivotX, float scalePivotY, float angle, float rotatePivotX, float rotatePivotY, float scale, ScaleRotateDetector detector) {
        Log.i(TAG, "[onScaleRotateBegin]");
        mLastScale  = scale;
        mLastRotate = angle;
        return true;
    }

    @Override
    public boolean onScaleRotate(float scalePivotX, float scalePivotY, float angle, float rotatePivotX, float rotatePivotY, float scale, ScaleRotateDetector detector) {
        Log.i(TAG, "[onScaleRotate]");
        float displayCenterX = mAnimator.getDisplayCenterX();
        float displayCenterY = mAnimator.getDisplayCenterY();
        if (mEnabledScaleGesture) {
            dampingScale(scale / mLastScale);
        }
        if (mEnabledRotateGesture) {
            mAnimator.rotate(angle - mLastRotate, displayCenterX, displayCenterY);
        }
        mLastScale  = scale;
        mLastRotate = angle;
        return true;
    }

    @Override
    public boolean onScaleRotateEnd(float scalePivotX, float scalePivotY, float angle, float rotatePivotX, float rotatePivotY, float scale, ScaleRotateDetector detector) {
        Log.i(TAG, "[onScaleRotateEnd]");
        return true;
    }

    @Override
    public boolean onHover(float x, float y) {
        return false;
    }

    @Override
    public boolean onHoverLeave(float x, float y) {
        return false;
    }

    @Override
    public void onInvaliadate() {
        postInvalidateOnAnimation();
    }

    @Override
    public void onClipStart(GridDrawable drawable) {
        Log.i(TAG, "[onClipStart]");
    }

    @Override
    public void onClipChanging(GridDrawable drawable, RectF clipRect) {
        Log.i(TAG, "[onClipChanging]");
        scrollImageBack();
    }

    @Override
    public void onClipStop(GridDrawable drawable) {
        Log.i(TAG, "[onClipStop]");
        RectF clip = drawable.getClipRect();
        RectF dest = mAnimator.zoomIn(clip);
        drawable.zoomTo(dest);
    }

    ///
    /// External interface
    ///

    public float getRotate() {
        return (float) Math.toDegrees(mAnimator.getRotate());
    }

    public void rotate(float degree) {
        float displayCenterX = mAnimator.getDisplayCenterX();
        float displayCenterY = mAnimator.getDisplayCenterY();
        mAnimator.rotate(degree, displayCenterX, displayCenterY);
    }

    public void rotateDirection(boolean isClockWise) {
        RectF clipRect       = mGridDrawable.getClipRect();
        RectF displayRect    = mAnimator.getDisplayRect();
        float deltaScale     = 1f;
        float displayWidth   = displayRect.width();
        float displayHeight  = displayRect.height();
        float displayCenterX = displayRect.centerX();
        float displayCenterY = displayRect.centerY();
        float clipCenterX    = clipRect.centerX();
        float clipCenterY    = clipRect.centerY();
        float newClipWidth   = clipRect.height();
        float newClipHeight  = clipRect.width();

        // Rotate 90 degrees, so just exchange the width and height value
        deltaScale = ImageUtils.scaleImage(
                newClipWidth, newClipHeight,
                displayWidth, displayHeight,
                ImageUtils.SCALE_MODE_INSIDE);
        newClipWidth  *= deltaScale;
        newClipHeight *= deltaScale;

        mGridDrawable.setClipRect(
                clipCenterX - newClipWidth * 0.5f,
                clipCenterY - newClipHeight * 0.5f,
                clipCenterX + newClipWidth * 0.5f,
                clipCenterY + newClipHeight * 0.5f
        );
        mAnimator.rotate(isClockWise ? 90 : -90, displayCenterX, displayCenterY);
        mAnimator.scale(deltaScale, displayCenterX, displayCenterY);
    }

    protected void revert(boolean withAnimation) {
        int viewWidth  = getWidth() - getPaddingLeft() - getPaddingRight();
        int viewHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        mAnimator.revert(withAnimation);
        mGridDrawable.setBounds(0, 0, viewWidth, viewHeight);
        mGridDrawable.setImageDrawingOutBounds(mAnimator.getDrawingOutBound());
        mGridDrawable.setClipLimitRect(mAnimator.getDrawingOutBound());
        mGridDrawable.resizeToLimitRect();
    }

    public void revert() {
        revert(true);
    }


    public void changeClipRatio(float ratio) {
        mGridDrawable.setFrameRatio(ratio);
    }

    public final Matrix getImageClipMatrix() {
        return new Matrix(mAnimator.getImageTransform());
    }

    public final RectF getImageClipRect() {
        RectF imageClipRect = mGridDrawable.getClipRect();
        return mAnimator.getImageClipRect(imageClipRect);
    }
}
