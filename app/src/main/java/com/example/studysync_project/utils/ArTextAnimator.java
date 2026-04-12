package com.example.studysync_project.utils;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.TransformableNode;

public class ArTextAnimator {
    
    /**
     * Animate text appearing with a fade-in and scale effect
     */
    public static void animateTextAppear(TransformableNode node) {
        if (node == null) return;
        
        // Start with small scale
        node.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
        
        // Animate to normal scale
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(0.1f, 0.6f);
        scaleAnimator.setDuration(500);
        scaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            node.setLocalScale(new Vector3(scale, scale, scale));
        });
        scaleAnimator.start();
    }
    
    /**
     * Animate text with a gentle floating motion
     */
    public static void animateFloating(TransformableNode node, float baseHeight) {
        if (node == null) return;
        
        ValueAnimator floatAnimator = ValueAnimator.ofFloat(baseHeight, baseHeight + 0.1f);
        floatAnimator.setDuration(2000);
        floatAnimator.setRepeatCount(ValueAnimator.INFINITE);
        floatAnimator.setRepeatMode(ValueAnimator.REVERSE);
        floatAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        floatAnimator.addUpdateListener(animation -> {
            float height = (float) animation.getAnimatedValue();
            Vector3 currentPos = node.getLocalPosition();
            node.setLocalPosition(new Vector3(currentPos.x, height, currentPos.z));
        });
        floatAnimator.start();
    }
    
    /**
     * Create a pulsing effect for text
     */
    public static void animatePulse(View textView) {
        if (textView == null) return;
        
        ObjectAnimator pulseAnimator = ObjectAnimator.ofFloat(textView, "alpha", 1.0f, 0.7f, 1.0f);
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.start();
    }
}