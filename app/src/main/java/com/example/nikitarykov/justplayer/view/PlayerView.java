package com.example.nikitarykov.justplayer.view;

import com.arellomobile.mvp.MvpView;
import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy;
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType;
import com.example.nikitarykov.justplayer.model.Track;

@StateStrategyType(AddToEndSingleStrategy.class)
public interface PlayerView extends MvpView {

    void setCurrentTrack(Track track);

    void setPlaying(boolean isPlaying);

    void setCurrentPosition(int position);

    void setRandom(boolean isRandom);

    void setRepeat(boolean isRepeat);
}
