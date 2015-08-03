package us.phyxsi.spotifystreamer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import us.phyxsi.spotifystreamer.object.ParcableTrack;
import us.phyxsi.spotifystreamer.ui.PlaybackControlsFragment;
import us.phyxsi.spotifystreamer.utils.NetworkHelper;

/**
 * Base activity for activities that need to show a playback control fragment when media is playing.
 */
public abstract class BaseActivity extends AppCompatActivity implements MusicService.MusicServiceCallback {

    private static final String TAG = BaseActivity.class.getSimpleName();

    private Toolbar mToolbar;
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

    public void showPlaybackControls() {
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

    protected void initializeToolbar(int menu) {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar == null) {
            throw new IllegalStateException("Layout is required to include a Toolbar with id " +
                    "'toolbar'");
        }
        mToolbar.inflateMenu(menu);

        setSupportActionBar(mToolbar);

        boolean isRoot = getFragmentManager().getBackStackEntryCount() == 0;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(!isRoot);
            getSupportActionBar().setDisplayHomeAsUpEnabled(!isRoot);
            getSupportActionBar().setHomeButtonEnabled(!isRoot);
        }
    }



    @Override
    public void onProgressChange(int progress) {

    }

    @Override
    public void onTrackChanged(ParcableTrack track) {

    }

    @Override
    public void onPlaybackStopped() {

    }
}
