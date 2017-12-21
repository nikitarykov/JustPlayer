package com.example.nikitarykov.justplayer;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

/**
 * Created by Nikita Rykov on 20.12.2017.
 */

public class PlaylistFragment extends ListFragment {

    List<AudioItem> audioList;
    OnPlaylistItemSelectedListener callback;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            callback = (OnPlaylistItemSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnPlaylistItemSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ArrayAdapter<AudioItem> adapter = new AudioAdapter(
                inflater.getContext(), android.R.layout.simple_list_item_1, audioList);
        setListAdapter(adapter);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        callback.onItemSelected(position);
    }

    public List<AudioItem> getAudioList() {
        return audioList;
    }

    public void setAudioList(List<AudioItem> audioList) {
        this.audioList = audioList;
    }

    public interface OnPlaylistItemSelectedListener {
        void onItemSelected(int position);
    }

}
