package com.natsu.launcher.simple;


import androidx.annotation.NonNull;

public class AppData {
    final private String packageName;
    private String appName = null;
    private String appIcon = null;

    public AppData(@NonNull String packageName) {
        this.packageName = packageName;
    }
    public AppData(@NonNull String packageName, String appIcon, String appName) {
        this.packageName = packageName;
        this.appIcon = appIcon;
        this.appName = appName;
    }

    public void setAppIcon(String appIcon) {
        this.appIcon = appIcon;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppIcon() {
        return appIcon;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }
}

