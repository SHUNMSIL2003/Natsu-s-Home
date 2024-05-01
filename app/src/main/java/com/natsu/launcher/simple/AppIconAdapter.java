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
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class AppIconAdapter extends RecyclerView.Adapter<AppIconAdapter.AppIconViewHolder> {

    final private List<AppData> appDataList = new ArrayList<>();
    final private Context context;
    final private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2); // Adjust thread pool size as needed
    final private Executor mainExecutor;
    final private RecyclerView recyclerView;
    final private View mainView;

    public AppIconAdapter(@NonNull Context context, @NonNull List<String> appDataList, RecyclerView recyclerView, View mainView) {
        this.context = context;
        this.mainExecutor = context.getMainExecutor();
        this.recyclerView = recyclerView;
        this.mainView = mainView;
        this.appDataList.addAll(convertListFormat(appDataList));
    }

    private List<AppData> convertListFormat(List<String> list) {
        final List<AppData> newList = new ArrayList<>();
        final String appPKG = context.getPackageName();
        while (!list.isEmpty()) {
            final String item = list.remove(0);
            if (!item.equals(appPKG)) newList.add(new AppData(item));
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
        final int finalPosition = position;
        if (position < appDataList.size() && position >= 0) {
            executor.execute(() -> {
                final AppData appData = appDataList.get(finalPosition);
                if (appData.getAppName() == null) {
                    appData.setAppName(getAppName(appData.getPackageName()));
                }
                if (appData.getAppIcon() == null) {
                    appData.setAppIcon(getAppIcon(appData.getPackageName()));
                }
                if (appDataList.get(finalPosition) != appData) {
                    appDataList.set(finalPosition, appData);
                }
                mainExecutor.execute(() -> {
                    if (holder.appNameView != null && appData.getAppName() != null)
                        holder.appNameView.setText(appData.getAppName());
                    if (holder.appIconView != null && appData.getAppIcon() != null)
                        holder.appIconView.setImageBitmap(appData.getAppIcon());
                    if (holder.appIconView != null && appData.getAppName() != null)
                        holder.appIconView.setContentDescription(appData.getAppName());
                    if (holder.appIconView != null) holder.appIconView.setOnClickListener(v -> {
                        final String PKG = appData.getPackageName();
                        startApp(finalPosition, PKG);
                    });
                    if (holder.appIconView != null && appData.getPackageName() != null)
                        holder.appIconView.setOnLongClickListener(v -> {
                            showPopupMenu(v, appData.getPackageName(), finalPosition);
                            return true;
                        });
                });
            });
        }
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

    private void saveIcon(Bitmap icon, @NonNull String packageName) {
        File wallpaperFile = new File(context.getFilesDir(), packageName + ".png");
        try (FileOutputStream outputStream = new FileOutputStream(wallpaperFile)) {
            icon.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (IOException ignored) {
        }
    }

    private Bitmap loadIcon(@NonNull String packageName) {
        File wallpaperFile = new File(context.getFilesDir(), packageName + ".png");
        if (wallpaperFile.exists()) {
            return BitmapFactory.decodeFile(wallpaperFile.getAbsolutePath());
        } else {
            return null;
        }
    }

    private Bitmap getAppIcon(@NonNull String packageName) {
        final Bitmap cachedIcon = loadIcon(packageName);
        if (cachedIcon == null) {
            try {
                final PackageManager pm = context.getPackageManager();
                final ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                final Drawable appIcon = pm.getApplicationIcon(appInfo);
                final Bitmap appIconBit = ((BitmapDrawable) appIcon).getBitmap();
                if (appIconBit != null) {
                    saveIcon(appIconBit, packageName);
                }
                return appIconBit;
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        } else {
            return cachedIcon;
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
        executor.shutdown();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void replaceAllItems(List<String> appDataList, boolean keepCache) {
        final List<AppData> cl = convertListFormat(appDataList);
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

    private float scaleHelper(int a, int b){
        double p = ((double)a /(double)b);
        if(a>b) p = ((double)b /(double)a);
        if(a==b) return 0.5f;
        if(p>=1.0) return 0.9f;
        if(p<=0) return 0.1f;
        return (float) p;
    }

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

                            int ox = (int) (x*width) + iconView.getWidth()/2;
                            int oy = (int) (y*height) + iconView.getHeight()/2;

                            final ActivityOptions optionsMakeScaleUpAnimation = ActivityOptions.makeScaleUpAnimation(
                                    mainView,
                                    ox,
                                    oy,
                                    iconView.getWidth(),
                                    iconView.getHeight());

                            if (optionsMakeScaleUpAnimation != null) {
                                ScaleAnimation zoomOut = new ScaleAnimation(1f, 2.25f, 1f, 2.25f, Animation.RELATIVE_TO_SELF, x, Animation.RELATIVE_TO_SELF, y);
                                zoomOut.setDuration(640); // Adjust the duration as needed
                                zoomOut.setFillAfter(false); // Keep the final state after the animation ends
                                zoomOut.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {
                                        context.startActivity(intent, optionsMakeScaleUpAnimation.toBundle());
                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {}
                                });
                                mainView.startAnimation(zoomOut);
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

    private void helpStart(Intent intent) {
        if (intent != null) {
            ScaleAnimation zoomOut = new ScaleAnimation(1f, 1.5f, 1f, 1.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            zoomOut.setDuration(2000); // Adjust the duration as needed
            zoomOut.setFillAfter(true); // Keep the final state after the animation ends
            zoomOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    context.startActivity(intent);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            mainView.startAnimation(zoomOut);
        }
    }


}
