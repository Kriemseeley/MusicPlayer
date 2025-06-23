package com.example.myapplication;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 音频可视化自定义视图
 * 实现实时频谱柱形图显示，支持颜色渐变和动画效果
 * 
 * 特性：
 * - 实时频谱显示
 * - 颜色渐变效果
 * - 平滑动画过渡
 * - 性能优化
 * - 支持背景播放
 */
public class AudioVisualizerView extends View {

    private static final String TAG = "AudioVisualizerView";

    // 常量定义
    private static final int DEFAULT_BAR_COUNT = 32;
    private static final int DEFAULT_BAR_WIDTH = 12;
    private static final int DEFAULT_BAR_SPACING = 4;
    private static final float DEFAULT_AMPLITUDE_SCALE = 1.3f; // 适中的振幅缩放
    private static final int ANIMATION_DURATION = 50; // 减少动画时长
    private static final float MIN_BAR_HEIGHT = 8f;
    private static final boolean FAST_MODE = true; // 快速响应模式

    // 绘制相关
    private Paint mBarPaint;
    private Paint mReflectionPaint;
    private LinearGradient mGradient;
    private LinearGradient mReflectionGradient;
    private Rect mCanvasRect;

    // 数据相关
    private byte[] mRawAudioBytes;
    private float[] mCurrentHeights;
    private float[] mTargetHeights;
    private float[] mBarVelocities;

    // 配置参数
    private int mBarCount = DEFAULT_BAR_COUNT;
    private int mBarWidth = DEFAULT_BAR_WIDTH;
    private int mBarSpacing = DEFAULT_BAR_SPACING;
    private float mAmplitudeScale = DEFAULT_AMPLITUDE_SCALE;

    // 颜色配置
    private int[] mGradientColors = {
            Color.parseColor("#FF6B6B"), // 红色
            Color.parseColor("#4ECDC4"), // 青色
            Color.parseColor("#45B7D1"), // 蓝色
            Color.parseColor("#96CEB4"), // 绿色
            Color.parseColor("#FFEAA7") // 黄色
    };

    // 动画相关
    private boolean mIsAnimating = false;
    private long mLastUpdateTime = 0;

    public AudioVisualizerView(Context context) {
        super(context);
        init();
    }

    public AudioVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioVisualizerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化视图组件
     */
    private void init() {
        // 初始化画笔
        mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBarPaint.setStyle(Paint.Style.FILL);
        mBarPaint.setStrokeCap(Paint.Cap.ROUND);

        mReflectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mReflectionPaint.setStyle(Paint.Style.FILL);
        mReflectionPaint.setStrokeCap(Paint.Cap.ROUND);

        // 初始化数据数组
        mCurrentHeights = new float[mBarCount];
        mTargetHeights = new float[mBarCount];
        mBarVelocities = new float[mBarCount];

        mCanvasRect = new Rect();

        // 启用硬件加速
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCanvasRect.set(0, 0, w, h);
        updateGradients();
    }

    /**
     * 更新渐变色
     */
    private void updateGradients() {
        if (mCanvasRect.width() <= 0 || mCanvasRect.height() <= 0)
            return;

        // 主渐变（从底部到顶部）
        mGradient = new LinearGradient(
                0, mCanvasRect.height() * 0.7f,
                0, 0,
                mGradientColors,
                null,
                Shader.TileMode.CLAMP);
        mBarPaint.setShader(mGradient);

        // 反射渐变（透明度递减）
        int[] reflectionColors = new int[mGradientColors.length];
        for (int i = 0; i < mGradientColors.length; i++) {
            reflectionColors[i] = Color.argb(
                    (int) (Color.alpha(mGradientColors[i]) * 0.3f),
                    Color.red(mGradientColors[i]),
                    Color.green(mGradientColors[i]),
                    Color.blue(mGradientColors[i]));
        }

        mReflectionGradient = new LinearGradient(
                0, mCanvasRect.height() * 0.7f,
                0, mCanvasRect.height(),
                reflectionColors,
                null,
                Shader.TileMode.CLAMP);
        mReflectionPaint.setShader(mReflectionGradient);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (mCurrentHeights == null || mCanvasRect.isEmpty()) {
            Log.w(TAG, "onDraw skipped - mCurrentHeights: " + (mCurrentHeights != null ? "available" : "null") +
                    ", canvasRect empty: " + mCanvasRect.isEmpty());
            return;
        }

        // 总是打印前几次调用，然后偶尔打印
        long currentTime = System.currentTimeMillis();
        if (mLastUpdateTime == 0 || currentTime - mLastUpdateTime > 2000 || currentTime % 2000 < 50) {
            Log.d(TAG, "onDraw called - mIsAnimating: " + mIsAnimating +
                    ", canvas size: " + canvas.getWidth() + "x" + canvas.getHeight() +
                    ", first bar heights: [" + mCurrentHeights[0] + ", " + mCurrentHeights[1] + ", "
                    + mCurrentHeights[2] + "], target heights: [" + mTargetHeights[0] + ", " + mTargetHeights[1] + ", "
                    + mTargetHeights[2] + "]");
        }

        // 更新动画
        updateAnimation();

        // 绘制频谱柱
        
        drawBars(canvas);

        // 绘制反射效果
        drawReflection(canvas);

        // 继续动画
        if (mIsAnimating) {
            Log.d(TAG, "Continuing animation, calling invalidate()");
            invalidate();
        } else {
            Log.d(TAG, "Animation stopped, no more invalidate()");
        }
    }

