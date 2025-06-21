package com.example.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AudioVisualizerView extends View {
    private static final String TAG = "AudioVisualizerView";
    private static final int MAX_PARTICLES = 500;

    private Paint particlePaint;
    private Paint backgroundPaint;

    private List<Particle> activeParticles = new ArrayList<>();
    private List<Particle> particlePool = new ArrayList<>();
    private Random random = new Random();

    private ValueAnimator animator;
    private boolean isPlaying = false;

    // --- 粒子内部类，简化物理属性 ---
    private static class Particle {
        float x, y, radius, alpha, vx, vy;
        float hue; // 使用hue来代表颜色
        float lifespan, initialLifespan;

        void reset() {
            x = 0;
            y = 0;
            radius = 0;
            alpha = 0;
            vx = 0;
            vy = 0;
            hue = 0;
            lifespan = 0;
            initialLifespan = 0;
        }
    }

    public AudioVisualizerView(Context context) {
        super(context);
        init();
    }

    public AudioVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);

        particlePaint = new Paint();
        particlePaint.setAntiAlias(true);
        particlePaint.setStyle(Paint.Style.FILL);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#121212"));

        for (int i = 0; i < MAX_PARTICLES; i++) {
            particlePool.add(new Particle());
        }

        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(16);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            updateParticles();
            invalidate();
        });
    }

    public void onMusicPulse(float volume, float bassIntensity) {
        if (!isPlaying)
            return;

        int particlesToEmit = (int) (bassIntensity * 40 + volume * 10);

        for (int i = 0; i < particlesToEmit && !particlePool.isEmpty(); i++) {
            Particle p = particlePool.remove(0);
            p.reset();

            p.x = getWidth() / 2f;
            p.y = getHeight() / 2f;
            p.radius = random.nextFloat() * 2f + 1f;
            p.initialLifespan = p.lifespan = random.nextFloat() * 1.5f + 0.5f;
            p.alpha = 255;

            // 随机初始色相
            p.hue = random.nextFloat() * 360;

            double angle = random.nextDouble() * 2 * Math.PI;
            double speed = random.nextDouble() * 12 + 8 * bassIntensity;
            p.vx = (float) (Math.cos(angle) * speed);
            p.vy = (float) (Math.sin(angle) * speed);

            activeParticles.add(p);
        }
    }

    private void updateParticles() {
        for (int i = activeParticles.size() - 1; i >= 0; i--) {
            Particle p = activeParticles.get(i);

            p.x += p.vx;
            p.y += p.vy;

            p.lifespan -= 0.02f;

            if (p.lifespan <= 0) {
                activeParticles.remove(i);
                particlePool.add(p);
            } else {
                p.alpha = (p.lifespan / p.initialLifespan) * 255;
                // 色相随时间演化
                p.hue = (p.hue + 1.0f) % 360;
            }
        }
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
        if (playing) {
            if (!animator.isRunning())
                animator.start();
        } else {
            if (animator.isRunning())
                animator.cancel();
            particlePool.addAll(activeParticles);
            activeParticles.clear();
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        float[] hsv = { 0f, 1f, 1f }; // 饱和度与亮度保持最大，以获得艳丽色彩

        for (Particle p : activeParticles) {
            // 根据实时hue计算颜色
            hsv[0] = p.hue;
            int color = Color.HSVToColor(hsv);

            // 直接设置颜色和透明度
            int alpha = (int) Math.max(0, Math.min(255, p.alpha));
            particlePaint.setColor(color);
            particlePaint.setAlpha(alpha);

            canvas.drawCircle(p.x, p.y, p.radius, particlePaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }
}