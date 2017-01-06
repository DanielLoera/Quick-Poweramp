package com.loera.quickpoweramp;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Created by Daniel on 5/30/2016.
 * <p>
 * :)
 */

public class AppUtils {

    public static int contrastColor(int color) {
        double a = 1 - ( 0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color))/255;

        if (a < 0.5)
            return Color.BLACK; // bright colors - black font

        return Color.WHITE;// dark colors - white font
    }
    
    
    /*Lazy way to check if poweramp is running  ¯\_(ツ)_/¯*/

    public static boolean powerampIsRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> procInfos = activityManager.getRunningServices(100);
        for (ActivityManager.RunningServiceInfo info : procInfos) {
            if (info.process.contains("maxmpz")) {
                return true;
            }
        }
        return false;
    }
}
