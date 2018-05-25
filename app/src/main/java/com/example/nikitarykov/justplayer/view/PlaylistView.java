package com.example.nikitarykov.justplayer.view;

import com.arellomobile.mvp.MvpView;
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy;
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType;

public interface PlaylistView extends MvpView {

    @StateStrategyType(SkipStrategy.class)
    void openTrack(int position);
}
