package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis;

import com.csri.ami.health_u.dataManagement.analyze.classifiers.Complex;
import com.csri.ami.health_u.dataManagement.analyze.classifiers.FFT_Features;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;

import java.util.ArrayList;

/**
 * Created by daniel on 30/09/2015.
 */
public class SignalStatsBuilder
{
    public String title = "";

    SignalStats dailyAverageDataStorage;
    SignalStats average;
    SignalStats variance;

    SignalStats rawFeatureStorage;

    DateTime date;


    public SignalStatsBuilder(int samples)
    {
        variance = new SignalStats(samples);
        average = new SignalStats(samples);
        dailyAverageDataStorage = new SignalStats(samples);
        rawFeatureStorage = new SignalStats(samples);

    }


    public int FFT_PEAKS = 3;
    private double[] UpdateFeatureWithFFTSummary(double[] data)
    {
        if (data != null)
        {
            double[] fftWindow = new double[SignalStats.FFT_NORMALIZE_SIZE];

            for (int i = 0; i < fftWindow.length; i++)
            {
                int index = (data.length - SignalStats.FFT_NORMALIZE_SIZE) + i;
                if(index < 0)
                {
                    int error =1;
                }
                fftWindow[i] = data[index];
            }

            Complex[] c = AndroidFrame.GetComplexWindow(fftWindow);
            FFT_Features[] ffts = AndroidFrame.GetPeaks(c, FFT_PEAKS );



            ArrayList<Double> newFeature = new ArrayList<Double>();

            for (int i = 0; i < data.length - SignalStats.FFT_NORMALIZE_SIZE; i++)
            {
                newFeature.add(data[i]);
            }

            for (int i = 0; i < ffts.length; i++)
            {
                newFeature.add(ffts[i].Frequency);
            }

            Double[] temp= newFeature.toArray(new Double[newFeature.size()]);
            double[] temp_2 = new double[temp.length];
            for(int i=0;i<temp.length;i++)
            {
                temp_2[i] = temp[i];
            }
            return temp_2;
        }
        else
        {
            return null;
        }
    }


    private int previousWindow = -1;

    private double[] ProcessTimeSlot(int p)
    {
        int featureSize =27;
        double[] f1=null;
        if(rawFeatureStorage.rawFeatures.get(p).size() > 0)
        {
            double[] dayStats_FFT = rawFeatureStorage.ComputeFeatures(p);

            double[] dayStats = rawFeatureStorage.Avg(rawFeatureStorage.rawFeatures.get(p));

            dayStats = Append(dayStats, dayStats_FFT);
            rawFeatureStorage.rawFeatures.get(p).clear();

            rawFeatureStorage.values.get(p).clear();
            rawFeatureStorage.values_fps.get(p).clear();


            f1 = UpdateFeatureWithFFTSummary(dayStats);
        }
        else
        {
            f1 = new double[featureSize];
            for(int i=0;i<f1.length;i++)
            {
                f1[i] = -9999;
            }
        }

        Double[] stationaryFeatures = rawFeatureStorage.StationaryFeatureGenerate(rawFeatureStorage.stationaryStats.get(p),rawFeatureStorage.stationaryCount[p],rawFeatureStorage.movementCount[p]);
        rawFeatureStorage.stationaryStats.get(p).clear();

        double[] f = Append(f1, stationaryFeatures);

        for(int i=0;i<f.length;i++)
        {
            if(Double.isNaN(f[i]))
            {
                f[i] = -9999;
            }
        }

        return f;

    }

    public void FinishProcessingCurrentDay(DateTime time, int samplesPerDay)
    {

        for (int p = 0; p < rawFeatureStorage.values.size(); p++)
        {
            if (rawFeatureStorage.values.get(p).size() > 0)
            {
                double[] f = ProcessTimeSlot(p);
                rawFeatureStorage.SaveTimeSlotFeatures_SingleSlot(time, f, p);
            }
        }

        Double[] dayStats = rawFeatureStorage.AverageTimeslotMeasurements();

        if (dayStats != null)
        {
            dailyAverageDataStorage.dayIds.add(time);
            dailyAverageDataStorage.dailyMeasurements.add(dayStats);
        }

        rawFeatureStorage.Clear();
        previousWindow = -1;


    }

    public int GetCurrentTimeSlotIndex(DateTime time,int samplesPerDay)
    {
        DateTime st = time.withTimeAtStartOfDay();
        int minutesElapsed = Minutes.minutesBetween(st, time).getMinutes();
        int minutesPerDay = Hours.hours(24).toStandardMinutes().getMinutes();

        double window_minutes_span = (double)minutesPerDay / (double)samplesPerDay;

        int window_index = (int)((double)minutesElapsed / window_minutes_span);
        return window_index;
    }


