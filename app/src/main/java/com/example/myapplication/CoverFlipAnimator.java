package com.example.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

public class CoverFlipAnimator {
    private static final long DURATION = 400; // ms

    private View frontView; // 正面视图
    private AudioVisualizerView backView; // 波形可视化View
    private ViewGroup container; // 包含两个视图的容器

    private boolean isShowingFront = true;
    private boolean isAnimating = false;
    private ObjectAnimator currentAnimation = null;

    public CoverFlipAnimator(ViewGroup container, View frontView, AudioVisualizerView backView) {
        this.container = container;
        this.frontView = frontView;
        this.backView = backView;

        // 初始状态
        frontView.setVisibility(View.VISIBLE);
        backView.setVisibility(View.GONE);

        // 设置点击监听
        frontView.setOnClickListener(v -> flipToBack());
        backView.setOnClickListener(v -> flipToFront());
    }

    /**
     * 开始播放时触发的动画（如果需要）
     */
    public void startFlipAnimation() {
        // 开始播放时，不再自动翻转到波形可视化，保持封面状态
        // if (isShowingFront && !isAnimating) {
        // flipToBack();
        // }
    }

    public void stopFlipAnimation() {
        // 停止播放时，停止当前动画并回到封面
        if (currentAnimation != null) {
            currentAnimation.cancel();
            currentAnimation = null;
        }

        if (!isShowingFront && !isAnimating) {
            flipToFront();
        }
    }

    public boolean isShowingFront() {
        return isShowingFront;
    }

    public void flipToBack() {
        if (!isShowingFront || isAnimating)
            return;

        isAnimating = true;

        // 前面消失动画
        ObjectAnimator frontAnim = ObjectAnimator.ofFloat(frontView, "rotationY", 0f, -90f);
        frontAnim.setDuration(DURATION / 2);
        frontAnim.setInterpolator(new AccelerateInterpolator());
        frontAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                frontView.setVisibility(View.GONE);

                // 后面出现动画
                backView.setVisibility(View.VISIBLE);
                backView.setRotationY(90f);
                ObjectAnimator backAnim = ObjectAnimator.ofFloat(backView, "rotationY", 90f, 0f);
                backAnim.setDuration(DURATION / 2);
                backAnim.setInterpolator(new DecelerateInterpolator());
                backAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isAnimating = false;
                        currentAnimation = null;
                    }
                });
                currentAnimation = backAnim;
                backAnim.start();
            }
        });
        currentAnimation = frontAnim;
        frontAnim.start();
        isShowingFront = false;
    }

    public void flipToFront() {
        if (isShowingFront || isAnimating)
            return;

        isAnimating = true;

        // 后面消失动画
        ObjectAnimator backAnim = ObjectAnimator.ofFloat(backView, "rotationY", 0f, 90f);
        backAnim.setDuration(DURATION / 2);
        backAnim.setInterpolator(new AccelerateInterpolator());
        backAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                backView.setVisibility(View.GONE);

                // 前面出现动画
                frontView.setVisibility(View.VISIBLE);
                frontView.setRotationY(-90f);
                ObjectAnimator frontAnim = ObjectAnimator.ofFloat(frontView, "rotationY", -90f, 0f);
                frontAnim.setDuration(DURATION / 2);
                frontAnim.setInterpolator(new DecelerateInterpolator());
                frontAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isAnimating = false;
                        currentAnimation = null;
                    }
                });
                currentAnimation = frontAnim;
                frontAnim.start();
            }
        });
        currentAnimation = backAnim;
        backAnim.start();
        isShowingFront = true;
    }
}