package com.example.nikitarykov.justplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Media;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nikita Rykov on 30.09.2017.
 */



public class MainActivity extends FragmentActivity implements PlaylistFragment.OnPlaylistItemSelectedListener,
        PlayerFragment.OnPlayPauseListener, PlayerFragment.OnNextListener, PlayerFragment.OnPrevListener,
        PlayerFragment.OnRandomListener, PlayerFragment.OnRepeatListener, PlayerFragment.OnPlaylistOpenListener {

    public static final String PLAY_NEW_AUDIO = "com.example.nikitarykov.justplayer.PlayNewAudio";

    private static final int REQUEST_PERMISSION_CODE = 61125;
    private static final String STATE_IN_PERMISSION = "inPermission";
    private boolean isInPermission = false;
    private PlaylistFragment playlistFragment;
    private PlayerFragment playerFragment;

    private static final String[] INITIAL_PERMS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private boolean shouldCreateFragmentOnCreate = false;
    private List<AudioItem> audioList;
    private AudioService audioService;
	private int curItem = 0;
	private boolean isServiceBound = false;
    private ServiceConnection audioConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioService.AudioBinder binder = (AudioService.AudioBinder)service;
            audioService = binder.getService();
            isServiceBound = true;
            audioService.setItems(audioList);
            audioService.setCurItem(curItem);
            audioService.playAudio();
            if (playerFragment != null) {
                playerFragment.setPlayer(audioService.getPlayer());
                audioService.setOnUIListener(playerFragment);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    private void playAudio(int item) {
        curItem = item;
        if (!isServiceBound) {
            Intent playerIntent = new Intent(this, AudioService.class);
            bindService(playerIntent, audioConnection, Context.BIND_AUTO_CREATE);
            startService(playerIntent);
        } else {
            audioService.setItems(audioList);
            audioService.setCurItem(curItem);
            audioService.playAudio();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shouldCreateFragmentOnCreate = savedInstanceState == null;

        if (savedInstanceState != null) {
            isInPermission = savedInstanceState
                    .getBoolean(STATE_IN_PERMISSION, false);
        }

        if (hasAllPermissions(INITIAL_PERMS)) {
            onReady();
        } else if (!isInPermission) {
            isInPermission = true;

            ActivityCompat.requestPermissions(this,
                            notGrantedPermissions(INITIAL_PERMS),
                            REQUEST_PERMISSION_CODE);
        }
    }

	@Override
	protected void onDestroy() {
		audioService = null;
		super.onDestroy();
	}

    @Override
    protected void onPause() {
        super.onPause();
        if (isServiceBound) {
            unbindService(audioConnection);
        }
        isServiceBound = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                       String permissions[], int[] grantResults) {
        if (hasAllPermissions(permissions)) {
            onReady();
        } else {
            onPermissionDenied();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(STATE_IN_PERMISSION, isInPermission);
    }

    private boolean hasAllPermissions(String[] perms) {
        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(this, perm) !=
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    private String[] notGrantedPermissions(String[] wanted) {
        List<String> result = new ArrayList<>();

        for (String perm : wanted) {
            if (ContextCompat.checkSelfPermission(this, perm) !=
                    PackageManager.PERMISSION_GRANTED) {
                result.add(perm);
            }
        }

        return result.toArray(new String[result.size()]);
    }

    private void onPermissionDenied() {
        Toast.makeText(this, R.string.msg_sorry, Toast.LENGTH_LONG)
                .show();
        finish();
    }

    protected void onReady() {
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            ContentResolver resolver = getContentResolver();
            Cursor cursor = resolver.query(
                    Media.EXTERNAL_CONTENT_URI,
                    new String[]{Media._ID,
                            Media.ARTIST, Media.TITLE,
                            Media.DATA, Media.ALBUM,
                            Media.ALBUM_ID, Media.DURATION},
                    Media.IS_MUSIC + " != 0",
                    null,
                    "TITLE ASC");
            audioList = new ArrayList<>();
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    AudioItem item = new AudioItem();
                    item.setId(cursor.getLong(cursor.getColumnIndex(Media._ID)));
                    item.setData(cursor.getString(cursor.getColumnIndex(Media.DATA)));
                    item.setTitle(cursor.getString(cursor.getColumnIndex(Media.TITLE)));
                    item.setAlbum(cursor.getString(cursor.getColumnIndex(Media.ALBUM)));
                    item.setArtist(cursor.getString(cursor.getColumnIndex(Media.ARTIST)));
                    item.setAlbumId(cursor.getLong(cursor.getColumnIndex(Media.ALBUM_ID)));
                    item.setDuration(cursor.getInt(cursor.getColumnIndex(Media.DURATION)));
                    audioList.add(item);
                }
            }
            cursor.close();

            if (findViewById(R.id.fragment_container) != null) {
                if (!shouldCreateFragmentOnCreate) {
                    return;
                }
                playlistFragment = new PlaylistFragment();
                playlistFragment.setAudioList(audioList);
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, playlistFragment)
                        .commit();
            }
        }
    }

    @Override
    public void onItemSelected(int position) {
        playAudio(position);
        if (playerFragment == null) {
            playerFragment = new PlayerFragment();
        }
        playerFragment.setItem(audioList.get(position));
        if (audioService != null) {
            playerFragment.setPlayer(audioService.getPlayer());
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, playerFragment)
                .commit();
    }

    @Override
    public AudioItem onPlayerNext() {
        return audioService.skipToNext();
    }

    @Override
    public AudioItem onPlayerPrev() {
        return audioService.skipToPrevious();
    }

    @Override
    public void onPlayerRepeat() {
        audioService.setRepeat();
    }

    @Override
    public void onPlayerPlayPause() {
        audioService.changePlaybackStatus();
    }

    @Override
    public void onPlayerRandom() {
        audioService.setRandom();
    }

    @Override
    public void onPlaylistOpen() {
        if (playlistFragment == null) {
            playlistFragment = new PlaylistFragment();
            playlistFragment.setAudioList(audioList);
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, playlistFragment)
                .commit();
    }
}
