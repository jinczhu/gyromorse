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

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

import java.io.FileOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements SensorEventListener{

    private SensorManager sensorManager;
    FileSave mFile = null;


    TextView xCoor; // declare X axis object
    TextView yCoor; // declare Y axis object
    TextView zCoor; // declare Z axis object

    private FileWriter writer;
    private FileOutputStream output;

    private List<AccelerometerClass> accelerometerDataList;
    private AccelerometerClass accelerometerData;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xCoor=(TextView)findViewById(R.id.xcoor); // create X axis object
        yCoor=(TextView)findViewById(R.id.ycoor); // create Y axis object
        zCoor=(TextView)findViewById(R.id.zcoor); // create Z axis object

        sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        // add listener. The listener will be MyActivity (this) class
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        mFile = new FileSave("morse.txt", false);


        try {
            File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"morsex.txt");
            output = new FileOutputStream(logFile);
            writer = new FileWriter(output.getFD());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // 当精度发生变化时调用
    public void onAccuracyChanged(Sensor sensor,int accuracy){

    }

    // 当sensor事件发生时候调用
    public void onSensorChanged(SensorEvent event){

        // check sensor type
        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){

            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date()) + ": ";


            // assign directions
            float x=event.values[0];
            float y=event.values[1];
            float z=event.values[2];

            xCoor.setText("X: "+x);
            yCoor.setText("Y: "+y);
            zCoor.setText("Z: "+z);


            String Content = currentDateTimeString  + " " + event.values[0] + " " + event.values[1] + " " + event.values[2];
            mFile.appendLog(Content);

            long tsLong = System.currentTimeMillis()/1000;
            recordAccelData(x, y, z, tsLong);

        }
    }

    @Override
    protected void onDestroy() {
        sensorManager.unregisterListener(this);
        super.onDestroy();

        //mFile.flushToLog();

        try {
            writer.close();
            output.getFD().sync();
            output.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void recordAccelData(float x, float y, float z, Long tsLong){
        String ts = tsLong.toString();
        String accelLine = ts+", "+Float.toString(x)+", "+Float.toString(y)+", "+Float.toString(z)+"\n";
        try {
            writer.write(accelLine);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}