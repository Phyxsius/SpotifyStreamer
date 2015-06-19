package us.phyxsi.spotifystreamer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;

/**
 * A placeholder fragment containing a simple view.
 */
public class TracksFragment extends Fragment implements ListView.OnItemClickListener {
    private final String LOG_TAG = TracksFragment.class.getSimpleName();

    private ArrayAdapter<Track> mTracksAdapter;

    public TracksFragment() {
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

        ListView listView = (ListView) view.findViewById(R.id.listview_items);
        listView.setAdapter(mTracksAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mTracksAdapter = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Artist artist = (Artist) parent.getItemAtPosition(position);

//        if (artist != null) {
//            Intent intent = new Intent(getActivity(), TracksActivity.class)
//                    .putExtra(Intent.EXTRA_TEXT, artist.name);
//
//            startActivity(intent);
//        }
    }

    public void fetchTracks(String artistId) {
        new FetchTracksTask().execute(artistId);
    }


    private class FetchTracksTask extends AsyncTask<String, Void, List<Track>> {

        private SpotifyApi api;

        @Override
        protected List<Track> doInBackground(String... params) {
            api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            Map<String, Object> map = new HashMap<String, Object>() {};
            map.put("country", "US");

            return spotify.getArtistTopTrack(params[0], map).tracks;
        }

        @Override
        protected void onPostExecute(List<Track> result) {
            if (result.size() == 0)
                Toast.makeText(
                        getActivity().getApplicationContext(),
                        getString(R.string.empty_tracks),
                        Toast.LENGTH_SHORT).show();

            mTracksAdapter.clear();
            mTracksAdapter.addAll(result);
        }
    }
}
