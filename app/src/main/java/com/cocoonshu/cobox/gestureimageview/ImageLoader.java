package com.cocoonshu.cobox.gestureimageview;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

import static android.support.v7.widget.StaggeredGridLayoutManager.TAG;

/**
 * Gestured image view
 * @Author Cocoonshu
 * @Date   2016-10-24 11:15:24
 */
public class ImageLoader {

    public static final String TAG                = "ImageLoader";
    public static final int    LIMIT_IMAGE_WIDTH  = 1440;
    public static final int    LIMIT_IMAGE_HEIGHT = 2560;

    private static ImageLoader     sImageLoader = null;
    private        ExecutorService mExecutor    = null;

    public interface OnFinishedListener {
        void onProgress(float progress);
        void onSuccessed(Bitmap bitmap);
        void onFailed();
    }

    private ImageLoader() {
        mExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactory() {

                    private int mThreadCounter = 0;
                    private Thread.UncaughtExceptionHandler mMainHanlder = new Thread.UncaughtExceptionHandler() {
                        @Override
                        public void uncaughtException(Thread thread, Throwable ex) {
                            Log.e(TAG, "[uncaughtException] " + thread.getName(), ex);
                        }
                    };

                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable, "ImageLoaderThread #" + mThreadCounter++);
                        thread.setUncaughtExceptionHandler(mMainHanlder);
                        return thread;
                    }

                });
    }

    private static ImageLoader getImageLoader() {
        if (sImageLoader == null) {
            sImageLoader = new ImageLoader();
        }
        return sImageLoader;
    }

    public static final Future<?> decodeImage(final Context context, final Uri uri, OnFinishedListener listener) {
        final Context            resContext   = context;
        final Uri                imageURI     = uri;
        final OnFinishedListener taskListener = listener;
        return getImageLoader().mExecutor.submit(new FutureTask<Bitmap>(new Callable<Bitmap>() {

            @Override
            public Bitmap call() throws Exception {
                String scheme = imageURI.getScheme();
                Bitmap bitmap = null;
                try {
                    if (scheme.equalsIgnoreCase("http")
                            || scheme.equalsIgnoreCase("https")) {
                        bitmap = getBitmapFromNetwork(context, uri, taskListener);
                    } else if (scheme.equalsIgnoreCase("content")) {
                        bitmap = getBitmapFromContentProvider(context, uri);
                    } else if (scheme.equalsIgnoreCase("assets")) {
                        bitmap = getBitmapFromAsset(context, uri);
                    } else if (scheme.equalsIgnoreCase("file")
                            || scheme.equalsIgnoreCase("")) {
                        bitmap = getBitmapFromFile(context, uri);
                    }
                } catch (Throwable thr) {
                    thr.printStackTrace();
                }

                if (taskListener != null) {
                    if (bitmap == null) {
                        taskListener.onFailed();
                    } else {
                        taskListener.onSuccessed(bitmap);
                    }
                }
                return bitmap;
            }

        }));
    }

    private static Bitmap getBitmapFromNetwork(Context context, Uri uri, OnFinishedListener listener) throws IOException {
        URL           url         = new URL(uri.toString());
        URLConnection connection  = url.openConnection();
        InputStream   inputStream = connection.getInputStream();
        Options       options     = new Options();
        Bitmap        bitmap      = null;

        // Cache to file
        Calendar calendar  = Calendar.getInstance();
        File     cacheDir  = context.getExternalCacheDir();
        File     cacheFile = new File(
                cacheDir.getPath() + String.format(
                        "/%04d-%02d-%02d_%02d:%02d:%02d:%04d",
                        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
                        calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), calendar.get(Calendar.MILLISECOND)
                ));
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        if (cacheFile.createNewFile()) {
            FileOutputStream fout = new FileOutputStream(cacheFile);
            try {
                byte[] buffer     = new byte[1024];
                int    readCount  = 0;
                int    writeCount = 0;
                int    totalCount = connection.getContentLength();
                do {
                    readCount = inputStream.read(buffer);
                    if (readCount > 0) {
                        fout.write(buffer, 0, readCount);
                        writeCount += readCount;
                        if (listener != null) {
                            listener.onProgress((float) writeCount / (float) totalCount);
                        }
                    }
                } while (readCount >= 0);
            } finally {
                fout.flush();
                fout.close();
            }
        }

        try {
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath(), options);
        } catch (OutOfMemoryError errPrev) {
            try {
                options.inSampleSize = ImageUtils.adviseSamplaSize(
                        options.outWidth, options.outHeight,
                        LIMIT_IMAGE_WIDTH, LIMIT_IMAGE_HEIGHT
                );
                options.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath(), options);
            } catch (OutOfMemoryError errNext) {
                bitmap = null;
                cacheFile.delete();
            }
        } catch (Throwable thr) {
            thr.printStackTrace();
            cacheFile.delete();
        }

        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException exp) {}
        cacheFile.delete();
        return bitmap;
    }

    private static Bitmap getBitmapFromContentProvider(Context context, Uri uri) throws IOException {
        ContentResolver      resolver       = context.getContentResolver();
        ParcelFileDescriptor fileDescriptor = resolver.openFileDescriptor(uri, "r");
        Options              options        = new Options();
        Rect                 outRect        = null;
        Bitmap               result         = null;

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
        if (outRect == null) {
            outRect = new Rect(0, 0, options.outWidth, options.outHeight);
        }

        int limitWidth = outRect.width();
        int limitHeight = outRect.height();
        int advisedSampleSize = ImageUtils.adviseSamplaSize(
                options.outWidth, options.outHeight,
                limitWidth, limitHeight);
        float scale = ImageUtils.adviseImageScale(
                options.outWidth, options.outHeight,
                limitWidth, limitHeight);

        options.inJustDecodeBounds = false;
        options.inSampleSize = advisedSampleSize;
        options.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888;
        result = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
        fileDescriptor.close();

        int scaledWidth = (int) (options.outWidth * scale);
        int scaledHeight = (int) (options.outHeight * scale);
        Bitmap sampledBitmap = result;
        if (scaledWidth != options.outWidth || scaledHeight != options.outHeight) {
            if (scaledWidth > limitWidth || scaledHeight > limitHeight) {
                result = Bitmap.createScaledBitmap(sampledBitmap,
                        scaledWidth, scaledHeight, true);
                sampledBitmap.recycle();
                sampledBitmap = null;
            }
        }

        return result;
    }

    private static Bitmap getBitmapFromAsset(Context context, Uri uri) {
        return null;
    }

    private static Bitmap getBitmapFromFile(Context context, Uri uri) {
        return null;
    }

}
