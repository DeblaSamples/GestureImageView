package com.cocoonshu.cobox.gestureimageview;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Toolbar              mActionBar   = null;
    private FloatingActionButton mFabRotate   = null;
    private FloatingActionButton mFabRelayout = null;
    private FloatingActionButton mFabRevert   = null;
    private GestureImageView     mImgPicture  = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupActionBar();
        findViews();
        setupListeners();
    }

    private void setupActionBar() {
        mActionBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mActionBar);
    }

    private void findViews() {
        mFabRotate   = (FloatingActionButton) findViewById(R.id.FloatingActionButton_Rotate);
        mFabRelayout = (FloatingActionButton) findViewById(R.id.FloatingActionButton_Relayout);
        mFabRevert   = (FloatingActionButton) findViewById(R.id.FloatingActionButton_Revert);
        mImgPicture  = (GestureImageView) findViewById(R.id.GestureImageView_Picture);
    }

    private void setupListeners() {
        mFabRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mImgPicture.rotateDirection(true);
            }
        });
        mFabRelayout.setOnClickListener(new View.OnClickListener() {

            private ClipImageTask mClipTask = null;

            @Override
            public void onClick(View view) {
                if (mClipTask != null) {
                    mClipTask.cancel(true);
                }
                mClipTask = new ClipImageTask();
                mClipTask.setClipParameters(mImgPicture.getImageClipMatrix(), mImgPicture.getImageClipRect())
                        .setSrcBitmap(((BitmapDrawable)mImgPicture.getImage()).getBitmap())
                        .setOnFinishedListener(new ClipImageTask.OnFinishedListener() {
                            @Override
                            public void onSuccessed(Bitmap bitmap) {
                                BitmapDrawable image = new BitmapDrawable(bitmap);
                                mImgPicture.setImage(image);
                            }

                            @Override
                            public void onFailed() {

                            }
                        }).start();
            }

        });
        mFabRevert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mImgPicture.revert();
            }
        });
        mImgPicture.setPaletteAsyncListener(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                Palette.Swatch vibrant = palette.getVibrantSwatch();
                Palette.Swatch dark    = palette.getDarkVibrantSwatch();
                Palette.Swatch light   = palette.getLightVibrantSwatch();

                if (vibrant != null && light != null) {
                    float[] vibrantHSL   = vibrant.getHsl();
                    float[] darkHSL      = {vibrantHSL[0], vibrantHSL[1], vibrantHSL[2] < 0f ? 0f : vibrantHSL[2]};
                    float[] bgDarkHSL    = {vibrantHSL[0], vibrantHSL[1], ((vibrantHSL[2] - 0.3f) < 0f ? 0f : (vibrantHSL[2] - 0.3f))};
                    float[] lightHSL     = light.getHsl();
                    float[] accentHSL    = {1 - lightHSL[0], lightHSL[1], lightHSL[2]};
                    int     primay       = vibrant.getRgb();
                    int     darkPrimay   = Color.HSVToColor(darkHSL);
                    int     bgDarkPrimay = Color.HSVToColor(bgDarkHSL);
                    int     accentPrimay = Color.HSVToColor(accentHSL);

                    mActionBar.setBackgroundColor(primay);
                    mFabRotate.setBackgroundTintList(ColorStateList.valueOf(accentPrimay));
                    mFabRelayout.setBackgroundTintList(ColorStateList.valueOf(accentPrimay));
                    mFabRevert.setBackgroundTintList(ColorStateList.valueOf(accentPrimay));
                    getWindow().setStatusBarColor(darkPrimay);
                    getWindow().setNavigationBarColor(darkPrimay);
                    getWindow().setBackgroundDrawable(new ColorDrawable(bgDarkPrimay));
                }
            }
        });
    }

    ///
    /// Option menu setups
    ///

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {

        } else if (id == R.id.action_revert) {
            mImgPicture.revert();
        } else if (id == R.id.action_rotate) {
            mImgPicture.rotateDirection(true);
        } else if (id == R.id.action_free) {
            mImgPicture.changeClipRatio(0.0f);
        }else if (id == R.id.action_1_1) {
            mImgPicture.changeClipRatio(1.0f);
        } else if (id == R.id.action_4_3) {
            mImgPicture.changeClipRatio(4.0f / 3.0f);
        } else if (id == R.id.action_3_4) {
            mImgPicture.changeClipRatio(3.0f / 4.0f);
        } else if (id == R.id.action_16_9) {
            mImgPicture.changeClipRatio(16.0f / 9.0f);
        } else if (id == R.id.action_9_16) {
            mImgPicture.changeClipRatio(9.0f / 16.0f);
        }

        return true;
    }

    /**
     * Clip image task
     */
    private static class ClipImageTask extends AsyncTask<Bitmap, Integer, Bitmap> {

        private Bitmap             mSrcBitmap = null;
        private Matrix             mTransform = new Matrix();
        private Rect               mClipRect  = new Rect();
        private OnFinishedListener mListener  = null;


        public static interface OnFinishedListener {
            void onSuccessed(Bitmap bitmap);
            void onFailed();
        }

        public ClipImageTask() {

        }

        public ClipImageTask setSrcBitmap(Bitmap src) {
            mSrcBitmap = src;
            return this;
        }

        public ClipImageTask setClipParameters(Matrix transform, Rect clipRect) {
            mTransform.set(transform);
            mClipRect.set(clipRect);
            return this;
        }

        public ClipImageTask setOnFinishedListener(OnFinishedListener listener) {
            mListener = listener;
            return this;
        }

        public void start() {
            this.execute(mSrcBitmap);
        }

        @Override
        protected Bitmap doInBackground(Bitmap... bitmap) {
            if (isCancelled()) {
                return null;
            }
            if (mSrcBitmap == null || mClipRect.isEmpty()) {
                return null;
            } else {
                return Bitmap.createBitmap(
                        mSrcBitmap,
                        mClipRect.left, mClipRect.top,
                        mClipRect.width(), mClipRect.height(),
                        mTransform, true);
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (mListener != null && !isCancelled()) {
                if (bitmap != null) {
                    mListener.onSuccessed(bitmap);
                } else {
                    mListener.onFailed();
                }
            }
        }
    }
}
