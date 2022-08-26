package com.csri.ami.health_u.dataManagement.analyze.graphing;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * Created by daniel on 20/10/2015.
 */
public class TimeValueFormatter implements ValueFormatter {



    public TimeValueFormatter()
    {

    }

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        // write your logic here

        
        if(value > 0 && value < 60)//0-60 seconds
        {
            return Integer.toString(Math.round(value)) + "s";
        }
        else if(value >= 60 && value < 60*60)//1 min to 1 hour
        {
            int minutes = (int)(value / 60);
            return Integer.toString(minutes) + "m";
        }
        else if(value > 60 * 60)
        {

            int secondsPerHour = 60*60;

            int numHours = (int)(value / secondsPerHour);
            int remainingSeconds = Math.round(value % secondsPerHour);
            int minutes = Math.round(remainingSeconds / 60);
            return Integer.toString(numHours) + "h" + Integer.toString(minutes) + "m";
        }
        else
        {
            return "";
        }
    }
}
