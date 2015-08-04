package us.phyxsi.spotifystreamer.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import us.phyxsi.spotifystreamer.BaseActivity;
import us.phyxsi.spotifystreamer.MusicService;
import us.phyxsi.spotifystreamer.R;
import us.phyxsi.spotifystreamer.player.FullScreenPlayerActivity;
import us.phyxsi.spotifystreamer.player.PlayerSession;

/**
 * A class that shows the Media Queue to the user.
 */
public class PlaybackControlsFragment extends Fragment implements MusicService.OnStateChangeListener {

    private static final String TAG = PlaybackControlsFragment.class.getSimpleName();

    private ImageButton mPlayPause;
    private TextView mTitle;
    private TextView mSubtitle;
    private TextView mExtraInfo;
    private ImageView mAlbumArt;
    private String mArtUrl;

    protected Intent serviceIntent;
    private PlayerSession mSession;
    private MusicService mMusicService;
    private boolean serviceBound = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (serviceIntent == null) {
            serviceIntent = new Intent(activity, MusicService.class);
//            activity.bindService(serviceIntent, streamingConnection, Context.BIND_AUTO_CREATE);
        }
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);

        mPlayPause = (ImageButton) rootView.findViewById(R.id.play_pause);
        mPlayPause.setEnabled(true);
        mPlayPause.setOnClickListener(mButtonListener);

        mTitle = (TextView) rootView.findViewById(R.id.title);
        mSubtitle = (TextView) rootView.findViewById(R.id.artist);
        mExtraInfo = (TextView) rootView.findViewById(R.id.extra_info);
        mAlbumArt = (ImageView) rootView.findViewById(R.id.album_art);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), FullScreenPlayerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                MediaMetadata metadata = getActivity().getMediaController().getMetadata();

                if (metadata != null) {
//                    intent.putExtra()
                }
                startActivity(intent);
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(TAG, "fragment.onStart");
    }

    @Override
    public void onStop() {
        super.onStop();

        Log.d(TAG, "fragment.onStop");
    }

    private void onMetadataChanged(MediaMetadata metadata) {
        Log.d(TAG, "onMetadataChanged " + metadata);
        if (getActivity() == null) {
            Log.w(TAG, "onMetadataChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (metadata == null) {
            return;
        }

        mTitle.setText(metadata.getDescription().getTitle());
        mSubtitle.setText(metadata.getDescription().getSubtitle());
        String artUrl = null;
        if (metadata.getDescription().getIconUri() != null) {
            artUrl = metadata.getDescription().getIconUri().toString();
        }
//        if (!TextUtils.equals(artUrl, mArtUrl)) {
//            mArtUrl = artUrl;
//            Bitmap art = metadata.getDescription().getIconBitmap();
//            AlbumArtCache cache = AlbumArtCache.getInstance();
//            if (art == null) {
//                art = cache.getIconImage(mArtUrl);
//            }
//            if (art != null) {
//                mAlbumArt.setImageBitmap(art);
//            } else {
//                cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
//                            @Override
//                            public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
//                                if (icon != null) {
////                                    LogHelper.d(TAG, "album art icon of w=", icon.getWidth(),
////                                            " h=", icon.getHeight());
//                                    if (isAdded()) {
//                                        mAlbumArt.setImageBitmap(icon);
//                                    }
//                                }
//                            }
//                        }
//                );
//            }
//        }
    }

    private void onPlaybackStateChanged(PlaybackState state) {
        Log.d(TAG, "onPlaybackStateChanged " + state);
        if (getActivity() == null) {
            Log.w(TAG, "onPlaybackStateChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (state == null) {
            return;
        }
        boolean enablePlay = false;
        switch (state.getState()) {
            case PlaybackState.STATE_PAUSED:
            case PlaybackState.STATE_STOPPED:
                enablePlay = true;
                break;
            case PlaybackState.STATE_ERROR:
                Log.e(TAG, "error playbackstate: " + state.getErrorMessage());
                Toast.makeText(getActivity(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
                break;
        }

        if (enablePlay) {
            mPlayPause.setImageDrawable(
                    getActivity().getDrawable(R.drawable.ic_play_arrow_white_36dp));
        } else {
            mPlayPause.setImageDrawable(getActivity().getDrawable(R.drawable.ic_pause_white_36dp));
        }
    }

    private final View.OnClickListener mButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PlaybackState stateObj = getActivity().getMediaController().getPlaybackState();
            final int state = stateObj == null ?
                    PlaybackState.STATE_NONE : stateObj.getState();
            Log.d(TAG, "Button pressed, in state " + state);
            switch (v.getId()) {
                case R.id.play_pause:
                    Log.d(TAG, "Play button pressed, in state " + state);
                    if (state == PlaybackState.STATE_PAUSED ||
                            state == PlaybackState.STATE_STOPPED ||
                            state == PlaybackState.STATE_NONE) {
                        playMedia();
                    } else if (state == PlaybackState.STATE_PLAYING ||
                            state == PlaybackState.STATE_BUFFERING ||
                            state == PlaybackState.STATE_CONNECTING) {
                        pauseMedia();
                    }
                    break;
            }
        }
    };

    private void playMedia() {
        MediaController controller = getActivity().getMediaController();
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }

    private void pauseMedia() {
        MediaController controller = getActivity().getMediaController();
        if (controller != null) {
            controller.getTransportControls().pause();
        }
    }

    @Override
    public void onStateChanged(boolean isPlaying) {
        BaseActivity activity = (BaseActivity) getActivity();
        activity.showPlaybackControls();

        Log.d(TAG, "On state changed");
    }

    private void setOnPlayerStateChanged() {
        if (mMusicService != null) mMusicService.setOnStateChangeListener(this);
    }

    private ServiceConnection streamingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "FullScreenPlayerFragment onServiceCreated called");
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;

            mMusicService = binder.getService();

            PlayerSession serviceSession = mMusicService.getSession();
            if (serviceSession != null && serviceSession.equals(mSession)) {
                mSession = serviceSession;
            } else {
                mMusicService.setSession(mSession);
            }

            mMusicService.registerCallback((BaseActivity) getActivity());
//            setOnPlayerStateChanged();

            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
}
