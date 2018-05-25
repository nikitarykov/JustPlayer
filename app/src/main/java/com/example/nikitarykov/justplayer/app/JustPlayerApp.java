package com.example.nikitarykov.justplayer.app;

import android.app.Application;

import com.example.nikitarykov.justplayer.di.AppComponent;
import com.example.nikitarykov.justplayer.di.DaggerAppComponent;
import com.example.nikitarykov.justplayer.di.module.AudioManagerModule;
import com.example.nikitarykov.justplayer.di.module.ContextModule;

public class JustPlayerApp extends Application {
    private static AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        appComponent = DaggerAppComponent.builder()
                .contextModule(new ContextModule(this))
                .audioManagerModule(new AudioManagerModule())
                .build();
    }

    public static AppComponent getAppComponent() {
        return appComponent;
    }

}
