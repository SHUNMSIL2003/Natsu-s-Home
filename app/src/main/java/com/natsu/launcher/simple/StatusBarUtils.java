package com.natsu.launcher.simple;

import android.graphics.Color;
import android.view.Window;
import android.view.WindowInsetsController;

public class StatusBarUtils {

    public static void setStatusBarColor(Window window, int color) {

        // Updated for API 30+
        WindowInsetsController insetsController = window.getInsetsController();
        if (insetsController != null) {
            if (isColorLight(color)) {
                insetsController.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            } else {
                insetsController.setSystemBarsAppearance(0, // Clear any previous settings
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        }
    }

    public static boolean isColorLight(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return luminance > 0.5;
    }

}
