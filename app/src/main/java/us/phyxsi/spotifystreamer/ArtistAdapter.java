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

public class ArtistAdapter extends ArrayAdapter<ParcableArtist> {
    private Context context;
    private LayoutInflater mLayoutInflater;

    public ArtistAdapter(Context context, int resource, List<ParcableArtist> objects) {
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

        ParcableArtist artist = getItem(position);
        artistName.setText(artist.name);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int imageDimension = Math.round(86 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

        if (!TextUtils.isEmpty(artist.imageUrl)) {
            Picasso.with(this.context)
                    .load(artist.imageUrl)
                    .resize(imageDimension, imageDimension)
                    .into(artistImage);
        }

        return view;
    }
}
