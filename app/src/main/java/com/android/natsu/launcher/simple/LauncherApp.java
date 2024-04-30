package com.android.natsu.launcher.simple;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LauncherApp  extends AppCompatActivity {

    private final List<String> packageNames = new ArrayList<>();
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private AppIconAdapter adapter;

    private void setAppsList(List<String> appsList){
        final Gson gson = new Gson();
        String jsonList = gson.toJson(appsList);
        SharedPreferences prefs = getSharedPreferences("home_apps", MODE_PRIVATE);
        prefs.edit().putString("appsList", jsonList).apply();
    }
    private List<String> getAppsList(){
        final Gson gson = new Gson();
        final List<String> appsList = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences("home_apps", MODE_PRIVATE);
        String jsonList = prefs.getString("appsList", "");
        if (!jsonList.isEmpty()) {
            Type type = new TypeToken<List<String>>(){}.getType();
            List<String> retrievedList = gson.fromJson(jsonList, type);
            appsList.addAll(retrievedList);
        }
        return appsList;
    }
    @SuppressLint("ApplySharedPref")
    private void setCrashState(boolean crashed){
        SharedPreferences prefs = getSharedPreferences("home_maintain", MODE_PRIVATE);
        prefs.edit().putBoolean("crashed", crashed).commit();
    }
    private boolean getCrashState(){
        SharedPreferences prefs = getSharedPreferences("home_maintain", MODE_PRIVATE);
        return prefs.getBoolean("crashed",false);
    }
    public int getStatusBarHeight() {
        int result = 0;

        // Fallback for API level 30 and above
        final WindowInsets windowInsets = getWindow().getDecorView().getRootWindowInsets();
        if (windowInsets != null) {
            WindowInsetsCompat insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets);
            result = insetsCompat.getInsets(WindowInsetsCompat.Type.statusBars()).top;
        }

        // Fallback for cases where previous methods don't work
        if (result == 0) {
            Rect rectangle = new Rect();
            getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);
            result = rectangle.top;
        }

        if (result == 0 && !getCrashState()) {
            // Try retrieving the height resource directly
            @SuppressLint({"DiscouragedApi", "InternalInsetResource"}) int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = getResources().getDimensionPixelSize(resourceId);
            }
        }

        final int maxHeightInPixels = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                64,
                getResources().getDisplayMetrics()
        );
        final int miniHeightInPixels = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                12,
                getResources().getDisplayMetrics()
        );

        if(result>maxHeightInPixels) result = maxHeightInPixels;
        if(result<miniHeightInPixels) result = miniHeightInPixels;

        return result;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Toast.makeText(getApplicationContext(),"An Unexpected Error Occurred!", Toast.LENGTH_SHORT).show();
            setCrashState(true);
            finish();
        });

        Bitmap originalBitmap = getWallpaperBitmap();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setTheme(R.style.Theme_NatsusHome);
        setContentView(R.layout.home);

        final View status_bar_height = findViewById(R.id.status_bar_view);
        ViewGroup.LayoutParams params = status_bar_height.getLayoutParams();
        params.height = getStatusBarHeight();
        status_bar_height.setLayoutParams(params);
        status_bar_height.requestLayout();

        final ImageView wallpaper = findViewById(R.id.img_view_app_wallpaper);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wallpaper.setImageBitmap(originalBitmap);

        RecyclerView appIconRecyclerView = findViewById(R.id.appIconRecyclerView);
        final FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this); // 'this' refers to the context
        layoutManager.setFlexDirection(FlexDirection.ROW); // Set row or column arrangement
        layoutManager.setJustifyContent(JustifyContent.SPACE_EVENLY); // Or other alignment options
        appIconRecyclerView.setLayoutManager(layoutManager);

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

        final List<String> quenchablePackageNames = getAppsList();
        if(quenchablePackageNames.isEmpty()) quenchablePackageNames.addAll(getLaunchablePackageNames()); else
            setAppsList(quenchablePackageNames);
        if (!quenchablePackageNames.isEmpty()) {
            packageNames.addAll(quenchablePackageNames);
            adapter = new AppIconAdapter(getBaseContext(),packageNames);
            appIconRecyclerView.setAdapter(adapter);
        }

        final View mainView = findViewById(R.id.main_ac);
        mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    findViewById(R.id.background_dim).setOnLongClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        openGallery();
                        return false;
                    });
                    findViewById(R.id.long_click_view_top).setOnLongClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        openGallery();
                        return false;
                    });
                    setCrashState(false);
                }, 640);
                mainView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });


    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

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
        final List<String> packageNames = new ArrayList<>();

        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> quenchableApps = pm.queryIntentActivities(mainIntent, 0);
        for (ResolveInfo app : quenchableApps) {
            packageNames.add(app.activityInfo.packageName);
        }

        return packageNames;
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
    @Override
    protected void onDestroy() {
        if(adapter!=null)adapter.shutDown();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        final List<String> newList = getLaunchablePackageNames();
        if(newList!=packageNames){
            packageNames.clear();
            packageNames.addAll(newList);
            if(adapter!=null) adapter.replaceAllItems(packageNames);
        }
    }
}
