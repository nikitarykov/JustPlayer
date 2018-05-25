package com.example.nikitarykov.justplayer.presenter;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;
import com.example.nikitarykov.justplayer.app.JustPlayerApp;
import com.example.nikitarykov.justplayer.model.TrackManager;
import com.example.nikitarykov.justplayer.view.StartView;

import javax.inject.Inject;

@InjectViewState
public class StartPresenter extends MvpPresenter<StartView> {

    @Inject
    TrackManager trackManager;

    public StartPresenter() {
        JustPlayerApp.getAppComponent().inject(this);
    }

    public static final String TAG = "START";

    public void loadTracks() {
        getViewState().setLoading();
        trackManager.loadTracks();
        getViewState().onLoaded(trackManager.getTracks());
    }
}
