package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.View;

/**
 * 毛玻璃效果工具类
 * 用于创建高斯模糊背景效果
 */
public class BlurUtil {

    /**
     * 创建高斯模糊背景
     * 
     * @param context      上下文
     * @param source       源图片
     * @param radius       模糊半径 (0-25)
     * @param scaleFactor  缩放因子 (值越大性能越好，但质量越低)
     * @param overlayColor 叠加的颜色 (可以用于调整亮度或添加色调)
     * @return 处理后的模糊图片
     */
    public static Bitmap createBlurredBitmap(Context context, Bitmap source, float radius, float scaleFactor,
            int overlayColor) {
        if (context == null || source == null) {
            return null;
        }

        // 缩放图片以提高性能
        int width = Math.round(source.getWidth() / scaleFactor);
        int height = Math.round(source.getHeight() / scaleFactor);

        // 创建缩放后的图片
        Bitmap inputBitmap = Bitmap.createScaledBitmap(source, width, height, false);
        // 创建输出图片
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        // 使用RenderScript进行高斯模糊处理
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);

        // 设置模糊半径 (范围: 0-25)
        theIntrinsic.setRadius(radius);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        // 添加颜色叠加层
        if (overlayColor != 0) {
            Canvas canvas = new Canvas(outputBitmap);
            Paint paint = new Paint();
            paint.setColorFilter(new PorterDuffColorFilter(overlayColor, PorterDuff.Mode.SRC_ATOP));
            canvas.drawBitmap(outputBitmap, 0, 0, paint);
        }

        // 释放资源
        rs.destroy();

        // 如果原始图片和输入图片不同，回收输入图片
        if (inputBitmap != source) {
            inputBitmap.recycle();
        }

        return outputBitmap;
    }

    /**
     * 从View创建位图
     * 
     * @param view 要转换的视图
     * @return 位图对象
     */
    public static Bitmap createBitmapFromView(View view) {
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    /**
     * 从Drawable创建位图
     * 
     * @param drawable 要转换的drawable对象
     * @return 位图对象
     */
    public static Bitmap createBitmapFromDrawable(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();

        // 确保尺寸有效
        if (width <= 0 || height <= 0) {
            width = 1;
            height = 1;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}