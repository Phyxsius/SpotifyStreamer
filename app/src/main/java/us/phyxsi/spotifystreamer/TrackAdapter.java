package us.phyxsi.spotifystreamer;

import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class TrackAdapter extends ArrayAdapter<ParcableTrack> {
    private Context context;
    private LayoutInflater mLayoutInflater;

    public TrackAdapter(Context context, int resource, List<ParcableTrack> objects) {
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

        ParcableTrack track = getItem(position);
        trackName.setText(track.name);
        albumName.setText(track.album);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int imageDimension = Math.round(86 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

        if (!TextUtils.isEmpty(track.imageUrl)) {
            Picasso.with(this.context)
                    .load(track.imageUrl)
                    .resize(imageDimension, imageDimension)
                    .into(albumImage);
        }

        return view;
    }
}
