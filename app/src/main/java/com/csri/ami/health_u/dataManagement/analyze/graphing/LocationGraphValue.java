package com.csri.ami.health_u.dataManagement.analyze.graphing;

import android.os.Environment;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.LocationAnalyser;
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
public class LocationGraphValue extends GraphValue
{
    private static double MAX_VALUE = 60;
    private static double LOG_MAX_VALUE = 8;

    private static double MIN_UPPERSCALE = 1;


    public LocationGraphValue(String t,String data,String index,boolean scale,double max,double min)
    {


        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy");

        try {
            time = formatter.parseDateTime(t);
            timeSlotIndex = Integer.parseInt(index) +1;
            double temp = Double.parseDouble(data);
            raw_value = temp;
            if(scale) {
                graph_value = LogScale(temp);//(temp / MAX_VALUE) * 100;

            }
            else
            {
                double MaxVal = Math.max(MIN_UPPERSCALE,max);

                double perc = temp / MaxVal;

                graph_value = perc * 100;
            }
            double MaxVal = Math.max(MIN_UPPERSCALE,max);
            double logMax = LogScale(MaxVal);
            summary_value = graph_value / logMax;
        }
        catch (Exception ex)
        {
            timeSlotIndex = -1;
        }
    }

    public static double CalculateDayScore(GraphValue[] day)
    {
        double minutesPerTimeslot = MotionFileProcessor.SECONDS_PER_TIMESLOT / 60;
        double totalChange =0;
        for(int i=1;i<day.length;i++)
        {
            double current =  Math.abs(day[i].summary_value - day[i-1].summary_value);
            totalChange += current;
        }
        totalChange = totalChange / MotionFileProcessor.SAMPLES_PER_DAY;
        totalChange /= MAX_VALUE;

        double totalAvg =0;
        for(int i=0;i<day.length;i++)
        {
            double current = day[i].summary_value;// Math.abs(day[i].value - day[i-1].value);
            totalAvg += current;
        }
        totalAvg = totalAvg / MotionFileProcessor.SAMPLES_PER_DAY;
        //totalAvg /= 10;
        return (totalAvg /* totalChange*/) * 100;
        //return (totalChange / MAX_VALUE) * 100;
    }


    private static double LogScale(double val)
    {
        if(val > 0)
        {

            //double realValu = val ;/// MAX_VALUE;

            //double X_axis_start = 0.00000000000000001;// 0.0000000000000000001;
            //double X_axis_end =1;
            double mult = 1000;

            double start = 0.00000000000000000000000000000000001;// 0.00000000000000000001;
            double real_valueStart = Math.log(start);
            double real_valueEnd = Math.log(LOG_MAX_VALUE);

            //double dist_X = X_axis_end - X_axis_start;
            double dist_real = real_valueEnd - real_valueStart;

            double l = Math.log(val + start);
            double realValuePercent = (/*realValu*/l - real_valueStart) / dist_real;

            //double X_distanceFromStart = realValuePercent * dist_X;
            //double X_vale = X_axis_start + X_distanceFromStart;

            //double offsetX = Math.log(X_axis_start);
            //double offsetXend = Math.log(X_axis_end);
            //double xScale = offsetXend - offsetX;

            //double val_x = Math.log(X_vale);

            //double val_offsetted = (val_x - offsetX) / xScale;

            return  realValuePercent * 100;
        }
        else
        {
            return 0;
        }
    }

    public static LocationGraphValue[] Load()
    {
        return Load(LocationAnalyser.LOCATION_FEATURES_SAVE_FILE_NAME_FOR_GRAPHS,true);
    }


    public static LocationGraphValue[] Load(String file,boolean scale)
    {
        ArrayList<String> data = null;//new ArrayList<String>();

        File f = Environment.getExternalStorageDirectory();
        String fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER;

        File fileToRead = new File(fullfilename, file);
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

            } catch (java.io.IOException ex) {
            }
            //Scanner scanner = new Scanner(fileToRead);
        }

        ArrayList<String[]> fileData = new ArrayList<String[]>();

        if(data != null && data.size() > 0)
        {
            ArrayList<LocationGraphValue> values = new ArrayList<LocationGraphValue>();

            double max = Double.MIN_VALUE;
            double min = Double.MAX_VALUE;
            for (int i = 0; i < data.size(); i++)
            {
                String line = data.get(i);
                Iterable<String> s = Splitter.on(',').split(line);
                String[] line_split = Iterables.toArray(s, String.class);
                fileData.add(line_split);

                double val = Double.parseDouble(line_split[2]);
                if(val > max)
                {
                    max = val;
                }
                if(val < min)
                {
                    min = val;
                }
//                if(line_split.length >= 3)
//                {
//                    LocationGraphValue v = new LocationGraphValue(line_split[0], line_split[2], line_split[1],scale);
//                    if(v.timeSlotIndex != -1)
//                    {
//                        values.add(v);
//                    }
//                }

            }

            for(int i=0;i<fileData.size();i++)
            {
                String[] current = fileData.get(i);
                LocationGraphValue v = new LocationGraphValue(current[0], current[2], current[1],scale,max,min);
                if(v.timeSlotIndex != -1)
                {
                    values.add(v);
                }
            }

            return values.toArray(new LocationGraphValue[values.size()]);
        }
        else
        {
            return null;
        }


    }


}
