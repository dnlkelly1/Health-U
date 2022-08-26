package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques.ClusterElement;
import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques.GPS_Summary;
import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques.HMM_LocationEntropy;
import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques.HierarchicalCluster;
import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques.TimeSyncTable;
import com.csri.ami.health_u.dataManagement.record.GPS;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.ArrayList;
import java.util.Random;

/**
 * Location data is recorded by the SensorRecorder class (via the GPS_Sensor and Wifi_Sensor class)
 * This class analyses the recorded sensor data to derive features about the predictability of the users travel patterns
 * This technique is based on novel techniques developed/published by Daniel Kelly in 2013: https://doi.org/10.1109/TSMC.2013.2238926
 * Created by daniel on 06/10/2015.
 */
public class LocationAnalyser
{

    public static String LOCATION_FEATURES_SAVE_FILE_NAME_FOR_UPLOAD = "DailyEntropyByHour_location.csv";

    public static String LOCATION_FEATURES_SAVE_FILE_NAME_FOR_GRAPHS = "GraphFeatures_location.csv";

    GPSFile gps;
    HierarchicalCluster clusters;

    HMM_LocationEntropy hmm = null;
   // HMM_LocationEntropy hmm_weekend = null;

    LocationEntropyFeatures EntropyFeatures;

    int samplesPerDay = MotionFileProcessor.SAMPLES_PER_DAY;

    public LocationAnalyser()
    {

    }

    public  void LoadLocation(String file)
    {
        gps = new GPSFile(file);
    }

    /**
     * Initaite the analysis of the location data
     * Stored locations are homogonized into predefined time blocks (15 mins by default)
     * These time blocks represent the median location within that block
     * Time blocks are then analysed to find locations of interest
     * Locations of interest are then modeled using a markov model with probability density functions and transition probabilities
     * Location entropy is then computed for a each day for the user based on the markov model
     *
     * @param minNumberDays
     */
    public void AnalyzeLocation(int minNumberDays)
    {
        if(gps != null)
        {
            DateTime start = TimeSyncTable.UnixTimeToDateTime(gps.GPSData[0].timestamp);
            DateTime end = TimeSyncTable.UnixTimeToDateTime(gps.GPSData[gps.GPSData.length-1].timestamp);

            int numberDays = Days.daysBetween(start,end).getDays();
            if(numberDays > minNumberDays)
            {


                ArrayList<GPS_Summary[]> gps_days_15mins = GetDailyGPSsummaryPerTimeSlot(gps, 5, 2000);
                double windowMinutes = GetDailyGPSsummaryPerTimeSlot_windowsMinutes;

                GPS_Summary[] dataall = GPS_Summary.Flatten(gps_days_15mins);

                int minutesPerDay = 60 * 24;
                int minutesPerTimeslot = minutesPerDay / samplesPerDay;
                ArrayList<GPS_Summary[]> gps_days = TryGetDailyGPSsummaryPerTimeSlot(gps, minutesPerTimeslot, 2000);

                clusters = new HierarchicalCluster(dataall, windowMinutes);



                LoadHMMs(dataall, gps_days, gps_days_15mins);

                EntropyFeatures = CalculateHMMEntropys(gps_days);

                EntropyFeatures.Write();
            }
        }
    }

    public void LoadHMMs(GPS_Summary[] dataall,ArrayList<GPS_Summary[]> gps_days,ArrayList<GPS_Summary[]> gps_days_15mins)
    {
        DataElement[][] learningDataWeekday = GetAllObs(gps_days, 0.7, false, HMM_LocationEntropy.DAY_TYPE_WEEKDAY);
        hmm = new HMM_LocationEntropy(learningDataWeekday, dataall, gps_days_15mins, clusters, HMM_LocationEntropy.DAY_TYPE_WEEKDAY,false);

    }

