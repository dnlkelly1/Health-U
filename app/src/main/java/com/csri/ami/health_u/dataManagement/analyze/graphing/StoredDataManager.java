package com.csri.ami.health_u.dataManagement.analyze.graphing;

import org.joda.time.DateTime;
import org.joda.time.Days;

/**
 * Created by daniel on 14/10/2015.
 */
public class StoredDataManager
{
    public GraphValueDay[] days;

    public GraphValueWeek[] weeks;

    public double MaxWeekDayValue =0;
    public double MaxHourValue=0;


    public StoredDataManager()
    {
        GraphValue[] m = MotionGraphValue.Load();

        //GraphValue[][] motion = GraphValue.GroupByDays(m, MotionFileProcessor.SAMPLES_PER_DAY);

        GraphValue[] s = SoundGraphValue.Load();
        //GraphValue[][] sound = GraphValue.GroupByDays(s, MotionFileProcessor.SAMPLES_PER_DAY);

        GraphValue[] l = LocationGraphValue.Load();
        //GraphValue[][] location = GraphValue.GroupByDays(l, MotionFileProcessor.SAMPLES_PER_DAY);




        DateTime minDate = null; //test[0].withTimeAtStartOfDay();// m[0].time.withTimeAtStartOfDay();
        DateTime maxDate = null; // test[0].withTimeAtStartOfDay();//m[0].time.withTimeAtStartOfDay();


        if(m != null && m.length > 0)
        {
            minDate = m[0].time.withTimeAtStartOfDay();
            maxDate = m[0].time.withTimeAtStartOfDay();
            for (int i = 1; i < m.length; i++)
            {
                int diff = Days.daysBetween(minDate, m[i].time.withTimeAtStartOfDay()).getDays();
                if (diff < 0)
                {
                    minDate = m[i].time.withTimeAtStartOfDay();
                }
                int diffMax = Days.daysBetween(maxDate, m[i].time.withTimeAtStartOfDay()).getDays();
                if (diffMax > 0)
                {
                    maxDate = m[i].time.withTimeAtStartOfDay();
                }
            }
        }

        if(s != null && s.length > 0)
        {
            if(minDate == null && maxDate == null)
            {
                minDate = s[0].time.withTimeAtStartOfDay();
                maxDate = s[0].time.withTimeAtStartOfDay();
            }

            for (int i = 1; i < s.length; i++)
            {
                int diff = Days.daysBetween(minDate, s[i].time.withTimeAtStartOfDay()).getDays();
                if (diff < 0)
                {
                    minDate = s[i].time.withTimeAtStartOfDay();
                }
                int diffMax = Days.daysBetween(maxDate, s[i].time.withTimeAtStartOfDay()).getDays();
                if (diffMax > 0)
                {
                    maxDate = s[i].time.withTimeAtStartOfDay();
                }
            }
        }

        if(l != null && l.length > 0)
        {
            if (minDate == null && maxDate == null)
            {
                minDate = l[0].time.withTimeAtStartOfDay();
                maxDate = l[0].time.withTimeAtStartOfDay();
            }

            for (int i = 1; i < l.length; i++)
            {
                int diff = Days.daysBetween(minDate, l[i].time.withTimeAtStartOfDay()).getDays();
                if (diff < 0)
                {
                    minDate = l[i].time.withTimeAtStartOfDay();
                }
                int diffMax = Days.daysBetween(maxDate, l[i].time.withTimeAtStartOfDay()).getDays();
                if (diffMax > 0)
                {
                    maxDate = l[i].time.withTimeAtStartOfDay();
                }
            }
        }

        days = GraphValueDay.GroupByDay(minDate, maxDate, m, s, l);

        GraphValueDay[][] weeks_days = GraphValueWeek.GroupByWeek(days);
        if(weeks_days != null)
        {
            weeks = new GraphValueWeek[weeks_days.length];
            int c = 0;
            for (int i = weeks.length - 1; i >= 0; i--) {
                weeks[c] = new GraphValueWeek(weeks_days[i]);
                c++;
            }
        }

        MaxHourValue = GraphValueDay.MaxValue(days);
        MaxWeekDayValue = GraphValueWeek.MaxTotalValue(weeks);

    }
}
