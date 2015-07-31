package us.phyxsi.spotifystreamer.player;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import us.phyxsi.spotifystreamer.MusicService;
import us.phyxsi.spotifystreamer.R;
import us.phyxsi.spotifystreamer.object.ParcableTrack;

public class PlayerFragment extends DialogFragment implements com.squareup.picasso.Callback,
        Palette.PaletteAsyncListener, MusicService.MusicServiceCallback, MusicService.OnStateChangeListener {

    private static final String TAG = PlayerFragment.class.getSimpleName();

    // Views
    private ImageView mSkipPrev;
    private ImageView mSkipNext;
    private ImageView mPlayPause;
    private TextView mStart;
    private TextView mEnd;
    private SeekBar mSeekbar;
    private TextView mLine1;
    private TextView mLine2;
    private ImageView mAlbumImage;
    private Toolbar mToolbar;
    private LinearLayout mControls;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;

    private PlayerSession mSession;
    private MusicService mMusicService;
    private Intent mPlayIntent;
    private ParcableTrack mTrack;
    private boolean serviceBound = false;
    private boolean isPlaying = (mMusicService != null);
    private boolean viewsAreCreated = false;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "PlayerFragment onCreate called");
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(PlayerActivity.PLAYER_SESSION)) {
            this.mSession = arguments.getParcelable(PlayerActivity.PLAYER_SESSION);

            assert this.mSession != null;
            this.mTrack = mSession.getCurrentTrack();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "PlayerFragment onCreateView called");
        View view = inflater.inflate(R.layout.fragment_player, container, false);

        // Initialize views
        mToolbar = (Toolbar) view.findViewById(R.id.actionbar);
        mAlbumImage = (ImageView) view.findViewById(R.id.player_album_image);
        mLine2 = (TextView) view.findViewById(R.id.player_track_title);
        mLine1 = (TextView) view.findViewById(R.id.player_artist_name);
        mControls = (LinearLayout) view.findViewById(R.id.player_control_layout);
        mSeekbar = (SeekBar) view.findViewById(R.id.player_seekbar);

        mSkipPrev = (ImageView) view.findViewById(R.id.player_control_prev);
        mPlayPause = (ImageView) view.findViewById(R.id.player_control_play_pause);
        mSkipNext = (ImageView) view.findViewById(R.id.player_control_next);

        mPauseDrawable = getActivity().getDrawable(R.drawable.ic_pause_white_48dp);
        mPlayDrawable = getActivity().getDrawable(R.drawable.ic_play_arrow_white_48dp);

        // Toolbar
//        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                unbindService();
//            }
//        });
//
//        if (getActivity() != null && getActivity() instanceof AppCompatActivity) {
//            AppCompatActivity activity = (AppCompatActivity) getActivity();
//            activity.setSupportActionBar(mToolbar);
//        }

        // Music control buttons
        mSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMusicService != null) mMusicService.playNext();
            }
        });

        mSkipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMusicService != null) mMusicService.playPrevious();
            }
        });

        mPlayPause.setImageDrawable((isPlaying) ? mPauseDrawable : mPlayDrawable);

        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPlaying = !isPlaying;

                if (mMusicService != null) mMusicService.togglePlay();
            }
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean userInitiated = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekTo(progress, userInitiated);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userInitiated = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userInitiated = false;
            }
        });

        viewsAreCreated = true;

        setViewsInfo(mTrack);

        return view;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "PlayerFragment onDestroyView called");
        viewsAreCreated = false;

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "PlayerFragment onDestroy called");
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "PlayerFragment onSaveInstanceState called");
        super.onSaveInstanceState(outState);

        outState.putParcelable(PlayerActivity.PLAYER_SESSION, mSession);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "PlayerFragment onActivityCreated called");
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null &&
                savedInstanceState.containsKey(PlayerActivity.PLAYER_SESSION)) {
            mSession = savedInstanceState.getParcelable(PlayerActivity.PLAYER_SESSION);

            if (mSession != null) {
                setViewsInfo(mSession.getCurrentTrack());
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "PlayerFragment onCreateDialog called");
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d(TAG, "PlayerFragment onAttach called");
        super.onAttach(activity);

        if (mPlayIntent == null) {
            mPlayIntent = new Intent(activity, MusicService.class);
            activity.bindService(mPlayIntent, streamingConnection, Context.BIND_AUTO_CREATE);
            activity.startService(mPlayIntent);
        }
    }

    private void setViewsInfo(ParcableTrack track) {
        if (viewsAreCreated) {
            mLine1.setText(track.name);
            mLine2.setText(track.artist);
            fetchImageAsync();

            mSeekbar.setMax(track.getDurationInMilli());
        }
    }


    // Seekbar functions
    public void seekTo(int position, boolean userInitiated) {
//        mStart.setText(formatMillis(position));
        if (userInitiated && mMusicService != null) mMusicService.seekTo(position);
    }

    private void updateProgress() {
        mSeekbar.setProgress((int) mMusicService.getCurrentPosition());
    }

    private void fetchImageAsync() {
        mAlbumImage.getViewTreeObserver()
                   .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                       @Override
                       public void onGlobalLayout() {
                           Picasso.with(getActivity())
                                   .load(mTrack.imageUrl)
                                   .resize(mAlbumImage.getWidth(), mAlbumImage.getHeight())
                                   .centerCrop()
                                   .into(mAlbumImage, PlayerFragment.this);

                       }
                   });
    }

    // Palette overrides
    @Override
    public void onGenerated(Palette palette) {

    }

    // Picasso overrides
    @Override
    public void onSuccess() {
        BitmapDrawable drawable = (BitmapDrawable) mAlbumImage.getDrawable();
        Palette.from(drawable.getBitmap()).generate(this);
    }

    @Override
    public void onError() {

    }

    // MusicService callback overrides
    @Override
    public void onProgressChange(int progress) {
        mSeekbar.setProgress(progress);
    }

    @Override
    public void onTrackChanged(ParcableTrack track) {
        setViewsInfo(track);
    }

    @Override
    public void onPlaybackStopped() {
        mPlayPause.setImageDrawable(mPlayDrawable);
        isPlaying = false;
    }

    @Override
    public void onStateChanged(boolean isPlaying) {
        if (!viewsAreCreated) return;

        mPlayPause.setImageDrawable((isPlaying) ? mPauseDrawable : mPlayDrawable);
    }

    private void setOnPlayerStateChanged() {
        if (mMusicService != null) mMusicService.setOnStateChangeListener(this);
    }

    public static String formatMillis(int millisec) {
        int seconds = millisec / 1000;
        int hours = seconds / 3600;
        seconds %= 3600;
        int minutes = seconds / 60;
        seconds %= 60;
        String time;
        if (hours > 0) {
            time = String.format("%d:%02d:%02d", new Object[]{Integer.valueOf(hours), Integer.valueOf(minutes), Integer.valueOf(seconds)});
        } else {
            time = String.format("%d:%02d", new Object[]{Integer.valueOf(minutes), Integer.valueOf(seconds)});
        }

        return time;
    }

    private ServiceConnection streamingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "PlayerFragment onServiceCreated called");
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;

            mMusicService = binder.getService();

            PlayerSession serviceSession = mMusicService.getSession();
            if (serviceSession != null && serviceSession.equals(mSession)) {
                mSession = serviceSession;
            } else {
                mMusicService.setSession(mSession, true);
            }

            mMusicService.registerCallback(PlayerFragment.this);
            setOnPlayerStateChanged();
            onStateChanged(mMusicService.isPlaying());

            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
}