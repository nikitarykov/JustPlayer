package com.example.nikitarykov.justplayer;

import android.content.ContentUris;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import static android.graphics.Color.WHITE;

/**
 * Created by Nikita Rykov on 20.12.2017.
 */

public class PlayerFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, AudioService.OnUIListener {
    private AudioItem item;
    private TextView curPositionTextView;
    private TextView totalDurationTextView;
    private SeekBar progressBar;
    private MediaPlayer player;

    private OnRepeatListener repeatListener;
    private OnRandomListener randomListener;
    private OnPlayPauseListener playPauseListener;
    private OnPrevListener prevListener;
    private OnNextListener nextListener;
    private OnPlaylistOpenListener playlistOpenListener;

    private TextView audioTitle;
    private TextView artist;
    private ImageView coverView;
    private ImageButton playPauseButton;

    private Handler handler = new Handler();
    private Runnable task = new Runnable() {

        @Override
        public void run() {
            if(player != null && progressBar != null) {
                int currentPosition = player.getCurrentPosition();
                progressBar.setProgress(currentPosition / 1000);
                curPositionTextView.setText(ProgressUtils.milliSecondsToDuration(currentPosition));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            prevListener = (OnPrevListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnPrevListener");
        }
        try {
            nextListener = (OnNextListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnNextListener");
        }
        try {
            playPauseListener = (OnPlayPauseListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnPlayPauseListener");
        }
        try {
            randomListener = (OnRandomListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnRandomListener");
        }
        try {
            repeatListener = (OnRepeatListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnRepeatListener");
        }
        try {
            playlistOpenListener = (OnPlaylistOpenListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnPlaylistOpenListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();
        if (view != null && item != null) {
            audioTitle = view.findViewById(R.id.audio_title);
            audioTitle.setText(item.getTitle());
            artist = view.findViewById(R.id.audio_artist);
            artist.setText(item.getArtist());
            coverView = view.findViewById(R.id.audio_cover);
            curPositionTextView = view.findViewById(R.id.cur_position);
            curPositionTextView.setText("00:00");
            totalDurationTextView = view.findViewById(R.id.total_duration);
            totalDurationTextView.setText(ProgressUtils.milliSecondsToDuration(item.getDuration()));
            progressBar = view.findViewById(R.id.progress_bar);
            progressBar.setBackgroundColor(WHITE);
            progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && player != null && player.isPlaying()) {
                        player.seekTo(1000 * progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            if (item != null) {
                progressBar.setMax(item.getDuration() / 1000);
            }
            ImageButton playlistButton = view.findViewById(R.id.playlist_button);
            if (playlistButton != null) {
                playlistButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        playlistOpenListener.onPlaylistOpen();
                    }
                });
            }
            ImageButton repeatButton = view.findViewById(R.id.repeat_button);
            repeatButton.setSelected(false);
            repeatButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    repeatButton.setSelected(!repeatButton.isSelected());
                    repeatListener.onPlayerRepeat();
                }
            });
            ImageButton randomButton = view.findViewById(R.id.random_button);
            randomButton.setSelected(false);
            randomButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    randomButton.setSelected(!randomButton.isSelected());
                    randomListener.onPlayerRandom();
                }
            });
            playPauseButton = view.findViewById(R.id.play_button);
            playPauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    playPauseButton.setSelected(!playPauseButton.isSelected());
                    playPauseListener.onPlayerPlayPause();
                }
            });
            ImageButton nextButton = view.findViewById(R.id.next_button);
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    playPauseButton.setSelected(false);
                    item = nextListener.onPlayerNext();
                    updateUI();
                }
            });
            ImageButton prevButton = view.findViewById(R.id.prev_button);
            prevButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    playPauseButton.setSelected(false);
                    item = prevListener.onPlayerPrev();
                    updateUI();
                }
            });

            Uri albumartUri = Uri.parse(getContext().getString(R.string.albumart_uri));
            Uri coverUri = ContentUris.withAppendedId(albumartUri, item.getAlbumId());

            if (player != null) {
                handler.post(task);
            }

            Glide.with(getActivity())
                    .load(coverUri)
                    .apply(new RequestOptions()
                            .centerInside()
                            .centerCrop()
                            .placeholder(R.drawable.icon_music)
                            .error(R.drawable.icon_music))
                    .into(coverView);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
        handler.post(task);
    }

    @Override
    public void onPause() {
        super.onPause();;
        handler.removeCallbacks(task);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(task);
    }

    public AudioItem getItem() {
        return item;
    }

    public void setItem(AudioItem item) {
        this.item = item;
    }

    public void setPlayer(MediaPlayer player) {
        this.player = player;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public interface OnNextListener {
        AudioItem onPlayerNext();
    }

    public interface OnPrevListener {
        AudioItem onPlayerPrev();
    }

    public interface OnRepeatListener {
        void onPlayerRepeat();
    }

    public interface OnPlayPauseListener {
        void onPlayerPlayPause();
    }

    public interface OnRandomListener {
        void onPlayerRandom();
    }

    public interface OnPlaylistOpenListener {
        void onPlaylistOpen();
    }

    public void startTask() {
        handler.post(task);
    }

    public void updateUI() {
        if (getContext() == null) {
            return;
        }
        audioTitle.setText(item.getTitle());
        artist.setText(item.getArtist());
        curPositionTextView.setText("00:00");
        totalDurationTextView.setText(ProgressUtils.milliSecondsToDuration(item.getDuration()));
        progressBar.setProgress(0);
        if (player != null) {
            playPauseButton.setSelected(player.isPlaying());
        }

        Uri albumartUri = Uri.parse(getContext().getString(R.string.albumart_uri));
        Uri coverUri = ContentUris.withAppendedId(albumartUri, item.getAlbumId());

        Glide.with(getActivity())
                .load(coverUri)
                .apply(new RequestOptions()
                        .centerInside()
                        .centerCrop()
                        .placeholder(R.drawable.icon_music)
                        .error(R.drawable.icon_music))
                .into(coverView);
    }

    public void setPlaybackStatus(AudioService.PlaybackStatus status) {
        if (status == AudioService.PlaybackStatus.PLAYING) {
            playPauseButton.setSelected(false);
        } else {
            playPauseButton.setSelected(true);
        }

    }

}