    public void UpdateStatistics(DateTime time, DateTime previousOLEDate, int samplesPerDay, double slowTime,int currentTimeslot,int previousTimeslot)
    {


        if (previousOLEDate != null)
        {
            DateTime currentOLDData = time;
            int daysDiff = Days.daysBetween(previousOLEDate.withTimeAtStartOfDay(),currentOLDData.withTimeAtStartOfDay()).getDays();
            if (daysDiff > 0)// && daysDiff < 30)
            {
                FinishProcessingCurrentDay(previousOLEDate, samplesPerDay);

            }


        }


        int window_index =currentTimeslot;

        if (slowTime != -1)
        {
            rawFeatureStorage.stationaryStats.get(previousTimeslot).add(slowTime);
        }


        if (previousTimeslot != -1 && currentTimeslot != previousTimeslot)
        {
            double[] f = ProcessTimeSlot(previousTimeslot);
            
            if(rawFeatureStorage.SecondsSaved(previousTimeslot) > MotionFileProcessor.SECONDS_PER_TIMESLOT / 2) //should have 30 mins saved at least...or else info wont be saved
            {
                rawFeatureStorage.SaveTimeSlotFeatures_SingleSlot(time, f, previousTimeslot);
            }
        }


    }

    public void UpdateStationaryTime(int windowIndex,double windowSeconds)
    {
        rawFeatureStorage.stationaryCount[windowIndex] += windowSeconds;
    }

    public void UpdateMovementTime(int windowIndex,double windowSeconds)
    {
        rawFeatureStorage.movementCount[windowIndex] += windowSeconds;
    }

    public void UpdateStatistics(DateTime time, int i, int windowSize, ArrayList<AndroidFrame> data, DateTime previousOLEDate, int samplesPerDay, double fps,int frameScale,double slowTime,int currentWindow,int previousWindow)
    {


        if (previousOLEDate != null)
        {
            DateTime currentOLDData = time;
            int daysDiff = Days.daysBetween(previousOLEDate.withTimeAtStartOfDay(),currentOLDData.withTimeAtStartOfDay()).getDays();
            if (daysDiff > 0)// && daysDiff < 30)
            {
                FinishProcessingCurrentDay(previousOLEDate, samplesPerDay);

            }

        }


        int window_index = currentWindow;
        //int c = i;
        if (slowTime != -1)
        {
            rawFeatureStorage.stationaryStats.get(window_index).add(slowTime);
        }


        int scaledWindow = (int)((double)windowSize / (double)frameScale);

        double minFrameRateWindow = 10;
        double frameSubsampleSize = 1;
        if(fps > minFrameRateWindow)
        {
            frameSubsampleSize = (int)(fps / minFrameRateWindow);
        }
        ArrayList<AndroidFrame> currentData = new ArrayList<AndroidFrame>();
        double avg_value=0;
        for (int c = i - (scaledWindow / 2); c < i + (scaledWindow / 2); c++)
        {
            if (c >= 0 && c <data.size())
            {
                currentData.add(data.get(c));

            }
            else
            {
                int error =1;
            }

            if(c >= 0 && c <data.size() && c % frameSubsampleSize ==0)
            {
                rawFeatureStorage.values.get(window_index).add(data.get(c).AccelMag());
                rawFeatureStorage.values_fps.get(window_index).add(fps/frameSubsampleSize);
            }
        }

        if(currentData.size() > 0)
        {
            Double[] currentFeature = rawFeatureStorage.ComputeFeatures(currentData, fps);
            rawFeatureStorage.rawFeatures.get(window_index).add(currentFeature);
        }




        if (previousWindow != -1 && window_index != previousWindow)
        {
            double[] f = ProcessTimeSlot(previousWindow);
            if(rawFeatureStorage.SecondsSaved(previousWindow) > MotionFileProcessor.SECONDS_PER_TIMESLOT / 2) //should have 30 mins saved at least...or else info wont be saved
            {
                rawFeatureStorage.SaveTimeSlotFeatures_SingleSlot(time, f, previousWindow);
            }
        }


    }



    private double[] Append(double[] f1,Double[] f2)
    {
        double[] data = new double[f1.length + f2.length];
        int c=0;
        for(int i=0;i<f1.length;i++)
        {
            data[i] = f1[i];
            c++;
        }

        for(int i=0;i<f2.length;i++)
        {
            data[c] = f2[i];
            c++;
        }

        return data;

    }

    private double[] Append(double[] f1,double[] f2)
    {
        double[] data = new double[f1.length + f2.length];
        int c=0;
        for(int i=0;i<f1.length;i++)
        {
            data[i] = f1[i];
            c++;
        }

        for(int i=0;i<f2.length;i++)
        {
            data[c] = f2[i];
            c++;
        }

        return data;

    }
}
