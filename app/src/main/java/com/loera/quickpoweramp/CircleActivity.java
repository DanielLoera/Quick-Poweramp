package com.loera.quickpoweramp;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lge.qcircle.template.QCircleTemplate;
import com.lge.qcircle.template.TemplateTag;
import com.lge.qcircle.template.TemplateType;
import com.maxmpz.poweramp.player.PowerampAPI;

import java.util.List;

public class CircleActivity extends Activity implements CoverReceiver.CoverListener {

    private final String TAG = "Circle Activity";
    private AudioManager audioMgr;
    private SharedPreferences prefs;
    private Bitmap artBitmap;

    static final int RESULT_ENABLE = 1;
    private int PLAYER_STYLE;
    private int PLAYER_COLOR;
    private int CONTRAST_COLOR;
    private int numTaps;
    private boolean tapRecognizing;
    private boolean requestAdmin;
    private boolean next;
    private boolean initialClose;
    private boolean initialArt;
    private boolean runWhilePowerampRunning;


    private ImageView albumArt;
    private TextView songText;
    private TextView albumArtistText;
    private ImageButton playPauseButton;
    private ImageButton volume, exit, ff, rw;
    private PercentRelativeLayout nonArtLayout;
    private RelativeLayout adminDialog;
    private DevicePolicyManager deviceManger;
    private ComponentName compName;


    //For Layout 2 Only.
    private ImageView animationArt;
    private ImageButton controlIndicator;
    private AlphaAnimation anim;
    private float gesXStart, gesXEnd, gesYStart, gesYEnd;
    private long gesTimeStart, gesTimeEnd;


    private Intent trackIntent;
    private Intent artIntent;
    private Intent statusIntent;

    private Bundle mCurrentTrack;

    private Context context;