    public LocationEntropyFeatures CalculateHMMEntropys(ArrayList<GPS_Summary[]> gps_days)
    {
        double p = 0;


        double[] dayProbabilities = new double[gps_days.size()];

        double[] WeekdayAvg = new double[5];
        double[] WeekendAvg = new double[2];
        double avg = 0;
        double avgweekday = 0;
        double avgweekend = 0;
        double max = Double.MIN_VALUE;

        ArrayList<Double[]> weekdayHourlyEnt = new ArrayList<Double[]>();
        ArrayList<Double[]> weekendHourlyEnt = new ArrayList<Double[]>();
        ArrayList<Double[]> allHourlyEnt = new ArrayList<Double[]>();
        ArrayList<Integer[]> allHourlyIndices = new ArrayList<Integer[]>();

        ArrayList<Double> weekdayDates = new ArrayList<Double>();
        ArrayList<Double> weekendDates = new ArrayList<Double>();
        ArrayList<Double> allDates = new ArrayList<Double>();


        ArrayList<Double> weekdayProbs = new ArrayList<Double>();
        ArrayList<Double> weekendProbs = new ArrayList<Double>();
        ArrayList<Double> weekProbs = new ArrayList<Double>();
        ArrayList<Double> probStorage = new ArrayList<Double>();
        ArrayList<Double> WeekprobStorage = new ArrayList<Double>();

        double[] weekendAvg_PerHour = null;
        double[] weekdayAvg_PerHour = null;
        Random r = new Random();
        for (int d = 0; d < gps_days.size(); d++)
        {
            int i = d;
            double[][] p_day = GetDayProbObs(gps_days, i);
            DataElement[] obs = GetDayObs(gps_days, i, 0.9, false);
            Integer[] obsIndices = GetDayObs_indices;


            if (obs != null)
            {
                if (weekdayAvg_PerHour == null && weekendAvg_PerHour == null)
                {
                    weekendAvg_PerHour = new double[p_day.length - 1];
                    weekdayAvg_PerHour = new double[p_day.length - 1];
                }


                {
                    Double[] c = null;
                    double ent = hmm.forwardEntropy(obs);
                    c = hmm.forwardEntropy_c;

                    weekdayProbs.add(ent);
                    probStorage.add(ent);
                    weekdayDates.add(obs[0].time);
                    allDates.add(obs[0].time);
                    weekdayHourlyEnt.add(c);
                    allHourlyEnt.add(c);
                    allHourlyIndices.add(obsIndices);

                    p = ent;

                }

                if (p > max)
                {
                    max = p;
                }


            }

        }

        for (int i = 0; i < weekendHourlyEnt.size(); i++)
        {
            Double[] current = weekendHourlyEnt.get(i);
            for (int j = 0; j < current.length; j++)
            {
                weekendAvg_PerHour[j] += current[j] / (double)weekendProbs.size();

            }
        }
        // weekendHourlyEnt.Add(weekendAvg_PerHour);
        for (int i = 0; i < weekdayHourlyEnt.size(); i++)
        {
            Double[] current = weekdayHourlyEnt.get(i);
            for (int j = 0; j < current.length; j++)
            {
                weekdayAvg_PerHour[j] += current[j] / (double)weekdayProbs.size();

            }
        }

        ArrayList<Double> features = new ArrayList<Double>();
        ArrayList<Double> weekday_features = new ArrayList<Double>();
        ArrayList<Double> weekend_features = new ArrayList<Double>();

        ////////////////////all days summary////////////////////
        if (probStorage.size() > 0)
        {
            Double[] all = probStorage.toArray(new Double[probStorage.size()]);
            double allEnt = Avg(all);
            double allVar = 0;
            for (int i = 0; i < probStorage.size(); i++)
            {
                allVar += (Math.abs(all[i] - allEnt)) / (double)probStorage.size();
            }


            features.add(allEnt);//0
            features.add(allVar);//1
            features.add(Skewness(all, allEnt, Math.sqrt(allVar)));//2
            features.add(Kurtosis(all, allEnt, Math.sqrt(allVar)));//3
            features.add(MinVal(all));//4
            features.add(MaxVal(all));//5

            for (int i = 0; i < weekdayAvg_PerHour.length; i++)
            {
                features.add((weekdayAvg_PerHour[i] + weekendAvg_PerHour[i]) / 2);
            }
        }
        /////////////////////////////////////////////


        ////////////////////weekend days summary//////////////////
        if (weekendProbs.size() > 0)
        {
            Double[] weekend = weekendProbs.toArray(new Double[weekendProbs.size()]);
            double weekendEnt = Avg(weekend);
            double weekendVar = 0;
            for (int i = 0; i < weekendProbs.size(); i++)
            {
                weekendVar += (Math.abs(weekend[i] - weekendEnt)) / (double)weekendProbs.size();
            }
            weekend_features.add(weekendEnt);//6
            weekend_features.add(weekendVar);//7
            weekend_features.add(Skewness(weekend, weekendEnt, Math.sqrt(weekendVar)));//8
            weekend_features.add(Kurtosis(weekend, weekendEnt, Math.sqrt(weekendVar)));//9
            weekend_features.add(MinVal(weekend));//10
            weekend_features.add(MaxVal(weekend));//11

            for (int i = 0; i < weekendAvg_PerHour.length; i++)
            {
                weekend_features.add(weekendAvg_PerHour[i]);
            }
        }
        ////////////////////////////////////////////

        /////////////////////////weekday days summary////////////
        if (weekdayProbs.size() > 0)
        {
            Double[] weekday = weekdayProbs.toArray(new Double[weekdayProbs.size()]);
            double weekdatEnt = Avg(weekday);
            double weekdayVar = 0;
            for (int i = 0; i < weekdayProbs.size(); i++)
            {
                weekdayVar += (Math.abs(weekday[i] - weekdatEnt)) / (double)weekdayProbs.size();
            }
            weekday_features.add(weekdatEnt);//12
            weekday_features.add(weekdayVar);//13
            weekday_features.add(Skewness(weekday, weekdatEnt, Math.sqrt(weekdayVar)));//14
            weekday_features.add(Kurtosis(weekday, weekdatEnt, Math.sqrt(weekdayVar)));//15
            weekday_features.add(MinVal(weekday));//16
            weekday_features.add(MaxVal(weekday));//17

            for (int i = 0; i < weekdayAvg_PerHour.length; i++)
            {
                weekday_features.add(weekdayAvg_PerHour[i]);
            }
        }
        /////////////////////////////////////////////


        LocationEntropyFeatures entropys = new LocationEntropyFeatures();
        entropys.hourlyEnt = allHourlyEnt;
        entropys.hourlyEntIndices = allHourlyIndices;
        entropys.dates = allDates;


        if (features.size() > 0)
            entropys.AllDays = features.toArray(new Double[features.size()]);
        if (weekday_features.size() > 0)
            entropys.WeekDay = weekday_features.toArray(new Double[weekday_features.size()]);

        return entropys;
    }

