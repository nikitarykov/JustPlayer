package com.example.nikitarykov.justplayer.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;

import com.arellomobile.mvp.MvpDelegate;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.PresenterType;
import com.example.nikitarykov.justplayer.common.BitmapUtils;
import com.example.nikitarykov.justplayer.model.Track;
import com.example.nikitarykov.justplayer.R;
import com.example.nikitarykov.justplayer.presenter.PlayerPresenter;
import com.example.nikitarykov.justplayer.view.PlayerView;

public class PlayerService extends Service implements PlayerView {

    private static final int NOTIFICATION_ID = 101;

    @InjectPresenter(type = PresenterType.WEAK, tag = PlayerPresenter.TAG)
    PlayerPresenter playerPresenter;

    private MvpDelegate<PlayerService> delegate;

    @Override
    public void onCreate() {
        super.onCreate();
        delegate = new MvpDelegate<>(this);
        delegate.onCreate(null);
        delegate.onAttach();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (MediaButtonReceiver.handleIntent(playerPresenter.getMediaSession(), intent) == null) {
            playerPresenter.play();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onRebind(Intent intent) {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        delegate.onDetach();
        delegate.onDestroy();
        stopForeground(true);
    }

    public void buildNotification(boolean isPlaying) {

        int playPauseIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        String channelId = getString(R.string.notification_channel_id);

        MediaMetadataCompat mediaMetadata = playerPresenter.getMediaController().getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setShowWhen(false)
                .setOngoing(isPlaying)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new MediaStyle()
                        .setMediaSession(playerPresenter.getMediaController().getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setLargeIcon(description.getIconBitmap())
                .setSmallIcon(R.drawable.icon_music)
                .setContentText(description.getSubtitle())
                .setContentTitle(description.getTitle())
                .setSubText(description.getDescription())
                .setContentIntent(playerPresenter.getMediaController().getSessionActivity())
                .addAction(
                        new NotificationCompat.Action(android.R.drawable.ic_media_previous, "previous",
                                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
                .addAction(
                        new NotificationCompat.Action(playPauseIcon, "play_pause",
                                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                        PlaybackStateCompat.ACTION_PLAY_PAUSE)))
                .addAction(
                        new NotificationCompat.Action(android.R.drawable.ic_media_next, "next",
                                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))
                .setDeleteIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                PlaybackStateCompat.ACTION_STOP))
                .setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                .setOnlyAlertOnce(true)
                .build();

        if (isPlaying) {
            startForeground(NOTIFICATION_ID, notification);
        } else {
            NotificationManagerCompat.from(PlayerService.this).notify(NOTIFICATION_ID, notification);
            stopForeground(false);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = getString(R.string.notification_channel_id);
        String channelName = getString(R.string.notification_channel_name);
        String channelDescription = getString(R.string.notification_channel_description);
        if (notificationManager.getNotificationChannel(channelId) == null) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription(channelDescription);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public void setCurrentTrack(Track track) {
        updateMetaData(track);
    }

    @Override
    public void setPlaying(boolean isPlaying) {
        buildNotification(isPlaying);
    }

    @Override
    public void setCurrentPosition(int position) {
        // do nothing
    }

    @Override
    public void setRandom(boolean isRandom) {
        // do nothing
    }

    @Override
    public void setRepeat(boolean isRepeat) {
        // do nothing
    }

    private void updateMetaData(Track track) {
        playerPresenter.getMediaSession().setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART,
                        BitmapUtils.loadBitmap(this, track.getAlbumId()))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getTitle())
                .build());
    }
}

