package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 动态模糊背景视图
 * 接收一个源Bitmap，异步处理成模糊背景并平滑过渡
 */
public class BlurBackgroundView extends ImageView {

    private static final int TRANSITION_DURATION_MS = 300;
    private static final float BLUR_RADIUS = 5f;
    private static final float SCALE_FACTOR = 1.2f;

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private RenderScript rs;

    private final Drawable defaultBackground;

    public BlurBackgroundView(@NonNull Context context) {
        this(context, null);
    }

    public BlurBackgroundView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        rs = RenderScript.create(context);
        // 无封面时的默认渐变背景
        defaultBackground = new ColorDrawable(Color.parseColor("#404040"));
        setScaleType(ScaleType.CENTER_CROP);
        setImageDrawable(defaultBackground);
    }

    /**
     * 设置源封面，触发异步模糊处理和背景更新
     * 
     * @param coverBitmap 原始封面图，可为null
     */
    public void setCover(Bitmap coverBitmap) {
        if (coverBitmap == null) {
            transitionTo(defaultBackground);
            return;
        }

        backgroundExecutor.submit(() -> {
            try {
                Bitmap blurredBitmap = processBitmap(coverBitmap);
                mainHandler.post(() -> transitionTo(
                        new android.graphics.drawable.BitmapDrawable(getResources(), blurredBitmap)));
            } catch (Exception e) {
                mainHandler.post(() -> transitionTo(defaultBackground));
            }
        });
    }

    /**
     * 对Bitmap进行缩放和高斯模糊处理
     */
    private Bitmap processBitmap(Bitmap bitmap) {
        // 1. 缩放
        Matrix matrix = new Matrix();
        matrix.postScale(SCALE_FACTOR, SCALE_FACTOR);
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // 2. 模糊
        Bitmap outputBitmap = Bitmap.createBitmap(scaledBitmap);
        Allocation in = Allocation.createFromBitmap(rs, scaledBitmap);
        Allocation out = Allocation.createFromBitmap(rs, outputBitmap);

        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius(BLUR_RADIUS);
        script.setInput(in);
        script.forEach(out);

        out.copyTo(outputBitmap);

        // 回收中间Bitmap
        scaledBitmap.recycle();
        in.destroy();
        out.destroy();
        script.destroy();

        return outputBitmap;
    }

    /**
     * 平滑过渡到新的Drawable
     */
    private void transitionTo(Drawable newDrawable) {
        Drawable oldDrawable = getDrawable();
        if (oldDrawable == null) {
            oldDrawable = new ColorDrawable(Color.TRANSPARENT);
        }

        TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[] { oldDrawable, newDrawable });
        setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(TRANSITION_DURATION_MS);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 释放RenderScript资源
        if (rs != null) {
            rs.destroy();
            rs = null;
        }
        // 关闭线程池
        backgroundExecutor.shutdown();
    }
}