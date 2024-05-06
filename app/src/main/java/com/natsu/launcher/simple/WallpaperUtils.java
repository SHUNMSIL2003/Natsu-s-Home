package com.natsu.launcher.simple;

import static android.content.Context.MODE_PRIVATE;

import android.app.WallpaperManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class WallpaperUtils {

    public static final String WALLPAPER_FILENAME = ".multi.com.natsu.launcher.simple.data.app_image_wallpaper.png";
    public static final String VIDEO_WALLPAPER_FILENAME = "singular.com.natsu.launcher.simple.data.app_video_wallpaper";

    public static void saveWallpaper(@NonNull Context context, @NonNull final View mainView, @NonNull Uri imageUri) {
        LauncherApp.setLoadingState(true);
        resetWallpaper(context, mainView);
        Bitmap bitmap;
        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            return; // Indicate failure
        }

        File wallpaperFile = new File(context.getDataDir(), "0"+WALLPAPER_FILENAME);
        try (FileOutputStream outputStream = new FileOutputStream(wallpaperFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (IOException ignored) {
        }

        final ImageView wallpaper = mainView.findViewById(R.id.img_view_app_wallpaper);
        final Bitmap wallpaperBitmap = loadWallpaper(context);
        if (wallpaperBitmap != null) {
            if(LauncherUtils.isHomeScreenApp(context)){
                if(wallpaper.getVisibility()!=View.GONE) wallpaper.setVisibility(View.GONE);
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

                try {
                    wallpaperManager.setBitmap(wallpaperBitmap);
                    context.getMainExecutor().execute(() -> LauncherApp.setLoadingState(false));
                } catch (IOException e) {
                    context.getMainExecutor().execute(() -> {
                        if(wallpaper.getVisibility()!=View.VISIBLE) wallpaper.setVisibility(View.VISIBLE);
                        wallpaper.setImageBitmap(wallpaperBitmap);
                        LauncherApp.setLoadingState(false);
                    });
                }

            }else {
                context.getMainExecutor().execute(() -> {
                    if(wallpaper.getVisibility()!=View.VISIBLE) wallpaper.setVisibility(View.VISIBLE);
                    wallpaper.setImageBitmap(wallpaperBitmap);
                    LauncherApp.setLoadingState(false);
                });
            }

        }
    }

    public static void saveMultiWallpapers(@NonNull Context context, @NonNull final View mainView, @NonNull ClipData clipData) {
        LauncherApp.setLoadingState(true);
        resetWallpaper(context, mainView);
        for (int i = 0; i < clipData.getItemCount(); i++) {
            Uri imageUri = clipData.getItemAt(i).getUri();
            Bitmap bitmap;
            try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
                bitmap = BitmapFactory.decodeStream(inputStream);
                File wallpaperFile = new File(context.getDataDir(), i+WALLPAPER_FILENAME);
                try (FileOutputStream outputStream = new FileOutputStream(wallpaperFile)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                } catch (IOException ignored) {
                }
            } catch (IOException ignored) {
            }

        }

        final ImageView wallpaper = mainView.findViewById(R.id.img_view_app_wallpaper);
        final Bitmap wallpaperBitmap = loadWallpaper(context);
        if (wallpaperBitmap != null) {
            if(LauncherUtils.isHomeScreenApp(context)){
                if(wallpaper.getVisibility()!=View.GONE) wallpaper.setVisibility(View.GONE);
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

                try {
                    wallpaperManager.setBitmap(wallpaperBitmap);
                    context.getMainExecutor().execute(() -> LauncherApp.setLoadingState(false));
                } catch (IOException e) {
                    context.getMainExecutor().execute(() -> {
                        if(wallpaper.getVisibility()!=View.VISIBLE) wallpaper.setVisibility(View.VISIBLE);
                        wallpaper.setImageBitmap(wallpaperBitmap);
                        LauncherApp.setLoadingState(false);
                    });
                }

            }else {
                context.getMainExecutor().execute(() -> {
                    if(wallpaper.getVisibility()!=View.VISIBLE) wallpaper.setVisibility(View.VISIBLE);
                    wallpaper.setImageBitmap(wallpaperBitmap);
                    LauncherApp.setLoadingState(false);
                });
            }

        }
    }

    public static void resetWallpaper(@NonNull Context context, @NonNull View mainView) {
        LauncherApp.wallpaperImageFiles.clear();
        LauncherApp.usedWallpapers.clear();
        deleteFilesByName(context.getDataDir().getAbsolutePath(),WALLPAPER_FILENAME);
        File wallpaperVideoFile = new File(context.getDataDir(), VIDEO_WALLPAPER_FILENAME+getVideoWallpaperExt(context));
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

    public static void deleteFilesByName(String targetDirectoryPath, String fileNamePattern) {
        File directory = new File(targetDirectoryPath);

        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().contains(fileNamePattern)) {
                        file.delete();
                    }
                }
            }
        }
    }

    public static boolean isVideoAvailable(Context context) {
        File wallpaperVideoFile = new File(context.getDataDir(), VIDEO_WALLPAPER_FILENAME+getVideoWallpaperExt(context));
        return wallpaperVideoFile.exists();
    }
    public static Uri getVideoWallpaperUri(Context context) {
        File wallpaperVideoFile = new File(context.getDataDir(), VIDEO_WALLPAPER_FILENAME+getVideoWallpaperExt(context));
        if(wallpaperVideoFile.exists()) {
            return Uri.fromFile(wallpaperVideoFile);
        }
        return null;
    }
    public static Bitmap loadWallpaper(@NonNull Context context) {
        if(LauncherApp.wallpaperImageFiles.isEmpty()){
            LauncherApp.wallpaperImageFiles.addAll(getMatchingFilePaths(context.getDataDir().getAbsolutePath(),WALLPAPER_FILENAME));
        }
        List<String> wallpaperImages = new ArrayList<>(LauncherApp.wallpaperImageFiles);
        if(LauncherApp.usedWallpapers.size()==wallpaperImages.size()){
            LauncherApp.usedWallpapers.clear();
        }
        if(!wallpaperImages.isEmpty()) {
            final Random random = new Random();
            final int randomIndex = random.nextInt(wallpaperImages.size());

            final String currWall = wallpaperImages.remove(randomIndex);
            File wallpaperFile = new File(currWall);
            if(wallpaperFile.exists()){
                final String currWallName = wallpaperFile.getName();
                if (!LauncherApp.usedWallpapers.contains(currWallName)) {
                    LauncherApp.usedWallpapers.add(currWallName);
                    wallpaperImages.clear();
                    return BitmapFactory.decodeFile(wallpaperFile.getAbsolutePath());
                }
            }
        }
        LauncherApp.usedWallpapers.clear();
        wallpaperImages = new ArrayList<>(LauncherApp.wallpaperImageFiles);
        if(!wallpaperImages.isEmpty()) {
            final Random random = new Random();
            final int randomIndex = random.nextInt(wallpaperImages.size());

            final String currWall = wallpaperImages.remove(randomIndex);
            File wallpaperFile = new File(currWall);
            if(wallpaperFile.exists()){
                final String currWallName = wallpaperFile.getName();
                if (!LauncherApp.usedWallpapers.contains(currWallName)) {
                    LauncherApp.usedWallpapers.add(currWallName);
                    wallpaperImages.clear();
                    return BitmapFactory.decodeFile(wallpaperFile.getAbsolutePath());
                }
            }
        }
        return null;
    }

    public static List<String> getMatchingFilePaths(String directoryPath, String filenamePattern) {
        List<String> matchingFiles = new ArrayList<>();
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().contains(filenamePattern)) {
                        matchingFiles.add(file.getAbsolutePath());
                    }
                }
            }
        }

        return matchingFiles;
    }

    public static void saveVideoWallpaper(@NonNull Context context, @NonNull ExoPlayer player, @NonNull View mainView, @NonNull Uri videoUri, @NonNull final String ext) {
        LauncherApp.setLoadingState(true);
        resetWallpaper(context, mainView);

        try (InputStream inputStream = context.getContentResolver().openInputStream(videoUri);
             OutputStream outputStream = Files.newOutputStream(new File(context.getDataDir(), VIDEO_WALLPAPER_FILENAME+getVideoWallpaperExt(context)).toPath())) {

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
        LauncherApp.setLoadingState(false);
    }

    private static void playVideoFromPrivateStorage(@NonNull Context context, @NonNull ExoPlayer player, @NonNull View mainView, @NonNull final String ext) {
        final File videoFile = new File(context.getDataDir(), VIDEO_WALLPAPER_FILENAME + ext);
        setVideoWallpaperExt(context, ext);
        if (videoFile.exists()) {
            final Uri videoUri = Uri.fromFile(videoFile);
            mainView.getContext().getMainExecutor().execute(() -> {
                if(LauncherUtils.isHomeScreenApp(context)){
                    Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                    String pkg = Objects.requireNonNull(VideoLiveWallpaperService.class.getPackage()).getName();
                    String cls = VideoLiveWallpaperService.class.getCanonicalName();
                    intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(pkg, cls));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);

                } else {
                    final PlayerView videoView = mainView.findViewById(R.id.video_background_exo);
                    if (videoView != null) {
                        player.setMediaItem(MediaItem.fromUri(videoUri));
                        player.setVolume(0.0f);
                        player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                        player.prepare();
                        player.play();
                        videoView.setVisibility(View.VISIBLE);
                    }
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

