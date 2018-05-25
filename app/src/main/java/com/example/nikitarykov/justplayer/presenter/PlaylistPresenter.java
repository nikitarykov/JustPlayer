package com.example.nikitarykov.justplayer.presenter;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;
import com.example.nikitarykov.justplayer.app.JustPlayerApp;
import com.example.nikitarykov.justplayer.model.Track;
import com.example.nikitarykov.justplayer.model.TrackManager;
import com.example.nikitarykov.justplayer.view.PlaylistView;

import java.util.List;

import javax.inject.Inject;

@InjectViewState
public class PlaylistPresenter extends MvpPresenter<PlaylistView> {

    public static final String TAG = "PLAYLIST";

    @Inject
    TrackManager trackManager;

    public PlaylistPresenter() {
        JustPlayerApp.getAppComponent().inject(this);
    }

    public List<Track> getTracks() {
        return trackManager.getTracks();
    }

    public void select(int trackId) {
        trackManager.setCurTrackId(trackId);
        getViewState().openTrack(trackId);
    }
}
