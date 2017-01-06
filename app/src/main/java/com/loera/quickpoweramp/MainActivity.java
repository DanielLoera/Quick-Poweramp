package com.loera.quickpoweramp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity implements CoverReceiver.CoverListener{

    private int PLAYER_COLOR;
    private int PLAYER_STYLE;

    private Context context;
    private SharedPreferences prefs;
    private final String message = "Leave a rating on the Play Store if you are a cool person. " + getEmijoByUnicode(0x1F60E);

    private RelativeLayout colorChangeLayout;
    private TextView colorText;
    private RadioButton option1;
    private RadioButton option2;
    private AdView ad;
    private BroadcastReceiver coverReceiver = new CoverReceiver(this);
    private final String PLAYSTORE_URL = "https://play.google.com/store/apps/details?id=com.loera.quickpoweramp";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        MobileAds.initialize(context, "SECRET");
        prefs = getSharedPreferences("Default",MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        initializeAd();
        colorChangeLayout = (RelativeLayout)findViewById(R.id.color_change);
        colorText = (TextView)findViewById(R.id.colorText);
        option1 = (RadioButton) findViewById(R.id.option_1);
        option2 = (RadioButton) findViewById(R.id.option_2);
        ((TextView)findViewById(R.id.message)).setText(message);

        PLAYER_STYLE = prefs.getInt("style",R.id.option_1);
        PLAYER_COLOR = prefs.getInt("color", ContextCompat.getColor(context,R.color.colorPrimary));
        updateUI();
        registerReceiver(coverReceiver,  new IntentFilter("com.lge.android.intent.action.ACCESSORY_COVER_EVENT"));
        if(getIntent().getBooleanExtra("cover opened", false)){
            moveTaskToBack(true);
        }
    }

    public void playstoreRate(View v){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAYSTORE_URL));
        startActivity(browserIntent);
    }

    private void initializeAd() {
        ad = (AdView) findViewById(R.id.ad);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        ad.loadAd(adRequest);
    }

    private void updateUI(){
        colorChangeLayout.setBackgroundColor(PLAYER_COLOR);
        getRadio(PLAYER_STYLE).setChecked(true);
    }

    public void changeColor(View v){
        ColorPickerDialogBuilder
                .with(context)
                .setTitle("Choose color")
                .initialColor(PLAYER_COLOR)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int selectedColor) {

                    }
                })
                .setPositiveButton("ok", new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        PLAYER_COLOR = selectedColor;
                        colorText.setTextColor(AppUtils.contrastColor(PLAYER_COLOR));
                        prefs.edit().putInt("color",PLAYER_COLOR).apply();
                        updateUI();
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .build()
                .show();

    }

    public void changeStyle(View v){

        getRadio(PLAYER_STYLE).setChecked(false);
        PLAYER_STYLE = v.getId();
        prefs.edit().putInt("style", PLAYER_STYLE).apply();
        updateUI();

    }

    public String getEmijoByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }

    private RadioButton getRadio(int id){
        switch(id){
            case R.id.option_1:
                return option1;
            case R.id.option_2:
                return option2;
            default:
                return option1;
        }
    }


    @Override
    public void onCoverClosed() {
        if(AppUtils.powerampIsRunning(this))
        startActivity(new Intent(this, CircleActivity.class).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER));
    }

    @Override
    public void onCoverOpened() {

    }
}
