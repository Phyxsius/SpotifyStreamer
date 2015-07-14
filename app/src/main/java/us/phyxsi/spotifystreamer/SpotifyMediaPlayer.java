package us.phyxsi.spotifystreamer;

import android.media.MediaPlayer;
import android.support.annotation.IntDef;

import java.util.Arrays;

/**
 * Created by arose on 7/14/2015.
 */
public class SpotifyMediaPlayer extends MediaPlayer {
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
    private OnStateChangeListener onStateChangeListener;

    @State
    private int currentState = STATE_EMPTY;

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
                return Arrays.asList(STATE_IDLE)
                        .contains(fromState);
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

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {

        super.setOnPreparedListener(listener);
    }

    public static interface OnStateChangeListener {
        public void onStateChanged(@State int state);
    }
}
