package com.loera.quickpoweramp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

/**
 * Created by Daniel on 7/3/2016.
 * <p>
 * :)
 */

public class CoverReceiver extends BroadcastReceiver{

    public interface CoverListener {
        void onCoverClosed();
        void onCoverOpened();
    }

    public CoverReceiver(CoverListener coverListener){
        this.coverListener = coverListener;
    }

    private final CoverListener coverListener;
    private final String TAG = "Cover Receiver";
    private final int EXTRA_ACCESSORY_COVER_OPENED = 0;
    private final int EXTRA_ACCESSORY_COVER_CLOSED = 1;
    private final String EXTRA_ACCESSORY_COVER_STATE = "com.lge.intent.extra.ACCESSORY_COVER_STATE";
    private final String ACTION_ACCESSORY_COVER_EVENT = "com.lge.android.intent.action.ACCESSORY_COVER_EVENT";
    private final int QUICKCOVERSETTINGS_QUICKCIRCLE = 3;
    private final int QUICKCOVERSETTINGS_USEQUICKCIRCLE = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        int quickCaseType = Settings.Global.getInt(context.getContentResolver(), "cover_type", 0);
        int quickCircleEnabled = Settings.Global.getInt(context.getContentResolver(), "quick_view_enable", 0);
        // Receives a LG QCirle intent for the cover event
        if (quickCaseType == QUICKCOVERSETTINGS_QUICKCIRCLE && quickCircleEnabled == QUICKCOVERSETTINGS_USEQUICKCIRCLE) {
            // Gets the current state of the cover
            int quickCoverState = intent.getIntExtra(EXTRA_ACCESSORY_COVER_STATE, EXTRA_ACCESSORY_COVER_OPENED);
            if (quickCoverState == EXTRA_ACCESSORY_COVER_CLOSED) {
                // closed
                Log.i(TAG, "Cover Closed.");
                coverListener.onCoverClosed();
            } else if (quickCoverState == EXTRA_ACCESSORY_COVER_OPENED) {
                // opened
                Log.i(TAG, "Cover Opened.");
                coverListener.onCoverOpened();
            }
        }
    }
}
