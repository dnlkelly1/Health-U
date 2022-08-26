package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques;

import org.joda.time.DateTime;

/**
 * Created by daniel on 29/09/2015.
 */
public class TimeSyncTable
{
    double[] accuratePoints;
    double[] rawPoints;
    private int previousIndex = 0;


    public TimeSyncTable(double[] AccuractPoints, double[] RawPoints)
    {
        accuratePoints = AccuractPoints;
        rawPoints = RawPoints;

        previousIndex = 0;
    }


    public static DateTime UnixTimeToDateTime(double time)
    {
        DateTime timeStart = new DateTime((long)(time * 1000));
        return timeStart;
    }

    public static double DateTimeToUnixTime(DateTime time)
    {
        return time.getMillis() / 1000;
    }





    boolean lastLookupFound = false;
    private int LookupPhoneTime(double time)
    {
        lastLookupFound = false;
        if (false/*time < rawPoints[0]*/)
        {
            return -1;
        }
        else if ( previousIndex >= rawPoints.length)
        {
            return rawPoints.length - 1;
        }
        else
        {
            int index = previousIndex;
            boolean f=false;
            for (int i = previousIndex; i < rawPoints.length - 1; i++)
            {
                if (time >= rawPoints[i] && time < rawPoints[i + 1])
                {
                    index = i;
                    f = true;
                    break;
                }
            }
            lastLookupFound = f;

            return index;
        }
    }
}
