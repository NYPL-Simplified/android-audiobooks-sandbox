 /*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.media.MediaRouter;

import com.example.android.uamp.model.MusicProvider;
import com.example.android.uamp.playback.CastPlayback;
import com.example.android.uamp.playback.LocalPlayback;
import com.example.android.uamp.playback.Playback;
import com.example.android.uamp.playback.PlaybackManager;
import com.example.android.uamp.playback.QueueManager;
import com.example.android.uamp.ui.NowPlayingActivity;
import com.example.android.uamp.utils.CarHelper;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.TvHelper;
import com.example.android.uamp.utils.WearHelper;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_EMPTY_ROOT;
import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_ROOT;

/**
 * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
 * client, through the onGetRoot and onLoadChildren methods.
 *
 * It also creates a MediaSession and exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and send control commands to the MediaSession remotely. This is useful for
 * user interfaces that need to interact with your media session, like Android Auto. You can
 * (should) also use the same service from your app's UI, which gives a seamless playback
 * experience to the user.
 *
 * To implement a MediaBrowserService, you need to:
 *
 * <ul>
 * <li> Extend {@link android.service.media.MediaBrowserService}, implementing the media browsing
 *      related methods {@link android.service.media.MediaBrowserService#onGetRoot} and
 *      {@link android.service.media.MediaBrowserService#onLoadChildren};
 *
 * <li> In onCreate, start a new {@link android.media.session.MediaSession} and notify its parent
 *      with the session's token {@link android.service.media.MediaBrowserService#setSessionToken};
 *
 * <li> Set a callback on the
 *      {@link android.media.session.MediaSession#setCallback(android.media.session.MediaSession.Callback)}.
 *      The callback will receive all the user's actions, like play, pause, etc;
 *
 * <li> Handle all the actual music playing using any method your app prefers (for example,
 *      {@link android.media.MediaPlayer})
 *
 * <li> Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 *      {@link android.media.session.MediaSession#setPlaybackState(android.media.session.PlaybackState)}
 *      {@link android.media.session.MediaSession#setMetadata(android.media.MediaMetadata)} and
 *      {@link android.media.session.MediaSession#setQueue(java.util.List)})
 *
 * <li> Declare and export the service in AndroidManifest with an intent receiver for the action
 *      android.media.browse.MediaBrowserService
 * </ul>
 *
 * To make your app compatible with Android Auto, you also need to:
 * <ul>
 * <li> Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
 *      with a &lt;automotiveApp&gt; root element. For a media app, this must include
 *      an &lt;uses name="media"/&gt; element as a child.
 *      For example, in AndroidManifest.xml:
 *          &lt;meta-data android:name="com.google.android.gms.car.application"
 *              android:resource="@xml/automotive_app_desc"/&gt;
 *      And in res/values/automotive_app_desc.xml:
 *          &lt;automotiveApp&gt;
 *              &lt;uses name="media"/&gt;
 *          &lt;/automotiveApp&gt;
 * </ul>
 * @see <a href="README.md">README.md</a> for more details.
 *
 * TODO:  rename MusicService in manifest and all code to AudioBrowserService.
 */
