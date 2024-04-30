package com.android.natsu.launcher.simple;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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


    public AppIconAdapter(@NonNull Context context, @NonNull List<String> appDataList) {
        this.context = context;
        this.mainExecutor = context.getMainExecutor();
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
                        startApp(PKG);
                    });
                    if (holder.appIconView != null && appData.getPackageName() != null)
                        holder.appIconView.setOnLongClickListener(v -> {
                            showPopupMenu(v, appData.getPackageName());
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
            return cachedName;
        }
    }

    private void startApp(@NonNull String packageNameToLaunch) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageNameToLaunch);
        if (launchIntent != null) {
            context.startActivity(launchIntent);
        }
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
    public void replaceAllItems(List<String> appDataList) {
        final List<AppData> cl = convertListFormat(appDataList);
        this.appDataList.clear();
        this.appDataList.addAll(cl);
        notifyDataSetChanged();
    }

    private void showPopupMenu(View anchorView, final String PKG) {
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
            return false;
        });
        popup.show();
    }

}

