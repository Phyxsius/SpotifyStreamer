package us.phyxsi.spotifystreamer.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.IntDef;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class SpotifyMediaPlayer extends MediaPlayer implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener {
    public static final int STATE_EMPTY = -1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_INITIALIZED = 1;
    public static final int STATE_PREPARING = 2;
    public static final int STATE_PREPARED = 3;
    public static final int STATE_STARTED = 4;
    public static final int STATE_PAUSED = 5;
    public static final int STATE_STOPPED = 6;
    public static final int STATE_PLAYBACK_COMPLETED = 7;
    public static final int STATE_ERROR = 8;
    public static final int STATE_END = 9;

    @IntDef({STATE_EMPTY, STATE_IDLE, STATE_INITIALIZED, STATE_PREPARING, STATE_PREPARED,
            STATE_STARTED, STATE_PAUSED, STATE_STOPPED, STATE_PLAYBACK_COMPLETED, STATE_ERROR,
            STATE_END})
    public @interface State{}

    @State
    private int currentState = STATE_EMPTY;
    private OnCompletionListener onCompletionListener;
    private OnErrorListener onErrorListener;
    private OnPreparedListener onPreparedListener;
    private OnStateChangeListener onStateChangeListener;

    public SpotifyMediaPlayer() {
        changeToState(STATE_IDLE);
    }

    @State
    public int getCurrentState() { return this.currentState; }

    private boolean changeToState(@State int state) {
        if (canChangeState(this.currentState, state)) {
            setCurrentState(state);

            return true;
        }

        return false;
    }

    private void setCurrentState(@State int state) {
        this.currentState = state;
        if (onStateChangeListener != null) {
            onStateChangeListener.onStateChanged(state);
        }
    }

    private boolean canChangeState(@State int state) {
        return canChangeState(this.currentState, state);
    }

    private boolean canChangeState(@State int fromState, @State int toState) {
        switch (toState) {
            case STATE_IDLE:
                return true;
            case STATE_INITIALIZED:
                return fromState == STATE_IDLE;
            case STATE_PREPARING:
                return Arrays.asList(STATE_INITIALIZED, STATE_STOPPED)
                        .contains(fromState);
            case STATE_PREPARED:
                return Arrays.asList(STATE_INITIALIZED, STATE_PREPARED,
                        STATE_PREPARING, STATE_STOPPED)
                        .contains(fromState);
            case STATE_STARTED:
                return Arrays.asList(STATE_PREPARED, STATE_STARTED,
                        STATE_PAUSED, STATE_PLAYBACK_COMPLETED)
                        .contains(fromState);
            case STATE_PAUSED:
                return Arrays.asList(STATE_STARTED, STATE_PAUSED)
                        .contains(fromState);
            case STATE_STOPPED:
                return Arrays.asList(STATE_PREPARED, STATE_STARTED,
                        STATE_PAUSED, STATE_PLAYBACK_COMPLETED, STATE_STOPPED)
                        .contains(fromState);
            case STATE_PLAYBACK_COMPLETED:
                return Arrays.asList(STATE_STARTED, STATE_PLAYBACK_COMPLETED)
                        .contains(fromState);
            case STATE_ERROR:
                return true;
            case STATE_END:
                return true;
            default:
                return false;
        }
    }

    // Media control overrides
    @Override
    public void start() throws IllegalStateException {
        if (canChangeState(STATE_STARTED)) {
            super.start();
            setCurrentState(STATE_STARTED);
        }
    }

    @Override
    public void pause() throws IllegalStateException {
        if (canChangeState(STATE_PAUSED)) {
            super.pause();
            setCurrentState(STATE_PAUSED);
        }
    }

    @Override
    public void stop() throws IllegalStateException {
        if (canChangeState(STATE_STOPPED)) {
            super.stop();
            setCurrentState(STATE_STOPPED);
        }
    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {
        if (canChangeState(this.currentState)) {
            super.seekTo(msec);
        }
    }

    @Override
    public void prepare() throws IOException, IllegalStateException {
        if (canChangeState(STATE_PREPARED)) {
            super.prepare();
            setCurrentState(STATE_PREPARED);
        }
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        if (canChangeState(STATE_PREPARING)) {
            super.prepareAsync();
            setCurrentState(STATE_PREPARING);
        }
    }

    @Override
    public void release() {
        if (canChangeState(STATE_END)) {
            super.release();
            setCurrentState(STATE_END);
            setOnCompletionListener(null);
            setOnErrorListener(null);
            setOnPreparedListener(null);
            setOnStateChangeListener(null);
        }
    }

    @Override
    public void reset() {
        if (canChangeState(STATE_IDLE)) {
            super.reset();
            setCurrentState(STATE_IDLE);
        }
    }

    // Implementation overrides
    @Override
    public void onCompletion(MediaPlayer mp) {
        setCurrentState(isLooping() ? STATE_STARTED : STATE_PLAYBACK_COMPLETED);

        if (onCompletionListener != null) onCompletionListener.onCompletion(mp);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        setCurrentState(STATE_ERROR);
        return onErrorListener != null && onErrorListener.onError(mp, what, extra);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        setCurrentState(STATE_PREPARED);
        if (onPreparedListener != null) onPreparedListener.onPrepared(mp);
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        onCompletionListener = listener;
        super.setOnCompletionListener(this);
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        onErrorListener = listener;
        super.setOnErrorListener(this);
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        onPreparedListener = listener;
        super.setOnPreparedListener(this);
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        onStateChangeListener = listener;
    }

    // Data source overrides
    @Override
    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        if (canChangeState(STATE_INITIALIZED)) {
            super.setDataSource(context, uri);
            changeToState(STATE_INITIALIZED);
        }
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        if (canChangeState(STATE_INITIALIZED)) {
            super.setDataSource(context, uri, headers);
            changeToState(STATE_INITIALIZED);
        }
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        if (canChangeState(STATE_INITIALIZED)) {
            super.setDataSource(path);
            changeToState(STATE_INITIALIZED);
        }
    }

    @Override
    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException {
        if (canChangeState(STATE_INITIALIZED)) {
            super.setDataSource(fd);
            changeToState(STATE_INITIALIZED);
        }
    }

    @Override
    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException, IllegalArgumentException, IllegalStateException {
        if (canChangeState(STATE_INITIALIZED)) {
            super.setDataSource(fd, offset, length);
            changeToState(STATE_INITIALIZED);
        }
    }

    public interface OnStateChangeListener {
        void onStateChanged(@State int state);
    }
}
