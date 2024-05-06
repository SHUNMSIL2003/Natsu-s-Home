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
        final int firstState = loadAppFirstState(context);
        if (firstState<3) {
            setAppFirstState(context,firstState);
            return true;
        }
        return false;
    }

    private static void setAppFirstState(@NonNull Context context, int i) {
        SharedPreferences prefs = context.getSharedPreferences("app_load_state_v2", Context.MODE_PRIVATE);
        prefs.edit().putInt("first_load", i+1).apply();
    }
    private static int loadAppFirstState(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_load_state_v2", Context.MODE_PRIVATE);
        return prefs.getInt("first_load", 0);
    }


}

