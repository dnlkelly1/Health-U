package com.csri.ami.health_u.dataManagement.analyze.graphing;

import android.os.Environment;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.MotionFileProcessor;
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
public class MotionGraphValue extends GraphValue
{
    static double maxVar = 9;
    static double minVar = 0;

    public MotionGraphValue(String line)
    {
        Iterable<String> s = Splitter.on(',').split(line);
        String[] line_split = Iterables.toArray(s, String.class);

        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

        try {
            time = formatter.parseDateTime(line_split[0]);

            timeSlotIndex = (int) (Math.round(Double.parseDouble(line_split[1]) * (double) MotionFileProcessor.SAMPLES_PER_DAY));
            Double percentageOfTimeSlotTimeActive = Double.parseDouble(line_split[line_split.length-6]);
            Double avg_varianceDuringActiveTIme = Double.parseDouble(line_split[2]);




            double intensity=0;
            if(percentageOfTimeSlotTimeActive > 0 && avg_varianceDuringActiveTIme != -9999)
            {
                intensity = avg_varianceDuringActiveTIme / (double) maxVar;
            }
            graph_value = (percentageOfTimeSlotTimeActive * intensity) * 100;
            if(graph_value > 100)
            {
                graph_value = 100;
            }

            summary_value = percentageOfTimeSlotTimeActive ;
        }
        catch (Exception ex)
        {
            timeSlotIndex = -1;
        }

    }

    public MotionGraphValue(int index,double v)
    {
        timeSlotIndex = index;
        graph_value = v;
        summary_value = v;

    }

    public static double CalculateDayScore(GraphValue[] day)
    {
        double minutesPerTimeslot = MotionFileProcessor.SECONDS_PER_TIMESLOT / 60;
        double totalMinutes =0;
        for(int i=0;i<day.length;i++)
        {
            double fraction = (double)day[i].summary_value;
            double curent = (double)(fraction * minutesPerTimeslot);
            totalMinutes += curent;
        }
        return totalMinutes;
    }



    public static MotionGraphValue[] Load()
    {
        ArrayList<String> data = null;// = new ArrayList<String>();

        File f = Environment.getExternalStorageDirectory();
        String fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER;

        File fileToRead = new File(fullfilename, MotionFileProcessor.MOTION_FEATURES_SAVE_FILE_NAME_FOR_GRAPHS);
        if(fileToRead.exists())
        {
            data = new ArrayList<String>();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(fileToRead));

                String line = reader.readLine();

                while (line != null) {
                    data.add(line);
                    line = reader.readLine();
                }
                reader.close();

            } catch (java.io.IOException ex) {
            }
        }
        //Scanner scanner = new Scanner(fileToRead);

        if(data != null && data.size() > 0)
        {
            ArrayList<MotionGraphValue> values = new ArrayList<MotionGraphValue>();

            for (int i = 0; i < data.size(); i++)
            {
                MotionGraphValue v = new MotionGraphValue(data.get(i));
                if(v.timeSlotIndex != -1)
                {
                    values.add(v);
                }
            }

            return values.toArray(new MotionGraphValue[values.size()]);
        }
        else
        {
            return null;
        }
    }
}
