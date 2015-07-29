package us.phyxsi.spotifystreamer.player;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import us.phyxsi.spotifystreamer.MusicService;
import us.phyxsi.spotifystreamer.R;

public class PlayerFragment extends DialogFragment {

    private static final String TAG = PlayerFragment.class.getSimpleName();

    private PlayerSession mSession;
    private MusicService mMusicService;
    private boolean serviceBound = false;
    private Intent mPlayIntent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments.containsKey(PlayerActivity.PLAYER_SESSION)) {
            this.mSession = arguments.getParcelable(PlayerActivity.PLAYER_SESSION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (mPlayIntent == null) {
            mPlayIntent = new Intent(activity, MusicService.class);
            activity.bindService(mPlayIntent, streamingConnection, Context.BIND_AUTO_CREATE);
            activity.startService(mPlayIntent);
        }
    }

    private ServiceConnection streamingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;

            mMusicService = binder.getService();

            PlayerSession serviceSession = mMusicService.getSession();
            if (serviceSession != null && serviceSession.equals(mSession)) {
                mSession = serviceSession;
            } else {
                mMusicService.setSession(mSession, true);
            }

            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
}