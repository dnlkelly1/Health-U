package com.csri.ami.health_u.dataManagement.analyze.graphing;

import android.os.Environment;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.MotionFileProcessor;
import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.SoundFileProcessor;
import com.csri.ami.health_u.dataManagement.record.SensorRecorder;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Created by daniel on 14/10/2015.
 */
public class SoundGraphValue extends GraphValue
{


    public SoundGraphValue(String line)
    {
        Iterable<String> s = Splitter.on(',').split(line);
        String[] line_split = Iterables.toArray(s, String.class);

        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

        try
        {
            time = formatter.parseDateTime(line_split[0]);
            timeSlotIndex = (int)Math.round(Double.parseDouble(line_split[1]) * (double)MotionFileProcessor.SAMPLES_PER_DAY);
            double voicePerLoud = Double.parseDouble(line_split[3]);
            double loudPerSample = Double.parseDouble(line_split[4]);
            graph_value = (voicePerLoud * loudPerSample) * 100;// Double.parseDouble(line_split[3]);
            summary_value = (voicePerLoud * loudPerSample);
        }
        catch (Exception ex)
        {
            timeSlotIndex = -1;
        }


    }

    public static double CalculateDayScore(GraphValue[] day)
    {
        double minutesPerTimeslot = MotionFileProcessor.SECONDS_PER_TIMESLOT / 60;
        int totalMinutes =0;
        for(int i=0;i<day.length;i++)
        {
            double fraction = (double)day[i].summary_value;
            int curent = (int)(fraction * minutesPerTimeslot);
            totalMinutes += curent;
        }
        return totalMinutes;
    }

    public static SoundGraphValue[] Load()
    {
        ArrayList<String> data = null;// = new ArrayList<String>();

        File f = Environment.getExternalStorageDirectory();
        String fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER;

        File fileToRead = new File(fullfilename, SoundFileProcessor.SOUND_FEATURES_SAVE_FILE_NAME_FOR_GRAPH);
        if(fileToRead.exists())
        {
            data = new ArrayList<String>();
            try
            {
                BufferedReader reader = new BufferedReader(new FileReader(fileToRead));

                String line = reader.readLine();

                while (line != null) {
                    data.add(line);
                    line = reader.readLine();
                }
                reader.close();

            }
            catch (java.io.IOException ex)
            {
                int test1 = 1;
                int test2 = test1 +1;
            }
            //Scanner scanner = new Scanner(fileToRead);
        }

        if(data != null && data.size() > 0)
        {
            ArrayList<SoundGraphValue> values = new ArrayList<SoundGraphValue>();

            for (int i = 0; i < data.size(); i++)
            {
                SoundGraphValue v = new SoundGraphValue(data.get(i));
                if(v.timeSlotIndex != -1)
                {
                    values.add(v);
                }
            }


            return values.toArray(new SoundGraphValue[values.size()]);
        }
        else
        {
            return null;
        }
    }
}
