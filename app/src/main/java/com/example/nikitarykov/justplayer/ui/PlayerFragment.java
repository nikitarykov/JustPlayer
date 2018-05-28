package com.example.nikitarykov.justplayer.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.arellomobile.mvp.MvpAppCompatFragment;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.PresenterType;
import com.example.nikitarykov.justplayer.common.BitmapUtils;
import com.example.nikitarykov.justplayer.model.Track;
import com.example.nikitarykov.justplayer.common.ProgressUtils;
import com.example.nikitarykov.justplayer.R;
import com.example.nikitarykov.justplayer.presenter.PlayerPresenter;
import com.example.nikitarykov.justplayer.view.PlayerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class PlayerFragment extends MvpAppCompatFragment implements PlayerView, SeekBar.OnSeekBarChangeListener {

    @InjectPresenter(type = PresenterType.WEAK, tag = PlayerPresenter.TAG)
    PlayerPresenter playerPresenter;

    private Unbinder unbinder;

    @BindView(R.id.cur_position)
    TextView curPositionTextView;

    @BindView(R.id.total_duration)
    TextView totalDurationTextView;

    @BindView(R.id.audio_title)
    TextView audioTitle;

    @BindView(R.id.audio_artist)
    TextView artist;

    @BindView(R.id.audio_cover)
    ImageView coverView;

    @BindView(R.id.play_button)
    ImageButton playButton;

    @BindView(R.id.prev_button)
    ImageButton prevButton;

    @BindView(R.id.next_button)
    ImageButton nextButton;

    @BindView(R.id.repeat_button)
    ImageButton repeatButton;

    @BindView(R.id.random_button)
    ImageButton randomButton;

    @BindView(R.id.progress_bar)
    SeekBar progressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);
        unbinder = ButterKnife.bind(this, view);
        progressBar.setOnSeekBarChangeListener(this);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @OnClick(R.id.play_button)
    public void onPlayButton() {
        if (playButton.isSelected()) {
            playerPresenter.pause();
        } else {
            playerPresenter.resume();
        }
    }

    @OnClick(R.id.prev_button)
    public void onPreviousTrack() {
        playerPresenter.skipToPrevious();
    }

    @OnClick(R.id.next_button)
    public void onNextTrack() {
        playerPresenter.skipToNext();
    }

    @OnClick(R.id.repeat_button)
    public void onRepeat() {
        playerPresenter.setRepeat(repeatButton.isSelected());
    }

    @OnClick(R.id.random_button)
    public void onRandom() {
        playerPresenter.setRandom(randomButton.isSelected());
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            playerPresenter.updateCurrentPosition(1000 * progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void setCurrentTrack(Track track) {
        audioTitle.setText(track.getTitle());
        artist.setText(track.getArtist());
        totalDurationTextView.setText(ProgressUtils.milliSecondsToDuration(track.getDuration()));
        progressBar.setMax(track.getDuration() / 1000);
        coverView.setImageBitmap(BitmapUtils.loadBitmap(getContext(), track.getAlbumId()));
    }

    @Override
    public void setPlaying(boolean isPlaying) {
        playButton.setSelected(isPlaying);
    }

    @Override
    public void setCurrentPosition(int position) {
        progressBar.setProgress(ProgressUtils.positionToProgress(position));
        curPositionTextView.setText(ProgressUtils.milliSecondsToDuration(position));
    }

    @Override
    public void setRandom(boolean isRandom) {
        randomButton.setSelected(isRandom);
    }

    @Override
    public void setRepeat(boolean isRepeat) {
        repeatButton.setSelected(isRepeat);
    }
}
