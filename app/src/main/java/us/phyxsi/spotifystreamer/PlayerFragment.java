package us.phyxsi.spotifystreamer;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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

import us.phyxsi.spotifystreamer.object.ParcableTrack;
import us.phyxsi.spotifystreamer.object.PlayerHelper;

public class PlayerFragment extends DialogFragment {

    static final String PLAYER_HELPER = "PLAYER_HELPER";

    private PlayerService mPlayerService;
    private PlayerHelper mPlayerHelper;
    private ParcableTrack mTrack;
    private Intent serviceIntent;
    private boolean serviceBound = false;
    private boolean isPlaying = false;

    // Views
    private Toolbar toolbar;
    private ImageView albumImage;
    private TextView trackTitle;
    private TextView artistName;
    private LinearLayout playerControls;
    private SeekBar seekBar;
    private ImageView prevButton;
    private ImageView nextButton;
    private ImageView playPauseButton;

    public PlayerFragment() {
    }

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

//        MediaPlayer mediaPlayer = new MediaPlayer();
//        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//        try {
//            mediaPlayer.setDataSource(mPlayerHelper.getCurrentTrack().previewUrl);
//            mediaPlayer.prepare();
//            mediaPlayer.start();
//        } catch (IOException e) {
//
//        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);

        initializeViews(view);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mPlayerHelper = arguments.getParcelable(PlayerFragment.PLAYER_HELPER);

            mTrack = mPlayerHelper.getCurrentTrack();

            setViewsInfo(mTrack);
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (serviceIntent == null) {
            serviceIntent = new Intent(activity, PlayerService.class);
            activity.bindService(serviceIntent, streamingConnection, Context.BIND_AUTO_CREATE);
            activity.startService(serviceIntent);
        }
    }

    @Override
    public void onDestroy() {
        unbindService();
        super.onDestroy();
    }



    private void initializeViews(View view) {
        toolbar = (Toolbar) view.findViewById(R.id.actionbar);
        albumImage = (ImageView) view.findViewById(R.id.player_album_image);
        trackTitle = (TextView) view.findViewById(R.id.player_track_title);
        artistName = (TextView) view.findViewById(R.id.player_artist_name);
        playerControls = (LinearLayout) view.findViewById(R.id.player_control_layout);
        seekBar = (SeekBar) view.findViewById(R.id.player_seekbar);

        prevButton = (ImageView) view.findViewById(R.id.player_control_prev);
        playPauseButton = (ImageView) view.findViewById(R.id.player_control_play_pause);
        nextButton = (ImageView) view.findViewById(R.id.player_control_next);
    }

    private void setupViews() {
        // ActionBar
        if(getActivity() != null && getActivity() instanceof AppCompatActivity){
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.setSupportActionBar(toolbar);
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService();
            }
        });

        // Player controls
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerService != null) mPlayerService.prev();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerService != null) mPlayerService.next();
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPlaying = !isPlaying;
                if (mPlayerService != null) mPlayerService.togglePlay();
            }
        });

        // Seekbar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO: Change the progress text
                if (mPlayerService != null) mPlayerService.seekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setViewsInfo(ParcableTrack track) {
        // Set toolbar title
        toolbar.setTitle(getString(R.string.now_playing));

        // Set the album image
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int imageDimension = Math.round(86 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

        if (!TextUtils.isEmpty(mTrack.imageUrl)) {
            Picasso.with(getActivity())
                    .load(mTrack.imageUrl)
                    .into(albumImage);
        }

        // Artist and track details
        trackTitle.setText(mTrack.name);
        artistName.setText(mTrack.artist);
        seekBar.setMax(mTrack.getDurationInMilli());

        // Change widget colors based on album art
        Picasso.with(getActivity()).load(mTrack.imageUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
                        Palette.Swatch mutedSwatch = palette.getMutedSwatch();
                        Palette.Swatch darkMutedSwatch = palette.getDarkMutedSwatch();

                        trackTitle.setBackgroundColor(getSwatchColor(mutedSwatch));
                        trackTitle.setTextColor(mutedSwatch != null ?
                                mutedSwatch.getBodyTextColor() : 0x000000);
                        artistName.setBackgroundColor(getSwatchColor(mutedSwatch));
                        artistName.setTextColor(mutedSwatch != null ?
                                mutedSwatch.getTitleTextColor() : 0x000000);

                        playerControls.setBackgroundColor(getSwatchColor(darkMutedSwatch));


                        // Set statusbar and seekbar color on Lollipop+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            seekBar.getThumb().setTint(getSwatchColor(vibrantSwatch));
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

    private ServiceConnection streamingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerService.StreamingBinder binder = (PlayerService.StreamingBinder) service;

            mPlayerService = binder.getService();

            PlayerHelper helper = mPlayerService.getHelper();

            if (helper != null && helper.equals(mPlayerHelper)) {
                mPlayerHelper = helper;
            } else {
                mPlayerService.setHelper(mPlayerHelper, true);
            }

            setViewsInfo(mPlayerService.getCurrentTrack());

            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    public void unbindService() {
        if (mPlayerService != null) {
//            mPlayerService.unregisterCallback(null);
            getActivity().unbindService(streamingConnection);
            mPlayerService = null;
        }
    }
}