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

public class MainActivity extends Activity implements SensorEventListener{

    private SensorManager sensorManager;
    //FileSave mFile = null;


    TextView xCoor; // declare X axis object
    TextView yCoor; // declare Y axis object
    TextView zCoor; // declare Z axis object

    private FileWriter writer;
    private FileOutputStream output;

    private List<AccelerometerClass> accelerometerDataList;
    private AccelerometerClass accelerometerData;

    long curTime;
    long diffTime;
    long lastUpdate= System.currentTimeMillis();


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

        //copy from  //adding accelerometer data list values for the starting
        this.accelerometerDataList = new ArrayList<AccelerometerClass>();

        //adding accelerometer data list values for the starting
        this.accelerometerDataList.add(new AccelerometerClass(0, 0, 0, 0, 0));
        /////////////////////////////////////////////////////////////////////////////

        //mFile = new FileSave("morse.txt", false);


        /*try {
            File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"morsex.txt");
            output = new FileOutputStream(logFile);
            writer = new FileWriter(output.getFD());
        }catch(Exception e){
            e.printStackTrace();
        }*/
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


            //String Content = currentDateTimeString  + " " + event.values[0] + " " + event.values[1] + " " + event.values[2];
            //mFile.appendLog(Content);

            //long tsLong = System.currentTimeMillis()/1000;
            //recordAccelData(x, y, z, tsLong);

            //calculating time lapse

            this.accelerometerData = new AccelerometerClass();
            this.accelerometerData.setxAxisValue(event.values[0]);
            this.accelerometerData.setyAxisValue(event.values[1]);
            this.accelerometerData.setzAxisValue(event.values[2]);
            this.accelerometerData.setAccuracy(event.accuracy);

            this.curTime = System.currentTimeMillis();
            diffTime = (curTime - this.lastUpdate);
            this.lastUpdate = curTime ;

            //setting time lapse between consecutive datapoints
            this.accelerometerData.setTimestamp(diffTime);

            //adding the class to the list of accelerometer data points
            this.accelerometerDataList.add(accelerometerData);


        }
    }

    //method called when application is on pause
    @Override
    protected void onPause() {
        super.onPause();

        //unregistering sensor when application is on pause
        //this is done to save battery
        //mSensorManager.unregisterListener(this);

        //saving data onto a file
        //File myFile = new File(Environment.getExternalStorageDirectory()+"/Documents/accelerometerData.txt");
        File myFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"gmorse.txt");

        try {

            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);

            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            for(AccelerometerClass accel: this.accelerometerDataList) {
                myOutWriter.append(String.valueOf(accel.getxAxisValue()));
                myOutWriter.append('\t');
                myOutWriter.append(String.valueOf(accel.getyAxisValue()));
                myOutWriter.append('\t');
                myOutWriter.append(String.valueOf(accel.getzAxisValue()));
                myOutWriter.append('\t');
                myOutWriter.append(String.valueOf(accel.getTimestamp()));
                myOutWriter.append('\n');
            }
            myOutWriter.close();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        sensorManager.unregisterListener(this);
        super.onDestroy();

        //mFile.flushToLog();

        /*try {
            writer.close();
            output.getFD().sync();
            output.close();
        } catch (Exception e){
            e.printStackTrace();
        }*/
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