package com.natsu.launcher.simple;

import android.app.role.RoleManager;
import android.content.Context;

public class LauncherUtils {
    public static boolean isHomeScreenApp(Context context) {
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        return roleManager.isRoleHeld(RoleManager.ROLE_HOME);
    }

}