    private double MaxVal(Double[] data)
    {
        double max = Double.MIN_VALUE;
        for (int i = 0; i < data.length; i++)
        {
            if (data[i] > max) {
                max = data[i];
            }
        }
        return max;
    }

    private double MinVal(Double[] data)
    {
        double min = Double.MAX_VALUE;
        for (int i = 0; i < data.length; i++)
        {
            if (data[i] < min)
            {
                min = data[i];
            }
        }
        return min;
    }

    private double Skewness(Double[] data, double mean, double stddev) {
        double sum = 0;
        for (int i = 0; i < data.length; i++)
        {
            sum += Math.pow((data[i] - mean), 3);
        }
        if (sum != 0)
        {
            return sum / ((data.length - 1) * (Math.pow(stddev, 3)));
        }
        else
        {
            return 0;
        }
    }

    private double Kurtosis(Double[] data, double mean, double stddev)
    {
        double sum = 0;
        for (int i = 0; i < data.length; i++)
        {
            sum += Math.pow((data[i] - mean), 4);
        }
        if (sum != 0)
        {
            return sum / ((data.length - 1) * (Math.pow(stddev, 4)));
        }
        else
        {
            return 0;
        }
    }

    private double Avg(Double[] probs)
    {


        double sum = 0;

        for (int i = 0; i < probs.length; i++)
        {
            sum += probs[i] / (double)probs.length;
        }

        // double test = (0.1 * Math.Log(0.1, 2)) + (0.1 * Math.Log(0.1, 2)) + (0.1 * Math.Log(0.1, 2)) + (0.7 * Math.Log(0.7, 2));

        return sum;
    }

