package com.example.nikitarykov.justplayer.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.arellomobile.mvp.MvpDelegate;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.PresenterType;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.nikitarykov.justplayer.R;
import com.example.nikitarykov.justplayer.model.Track;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PlaylistAdapter extends MvpArrayAdapter<Track> {

    private List<Track> tracks;

    public PlaylistAdapter(MvpDelegate<?> parentDelegate, Context context, int resourceId, List<Track> tracks) {
        super(parentDelegate, context, resourceId, tracks);
        this.tracks = tracks;
        getMvpDelegate().onCreate();
    }

    @Nullable
    @Override
    public Track getItem(int position) {
        return tracks.get(position);
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_audio, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Track track = getItem(position);
        holder.bind(position, track);

        return convertView;
    }

    public class ViewHolder {
        private Track track;

        @BindView(R.id.item_title)
        TextView titleView;

        @BindView(R.id.item_artist)
        TextView artistView;

        @BindView(R.id.item_cover)
        ImageView coverView;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }

        void bind(int position, Track track) {
            /*if (getMvpDelegate() != null) {
                getMvpDelegate().onSaveInstanceState();
                getMvpDelegate().onDetach();
                getMvpDelegate().onDestroyView();
                mvpDelegate = null;
            }

            getMvpDelegate().onCreate();
            getMvpDelegate().onAttach();*/

            this.track = track;
            titleView.setText(track.getTitle());
            artistView.setText(track.getArtist());
            Long albumId = track.getAlbumId();
            Uri albumartUri = Uri.parse(getContext().getString(R.string.albumart_uri));
            Uri coverUri = ContentUris.withAppendedId(albumartUri, albumId);

            Glide.with(getContext())
                    .load(coverUri)
                    .apply(new RequestOptions()
                            .centerCrop()
                            .placeholder(R.drawable.icon_music)
                            .error(R.drawable.icon_music))
                    .into(coverView);
        }
    }
}
