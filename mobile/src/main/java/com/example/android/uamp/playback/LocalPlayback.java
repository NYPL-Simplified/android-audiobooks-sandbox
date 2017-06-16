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
package com.example.android.uamp.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.model.MusicProvider;
import com.example.android.uamp.model.MusicProviderSource;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;

import java.io.IOException;

import static android.media.MediaPlayer.OnCompletionListener;
import static android.media.MediaPlayer.OnErrorListener;
import static android.media.MediaPlayer.OnPreparedListener;
import static android.media.MediaPlayer.OnSeekCompleteListener;
import static android.support.v4.media.session.MediaSessionCompat.QueueItem;

/**
 * A class that implements local media playback using {@link android.media.MediaPlayer}
 */
public class LocalPlayback implements Playback, AudioManager.OnAudioFocusChangeListener,
        OnCompletionListener, OnErrorListener, OnPreparedListener, OnSeekCompleteListener {

    private static final String TAG = LogHelper.makeLogTag(LocalPlayback.class);

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED  = 2;

    private final Context mContext;
    //darya private final WifiManager.WifiLock mWifiLock;
    private int mState;
    private boolean mPlayOnFocusGain;
    private Callback mCallback;
    private final MusicProvider mMusicProvider;
    private volatile boolean mAudioNoisyReceiverRegistered;
    private volatile int mCurrentPosition;
    private volatile String mCurrentMediaId;

    // Type of audio focus we have:
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private final AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;

    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                LogHelper.d(TAG, "Headphones disconnected.");
                if (isPlaying()) {
                    Intent i = new Intent(context, MusicService.class);
                    i.setAction(MusicService.ACTION_CMD);
                    i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
                    mContext.startService(i);
                }
            }
        }
    };

    public LocalPlayback(Context context, MusicProvider musicProvider) {
        this.mContext = context;
        this.mMusicProvider = musicProvider;
        this.mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        //darya this.mWifiLock = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
        //darya        .createWifiLock(WifiManager.WIFI_MODE_FULL, "uAmp_lock");
        this.mState = PlaybackStateCompat.STATE_NONE;

        // TODO:  mMediaPlayer is not being set here.  where and why wait?
        // TODO:  What's the NoisyReceiver for?
    }

    @Override
    public void start() {
        // TODO: why no code?
    }

    @Override
    public void stop(boolean notifyListeners) {
        this.setState(PlaybackStateCompat.STATE_STOPPED);
        if (notifyListeners && mCallback != null) {
            mCallback.onPlaybackStatusChanged(this.getState());
        }
        mCurrentPosition = getCurrentStreamPosition();
        // Give up Audio focus
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        // Relax all resources
        relaxResources(true);
    }

    @Override
    public void setState(int state) {
        this.mState = state;
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public boolean isConnected() {
        // TODO:  really?
        return true;
    }

    @Override
    public boolean isPlaying() {
        return mPlayOnFocusGain || (mMediaPlayer != null && mMediaPlayer.isPlaying());
    }

    @Override
    public int getCurrentStreamPosition() {
        return mMediaPlayer != null ?
                mMediaPlayer.getCurrentPosition() : mCurrentPosition;
    }

    @Override
    public void updateLastKnownStreamPosition() {
        if (mMediaPlayer != null) {
            mCurrentPosition = mMediaPlayer.getCurrentPosition();
        }
    }


    /**
     * TODO:  play() but not start()?
     * TODO:  Is QueueItem related to MenuItem?
     *
     * @param item to play
     */
    @Override
    public void play(QueueItem item) {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();

        String mediaId = item.getDescription().getMediaId();
        boolean mediaHasChanged = !TextUtils.equals(mediaId, mCurrentMediaId);
        if (mediaHasChanged) {
            mCurrentPosition = 0;
            mCurrentMediaId = mediaId;
        }

        if (this.getState() == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged && mMediaPlayer != null) {
            configMediaPlayerState();
        } else {
            this.setState(PlaybackStateCompat.STATE_STOPPED);  // TODO:  probably because about to release?
            relaxResources(false); // release everything except MediaPlayer
            MediaMetadataCompat track = mMusicProvider.getMusic(
                    MediaIDHelper.extractMusicIDFromMediaID(item.getDescription().getMediaId()));

            //noinspection ResourceType
            // TODO:  will need source later for what?  because might not need to filter url stuff from it now, or filter better for filenames
            String source = track.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
            if (source != null) {
                Log.d(TAG, "play: source=" + source);
                source = source.replaceAll(" ", "%20"); // Escape spaces for URLs
            } else {
                Log.d(TAG, "play: source is null");
            }

                try {
                createMediaPlayerIfNeeded();

                this.setState(PlaybackStateCompat.STATE_BUFFERING);  // TODO:  now we're starting again

                // TODO:  deprecated.  use setAudioAttributes instead (https://developer.android.com/reference/android/media/AudioFocusRequest.Builder.html),
                // TODO:  except that requires API level O (the letter), so 1.  weird and 2.  maybe wait.
                // TODO:  use https://developer.android.com/reference/android/media/AudioAttributes.html#CONTENT_TYPE_SPEECH

                // TODO:  to play mp3 while downloading, see https://stackoverflow.com/questions/41373243/playing-a-music-file-while-downloading-it for ExoPlayer.
                /*
                 from online, reading mp3s locally is still STREAM_MUSIC:
                 String u = "/sdcard/Download/01 Ae Dil Hai Mushkil - Title Song (Arijit Singh) 320kbps.mp3";
                 MediaPlayer mediaPlayer = new MediaPlayer();
                 mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                 mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(u));
                 mediaPlayer.prepare();
                 mediaPlayer.start();
                 */
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(source);

                // Starts preparing the media player in the background. When
                // it's done, it will call our OnPreparedListener (that is,
                // the onPrepared() method on this class, since we set the
                // listener to 'this'). Until the media player is prepared,
                // we *cannot* call start() on it!
                mMediaPlayer.prepareAsync();

                // If we are streaming from the internet, we want to hold a
                // Wifi lock, which prevents the Wifi radio from going to
                // sleep while the song is playing.
                //darya mWifiLock.acquire();

                if (mCallback != null) {
                    mCallback.onPlaybackStatusChanged(this.getState());
                }

            } catch (IOException ex) {
                LogHelper.e(TAG, ex, "Exception playing song");
                if (mCallback != null) {
                    mCallback.onError(ex.getMessage());
                }
            }
        }
    }

    @Override
    public void pause() {
        if (this.getState() == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentPosition = mMediaPlayer.getCurrentPosition();
            }
            // while paused, retain the MediaPlayer but give up audio focus
            relaxResources(false);
        }
        this.setState(PlaybackStateCompat.STATE_PAUSED);
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(this.getState());
        }
        unregisterAudioNoisyReceiver();
    }


    @Override
    public void seekTo(int position) {
        LogHelper.d(TAG, "seekTo called with ", position);

        if (mMediaPlayer == null) {
            // If we do not have a current media player, simply update the current position
            mCurrentPosition = position;
        } else {
            if (mMediaPlayer.isPlaying()) {
                this.setState(PlaybackStateCompat.STATE_BUFFERING);
            }
            registerAudioNoisyReceiver();
            mMediaPlayer.seekTo(position);
            if (mCallback != null) {
                mCallback.onPlaybackStatusChanged(this.getState());
            }
        }
    }

    @Override
    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public void setCurrentStreamPosition(int pos) {
        this.mCurrentPosition = pos;
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        this.mCurrentMediaId = mediaId;
    }

    @Override
    public String getCurrentMediaId() {
        return mCurrentMediaId;
    }

    /**
     * Try to get the system audio focus.
     */
    private void tryToGetAudioFocus() {
        LogHelper.d(TAG, "tryToGetAudioFocus");
        int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mAudioFocus = AUDIO_FOCUSED;
        } else {
            mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    /**
     * Give up the audio focus.
     */
    private void giveUpAudioFocus() {
        LogHelper.d(TAG, "giveUpAudioFocus");
        if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the MediaPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes mPlayer !=
     * null, so if you are calling it, you have to do so from a context where
     * you are sure this is the case.
     */
    private void configMediaPlayerState() {
        LogHelper.d(TAG, "configMediaPlayerState. mAudioFocus=", mAudioFocus);
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (this.getState() == PlaybackStateCompat.STATE_PLAYING) {
                pause();
            }
        } else {  // we have audio focus:
            registerAudioNoisyReceiver();
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
            } else {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
                } // else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
                    LogHelper.d(TAG,"configMediaPlayerState startMediaPlayer. seeking to ",
                        mCurrentPosition);
                    if (mCurrentPosition == mMediaPlayer.getCurrentPosition()) {
                        mMediaPlayer.start();
                        this.setState(PlaybackStateCompat.STATE_PLAYING);
                    } else {
                        mMediaPlayer.seekTo(mCurrentPosition);
                        this.setState(PlaybackStateCompat.STATE_BUFFERING);
                    }
                }
                mPlayOnFocusGain = false;
            }
        }
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(this.getState());
        }
    }

    /**
     * Listener, called by AudioManager on audio focus changes.
     * see https://developer.android.com/guide/topics/media-apps/volume-and-earphones.html#audio-focus
     * "When an app acquires audio focus it must be able to release it when another app requests audio focus for itself. When this happens your app receives a call to the onAudioFocusChange() method in the AudioFocusChangeListener that you specified when the app called requestAudioFocus().

     The focusChange parameter passed to onAudioFocusChange() indicates the kind of change that's happening. It corresponds to the duration hint used by the app that's aquiring focus. Your app should respond appropriately.

     Transient loss of focus
     If the focus change is transient (AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK or AUDIOFOCUS_LOSS_TRANSIENT), your app should duck or pause playing but otherwise maintain the same state.
     During a transient loss of audio focus you should continue to monitor changes in audio focus and be prepared to resume normal playback when you regain the focus. When the blocking app abandons focus you'll receive a a callback (AUDIOFOCUS_GAIN) . At this point you can restore the volume to normal level or restart playback.

     Permanent loss of focus
     If the audio focus loss is permanent (AUDIOFOCUS_LOSS), another application is playing audio. Your app should pause play immediately. At this point your app will never receive an AUDIOFOCUS_GAIN callback. To restart playback the user must take an explicit action, like pressing the play transport control in a notification or app UI.
     After pausing your app should wait a short interval and then stop its media session to release resources and abandon audio focus. Delaying the stop call gives the user the opportunity to restart your app's playback. This can be useful if your app goes silent because the user accidentally started a different app that requested the audio focus."
     *
     * "To ensure the delayed stop does not kick in if the user restarts playback, call mHandler.removeCallbacks(mDelayedStopRunnable) in response to any state changes. For example, call removeCallbacks() in your Callback's onPlay(), onSkipToNext(), etc. You should also call this method in your service's onDestroy() callback when cleaning up the resources used by your service."
     * TODO:  I don't see above doc-prescribed removeCallbacks called, check to see if something else calls it or if it needs to be added.
     *
     * Implementation of {@link android.media.AudioManager.OnAudioFocusChangeListener}
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        LogHelper.d(TAG, "onAudioFocusChange. focusChange=", focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            mAudioFocus = AUDIO_FOCUSED;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (this.getState() == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            LogHelper.e(TAG, "onAudioFocusChange: Ignoring unsupported focusChange: ", focusChange);
        }
        configMediaPlayerState();
    }

    /**
     * Called when MediaPlayer has completed a seek
     *
     * @see OnSeekCompleteListener
     */
    @Override
    public void onSeekComplete(MediaPlayer mp) {
        LogHelper.d(TAG, "onSeekComplete from MediaPlayer:", mp.getCurrentPosition());
        mCurrentPosition = mp.getCurrentPosition();
        if (this.getState() == PlaybackStateCompat.STATE_BUFFERING) {
            registerAudioNoisyReceiver();
            mMediaPlayer.start();
            this.setState(PlaybackStateCompat.STATE_PLAYING);
        }
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(this.getState());
        }
    }

    /**
     * Called when media player is done playing current song.
     *
     * @see OnCompletionListener
     */
    @Override
    public void onCompletion(MediaPlayer player) {
        LogHelper.d(TAG, "onCompletion from MediaPlayer");
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (mCallback != null) {
            mCallback.onCompletion();
        }
    }

    /**
     * Called when media player is done preparing.
     *
     * @see OnPreparedListener
     */
    @Override
    public void onPrepared(MediaPlayer player) {
        LogHelper.d(TAG, "onPrepared from MediaPlayer");
        // The media player is done preparing. That means we can start playing if we
        // have audio focus.
        configMediaPlayerState();
    }

    /**
     * Called when there's an error playing media. When this happens, the media
     * player goes to the Error state. We warn the user about the error and
     * reset the media player.
     *
     * @see OnErrorListener
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        LogHelper.e(TAG, "Media player error: what=" + what + ", extra=" + extra);
        if (mCallback != null) {
            mCallback.onError("MediaPlayer error " + what + " (" + extra + ")");
        }
        return true; // true indicates we handled the error
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private void createMediaPlayerIfNeeded() {
        LogHelper.d(TAG, "createMediaPlayerIfNeeded. needed? ", (mMediaPlayer==null));
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mMediaPlayer.setWakeMode(mContext.getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing,
            // and when it's done playing:
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
        } else {
            mMediaPlayer.reset();
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     *            be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        LogHelper.d(TAG, "relaxResources. releaseMediaPlayer=", releaseMediaPlayer);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        //darya if (mWifiLock.isHeld()) {
        //darya     mWifiLock.release();
        //darya }
    }


    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mContext.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }


    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mContext.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }
}
