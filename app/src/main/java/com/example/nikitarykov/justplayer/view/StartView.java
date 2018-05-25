package com.example.nikitarykov.justplayer.view;

import com.arellomobile.mvp.MvpView;
import com.example.nikitarykov.justplayer.model.Track;

import java.util.List;

public interface StartView extends MvpView {

    void setLoading();

    void onLoaded(List<Track> tracks);
}
