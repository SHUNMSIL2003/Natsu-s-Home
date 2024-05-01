package com.natsu.launcher.simple;

import android.content.Context;
import android.view.WindowManager;
import android.view.WindowMetrics;

public class DisplayUtils {

    public static int getDisplayWidth(Context context) {
        WindowMetrics metrics = context.getSystemService(WindowManager.class).getCurrentWindowMetrics();
        return metrics.getBounds().width();
    }

    public static int getDisplayHeight(Context context) {
        WindowMetrics metrics = context.getSystemService(WindowManager.class).getCurrentWindowMetrics();
        return metrics.getBounds().height();
    }
}

