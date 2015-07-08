package us.phyxsi.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

public class PlayerActivity extends AppCompatActivity {

    private ParcableTrack mTrack;
    boolean mIsLargeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mIsLargeLayout = getResources().getBoolean(R.bool.large_layout);
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            mTrack = (ParcableTrack) getIntent().getParcelableExtra(PlayerFragment.TRACK);

            arguments.putParcelable(PlayerFragment.TRACK, mTrack);

            PlayerFragment fragment = new PlayerFragment();
            fragment.setArguments(arguments);

            if (mIsLargeLayout) {
                // The device is using a large layout, so show the fragment as a dialog
                fragment.show(fragmentManager, "player");
            } else {
                // The device is smaller, so show the fragment fullscreen
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                // For a little polish, specify a transition animation
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                // To make it fullscreen, use the 'content' root view as the container
                // for the fragment, which is always the root view for the activity
                transaction.add(R.id.player_fragment, fragment)
                        .commit();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
