package com.natsu.launcher.simple;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

public class AppData {
    final private String packageName;
    private String appName = null;
    private Bitmap appIcon = null;

    public AppData(@NonNull String packageName) {
        this.packageName = packageName;
    }

    public void setAppIcon(Bitmap appIcon) {
        this.appIcon = appIcon;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Bitmap getAppIcon() {
        return appIcon;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }
}

