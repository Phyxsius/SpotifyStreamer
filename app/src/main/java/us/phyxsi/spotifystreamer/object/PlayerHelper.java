package us.phyxsi.spotifystreamer.object;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class PlayerHelper implements Parcelable {

    private ParcableArtist artist;
    private ArrayList<ParcableTrack> tracks;
    private int startingPosition;
    private int currentPosition;
    private String sessionToken;

    // Constructor
    public PlayerHelper(ParcableArtist artist, ArrayList<ParcableTrack> tracks, int startingPosition) {
        this.artist = artist;
        this.tracks = tracks;
        this.startingPosition = this.currentPosition = startingPosition;
        this.sessionToken = (artist == null ? "null" : artist.id) + "_" + startingPosition;
    }

    protected PlayerHelper(Parcel in) {
        this.artist = in.readParcelable(ParcableArtist.class.getClassLoader());
        this.tracks = in.createTypedArrayList(ParcableTrack.CREATOR);
        this.startingPosition = in.readInt();
        this.currentPosition = in.readInt();
        this.sessionToken = in.readString();
    }

    // Getters
    public int getStartingPosition() {
        return startingPosition;
    }

    public ParcableArtist getArtist() {
        return artist;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public ArrayList<ParcableTrack> getTracks() {
        return tracks;
    }

    public ParcableTrack getTrackAt(int position) {
        if (this.tracks == null || this.tracks.isEmpty()) return null;

        if (position < 0 || position >= this.tracks.size()) return null;

        return this.tracks.get(position);
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public int getPlaylistSize() {
        return (getTracks() != null) ? getTracks().size() : 0;
    }

    public ParcableTrack getCurrentTrack() {
        return this.tracks.get(this.currentPosition);
    }

    private int getPreviousPosition() {
        int prev = this.currentPosition - 1;

        return (prev < 0) ? this.tracks.size() - 1 : prev;
    }

    private int getNextPosition() {
        int next = this.currentPosition + 1;

        return (next >= this.tracks.size()) ? 0 : next;
    }

    public ParcableTrack getPreviousTrack() {
        this.currentPosition = getPreviousPosition();
        return getTrackAt(this.currentPosition);
    }

    public ParcableTrack getNextTrack() {
        this.currentPosition = getNextPosition();
        return getTrackAt(this.currentPosition);
    }

    //Setters
    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    // Parcable methods
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.artist, 0);
        dest.writeTypedList(tracks);
        dest.writeInt(this.startingPosition);
        dest.writeInt(this.currentPosition);
        dest.writeString(this.sessionToken);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PlayerHelper) {
            return sessionToken.equals(((PlayerHelper) o). getSessionToken());
        }

        return super.equals(o);
    }

    public static final Creator<PlayerHelper> CREATOR = new Creator<PlayerHelper>() {
        public PlayerHelper createFromParcel(Parcel source) {
            return new PlayerHelper(source);
        }

        public PlayerHelper[] newArray(int size) {
            return new PlayerHelper[size];
        }
    };
}
