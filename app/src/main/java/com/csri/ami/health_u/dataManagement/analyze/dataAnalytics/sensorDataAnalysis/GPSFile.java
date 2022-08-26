package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis;

import com.csri.ami.health_u.dataManagement.record.GPS;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Class to load GPS data from file
 * Created by daniel on 06/10/2015.
 */
public class GPSFile
{
    GPS[] GPSData;

    public GPSFile(String file)
    {
        Load(file);
    }

    public void Load(String file)
    {
        BufferedReader reader;

        ArrayList<GPS> data_raw = new ArrayList<GPS>();

        File f = new File(file);
        try
        {
            reader = new BufferedReader(new FileReader(f));

            String line = reader.readLine();
            while (line != null)
            {
                Iterable<String> s = Splitter.on(',').split(line);
                String[] line_split = Iterables.toArray(s, String.class);

                GPS current = GPS.Parse(line_split, true);
                data_raw.add(current);
                line = reader.readLine();

            }

            GPSData = data_raw.toArray(new GPS[data_raw.size()]);

        }
        catch(FileNotFoundException ex){}
        catch (IOException exIO){}


    }

    public static GPS[] GetWindow(GPS[] data, double start, double end, int startIndex)
    {
        ArrayList<GPS> window = new ArrayList<GPS>();
        boolean windowEnded = false;

        if (startIndex < 0)
        {
            startIndex = 0;
        }

        int index = startIndex;

        while (!windowEnded && index < data.length)
        {
            if (data[index].timestamp >= start && data[index].timestamp <= end)
            {
                window.add(data[index]);

            }
            else if (data[index].timestamp > end)
            {
                windowEnded = true;
            }
            index++;
        }

        return window.toArray(new GPS[window.size()]);
    }
}
