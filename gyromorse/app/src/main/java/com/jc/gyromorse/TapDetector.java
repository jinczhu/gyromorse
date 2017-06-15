package com.jc.gyromorse;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by jc on 6/12/17.
 */

public class TapDetector implements SensorEventListener {

 //   state
private float gravity[]={0f,0f,0f};
    private float ALPHA=0.15f;

    private long bufflength;

    private  TListener mTListener;

    long curTime;
    long diffTime;
    long lastUpdate= System.nanoTime();

    private  LinkedList<TapDetector.EnergySamplePair> mEnergySamplesList;
    private  LinkedList<TapDetector.InputSamplePair> mInputSamplesList;
    private  LinkedList<TapDetector.OutputSamplePair> mOutputSamplesList;

    private long mFTapTime;
    private long mSTapTime;

    private TapState mstate;

    //envolope parameter
    private long mTStime;

    private float maxMsq;
    private float taplowMsq;
    private float mEnergy;

    private long gaptime;

    private float NoisEPerSample;

    private long evwidth;

    private float tapbgth_this;
    private float evwidth_this;

    private float NoisEPerSampl;

    //private final Handler mHandler;
    //private final Map<TapDetector.TListener, Handler> mListenerMap = new HashMap<>();
    private final SensorManager mSensorManager;


    //private SensorManager sensorManager;

    public TapDetector(TapDetector.TListener tListener, SensorManager sensorManager)
    {
        mSensorManager = sensorManager;

        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //HandlerThread thread = new HandlerThread("TapDetector");
        //thread.start();
        //mHandler = new Handler(thread.getLooper());

        mSensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        float maxRange = accelerometer.getMaximumRange();

        mTListener=tListener;

        maxMsq   = 3 * maxRange * maxRange;


        mInputSamplesList = new LinkedList<>();
        mOutputSamplesList = new LinkedList<>();
        mEnergySamplesList = new LinkedList<>();


        mstate=TapState.NOTAP;
        bufflength = 100 * 1000 * 1000;

        gaptime =300*1000*1000;
        gaptime =80*1000*1000;


        evwidth = 60 * 1000 * 1000;
        taplowMsq = 1f;
        NoisEPerSample =1f;

    }

  /*  void addListener(TapDetector.TListener listener, Handler handler) {
        synchronized (mListenerMap) {
            mListenerMap.put(listener, handler);
        }
    }

    *//**
     * Add a listener on the current thread.
     *//*
    public void addListener(TapDetector.TListener listener) {
        addListener(listener, new Handler());
    }*/

 /*   public void start() {
        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(
                this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST, mHandler);

    }
*/
    public void onAccuracyChanged(Sensor sensor,int accuracy){

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //this.curTime = System.currentTimeMillis();
        this.curTime =System.nanoTime();
        diffTime = (curTime - this.lastUpdate);
        this.lastUpdate = curTime ;

        //Log.e("tapdetector", "here 2here");


        // assign directions
        float x=event.values[0]*5f;
        float y=event.values[1]*5f;
        float z=event.values[2]*5f;

        float[] values=highPass(x,y,z);


        mOutputSamplesList.addLast(new OutputSamplePair(curTime, values));
        while (mOutputSamplesList.getFirst().mTime <= curTime - bufflength) {
            mOutputSamplesList.removeFirst();
        }

       float mMsq=0f;
        for (int i = 0; i < 3; ++i) {
            mMsq += values[i] * values[i];
        }

        mEnergy += mMsq;

       mEnergySamplesList.addLast(new EnergySamplePair(curTime, mMsq));
        while (mEnergySamplesList.getFirst().mTime <= curTime - bufflength) {
            mEnergy -= mEnergySamplesList.getFirst().mValue;
            mEnergySamplesList.removeFirst();
        }

        float yy = taplowMsq;

        float envelope = maxMsq + (yy - maxMsq) * ((float) curTime - mTStime+evwidth/2) / evwidth;



        if (mFTapTime>0 /*&& curTime-mFTapTime>gaptime && (mstate==TapState.NOTAP|| mstate==TapState.NOISE)*/)
        {
            Log.e("tapdetector", String.format("here2  %d %d", gaptime, curTime-mFTapTime));
            if(curTime-mFTapTime>gaptime && curTime-mFTapTime<mgaptime)
            {
            mTListener.onSTap(curTime);
            mFTapTime=0;}
        }

        float NoisE=(float)mEnergySamplesList.size() * NoisEPerSample;

 /*       if (mMsq > taplowMsq) {
            Log.e("tapdetector", String.format("here 2here msg %f, %f， %s, %f", (float)mInputSamplesList.size(), mEnergy, mstate.name(),NoisE));
        }*/

        if (mstate==TapState.NOTAP) {
            //Log.e("tapdetector", String.format("here 2here msg %f", mMsq));

            if (mMsq > taplowMsq) {

                if (mEnergy>NoisE)
                {
                    mstate=TapState.NOISE;
                }else
                {mstate=TapState.TAPBG;
                    mTStime=curTime;

                    tapbgth_this=mMsq;

                }

            }
        }
        else if(mstate==TapState.TAPBG)
        {
            if (mMsq > envelope) {
                mstate=TapState.NOTAP;
            }
            else {
                  if(curTime >evwidth)
                {
                    //tap detected
                    mstate = TapState.NOTAP;


                    updateshsz();

                    Log.e("tapdetector", String.format("here  %d", mFTapTime));


                    if (mFTapTime == 0) {
                        mFTapTime = curTime;
                    } else {
                        mSTapTime = curTime;
                        mTListener.onDTap(curTime);
                        mFTapTime=0;
                    }
                }
                if(mMsq<tapbgth_this){
                    //evwidth_this=curTime-mTStime;
                }
            }
        }else if(mstate==TapState.NOISE)
        {
            if (mEnergy<NoisE)
            {
                mstate=TapState.NOTAP;
            }


        }


    }

    private void updateshsz(){

        //evwidth;
        float a=0.1f;
        taplowMsq = a * tapbgth_this+ (1-a) * taplowMsq;

        NoisEPerSample = Math.min(tapbgth_this*0.7f, NoisEPerSample);
    }

  /*  private float[] calibration(long timeStamp,  double values[]) {
        sg[0] += values[0];
        sg[1] += values[1];
        sg[2] += values[2];
        ++count;

        sg[0] /= count;
        sg[1] /= count;
        sg[2] /= count;


    }*/

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

    private class InputSamplePair {
        public long mTime;

        public float mValues[];

        public InputSamplePair(long time, float values[]) {
            mTime = time;
            mValues = values;
        }
    }

    private class OutputSamplePair {
        public long mTime;

        public float mValues[];

        public OutputSamplePair(long time, float values[]) {
            mTime = time;
            mValues = values;
        }
    }

    private enum TapState {
        NOTAP,
        NOISE,
        TAPBG
    }

    private class envelop {
        public float peak;
        public float peakt;


    }

    private class EnergySamplePair {
        public long mTime;

        public float mValue;

        public EnergySamplePair(long time, float value) {
            mTime = time;
            mValue = value;
        }
    }

    public interface TListener {
        // Invoked when tap occurs
        void onSTap(long timestampNanos);

        // Invoked on double-tap
        void onDTap(long timestampNanos);
    }
}
