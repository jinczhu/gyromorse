package com.jc.gyromorse;

/**
 * jincao：copy from https://github.com/SiamH/Sensor-Data
 */
import android.os.CountDownTimer;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by hasan on 2016-06-17.
 */
public class FileSave {
    private File mFile = null;
    private  String mLog = "";
    private  boolean mbFlash = true;
    private int mCount = 0;



    public FileSave(String FileName, boolean bFlush)
    {
        mbFlash = bFlush;

        try {
            mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FileName);
            if(mFile.exists())
                mFile.delete();

            mFile.createNewFile();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void appendLog(String text) {
        mLog = mLog + text;
        mLog = mLog + System.getProperty("line.separator");
        if (mbFlash || mCount == 1000) {
            mCount = 0;
            flushToLog();
        }

        if (!mbFlash)
            mCount++;
    }

    public void flushToLog()
    {
        if (mLog == "")
            return;

        FileOutputStream outputStream;
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(mFile, true));
            buf.append(mLog);
            buf.newLine();
            buf.close();
            mLog = "";
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}