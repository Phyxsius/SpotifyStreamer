package us.phyxsi.spotifystreamer.object;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcableArtist implements Parcelable {

    public String id;
    public String name;
    public String imageUrl;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
        dest.writeString(this.imageUrl);
    }

    public ParcableArtist(String id,
                      String name,
                      String imageUrl) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
    }

    protected ParcableArtist(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.imageUrl = in.readString();
    }

    public static final Creator<ParcableArtist> CREATOR = new Creator<ParcableArtist>() {
        public ParcableArtist createFromParcel(Parcel source) {
            return new ParcableArtist(source);
        }

        public ParcableArtist[] newArray(int size) {
            return new ParcableArtist[size];
        }
    };
}
