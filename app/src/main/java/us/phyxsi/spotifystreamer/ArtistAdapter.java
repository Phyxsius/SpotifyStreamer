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

import kaaes.spotify.webapi.android.models.Artist;

public class ArtistAdapter extends ArrayAdapter<Artist> {
    private Context context;
    private LayoutInflater mLayoutInflater;

    public ArtistAdapter(Context context, int resource, List<Artist> objects) {
        super(context, resource, objects);

        this.context = context;

        this.mLayoutInflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = mLayoutInflater.inflate(R.layout.list_item_artist, parent, false);
        }

        TextView artistName = (TextView) view.findViewById(R.id.list_item_artist_name);
        ImageView artistImage = (ImageView) view.findViewById(R.id.list_item_icon);

        Artist artist = getItem(position);
        artistName.setText(artist.name);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int imageDimension = Math.round(86 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

        if (artist.images.size() > 0) {
            Picasso.with(this.context)
                    .load(artist.images.get(0).url)
                    .resize(imageDimension, imageDimension)
                    .into(artistImage);
        }

        return view;
    }
}
