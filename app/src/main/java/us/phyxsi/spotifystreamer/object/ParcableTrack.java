package us.phyxsi.spotifystreamer.object;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcableTrack implements Parcelable {
    public String name;
    public String artist;
    public String album;
    public String imageUrl;
    public String previewUrl;


    public ParcableTrack(String name,
                         String artist,
                         String album,
                         String imageUrl,
                         String previewUrl) {
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.imageUrl = imageUrl;
        this.previewUrl = previewUrl;
    }

    public int getDurationInMilli() { return 30000; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.artist);
        dest.writeString(this.album);
        dest.writeString(this.imageUrl);
        dest.writeString(this.previewUrl);
    }

    protected ParcableTrack(Parcel in) {
        this.name = in.readString();
        this.artist = in.readString();
        this.album = in.readString();
        this.imageUrl = in.readString();
        this.previewUrl = in.readString();
    }

    public static final Creator<ParcableTrack> CREATOR = new Creator<ParcableTrack>() {
        public ParcableTrack createFromParcel(Parcel source) {
            return new ParcableTrack(source);
        }

        public ParcableTrack[] newArray(int size) {
            return new ParcableTrack[size];
        }
    };
}
