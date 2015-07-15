package us.phyxsi.spotifystreamer.player;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import us.phyxsi.spotifystreamer.object.ParcableTrack;
import us.phyxsi.spotifystreamer.object.PlayerHelper;

public class PlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        SpotifyMediaPlayer.OnStateChangeListener {
    private final String LOG_TAG = PlayerService.class.getSimpleName();
    public static final String ACTION_PLAY = "PLAY";
    public static final String EXTRA_SESSION = "SESSION";

    private final IBinder streamingBinder = new StreamingBinder();

    private SpotifyMediaPlayer mMediaPlayer;
    private PlayerHelper helper;
    private List<Callback> callbacks;
    private Handler mHandler = new Handler();
    private boolean playWhenPrepared = false;
    private boolean watchProgressUpdate = false;

    private Runnable progressUpdate = new Runnable() {
        @Override
        public void run() {
            if (callbacks != null && mMediaPlayer != null) {
                for (Callback callback : callbacks) {
                    callback.onProgressChange(mMediaPlayer.getCurrentPosition());
                }
            }

            if (watchProgressUpdate) mHandler.postDelayed(progressUpdate, 16);
        }
    };

    public PlayerHelper getHelper() {
        return helper;
    }

    public void setHelper(PlayerHelper helper) {
        setHelper(helper, false);
    }

    public void setHelper(PlayerHelper helper, boolean playWhenPrepared) {
        this.helper = helper;
        this.playWhenPrepared = playWhenPrepared;

        if (helper == null || helper.getPlaylistSize() == 0) {
            stopAndRelease();
            return;
        }

        resetMediaPlayer();
        prepareToPlay(helper.getCurrentTrack());
    }

    public PlayerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initMusicPlayer();
    }

    @Override
    public void onDestroy() {
        releaseMediaPlayer();
        super.onDestroy();
    }

    private void initMusicPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new SpotifyMediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnStateChangeListener(this);
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        }
    }

    private void setWatchProgress(boolean watchProgress) {
        watchProgressUpdate = watchProgress;
        if (watchProgress) progressUpdate.run();
    }

    private void stopAndRelease() {
        releaseMediaPlayer();
        stopForeground(true);
        stopSelf();
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public ParcableTrack getCurrentTrack() {
        return helper == null ? null : helper.getCurrentTrack();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return streamingBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if (ACTION_PLAY.equals(action)) {
            resetMediaPlayer();

            setHelper((PlayerHelper) intent.getParcelableExtra(EXTRA_SESSION));
        }

        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mMediaPlayer.stop();
        mMediaPlayer.release();
        return false;
    }

    public void play() {
        if (mMediaPlayer.getCurrentState() == SpotifyMediaPlayer.STATE_STARTED) return;

        if (mMediaPlayer.getCurrentState() == SpotifyMediaPlayer.STATE_PREPARING) {
            playWhenPrepared = true;
            return;
        }

        playWhenPrepared = true;
        mMediaPlayer.start();
        setWatchProgress(true);
    }

    public void pause() {
        if (mMediaPlayer.getCurrentState() == SpotifyMediaPlayer.STATE_PAUSED) return;

        if (mMediaPlayer.getCurrentState() == SpotifyMediaPlayer.STATE_PREPARING) {
            playWhenPrepared = false;
            return;
        }

        playWhenPrepared = false;
        mMediaPlayer.pause();
        setWatchProgress(false);
    }

    private void stop() {
        if (mMediaPlayer.getCurrentState() == SpotifyMediaPlayer.STATE_STOPPED) return;

        if (mMediaPlayer.getCurrentState() == SpotifyMediaPlayer.STATE_PREPARING) {
            playWhenPrepared = false;
            return;
        }

        playWhenPrepared = false;
        mMediaPlayer.stop();
        setWatchProgress(false);
    }

    public void togglePlay() {
        if (mMediaPlayer.isPlaying()) pause();
        else play();
    }

    public void next() {
        playNext();
    }

    public void prev() {
        if (canPlayPrev()) playPrevious();
        else seekTo(0);
    }

    public void seekTo(int position) {
        mMediaPlayer.seekTo(position);
    }

    public boolean canPlayNext() {
        if (helper == null) return false;

        int playlistSize = helper.getPlaylistSize();
        if (playlistSize <= 1) return false;

        int nextIndex = helper.getCurrentPosition() + 1;
        return nextIndex >= 0 && nextIndex < playlistSize;
    }

    public boolean canPlayPrev() {
        if (helper == null) return false;

        int playlistSize = helper.getPlaylistSize();
        if (playlistSize <= 1) return false;

        int prevIndex = helper.getCurrentPosition() - 1;
        return prevIndex >= 0 && prevIndex < playlistSize;
    }

    private void playNext() {
        ParcableTrack track = helper.getNextTrack();

        if (callbacks != null) {
            for (Callback callback : callbacks) {
                callback.onTrackChanged(track);
            }
        }

        resetMediaPlayer();
        prepareToPlay(track);
    }

    private void playPrevious() {
        ParcableTrack track = helper.getPreviousTrack();

        if (callbacks != null) {
            for (Callback callback : callbacks) {
                callback.onTrackChanged(track);
            }
        }

        resetMediaPlayer();
        prepareToPlay(track);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (canPlayNext()) {
            playNext();
        } else {
            if (callbacks != null) {
                for (Callback callback : callbacks) {
                    callback.onPlaybackStopped();
                }
            }
            stopAndRelease();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(LOG_TAG, what + ", " + extra);

        mMediaPlayer.reset();
        mMediaPlayer.release();

        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (playWhenPrepared) play();
    }

    @Override
    public void onStateChanged(@SpotifyMediaPlayer.State int state) {
        if (callbacks != null) {
            for (Callback callback : callbacks) {
                callback.onPlayerStateChanged(state);
            }
        }

        if (state == SpotifyMediaPlayer.STATE_STARTED || state == SpotifyMediaPlayer.STATE_PAUSED) {
            // TODO: Notifications
        }
    }

    private void resetMediaPlayer() {
        if (mMediaPlayer == null) {
            initMusicPlayer();
        } else if (mMediaPlayer.getCurrentState() != SpotifyMediaPlayer.STATE_IDLE) {
            mMediaPlayer.reset();
        }
    }

    private void prepareToPlay(ParcableTrack track) {
        try {
            mMediaPlayer.setDataSource(track.previewUrl);
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        mMediaPlayer.prepareAsync();
    }

    public class StreamingBinder extends Binder {
        PlayerService getService() { return PlayerService.this; }
    }

    public void registerCallback(Callback callback) {
        if (callbacks == null) {
            callbacks = new ArrayList<>();
        }

        callbacks.add(callback);
    }

    public void unregisterCallback(Callback callback) {
        if (callbacks != null) callbacks.remove(callback);
    }

    public interface Callback {
        void onProgressChange(int progress);

        void onTrackChanged(ParcableTrack track);

        void onPlaybackStopped();

        void onPlayerStateChanged(@SpotifyMediaPlayer.State int state);
    }
}
