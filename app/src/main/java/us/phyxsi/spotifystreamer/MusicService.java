package us.phyxsi.spotifystreamer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import us.phyxsi.spotifystreamer.object.ParcableTrack;
import us.phyxsi.spotifystreamer.player.PlayerActivity;
import us.phyxsi.spotifystreamer.player.PlayerSession;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = MusicService.class.getSimpleName();
    private static final long PROGRESS_UPDATE_INTERNAL = 100;
    public static final String ACTION_PLAY = "us.phyxi.spotifystreamer.action.PLAY";

    private final IBinder mMusicBinder = new MusicBinder();
    private MediaPlayer mPlayer;
    private PlayerSession mSession;

    private List<MusicServiceCallback> mCallbacks;

    private OnStateChangeListener mOnStateChangeListener;
    private Handler mHandler = new Handler();
    private boolean watchProgressUpdate = false;
    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            if (mCallbacks != null && mPlayer != null) {
                for (MusicServiceCallback callback : mCallbacks) {
                    try {
                        callback.onProgressChange(mPlayer.getCurrentPosition());
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (watchProgressUpdate) mHandler.postDelayed(this, PROGRESS_UPDATE_INTERNAL);
        }
    };

    public PlayerSession getSession(){
        return mSession;
    }

    public void setSession(PlayerSession session) {
        setSession(session, false);
    }

    public void setSession(PlayerSession session, boolean playWhenPrepared) {
        this.mSession = session;

        if (session == null || session.getPlaylistSize() == 0) {
            return;
        }

        initializePlayer();

        play();
    }

    public boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    public void registerCallback(MusicServiceCallback callback) {
        if (mCallbacks == null) mCallbacks = new ArrayList<>();

        mCallbacks.add(callback);
    }

    public void unregisterCallback(MusicServiceCallback callback) {
        if (mCallbacks == null) return;

        mCallbacks.remove(callback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMusicBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (ACTION_PLAY.equals(action)) {
            if (mPlayer == null) initializePlayer();
            else mPlayer.reset();

            setSession((PlayerSession) intent.getParcelableExtra(PlayerActivity.PLAYER_SESSION));
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "MusicService onCreate");
        super.onCreate();

        initializePlayer();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mPlayer.stop();
        mPlayer.release();

        return false;
    }

    public void initializePlayer() {
        this.mPlayer = new MediaPlayer();

        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);
    }

    public void play() {
        prepareToPlay(mSession.getCurrentTrack());

        changeState(true);
        setWatchProgressUpdate(true);
    }

    public void resume() {
        mPlayer.start();

        changeState(true);
        setWatchProgressUpdate(true);
    }

    public void pause() {
        mPlayer.pause();

        changeState(false);
        setWatchProgressUpdate(false);
    }

    private void stop() {
        mPlayer.stop();

        changeState(false);
        setWatchProgressUpdate(false);
    }

    public void togglePlay() {
        if (mPlayer == null) return;

        if (mPlayer.isPlaying()) pause();
        else resume();
    }

    public void playNext() {
        if (canPlayNext()) {
            ParcableTrack track = mSession.getNextTrack();

            if (mCallbacks != null) {
                for (MusicServiceCallback callback : mCallbacks) {
                    callback.onTrackChanged(track);
                }
            }

            prepareToPlay(track);
        }
    }

    public void playPrevious() {
        if (canPlayPrev()) {
            ParcableTrack track = mSession.getPreviousTrack();

            if (mCallbacks != null) {
                for (MusicServiceCallback callback : mCallbacks) {
                    callback.onTrackChanged(track);
                }
            }

            prepareToPlay(track);
        } else
            seekTo(0);
    }

    public void seekTo(int position) {
        mPlayer.seekTo(position);
    }

    private void setWatchProgressUpdate(boolean watchProgress) {
        this.watchProgressUpdate = watchProgress;

        if (watchProgress) {
            mUpdateProgressTask.run();
        }
    }

    public boolean canPlayNext() {
        if (mSession == null) return false;

        int playlistSize = mSession.getPlaylistSize();
        if (playlistSize <= 1) return false;

        int nextIndex = mSession.getCurrentPosition() + 1;
        return nextIndex >= 0 && nextIndex < playlistSize;
    }

    public boolean canPlayPrev() {
        if (mSession == null) return false;

        int playlistSize = mSession.getPlaylistSize();
        if (playlistSize <= 1) return false;

        int prevIndex = mSession.getCurrentPosition() - 1;
        return prevIndex >= 0 && prevIndex < playlistSize;
    }

    public int getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    private void prepareToPlay(ParcableTrack track) {
        if (mPlayer == null) initializePlayer();

        mPlayer.reset();

        try {
            mPlayer.setDataSource(track.previewUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mPlayer.prepareAsync();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        changeState(true);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mPlayer.reset();

        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (canPlayNext()) playNext();
        else {
            if (mCallbacks != null) {
                for (MusicServiceCallback callback : mCallbacks) {
                    callback.onPlaybackStopped();
                }
            }

            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }

    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        this.mOnStateChangeListener = onStateChangeListener;
    }

    private void changeState(boolean isPlaying) {
        if (mOnStateChangeListener != null) {
            mOnStateChangeListener.onStateChanged(isPlaying);
        }
    }

    public static interface OnStateChangeListener {
        public void onStateChanged(boolean isPlaying);
    }

    public interface MusicServiceCallback {

        /**
         * Called when the progress of a track has changed
         * @param progress the progress of the track in milliseconds
         */
        public void onProgressChange(int progress);

        /**
         * Called when a new track is set
         * @param track the new track to be played
         */
        public void onTrackChanged(ParcableTrack track);

        /**
         * Called when the playlist is completed and playback stopped
         */
        public void onPlaybackStopped();
    }
}
