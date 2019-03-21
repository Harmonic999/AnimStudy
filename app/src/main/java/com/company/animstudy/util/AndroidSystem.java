package com.company.animstudy.util;


import android.content.Context;
import android.os.Vibrator;

public class AndroidSystem {

    private AndroidSystem() {
    }

    public static void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) vibrator.vibrate(100);
    }

}
