package com.example.nikitarykov.justplayer.di;

import android.content.Context;

import com.example.nikitarykov.justplayer.di.module.AudioManagerModule;
import com.example.nikitarykov.justplayer.di.module.ContextModule;
import com.example.nikitarykov.justplayer.model.TrackManager;
import com.example.nikitarykov.justplayer.presenter.PlayerPresenter;
import com.example.nikitarykov.justplayer.presenter.PlaylistPresenter;
import com.example.nikitarykov.justplayer.presenter.StartPresenter;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ContextModule.class, AudioManagerModule.class})
public interface AppComponent {
    Context getContext();
    TrackManager getTrackManager();

    void inject(StartPresenter startPresenter);
    void inject(PlayerPresenter playerPresenter);
    void inject(PlaylistPresenter playlistPresenter);
}
