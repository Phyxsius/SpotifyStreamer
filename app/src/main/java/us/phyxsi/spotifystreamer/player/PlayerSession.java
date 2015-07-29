package us.phyxsi.spotifystreamer.player;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import us.phyxsi.spotifystreamer.object.ParcableArtist;
import us.phyxsi.spotifystreamer.object.ParcableTrack;

/**
 * A class that implements media playback using {@link android.media.MediaPlayer}
 */
public class PlayerSession implements Parcelable {

    private static final String TAG = PlayerSession.class.getSimpleName();

    private ParcableArtist artist;
    private ArrayList<ParcableTrack> playlist;
    private int startingPosition;
    private int currentPosition;
    private String sessionToken;

    // Constructor
    public PlayerSession(ParcableArtist artist, ArrayList<ParcableTrack> tracks, int startingPosition) {
        this.artist = artist;
        this.playlist = tracks;
        this.startingPosition = this.currentPosition = startingPosition;
        this.sessionToken = (artist == null ? "null" : artist.id) + "_" + startingPosition;
    }

    protected PlayerSession(Parcel in) {
        this.artist = in.readParcelable(ParcableArtist.class.getClassLoader());
        this.playlist = in.createTypedArrayList(ParcableTrack.CREATOR);
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

    public ArrayList<ParcableTrack> getPlaylist() {
        return playlist;
    }

    public ParcableTrack getTrackAt(int position) {
        if (this.playlist == null || this.playlist.isEmpty()) return null;

        if (position < 0 || position >= this.playlist.size()) return null;

        return this.playlist.get(position);
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public int getPlaylistSize() {
        return (getPlaylist() != null) ? getPlaylist().size() : 0;
    }

    public ParcableTrack getCurrentTrack() {
        return this.playlist.get(this.currentPosition);
    }

    private int getPreviousPosition() {
        int prev = this.currentPosition - 1;

        return (prev < 0) ? this.playlist.size() - 1 : prev;
    }

    private int getNextPosition() {
        int next = this.currentPosition + 1;

        return (next >= this.playlist.size()) ? 0 : next;
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
        dest.writeTypedList(this.playlist);
        dest.writeInt(this.startingPosition);
        dest.writeInt(this.currentPosition);
        dest.writeString(this.sessionToken);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PlayerSession) {
            return sessionToken.equals(((PlayerSession) o). getSessionToken());
        }

        return super.equals(o);
    }

    public static final Creator<PlayerSession> CREATOR = new Creator<PlayerSession>() {
        public PlayerSession createFromParcel(Parcel source) {
            return new PlayerSession(source);
        }

        public PlayerSession[] newArray(int size) {
            return new PlayerSession[size];
        }
    };

}
