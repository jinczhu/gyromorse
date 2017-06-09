package com.jc.gyromorse;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.io.FileOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity
        implements IntegratedTapDetector.TapListener

{

    private static final long MIN_TIME_BETWEEN_TOUCH_AND_TAP_NANOS = 500 * 1000 * 1000;
    private static final long MILIS_PER_NANO = 1000 * 1000;


    TextView xCoor; // declare X axis object
    TextView yCoor; // declare Y axis object
    TextView zCoor; // declare Z axis object


    public long mstapcount=0;
    public long mdtapcount=0;
    private IntegratedTapDetector mIntegratedTapDetector;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xCoor=(TextView)findViewById(R.id.xcoor); // create X axis object
        yCoor=(TextView)findViewById(R.id.ycoor); // create Y axis object
        zCoor=(TextView)findViewById(R.id.zcoor); // create Z axis object

        mIntegratedTapDetector = new IntegratedTapDetector(
                (SensorManager) this.getSystemService(SENSOR_SERVICE));
        mIntegratedTapDetector.addListener(this);
        mIntegratedTapDetector
                .setPostDelayTimeMillis(MIN_TIME_BETWEEN_TOUCH_AND_TAP_NANOS / MILIS_PER_NANO);


/*        // assign directions
        float x=event.values[0];
        float y=event.values[1];
        float z=event.values[2];

        xCoor.setText("X: "+x);
        yCoor.setText("Y: "+y);
        zCoor.setText("Z: "+z);*/
        mIntegratedTapDetector.start();


    }



    @Override
    public void onSingleTap(long timeStamp) {
       /* boolean talkBackActive = TalkBackService.isServiceActive();
        boolean tapIsntFromScreenTouch =
                (Math.abs(timeStamp - mLastTouchTime) > MIN_TIME_BETWEEN_TOUCH_AND_TAP_NANOS);
        boolean tapIsntFromHaptic =
                (Math.abs(timeStamp - mLastHapticTime) > MIN_TIME_BETWEEN_HAPTIC_AND_TAP_NANOS);
        if (talkBackActive && tapIsntFromScreenTouch && tapIsntFromHaptic) {
            SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(mContext);
            mGestureController.performAction(prefs.getString(
                    mContext.getString(R.string.pref_shortcut_single_tap_key),
                    mContext.getString(R.string.pref_shortcut_single_tap_default)));
        }
        */
        mstapcount++;
        xCoor.setText("X: "+mstapcount);

    }

    /* Handle a double tap on the side of the device */
    @Override
    public void onDoubleTap(long timeStamp) {
        /*boolean talkBackActive = TalkBackService.isServiceActive();
        boolean tapIsntFromScreenTouch =
                (Math.abs(timeStamp - mLastTouchTime) > MIN_TIME_BETWEEN_TOUCH_AND_TAP_NANOS);
        boolean tapIsntFromHaptic =
                (Math.abs(timeStamp - mLastHapticTime) > MIN_TIME_BETWEEN_HAPTIC_AND_TAP_NANOS);
        if (talkBackActive && tapIsntFromScreenTouch && tapIsntFromHaptic) {
            SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(mContext);
            mGestureController.performAction(prefs.getString(
                    mContext.getString(R.string.pref_shortcut_double_tap_key),
                    mContext.getString(R.string.pref_shortcut_double_tap_default)));
        }*/
        mdtapcount++;
        yCoor.setText("Y: "+mdtapcount);
    }






}