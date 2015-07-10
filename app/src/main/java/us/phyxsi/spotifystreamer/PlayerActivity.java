package us.phyxsi.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import us.phyxsi.spotifystreamer.object.PlayerHelper;

public class PlayerActivity extends AppCompatActivity {

    private PlayerHelper mPlayerHelper;
    boolean mIsLargeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mIsLargeLayout = getResources().getBoolean(R.bool.large_layout);
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            mPlayerHelper = (PlayerHelper) getIntent().getParcelableExtra(PlayerFragment.PLAYER_HELPER);

            arguments.putParcelable(PlayerFragment.PLAYER_HELPER, mPlayerHelper);

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
                        .addToBackStack(null).commit();
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

    @Override
    public void setSupportActionBar(Toolbar toolbar) {
        super.setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.abc_ic_clear_mtrl_alpha);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
}
