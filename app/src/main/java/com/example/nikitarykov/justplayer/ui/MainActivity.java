package com.example.nikitarykov.justplayer.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.widget.Toast;

import com.arellomobile.mvp.MvpAppCompatActivity;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.PresenterType;
import com.example.nikitarykov.justplayer.R;
import com.example.nikitarykov.justplayer.model.Track;
import com.example.nikitarykov.justplayer.presenter.StartPresenter;
import com.example.nikitarykov.justplayer.view.StartView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nikita Rykov on 30.09.2017.
 */
public class MainActivity extends MvpAppCompatActivity implements StartView {

    @InjectPresenter(type = PresenterType.WEAK, tag = StartPresenter.TAG)
    StartPresenter startPresenter;

    public static final String PLAY_NEW_AUDIO = "com.example.nikitarykov.justplayer.PlayNewAudio";

    private static final int REQUEST_PERMISSION_CODE = 61125;
    private static final String STATE_IN_PERMISSION = "inPermission";
    private boolean isInPermission = false;
    private PlaylistFragment playlistFragment;
    private PlayerFragment playerFragment;

    private static final String[] INITIAL_PERMS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private boolean shouldCreateFragmentOnCreate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shouldCreateFragmentOnCreate = savedInstanceState == null;

        if (savedInstanceState != null) {
            isInPermission = savedInstanceState
                    .getBoolean(STATE_IN_PERMISSION, false);
        }

        if (hasAllPermissions(INITIAL_PERMS)) {
            onReady();
        } else if (!isInPermission) {
            isInPermission = true;

            ActivityCompat.requestPermissions(this,
                            notGrantedPermissions(INITIAL_PERMS),
                            REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                       String permissions[], int[] grantResults) {
        if (hasAllPermissions(permissions)) {
            onReady();
        } else {
            onPermissionDenied();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(STATE_IN_PERMISSION, isInPermission);
    }

    private boolean hasAllPermissions(String[] perms) {
        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(this, perm) !=
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    private String[] notGrantedPermissions(String[] wanted) {
        List<String> result = new ArrayList<>();

        for (String perm : wanted) {
            if (ContextCompat.checkSelfPermission(this, perm) !=
                    PackageManager.PERMISSION_GRANTED) {
                result.add(perm);
            }
        }

        return result.toArray(new String[result.size()]);
    }

    private void onPermissionDenied() {
        Toast.makeText(this, R.string.msg_sorry, Toast.LENGTH_LONG)
                .show();
        finish();
    }

    protected void onReady() {
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            startPresenter.loadTracks();
        }
    }

    @Override
    public void onLoaded(List<Track> tracks) {
        if (findViewById(R.id.fragment_container) != null) {
            if (!shouldCreateFragmentOnCreate) {
                return;
            }
            playlistFragment = new PlaylistFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, playlistFragment)
                    .commit();
        }
    }

    @Override
    public void setLoading() {

    }
}
