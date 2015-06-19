package us.phyxsi.spotifystreamer;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class ArtistFragment extends Fragment implements ListView.OnItemClickListener {
    private final String LOG_TAG = ArtistFragment.class.getSimpleName();
    public final static String ARTIST_ID = "ARTIST_ID";
    public final static String ARTIST_NAME = "ARTIST_NAME";

    private ArrayAdapter<Artist> mArtistAdapter;

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

        ListView listView = (ListView) view.findViewById(R.id.listview_items);
        listView.setAdapter(mArtistAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        listView.setOnItemClickListener(this);

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
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);
    }

    public void fetchArtist(String artist) {
        new FetchArtistTask().execute(artist);
    }


    private class FetchArtistTask extends AsyncTask<String, Void, List<Artist>> {

        private final String LOG_TAG = FetchArtistTask.class.getSimpleName();

        private SpotifyApi api;

        @Override
        protected List<Artist> doInBackground(String... params) {
            api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            return spotify.searchArtists(params[0]).artists.items;
        }

        @Override
        protected void onPostExecute(List<Artist> result) {
            if (result.size() == 0)
                Toast.makeText(
                        getActivity().getApplicationContext(),
                        getString(R.string.empty_artist),
                        Toast.LENGTH_SHORT).show();

            mArtistAdapter.clear();
            mArtistAdapter.addAll(result);
        }
    }
}
