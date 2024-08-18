package com.natsu.launcher.simple;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;

public class DisplayUtils {

    public static int getDisplayWidth(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowMetrics metrics = context.getSystemService(WindowManager.class).getCurrentWindowMetrics();

            return metrics.getBounds().width();
        } else {
            return getScreenWidth_LOWAPI(context);
        }
    }
    public static int getDisplayHeight(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowMetrics metrics = context.getSystemService(WindowManager.class).getCurrentWindowMetrics();

            return metrics.getBounds().height();
        } else {
            return getScreenHeight_LOWAPI(context);
        }
    }

    public static int getScreenWidth_LOWAPI(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    public static int getScreenHeight_LOWAPI(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
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

