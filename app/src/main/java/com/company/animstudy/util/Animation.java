package com.company.animstudy.util;


import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup;

public class Animation {

    private Animation() {
    }

    public static void scaleView(View view, float xScale, float yScale) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, xScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, yScale);
        scaleX.setDuration(50);
        scaleY.setDuration(50);

        AnimatorSet scale = new AnimatorSet();
        scale.play(scaleX).with(scaleY);
        scale.start();
    }

    public static void setLayoutTransition(ViewGroup view, long duration) {
        LayoutTransition transition = new LayoutTransition();
        transition.enableTransitionType(LayoutTransition.CHANGING);
        transition.setDuration(duration);
        view.setLayoutTransition(transition);
    }

}
