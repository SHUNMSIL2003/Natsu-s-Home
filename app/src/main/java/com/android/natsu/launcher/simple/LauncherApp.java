package com.android.natsu.launcher.simple;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LauncherApp  extends AppCompatActivity {

    private final List<String> packageNames = new ArrayList<>();
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Toast.makeText(getApplicationContext(),"Uncepected Error!", Toast.LENGTH_SHORT).show());


        Bitmap originalBitmap = getWallpaperBitmap();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setTheme(R.style.Theme_NatsusHome);
        setContentView(R.layout.home);
        ImageView wallaper = findViewById(R.id.img_view_app_wallpaper);
        wallaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wallaper.setImageBitmap(originalBitmap);
        getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        // Handle the selected image URI (data.getData())
                        if(data!=null){
                            setWallpaperBitmap(data.getData());
                            cropAndSaveImage();
                        }
                    }
                }
        );

        List<String> launchablePackageNames = getLaunchablePackageNames();
        final Context context = this;
        if (!launchablePackageNames.isEmpty()) {
            packageNames.addAll(launchablePackageNames);
            //final Context context = getApplicationContext();
            for(int i=0;i<packageNames.size();i++){
                addAppShortcut(context,i);
            }
        }

        final View mainView = findViewById(R.id.main_ac);
        mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    findViewById(R.id.scrollable).setOnLongClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        openGallery();
                        return false;
                    });
                    findViewById(R.id.background_dim).setOnLongClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        openGallery();
                        return false;
                    });
                    findViewById(R.id.flex_box).setOnLongClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        openGallery();
                        return false;
                    });
                    findViewById(R.id.long_click_view_top).setOnLongClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        openGallery();
                        return false;
                    });
                    findViewById(R.id.long_click_view_bottom).setOnLongClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        openGallery();
                        return false;
                    });

                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    String packageName = getPackageName();
                    if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                        @SuppressLint("BatteryLife") Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:com.android.natsu.launcher.simple"));
                        startActivity(intent);
                        Toast.makeText(getBaseContext(),"neeeeeed",Toast.LENGTH_SHORT).show();
                    }

                }, 640);
                mainView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });


    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @SuppressLint("ApplySharedPref")
    private void setWallpaperBitmap(Uri uri){
        WallpaperUtils.saveWallpaper(getBaseContext(),uri);
    }
    private Bitmap getWallpaperBitmap(){
        return WallpaperUtils.loadWallpaper(getBaseContext());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void cropAndSaveImage() {
        try {
            Bitmap originalBitmap = getWallpaperBitmap();
            ((ImageView)findViewById(R.id.img_view_app_wallpaper)).setScaleType(ImageView.ScaleType.CENTER_CROP);
            ((ImageView)findViewById(R.id.img_view_app_wallpaper)).setImageBitmap(originalBitmap);
        } catch (Exception e) {
            Toast.makeText(getBaseContext(),"null wallaper",Toast.LENGTH_SHORT).show();
            // Handle potential errors
        }
    }

    private List<String> getLaunchablePackageNames() {
        List<String> packageNames = new ArrayList<>();

        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> launchableApps = pm.queryIntentActivities(mainIntent, 0);
        for (ResolveInfo app : launchableApps) {
            packageNames.add(app.activityInfo.packageName);
        }

        return packageNames;
    }
    private void startRandomApp(String packageNameToLaunch) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageNameToLaunch);
        if (launchIntent != null) {
            startActivity(launchIntent);
        }
    }

    private void addAppShortcut(Context context, final int id){
        final FlexboxLayout flexboxLayout = findViewById(R.id.flex_box);
        LayoutInflater inflater = LayoutInflater.from(context); // Get a LayoutInflater
        //View customView = inflater.inflate(R.layout.appicon, null);
        View customView = inflater.inflate(R.layout.appicon, flexboxLayout, false);
        final String pkg = packageNames.get(id);
        if(!pkg.toLowerCase(Locale.ROOT).contains("com.android.natsu.launcher.simple".toLowerCase(Locale.ROOT))) {
            ((ImageView) customView.findViewById(R.id.app_icon)).setImageBitmap(getAppIcon(context, pkg));
            ((TextView) customView.findViewById(R.id.app_name)).setText(getAppName(context, pkg));
            ((TextView) customView.findViewById(R.id.app_pkg)).setText(pkg);
            customView.setOnClickListener(v -> {
                final String pkg12 = ((TextView) v.findViewById(R.id.app_pkg)).getText().toString();
                startRandomApp(pkg12);
            });
            customView.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                openGallery();
                return false;
            });
            flexboxLayout.addView(customView);
        }

    }

    private static Bitmap getAppIcon(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            Drawable appIcon = pm.getApplicationIcon(appInfo);
            return ((BitmapDrawable) appIcon).getBitmap();
        } catch (PackageManager.NameNotFoundException e) {
            return null; // Handle the case where the package is not found
        }
    }

    public static String getAppName(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo appInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, 0);
            return (String) pm.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            return null; // Or return a default name like "Unknown"
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true; // Indicates that you've consumed the event
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
    }

}
