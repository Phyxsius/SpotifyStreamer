package us.phyxsi.spotifystreamer;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
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
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class ArtistFragment extends Fragment {
    private final String LOG_TAG = ArtistFragment.class.getSimpleName();

    private ArrayAdapter<Artist> mArtistAdapter;

    private OnFragmentInteractionListener mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

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
        View view = inflater.inflate(R.layout.fragment_artist, container, false);


        mArtistAdapter = new ArtistAdapter(
                getActivity(),
                R.layout.list_item_artist,
                new ArrayList<Artist>()
        );

        ListView listView = (ListView) view.findViewById(R.id.listview_artist);
        listView.setAdapter(mArtistAdapter);

//        // Set OnItemClickListener so we can be notified on item clicks
//        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mArtistAdapter = null;
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

    public void searchArtist(String artist) {
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
