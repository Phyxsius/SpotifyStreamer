package us.phyxsi.spotifystreamer;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import kaaes.spotify.webapi.android.models.Track;

public class TrackAdapter extends ArrayAdapter<Track> {
    private Context context;
    private LayoutInflater mLayoutInflater;

    public TrackAdapter(Context context, int resource, List<Track> objects) {
        super(context, resource, objects);

        this.context = context;

        this.mLayoutInflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.list_item_track, parent, false);
        }

        TextView trackName = (TextView) view.findViewById(R.id.list_item_track);
        TextView albumName = (TextView) view.findViewById(R.id.list_item_album);
        ImageView albumImage = (ImageView) view.findViewById(R.id.list_item_icon);

        Track track = getItem(position);
        trackName.setText(track.name);
        albumName.setText(track.album.name);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int imageDimension = Math.round(86 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

        if (track.album.images.size() > 0) {
            Picasso.with(this.context)
                    .load(track.album.images.get(0).url)
                    .resize(imageDimension, imageDimension)
                    .into(albumImage);
        }

        return view;
    }
}
