package us.phyxsi.spotifystreamer;

import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import us.phyxsi.spotifystreamer.ui.PlaybackControlsFragment;
import us.phyxsi.spotifystreamer.utils.NetworkHelper;

/**
 * Base activity for activities that need to show a playback control fragment when media is playing.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    private MediaBrowser mMediaBrowser;
    private PlaybackControlsFragment mControlsFragment;

    public static boolean mIsLargeLayout;

    public boolean ismIsLargeLayout() {
        return mIsLargeLayout;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Activity onCreate");

        mIsLargeLayout = getResources().getBoolean(R.bool.large_layout);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "Activity onStart");

        mControlsFragment = (PlaybackControlsFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_playback_controls);
        if (mControlsFragment == null) {
            throw new IllegalStateException("Mising fragment with id 'controls'. Cannot continue.");
        }

        hidePlaybackControls();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "Activity onStop");
    }

    protected void showPlaybackControls() {
        Log.d(TAG, "showPlaybackControls");
        if (NetworkHelper.isOnline(this)) {
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom,
                            R.animator.slide_in_from_bottom, R.animator.slide_out_to_bottom)
                    .show(mControlsFragment)
                    .commit();
        }
    }

    protected void hidePlaybackControls() {
        Log.d(TAG, "hidePlaybackControls");
        getFragmentManager().beginTransaction()
                .hide(mControlsFragment)
                .commit();
    }
}
