package com.example.nikitarykov.justplayer.di.module;

import android.content.ContentResolver;

import com.example.nikitarykov.justplayer.model.TrackManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = {ContextModule.class})
public class AudioManagerModule {

    @Provides
    @Singleton
    public TrackManager provideGithubService(ContentResolver contentResolver) {
        return new TrackManager(contentResolver);
    }
}
