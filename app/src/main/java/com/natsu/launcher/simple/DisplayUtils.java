package com.natsu.launcher.simple;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;

public class DisplayUtils {

    public static int getDisplayWidth(Context context) {
        WindowMetrics metrics = context.getSystemService(WindowManager.class).getCurrentWindowMetrics();
        return metrics.getBounds().width();
    }
    public static int getDisplayHeight(Context context) {
        WindowMetrics metrics = context.getSystemService(WindowManager.class).getCurrentWindowMetrics();
        return metrics.getBounds().height();
    }

    public static boolean isFirst(@NonNull Context context) {
        final boolean firstState = loadAppFirstState(context);
        if (firstState) {
            setAppFirstState(context);
        }
        return firstState;
    }
    public static void setAppFirstState(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_load_state", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("firstLoad", false).apply();
    }
    public static boolean loadAppFirstState(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_load_state", Context.MODE_PRIVATE);
        return prefs.getBoolean("firstLoad", true);
    }


}

