package com.example.myapplication;

import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.util.Log;

public class AudioVisualizerManager {
    private static final String TAG = "AudioVisualizerManager";
    private static final int SAMPLING_RATE = 44100; // 标准采样率
    private static final int CAPTURE_SIZE = 512; // 增加捕获大小以获得更好的数据

    private Visualizer visualizer;
    private AudioVisualizerView visualizerView;
    private int audioSessionId = -1;
    private boolean isVisualizerEnabled = false;

    public AudioVisualizerManager(AudioVisualizerView view) {
        this.visualizerView = view;
    }

    public void setupVisualizer(MediaPlayer mediaPlayer) {
        if (mediaPlayer == null) {
            Log.w(TAG, "MediaPlayer is null, cannot setup visualizer");
            return;
        }

        releaseVisualizer(); // 确保先释放之前的资源

        try {
            audioSessionId = mediaPlayer.getAudioSessionId();
            Log.d(TAG, "Audio session ID from MediaPlayer: " + audioSessionId);

            if (audioSessionId == -1) {
                Log.w(TAG, "Invalid audio session ID (-1). Falling back to global audio output (session 0).");
                visualizer = new Visualizer(0);
            } else {
                visualizer = new Visualizer(audioSessionId);
            }

            visualizer.setCaptureSize(CAPTURE_SIZE);
            Log.d(TAG, "Visualizer created with capture size: " + CAPTURE_SIZE);

            // 设置数据捕获监听器
            visualizer.setDataCaptureListener(
                    new Visualizer.OnDataCaptureListener() {
                        @Override
                        public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                            // 为了专注FFT数据，我们暂时不处理原始波形
                        }

                        @Override
                        public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                            if (fft != null && fft.length > 0) {
                                processFftData(fft);
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2, // 使用最大捕获率的一半
                    false, // 禁用波形数据
                    true // 只启用FFT数据
            );

            visualizer.setEnabled(true);
            isVisualizerEnabled = true;
            Log.d(TAG, "Visualizer setup completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to setup visualizer", e);
            isVisualizerEnabled = false;
        }
    }

    private void processFftData(byte[] fft) {
        if (visualizerView == null)
            return;

        // 计算整体音量强度 (RMS - Root Mean Square)
        double rms = 0;
        for (int i = 0; i < fft.length; i++) {
            rms += fft[i] * fft[i];
        }
        rms = Math.sqrt(rms / fft.length);
        float volume = (float) (rms / 128.0); // 归一化

        // 计算低音强度 (例如：前1/8的频率范围)
        double bassRms = 0;
        int bassBand = fft.length / 8;
        for (int i = 0; i < bassBand; i++) {
            bassRms += fft[i] * fft[i];
        }
        bassRms = Math.sqrt(bassRms / bassBand);
        float bassIntensity = (float) (bassRms / 128.0); // 归一化

        visualizerView.onMusicPulse(volume, bassIntensity);
    }

    public void releaseVisualizer() {
        if (visualizer != null) {
            try {
                visualizer.setEnabled(false);
                visualizer.release();
                Log.d(TAG, "Visualizer released");
            } catch (Exception e) {
                Log.e(TAG, "Error releasing visualizer", e);
            } finally {
                visualizer = null;
                isVisualizerEnabled = false;
            }
        }
    }

    public void updatePlayingState(boolean isPlaying) {
        Log.d(TAG, "Updating playing state: " + isPlaying);
        if (visualizerView != null) {
            visualizerView.setPlaying(isPlaying);
        }

        // 如果可视化器已启用但播放状态改变，重新设置
        if (isVisualizerEnabled && !isPlaying) {
            Log.d(TAG, "Playback stopped, visualizer will be updated when playback resumes");
        }
    }

    public boolean isVisualizerEnabled() {
        return isVisualizerEnabled;
    }
}