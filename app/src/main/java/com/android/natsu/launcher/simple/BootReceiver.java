package com.android.natsu.launcher.simple;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent startAppIntent = new Intent(context, LauncherApp.class);
            startAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startAppIntent);
        }
    }
}

