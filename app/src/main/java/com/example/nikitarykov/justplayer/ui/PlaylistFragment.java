package com.example.nikitarykov.justplayer.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.PresenterType;
import com.example.nikitarykov.justplayer.R;
import com.example.nikitarykov.justplayer.adapter.PlaylistAdapter;
import com.example.nikitarykov.justplayer.model.Track;
import com.example.nikitarykov.justplayer.presenter.PlayerPresenter;
import com.example.nikitarykov.justplayer.presenter.PlaylistPresenter;
import com.example.nikitarykov.justplayer.view.PlayerView;
import com.example.nikitarykov.justplayer.view.PlaylistView;

import java.util.List;

/**
 * Created by Nikita Rykov on 20.12.2017.
 */

public class PlaylistFragment extends MvpListFragment implements PlaylistView {

    @InjectPresenter(type = PresenterType.WEAK, tag = PlaylistPresenter.TAG)
    PlaylistPresenter playlistPresenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ArrayAdapter<Track> adapter = new PlaylistAdapter(getMvpDelegate(),
                inflater.getContext(), android.R.layout.simple_list_item_1, playlistPresenter.getTracks());
        setListAdapter(adapter);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        playlistPresenter.select(position);
    }

    @Override
    public void openTrack(int position) {
        Intent playerIntent = new Intent(getActivity(), PlayerService.class);
        ContextCompat.startForegroundService(getActivity(), playerIntent);
        PlayerFragment playerFragment = new PlayerFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, playerFragment)
                .addToBackStack(null)
                .commit();
    }

}
