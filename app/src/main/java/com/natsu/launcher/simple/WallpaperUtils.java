package com.natsu.launcher.simple;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class WallpaperUtils {

    public static final String WALLPAPER_FILENAME = "app_wallpaper.png";
    public static final String VIDEO_WALLPAPER_FILENAME = "app_video_wallpaper";

    public static void saveWallpaper(@NonNull Context context, @NonNull final View mainView, @NonNull Uri imageUri) {
        resetWallpaper(context, mainView);
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

        final Bitmap originalBitmap = loadWallpaper(context);
        mainView.getContext().getMainExecutor().execute(() -> {
            try {
                ((ImageView) mainView.findViewById(R.id.img_view_app_wallpaper)).setScaleType(ImageView.ScaleType.CENTER_CROP);
                ((ImageView) mainView.findViewById(R.id.img_view_app_wallpaper)).setImageBitmap(originalBitmap);
                mainView.findViewById(R.id.img_view_app_wallpaper).setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Toast.makeText(context, "null wallpaper", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void resetWallpaper(@NonNull Context context, @NonNull View mainView) {
        File wallpaperFile = new File(context.getFilesDir(), WALLPAPER_FILENAME);
        if (wallpaperFile.exists()) {
            wallpaperFile.delete();
        }
        File wallpaperVideoFile = new File(context.getFilesDir(), VIDEO_WALLPAPER_FILENAME+getVideoWallpaperExt(context));
        if (wallpaperVideoFile.exists()) {
            wallpaperVideoFile.delete();
        }

        mainView.getContext().getMainExecutor().execute(() -> {
            final PlayerView videoView = mainView.findViewById(R.id.video_background_exo);
            final ImageView imageView = mainView.findViewById(R.id.img_view_app_wallpaper);
            imageView.setImageResource(R.drawable.fully_trans);
            videoView.setVisibility(View.GONE);
        });
    }

    public static boolean isVideoAvailable(Context context) {
        File wallpaperVideoFile = new File(context.getFilesDir(), VIDEO_WALLPAPER_FILENAME+getVideoWallpaperExt(context));
        return wallpaperVideoFile.exists();
    }

    public static Bitmap loadWallpaper(@NonNull Context context) {
        File wallpaperFile = new File(context.getFilesDir(), WALLPAPER_FILENAME);
        if (wallpaperFile.exists()) {
            return BitmapFactory.decodeFile(wallpaperFile.getAbsolutePath());
        } else {
            return null; // Indicate no wallpaper set
        }
    }

    public static void saveVideoWallpaper(@NonNull Context context, @NonNull ExoPlayer player, @NonNull View mainView, @NonNull Uri videoUri, @NonNull final String ext) {
        resetWallpaper(context, mainView);

        try (InputStream inputStream = context.getContentResolver().openInputStream(videoUri);
             OutputStream outputStream = Files.newOutputStream(new File(context.getFilesDir(), VIDEO_WALLPAPER_FILENAME+getVideoWallpaperExt(context)).toPath())) {

            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                playVideoFromPrivateStorage(context, player, mainView, ext);

            }

        } catch (IOException ignored) {
        }
    }

    private static void playVideoFromPrivateStorage(@NonNull Context context, @NonNull ExoPlayer player, @NonNull View mainView, @NonNull final String ext) {
        final File videoFile = new File(context.getFilesDir(), VIDEO_WALLPAPER_FILENAME + ext);
        setVideoWallpaperExt(context, ext);
        if (videoFile.exists()) {
            final Uri videoUri = Uri.fromFile(videoFile);
            mainView.getContext().getMainExecutor().execute(() -> {
                final PlayerView videoView = mainView.findViewById(R.id.video_background_exo);
                if (videoView != null) {
                    //player.release();
                    player.setMediaItem(MediaItem.fromUri(videoUri));
                    player.setVolume(0.0f);
                    player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                    player.prepare();
                    player.play();
                    videoView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private static void setVideoWallpaperExt(Context context, String ext) {
        if (!ext.contains(".")) ext = "." + ext;
        SharedPreferences prefs = context.getSharedPreferences("home_wallpaper_video", MODE_PRIVATE);
        prefs.edit().putString("ext", ext).apply();
    }

    public static String getVideoWallpaperExt(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("home_wallpaper_video", MODE_PRIVATE);
        return prefs.getString("ext", ".mp4");
    }

}