    private BroadcastReceiver mTrackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            trackIntent = intent;
            processTrackIntent();
            Log.w(TAG, "mTrackReceiver " + intent);
        }
    };

    private BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            statusIntent = intent;
            updateStatusUI();
        }

        private void updateStatusUI() {
            Log.w(TAG, "updateStatusUI");
            if (statusIntent != null) {
                boolean paused = true;

                int status = statusIntent.getIntExtra(PowerampAPI.STATUS, -1);

                switch (status) {
                    case PowerampAPI.Status.TRACK_PLAYING:
                        paused = statusIntent.getBooleanExtra(PowerampAPI.PAUSED, false);
                        break;

                    case PowerampAPI.Status.TRACK_ENDED:
                    case PowerampAPI.Status.PLAYING_ENDED:
                        break;
                }

                if (PLAYER_STYLE == R.id.option_1) {
                    int statusIcon;
                    if (CONTRAST_COLOR == Color.WHITE) {
                        statusIcon = paused ? R.drawable.ic_play_white_36dp : R.drawable.ic_pause_white_36dp;
                    } else {
                        statusIcon = paused ? R.drawable.ic_play_black_36dp : R.drawable.ic_pause_black_36dp;
                    }
                    playPauseButton.setImageDrawable(ContextCompat.getDrawable(context, statusIcon));
                } else {
                    int statusIcon = paused ? R.drawable.ic_pause_white_48dp : R.drawable.ic_play_white_48dp;
                    controlIndicator.setImageDrawable(ContextCompat.getDrawable(context, statusIcon));
                    controlIndicator.startAnimation(anim);
                }
            }
        }


    };

    private BroadcastReceiver mAAReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            artIntent = intent;
            updateAlbumArt();
            Log.w(TAG, "mAAReceiver " + intent);
        }
    };
    private BroadcastReceiver mCoverReceiver = new CoverReceiver(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        initialClose = true;
        prefs = getSharedPreferences("Default", Context.MODE_PRIVATE);
        deviceManger = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        setupDeviceManager();
        getPreferences();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        audioMgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        registerReceivers();
        setContentView(createTemplate().getView());
        setContrastUI();
    }

    public void onStart() {
        super.onStart();
        initialArt = true;
    }

    private void setupDeviceManager() {
        deviceManger = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        compName = new ComponentName(this, MyAdmin.class);
    }

    private void getPreferences() {

        runWhilePowerampRunning = prefs.getBoolean("run in bg", false);

        int styleTemp = prefs.getInt("style", -1);
        if (styleTemp == -1) {
            PLAYER_STYLE = R.id.option_1;
            prefs.edit().putInt("style", PLAYER_STYLE).apply();
        } else {
            PLAYER_STYLE = styleTemp;
        }
        int colorTemp = prefs.getInt("color", -1);
        if (colorTemp == -1) {
            PLAYER_COLOR = ContextCompat.getColor(context, R.color.colorPrimary);
            prefs.edit().putInt("color", PLAYER_COLOR).apply();
        } else {
            PLAYER_COLOR = colorTemp;
        }

        CONTRAST_COLOR = AppUtils.contrastColor(PLAYER_COLOR);

    }

    private void setContrastUI() {
        if (PLAYER_STYLE == R.id.option_1 && CONTRAST_COLOR != Color.WHITE) {
            exit = (ImageButton) findViewById(R.id.exit);
            volume = (ImageButton) findViewById(R.id.volume);
            ff = (ImageButton) findViewById(R.id.ff);
            rw = (ImageButton) findViewById(R.id.rw);

            exit.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_close_black_24dp));
            volume.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_volume_high_black_24dp));
            ff.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_fast_forward_black_36dp));
            rw.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_rewind_black_36dp));
            songText.setTextColor(Color.BLACK);
            albumArtistText.setTextColor(Color.BLACK);
        }
    }

    private QCircleTemplate createTemplate() {
        QCircleTemplate template = new QCircleTemplate(this, TemplateType.CIRCLE_EMPTY);
        View myContent;
        if (PLAYER_STYLE == R.id.option_1) {
            myContent = getLayoutInflater().inflate(R.layout.player_layout_1, null);

            albumArt = (ImageView) myContent.findViewById(R.id.albumArt);
            songText = (TextView) myContent.findViewById(R.id.songText);
            albumArtistText = (TextView) myContent.findViewById(R.id.albumArtistText);
            playPauseButton = (ImageButton) myContent.findViewById(R.id.pausePlay);
            nonArtLayout = (PercentRelativeLayout) myContent.findViewById(R.id.nonArt);
            nonArtLayout.setBackgroundColor(PLAYER_COLOR);
        } else {
            initialArt = true;
            myContent = getLayoutInflater().inflate(R.layout.player_layout_2, null);
            albumArt = (ImageView) myContent.findViewById(R.id.albumArt);
            animationArt = (ImageView) myContent.findViewById(R.id.animationArt);
            songText = (TextView) myContent.findViewById(R.id.songText);
            albumArtistText = (TextView) myContent.findViewById(R.id.albumArtistText);
            controlIndicator = (ImageButton) myContent.findViewById(R.id.control_indicator);
            anim = new AlphaAnimation(1.0f, 0.0f);
            anim.setDuration(2000);
            anim.setFillAfter(true);
        }
        adminDialog = (RelativeLayout) myContent.findViewById(R.id.adminDialog);
        songText.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        albumArtistText.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        songText.setSelected(true);
        albumArtistText.setSelected(true);

        RelativeLayout circleLayout = template.getLayoutById(TemplateTag.CONTENT);
        circleLayout.addView(myContent);
        return template;
    }

    private void registerReceivers() {
        registerReceiver(mTrackReceiver, new IntentFilter(PowerampAPI.ACTION_TRACK_CHANGED));
        registerReceiver(mAAReceiver, new IntentFilter(PowerampAPI.ACTION_AA_CHANGED));
        registerReceiver(mStatusReceiver, new IntentFilter(PowerampAPI.ACTION_STATUS_CHANGED));
        registerReceiver(mCoverReceiver, new IntentFilter("com.lge.android.intent.action.ACCESSORY_COVER_EVENT"));
    }

    private void processTrackIntent() {

        mCurrentTrack = null;

        if (trackIntent != null) {
            mCurrentTrack = trackIntent.getBundleExtra(PowerampAPI.TRACK);
            updateTrackUI();
        }
    }

    private void updateTrackUI() {
        String title = "";
        String album = "";
        String artist = "";

        if (trackIntent != null && mCurrentTrack != null) {
            title = mCurrentTrack.getString(PowerampAPI.Track.TITLE);
            album = mCurrentTrack.getString(PowerampAPI.Track.ALBUM);
            artist = mCurrentTrack.getString(PowerampAPI.Track.ARTIST);
        }

        Log.i(TAG, "Title: " + title + "\nAlbum: " + album + "\nArtist: " + artist);

        songText.setText(title);
        albumArtistText.setText(artist + " - " + album);

        songText.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        songText.setSingleLine(true);
        songText.setMarqueeRepeatLimit(-1);
        songText.setFocusable(true);
        songText.setFocusableInTouchMode(true);
        songText.setSelected(true);

        albumArtistText.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        albumArtistText.setSingleLine(true);
        albumArtistText.setMarqueeRepeatLimit(-1);
        albumArtistText.setFocusable(true);
        albumArtistText.setFocusableInTouchMode(true);
        albumArtistText.setSelected(true);

    }

    private void updateAlbumArt() {

        Log.w(TAG, "updateAlbumArt");
        String directAAPath = artIntent.getStringExtra(PowerampAPI.ALBUM_ART_PATH);

        if (!TextUtils.isEmpty(directAAPath)) {
            Log.w(TAG, "has AA, albumArtPath=" + directAAPath);
            Bitmap temp = BitmapFactory.decodeFile(directAAPath);
            if (artBitmap == null || !artBitmap.equals(temp)) {
                artBitmap = temp;
                if (PLAYER_STYLE == R.id.option_1)
                    albumArt.setImageBitmap(artBitmap);
                else
                    animateImageView(artBitmap);
            }
        } else {
            Bitmap temp = artIntent.getParcelableExtra(PowerampAPI.ALBUM_ART_BITMAP);
            if (artBitmap == null || !artBitmap.equals(temp)) {
                artBitmap = temp;
                Log.w(TAG, "has AA, bitmap");
                if (PLAYER_STYLE == R.id.option_1)
                    albumArt.setImageBitmap(artBitmap);
                else
                    animateImageView(artBitmap);
            }
        }
    }

    private void animateImageView(final Bitmap newArt) {
        if (initialArt) {
            albumArt.setImageBitmap(newArt);
            initialArt = false;
            return;
        }
        Animation animOut;
        final Animation animIn;
        if (next) {
            animOut = AnimationUtils.loadAnimation(context, R.anim.left_out);
            animIn = AnimationUtils.loadAnimation(context, R.anim.left_out);
            animationArt.setX(albumArt.getX() + albumArt.getWidth());
        } else {
            animOut = AnimationUtils.loadAnimation(context, R.anim.right_out);
            animIn = AnimationUtils.loadAnimation(context, R.anim.right_out);
            animationArt.setX(albumArt.getX() - albumArt.getWidth());
        }
        animationArt.setVisibility(View.VISIBLE);
        animationArt.setImageBitmap(newArt);
        animIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                albumArt.setImageBitmap(newArt);
                animationArt.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                animationArt.startAnimation(animIn);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        albumArt.startAnimation(animOut);
    }

    public void playPausePressed(View v) {
        Intent playPause = new Intent(PowerampAPI.ACTION_API_COMMAND).putExtra(PowerampAPI.COMMAND, PowerampAPI.Commands.TOGGLE_PLAY_PAUSE);
        startService(createExplicitFromImplicitIntent(this, playPause));
    }

    public void nextPressed(View v) {
        Intent next = new Intent(PowerampAPI.ACTION_API_COMMAND).putExtra(PowerampAPI.COMMAND, PowerampAPI.Commands.NEXT);
        startService(createExplicitFromImplicitIntent(this, next));
        if (PLAYER_STYLE != R.id.option_1)
            updateControlIndicator(true);
    }

    public void previousPressed(View v) {
        Intent previous = new Intent(PowerampAPI.ACTION_API_COMMAND).putExtra(PowerampAPI.COMMAND, PowerampAPI.Commands.PREVIOUS);
        startService(createExplicitFromImplicitIntent(this, previous));
        if (PLAYER_STYLE != R.id.option_1)
            updateControlIndicator(false);
    }

    public void volumePressed(View v) {
        audioMgr.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
    }

    public void cancelPressed(View v) {
        closeCircleView();
    }

    private void closeCircleView() {
        moveTaskToBack(true);
    }

    private Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    private void updateControlIndicator(boolean next) {
        if (next) {
            controlIndicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_fast_forward_white_48dp));
            controlIndicator.startAnimation(anim);
        } else {
            controlIndicator.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_rewind_white_48dp));
            controlIndicator.startAnimation(anim);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            gesTimeStart = System.currentTimeMillis();
            gesXStart = event.getRawX();
            gesYStart = event.getRawY();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            gesTimeEnd = System.currentTimeMillis();
            gesXEnd = event.getRawX();
            gesYEnd = event.getRawY();
            recognizeGestures();
        }

        return super.onTouchEvent(event);
    }

    private void lockScreen() {
        boolean active = deviceManger.isAdminActive(compName);
        if (active) {
            deviceManger.lockNow();
        } else {
            requestAdmin = true;
            adminDialog.setVisibility(View.VISIBLE);
        }
    }

    private void requestAdminAccess() {
        Intent intent = new Intent(DevicePolicyManager
                .ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                compName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Used to lock the screen when you double tap.");
        startActivityForResult(intent, RESULT_ENABLE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_ENABLE:
                if (resultCode == Activity.RESULT_OK) {
                    Log.i("DeviceAdmin", "Admin enabled!");
                } else {
                    Log.i("DeviceAdmin", "Admin enable FAILED!");
                }
                closeCircleView();
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void recognizeGestures() {
        long timeTaken = gesTimeEnd - gesTimeStart;


        //if negative, it is a right swipe
        float xDiff = gesXStart - gesXEnd;
        //if negative, it is a up swipe
        float yDiff = gesYStart - gesYEnd;

        float absX = Math.abs(xDiff);
        float absY = Math.abs(yDiff);

        if (absX <= 150 && absY <= 150) {
            if (timeTaken >= 500) {
                closeCircleView();
                return;
            }
            numTaps++;
            if (!tapRecognizing)
                new TapRecognizer().execute();
            return;
        }

        if (PLAYER_STYLE == R.id.option_1)
            return;

        //X Swipe was greater, using this gesture
        if (absX >= absY) {
            Log.i(TAG, "Using X Swipe");

            if (xDiff > 0) {
                Log.i(TAG, "Left Swipe");
                next = true;
                nextPressed(null);
            } else {
                Log.i(TAG, "Right Swipe");
                next = false;
                previousPressed(null);
            }
            return;
        } else {
            //Y Swipe was greater, using this gesture
            Log.i(TAG, "Using Y Swipe");
            if (yDiff > 0) {
                Log.i(TAG, "Up Swipe");
                audioMgr.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            } else {
                Log.i(TAG, "Down Swipe");
                audioMgr.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            }

        }
    }

    @Override
    public void onCoverClosed() {
        if (!initialClose) {
            if(AppUtils.powerampIsRunning(this)){
                startActivity(new Intent(context, CircleActivity.class)
                        .setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER));
            }
        }
        initialClose = false;
    }

    @Override
    public void onCoverOpened() {
        if (requestAdmin) {
            adminDialog.setVisibility(View.INVISIBLE);
            requestAdminAccess();
        } else {
        closeCircleView();
        startActivity(new Intent(context, MainActivity.class).putExtra("cover opened", true));
        }
    }

    private class TapRecognizer extends AsyncTask<Void, Void, Void> {
        private final String TAG = "Tap Recognizer";

        @Override
        protected Void doInBackground(Void... params) {
            tapRecognizing = true;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.i(TAG, e.toString());
            }
            return null;
        }

        public void onPostExecute(Void v) {
            if (numTaps <= 1) {
                if (PLAYER_STYLE != R.id.option_1) {
                    Log.i(TAG, "Single Tap Recognized.");
                    playPausePressed(null);
                }
            } else {
                Log.i(TAG, "Double Tap Recognized.");
                lockScreen();
            }
            numTaps = 0;
            tapRecognizing = false;
        }
    }

    public static class MyAdmin extends DeviceAdminReceiver {

        void showToast(Context context, CharSequence msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEnabled(Context context, Intent intent) {
            showToast(context, "Device Admin: enabled");
        }

        @Override
        public CharSequence onDisableRequested(Context context, Intent intent) {
            return "You wont be able to double tap to lock the screen.";
        }

        @Override
        public void onDisabled(Context context, Intent intent) {
            showToast(context, "Device Admin: disabled");
        }

    }
}