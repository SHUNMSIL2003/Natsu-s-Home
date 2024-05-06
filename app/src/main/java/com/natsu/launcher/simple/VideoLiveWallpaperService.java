package com.natsu.launcher.simple;

import android.net.Uri;
import android.service.wallpaper.WallpaperService;

import android.view.SurfaceHolder;
import android.view.ViewGroup;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;



public class VideoLiveWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new VideoWallpaperEngine();
    }

    private class VideoWallpaperEngine extends Engine {
        private ExoPlayer exoPlayer;
        private PlayerView playerView;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            playerView = new PlayerView(VideoLiveWallpaperService.this);
            Uri uri = WallpaperUtils.getVideoWallpaperUri(getBaseContext());
            if(uri!=null) {
                initializePlayer(uri);
            }
        }

        private void initializePlayer(Uri uri) {
            exoPlayer = new ExoPlayer.Builder(VideoLiveWallpaperService.this).build();
            playerView.setPlayer(exoPlayer);
            playVideo(uri);
        }

        private void playVideo(Uri uri) {
            MediaItem mediaItem = MediaItem.fromUri(uri);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
            exoPlayer.setVolume(0.0f);
            exoPlayer.prepare();
            exoPlayer.setPlayWhenReady(true);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            exoPlayer.setPlayWhenReady(visible);
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            playerView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            exoPlayer.setVideoSurfaceHolder(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
            }
        }

    }
}