    /**
     * 绘制频谱柱
     */
    private void drawBars(@NonNull Canvas canvas) {
        float centerX = mCanvasRect.width() / 2f;
        float baseY = mCanvasRect.height() * 0.7f;
        float totalWidth = mBarCount * mBarWidth + (mBarCount - 1) * mBarSpacing;
        float startX = centerX - totalWidth / 2f;

        for (int i = 0; i < mBarCount; i++) {
            float x = startX + i * (mBarWidth + mBarSpacing);
            float barHeight = Math.max(mCurrentHeights[i], MIN_BAR_HEIGHT);

            // 绘制主柱形
            RectF barRect = new RectF(
                    x, baseY - barHeight,
                    x + mBarWidth, baseY);
            canvas.drawRoundRect(barRect, mBarWidth / 2f, mBarWidth / 2f, mBarPaint);
        }
    }

    /**
     * 绘制反射效果
     */
    private void drawReflection(@NonNull Canvas canvas) {
        float centerX = mCanvasRect.width() / 2f;
        float baseY = mCanvasRect.height() * 0.7f;
        float totalWidth = mBarCount * mBarWidth + (mBarCount - 1) * mBarSpacing;
        float startX = centerX - totalWidth / 2f;

        for (int i = 0; i < mBarCount; i++) {
            float x = startX + i * (mBarWidth + mBarSpacing);
            float barHeight = Math.max(mCurrentHeights[i], MIN_BAR_HEIGHT);
            float reflectionHeight = barHeight * 0.4f; // 反射高度为原高度的40%

            // 绘制反射柱形
            RectF reflectionRect = new RectF(
                    x, baseY + 4, // 4dp间距
                    x + mBarWidth, baseY + 4 + reflectionHeight);
            canvas.drawRoundRect(reflectionRect, mBarWidth / 2f, mBarWidth / 2f, mReflectionPaint);
        }
    }

    /**
     * 更新动画状态
     */
    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (mLastUpdateTime == 0) {
            mLastUpdateTime = currentTime;
            Log.d(TAG, "Animation initialized");
            return;
        }

        float deltaTime = (currentTime - mLastUpdateTime) / 1000f; // 转换为秒
        mLastUpdateTime = currentTime;

        boolean hasChanges = false;
        int animatingBars = 0;

        for (int i = 0; i < mBarCount; i++) {
            float diff = mTargetHeights[i] - mCurrentHeights[i];

            if (FAST_MODE) {
                // 快速模式：直接设置目标值，只保留轻微的平滑
                if (Math.abs(diff) > 0.1f) {
                    float smoothFactor = 0.4f; // 平滑因子，0.4表示40%直接跳到目标值，更平滑
                    mCurrentHeights[i] += diff * smoothFactor;
                    hasChanges = true;
                    animatingBars++;
                } else {
                    mCurrentHeights[i] = mTargetHeights[i];
                }
                mBarVelocities[i] = 0; // 快速模式不使用速度
            } else {
                // 原始弹性动画模式
                if (Math.abs(diff) > 0.1f) {
                    float acceleration = diff * 25f - mBarVelocities[i] * 15f;
                    mBarVelocities[i] += acceleration * deltaTime;
                    mCurrentHeights[i] += mBarVelocities[i] * deltaTime;

                    // 防止过冲
                    if ((diff > 0 && mCurrentHeights[i] > mTargetHeights[i]) ||
                            (diff < 0 && mCurrentHeights[i] < mTargetHeights[i])) {
                        mCurrentHeights[i] = mTargetHeights[i];
                        mBarVelocities[i] = 0;
                    }

                    hasChanges = true;
                    animatingBars++;
                } else {
                    mCurrentHeights[i] = mTargetHeights[i];
                    mBarVelocities[i] = 0;
                }
            }
        }

        boolean wasAnimating = mIsAnimating;
        mIsAnimating = hasChanges;

        if (wasAnimating != mIsAnimating) {
            Log.d(TAG, "Animation state changed: " + wasAnimating + " -> " + mIsAnimating +
                    ", animatingBars: " + animatingBars);
        }