    private double[][] GetDayProbObs(ArrayList<GPS_Summary[]> gps_days, int day)
    {
        double minsPerTimeSlot = clusters.TimeProbabilities_NumberMinsPerTimeSlot();
        GPS_Summary[] currentDay = gps_days.get(day);

        double durationOfSingleObs = (60 * 24) / gps_days.get(0).length;


        int startIndex = 0;
        boolean found = true;
        while (currentDay[startIndex] == null && startIndex < currentDay.length)
        {
            startIndex++;
            if (startIndex >= currentDay.length)
            {
                found = false;
                break;
            }
        }

        if (found)
        {


            DateTime startTime = TimeSyncTable.UnixTimeToDateTime(currentDay[startIndex].TimeStamp);
            DateTime start_day = startTime.withTimeAtStartOfDay();// new DateTime(startTime.Year, startTime.Month, startTime.Day);
            startTime = start_day;
            DateTime currentTime = startTime;
            double totalMins = 0;

            double[][] probs = new double[currentDay.length][];

            for (int j = 0; j < currentDay.length; j++)
            {
                currentTime = startTime.plusMinutes((int) totalMins);
                double[] p = null;
                if (currentDay[j] != null)
                {
                    ClusterElement x = currentDay[j];
                    p = clusters.GetProbabilities(x.GetElement());
                }
                else
                {
                    p = clusters.GetTimeProbabilities(TimeSyncTable.DateTimeToUnixTime(currentTime));
                }
                probs[j] = p;
                totalMins += durationOfSingleObs;
            }
            return probs;
        }
        else
        {
            return null;
        }
    }

    private DataElement[][] GetAllObs(ArrayList<GPS_Summary[]> gps_days, double validPercentageThreshold, boolean addNulls,int daytype)
    {
        ArrayList<DataElement[]> all = new ArrayList<DataElement[]>();
        for (int i = 0; i < gps_days.size(); i++)
        {
            DataElement[] currentDay = GetDayObs(gps_days, i, validPercentageThreshold, addNulls);
            if (currentDay != null)
            {
                DateTime time = TimeSyncTable.UnixTimeToDateTime(currentDay[0].time);
                all.add(currentDay);
            }

        }
        return all.toArray(new DataElement[all.size()][]);
    }

    private Integer[] GetDayObs_indices;
    private DataElement[] GetDayObs(ArrayList<GPS_Summary[]> gps_days, int day, double validPercentageThreshold,boolean addNulls)
    {
        ArrayList<Integer> indices = new ArrayList<Integer>();
        double minsPerTimeSlot = (60 * 24) / gps_days.get(0).length;

        GPS_Summary[] currentDay = gps_days.get(day);


        int startIndex = 0;
        boolean found = true;
        while (currentDay[startIndex] == null && startIndex < currentDay.length)
        {
            startIndex++;
            if (startIndex >= currentDay.length)
            {
                found = false;
                break;
            }
        }
        if (found)
        {

            DateTime startTime = TimeSyncTable.UnixTimeToDateTime(currentDay[startIndex].TimeStamp);
            DateTime start_day = startTime.withTimeAtStartOfDay();
            startTime = start_day;
            DateTime currentTime = startTime;
            double totalMins = 0;
            double NullCountThreshold = (double)currentDay.length * validPercentageThreshold;


            ArrayList<DataElement> obs = new ArrayList<DataElement>();
            int nullCount = 0;
            for (int j = 0; j < currentDay.length; j++)
            {
                currentTime = startTime.plusMinutes((int) totalMins);
                ClusterElement x = currentDay[j];
                if (x == null)
                {
                    nullCount++;
                    if (addNulls)
                    {
                        boolean existsNonNullAfter = false;
                        for(int k=j+1;k<currentDay.length;k++)
                        {
                            if(currentDay[k] != null)
                            {
                                existsNonNullAfter = true;
                                break;
                            }
                        }
                        if(existsNonNullAfter)
                        {
                            GPS_Summary temp = new GPS_Summary(TimeSyncTable.DateTimeToUnixTime(currentTime));
                            x = temp;
                            obs.add(x.GetElement());
                            indices.add(j);
                        }

                    }

                }
                else
                {
                    obs.add(x.GetElement());
                    indices.add(j);
                }

                totalMins += minsPerTimeSlot;
            }

            GetDayObs_indices = indices.toArray(new Integer[indices.size()]);


            if (nullCount > NullCountThreshold)
            {
                return null;
            }
            else
            {
                return obs.toArray(new DataElement[obs.size()]);
            }
        }
        else
        {
            return null;
        }
    }