public class MusicService extends MediaBrowserServiceCompat implements
        PlaybackManager.PlaybackServiceCallback {

    // TODO:  make sure setPlaybackState, setMetadata, and setQueue are all called on each track change.

    private static final String TAG = LogHelper.makeLogTag(MusicService.class);

    // Extra on MediaSession that contains the Cast device name currently connected to
    public static final String EXTRA_CONNECTED_CAST = "com.example.android.uamp.CAST_NAME";

    // The action of the incoming Intent indicating that it contains a command
    // to be executed (see {@link #onStartCommand})
    public static final String ACTION_CMD = "com.example.android.uamp.ACTION_CMD";
    // The key in the extras of the incoming Intent indicating the command that
    // should be executed (see {@link #onStartCommand})
    public static final String CMD_NAME = "CMD_NAME";
    // A value of a CMD_NAME key in the extras of the incoming Intent that
    // indicates that the music playback should be paused (see {@link #onStartCommand})
    public static final String CMD_PAUSE = "CMD_PAUSE";

    // A value of a CMD_NAME key that indicates that the music playback should switch
    // to local playback from cast playback.
    public static final String CMD_STOP_CASTING = "CMD_STOP_CASTING";

    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;

    private MusicProvider mMusicProvider;
    private PlaybackManager mPlaybackManager;

    private MediaSessionCompat mSession;
    private MediaNotificationManager mMediaNotificationManager;
    private Bundle mSessionExtras;
    private final DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
    private MediaRouter mMediaRouter;
    private PackageValidator mPackageValidator;
    private SessionManager mCastSessionManager;
    private SessionManagerListener<CastSession> mCastSessionManagerListener;

    private boolean mIsConnectedToCar;
    private BroadcastReceiver mCarConnectionReceiver;


    /**
     * TODO doc
     * A newly-created media session has no capabilities. When the service receives the onCreate() lifecycle callback method,
     * you must initialize the session by performing these steps:
     *
     Create and initialize the media session
     Set the media session callback (Set flags so that the media session can receive callbacks from media controllers and media buttons.)
     Set the media session token

     Create and initialize an instance of PlaybackStateCompat and assign it to the session. The playback state changes throughout
     the session, so we recommend caching the PlaybackStateCompat.Builder for reuse.
     Create an instance of MediaSessionCompat.Callback and assign it to the session (more on callbacks below).
     *
     * In order for media buttons to work when your app is newly initialized (or stopped), its PlaybackState must contain a play action matching the intent that the media button sends. This is why ACTION_PLAY is assigned to the session state during initialization. For more information, see Responding to Media Buttons.
     *
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        LogHelper.d(TAG, "onCreate");

        mMusicProvider = new MusicProvider();

        // To make the app more responsive, fetch and cache catalog information now.
        // This can help improve the response time in the method
        // {@link #onLoadChildren(String, Result<List<MediaItem>>) onLoadChildren()}.

        // mMusicProvider.retrieveMediaAsync(null /* Callback */);  // darya

        mPackageValidator = new PackageValidator(this);  // darya

        QueueManager queueManager = new QueueManager(mMusicProvider, getResources(),
                new QueueManager.MetadataUpdateListener() {
                    @Override
                    public void onMetadataChanged(MediaMetadataCompat metadata) {
                        mSession.setMetadata(metadata);
                    }

                    @Override
                    public void onMetadataRetrieveError() {
                        mPlaybackManager.updatePlaybackState(
                                getString(R.string.error_no_metadata));
                    }

                    @Override
                    public void onCurrentQueueIndexUpdated(int queueIndex) {
                        // TODO: important
                        mPlaybackManager.handlePlayRequest();
                    }

                    @Override
                    public void onQueueUpdated(String title,
                                               List<MediaSessionCompat.QueueItem> newQueue) {
                        mSession.setQueue(newQueue);
                        mSession.setQueueTitle(title);
                    }
                });

        LocalPlayback playback = new LocalPlayback(this, mMusicProvider);
        mPlaybackManager = new PlaybackManager(this, getResources(), mMusicProvider, queueManager,
                playback);

        // Start a new MediaSession
        mSession = new MediaSessionCompat(this, "MusicService");

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mSession.getSessionToken());

        // getMediaSessionCallback() has methods that handle callbacks from a media controller
        // The callback will receive all the user's actions, like play, pause, etc..
        mSession.setCallback(mPlaybackManager.getMediaSessionCallback());

        // Enable callbacks from MediaButtons and TransportControls
        // NOTE: KEYCODE_MEDIA_PLAY, KEYCODE_MEDIA_PAUSE, etc. should be listened for inside
        // MediaSessionCompat.Callback.onMediaButtonEvent() (in sdk code).
        // (see https://developer.android.com/guide/topics/media-apps/mediabuttons.html for explanation.)
        // TODO: if I end up writing custom "double-click reverses 20 seconds" functionality, then will be overriding onMediaButtonEvent().
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        Context context = getApplicationContext();
        Intent intent = new Intent(context, NowPlayingActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 99 /*request code*/,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setSessionActivity(pi);

        mSessionExtras = new Bundle();
        CarHelper.setSlotReservationFlags(mSessionExtras, true, true, true);
        WearHelper.setSlotReservationFlags(mSessionExtras, true, true);
        WearHelper.setUseBackgroundFromTheme(mSessionExtras, true);
        mSession.setExtras(mSessionExtras);

        mPlaybackManager.updatePlaybackState(null);

        try {
            mMediaNotificationManager = new MediaNotificationManager(this);
        } catch (RemoteException e) {
            throw new IllegalStateException("Could not create a MediaNotificationManager", e);
        }

        if (!TvHelper.isTvUiMode(this)) {
            mCastSessionManager = CastContext.getSharedInstance(this).getSessionManager();
            mCastSessionManagerListener = new CastSessionManagerListener();
            mCastSessionManager.addSessionManagerListener(mCastSessionManagerListener,
                    CastSession.class);
        }

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());

        //darya registerCarConnectionReceiver();
    }


    /**
     * TODO: doc
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        if (startIntent != null) {
            String action = startIntent.getAction();
            String command = startIntent.getStringExtra(CMD_NAME);
            if (ACTION_CMD.equals(action)) {
                if (CMD_PAUSE.equals(command)) {
                    mPlaybackManager.handlePauseRequest();
                } else if (CMD_STOP_CASTING.equals(command)) {
                    CastContext.getSharedInstance(this).getSessionManager().endCurrentSession(true);
                }
            } else {
                // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
                MediaButtonReceiver.handleIntent(mSession, startIntent);
            }
        }
        // Reset the delay handler to enqueue a message to stop the service if
        // nothing is playing.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        return START_STICKY;
    }


    /**
     * TODO: doc
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        LogHelper.d(TAG, "onDestroy");
        // darya unregisterCarConnectionReceiver();

        // Service is being killed, so make sure we release our resources
        mPlaybackManager.handleStopRequest(null);
        mMediaNotificationManager.stopNotification();

        if (mCastSessionManager != null) {
            mCastSessionManager.removeSessionManagerListener(mCastSessionManagerListener,
                    CastSession.class);
        }

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mSession.release();
    }


    /**
     * controls access to the service
     * returns the root node of the content hierarchy. If the method returns null, the connection is refused.
     *
     * To allow all clients to connect to your service and browse its media content, onGetRoot() should return a non-null BrowserRoot with a root ID.
     * To allow connections to your service without browsing, return a non-null BrowserRoot with a null root ID.
     */
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                 Bundle rootHints) {
        LogHelper.d(TAG, "OnGetRoot: clientPackageName=" + clientPackageName,
                "; clientUid=" + clientUid + " ; rootHints=", rootHints);
        // To ensure you are not allowing any arbitrary app to browse your app's contents, you
        // need to check the origin:
        // This is optional, and allows control over the level of access for the specified package name.
        if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return an empty browser root.
            // If you return null, then the media browser will not be able to connect and
            // no further calls will be made to other media browsing methods.
            // Returning an empty BrowserRoot string lets clients connect, but their
            // onLoadChildren requests will return nothing. This disables the ability to browse for content.
            LogHelper.i(TAG, "OnGetRoot: Browsing NOT ALLOWED for unknown caller. "
                    + "Returning empty browser root so all apps can use MediaController."
                    + clientPackageName);
            return new MediaBrowserServiceCompat.BrowserRoot(MEDIA_ID_EMPTY_ROOT, null);
        }
        //noinspection StatementWithEmptyBody
        if (CarHelper.isValidCarPackage(clientPackageName)) {
            // Optional: if your app needs to adapt the music library to show a different subset
            // when connected to the car, this is where you should handle it.
            // If you want to adapt other runtime behaviors, like tweak ads or change some behavior
            // that should be different on cars, you should instead use the boolean flag
            // set by the BroadcastReceiver mCarConnectionReceiver (mIsConnectedToCar).

            // Note: You should consider providing different content hierarchies depending on what type of client is making the query. In particular, Android Auto limits how users interact with audio apps. For more information, see Playing Audio for Auto. You can look at the clientPackageName at connection time to determine the client type, and return a different BrowserRoot depending on the client (or rootHints if any).

        }
        //noinspection StatementWithEmptyBody
        if (WearHelper.isValidWearCompanionPackage(clientPackageName)) {
            // Optional: if your app needs to adapt the music library for when browsing from a
            // Wear device, you should return a different MEDIA ROOT here, and then,
            // on onLoadChildren, handle it accordingly.
        }

        // Returning a non-null, non-empty root ID, lets clients use onLoadChildren() to retrieve the content hierarchy.
        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }


    /**
     * TODO: doc
     *
     * After the client connects, it can traverse the content hierarchy by making repeated calls to MediaBrowserCompat.subscribe() to build a local representation of the UI. The subscribe() method sends the callback onLoadChildren() to the service, which returns a list of MediaBrowser.MediaItem objects.

     Each MediaItem has a unique ID string, which is an opaque token. When a client wants to open a submenu or play an item, it passes the ID. Your service is responsible for associating the ID with the appropriate menu node or content item.

     * provides the ability for a client to build and display a menu of the MediaBrowserService's content hierarchy
     * Note: MediaItem objects delivered by the MediaBrowserService should not contain icon bitmaps.
     * Use a Uri instead by calling setIconUri() when you build the MediaDescription for each item.
     *
     * @param parentMediaId
     * @param result
     */
    @Override
    public void onLoadChildren(@NonNull final String parentMediaId,
                               @NonNull final Result<List<MediaItem>> result) {
        LogHelper.d(TAG, "OnLoadChildren: parentMediaId=", parentMediaId);

        if (MEDIA_ID_EMPTY_ROOT.equals(parentMediaId)) {
            //  Browsing not allowed
            // online doc returns a null, not empty list
            result.sendResult(new ArrayList<MediaItem>());
            return;
        }

        if (mMusicProvider.isInitialized()) {
            // if music library is ready, return immediately
            result.sendResult(mMusicProvider.getChildren(parentMediaId, getResources()));
        } else {
            // otherwise, only return results when the music library is retrieved
            result.detach();
            mMusicProvider.retrieveMediaAsync(new MusicProvider.Callback() {
                @Override
                public void onMusicCatalogReady(boolean success) {
                    result.sendResult(mMusicProvider.getChildren(parentMediaId, getResources()));
                }
            });
        }
    }


    /**
     * Callback method called from PlaybackManager whenever the music is about to play.
     *
     * The behavior of an Android service depends on whether it is started or bound to one or more clients.
     * After a service is created, it can be started, bound, or both.  In all of these states, it is fully functional
     * and can perform the work it's designed to do.  The difference is how long the service will exist.
     * A bound service is not destroyed until all its bound clients unbind.
     * A started service can be explicitly stopped and destroyed (assuming it is no longer bound to any clients).

     * When a MediaBrowser running in another activity connects to a MediaBrowserService, it binds the activity to the service,
     * making the service bound (but not started). This default behavior is built into the MediaBrowserServiceCompat class.

     A service that is only bound (and not started) is destroyed when all of its clients unbind.
     If your UI activity disconnects at this point, the service is destroyed. This isn't a problem if you haven't played any audio yet.
     However, when playback starts, the user probably expects to continue listening even after switching apps.
     You don't want to destroy the player when you unbind the UI to work with another app.

     For this reason, you need to be sure that the service is started when it begins to play by calling startService().
     A started service must be explicitly stopped, whether or not it's bound. This ensures that your player continues
     to perform even if the controlling UI activity unbinds.

     To stop a started service, call Context.stopService() or stopSelf(). The system stops and destroys the service as soon as possible.
     However, if one or more clients are still bound to the service, the call to stop the service is delayed until all its clients unbind.
     */
    @Override
    public void onPlaybackStart() {
        mSession.setActive(true);

        mDelayedStopHandler.removeCallbacksAndMessages(null);

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        startService(new Intent(getApplicationContext(), MusicService.class));
    }


    /**
     * Callback method called from PlaybackManager whenever the music stops playing.
     * TODO:  make sure either Context.stopService() or stopSelf() is called somewhere here in the depths,
     * although stopForeground might be enough -- GC can stop the service if I say it's no longer foreground-needed.
     * TODO:  also find where we're setting this service to the foreground on play start.
     */
    @Override
    public void onPlaybackStop() {
        mSession.setActive(false);
        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        stopForeground(true);
    }


    /**
     * TODO: doc
     */
    @Override
    public void onNotificationRequired() {
        mMediaNotificationManager.startNotification();
    }


    /**
     * TODO: doc
     */
    @Override
    public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
        mSession.setPlaybackState(newState);
    }


    /**
     * TODO: doc
     * commenting out for now: darya
    private void registerCarConnectionReceiver() {
        IntentFilter filter = new IntentFilter(CarHelper.ACTION_MEDIA_STATUS);
        mCarConnectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String connectionEvent = intent.getStringExtra(CarHelper.MEDIA_CONNECTION_STATUS);
                mIsConnectedToCar = CarHelper.MEDIA_CONNECTED.equals(connectionEvent);
                LogHelper.i(TAG, "Connection event to Android Auto: ", connectionEvent,
                        " isConnectedToCar=", mIsConnectedToCar);
            }
        };
        registerReceiver(mCarConnectionReceiver, filter);
    }
    */


    /**
     * TODO: doc
     * commenting out for now: darya
    private void unregisterCarConnectionReceiver() {
        unregisterReceiver(mCarConnectionReceiver);
    }
    */


    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {
        private final WeakReference<MusicService> mWeakReference;

        private DelayedStopHandler(MusicService service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MusicService service = mWeakReference.get();
            if (service != null && service.mPlaybackManager.getPlayback() != null) {
                if (service.mPlaybackManager.getPlayback().isPlaying()) {
                    LogHelper.d(TAG, "Ignoring delayed stop since the media player is in use.");
                    return;
                }
                LogHelper.d(TAG, "Stopping service with delay handler.");
                service.stopSelf();
            }
        }
    }


    /**
     * Session Manager Listener responsible for switching the Playback instances
     * depending on whether it is connected to a remote player.
     */
    private class CastSessionManagerListener implements SessionManagerListener<CastSession> {

        @Override
        public void onSessionEnded(CastSession session, int error) {
            LogHelper.d(TAG, "onSessionEnded");
            mSessionExtras.remove(EXTRA_CONNECTED_CAST);
            mSession.setExtras(mSessionExtras);
            Playback playback = new LocalPlayback(MusicService.this, mMusicProvider);
            mMediaRouter.setMediaSessionCompat(null);
            mPlaybackManager.switchToPlayback(playback, false);
        }

        @Override
        public void onSessionResumed(CastSession session, boolean wasSuspended) {
        }

        @Override
        public void onSessionStarted(CastSession session, String sessionId) {
            // In case we are casting, send the device name as an extra on MediaSession metadata.
            mSessionExtras.putString(EXTRA_CONNECTED_CAST,
                    session.getCastDevice().getFriendlyName());
            mSession.setExtras(mSessionExtras);
            // Now we can switch to CastPlayback
            Playback playback = new CastPlayback(mMusicProvider, MusicService.this);
            mMediaRouter.setMediaSessionCompat(mSession);
            mPlaybackManager.switchToPlayback(playback, true);
        }

        @Override
        public void onSessionStarting(CastSession session) {
        }

        @Override
        public void onSessionStartFailed(CastSession session, int error) {
        }

        @Override
        public void onSessionEnding(CastSession session) {
            // This is our final chance to update the underlying stream position
            // In onSessionEnded(), the underlying CastPlayback#mRemoteMediaClient
            // is disconnected and hence we update our local value of stream position
            // to the latest position.
            mPlaybackManager.getPlayback().updateLastKnownStreamPosition();
        }

        @Override
        public void onSessionResuming(CastSession session, String sessionId) {
        }

        @Override
        public void onSessionResumeFailed(CastSession session, int error) {
        }

        @Override
        public void onSessionSuspended(CastSession session, int reason) {
        }
    }
}
