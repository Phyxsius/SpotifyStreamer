package us.phyxsi.spotifystreamer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import java.io.IOException;

import us.phyxsi.spotifystreamer.object.ParcableTrack;
import us.phyxsi.spotifystreamer.player.PlayerSession;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = MusicService.class.getSimpleName();

    private final IBinder mMusicBinder = new MusicBinder();
    private MediaPlayer mPlayer;
    private PlayerSession mSession;

    public PlayerSession getSession(){
        return mSession;
    }

    public void setSesion(PlayerSession session) {
        setSession(session, false);
    }

    public void setSession(PlayerSession session, boolean playWhenPrepared) {
        this.mSession = session;
        if (session == null || session.getPlaylistSize() == 0) {
            return;
        }

        play();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMusicBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.mPlayer = new MediaPlayer();

        initializePlayer();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mPlayer.stop();
        mPlayer.release();

        return false;
    }

    public void initializePlayer() {
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);
    }

    public void play() {
        mPlayer.reset();

        prepareToPlay(mSession.getCurrentTrack());
    }

    private void prepareToPlay(ParcableTrack track) {
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
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }
}
