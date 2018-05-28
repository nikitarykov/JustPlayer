package com.example.nikitarykov.justplayer.model;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrackManager {
    private ContentResolver contentResolver;

    private List<Track> tracks;
    private List<Track> playlist;
    private int curTrackId = -1;
    private int resumePosition = -1;
    private boolean isRepeat;

    private static final String[] NEEDED_COLUMNS = new String[] {
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION
    };

    public TrackManager(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public void loadTracks() {
        Cursor cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                NEEDED_COLUMNS,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                "TITLE ASC");
        tracks = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                Track item = new Track();
                item.setId(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                item.setData(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                item.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                item.setAlbum(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
                item.setArtist(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                item.setAlbumId(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
                item.setDuration(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
                tracks.add(item);
            }
        }
        cursor.close();
        playlist = new ArrayList<>(tracks);
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public List<Track> getPlaylist() {
        return playlist;
    }

    public Track getCurTrack() {
        return playlist.get(curTrackId);
    }

    public void setCurTrackId(int curTrackId) {
        Track track = tracks.get(curTrackId);
        if (track != null) {
            for (int id = 0; id < playlist.size(); id++) {
                if (track.getId() == playlist.get(id).getId()) {
                    this.curTrackId = id;
                }
            }
        }
    }

    public int getResumePosition() {
        return resumePosition;
    }

    public void setResumePosition(int resumePosition) {
        this.resumePosition = resumePosition;
    }

    public void nextTrack() {
        if (!isRepeat) {
            curTrackId++;
            if (curTrackId == playlist.size()) {
                curTrackId = 0;
            }
        }
    }

    public void previousTrack() {
        if (!isRepeat) {
            curTrackId--;
            if (curTrackId < 0) {
                curTrackId = playlist.size() - 1;
            }
        }
    }

    public void setRandom(boolean isRandom) {
        Long trackId = null;
        if (curTrackId > 0) {
            trackId = playlist.get(curTrackId).getId();
        }
        playlist = new ArrayList<>(tracks);
        if (isRandom) {
            Collections.shuffle(playlist);
        }
        if (trackId != null) {
            for (int id = 0; id < playlist.size(); id++) {
                if (trackId == playlist.get(id).getId()) {
                    curTrackId = id;
                }
            }
        }
    }

    public boolean isRepeat() {
        return isRepeat;
    }

    public void setRepeat(boolean repeat) {
        isRepeat = repeat;
    }
}
