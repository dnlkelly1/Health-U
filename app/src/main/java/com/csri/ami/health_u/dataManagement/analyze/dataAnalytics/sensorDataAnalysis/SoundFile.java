package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis;

import com.csri.ami.health_u.dataManagement.record.sound.SoundFeatures;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Manages loading of data from sound features file (created by SoundSensor)
 * Allows loading of file in batches to reduce memory burden
 * Created by daniel on 02/10/2015.
 */
public class SoundFile
{
    //Scanner loader;
    BufferedReader reader;
    ArrayList<SoundFeatures> data;
    double previousTime = 0;

    //int iter_fieldCount_sensor;
    int iter_frameIndex = 1;
    double iter_previousTime = 0;
    double iter_offset = -1;

    public SoundFile()
    {

    }

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

        float a = 0.02f;
        float b = 0.2f;

        iter_frameIndex = 1;
        iter_offset = -1;
        iter_previousTime = 0;

        data = new ArrayList<SoundFeatures>();
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


    private long LoadFrame_Iterative_size;
    private boolean LoadFrame_Iterative_newFrameAvailable;

    /**
     * Loads next window of sound fetures from stored sound file
     * Each iteration
     * @return SoundFeatures frame summarizing sound data for current window
     */
    public SoundFeatures LoadFrame_Iterative()
    {
        boolean scaleTime = true;
        SoundFeatures frame = null;
        LoadFrame_Iterative_newFrameAvailable = false;
        LoadFrame_Iterative_size = 0;
        try
        {
            if (reader != null)
            {
                String line = reader.readLine();

                if (line != null)//loader.hasNextLine())
                {
                    LoadFrame_Iterative_newFrameAvailable = true;

                    Iterable<String> s = Splitter.on(',').split(line);
                    String[] line_split = Iterables.toArray(s,String.class);
                    String[] currentData_sensor = new String[line_split.length];
                    String raw = line;
                    for (int i = 0; i < line_split.length; i++) {
                        currentData_sensor[i] = line_split[i];
                    }

                    int lineSize = raw.length();// *sizeof(char);

                    SoundFeatures f = new SoundFeatures(currentData_sensor, scaleTime);

                    frame = f;

                    iter_frameIndex++;

                    iter_previousTime = f.timestamp;

                    LoadFrame_Iterative_size = lineSize;

                }
            }
        }
        catch (IOException ex){}
        return frame;
    }
}
