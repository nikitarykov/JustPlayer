package com.example.nikitarykov.justplayer.presenter;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;
import com.example.nikitarykov.justplayer.app.JustPlayerApp;
import com.example.nikitarykov.justplayer.model.Track;
import com.example.nikitarykov.justplayer.model.TrackManager;
import com.example.nikitarykov.justplayer.ui.PlayerService;
import com.example.nikitarykov.justplayer.view.PlayerView;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.io.IOException;

import javax.inject.Inject;

@InjectViewState
public class PlayerPresenter extends MvpPresenter<PlayerView> implements AudioManager.OnAudioFocusChangeListener,
             MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    public static final String TAG = "PLAYER";

    @Inject
    TrackManager trackManager;

    @Inject
    Context context;

    private AudioManager audioManager;
    private TelephonyManager telephonyManager;
    private MediaSessionCompat mediaSession;
    private MediaPlayer mediaPlayer;
    private boolean isActiveCall = false;

    private PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_STOP);

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_RINGING:
                    if (mediaPlayer != null) {
                        play();
                        isActiveCall = true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (mediaPlayer != null && isActiveCall) {
                        isActiveCall = false;
                        pause();
                    }
                    break;
            }
        }
    };

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pause();
        }
    };

    private Handler handler = new Handler();
    private Runnable task = new Runnable() {

        @Override
        public void run() {
            if(mediaPlayer != null) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                getViewState().setCurrentPosition(currentPosition);
            }
            handler.postDelayed(this, 500);
        }
    };

    public PlayerPresenter() {
        JustPlayerApp.getAppComponent().inject(this);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        registerBecomingNoisyReceiver();
    }

    public MediaSessionCompat getMediaSession() {
        return mediaSession;
    }

    public MediaControllerCompat getMediaController() {
        return mediaSession.getController();
    }

    public void play() {
        if (mediaSession == null) {
            initMediaSession();
            initMediaPlayer();
        }
        mediaPlayer.reset();
        Track track = trackManager.getCurTrack();
        try {
            mediaPlayer.setDataSource(track.getData());
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            mediaSession.setActive(true);
            mediaPlayer.prepareAsync();
            getViewState().setCurrentTrack(track);
            getViewState().setPlaying(true);
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
            context.stopService(new Intent(context, PlayerService.class));
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            handler.removeCallbacks(task);
            trackManager.setResumePosition(mediaPlayer.getCurrentPosition());
            getViewState().setPlaying(false);
        }
    }

    public void stop() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            handler.removeCallbacks(task);
        }
        context.stopService(new Intent(context, PlayerService.class));
    }

    public void resume() {
        if (!mediaPlayer.isPlaying()) {
            if (requestAudioFocus()) {
                setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                mediaSession.setActive(true);
                mediaPlayer.seekTo(trackManager.getResumePosition());
                mediaPlayer.start();
                handler.post(task);
                getViewState().setPlaying(true);
            }
        }
    }

    public void skipToPrevious() {
        trackManager.previousTrack();
        setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);
        play();
    }

    public void skipToNext() {
        trackManager.nextTrack();
        setMediaPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT);
        play();
    }

    public void updateCurrentPosition(int position) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(position);
        } else {
            trackManager.setResumePosition(position);
        }
        getViewState().setCurrentPosition(position);
    }

    public void setRepeat(boolean isRepeat) {
        trackManager.setRepeat(!isRepeat);
        getViewState().setRepeat(!isRepeat);
    }

    public void setRandom(boolean isRandom) {
        trackManager.setRandom(!isRandom);
        getViewState().setRandom(!isRandom);
    }

    private void initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setWakeMode(context.getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
        }
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(context, "JustPlayer");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {

            @Override
            public void onPlay() {
                super.onPlay();
                resume();
            }

            @Override
            public void onPause() {
                super.onPause();
                pause();
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
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(context, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, mediaButtonIntent, 0);
        mediaSession.setMediaButtonReceiver(pendingIntent);
        mediaSession.setActive(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stop();
            mediaSession.release();
            mediaPlayer.release();
        }
        removeAudioFocus();

        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        context.unregisterReceiver(becomingNoisyReceiver);
    }

    @Override
    public void onAudioFocusChange(int state) {
        switch (state) {
            case AudioManager.AUDIOFOCUS_GAIN:
                resume();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.setVolume(0.1f, 0.1f);
                }
                break;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        trackManager.nextTrack();
        play();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if (requestAudioFocus()) {
            mediaPlayer.start();
            handler.post(task);
            getViewState().setCurrentTrack(trackManager.getCurTrack());
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    private void registerBecomingNoisyReceiver() {
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        context.registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    private boolean requestAudioFocus() {
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private boolean removeAudioFocus() {
        return audioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void setMediaPlaybackState(int state) {
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mediaSession.setPlaybackState(playbackstateBuilder.build());
    }
}
