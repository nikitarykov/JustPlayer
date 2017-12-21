package com.example.nikitarykov.justplayer;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

/**
 * Created by Nikita Rykov on 30.09.2017.
 */

public class AudioAdapter extends ArrayAdapter<AudioItem> {

    private static class ViewHolder {
		TextView titleView;
		TextView artistView;
		ImageView coverView;
	}
	private LayoutInflater inflater;
	private Activity activity;
    private List<AudioItem> items;

	public AudioAdapter(Context context, int resourceId, List<AudioItem> items) {
		super(context, resourceId, items);
        if (context instanceof Activity) {
            this.activity = (Activity) context;
        }
		inflater = LayoutInflater.from(context);
		this.items = items;
	}

    @Nullable
    @Override
    public AudioItem getItem(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
	    View view = convertView;
        ViewHolder holder = null;
	    if (view == null) {
	        view = inflater.inflate(R.layout.item_audio, parent, false);
            holder = new ViewHolder();
            holder.titleView = view.findViewById(R.id.item_title);
            holder.artistView = view.findViewById(R.id.item_artist);
            holder.coverView = view.findViewById(R.id.item_cover);
            view.setTag(holder);
        } else {
	        holder = (ViewHolder) view.getTag();
        }

        AudioItem item = items.get(position);
        String title = item.getTitle();
        String artist = item.getArtist();

        holder.titleView.setText(title);
        holder.artistView.setText(artist);
        Long albumId = item.getAlbumId();
        Uri albumartUri = Uri.parse(getContext().getString(R.string.albumart_uri));
        Uri coverUri = ContentUris.withAppendedId(albumartUri, albumId);

        Glide.with(activity)
            .load(coverUri)
            .apply(new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.icon_music)
                .error(R.drawable.icon_music))
                .into(holder.coverView);

        return view;
    }

}