        // 偶尔打印动画状态
        if (currentTime % 1000 < 50) {
            Log.d(TAG, "Animation update - hasChanges: " + hasChanges +
                    ", animatingBars: " + animatingBars +
                    ", first bar: current=" + mCurrentHeights[0] + ", target=" + mTargetHeights[0]);
        }
    }

    /**
     * 更新音频数据
     * 
     * @param bytes 音频字节数据
     */
    public void updateAudioData(byte[] bytes) {
        Log.d(TAG, "updateAudioData called - bytes: " + (bytes != null ? bytes.length : "null") +
                ", mIsAnimating: " + mIsAnimating + ", canvasRect: " + mCanvasRect);

        if (bytes == null || bytes.length == 0) {
            Log.w(TAG, "No audio data, setting bars to minimum height");
            // 没有数据时，目标高度设为最小值
            for (int i = 0; i < mBarCount; i++) {
                mTargetHeights[i] = MIN_BAR_HEIGHT;
            }
            mIsAnimating = true;
            Log.d(TAG, "Calling invalidate() for empty data");
            invalidate();
            return;
        }

        mRawAudioBytes = bytes.clone();
        processAudioData();

        // 强制重置动画状态
        mIsAnimating = true;
        mLastUpdateTime = 0; // 重置时间，确保动画重新开始

        Log.d(TAG, "Audio data processed, mIsAnimating: " + mIsAnimating + ", calling invalidate()");
        invalidate();

        // 强制立即重绘一次以确保视图更新
        post(() -> {
            Log.d(TAG, "Post-invalidate check - mIsAnimating: " + mIsAnimating);
            // 强制再次调用 invalidate，确保动画循环开始
            invalidate();
        });
    }

    /**
     * 处理音频数据，转换为频谱高度
     */
    private void processAudioData() {
        if (mRawAudioBytes == null) {
            Log.w(TAG, "mRawAudioBytes is null in processAudioData");
            return;
        }

        int dataLength = Math.min(mRawAudioBytes.length, mBarCount * 2);
        float maxHeight = mCanvasRect.height() * 0.6f;

        Log.d(TAG, "Processing audio data - dataLength: " + dataLength + ", maxHeight: " + maxHeight +
                ", canvasHeight: " + mCanvasRect.height());

        float totalAmplitude = 0;
        int validBars = 0;

        for (int i = 0; i < mBarCount; i++) {
            float amplitude = 0;

            if (i * 2 < dataLength) {
                // 将字节转换为振幅
                byte a = mRawAudioBytes[i * 2];
                byte b = i * 2 + 1 < dataLength ? mRawAudioBytes[i * 2 + 1] : 0;
                amplitude = Math.abs((a + (b << 8))) / 32768f;

                if (amplitude > 0) {
                    totalAmplitude += amplitude;
                    validBars++;
                }
            }

            // 应用缩放和平滑
            amplitude *= mAmplitudeScale;
            amplitude = Math.min(amplitude, 1f);

            // 更激进的对数缩放，让变化更明显
            amplitude = (float) Math.pow(amplitude, 0.5); // 从0.7改为0.5，让小变化更明显

            // 增加基础高度变化，让静音时也有一定的动态效果
            float dynamicHeight = amplitude * maxHeight;
            mTargetHeights[i] = Math.max(dynamicHeight, MIN_BAR_HEIGHT);
        }

        float avgAmplitude = validBars > 0 ? totalAmplitude / validBars : 0;
        Log.d(TAG, "Audio processing complete - avgAmplitude: " + avgAmplitude +
                ", validBars: " + validBars + ", first few heights: [" +
                mTargetHeights[0] + ", " + mTargetHeights[1] + ", " + mTargetHeights[2] + "]");
    }

    /**
     * 开始可视化
     */
    public void startVisualization() {
        mIsAnimating = true;
        invalidate();
    }

    /**
     * 停止可视化
     */
    public void stopVisualization() {
        // 平滑降到最小高度
        for (int i = 0; i < mBarCount; i++) {
            mTargetHeights[i] = MIN_BAR_HEIGHT;
        }
        mIsAnimating = true;
        invalidate();
    }

    /**
     * 设置柱形数量
     */
    public void setBarCount(int barCount) {
        if (barCount > 0 && barCount != mBarCount) {
            mBarCount = barCount;
            mCurrentHeights = new float[mBarCount];
            mTargetHeights = new float[mBarCount];
            mBarVelocities = new float[mBarCount];
            invalidate();
        }
    }

    /**
     * 设置振幅缩放
     */
    public void setAmplitudeScale(float scale) {
        if (scale > 0) {
            mAmplitudeScale = scale;
        }
    }

    /**
     * 设置渐变颜色
     */
    public void setGradientColors(int[] colors) {
        if (colors != null && colors.length > 0) {
            mGradientColors = colors.clone();
            updateGradients();
            invalidate();
        }
    }

    /**
     * 检查是否正在动画
     */
    public boolean isAnimating() {
        return mIsAnimating;
    }
}