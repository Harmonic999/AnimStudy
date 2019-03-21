package com.company.animstudy.util;


import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;

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

}
