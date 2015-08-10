package us.phyxsi.spotifystreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import us.phyxsi.spotifystreamer.object.ParcableTrack;
import us.phyxsi.spotifystreamer.ui.FullScreenPlayerActivity;
import us.phyxsi.spotifystreamer.ui.MainActivity;

/**
 * Based on https://github.com/googlesamples/android-UniversalMusicPlayer/blob/master/mobile/src/main/java/com/example/android/uamp/MediaNotificationManager.java
 */
public class MediaNotificationManager extends BroadcastReceiver {
    private static final String TAG = MediaNotificationManager.class.getSimpleName();

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;

    public static final String ACTION_PAUSE = "us.phyxsi.spotifystreamer.pause";
    public static final String ACTION_PLAY = "us.phyxsi.spotifystreamer.play";
    public static final String ACTION_PREV = "us.phyxsi.spotifystreamer.prev";
    public static final String ACTION_NEXT = "us.phyxsi.spotifystreamer.next";

    private final MusicService mMusicService;

    private ParcableTrack mTrack;

    private final NotificationManager mNotificationManager;

    private final PendingIntent mPauseIntent;
    private final PendingIntent mPlayIntent;
    private final PendingIntent mPreviousIntent;
    private final PendingIntent mNextIntent;

    private final int mNotificationColor;

    private boolean mStarted = false;

    public MediaNotificationManager(MusicService service) {
        mMusicService = service;

        if (mMusicService.getSession() != null)
            mTrack = mMusicService.getSession().getCurrentTrack();

        mNotificationColor = mMusicService.getResources().getColor(R.color.primary);

        mNotificationManager = (NotificationManager)
                mMusicService.getSystemService(Context.NOTIFICATION_SERVICE);

        String pkg = mMusicService.getPackageName();
        mPauseIntent = PendingIntent.getBroadcast(mMusicService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent = PendingIntent.getBroadcast(mMusicService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPreviousIntent = PendingIntent.getBroadcast(mMusicService, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mNextIntent = PendingIntent.getBroadcast(mMusicService, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancelAll();
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification() {
        if (!mStarted) {
            mTrack = mMusicService.getSession().getCurrentTrack();

            // The notification must be updated after setting started to true
            Notification notification = createNotification();
            if (notification != null) {
                mMusicService.registerCallback(mCb);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PREV);
                mMusicService.registerReceiver(this, filter);

                mStarted = true;
            }
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        if (mStarted) {
            mStarted = false;
            mMusicService.unregisterCallback(mCb);
            try {
                mNotificationManager.cancel(NOTIFICATION_ID);
                mMusicService.unregisterReceiver(this);
            } catch(IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "Received intent with action " + action);
        switch (action) {
            case ACTION_PAUSE:
                mMusicService.pause();
                break;
            case ACTION_PLAY:
                mMusicService.togglePlay();
                break;
            case ACTION_NEXT:
                mMusicService.playNext();
                break;
            case ACTION_PREV:
                mMusicService.playPrevious();
                break;
            default:
                Log.w(TAG, "Unknown intent ignored. Action=" + action);
        }
    }

    private PendingIntent createContentIntent() {
        Intent openUI = new Intent(mMusicService, FullScreenPlayerActivity.class);

        if (MainActivity.mIsLargeLayout)
            openUI = new Intent(mMusicService, MainActivity.class);

        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openUI.putExtra(FullScreenPlayerActivity.PLAYER_SESSION, mMusicService.getSession());

        return PendingIntent.getActivity(mMusicService, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private final MusicService.MusicServiceCallback mCb = new MusicService.MusicServiceCallback() {
        @Override
        public void onProgressChange(int progress) {

        }

        @Override
        public void onTrackChanged(ParcableTrack track) {
            mTrack = track;
            updateState();
        }

        @Override
        public void onPlaybackStopped() {
            stopNotification();
        }
    };

    public void updateState() {
        Notification notification = createNotification();
        if (notification != null) {
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification createNotification(){
        if (mMusicService == null || mMusicService.getSession() == null) return null;
        mTrack = mMusicService.getSession().getCurrentTrack();

        Notification.Builder notificationBuilder =
                new Notification.Builder(mMusicService);
        int playPauseButtonPosition = 0;

        // If skip to previous action is enabled
        if(mMusicService.canPlayPrev()){
            notificationBuilder.addAction(R.drawable.ic_skip_previous_white_24dp,
                    mMusicService.getString(R.string.skip_prev), mPreviousIntent);

            // If there is a "skip to previous" button, the play/pause button will
            // be the second one. We need to keep track of it, because the MediaStyle notification
            // requires to specify the index of the buttons (actions) that should be visible
            // when in compact view.
            playPauseButtonPosition = 1;
        }

        addPlayPauseAction(notificationBuilder);

        // If skip to next action is enabled
        if(mMusicService.canPlayNext()){
            notificationBuilder.addAction(R.drawable.ic_skip_next_white_24dp,
                    mMusicService.getString(R.string.skip_next), mNextIntent);
        }


        String imageUrl = mTrack.imageUrl;
        Bitmap image = BitmapFactory.decodeResource(mMusicService.getResources(),
                R.drawable.ic_default_art);

        notificationBuilder
                .setStyle(new Notification.MediaStyle()
                        .setShowActionsInCompactView(playPauseButtonPosition)  // show only play/pause in compact view
                )
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setContentIntent(createContentIntent())
                .setContentTitle(mTrack.name)
                .setContentText(mTrack.artist)
                .setLargeIcon(image);

        setNotificationPlaybackState(notificationBuilder);
        if (imageUrl != null) {
            fetchBitmapFromURLAsync(imageUrl, notificationBuilder);
        }

        return notificationBuilder.build();
    }

    private void addPlayPauseAction(Notification.Builder builder) {
        String label;
        int icon;
        PendingIntent intent;
        if (mMusicService.getPlayer().isPlaying()) {
            label = mMusicService.getString(R.string.label_pause);
            icon = R.drawable.ic_pause_white_24dp;
            intent = mPauseIntent;
        } else {
            label = mMusicService.getString(R.string.label_play);
            icon = R.drawable.ic_play_arrow_white_24dp;
            intent = mPlayIntent;
        }
        builder.addAction(new Notification.Action(icon, label, intent));
    }

    private void setNotificationPlaybackState(Notification.Builder builder) {
        if (!mStarted) {
            mMusicService.stopForeground(true);
            return;
        }

        if (mMusicService.getPlayer().isPlaying() && mMusicService.getCurrentPosition() >= 0) {
            builder
                    .setWhen(System.currentTimeMillis() - mMusicService.getCurrentPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
        } else {
            builder
                    .setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        // Make sure that the notification can be dismissed by the user when we are not playing:
        builder.setOngoing(mMusicService.getPlayer().isPlaying());
    }

    private void fetchBitmapFromURLAsync(final String bitmapUrl,
                                         final Notification.Builder builder) {
        Picasso.with(mMusicService).load(bitmapUrl).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from){
                builder.setLargeIcon(bitmap);
                mNotificationManager.notify(NOTIFICATION_ID, builder.build());
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable){

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable){

            }
        });
    }

}
