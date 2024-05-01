package com.natsu.launcher.simple;


import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.slider.Slider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class LauncherApp  extends AppCompatActivity {

    private final List<String> packageNames = new ArrayList<>();
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    final int executorThreadPool = 4;//Runtime.getRuntime().availableProcessors();
    final private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(executorThreadPool);
    private ExoPlayer player;
    private PlayerView playerView;
    private String getVideoExtFromURI(Uri uri){
        final String uriString = uri.toString();
        if(uriString.contains(".")) {
            return uriString.substring(uriString.lastIndexOf("."));
        } else {
            return ".mp4";
        }
    }
    private final ActivityResultLauncher<Intent> videoPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    executor.execute(() -> {
                        Intent data = result.getData();
                        if (data != null) {
                            final Uri videoUri = data.getData();
                            if(videoUri!=null) {
                                WallpaperUtils.saveVideoWallpaper(getBaseContext(), player, findViewById(R.id.main_ac), videoUri, getVideoExtFromURI(videoUri));
                            }
                        }
                    });
                }
            });
    private AppIconAdapter adapter;
    private boolean settingsVisible = false;
    private final int animD = 320;
    private ValueAnimator colorAnimatorExpand;
    private ValueAnimator heightAnimatorExpand;
    private ValueAnimator colorAnimatorCollapse;
    private ValueAnimator heightAnimatorCollapse;
    private DecelerateInterpolator animInterpolator;

    private void setAppsList(List<String> appsList) {
        final Gson gson = new Gson();
        String jsonList = gson.toJson(appsList);
        SharedPreferences prefs = getSharedPreferences("home_apps", MODE_PRIVATE);
        prefs.edit().putString("appsList", jsonList).apply();
    }

    private List<String> getAppsList() {
        final Gson gson = new Gson();
        final List<String> appsList = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences("home_apps", MODE_PRIVATE);
        String jsonList = prefs.getString("appsList", "");
        if (!jsonList.isEmpty()) {
            Type type = new TypeToken<List<String>>() {
            }.getType();
            List<String> retrievedList = gson.fromJson(jsonList, type);
            appsList.addAll(retrievedList);
        }
        return appsList;
    }

    @SuppressLint("ApplySharedPref")
    private void setCrashState(boolean crashed) {
        SharedPreferences prefs = getSharedPreferences("home_maintain", MODE_PRIVATE);
        prefs.edit().putBoolean("crashed", crashed).commit();
    }

    private boolean getCrashState() {
        SharedPreferences prefs = getSharedPreferences("home_maintain", MODE_PRIVATE);
        return prefs.getBoolean("crashed", false);
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

        if (result > maxHeightInPixels) result = maxHeightInPixels;
        if (result < miniHeightInPixels) result = miniHeightInPixels;

        return result;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Toast.makeText(getApplicationContext(), "An Unexpected Error Occurred!", Toast.LENGTH_SHORT).show();
            setCrashState(true);
            finish();
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Your custom back handling logic (e.g., save data, prompt user)
                // Call super.handleOnBackPressed() if you want default behavior
                moveTaskToBack(true);
            }
        });

        Bitmap originalBitmap = getWallpaperBitmap();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
                WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        setTheme(R.style.Theme_NatsusHome);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        animInterpolator = new DecelerateInterpolator();

        setContentView(R.layout.home);

        final View status_bar_height = findViewById(R.id.status_bar_view);
        ViewGroup.LayoutParams params = status_bar_height.getLayoutParams();
        params.height = getStatusBarHeight();
        status_bar_height.setLayoutParams(params);
        status_bar_height.requestLayout();

        final ImageView wallpaper = findViewById(R.id.img_view_app_wallpaper);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wallpaper.setImageBitmap(originalBitmap);
        wallpaper.setVisibility(View.VISIBLE);

        loadColors();

        statusBarLightStateManager();

        RecyclerView appIconRecyclerView = findViewById(R.id.appIconRecyclerView);
        final FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this); // 'this' refers to the context
        layoutManager.setFlexDirection(FlexDirection.ROW); // Set row or column arrangement
        layoutManager.setJustifyContent(JustifyContent.SPACE_EVENLY); // Or other alignment options
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        appIconRecyclerView.setLayoutManager(layoutManager);

        player = new ExoPlayer.Builder(this).build();
        playerView = findViewById(R.id.video_background_exo);
        playerView.setPlayer(player);

        playVideoFromPrivateStorage();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> executor.execute(() -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            WallpaperUtils.saveWallpaper(getBaseContext(), findViewById(R.id.main_ac), Objects.requireNonNull(data.getData()));
                        }
                    }
                })
        );

        final List<String> quenchablePackageNames = getAppsList();
        if (quenchablePackageNames.isEmpty())
            quenchablePackageNames.addAll(getLaunchablePackageNames());
        else
            setAppsList(quenchablePackageNames);

        final View mainView = findViewById(R.id.main_ac);

        if (!quenchablePackageNames.isEmpty()) {
            packageNames.addAll(quenchablePackageNames);
            adapter = new AppIconAdapter(getBaseContext(), packageNames, appIconRecyclerView, mainView);
            appIconRecyclerView.setAdapter(adapter);
        }



        mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    findViewById(R.id.open_gal_btn).setOnClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                        openGallery();
                    });
                    findViewById(R.id.open_gal_video_btn).setOnClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                        pickVideoWallpaper();
                    });
                    findViewById(R.id.applyTransWall_BTN).setOnClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                        wallpaper.setImageResource(R.drawable.fully_trans);
                        //wallpaper.setVisibility(View.INVISIBLE);
                        executor.execute(() -> WallpaperUtils.resetWallpaper(getBaseContext(), findViewById(R.id.main_ac)));
                    });
                    findViewById(R.id.resetCache_BTN).setOnClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                        resetCache();
                    });

                    ((Slider)findViewById(R.id.bg_dim_slider)).setValue(loadBGTint());

                    ((Slider)findViewById(R.id.bg_dim_slider)).addOnChangeListener((slider, value, fromUser) -> {
                        // Do something when the slider value changes
                        final int opacityForAppBG = (int) (value);
                        final int alpha = Math.max(0, Math.min(255, ((int)(255*(opacityForAppBG/100.0)))));
                        final int currC = getColor(R.color.bg);
                        final int modifiedColor = Color.argb(alpha, Color.red(currC), Color.green(currC), Color.blue(currC));
                        saveBGTint(opacityForAppBG);
                        findViewById(R.id.background_dim).setBackgroundColor(modifiedColor);
                        statusBarLightStateManager();
                    });

                    ((Slider)findViewById(R.id.bar_dim_slider)).setValue(loadTBarTint());

                    ((Slider)findViewById(R.id.bar_dim_slider)).addOnChangeListener((slider, value, fromUser) -> {
                        // Do something when the slider value changes
                        final int opacityForAppBG = (int) (value);
                        final int alpha = Math.max(0, Math.min(255, ((int)(255*(opacityForAppBG/100.0)))));
                        final int currC = getColor(R.color.bg);
                        final int modifiedColor = Color.argb(alpha, Color.red(currC), Color.green(currC), Color.blue(currC));
                        saveTBarTint(opacityForAppBG);
                        StatusBarUtils.setStatusBarColor(getWindow(),modifiedColor);
                        findViewById(R.id.long_click_view_top).setBackgroundColor(modifiedColor);
                        statusBarLightStateManager();
                    });

                    final View main_set_cont_view = findViewById(R.id.settings_cont);
                    final View long_click_view_top_view = findViewById(R.id.long_click_view_top);

                    final int alphaTBar = Math.max(0, Math.min(255, ((int)(255*(loadTBarTint()/100.0)))));
                    final int CurrTBarC = getColor(R.color.bar_bg);
                    final int startColorExpand = Color.argb(alphaTBar, Color.red(CurrTBarC), Color.green(CurrTBarC), Color.blue(CurrTBarC));
                    final int endColorExpand = ContextCompat.getColor(getBaseContext(), R.color.bar_bg_exp);

                    colorAnimatorExpand = ValueAnimator.ofObject(new ArgbEvaluator(), startColorExpand, endColorExpand);
                    colorAnimatorExpand.setDuration(animD); // Animate over 1 second
                    colorAnimatorExpand.addUpdateListener(animation -> {
                        int animatedColor = (int) animation.getAnimatedValue();
                        long_click_view_top_view.setBackgroundColor(animatedColor);
                    });
                    colorAnimatorExpand.setInterpolator(animInterpolator);

                    final int initialHeightExpand = main_set_cont_view.getHeight();
                    final int finalHeightExpand = findViewById(R.id.settings_hh).getHeight(); // Or any specific height

                    heightAnimatorExpand = ValueAnimator.ofInt(initialHeightExpand, finalHeightExpand);
                    heightAnimatorExpand.addUpdateListener(animation -> {
                        int animatedHeight = (int) animation.getAnimatedValue();
                        LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) main_set_cont_view.getLayoutParams();
                        params1.height = animatedHeight;
                        main_set_cont_view.setLayoutParams(params1);
                    });

                    heightAnimatorExpand.setDuration(animD); // Animation duration in milliseconds
                    heightAnimatorExpand.setInterpolator(animInterpolator); // Smooth deceleration

                    final int startColorCollapse = ContextCompat.getColor(getBaseContext(), R.color.bar_bg_exp);
                    final int endColorCollapse =Color.argb(alphaTBar, Color.red(CurrTBarC), Color.green(CurrTBarC), Color.blue(CurrTBarC));

                    colorAnimatorCollapse = ValueAnimator.ofObject(new ArgbEvaluator(), startColorCollapse, endColorCollapse);
                    colorAnimatorCollapse.setDuration(animD); // Animate over 1 second
                    colorAnimatorCollapse.addUpdateListener(animation -> {
                        int animatedColor = (int) animation.getAnimatedValue();
                        long_click_view_top_view.setBackgroundColor(animatedColor);
                    });
                    colorAnimatorCollapse.setInterpolator(animInterpolator);


                    heightAnimatorCollapse = ValueAnimator.ofInt(finalHeightExpand, 0);
                    heightAnimatorCollapse.addUpdateListener(animation -> {
                        int animatedHeight = (int) animation.getAnimatedValue();
                        LinearLayout.LayoutParams params12 = (LinearLayout.LayoutParams) main_set_cont_view.getLayoutParams();
                        params12.height = animatedHeight;
                        main_set_cont_view.setLayoutParams(params12);
                    });

                    heightAnimatorCollapse.setDuration(animD);
                    heightAnimatorCollapse.setInterpolator(animInterpolator); // Smooth deceleration

                    colorAnimatorExpand.start();
                    colorAnimatorExpand.end();
                    colorAnimatorCollapse.start();
                    colorAnimatorCollapse.end();
                    heightAnimatorExpand.start();
                    heightAnimatorExpand.end();
                    heightAnimatorCollapse.start();
                    heightAnimatorCollapse.end();

                    findViewById(R.id.long_click_view_top).setOnLongClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        if (!settingsVisible) {
                            StatusBarUtils.setStatusBarColor(getWindow(),getColor(R.color.bar_bg_exp));

                            final int alphaTBarIN = Math.max(0, Math.min(255, ((int)(255*(loadTBarTint()/100.0)))));
                            final int CurrTBarCIN = getColor(R.color.bar_bg);
                            final int startColorExpandIN = Color.argb(alphaTBarIN, Color.red(CurrTBarCIN), Color.green(CurrTBarCIN), Color.blue(CurrTBarCIN));
                            final int endColorExpandIN = ContextCompat.getColor(getBaseContext(), R.color.bar_bg_exp);

                            colorAnimatorExpand = ValueAnimator.ofObject(new ArgbEvaluator(), startColorExpandIN, endColorExpandIN);
                            colorAnimatorExpand.setDuration(animD); // Animate over 1 second
                            colorAnimatorExpand.addUpdateListener(animation -> {
                                int animatedColor = (int) animation.getAnimatedValue();
                                long_click_view_top_view.setBackgroundColor(animatedColor);
                            });
                            colorAnimatorExpand.setInterpolator(animInterpolator);

                            colorAnimatorExpand.start();
                            heightAnimatorExpand.start();
                            settingsVisible = true;
                        } else {
                            final int alphaTBarIN = Math.max(0, Math.min(255, ((int)(255*(loadTBarTint()/100.0)))));
                            final int CurrTBarCIN = getColor(R.color.bar_bg);
                            final int startColorExpandIN = Color.argb(alphaTBarIN, Color.red(CurrTBarCIN), Color.green(CurrTBarCIN), Color.blue(CurrTBarCIN));
                            final int endColorExpandIN = ContextCompat.getColor(getBaseContext(), R.color.bar_bg_exp);

                            colorAnimatorCollapse = ValueAnimator.ofObject(new ArgbEvaluator(), endColorExpandIN, startColorExpandIN);
                            colorAnimatorCollapse.setDuration(animD); // Animate over 1 second
                            colorAnimatorCollapse.addUpdateListener(animation -> {
                                int animatedColor = (int) animation.getAnimatedValue();
                                long_click_view_top_view.setBackgroundColor(animatedColor);
                            });
                            colorAnimatorCollapse.setInterpolator(animInterpolator);

                            heightAnimatorCollapse.start();
                            colorAnimatorCollapse.start();
                            settingsVisible = false;
                            statusBarLightStateManager();
                        }
                        final RelativeLayout tet = findViewById(R.id.teto_cont);
                        if (tet.getVisibility() == View.VISIBLE) {
                            tet.removeAllViews();
                            tet.setVisibility(View.GONE);
                        }
                        return true;
                    });
                    setCrashState(false);
                    if (DisplayUtils.isFirst(getBaseContext())) {
                        final RelativeLayout tetoCont = findViewById(R.id.teto_cont);
                        final View teto = LayoutInflater.from(tetoCont.getContext())
                                .inflate(R.layout.teto, tetoCont, false);
                        teto.setOnClickListener(v -> {
                        });
                        tetoCont.addView(teto);
                    }
                }, 100);

                loadColors();
                statusBarLightStateManager();

                mainView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });


    }

    private void pickVideoWallpaper(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        videoPickerLauncher.launch(intent);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void playVideoFromPrivateStorage() {
        final File videoFile = new File(getFilesDir(), WallpaperUtils.VIDEO_WALLPAPER_FILENAME+WallpaperUtils.getVideoWallpaperExt(getBaseContext()));
        if (videoFile.exists()) {
            final Uri videoUri = Uri.fromFile(videoFile);
            MediaItem mediaItem = MediaItem.fromUri(videoUri);
            player.setMediaItem(mediaItem);
            player.setVolume(0.0f);
            player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
            player.prepare();
            player.play();
            playerView.setVisibility(View.VISIBLE);
        } else {
            playerView.setVisibility(View.GONE);
        }
    }

    private void loadColors(){
        final int CurrTBarC = getColor(R.color.bar_bg_exp);

        final int alphaBG = Math.max(0, Math.min(255, ((int)(255*(loadBGTint()/100.0)))));
        int modifiedColorBG = Color.argb(alphaBG, Color.red(CurrTBarC), Color.green(CurrTBarC), Color.blue(CurrTBarC));
        findViewById(R.id.background_dim).setBackgroundColor(modifiedColorBG);

        final int alphaTBar = Math.max(0, Math.min(255, ((int)(255*(loadTBarTint()/100.0)))));
        int modifiedColorTBar = Color.argb(alphaTBar, Color.red(CurrTBarC), Color.green(CurrTBarC), Color.blue(CurrTBarC));
        findViewById(R.id.long_click_view_top).setBackgroundColor(modifiedColorTBar);
    }

    private void statusBarLightStateManager(){
        final int BarAlpha = loadTBarTint();
        final int BGAlpha = loadBGTint();
        if(BGAlpha==0&&BarAlpha==0){
            StatusBarUtils.setStatusBarColor(getWindow(),Color.BLACK);
        }else {
            if(BGAlpha>0&&BarAlpha>0){
                StatusBarUtils.setStatusBarColor(getWindow(),getColor(R.color.bar_bg_exp));
            } else {
                if(BarAlpha==0){
                    StatusBarUtils.setStatusBarColor(getWindow(),getColor(R.color.bar_bg_exp));
                } else {
                    StatusBarUtils.setStatusBarColor(getWindow(),getColor(R.color.bar_bg_exp));
                }
            }
        }
    }

    private void saveBGTint(int alpha) {
        SharedPreferences prefs = getSharedPreferences("home_settings", MODE_PRIVATE);
        if(alpha>50) alpha = 50;
        if(alpha<0) alpha = 0;
        prefs.edit().putInt("bg_alpha", alpha).apply();
    }

    private int loadBGTint() {
        SharedPreferences prefs = getSharedPreferences("home_settings", MODE_PRIVATE);
        return prefs.getInt("bg_alpha", 16);
    }

    private void saveTBarTint(int alpha) {
        SharedPreferences prefs = getSharedPreferences("home_settings", MODE_PRIVATE);
        if(alpha>100) alpha = 100;
        if(alpha<0) alpha = 0;
        prefs.edit().putInt("top_bar_alpha", alpha).apply();
    }

    private int loadTBarTint() {
        SharedPreferences prefs = getSharedPreferences("home_settings", MODE_PRIVATE);
        return prefs.getInt("top_bar_alpha", 32);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private Bitmap getWallpaperBitmap() {
        return WallpaperUtils.loadWallpaper(getBaseContext());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
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
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
    }

    @Override
    protected void onDestroy() {
        if (adapter != null) adapter.shutDown();
        if(WallpaperUtils.isVideoAvailable(getBaseContext())) {
            if(WallpaperUtils.isVideoAvailable(getBaseContext())){
                if(player!=null)
                        player.release();
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(WallpaperUtils.isVideoAvailable(getBaseContext())){
            if(player!=null)
                if(player.getDuration()>0)
                    player.play();
        }
        final List<String> newList = getLaunchablePackageNames();
        if (newList != packageNames) {
            packageNames.clear();
            packageNames.addAll(newList);
            if (adapter != null) adapter.replaceAllItems(packageNames, true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(WallpaperUtils.isVideoAvailable(getBaseContext())){
            if(player!=null)
                if(player.isPlaying())
                    player.pause();
        }
    }

    private void resetCache() {
        executor.execute(() -> {
            deleteDirectoryContent(getFilesDir());
            Glide.get(getBaseContext()).clearDiskCache();

            if (adapter != null) {
                final List<String> newList = getLaunchablePackageNames();
                if (newList != packageNames) {
                    packageNames.clear();
                    packageNames.addAll(newList);
                    getMainExecutor().execute(() -> {
                        if (adapter != null) adapter.replaceAllItems(packageNames, false);
                    });
                }
            }
        });
    }

    private void deleteDirectoryContent(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectoryContent(file);  // Recursive call for subdirectories
                    } else {
                        if(!Objects.equals(file.getName(), WallpaperUtils.WALLPAPER_FILENAME) &&
                                !Objects.equals(file.getName(), WallpaperUtils.VIDEO_WALLPAPER_FILENAME+WallpaperUtils.getVideoWallpaperExt(getBaseContext()))) file.delete();
                    }
                }
            }
        }
    }

}
