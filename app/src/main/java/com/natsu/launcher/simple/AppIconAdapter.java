package com.natsu.launcher.simple;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AppIconAdapter extends RecyclerView.Adapter<AppIconAdapter.AppIconViewHolder> {

    final private List<AppData> appDataList = new ArrayList<>();
    final private Context context;
    final private RecyclerView recyclerView;
    final private View mainView;
    private static final int MaxRes = 512;
    private final DecelerateInterpolator animInterpolator;

    public AppIconAdapter(@NonNull Context context, @NonNull List<String> appDataList, @NonNull RecyclerView recyclerView, @NonNull View mainView) {
        this.context = context;
        animInterpolator = new DecelerateInterpolator();
        this.recyclerView = recyclerView;
        this.mainView = mainView;
        this.appDataList.addAll(convertListFormat(context, appDataList));
    }

    private List<AppData> convertListFormat(@NonNull Context context, @NonNull List<String> list) {
        final List<AppData> newList = new ArrayList<>();
        final String appPKG = context.getPackageName();
        if (DisplayUtils.isFirst(context)) {
            while (!list.isEmpty()) {
                final String PKG = list.remove(0);
                if (!PKG.equals(appPKG))
                    newList.add(new AppData(PKG, getAppIConABS(PKG), getAppName(PKG)));
            }
        } else {
            while (!list.isEmpty()) {
                final String PKG = list.remove(0);
                if (!PKG.equals(appPKG)) newList.add(new AppData(PKG));
            }
        }
        return newList;
    }

    @NonNull
    @Override
    public AppIconViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.appicon, parent, false);
        return new AppIconViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AppIconViewHolder holder, int position) {
        final AppData appData = appDataList.get(position);
        if (appData.getAppName() == null) {
            appData.setAppName(getAppName(appData.getPackageName()));
        }
        if (appData.getAppIcon() == null) {
            appData.setAppIcon(getAppIConABS(appData.getPackageName()));
        }
        if (appDataList.get(position) != appData) {
            appDataList.set(position, appData);
        }
        if (holder.appNameView != null && appData.getAppName() != null)
            holder.appNameView.setText(appData.getAppName());
        if (holder.appIconView != null && appData.getAppIcon() != null)
            Glide.with(context)
                    .load(new File(appData.getAppIcon())) // Directly load from File
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original and resized versions
                    .centerCrop() // Or use your desired crop/fit option
                    .placeholder(R.drawable.trans)
                    .into(holder.appIconView);
        if (holder.appIconView != null && appData.getAppName() != null)
            holder.appIconView.setContentDescription(appData.getAppName());
        if (holder.appIconView != null) holder.appIconView.setOnClickListener(v -> {
            final String PKG = appData.getPackageName();
            startApp(position, PKG);
        });
        if (holder.appIconView != null && appData.getPackageName() != null)
            holder.appIconView.setOnLongClickListener(v -> {
                showPopupMenu(v, appData.getPackageName(), position);
                return true;
            });
    }

    @Override
    public int getItemCount() {
        return appDataList.size();
    }

    public static class AppIconViewHolder extends RecyclerView.ViewHolder {
        ImageView appIconView;
        TextView appNameView;

        public AppIconViewHolder(View itemView) {
            super(itemView);
            appIconView = itemView.findViewById(R.id.app_icon);
            appNameView = itemView.findViewById(R.id.app_name);
        }
    }

    private String saveIcon(Bitmap icon, @NonNull String packageName) {
        File wallpaperFile = new File(context.getFilesDir(), packageName + ".png");
        try (FileOutputStream outputStream = new FileOutputStream(wallpaperFile)) {
            icon.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            return wallpaperFile.getAbsolutePath();
        } catch (IOException ignored) {
            return null;
        }
    }

    private String loadIcon(@NonNull String packageName) {
        File wallpaperFile = new File(context.getFilesDir(), packageName + ".png");
        if (wallpaperFile.exists()) {
            return wallpaperFile.getAbsolutePath();//BitmapFactory.decodeFile(wallpaperFile.getAbsolutePath());
        } else {
            return null;
        }
    }

    private String getAppIConABS(@NonNull String packageName) {

        final String load = loadIcon(packageName);

        if (load != null && !load.isEmpty()) {
            return load;
        } else {
            Bitmap bitInit = getAppIconSubSer(packageName);

            int width = bitInit.getWidth();
            int height = bitInit.getHeight();

            if (width <= MaxRes && height <= MaxRes) {
                return saveIcon(bitInit, packageName);
            }

            final Bitmap newBit = Bitmap.createScaledBitmap(bitInit, MaxRes, MaxRes, true);
            return saveIcon(newBit, packageName);
        }
    }

    private Bitmap getAppIconSubSer(@NonNull String packageName) {
        Bitmap OGBit = getIconOGM(packageName);
        if (OGBit == null) {
            return getIconByPkgMain(context, packageName);
        } else {
            return OGBit;
        }
    }

    private Bitmap getIconOGM(@NonNull String packageName) {
        try {
            final PackageManager pm = context.getPackageManager();
            final ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            final Drawable appIcon = pm.getApplicationIcon(appInfo);
            return drawableToBitmap(appIcon);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private String getAppName(@NonNull String packageName) {
        final String cachedName = loadName(packageName);
        if (cachedName == null || cachedName.isEmpty()) {
            final String FIRSTName = getLaunchableActivity(packageName);
            if (FIRSTName == null || FIRSTName.isEmpty() || FIRSTName.contains(".") || FIRSTName.contains(packageName)) {
                final PackageManager pm = context.getPackageManager();
                ApplicationInfo appInfo;
                try {
                    appInfo = pm.getApplicationInfo(packageName, 0);
                    final String name = (String) pm.getApplicationLabel(appInfo);
                    saveName(packageName, name);
                    return name;
                } catch (PackageManager.NameNotFoundException e) {
                    return null;
                }
            } else {
                saveName(packageName, FIRSTName);
                return FIRSTName;
            }
        } else {
            return cachedName;
        }
    }

    private void startApp(int position, @NonNull String packageNameToLaunch) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageNameToLaunch);
        openIntent(position, launchIntent);
    }

    private void saveName(@NonNull String packageName, String name) {
        SharedPreferences prefs = context.getSharedPreferences("rv_cache", MODE_PRIVATE);
        prefs.edit().putString(packageName, name).apply();
    }

    private String loadName(@NonNull String packageName) {
        SharedPreferences prefs = context.getSharedPreferences("rv_cache", MODE_PRIVATE);
        return prefs.getString(packageName, null);
    }

    public void shutDown() {
    }

    @SuppressLint("NotifyDataSetChanged")
    public void replaceAllItems(List<String> appDataList, boolean keepCache) {
        final List<AppData> cl = convertListFormat(context, appDataList);
        if (keepCache) {
            if (!isSame(this.appDataList, cl)) {
                this.appDataList.clear();
                this.appDataList.addAll(cl);
                notifyDataSetChanged();
            }
        } else {
            this.appDataList.clear();
            this.appDataList.addAll(cl);
            notifyDataSetChanged();
        }
    }

    private boolean isSame(List<AppData> a, List<AppData> b) {
        if (a.size() != b.size()) return false;
        final List<AppData> ac = new ArrayList<>(a);
        final List<AppData> bc = new ArrayList<>(b);
        while (!bc.isEmpty()) {
            boolean isThere = false;
            final String PKG = bc.remove(0).getPackageName();
            for (int i = 0; i < ac.size(); i++) {
                if (ac.get(i).getPackageName().equals(PKG)) {
                    ac.remove(i);
                    isThere = true;
                    i = ac.size();
                }
            }
            if (!isThere) return false;
        }
        return true;
    }

    private void showPopupMenu(@NonNull View anchorView, @NonNull final String PKG, int position) {
        ContextThemeWrapper themedContext = new ContextThemeWrapper(anchorView.getContext(), R.style.MyPopupMenu);
        PopupMenu popup = new PopupMenu(themedContext, anchorView);
        popup.getMenuInflater().inflate(R.menu.uninstall_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_uninstall) {
                Uri packageUri = Uri.parse("package:" + PKG);
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
                anchorView.getContext().startActivity(uninstallIntent);
                return true;
            }
            if (item.getItemId() == R.id.menu_app_info) {
                openAppInfo(position, PKG);
            }
            return false;
        });
        popup.show();
    }

    public String getLaunchableActivity(String packageName) {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(packageName);

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);

        if (!activities.isEmpty()) {
            return activities.get(0).activityInfo.name;
        } else {
            return null;
        }
    }

    public void openAppInfo(int position, @NonNull String packageName) {
        try {
            // Construct the Intent to launch the app info screen
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            openIntent(position, intent);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "Unable to open App Info", Toast.LENGTH_SHORT).show();
        }
    }

    private float scaleHelper(int a, int b) {
        double p = ((double) a / (double) b);
        if (a > b) p = ((double) b / (double) a);
        if (a == b) return 0.5f;
        if (p >= 1.0) return 0.9f;
        if (p <= 0) return 0.1f;
        return (float) p;
    }

    private ScaleAnimation zoomIn_openIntent;
    private boolean zoomOut_openIntent_state = false;

    private void openIntent(int position, Intent intent) {
        if (intent != null) {
            if (position > -1 && position < getItemCount()) {

                final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    final View iconViewParent = layoutManager.findViewByPosition(position); // Positions start at index 0
                    if (iconViewParent != null) {

                        final View iconView = iconViewParent.findViewById(R.id.app_icon); // Positions start at index 0

                        if (iconView != null) {
                            final int[] location = new int[2];
                            iconView.getLocationOnScreen(location);
                            int xScreen = location[0];
                            int yScreen = location[1];
                            final int width = DisplayUtils.getDisplayWidth(context);
                            final int height = DisplayUtils.getDisplayHeight(context);
                            if (xScreen > width) xScreen = width - iconViewParent.getWidth();
                            if (xScreen < 0) xScreen = 0;//iconView.getWidth();
                            if (yScreen < 0) yScreen = 0;//iconView.getHeight();
                            if (yScreen > height) yScreen = height - iconViewParent.getHeight();

                            final float x = scaleHelper(xScreen, width);
                            final float y = scaleHelper(yScreen, height);

                            int ox = (int) (x * width) + iconView.getWidth() / 2;
                            int oy = (int) (y * height) + iconView.getHeight() / 2;

                            final ActivityOptions optionsMakeScaleUpAnimation = ActivityOptions.makeScaleUpAnimation(
                                    mainView,
                                    ox,
                                    oy,
                                    iconView.getWidth(),
                                    iconView.getHeight());

                            if (optionsMakeScaleUpAnimation != null) {
                                ScaleAnimation zoomOut_openIntent = new ScaleAnimation(1f, 2.25f, 1f, 2.25f, Animation.RELATIVE_TO_SELF, x, Animation.RELATIVE_TO_SELF, y);
                                zoomOut_openIntent.setDuration(640); // Adjust the duration as needed
                                zoomOut_openIntent.setFillAfter(false); // Keep the final state after the animation ends
                                zoomOut_openIntent.setInterpolator(animInterpolator);
                                zoomOut_openIntent.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {
                                        zoomOut_openIntent_state = true;
                                        LauncherApp.changeWallpaper = true;
                                        context.startActivity(intent, optionsMakeScaleUpAnimation.toBundle());
                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        LauncherApp.changeWallpaper = true;
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {
                                    }
                                });
                                zoomIn_openIntent = new ScaleAnimation(1.125f, 1.0f, 1.125f, 1.0f, Animation.RELATIVE_TO_SELF, x, Animation.RELATIVE_TO_SELF, y);
                                zoomIn_openIntent.setDuration(300); // Adjust the duration as needed
                                zoomIn_openIntent.setFillAfter(false); // Keep the final state after the animation ends
                                zoomIn_openIntent.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {
                                        LauncherApp.changeWallpaper = false;
                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {

                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {

                                    }
                                });
                                zoomIn_openIntent.setInterpolator(animInterpolator);
                                mainView.startAnimation(zoomOut_openIntent);
                            } else {
                                helpStart(intent);
                            }
                        } else {
                            helpStart(intent);
                        }
                    } else {
                        helpStart(intent);
                    }
                } else {
                    helpStart(intent);
                }
            } else {
                helpStart(intent);
            }
        }
    }

    private ScaleAnimation zoomIn_helpStart;
    private boolean zoomOut_helpStart_state = false;

    private void helpStart(Intent intent) {
        if (intent != null) {
            ScaleAnimation zoomOut_helpStart = getScaleAnimation(intent);
            zoomIn_helpStart = new ScaleAnimation(1.125f, 1.0f, 1.125f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            zoomIn_helpStart.setDuration(300); // Adjust the duration as needed
            zoomIn_helpStart.setFillAfter(false); // Keep the final state after the animation ends
            zoomIn_helpStart.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    LauncherApp.changeWallpaper = false;
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            zoomIn_helpStart.setInterpolator(animInterpolator);
            mainView.startAnimation(zoomOut_helpStart);
        }
    }

    @NonNull
    private ScaleAnimation getScaleAnimation(Intent intent) {
        ScaleAnimation zoomOut_helpStart = new ScaleAnimation(1f, 2.25f, 1f, 2.25f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        zoomOut_helpStart.setDuration(640); // Adjust the duration as needed
        zoomOut_helpStart.setFillAfter(false); // Keep the final state after the animation ends
        zoomOut_helpStart.setInterpolator(animInterpolator);
        zoomOut_helpStart.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                zoomOut_helpStart_state = true;
                LauncherApp.changeWallpaper = true;
                context.startActivity(intent);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                LauncherApp.changeWallpaper = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        return zoomOut_helpStart;
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();

        // Create a software-accelerated bitmap regardless of the drawable type
        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());

        // Ensure that the drawable is not hardware-backed
        if (drawable instanceof BitmapDrawable) {
            drawable.draw(canvas);
        } else {
            // Temporarily disable hardware acceleration
            int saveFlags = canvas.save();
            canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG, Paint.FILTER_BITMAP_FLAG));
            drawable.draw(canvas);
            canvas.restoreToCount(saveFlags);
        }

        return bitmap;
    }

    private Bitmap getIconByPkgMain(@NonNull Context context, @NonNull final String packageName) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return getIconByPkgorg(context, packageName);
        }
        Drawable appIconDrawable = applicationInfo.loadIcon(packageManager);
        if (appIconDrawable instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) appIconDrawable;
            Drawable foreground = adaptiveIcon.getForeground();
            Drawable background = adaptiveIcon.getBackground();
            if (foreground != null && background != null) {
                int width = adaptiveIcon.getIntrinsicWidth();
                int height = adaptiveIcon.getIntrinsicHeight();
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                background.setBounds(0, 0, width, height);
                background.draw(canvas);
                foreground.setBounds(0, 0, width, height);
                foreground.draw(canvas);
                return bitmap;
            } else {
                return getIconByPkgorg(context, packageName);
            }
        } else {
            return getIconByPkgorg(context, packageName);
        }
    }

    private Bitmap getIconByPkgorg(@NonNull Context context, @NonNull final String pkgName) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        Drawable appIcon = applicationInfo.loadIcon(packageManager);
        return drawableToBitmap(appIcon);
    }

    public void stopZoomAnimation() {
        if (zoomOut_openIntent_state) {
            zoomOut_openIntent_state = false;
            if (zoomIn_openIntent != null) {
                if(mainView!=null) {
                    mainView.startAnimation(zoomIn_openIntent);
                }
            }
        } else {
            if (zoomOut_helpStart_state) {
                zoomOut_helpStart_state = false;
                if (zoomIn_helpStart != null) {
                    if (mainView != null) {
                        mainView.startAnimation(zoomIn_helpStart);
                    }
                }
            }
        }
    }


}
