package us.phyxsi.spotifystreamer.player;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import us.phyxsi.spotifystreamer.MusicService;
import us.phyxsi.spotifystreamer.R;
import us.phyxsi.spotifystreamer.object.ParcableTrack;
import us.phyxsi.spotifystreamer.object.PlayerHelper;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class PlayerFragment extends DialogFragment {

    private static final String TAG = PlayerFragment.class.getSimpleName();
    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

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

    private final Handler mHandler = new Handler();
    private MediaBrowser mMediaBrowser;

    private PlayerHelper mPlayerHelper;
    private ParcableTrack mTrack;
    private Intent serviceIntent;
    private boolean serviceBound = false;
    private boolean isPlaying = false;
    private boolean viewsAreCreated = false;

    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final ScheduledExecutorService mExecutorService =
            Executors.newSingleThreadScheduledExecutor();
    public PlayerFragment() {
    }

    private ScheduledFuture<?> mScheduleFuture;
    private PlaybackState mLastPlaybackState;

    private final MediaController.Callback mCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            Log.d(TAG, "onPlaybackstate changed" + state);
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            if (metadata != null) {
                updateMediaDescription(metadata.getDescription());
                updateDuration(metadata);
            }
        }
    };

    private final MediaBrowser.ConnectionCallback mConnectionCallback =
        new MediaBrowser.ConnectionCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "onConnected");
                connectToSession(mMediaBrowser.getSessionToken());
            }
        };



    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupViews();

        viewsAreCreated = true;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);

        initializeViews(view);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mPlayerHelper = arguments.getParcelable(PlayerActivity.PLAYER_HELPER);

            assert mPlayerHelper != null;
            mTrack = mPlayerHelper.getCurrentTrack();

            setViewsInfo(mTrack);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        viewsAreCreated = false;
        super.onDestroyView();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        getActivity().finish();
    }

    private void initializeViews(View view) {
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
    }

    private void setupViews() {
        // ActionBar
        if(getActivity() != null && getActivity() instanceof AppCompatActivity){
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.setSupportActionBar(mToolbar);
        }

        // Set toolbar title
        mToolbar.setTitle(getString(R.string.now_playing));

        // Player controls
        mSkipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaController.TransportControls controls =
                        getActivity().getMediaController().getTransportControls();
                controls.skipToPrevious();
            }
        });

        mSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaController.TransportControls controls =
                        getActivity().getMediaController().getTransportControls();
                controls.skipToNext();
            }
        });

        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackState state = getActivity().getMediaController().getPlaybackState();
                if (state != null) {
                    MediaController.TransportControls controls =
                            getActivity().getMediaController().getTransportControls();
                    switch (state.getState()) {
                        case PlaybackState.STATE_PLAYING: // fall through
                        case PlaybackState.STATE_BUFFERING:
                            controls.pause();
                            stopSeekbarUpdate();
                            break;
                        case PlaybackState.STATE_PAUSED:
                        case PlaybackState.STATE_STOPPED:
                            controls.play();
                            scheduleSeekbarUpdate();
                            break;
                        default:
                            Log.d(TAG, "onClick with state " + state.getState());
                    }
                }
            }
        });

        // Seekbar
        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean userInitiated = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mStart.setText(formatMillis(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getActivity().getMediaController().getTransportControls().seekTo(seekBar.getProgress());
                scheduleSeekbarUpdate();
            }
        });

        mMediaBrowser = new MediaBrowser(getActivity(),
                new ComponentName(getActivity(), MusicService.class), mConnectionCallback, null);
    }


    private void connectToSession(MediaSession.Token token) {
        MediaController mediaController = new MediaController(getActivity(), token);
        if (mediaController.getMetadata() == null) {
            getActivity().finish();
            return;
        }
        getActivity().setMediaController(mediaController);
        mediaController.registerCallback(mCallback);
        PlaybackState state = mediaController.getPlaybackState();
        updatePlaybackState(state);
        MediaMetadata metadata = mediaController.getMetadata();
        if (metadata != null) {
            setViewsInfo(mTrack);
            updateDuration(metadata);
        }
        updateProgress();
        if (state != null && (state.getState() == PlaybackState.STATE_PLAYING ||
                state.getState() == PlaybackState.STATE_BUFFERING)) {
            scheduleSeekbarUpdate();
        }
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(mUpdateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mMediaBrowser != null) {
            mMediaBrowser.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
        if (getActivity().getMediaController() != null) {
            getActivity().getMediaController().unregisterCallback(mCallback);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSeekbarUpdate();
        mExecutorService.shutdown();
    }

    private void setViewsInfo(ParcableTrack track) {
        if (viewsAreCreated) {
            // Set the album image
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int imageDimension = Math.round(86 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

            if (!TextUtils.isEmpty(track.imageUrl)) {
                Picasso.with(getActivity())
                        .load(track.imageUrl)
                        .into(mAlbumImage);
            }

            // Artist and track details
            mLine2.setText(track.name);
            mLine1.setText(track.artist);
            mSeekbar.setMax(track.getDurationInMilli());

            // Change widget colors based on album art
            Picasso.with(getActivity()).load(track.imageUrl).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
                            Palette.Swatch mutedSwatch = palette.getMutedSwatch();
                            Palette.Swatch darkMutedSwatch = palette.getDarkMutedSwatch();

                            mLine2.setBackgroundColor(getSwatchColor(mutedSwatch));
                            mLine2.setTextColor(mutedSwatch != null ?
                                    mutedSwatch.getBodyTextColor() : 0x000000);
                            mLine1.setBackgroundColor(getSwatchColor(mutedSwatch));
                            mLine1.setTextColor(mutedSwatch != null ?
                                    mutedSwatch.getTitleTextColor() : 0x000000);

                            mControls.setBackgroundColor(getSwatchColor(darkMutedSwatch));


                            // Set statusbar and seekbar color on Lollipop+
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                mSeekbar.getThumb().setTint(getSwatchColor(vibrantSwatch));
                                getActivity().getWindow().setStatusBarColor(getSwatchColor(mutedSwatch));
                            }
                        }
                    });
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }

                private int getSwatchColor(Palette.Swatch swatch) {
                    return swatch != null ? swatch.getRgb() : 0x000000;
                }
            });
        }
    }

    private void updateMediaDescription(MediaDescription description) {
        if (description == null) {
            return;
        }
        Log.d(TAG, "updateMediaDescription called ");
        mLine1.setText(description.getTitle());
        mLine2.setText(description.getSubtitle());
//        fetchImageAsync(description);
    }

    private void updateDuration(MediaMetadata metadata) {
        if (metadata == null) {
            return;
        }
        Log.d(TAG, "updateDuration called ");
        int duration = (int) metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        mSeekbar.setMax(duration);
        mEnd.setText(formatMillis(duration));
    }

    private void updatePlaybackState(PlaybackState state) {
        if (state == null) {
            return;
        }
        mLastPlaybackState = state;

        switch (state.getState()) {
            case PlaybackState.STATE_PLAYING:
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPauseDrawable);
                mControls.setVisibility(VISIBLE);
                scheduleSeekbarUpdate();
                break;
            case PlaybackState.STATE_PAUSED:
                mControls.setVisibility(VISIBLE);
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackState.STATE_NONE:
            case PlaybackState.STATE_STOPPED:
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackState.STATE_BUFFERING:
                mPlayPause.setVisibility(INVISIBLE);
                stopSeekbarUpdate();
                break;
            default:
                Log.d(TAG, "Unhandled state " + state.getState());
        }

        mSkipNext.setVisibility((state.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) == 0
                ? INVISIBLE : VISIBLE);
        mSkipPrev.setVisibility((state.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) == 0
                ? INVISIBLE : VISIBLE );
    }

    private void updateProgress() {
        if (mLastPlaybackState == null) {
            return;
        }
        long currentPosition = mLastPlaybackState.getPosition();
        if (mLastPlaybackState.getState() != PlaybackState.STATE_PAUSED) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaController.
            long timeDelta = SystemClock.elapsedRealtime() -
                    mLastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
        }
        mSeekbar.setProgress((int) currentPosition);
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
}