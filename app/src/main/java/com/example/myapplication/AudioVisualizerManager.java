package com.example.myapplication;

import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/**
 * 音频可视化管理器
 * 负责管理Visualizer的生命周期，处理音频数据采集和分发
 * 
 * 架构特点：
 * - 遵循MVVM模式
 * - 支持生命周期感知
 * - 性能优化
 * - 线程安全
 * - 资源管理
 */
public class AudioVisualizerManager implements DefaultLifecycleObserver {

    private static final String TAG = "AudioVisualizerManager";

    // 配置常量
    private static final int DEFAULT_CAPTURE_RATE = 30; // 30ms采样率，适中的响应速度
    private static final int DEFAULT_CAPTURE_SIZE = 1024; // 采样大小
    private static final int MAX_CAPTURE_RATE = 60; // 最大采样率
    private static final int MIN_CAPTURE_RATE = 15; // 最小采样率，适中响应

    // 核心组件
    private Visualizer mVisualizer;
    private MediaPlayer mMediaPlayer;
    private Handler mMainHandler;
    private Handler mBackgroundHandler;

    // 配置参数
    private int mCaptureRate = DEFAULT_CAPTURE_RATE;
    private int mCaptureSize = DEFAULT_CAPTURE_SIZE;
    private boolean mIsEnabled = false;
    private boolean mIsInitialized = false;

    // 回调接口
    private AudioDataCallback mCallback;
    private StateChangeCallback mStateCallback;

    // 性能监控
    private long mLastCaptureTime = 0;
    private int mDroppedFrames = 0;

    /**
     * 音频数据回调接口
     */
    public interface AudioDataCallback {
        /**
         * 音频数据更新
         * 
         * @param waveform     波形数据
         * @param fft          FFT频谱数据
         * @param samplingRate 采样率
         */
        void onAudioDataUpdate(@NonNull byte[] waveform, @Nullable byte[] fft, int samplingRate);
    }

    /**
     * 状态变化回调接口
     */
    public interface StateChangeCallback {
        /**
         * 可视化器状态变化
         * 
         * @param isEnabled 是否启用
         * @param error     错误信息，null表示无错误
         */
        void onStateChanged(boolean isEnabled, @Nullable String error);

        /**
         * 性能统计
         * 
         * @param droppedFrames 丢帧数
         * @param avgLatency    平均延迟(ms)
         */
        void onPerformanceUpdate(int droppedFrames, float avgLatency);
    }

    /**
     * 构造函数
     */
    public AudioVisualizerManager() {
        mMainHandler = new Handler(Looper.getMainLooper());
        // 创建后台线程处理音频数据
        Thread backgroundThread = new Thread(() -> {
            Looper.prepare();
            mBackgroundHandler = new Handler(Looper.myLooper());
            Looper.loop();
        });
        backgroundThread.setName("AudioVisualizer-Background");
        backgroundThread.start();
    }

    /**
     * 初始化可视化器
     * 
     * @param mediaPlayer MediaPlayer实例
     * @return 是否初始化成功
     */
    public boolean initialize(@NonNull MediaPlayer mediaPlayer) {
        if (mIsInitialized) {
            Log.w(TAG, "Already initialized");
            return true;
        }

        mMediaPlayer = mediaPlayer;

        try {
            // 检查权限
            if (!checkAudioPermission()) {
                notifyStateChange(false, "缺少音频录制权限");
                return false;
            }

            // 创建Visualizer
            int audioSessionId = mMediaPlayer.getAudioSessionId();
            Log.d(TAG, "MediaPlayer audio session ID: " + audioSessionId);
            if (audioSessionId == 0) {
                Log.e(TAG, "MediaPlayer audio session ID is 0, not ready");
                notifyStateChange(false, "MediaPlayer未准备就绪");
                return false;
            }

            mVisualizer = new Visualizer(audioSessionId);
            Log.d(TAG, "Visualizer created successfully with session ID: " + audioSessionId);

            // 配置采样大小
            int[] captureSizes = Visualizer.getCaptureSizeRange();
            Log.d(TAG, "Capture size range: [" + captureSizes[0] + ", " + captureSizes[1] + "]");
            mCaptureSize = Math.max(captureSizes[0],
                    Math.min(mCaptureSize, captureSizes[1]));
            Log.d(TAG, "Setting capture size to: " + mCaptureSize);
            mVisualizer.setCaptureSize(mCaptureSize);

            // 设置数据采集监听器
            Log.d(TAG, "Setting data capture listener with rate: " + mCaptureRate + "ms");
            mVisualizer.setDataCaptureListener(
                    new Visualizer.OnDataCaptureListener() {
                        @Override
                        public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                            Log.d(TAG,
                                    "onWaveFormDataCapture called - length: "
                                            + (waveform != null ? waveform.length : "null") +
                                            ", samplingRate: " + samplingRate + ", visualizer enabled: "
                                            + visualizer.getEnabled());
                            handleWaveformData(waveform, samplingRate);
                        }

                        @Override
                        public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                            Log.d(TAG, "onFftDataCapture called");
                            handleFftData(fft, samplingRate);
                        }
                    },
                    mCaptureRate,
                    true, // 启用波形数据
                    true // 启用FFT数据
            );
            Log.d(TAG, "Data capture listener set successfully");

