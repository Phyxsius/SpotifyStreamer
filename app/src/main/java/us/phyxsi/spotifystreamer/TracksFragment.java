package us.phyxsi.spotifystreamer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;

/**
 * A placeholder fragment containing a simple view.
 */
public class TracksFragment extends Fragment implements ListView.OnItemClickListener {
    private final String LOG_TAG = TracksFragment.class.getSimpleName();

    private ArrayAdapter<Track> mTracksAdapter;
    private AbsListView mListView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TracksFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);


        mTracksAdapter = new TrackAdapter(
                getActivity(),
                R.layout.list_item_track,
                new ArrayList<Track>()
        );

        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mTracksAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mTracksAdapter = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Track track = (Track) parent.getItemAtPosition(position);

//        if (artist != null) {
//            Intent intent = new Intent(getActivity(), TracksActivity.class)
//                    .putExtra(Intent.EXTRA_TEXT, artist.name);
//
//            startActivity(intent);
//        }
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

    public void fetchTracks(String artistId) {
        new FetchTracksTask().execute(artistId);
    }


    private class FetchTracksTask extends AsyncTask<String, Void, List<Track>> {

        private boolean apiException = false;
        private SpotifyApi api;

        @Override
        protected List<Track> doInBackground(String... params) {
            try {
                api = new SpotifyApi();
                SpotifyService spotify = api.getService();

                Map<String, Object> map = new HashMap<String, Object>() {
                };
                map.put("country", "US");

                return spotify.getArtistTopTrack(params[0], map).tracks;
            } catch (Exception e) {
                this.apiException = true;
            }

            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<Track> result) {
            if (apiException)
                Toast.makeText(getActivity(),
                        getString(R.string.tracks_api_error),
                        Toast.LENGTH_SHORT).show();
            else {
                if (result.size() == 0)
                    Toast.makeText(
                            getActivity().getApplicationContext(),
                            getString(R.string.empty_tracks),
                            Toast.LENGTH_SHORT).show();
            }

            mTracksAdapter.clear();
            mTracksAdapter.addAll(result);
        }
    }
}
