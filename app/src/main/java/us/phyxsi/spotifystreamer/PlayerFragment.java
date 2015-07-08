package us.phyxsi.spotifystreamer;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class PlayerFragment extends DialogFragment {

    static final String TRACK = "TRACK";

    private ParcableTrack mTrack;

    public PlayerFragment() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = view = inflater.inflate(R.layout.fragment_player, container, false);

        ImageView albumImage = (ImageView) view.findViewById(R.id.player_album_image);
        final TextView trackTitle = (TextView) view.findViewById(R.id.player_track_title);
        final TextView artistName = (TextView) view.findViewById(R.id.player_artist_name);
        final LinearLayout playerControls =
                (LinearLayout) view.findViewById(R.id.player_control_layout);
        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.player_seekbar);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mTrack = arguments.getParcelable(PlayerFragment.TRACK);
        }

        // Set the album image
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int imageDimension = Math.round(86 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

        if (!TextUtils.isEmpty(mTrack.imageUrl)) {
            Picasso.with(getActivity())
                    .load(mTrack.imageUrl)
                    .into(albumImage);
        }

        // Artist and track details
        trackTitle.setText(mTrack.name);
        artistName.setText(mTrack.artist);

        // Change widget colors based on album art
        Picasso.with(getActivity()).load(mTrack.imageUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
                        Palette.Swatch mutedSwatch = palette.getMutedSwatch();
                        Palette.Swatch darkMutedSwatch = palette.getDarkMutedSwatch();

                        trackTitle.setBackgroundColor(getSwatchColor(mutedSwatch));
                        trackTitle.setTextColor(mutedSwatch != null ?
                                mutedSwatch.getBodyTextColor() : 0x000000);
                        artistName.setBackgroundColor(getSwatchColor(mutedSwatch));
                        artistName.setTextColor(mutedSwatch != null ?
                                mutedSwatch.getTitleTextColor() : 0x000000);

                        playerControls.setBackgroundColor(getSwatchColor(darkMutedSwatch));

                        seekBar.setBackgroundColor(getSwatchColor(vibrantSwatch));
                    }
                });
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }

            private int getSwatchColor(Palette.Swatch swatch) {
                return swatch != null ? swatch.getRgb() : 0x000000;
            }
        });

        return view;
    }

}