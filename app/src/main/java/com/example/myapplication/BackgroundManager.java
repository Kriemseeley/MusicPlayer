package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;

//import com.wonderkiln.blurkit.BlurKit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 背景管理类
 * 负责处理背景图片的加载、保存和应用
 */
public class BackgroundManager {
    private static final String TAG = "BackgroundManager";
    private static final String PREFS_NAME = "BackgroundPrefs";
    private static final String KEY_BACKGROUND_PATH = "background_path";
    private static final String BACKGROUND_FILENAME = "custom_background.jpg";

    private Context context;
    private SharedPreferences prefs;

    public BackgroundManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 保存背景图片
     * 
     * @param uri 图片URI
     * @return 是否保存成功
     */
    public boolean saveBackgroundFromUri(Uri uri) {
        try {
            // 创建应用私有目录中的文件
            File backgroundFile = new File(context.getFilesDir(), BACKGROUND_FILENAME);

            // 从URI复制到文件
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return false;
            }

            FileOutputStream outputStream = new FileOutputStream(backgroundFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            // 保存路径到SharedPreferences
            prefs.edit().putString(KEY_BACKGROUND_PATH, backgroundFile.getAbsolutePath()).apply();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "保存背景图片失败", e);
            return false;
        }
    }

    /**
     * 获取保存的背景图片
     * 
     * @return 背景图片位图，如果没有则返回null
     */
    public Bitmap getBackgroundBitmap() {
        String path = prefs.getString(KEY_BACKGROUND_PATH, null);
        if (path != null) {
            File backgroundFile = new File(path);
            if (backgroundFile.exists()) {
                return BitmapFactory.decodeFile(path);
            }
        }
        return null;
    }

    /**
     * 应用毛玻璃背景效果到指定布局
     */
//    public void applyBlurredBackground(ConstraintLayout layout, float blurRadius, int overlayColor) {
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
//            // Android 12及以上使用RenderEffect
//            applyModernBlurEffect(layout, blurRadius, overlayColor);
//        } else {
//            // 低版本使用RenderScript
//            applyLegacyBlurEffect(layout, blurRadius, overlayColor);
//        }
//    }

    /**
     * 应用现代毛玻璃效果
     */
//    private void applyModernBlurEffect(ConstraintLayout layout, float blurRadius, int overlayColor) {
//        layout.setBackground(BlurKit.getInstance().blur(layout, (int) blurRadius));
//        layout.getBackground().setAlpha(255 - Color.alpha(overlayColor));
//    }

    /**
     * 应用传统毛玻璃效果（Android 12以下）
     */
    private void applyLegacyBlurEffect(ConstraintLayout layout, float blurRadius, int overlayColor) {
        Bitmap backgroundBitmap = getBackgroundBitmap();
        if (backgroundBitmap != null) {
            Bitmap blurredBitmap = BlurUtil.createBlurredBitmap(
                    context,
                    backgroundBitmap,
                    blurRadius,
                    3.0f,
                    overlayColor);

            if (blurredBitmap != null) {
                Drawable blurredDrawable = new BitmapDrawable(context.getResources(), blurredBitmap);
                layout.setBackground(blurredDrawable);
            }
        } else {
            applyDefaultFrostedBackground(layout);
        }
    }

    /**
     * 应用默认透明磨砂背景
     */
    public void applyDefaultFrostedBackground(ConstraintLayout layout) {
        GradientDrawable frostDrawable = new GradientDrawable();
        frostDrawable.setColor(Color.argb(40, 255, 255, 255)); // 降低透明度
        frostDrawable.setCornerRadius(16);
        frostDrawable.setStroke(1, Color.argb(20, 255, 255, 255)); // 更细腻的边框
        layout.setBackground(frostDrawable);
    }

    /**
     * 清除自定义背景
     */
    public void clearCustomBackground() {
        String path = prefs.getString(KEY_BACKGROUND_PATH, null);
        if (path != null) {
            File backgroundFile = new File(path);
            if (backgroundFile.exists()) {
                backgroundFile.delete();
            }
        }
        prefs.edit().remove(KEY_BACKGROUND_PATH).apply();
    }

    /**
     * 设置背景图片到ImageView
     * 
     * @param imageView 要设置背景的ImageView
     */
    public void setBackgroundToImageView(ImageView imageView) {
        Bitmap backgroundBitmap = getBackgroundBitmap();
        if (backgroundBitmap != null) {
            imageView.setImageBitmap(backgroundBitmap);
        }
    }

    /**
     * 检查是否有自定义背景
     * 
     * @return 是否有自定义背景
     */
    public boolean hasCustomBackground() {
        String path = prefs.getString(KEY_BACKGROUND_PATH, null);
        if (path != null) {
            File backgroundFile = new File(path);
            return backgroundFile.exists();
        }
        return false;
    }
}