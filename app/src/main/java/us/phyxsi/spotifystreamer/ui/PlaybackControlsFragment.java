package us.phyxsi.spotifystreamer.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import us.phyxsi.spotifystreamer.MusicService;
import us.phyxsi.spotifystreamer.R;
import us.phyxsi.spotifystreamer.object.ParcableTrack;
import us.phyxsi.spotifystreamer.object.PlayerSession;

/**
 * A class that shows the Media Queue to the user.
 */
public class PlaybackControlsFragment extends Fragment implements
        MusicService.MusicServiceCallback, MusicService.OnStateChangeListener {

    private static final String TAG = PlaybackControlsFragment.class.getSimpleName();

    private BaseActivity activity;

    private ImageButton mPlayPause;
    private TextView mTitle;
    private TextView mSubtitle;
    private TextView mExtraInfo;
    private ImageView mAlbumArt;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private String mArtUrl;

    private PlayerSession mSession;
    private MusicService mMusicService;
    private boolean serviceBound;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.activity = (BaseActivity) activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);

        mPlayPause = (ImageButton) rootView.findViewById(R.id.play_pause);
        mPlayPause.setEnabled(true);
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMusicService != null) mMusicService.togglePlay();

                mPlayPause.setImageDrawable((mMusicService.isPlaying()) ? mPauseDrawable : mPlayDrawable);
            }
        });

        mPauseDrawable = getActivity().getDrawable(R.drawable.ic_pause_white_36dp);
        mPlayDrawable = getActivity().getDrawable(R.drawable.ic_play_arrow_white_36dp);

        mTitle = (TextView) rootView.findViewById(R.id.title);
        mSubtitle = (TextView) rootView.findViewById(R.id.artist);
        mExtraInfo = (TextView) rootView.findViewById(R.id.extra_info);
        mAlbumArt = (ImageView) rootView.findViewById(R.id.album_art);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FragmentManager fragmentManager =
                        ((FragmentActivity) getActivity()).getSupportFragmentManager();

                if (MainActivity.mIsLargeLayout) {
                    FullScreenPlayerFragment fragment = new FullScreenPlayerFragment();
                    Bundle arguments = new Bundle();

                    arguments.putParcelable(FullScreenPlayerActivity.PLAYER_SESSION, mSession);
                    fragment.setArguments(arguments);
                    fragment.show(fragmentManager, "PLAYER");
                } else {
                    Intent intent = new Intent(getActivity(), FullScreenPlayerActivity.class);
                    intent.putExtra(FullScreenPlayerActivity.PLAYER_SESSION, mSession);

                    startActivity(intent);
                }
            }
        });

        setViewsInfo();

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(TAG, "fragment.onStart");
    }

    @Override
    public void onResume() {
        super.onResume();

        setViewsInfo();
    }

    @Override
    public void onDestroy() {
        unbindService();
        super.onDestroy();
    }

    public void unbindService() {
        if (mMusicService != null) {
            mMusicService.unregisterCallback(null);
            getActivity().unbindService(streamingConnection);
            mMusicService = null;
        }
    }

    public void setViewsInfo() {
        if (mSession == null) return;

        setViewsInfo(mSession.getCurrentTrack());
    }

    private void setViewsInfo(ParcableTrack track) {
        if (mSession == null) return;

        mTitle.setText(track.name);
        mSubtitle.setText(track.artist);
        Picasso.with(getActivity())
                .load(mSession.getCurrentTrack().imageUrl)
                .into(mAlbumArt);
    }

    // MusicService callback overrides
    @Override
    public void onProgressChange(int progress) {
    }

    @Override
    public void onTrackChanged(ParcableTrack track) {
        setViewsInfo(track);
    }

    @Override
    public void onPlaybackStopped() {
        mPlayPause.setImageDrawable(mPlayDrawable);
    }

    @Override
    public void onStateChanged(boolean isPlaying) {
        BaseActivity activity = (BaseActivity) getActivity();

        mPlayPause.setImageDrawable((isPlaying) ? mPauseDrawable : mPlayDrawable);
    }

    public boolean shouldShowControls() {
        if (mMusicService == null || mSession == null || mSession.getCurrentTrack() == null)
            return false;

        return true;
    }

    protected void setSession() {
        mSession = mMusicService.getSession();

        if (mMusicService != null) mMusicService.setOnStateChangeListener(this);

        onStateChanged(mMusicService.isPlaying());
    }

    public ServiceConnection streamingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;

            mMusicService = binder.getService();

            setSession();
            mMusicService.registerCallback(PlaybackControlsFragment.this);

            setViewsInfo();

            // Show controls if something is playing or paused
            if (shouldShowControls()) ((BaseActivity) getActivity()).showPlaybackControls();

            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
}
