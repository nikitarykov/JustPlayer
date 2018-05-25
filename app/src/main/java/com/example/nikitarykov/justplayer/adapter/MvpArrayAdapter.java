package com.example.nikitarykov.justplayer.adapter;

import android.app.Activity;
import android.content.Context;
import android.widget.ArrayAdapter;

import com.arellomobile.mvp.MvpDelegate;
import com.example.nikitarykov.justplayer.model.Track;

import java.util.List;

public class MvpArrayAdapter<T> extends ArrayAdapter<T> {
    private MvpDelegate<? extends MvpArrayAdapter> mvpDelegate;
    private MvpDelegate<?> parentDelegate;

    private Activity activity;
    private List<Track> items;

    public MvpArrayAdapter(MvpDelegate<?> parentDelegate, Context context, int resourceId, List<T> items) {
        super(context, resourceId, items);
        this.parentDelegate = parentDelegate;
        getMvpDelegate().onCreate();
    }

    public MvpDelegate getMvpDelegate() {
        if (mvpDelegate == null) {
            mvpDelegate = new MvpDelegate<>(this);
            mvpDelegate.setParentDelegate(parentDelegate, getClass().getName());

        }
        return mvpDelegate;
    }
}