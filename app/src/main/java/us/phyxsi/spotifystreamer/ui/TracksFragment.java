package us.phyxsi.spotifystreamer.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.RetrofitError;
import us.phyxsi.spotifystreamer.R;
import us.phyxsi.spotifystreamer.object.ParcableArtist;
import us.phyxsi.spotifystreamer.object.ParcableTrack;
import us.phyxsi.spotifystreamer.object.PlayerSession;
import us.phyxsi.spotifystreamer.utils.TrackAdapter;

/**
 * A placeholder fragment containing a simple view.
 */
public class TracksFragment extends Fragment implements ListView.OnItemClickListener {
    private final String LOG_TAG = TracksFragment.class.getSimpleName();
    private final String TRACK_LIST = "TRACK_LIST";
    static final String ARTIST = "ARTIST";

    private ArrayAdapter<ParcableTrack> mTracksAdapter;
    private AbsListView mListView;
    private ArrayList<ParcableTrack> mTracks;
    private ParcableArtist mArtist;

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

        Bundle arguments = getArguments();
        if (arguments != null) {
            mArtist = arguments.getParcelable(TracksFragment.ARTIST);
        }

        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mTracksAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mTracks = (ArrayList<ParcableTrack>) savedInstanceState.get(TRACK_LIST);
        } else {
            mTracks = new ArrayList<>();

            // No previous results found so we can fetch the tracks
            // for the first time for this artist
            if (null != mArtist)
                this.fetchTracks(mArtist.id);
        }

        mTracksAdapter = new TrackAdapter(
                getActivity(),
                R.layout.list_item_track,
                mTracks
        );

        mTracksAdapter.notifyDataSetChanged();
        mListView.setAdapter(mTracksAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(TRACK_LIST, mTracks);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mTracksAdapter = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ParcableTrack track = (ParcableTrack) parent.getItemAtPosition(position);

        if (track != null) {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

            PlayerSession session = new PlayerSession(mArtist, mTracks, position);

            if (MainActivity.mIsLargeLayout) {
                FullScreenPlayerFragment fragment = new FullScreenPlayerFragment();
                Bundle arguments = new Bundle();

                arguments.putParcelable(FullScreenPlayerActivity.PLAYER_SESSION, session);
                fragment.setArguments(arguments);
                fragment.show(fragmentManager, "PLAYER");
            } else {
                Intent intent = new Intent(getActivity(), FullScreenPlayerActivity.class);
                intent.putExtra(FullScreenPlayerActivity.PLAYER_SESSION, session);

                startActivity(intent);
            }
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

    public void fetchTracks(String artistId) {
        new FetchTracksTask().execute(artistId);
    }


    private class FetchTracksTask extends AsyncTask<String, Void, List<Track>> {

        private RetrofitError apiException = null;
        private SpotifyApi api;

        @Override
        protected List<Track> doInBackground(String... params) {
            try {
                api = new SpotifyApi();
                SpotifyService spotify = api.getService();

                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(getActivity());

                Map<String, Object> map = new HashMap<String, Object>() {
                };
                map.put("country", prefs.getString(getString(R.string.pref_country_code_key),
                        getString(R.string.pref_country_code)));

                return spotify.getArtistTopTrack(params[0], map).tracks;
            } catch (RetrofitError e) {
                this.apiException = e;
            }

            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<Track> result) {
            if (apiException != null) {
                SpotifyError spotifyError = SpotifyError.fromRetrofitError(apiException);
                String error = spotifyError.getErrorDetails() != null ?
                        spotifyError.getErrorDetails().message :
                        null;

                if (error == null) error = getString(R.string.tracks_api_error);

                Toast.makeText(getActivity(),
                        error,
                        Toast.LENGTH_SHORT).show();
                return;
            } else {
                if (result.size() == 0) {
                    Toast.makeText(
                            getActivity().getApplicationContext(),
                            getString(R.string.empty_tracks),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (mTracksAdapter != null) {
                mTracksAdapter.clear();
                for (Track track : result) {
                    mTracksAdapter.add(new ParcableTrack(
                            track.name,
                            track.artists.get(0).name,
                            track.album.name,
                            track.album.images.size() > 0 ? track.album.images.get(0).url : "",
                            track.preview_url
                    ));
                }
            }
        }
    }
}
