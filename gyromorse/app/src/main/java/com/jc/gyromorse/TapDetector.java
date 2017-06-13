package com.jc.gyromorse;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * Created by jc on 6/12/17.
 */

public class TapDetector implements SensorEventListener {

//thresholds:
    private static  long   WINDOW_TIMER        = 300L;
    private static  long   LATENCY_TIMER       = 100L;
    private static  long   DIFF_UPDATE_TIMEOUT = 3L;
    private static  long   PI_LOWER_THRESH     = 10L;
    private static  long   PI_HIGHER_THRESH    = 40L;
    private static  long   Z_THRESH            = 3L;
    private static  long   X_THRESH            = 2L;
    private static  long   Y_THRESH            = 2L;
 //   state
private float gravity[]={0f,0f,0f};
    private float ALPHA=0.15f;

    public void TapDetector()
    {

    }

    public void onAccuracyChanged(Sensor sensor,int accuracy){

    }

    @Override
    public void onSensorChanged(SensorEvent event) {


    }

    private float[] highPass(float x, float y, float z) {

        float[] filteredValues = new float[3];

        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * x;
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * y;
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * z;

        filteredValues[0] = x - gravity[0];
        filteredValues[1] = y - gravity[1];
        filteredValues[2] = z - gravity[2];

        return filteredValues;

    }

}
