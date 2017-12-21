package com.example.nikitarykov.justplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Created by Nikita Rykov on 01.10.2017.
 */

public class AudioService extends Service implements
		MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
		MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener, AudioManager.OnAudioFocusChangeListener {

    private static final int NOTIFICATION_ID = 101;
    private static final String NOTIFICATION_CLOSED = "com.example.nikitarykov.justplayer.NotificationClosed";
    private static final String NOTIFICATION_CHANNEL_ID = "com.example.nikitarykov.justplayer.NotificationChannelId";
    public static final String ACTION_PLAY = "com.example.nikitarykov.justplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.nikitarykov.justplayer.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.example.nikitarykov.justplayer.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.example.nikitarykov.justplayer.ACTION_NEXT";
    public static final String ACTION_STOP = "com.example.nikitarykov.justplayer.ACTION_STOP";

    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;
    private Random random = new Random();
    private OnUIListener onUIListener;

    private boolean isBound = false;
    private boolean isRepeat = false;
    private boolean isRandom = false;
    private MediaPlayer player;
	private IBinder audioBinder = new AudioBinder();
	private AudioManager manager;
	private int curItem;
	private int resumePosition;
	private List<AudioItem> items;
	private PlaybackStatus playbackStatus = PlaybackStatus.PAUSED;

    private boolean isActiveCall = false;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_RINGING:
                    if (player != null) {
                        pauseAudio();
                        isActiveCall = true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (player != null && isActiveCall) {
                        isActiveCall = false;
                        resumeAudio();
                    }
                    break;
            }
        }
    };

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        pauseAudio();
        buildNotification();
        }
    };

    private BroadcastReceiver closeNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
        }
    };

	@Override
	public void onCreate() {
		super.onCreate();
        manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setCallStateListener();
        registerBecomingNoisyReceiver();
        registerCloseNotificationReciever();
	}

	@Override
	public IBinder onBind(Intent intent) {
        isBound = true;
	    return audioBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
        isBound = false;
		if (playbackStatus == PlaybackStatus.PLAYING) {
            buildNotification();
        } else {
		    stopSelf();
        }
	    return false;
	}

    @Override
    public void onRebind(Intent intent) {
        removeNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
        }

        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) {
            stopAudio();
            player.release();
        }
        removeAudioFocus();

        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();

        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(closeNotificationReceiver);

        stopForeground(true);
    }

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
        if (!isRepeat) {
            if (!isRandom) {
                curItem++;
                if (curItem == items.size()) {
                    curItem = 0;
                }
            } else {
                curItem = random.nextInt(items.size());
            }
        }
		playAudio();
	}

	@Override
	public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		player.start();
        playbackStatus = PlaybackStatus.PLAYING;
        if (onUIListener != null) {
            onUIListener.setItem(items.get(curItem));
            onUIListener.updateUI();
            onUIListener.setPlaybackStatus(playbackStatus);
        }
        updateMetaData();
        buildNotification();
	}

	public void playAudio() {
        player.reset();
        AudioItem item = items.get(curItem);
        try {
            player.setDataSource(item.getData());
            player.prepareAsync();
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
            stopSelf();
        }
	}

    private void stopAudio() {
        if (player != null && player.isPlaying()) {
            player.stop();
            playbackStatus = PlaybackStatus.PAUSED;
        }
    }

    private void pauseAudio() {
        if (player != null && player.isPlaying()) {
            player.pause();
            resumePosition = player.getCurrentPosition();
            playbackStatus = PlaybackStatus.PAUSED;
            buildNotification();
        }
    }

    private void resumeAudio() {
        if (!player.isPlaying()) {
            player.seekTo(resumePosition);
            player.start();
            playbackStatus = PlaybackStatus.PLAYING;
            buildNotification();
        }
    }

	private void initPlayer() {
	    if (player == null) {
            player = new MediaPlayer();
            player.setWakeMode(getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            player.setOnPreparedListener(this);
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
            player.setOnSeekCompleteListener(this);
        }
	}

	@Override
	public void onSeekComplete(MediaPlayer mediaPlayer) {

	}

    @Override
    public void onAudioFocusChange(int state) {
        switch (state) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (player == null) {
                    initPlayer();
                    playAudio();
                } else if (!player.isPlaying()) {
                    player.start();
                }
                player.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (player.isPlaying()) {
                    player.stop();
                }
                player.release();
                player = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (player.isPlaying()) {
                    player.pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (player.isPlaying()) {
                    player.setVolume(0.1f, 0.1f);
                }
                break;
        }
    }

    private boolean requestAudioFocus() {
        int result = manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private boolean removeAudioFocus() {
        return manager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void registerCloseNotificationReciever() {
        IntentFilter intentFilter = new IntentFilter(NOTIFICATION_CLOSED);
        registerReceiver(closeNotificationReceiver, intentFilter);
    }

    private void registerBecomingNoisyReceiver() {
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    private void setCallStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public int getCurItem() {
        return curItem;
    }

    public void setCurItem(int curItem) {
        this.curItem = curItem;
    }

    public List<AudioItem> getItems() {
        return items;
    }

    public void setItems(List<AudioItem> items) {
        this.items = items;
    }

    public class AudioBinder extends Binder {
		AudioService getService() {
			return AudioService.this;
		}
	}

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) {
            return;
        }

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mediaSession = new MediaSessionCompat(getApplicationContext(), "JustPlayer");
        transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSession.setCallback(new MediaSessionCompat.Callback() {

            @Override
            public void onPlay() {
                super.onPlay();
                resumeAudio();
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseAudio();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    public void updateMetaData() {
	    AudioItem item = items.get(curItem);
        Uri albumartUri = Uri.parse(getApplicationContext().getString(R.string.albumart_uri));
        Uri coverUri = ContentUris.withAppendedId(albumartUri, item.getAlbumId());
        Bitmap albumArt;
        try {
            albumArt = MediaStore.Images.Media.getBitmap(getContentResolver(), coverUri);
        } catch (IOException e) {
            albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.icon_music);
        }

        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, item.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, item.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.getTitle())
                .build());
    }

    public AudioItem skipToNext() {
	    if (!isRepeat) {
            if (!isRandom) {
                    curItem++;
                if (curItem == items.size()) {
                    curItem = 0;
                }
            } else {
                curItem = random.nextInt(items.size());
            }
        }

        stopAudio();
        playAudio();
        return items.get(curItem);
    }

    public AudioItem skipToPrevious() {
        if (!isRepeat) {
            if (!isRandom) {
                curItem--;
                if (curItem == -1) {
                    curItem = items.size() - 1;
                }
            } else {
                curItem = random.nextInt(items.size());
            }
        }
        stopAudio();
        playAudio();
        return items.get(curItem);
    }

    public void changePlaybackStatus() {
	    if (playbackStatus == PlaybackStatus.PLAYING) {
	        pauseAudio();
        } else {
	        resumeAudio();
        }
    }

    public void setRepeat() {
        isRepeat = !isRepeat;
    }

    public void setRandom() {
        isRandom = !isRandom;
    }

    private void buildNotification() {

        int notificationAction = android.R.drawable.ic_media_pause;
        PendingIntent playPauseAction = null;

        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            playPauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            playPauseAction = playbackAction(0);
        }

        AudioItem item = items.get(curItem);
        Uri albumartUri = Uri.parse(getApplicationContext().getString(R.string.albumart_uri));
        Uri coverUri = ContentUris.withAppendedId(albumartUri, item.getAlbumId());
        Bitmap albumArt;
        try {
            albumArt = MediaStore.Images.Media.getBitmap(getContentResolver(), coverUri);
        } catch (IOException e) {
            albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.icon_music);
        }

        Intent deleteIntent = new Intent(NOTIFICATION_CLOSED);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                2323, deleteIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setShowWhen(false)
                .setOngoing(playbackStatus == PlaybackStatus.PLAYING || isBound)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession.getSessionToken()))
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setLargeIcon(albumArt)
                .setSmallIcon(item.getAlbumId().intValue())
                .setContentText(item.getArtist())
                .setContentTitle(item.getTitle())
                .setContentInfo(item.getAlbum())
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", playPauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2))
                .setDeleteIntent(pendingIntent)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, AudioService.class);
        switch (actionNumber) {
            case 0:
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) {
            return;
        }
        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
            playbackStatus = PlaybackStatus.PLAYING;
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
            playbackStatus = PlaybackStatus.PAUSED;
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
            playbackStatus = PlaybackStatus.PLAYING;
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
            playbackStatus = PlaybackStatus.PLAYING;
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
            playbackStatus = PlaybackStatus.PAUSED;
        }
        if (onUIListener != null) {
            onUIListener.setItem(items.get(curItem));
            onUIListener.updateUI();
            onUIListener.setPlaybackStatus(playbackStatus);
        }
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    public MediaControllerCompat.TransportControls getTransportControls() {
	    return transportControls;
    }

    public static enum PlaybackStatus {
        PLAYING,
        PAUSED
    }

    public void setOnUIListener(OnUIListener listener) {
	    onUIListener = listener;
    }

    public interface OnUIListener {
	    void updateUI();
	    void setItem(AudioItem item);
	    void setPlaybackStatus(PlaybackStatus status);
    }
}
