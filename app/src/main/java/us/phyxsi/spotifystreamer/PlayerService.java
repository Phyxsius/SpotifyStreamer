package us.phyxsi.spotifystreamer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

import us.phyxsi.spotifystreamer.object.ParcableTrack;
import us.phyxsi.spotifystreamer.object.PlayerHelper;

public class PlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private final String LOG_TAG = PlayerService.class.getSimpleName();
    public static final String ACTION_PLAY = "PLAY";
    public static final String EXTRA_SESSION = "SESSION";

    private final IBinder streamingBinder = new StreamingBinder();

    private MediaPlayer mMediaPlayer;
    private PlayerHelper helper;

    private boolean playWhenPrepared = false;

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
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        }
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
            releaseMediaPlayer();

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
        if (mMediaPlayer.isPlaying()) return;

        playWhenPrepared = true;
        mMediaPlayer.start();
    }

    public void pause() {
        if (!mMediaPlayer.isPlaying()) return;

        playWhenPrepared = false;
        mMediaPlayer.pause();
    }

    private void stop() {
        if (!mMediaPlayer.isPlaying()) return;

        playWhenPrepared = false;
        mMediaPlayer.stop();
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

        resetMediaPlayer();
        prepareToPlay(track);
    }

    private void playPrevious() {
        ParcableTrack track = helper.getPreviousTrack();

        resetMediaPlayer();
        prepareToPlay(track);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (canPlayNext()) {
            playNext();
        } else {
            stopAndRelease();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(LOG_TAG, what + ", " + extra);
        mMediaPlayer.release();

        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (playWhenPrepared) play();
    }

    private void resetMediaPlayer() {
        if (mMediaPlayer == null) {
            initMusicPlayer();
        } else {
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
}
