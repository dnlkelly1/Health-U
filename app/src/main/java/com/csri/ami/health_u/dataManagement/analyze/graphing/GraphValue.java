package com.csri.ami.health_u.dataManagement.analyze.graphing;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.ArrayList;

/**
 * Created by daniel on 14/10/2015.
 */
public abstract class GraphValue
{
    public DateTime time;

    public int timeSlotIndex;
    public double graph_value;
    public double summary_value;
    public double raw_value;

    public static GraphValue[] RemoveDuplicates(GraphValue[] data)
    {
        ArrayList<GraphValue> uniques = new ArrayList<>();

        for(int i=data.length-1;i>=0;i--)
        {
            boolean exists = Exists(uniques,data[i]);
            if(!exists)
            {
                uniques.add(data[i]);
            }
        }

        GraphValue[] reversed = new GraphValue[uniques.size()];

        int rIndex = uniques.size()-1;
        for(int i=0;i<uniques.size();i++)
        {
            reversed[i] = uniques.get(rIndex);
            rIndex--;
        }
        return reversed;
    }



    public static boolean Exists(ArrayList<GraphValue> data,GraphValue value)
    {
        boolean exists = false;
        for(int i=0;i<data.size();i++)
        {
            int daysDiff = Days.daysBetween(data.get(i).time.withTimeAtStartOfDay(),value.time.withTimeAtStartOfDay()).getDays();
            if(data.get(i).timeSlotIndex == value.timeSlotIndex && daysDiff ==0)
            {
                exists = true;
                break;
            }
        }
        return exists;
    }




}
