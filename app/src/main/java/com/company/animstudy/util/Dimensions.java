package com.company.animstudy.util;


import android.content.res.Resources;
import android.util.TypedValue;

public class Dimensions {

    private Dimensions() {
    }

    public static float px(int dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics()
        );
    }

    public static float dp(float px) {
        return px / Resources.getSystem().getDisplayMetrics().density;
    }

}
