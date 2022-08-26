package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis;

import com.csri.ami.health_u.dataManagement.record.Filter;
import com.csri.ami.health_u.dataManagement.record.motion.MadgwickAHRS;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Class to manage loading of raw motion data recorded by MotionSensor
 * File is usually too large to load into memory at one time
 * Therefore process must be done in batched. This class allows loading of the sensor in batches
 * Created by daniel on 30/09/2015.
 */
public class MotionFile {
    protected ArrayList<AndroidFrame> data;
    public ArrayList<AndroidFrame> Frames;


    private ArrayList<AndroidFrame> StandingData;
    private AndroidFrame avgStand;
    public String FileName = "";

    protected static int LABEL_NAME_LENGTH_NOPOSITION = 7;
    protected static int LABEL_NAME_LENGTH = 9;

    protected static int WINDOW_INCREMENT = 2;
    protected static int WINDOW_AVERAGE_SIZE = 32;

    protected int LabelPadding = 32;

    boolean useStand = false;

    protected int FPS = 0;

    public MotionFile()
    {

    }

    /// //////////////////ITERATIVE LOADER/////////////////////////////////
    //Scanner loader;
    BufferedReader reader;
    Filter iter_filter;
    Filter iter_bandpass;
    MadgwickAHRS madgwick;
    double previousTime = 0;

    //int iter_fieldCount_sensor;
    int iter_frameIndex = 1;
    double iter_previousTime = 0;
    double iter_offset = -1;

    public void Init_LoadRawData_Iterative(String fileprefix)
    {
        File f = new File(fileprefix);
        try
        {
            //loader = new Scanner(f);
            reader = new BufferedReader(new FileReader(f));
        }
        catch (FileNotFoundException ex)
        {

        }

        iter_bandpass = new Filter();
        iter_filter = new Filter();
        float a = 0.02f;
        float b = 0.2f;
        madgwick = new MadgwickAHRS(a,b);
        //iter_fieldCount_sensor = cvs_loader.FieldCount;

        iter_frameIndex = 1;
        iter_offset = -1;
        iter_previousTime = 0;
        data = new ArrayList<AndroidFrame>();
    }

    public void Close_File()
    {
        if(reader != null)
        {
            try {
                reader.close();
            }catch (IOException ex){}

        }
    }

    public ArrayList<AndroidFrame> LoadData_Iterative_data;
    public long LoadData_Iterative_totalSize;
    public void LoadData_Iterative(int maxFrames, String logFile)
    {
        LoadData_Iterative_totalSize = 0;
        while (data.size() < maxFrames)
        {
            boolean newFrameAvailable = false;

            long size = 0;
            AndroidFrame f = LoadFrame_Iterative( logFile);
            LoadData_Iterative_totalSize += LoadFrame_Iterative_size;
            if (!LoadFrame_Iterative_newFrameAvailable)
            {
                break;
            }
            else if (f != null)
            {
                data.add(f);
            }
        }

        LoadData_Iterative_data = data;
    }

    private long LoadFrame_Iterative_size;
    private boolean LoadFrame_Iterative_newFrameAvailable;
    public AndroidFrame LoadFrame_Iterative( String logfile)
    {
        AndroidFrame frame = null;
        LoadFrame_Iterative_newFrameAvailable = false;
        LoadFrame_Iterative_size = 0;
        if (reader != null)
        {
            try
            {
                String line = reader.readLine();
                if (line != null)
                {
                    LoadFrame_Iterative_newFrameAvailable = true;


                    Iterable<String> s = Splitter.on(',').split(line);
                    String[] line_split = Iterables.toArray(s, String.class);

                    double[] currentData_sensor = new double[line_split.length];
                    String raw = line;
                    boolean valid = true;
                    for (int i = 0; i < currentData_sensor.length; i++)
                    {
                        try {
                            currentData_sensor[i] = Double.parseDouble(line_split[i]);
                        }
                        catch (NumberFormatException nfe)
                        {
                            valid = false;
                            break;
                        }

                    }

                    if(valid) {
                        int lineSize = raw.length();
                        double currentTime = (currentData_sensor[1] / 1000);
                        if (iter_offset == -1) {
                            iter_offset = currentTime - currentData_sensor[0];
                            iter_previousTime = iter_offset;
                        } else if (Math.abs(currentTime - iter_previousTime) > 2) {
                            iter_offset = currentTime - currentData_sensor[0];
                            iter_previousTime = iter_offset;
                        }


                        AndroidFrame f = new AndroidFrame("", currentData_sensor, iter_offset, madgwick, previousTime);
                        previousTime = f.EventTimeStamp();
                        f.Accel_GF().LowPass_Mag = iter_filter.ApplyLowPassFilter(f.Accel_GF().Magnitude, 2.85);
                        f.Accel_GF().BandPass_Mag = iter_bandpass.BandPassFilter(f.Accel_GF().Magnitude, 1.6, 4.5);

                        frame = f;


                        iter_frameIndex++;

                        iter_previousTime = f.EventTimeStamp();

                        LoadFrame_Iterative_size = lineSize;
                    }


                }
            }
            catch(IOException ex){}
        }
        return frame;
    }
}