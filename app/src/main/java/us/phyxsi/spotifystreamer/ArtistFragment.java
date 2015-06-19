package us.phyxsi.spotifystreamer;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class ArtistFragment extends Fragment implements ListView.OnItemClickListener {
    private final String LOG_TAG = ArtistFragment.class.getSimpleName();
    public final static String ARTIST_ID = "ARTIST_ID";
    public final static String ARTIST_NAME = "ARTIST_NAME";

    private ArrayAdapter<Artist> mArtistAdapter;
    private AbsListView mListView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArtistFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);


        mArtistAdapter = new ArtistAdapter(
                getActivity(),
                R.layout.list_item_artist,
                new ArrayList<Artist>()
        );

        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mArtistAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mArtistAdapter = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Artist artist = (Artist) parent.getItemAtPosition(position);

        if (artist != null) {
            Intent intent = new Intent(getActivity(), TracksActivity.class)
                    .putExtra(ARTIST_ID, artist.id)
                    .putExtra(ARTIST_NAME, artist.name);

            startActivity(intent);
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    public void fetchArtist(String artist) {
        new FetchArtistTask().execute(artist);
    }


    private class FetchArtistTask extends AsyncTask<String, Void, List<Artist>> {

        private boolean apiException = false;
        private SpotifyApi api;

        @Override
        protected List<Artist> doInBackground(String... params) {
            try {
                api = new SpotifyApi();
                SpotifyService spotify = api.getService();

                return spotify.searchArtists(params[0]).artists.items;
            } catch (Exception e) {
                this.apiException = true;
            }

            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<Artist> result) {
            if (apiException)
                Toast.makeText(getActivity(),
                        getString(R.string.artist_api_error),
                        Toast.LENGTH_SHORT).show();
            else {
                if (result.size() == 0) {
                    Toast.makeText(
                            getActivity().getApplicationContext(),
                            getString(R.string.empty_artist),
                            Toast.LENGTH_SHORT).show();
                }
            }

            mArtistAdapter.clear();
            mArtistAdapter.addAll(result);
        }
    }
}
