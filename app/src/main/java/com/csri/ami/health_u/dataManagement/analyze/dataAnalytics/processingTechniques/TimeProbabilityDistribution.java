package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Minutes;

/**
 * Create a probability distribution model for time used to model times in specific locations
 * Created by daniel on 06/10/2015.
 */
public class TimeProbabilityDistribution
{
    public double[] weekDayProbability;
    public double[] weekEndProbability;
    public double[] probability;

    double weekEndRatio;
    double weekDayRatio;

    public TimeProbabilityDistribution(int timeslots,double[] times)
    {
        GenerateTimeProbabilityDistribution(timeslots, times);
    }


    public String ToString()
    {
        String line ="";

        for(int i=0;i<probability.length;i++)
        {
            line += probability[i] + ",";
        }

        for (int i = 0; i < weekDayProbability.length; i++)
        {
            line += weekDayProbability[i] + ",";
        }

        for (int i = 0; i < weekEndProbability.length; i++)
        {
            line += weekEndProbability[i] + ",";
        }

        line += weekDayRatio + "," + weekEndRatio + ",";

        return line;
    }

    /**
     * Given a time, and set of previous times, compute probability time belongs to distribution of previous times
     * @param time
     * @return Probability 0 <= p <= 1
     */
    public double Probability(double time)
    {
        DateTime datetime = TimeSyncTable.UnixTimeToDateTime(time);
        DateTime startDay = datetime.withTimeAtStartOfDay();

        int minutesElapsed = Minutes.minutesBetween(startDay,datetime).getMinutes();

        double numberSlots = weekDayProbability.length;
        double minutesPerTime = (60 * 24) / numberSlots;

        int timeslot = (int)(minutesElapsed / minutesPerTime);
        if(timeslot < 0)
        {
            timeslot=0;
        }
        else if(timeslot >= weekDayProbability.length)
        {
            timeslot = weekDayProbability.length-1;
        }
        if (datetime.getDayOfWeek()== DateTimeConstants.SUNDAY || datetime.getDayOfWeek()== DateTimeConstants.SATURDAY)
        {
            return weekEndProbability[timeslot];
        }
        else
        {
            return weekDayProbability[timeslot];
        }
    }


    public void GenerateTimeProbabilityDistribution(int timeslots, double[] times)
    {
        double MinutesInDay = 60 * 24;
        double MinutesPerTimeSlot = MinutesInDay / (double)timeslots;
        weekDayProbability = new double[timeslots];
        weekEndProbability = new double[timeslots];
        probability = new double[timeslots];
        double weekendCount = 0;
        double weekdayCount = 0;
        double Length = times.length;
        for (int i = 0; i < times.length; i++)
        {
            //DataElement current = Elements[i].GetElement();
            DateTime time = TimeSyncTable.UnixTimeToDateTime(times[i]);
            DateTime startDay = time.withTimeAtStartOfDay();

            int minutesElapsed = Minutes.minutesBetween(startDay,time).getMinutes();
            int timeslotNumber = (int)(minutesElapsed / MinutesPerTimeSlot);


            probability[timeslotNumber]+= 1 / Length;

            if (time.getDayOfWeek()== DateTimeConstants.SUNDAY || time.getDayOfWeek()== DateTimeConstants.SATURDAY)
            {
                weekEndProbability[timeslotNumber] += 1;
                weekendCount++;
            }
            else
            {
                weekDayProbability[timeslotNumber] += 1;
                weekdayCount++;
            }
        }

        for (int i = 0; i < weekDayProbability.length; i++)
        {
            if (weekdayCount > 0)
            {
                weekDayProbability[i] = weekDayProbability[i] / weekdayCount;
            }
            if (weekendCount > 0)
            {
                weekEndProbability[i] = weekEndProbability[i] / weekendCount;
            }
        }

        weekdayCount = weekdayCount / 5;
        weekendCount = weekendCount / 2;
        weekDayRatio = weekdayCount / (weekdayCount + weekendCount);
        weekEndRatio = weekendCount / (weekdayCount + weekendCount);
        //return Probs;
    }
}
