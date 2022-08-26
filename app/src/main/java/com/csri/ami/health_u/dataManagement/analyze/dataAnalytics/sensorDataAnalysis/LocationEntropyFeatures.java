package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis;

import android.os.Environment;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques.TimeSyncTable;
import com.csri.ami.health_u.dataManagement.record.SensorRecorder;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 * Created by daniel on 06/10/2015.
 */
public class LocationEntropyFeatures
{
    public Double[] AllDays;

    public Double[] WeekDay;
    public Double[] WekkEnd;

    ArrayList<Double[]> hourlyEnt = new ArrayList<Double[]>();
    ArrayList<Integer[]> hourlyEntIndices = new ArrayList<Integer[]>();

    ArrayList<Double> dates = new ArrayList<Double>();


    public LocationEntropyFeatures()
    {
        AllDays = new Double[6];
    }


    public void Write()
    {
        File f = Environment.getExternalStorageDirectory();
        String fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER;

        File fileToWrite = new File(fullfilename + LocationAnalyser.LOCATION_FEATURES_SAVE_FILE_NAME_FOR_UPLOAD);

        File fileToWrite_g = new File(fullfilename + LocationAnalyser.LOCATION_FEATURES_SAVE_FILE_NAME_FOR_GRAPHS);

        try
        {
            FileOutputStream fos = new FileOutputStream(fileToWrite, false);
            OutputStreamWriter osw = new OutputStreamWriter(fos);

            FileOutputStream fos_g = new FileOutputStream(fileToWrite_g, false);
            OutputStreamWriter osw_g = new OutputStreamWriter(fos_g);
            if(hourlyEnt != null && hourlyEnt.size() > 0)
            {
                for (int i = 0; i < hourlyEnt.size(); i++)
                {
                    if (hourlyEnt.get(i) != null && dates.get(i) != null)
                    {
                        Double[] currentDayEnt = hourlyEnt.get(i);
                        Integer[] currentDayIndices = hourlyEntIndices.get(i);

                        DateTime d = TimeSyncTable.UnixTimeToDateTime(dates.get(i));
                        String date = d.toString("dd/MM/yyyy") ;

                        for (int j = 0; j < currentDayEnt.length; j++)
                        {
                            String line = date + "," + currentDayIndices[j] + "," + currentDayEnt[j] + ",";
                            osw.write(line + "\n");
                            osw_g.write(line +"\n");
                        }

                    }
                }
                osw.close();
                osw_g.close();
            }
        }
        catch (FileNotFoundException ex)
        {

        }
        catch (IOException ioex)
        {

        }
    }


}
