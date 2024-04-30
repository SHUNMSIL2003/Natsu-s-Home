package com.android.natsu.launcher.simple;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class WallpaperUtils {

    private static final String WALLPAPER_FILENAME = "app_wallpaper.png";

    public static void saveWallpaper(Context context, Uri imageUri) {
        Bitmap bitmap;
        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            return; // Indicate failure
        }

        File wallpaperFile = new File(context.getFilesDir(), WALLPAPER_FILENAME);
        try (FileOutputStream outputStream = new FileOutputStream(wallpaperFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (IOException ignored) {
        }
    }

    public static Bitmap loadWallpaper(Context context) {
        File wallpaperFile = new File(context.getFilesDir(), WALLPAPER_FILENAME);
        if (wallpaperFile.exists()) {
            return BitmapFactory.decodeFile(wallpaperFile.getAbsolutePath());
        } else {
            return null; // Indicate no wallpaper set
        }
    }
}

