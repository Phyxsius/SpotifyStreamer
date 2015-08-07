package us.phyxsi.spotifystreamer.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import us.phyxsi.spotifystreamer.MusicService;
import us.phyxsi.spotifystreamer.R;
import us.phyxsi.spotifystreamer.object.ParcableArtist;
import us.phyxsi.spotifystreamer.utils.NetworkHelper;

public class MainActivity extends BaseActivity implements ArtistFragment.Callback {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String TRACKSFRAGMENT_TAG = "TFTAG";

    ArtistFragment artistFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initializeToolbar(R.menu.menu_main);

        if (findViewById(R.id.tracks_container) != null) {
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.tracks_container, new TracksFragment(), TRACKSFRAGMENT_TAG)
                        .commit();
            }
        }

        startService(new Intent(this, MusicService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (NetworkHelper.isOnline(getApplicationContext())) {
                    artistFragment = (ArtistFragment) getFragmentManager().findFragmentById(R.id.fragment_artist);
                    artistFragment.fetchArtist(query);

                    searchView.clearFocus();
                } else {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.network_offline),
                            Toast.LENGTH_LONG).show();

                    return false;
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

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
    public void onItemSelected(ParcableArtist artist) {
        if (mIsLargeLayout) {
            Bundle args = new Bundle();
            args.putParcelable(TracksFragment.ARTIST, artist);

            TracksFragment fragment = new TracksFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.tracks_container, fragment, TRACKSFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, TracksActivity.class)
                    .putExtra(TracksFragment.ARTIST, artist);

            startActivity(intent);
        }
    }
}