            mIsInitialized = true;
            Log.i(TAG, "Visualizer initialized successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize visualizer", e);
            notifyStateChange(false, "初始化失败: " + e.getMessage());
            cleanup();
            return false;
        }
    }

    /**
     * 开始可视化
     */
    public void start() {
        Log.d(TAG, "start() called - initialized: " + mIsInitialized + ", enabled: " + mIsEnabled +
                ", visualizer: " + (mVisualizer != null ? "available" : "null"));

        if (!mIsInitialized || mVisualizer == null) {
            Log.w(TAG, "Visualizer not initialized");
            return;
        }

        if (mIsEnabled) {
            Log.w(TAG, "Visualizer already started");
            return;
        }

        try {
            Log.d(TAG, "Enabling visualizer...");
            mVisualizer.setEnabled(true);
            mIsEnabled = true;
            mLastCaptureTime = System.currentTimeMillis();
            mDroppedFrames = 0;

            // 检查Visualizer状态
            Log.d(TAG, "Visualizer enabled: " + mVisualizer.getEnabled());
            Log.d(TAG, "Visualizer capture size: " + mVisualizer.getCaptureSize());
            Log.d(TAG, "Visualizer sampling rate: " + mVisualizer.getSamplingRate());

            // 启动定期检查，确保Visualizer持续工作
            startPeriodicCheck();

            notifyStateChange(true, null);
            Log.i(TAG, "Visualizer started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start visualizer", e);
            notifyStateChange(false, "启动失败: " + e.getMessage());
        }
    }

    /**
     * 停止可视化
     */
    public void stop() {
        if (!mIsEnabled || mVisualizer == null) {
            return;
        }

        try {
            mVisualizer.setEnabled(false);
            mIsEnabled = false;

            // 发送空数据，让视图平滑停止
            if (mCallback != null) {
                mMainHandler.post(() -> mCallback.onAudioDataUpdate(new byte[0], null, 0));
            }

            notifyStateChange(false, null);
            Log.i(TAG, "Visualizer stopped");

        } catch (Exception e) {
            Log.e(TAG, "Failed to stop visualizer", e);
        }
    }

    /**
     * 处理波形数据
     */
    private void handleWaveformData(byte[] waveform, int samplingRate) {
        Log.d(TAG, "handleWaveformData called - waveform length: " + (waveform != null ? waveform.length : "null") +
                ", samplingRate: " + samplingRate + ", callback: " + (mCallback != null ? "available" : "null"));

        if (mCallback == null) {
            Log.w(TAG, "No callback available for waveform data");
            return;
        }

        // 检查波形数据是否有效
        if (waveform == null || waveform.length == 0) {
            Log.w(TAG, "Invalid waveform data received");
            return;
        }

        // 计算波形数据的平均幅度用于调试
        int sum = 0;
        for (byte b : waveform) {
            sum += Math.abs(b);
        }
        int avgAmplitude = sum / waveform.length;
        Log.d(TAG, "Waveform average amplitude: " + avgAmplitude);

        // 性能监控
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastCaptureTime > mCaptureRate * 2) {
            mDroppedFrames++;
            Log.w(TAG, "Dropped frame detected, total dropped: " + mDroppedFrames);
        }
        mLastCaptureTime = currentTime;

        // 在主线程更新UI
        mMainHandler.post(() -> {
            if (mCallback != null) {
                Log.d(TAG, "Posting waveform data to main thread - samplingRate: " + samplingRate);
                mCallback.onAudioDataUpdate(waveform, null, samplingRate);
            } else {
                Log.w(TAG, "Callback is null when trying to post waveform data");
            }
        });
    }

    /**
     * 处理FFT数据
     */
    private void handleFftData(byte[] fft, int samplingRate) {
        // FFT数据处理可以在这里进行更复杂的频谱分析
        // 目前主要使用波形数据
    }

    /**
     * 检查音频权限
     */
    private boolean checkAudioPermission() {
        // 直接检查系统权限而不是尝试创建Visualizer
        // 因为Visualizer(0)可能不会抛出权限异常
        return true; // 让上层Activity处理权限检查
    }

    /**
     * 通知状态变化
     */
    private void notifyStateChange(boolean isEnabled, @Nullable String error) {
        if (mStateCallback != null) {
            mMainHandler.post(() -> mStateCallback.onStateChanged(isEnabled, error));
        }
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        if (mVisualizer != null) {
            try {
                if (mIsEnabled) {
                    mVisualizer.setEnabled(false);
                }
                mVisualizer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing visualizer", e);
            }
            mVisualizer = null;
        }

        mIsEnabled = false;
        mIsInitialized = false;
        mMediaPlayer = null;
    }

    // Lifecycle callbacks
    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (mIsInitialized && !mIsEnabled) {
            start();
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (mIsEnabled) {
            stop();
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        cleanup();

        // 清理后台线程
        if (mBackgroundHandler != null) {
            mBackgroundHandler.getLooper().quit();
            mBackgroundHandler = null;
        }
    }

    // Getters and Setters

    /**
     * 设置音频数据回调
     */
    public void setAudioDataCallback(@Nullable AudioDataCallback callback) {
        mCallback = callback;
    }

    /**
     * 设置状态变化回调
     */
    public void setStateChangeCallback(@Nullable StateChangeCallback callback) {
        mStateCallback = callback;
    }

    /**
     * 设置采样率
     * 
     * @param rate 采样率(ms)
     */
    public void setCaptureRate(int rate) {
        mCaptureRate = Math.max(MIN_CAPTURE_RATE, Math.min(rate, MAX_CAPTURE_RATE));

        // 如果已经初始化，需要重新设置
        if (mIsInitialized && mVisualizer != null) {
            try {
                boolean wasEnabled = mIsEnabled;
                if (wasEnabled)
                    stop();

                // 重新设置数据采集监听器
                mVisualizer.setDataCaptureListener(
                        new Visualizer.OnDataCaptureListener() {
                            @Override
                            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform,
                                    int samplingRate) {
                                handleWaveformData(waveform, samplingRate);
                            }

                            @Override
                            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                                handleFftData(fft, samplingRate);
                            }
                        },
                        mCaptureRate, true, true);

                if (wasEnabled)
                    start();
            } catch (Exception e) {
                Log.e(TAG, "Failed to update capture rate", e);
            }
        }
    }

    /**
     * 获取当前状态
     */
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * 获取初始化状态
     */
    public boolean isInitialized() {
        return mIsInitialized;
    }

    /**
     * 获取性能统计
     */
    public int getDroppedFrames() {
        return mDroppedFrames;
    }

    /**
     * 启动定期检查，确保Visualizer持续工作
     */
    private void startPeriodicCheck() {
        if (mMainHandler != null) {
            mMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mIsEnabled && mVisualizer != null) {
                        // 检查Visualizer状态
                        boolean isEnabled = mVisualizer.getEnabled();
                        Log.d(TAG, "Periodic check - Visualizer enabled: " + isEnabled +
                                ", capture rate: " + mCaptureRate + "ms");

                        if (!isEnabled) {
                            Log.w(TAG, "Visualizer was disabled, attempting to restart");
                            try {
                                mVisualizer.setEnabled(true);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to restart visualizer", e);
                            }
                        }

                        // 继续定期检查
                        if (mIsEnabled) {
                            mMainHandler.postDelayed(this, 2000); // 每2秒检查一次
                        }
                    }
                }
            }, 2000);
        }
    }
}