package com.csri.ami.health_u.dataManagement.analyze.graphing;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.ArrayList;

/**
 * Created by daniel on 29/10/2015.
 */
public class GraphValueWeek
{
    public GraphValue[] motion;
    public GraphValue[] sound;
    public GraphValue[] location;

    public double motion_weekScore;
    public double sound_weekScore;
    public double location_weekScore;

    public DateTime startdate;
    public DateTime enddate;

    public int dayCount;


    public GraphValueWeek(GraphValueDay[] week)
    {
        startdate = week[0].date;
        enddate = week[week.length-1].date;

        int numDays = Days.daysBetween(startdate.withTimeAtStartOfDay(),enddate.withTimeAtStartOfDay()).getDays() + 1;

        motion = new GraphValue[numDays];
        sound = new GraphValue[numDays];
        location = new GraphValue[numDays];

        int currentIndexOfDay= 0;
        float minsPerDay_active = (60 * 16);
        for(int i=0;i<numDays;i++)
        {
            DateTime current = startdate.withTimeAtStartOfDay().plusDays(i);

            int index = GetIndexOfDate(week, current);
            if(index != -1)
            {
                int dayOfWeek = current.getDayOfWeek();
                motion[i] = new MotionGraphValue(dayOfWeek,week[index].motion_dayScore);
                sound[i] = new MotionGraphValue(dayOfWeek,week[index].sound_dayScore);
                location[i] = new MotionGraphValue(dayOfWeek,week[index].location_dayScore);

                motion[i].graph_value = (motion[i].graph_value / minsPerDay_active) * 100;
                if(motion[i].graph_value > 100)
                {
                    motion[i].graph_value = 100;
                }
                sound[i].graph_value = (sound[i].graph_value / minsPerDay_active) * 100;
                if(sound[i].graph_value > 100)
                {
                    sound[i].graph_value = 100;
                }

                location[i].graph_value = location[i].graph_value / 1.4;

                motion_weekScore += week[index].motion_dayScore;
                sound_weekScore += week[index].sound_dayScore;
                location_weekScore += week[index].location_dayScore / (double)7;

                dayCount ++;
            }

        }
    }

    public static double MaxTotalValue(GraphValueWeek[] weeks)
    {
        double maxValue=0;
        if(weeks != null)
        {
            if(weeks.length > 0) {
                maxValue = Double.MIN_VALUE;

                for (int i = 0; i < weeks.length; i++) {
                    double m1 = Math.max(weeks[i].motion.length, weeks[i].sound.length);
                    double max = Math.max(m1, weeks[i].location.length);
                    for (int j = 1; j <= 7; j++) {
                        int m_index = GetIndex(weeks[i].motion, j);
                        int s_index = GetIndex(weeks[i].sound, j);
                        int l_index = GetIndex(weeks[i].location, j);

                        double sum = 0;
                        if (m_index != -1) {
                            sum += weeks[i].motion[m_index].graph_value;
                        }
                        if (s_index != -1) {
                            sum += weeks[i].sound[s_index].graph_value;
                        }
                        if (l_index != -1) {
                            sum += weeks[i].location[l_index].graph_value;
                        }

                        if (sum > maxValue) {
                            maxValue = sum;
                        }
                    }
                }
            }
        }

        return maxValue;

    }

    private static int GetIndex(GraphValue[] data,int timeslot)
    {
        int index =-1;
        for(int i=0;i<data.length;i++)
        {
            if(data[i].timeSlotIndex == timeslot)
            {
                index=i;
                break;
            }
        }
        return index;
    }

    public static GraphValueDay[][] GroupByWeek(GraphValueDay[] days)
    {
        ArrayList<GraphValueDay[]> weeks = new ArrayList<GraphValueDay[]>();

        ArrayList<GraphValueDay> current = new ArrayList<GraphValueDay>();
        if(days != null) {
            current.add(days[days.length - 1]);

            int lastDay = days[days.length - 1].date.getDayOfWeek();

            for (int i = days.length - 2; i >= 0; i--) {
                DateTime currentTime = days[i].date;
                int currentDay = currentTime.getDayOfWeek();
                if (currentDay < lastDay) {
                    weeks.add(current.toArray(new GraphValueDay[current.size()]));

                    current = new ArrayList<GraphValueDay>();
                    current.add(days[i]);
                } else {
                    current.add(days[i]);
                }
                lastDay = currentDay;
            }

            if (current.size() > 0) {
                weeks.add(current.toArray(new GraphValueDay[current.size()]));
            }

            return weeks.toArray(weeks.toArray(new GraphValueDay[weeks.size()][]));
        }
        else
        {
            return null;
        }
    }

    private int GetIndexOfDate(GraphValueDay[] week,DateTime date)
    {
        int index = -1;
        for(int i=0;i<week.length;i++)
        {
            int numDays = Days.daysBetween(date,week[i].date.withTimeAtStartOfDay()).getDays();
            if(numDays == 0)
            {
                index = i;
                break;
            }
        }

        return index;
    }

}
