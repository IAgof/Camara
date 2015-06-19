package com.example.root.kickflip.sdk;

import android.os.Build;
import android.os.Environment;
import android.text.format.DateUtils;

import com.example.root.kickflip.sdk.av.SessionConfig;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by David Brodsky on 3/20/14.
 */
public class Util {
    //"04/03/2014 23:41:37",
    private static SimpleDateFormat mMachineSdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
    ;
    private static SimpleDateFormat mHumanSdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US);

    final public static String PATH_APP = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES) + File.separator + "kickFlip";

    final public static String pathVideoEdited = PATH_APP + File.separator + "V_EDIT_" +
            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp4";

    final public static String testRecorded = "V_TEST_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
            + ".mp4";

    static {
        mMachineSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Returns whether the current device is running Android 4.4, KitKat, or newer
     *
     * KitKat is required for certain Kickflip features like Adaptive bitrate streaming
     */
    public static boolean isKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static String getHumanDateString() {
        return mHumanSdf.format(new Date());
    }

    public static String getHumanRelativeDateStringFromString(String machineDateStr) {
        String result = null;
        try {
            result = DateUtils.getRelativeTimeSpanString(mMachineSdf.parse(machineDateStr).getTime()).toString();
            result = result.replace("in 0 minutes", "just now");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }


    //amm
    public static SessionConfig create720pSessionConfig(String pathDir) {
        String outputLocation = new File(pathDir, testRecorded).getAbsolutePath();
        SessionConfig config = new SessionConfig.Builder(outputLocation)
                .withVideoBitrate(5 * 1000 * 1000)
                .withAudioBitrate(192 * 1000)
                .build();
        return config;
    }





}
