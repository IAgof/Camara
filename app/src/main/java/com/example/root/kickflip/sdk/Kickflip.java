package com.example.root.kickflip.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.root.kickflip.R;
import com.example.root.kickflip.sdk.activity.RecordActivity;
import com.example.root.kickflip.sdk.av.RecordListener;
import com.example.root.kickflip.sdk.av.SessionConfig;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This is a top-level manager class for all the fundamental SDK actions. Herer you can register
 * your Kickflip account credentials, start a live broadcast or play one back.
 * <p/>
 * <h2>Setup</h2>
 * Before use Kickflip must be setup with your Kickflip Client ID and Client Secret with
 * {@link #setup(Context, String, String)}. These tokens are available in your kickflip
 * account dashboard.
 * <h2>Example Usage</h2>
 * <b>Starting a single live broadcast</b>
 * <p/>
 * <ol>
 * <li>{@link #setup(Context, String, String)}</li>
 * <li>(Optional) {@link #setSessionConfig(com.example.root.kickflip.sdk.av.SessionConfig)}</li>
 * <li>{@link #startRecordActivity(Activity, com.example.root.kickflip.sdk.av.BroadcastListener)}</li>
 * </ol>
 * The {@link RecordActivity} will present a standard camera UI with controls
 * for starting and stopping the broadcast. When the broadcast is stopped, BroadcastActivity will finish
 * after notifying {@link com.example.root.kickflip.sdk.av.BroadcastListener#onBroadcastStop()}.
 * <p/>
 * <br/>
 * <b>Customizing broadcast parameters</b>
 * <p/>
 * As noted above, you can optionally call {@link #setSessionConfig(com.example.root.kickflip.sdk.av.SessionConfig)} before
 * each call to {@link #startRecordActivity(Activity, com.example.root.kickflip.sdk.av.BroadcastListener)}.
 * Here's an example of how to build a {@link com.example.root.kickflip.sdk.av.SessionConfig} with {@link com.example.root.kickflip.sdk.av.SessionConfig.Builder}:
 * <p/>
 * <code>
 *   SessionConfig config = new SessionConfig.Builder(mRecordingOutputPath)
 *     <br/>&nbsp.withTitle(Util.getHumanDateString())
 *     <br/>&nbsp.withDescription("Example Description")
 *     <br/>&nbsp.withVideoResolution(1280, 720)
 *     <br/>&nbsp.withVideoBitrate(2 * 1000 * 1000)
 *     <br/>&nbsp.withAudioBitrate(192 * 1000)
 *     <br/>&nbsp.withAdaptiveStreaming(true)
 *     <br/>&nbsp.withVerticalVideoCorrection(true)
 *     <br/>&nbsp.withExtraInfo(extraDataMap)
 *     <br/>&nbsp.withPrivateVisibility(false)
 *     <br/>&nbsp.withLocation(true)
 *
 *     <br/>&nbsp.build();
 *    <br/>Kickflip.setSessionConfig(config);
 *
 * </code>
 * <br/>
 * Note that SessionConfig is initialized with sane defaults for a 720p broadcast. Every parameter is optional.
 */
public class Kickflip {

    public static final String TAG = "Kickflip";

    // Per-Stream settings
    private static SessionConfig sSessionConfig;          // Absolute path to root storage location
    private static RecordListener sRecordListener;


    /**
     * Start {@link RecordActivity}. This Activity
     * facilitates control over a single live broadcast.
     * <p/>
     * <b>Must be called after {@link Kickflip#setup(Context, String, String)} or
     * {@link Kickflip#setup(Context, String, String, com.example.root.kickflip.sdk.api.KickflipCallback)}.</b>
     *
     * @param host     the host {@link Activity} initiating this action
     * @param listener an optional {@link com.example.root.kickflip.sdk.av.BroadcastListener} to be notified on
     *                 broadcast events
     */
    public static void startRecordActivity(Activity host, RecordListener listener) {
        checkNotNull(listener, host.getString(R.string.error_no_broadcastlistener));
        if (sSessionConfig == null) {
            setupDefaultSessionConfig();
        }

        sRecordListener = listener;

        Intent recordIntent = new Intent(host, RecordActivity.class);
        recordIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        host.startActivity(recordIntent);
    }



    /**
     * Get the {@link com.example.root.kickflip.sdk.av.BroadcastListener} to be notified on broadcast events.
     */
    public static RecordListener getRecordListener() {
        return sRecordListener;
    }

    /**
     * Set a {@link com.example.root.kickflip.sdk.av.BroadcastListener} to be notified on broadcast events.
     *
     * @param listener a {@link com.example.root.kickflip.sdk.av.BroadcastListener}
     */
    public static void setRecordListener(RecordListener listener) {
        sRecordListener = listener;
    }


    /**
     * Return the {@link com.example.root.kickflip.sdk.av.SessionConfig} responsible for configuring this broadcast.
     *
     * @return the {@link com.example.root.kickflip.sdk.av.SessionConfig} responsible for configuring this broadcast.
     * @hide
     */
    public static SessionConfig getSessionConfig() {
        return sSessionConfig;
    }

    /**
     * Clear the current SessionConfig, marking it as in use by a Broadcaster.
     * This is typically safe to do after constructing a Broadcaster, as it will
     * hold reference.
     *
     * @hide
     */
    public static void clearSessionConfig() {
        Log.i(TAG, "Clearing SessionConfig");
        sSessionConfig = null;
    }

    /**
     * Set the {@link com.example.root.kickflip.sdk.av.SessionConfig} responsible for configuring this broadcast.
     *
     * @param config the {@link com.example.root.kickflip.sdk.av.SessionConfig} responsible for configuring this broadcast.
     */
    public static void setSessionConfig(SessionConfig config) {
        sSessionConfig = config;
    }

    /**
     * Check whether credentials required for broadcast are provided
     *
     * @return true if credentials required for broadcast are provided. false otherwise
     */
    public static boolean readyToRecord() {
       //amm return sClientKey != null && sClientSecret != null && sSessionConfig != null;
        return true;
    }


    private static void setupDefaultSessionConfig() {
        Log.i(TAG, "Setting default SessonConfig");

        String outputLocation = new File(Util.PATH_APP, Util.testRecorded).getAbsolutePath();
        SessionConfig config = new SessionConfig.Builder(outputLocation)
                .withVideoBitrate(2 * 1000 * 1000)
                .withAudioBitrate(192 * 1000)
                .build();
    }

    /**
     * Returns whether the current device is running Android 4.4, KitKat, or newer
     *
     * KitKat is required for certain Kickflip features like Adaptive bitrate streaming
     */
    public static boolean isKitKat() {
        return Build.VERSION.SDK_INT >= 19;
    }

}
