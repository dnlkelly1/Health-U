package com.csri.ami.health_u.dataManagement.analyze.graphing;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.ArrayList;

/**
 * Created by daniel on 16/10/2015.
 */
public class GraphValueDay
{
    public GraphValue[] motion;
    public GraphValue[] sound;
    public GraphValue[] location;

    public double motion_dayScore;
    public double sound_dayScore;
    public double location_dayScore;

    public DateTime date;

    public GraphValueDay(DateTime Date,GraphValue[] Motion,GraphValue[] Sound,GraphValue[] Location)
    {
        date = Date;
        motion = Motion;
        sound = Sound;
        location = Location;

        motion_dayScore = MotionGraphValue.CalculateDayScore(motion);
        sound_dayScore = SoundGraphValue.CalculateDayScore(sound);
        location_dayScore = LocationGraphValue.CalculateDayScore(location);

    }

    public static double MaxValue(GraphValueDay[] days)
    {
        double max=0;
        if(days != null)
        {
            if(days.length > 0) {
                max = Double.MIN_VALUE;
                for (int i = 0; i < days.length; i++)
                {
                    int locationTimeslotEnd = -1;
                    if(days[i].location != null && days[i].location.length > 0)
                    {
                        locationTimeslotEnd = days[i].location[days[i].location.length-1].timeSlotIndex;
                    }
                    int motionTimeslotEnd =-1;
                    if(days[i].motion != null && days[i].motion.length > 0)
                    {
                        motionTimeslotEnd = days[i].motion[days[i].motion.length-1].timeSlotIndex;
                    }
                    int soundTimeslotEnd =-1;
                    if(days[i].sound != null && days[i].sound.length > 0)
                    {
                        soundTimeslotEnd = days[i].sound[days[i].sound.length-1].timeSlotIndex;
                    }
                    int maxCurrent1 = Math.max(locationTimeslotEnd,soundTimeslotEnd);
                    int maxCurrent = Math.max(maxCurrent1, motionTimeslotEnd);
                    for (int j = 0; j < maxCurrent; j++) {
                        int soundIndex = GetIndex(j, days[i].sound);
                        int locactionIndex = GetIndex(j, days[i].location);
                        int motionIndex = GetIndex(j, days[i].motion);

                        double sum = 0;
                        if (soundIndex != -1) {
                            sum += days[i].sound[soundIndex].graph_value;
                        }
                        if (locactionIndex != -1) {
                            sum += days[i].location[locactionIndex].graph_value;
                        }
                        if (motionIndex != -1) {
                            sum += days[i].motion[motionIndex].graph_value;
                        }

                        if (sum > max) {
                            max = sum;
                        }
                    }
                }
            }

        }
        return max;
    }

    public static int GetIndex(int desiredTimeslot,GraphValue[] slots)
    {
        int index = -1;
        for(int i=0;i<slots.length;i++)
        {
            if(slots[i].timeSlotIndex == desiredTimeslot)
            {
                index = i;
                break;
            }
        }
        return index;
    }

    public static GraphValue[][] GroupByDay(GraphValue[] location)
    {
        if(location != null)
        {
            DateTime start = location[0].time;
            DateTime end = location[location.length - 1].time;

            int numDays = Days.daysBetween(start, end).getDays();

            GraphValue[][] days = new GraphValue[numDays + 1][];//

            DateTime currentDay = start.withTimeAtStartOfDay();
            for (int i = 0; i < days.length; i++) {

                GraphValue[] locationRow = GetRowByDate(currentDay, location);

                if (locationRow != null)
                {
                    days[i] = locationRow;
                } else
                {
                    days[i] = null;
                }

                currentDay = currentDay.plusDays(1);
            }

            ArrayList<GraphValue[]> days_nullRemoved = new ArrayList<GraphValue[]>();
            for (int i = 0; i < days.length;i++) {
                if (days[i] != null)
                {
                    days_nullRemoved.add(days[i]);

                }
            }

            return days_nullRemoved.toArray(new GraphValue[days_nullRemoved.size()][]);



        }
        else
        {
            return null;
        }


    }


    public static GraphValueDay[] GroupByDay(DateTime start,DateTime end,GraphValue[] motion,GraphValue[] sound,GraphValue[] location)
    {
        if(motion != null || sound != null || location != null)
        {
            int numDays = Days.daysBetween(start, end).getDays();

            GraphValueDay[] days = new GraphValueDay[numDays + 1];//

            DateTime currentDay = start.withTimeAtStartOfDay();
            for (int i = 0; i < days.length; i++) {
                GraphValue[] motionRow = GetRowByDate(currentDay, motion);
                GraphValue[] soundRow = GetRowByDate(currentDay, sound);
                GraphValue[] locationRow = GetRowByDate(currentDay, location);

                if (motionRow != null || soundRow != null || locationRow != null) {
                    days[i] = new GraphValueDay(currentDay, motionRow, soundRow, locationRow);
                } else {
                    days[i] = null;
                }

                currentDay = currentDay.plusDays(1);
            }

            ArrayList<GraphValueDay> days_nullRemoved = new ArrayList<GraphValueDay>();
            for (int i = days.length - 1; i >= 0; i--) {
                if (days[i] != null) {
                    days_nullRemoved.add(days[i]);

                }
            }

            return days_nullRemoved.toArray(new GraphValueDay[days_nullRemoved.size()]);
        }
        else
        {
            return null;
        }


    }

    public static GraphValue[] GetRowByDate(DateTime date,GraphValue[] data)
    {
        ArrayList<GraphValue> row = new ArrayList<GraphValue>();
       // GraphValue[] row=null;
        if(data != null)
        {
            for (int i = 0; i < data.length; i++)
            {
                int numDays = Days.daysBetween(date, data[i].time.withTimeAtStartOfDay()).getDays();
                if (numDays == 0)
                {
                    row.add( data[i] );

                    //break;
                }
            }
        }
        GraphValue[] current = row.toArray(new GraphValue[row.size()]);
        GraphValue[] removed = GraphValue.RemoveDuplicates(current);

        GraphValue[] sorted = Sort(removed);


        return sorted;
    }

    private static GraphValue[] Sort(GraphValue[] row)
    {
        ArrayList<GraphValue> row_sorted = new ArrayList<GraphValue>();

        int max = 0;
        for(int i=0;i<row.length;i++)
        {
            if(row[i].timeSlotIndex > max)
            {
                max = row[i].timeSlotIndex;
            }
        }

        for(int i=0;i<=max;i++)
        {
            GraphValue x = FindAtIndex(row,i);

            if(x != null)
            {
                row_sorted.add(x);
            }
        }
        return row_sorted.toArray(new GraphValue[row_sorted.size()]);
    }

    private static GraphValue FindAtIndex(GraphValue[] row,int index)
    {
        GraphValue x = null;
        for(int i=0;i<row.length;i++)
        {
            if(row[i].timeSlotIndex == index)
            {
                x =row[i];
                break;
            }
        }
        return x;
    }
}
