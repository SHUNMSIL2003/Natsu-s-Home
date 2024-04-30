package com.android.natsu.launcher.simple;

import android.graphics.Bitmap;

public class AppData {
    final private String packageName;
    private String appName = null;
    private Bitmap appIcon = null;

    public AppData(String packageName){
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