    public double GetDailyGPSsummaryPerTimeSlot_windowsMinutes;
    private ArrayList<GPS_Summary[]> GetDailyGPSsummaryPerTimeSlot(GPSFile t, double timeslotDuration_mins,int maxValidTimeSlots)
    {
        ArrayList<GPS_Summary[]> res = null;
        int validCount = maxValidTimeSlots;

        double startMins = timeslotDuration_mins;
        double currentMins = startMins;

        while (validCount >= maxValidTimeSlots && currentMins < 20)
        {
            res = TryGetDailyGPSsummaryPerTimeSlot(t, currentMins, maxValidTimeSlots);
            validCount = TryGetDailyGPSsummaryPerTimeSlot_ValidCount;
            if(validCount >= maxValidTimeSlots) {
                currentMins += 1;
            }
        }

        GetDailyGPSsummaryPerTimeSlot_windowsMinutes = currentMins;


        return res;
    }

    private int TryGetDailyGPSsummaryPerTimeSlot_ValidCount;
    private ArrayList<GPS_Summary[]> TryGetDailyGPSsummaryPerTimeSlot(GPSFile t, double timeslotDuration_mins,int maxValidTimeSlots)
    {
        ArrayList<GPS_Summary[]> days = new ArrayList<GPS_Summary[]>();

        int GPS_StartIndex = 0;

        DateTime start = TimeSyncTable.UnixTimeToDateTime(t.GPSData[0].timestamp);
        DateTime start_day = start.withTimeAtStartOfDay();
        start = start_day;

        DateTime endTime = TimeSyncTable.UnixTimeToDateTime(t.GPSData[t.GPSData.length-1].timestamp);
        int slotsPerDay = (int)((double)(24 * 60) / timeslotDuration_mins);

        double totalMins = 0;

        DateTime date_previousPoint = start_day;

        ArrayList<GPS_Summary> CurrentDayGpsSummaries = new ArrayList<GPS_Summary>();

        int nullCount = 0;
        int validCount = 0;
        while (start.getMillis() <  endTime.getMillis() && validCount < maxValidTimeSlots)
        {
            DateTime end = start.plusMinutes((int) timeslotDuration_mins);
            GPS[] gps_window = GPSFile.GetWindow(t.GPSData, TimeSyncTable.DateTimeToUnixTime(start), TimeSyncTable.DateTimeToUnixTime(end), GPS_StartIndex);


            if (gps_window.length > 0)
            {
                if (gps_window != null && gps_window.length > 0)
                {
                    CurrentDayGpsSummaries.add(new GPS_Summary(gps_window, 0, 0, gps_window[0].timestamp, true));
                    validCount++;
                }

            }
            else
            {
                CurrentDayGpsSummaries.add(null);
            }
            totalMins += timeslotDuration_mins;

            GPS_StartIndex += gps_window.length;

            start = end;

            DateTime currentDate = start.withTimeAtStartOfDay();
            int daysBetween = Days.daysBetween(currentDate,date_previousPoint).getDays();
            date_previousPoint = currentDate;

            if (daysBetween != 0)
            {
                totalMins = 0;
                days.add(CurrentDayGpsSummaries.toArray(new GPS_Summary[CurrentDayGpsSummaries.size()]));
                CurrentDayGpsSummaries = new ArrayList<GPS_Summary>();
            }


        }
        TryGetDailyGPSsummaryPerTimeSlot_ValidCount = validCount;
        if(CurrentDayGpsSummaries.size() > 0)
        {
            while(totalMins < (60 * 24))
            {
                CurrentDayGpsSummaries.add(null);
                totalMins += timeslotDuration_mins;
            }
            days.add(CurrentDayGpsSummaries.toArray(new GPS_Summary[CurrentDayGpsSummaries.size()]));
        }

        return days;
    }
}
