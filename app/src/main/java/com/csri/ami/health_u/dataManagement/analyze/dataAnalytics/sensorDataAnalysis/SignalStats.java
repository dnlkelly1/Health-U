package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis;

import android.os.Environment;

import com.csri.ami.health_u.dataManagement.analyze.classifiers.Complex;
import com.csri.ami.health_u.dataManagement.analyze.classifiers.FFT_Features;
import com.csri.ami.health_u.dataManagement.record.SensorRecorder;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Feature Vector Generator for Raw motion data
 * Computes 1-d dimensional feature vector to summarise period of motion data
 * Created by daniel on 29/09/2015.
 */
public class SignalStats
{
    //public double[] TimeslotMeasurement;
    //public int[] TimeslotSamplesCount;

    public String filename;
    public ArrayList<DateTime> dayIds;
    public ArrayList<Double[]> dailyMeasurements;

    public double[][] dailyMeasurements_PCA;

    public double[] stationaryCount;
    public double[] movementCount;


    public ArrayList<ArrayList<Double>> values = new ArrayList<ArrayList<Double>>();
    public ArrayList<ArrayList<Double>> values_fps = new ArrayList<ArrayList<Double>>();
    public ArrayList<ArrayList<Double>> stationaryStats = new ArrayList<ArrayList<Double>>();

    public ArrayList<ArrayList<Double[]>> rawFeatures = new ArrayList<ArrayList<Double[]>>();

    public int[] timeSlotCount;
    public double[][] timeSlotMeasurements;
    public ArrayList<double[][]> daily_timeSlotMeasurements;
    public double[] DailyTimeslotMeasurements_NoFFT(int i, int j)
    {
        double[] f = new double[daily_timeSlotMeasurements.get(i)[j].length - FFT_NORMALIZE_SIZE];

        for (int k = 0; k < f.length; k++)
        {
            f[k] = daily_timeSlotMeasurements.get(i)[j][k];
        }

        return f;
    }

    public int[] secondsCounts;

    private int noTimeSlots;

    public static int NUMPCAS = 2;

    public static int FFT_NORMALIZE_SIZE = 256;

    public SignalStats(int samples)
    {
        noTimeSlots = samples;
        //TimeslotMeasurement = new double[samples];
        //TimeslotSamplesCount = new int[samples];
        timeSlotMeasurements = new double[samples][];
        timeSlotCount = new int[samples];
        daily_timeSlotMeasurements = new ArrayList<double[][]>();
        secondsCounts = new int[samples];

        stationaryCount = new double[samples];
        movementCount = new double[samples];

        values = new ArrayList<ArrayList<Double>>();
        for (int i = 0; i < samples; i++)
        {
            values.add(new ArrayList<Double>());
        }

        rawFeatures = new ArrayList<ArrayList<Double[]>>();
        for (int i = 0; i < samples; i++)
        {
            rawFeatures.add(new ArrayList<Double[]>());
        }

        values_fps = new ArrayList<ArrayList<Double>>();
        for (int i = 0; i < samples; i++)
        {
            values_fps.add(new ArrayList<Double>());
        }

        stationaryStats = new ArrayList<ArrayList<Double>>();
        for (int i = 0; i < samples; i++)
        {
            stationaryStats.add(new ArrayList<Double>());
        }

        dayIds = new ArrayList<DateTime>();
        dailyMeasurements = new ArrayList<Double[]>();
        //dailyMeasurements_PCA = new ArrayList<double[]>();
    }


    /**
     * Generates a vector of statistical features to summarise a window of raw motion data
     * @param data: raw motion data
     * @param stationaryDurations: Array specifiying periods of no movement
     * @param fps: Frames per second of data
     * @return 1-D Feature Vector
     */
    private Double[] FeatureGenerate(ArrayList<AndroidFrame> data, Double[] stationaryDurations, double fps)
    {
        ArrayList<Double> dayStats = new ArrayList<Double>();

        ArrayList<AndroidFrame> dayFlat_ = data;// FlattenTimeSlots();
        AndroidFrame[] dayFlat = dayFlat_.toArray(new AndroidFrame[dayFlat_.size()]);
        int pow2Size = Math.min(8192, pow2(dayFlat.length / 2));
        //avg,var,deviation
        double mean = AndroidFrame.AVG_AccelNorm(dayFlat);
        dayStats.add(mean);//0,24,51 f1
        double stdDev = AndroidFrame.Var_AccelNorm(dayFlat, dayStats.get(dayStats.size() - 1));
        dayStats.add(stdDev);//1,25,52 f2

        ///newly added 4/11/2015
        dayStats.add(AndroidFrame.Kurtosis(dayFlat, mean,stdDev)); //f3
        dayStats.add(AndroidFrame.Skewness(dayFlat, mean, stdDev)); //f4
        dayStats.add(AndroidFrame.MIN_AccelNorm(dayFlat));//f5
        dayStats.add(AndroidFrame.MAX_AccelNorm(dayFlat));//f6
        //////////////////////////////

        dayStats.add(AndroidFrame.Avg_Gyro_BF(dayFlat));//2,26,53 f7
        dayStats.add(AndroidFrame.Var_Gyro_BF(dayFlat, dayStats.get(dayStats.size() - 1)));//3,27,54 f8
        dayStats.add(AndroidFrame.Avg_AccelHorizontal_BF(dayFlat));//4,28,55 f9
        dayStats.add(AndroidFrame.Var_AccelHorizontal_BF(dayFlat, dayStats.get(dayStats.size() - 1)));//5,29,56 f10
        dayStats.add(AndroidFrame.Avg_AccelVertical_BF(dayFlat));//6,30,57 f11
        dayStats.add(AndroidFrame.Var_AccelVertical_BF(dayFlat, dayStats.get(dayStats.size() - 1)));//7,31,58 f12
        dayStats.add(AndroidFrame.Avg_GyroHorizontal_BF(dayFlat));//8,32,59 f13
        dayStats.add(AndroidFrame.Var_GyroHorizontal_BF(dayFlat, dayStats.get(dayStats.size() - 1)));//9,33,60 f14
        dayStats.add(AndroidFrame.Avg_GyroVertical_BF(dayFlat));//10,34,61 f15
        dayStats.add(AndroidFrame.Var_GyroVertical_BF(dayFlat, dayStats.get(dayStats.size() - 1)));//11,35,62 f16


        dayStats.add(AndroidFrame.CORR_AccelVertBF_AccelNorm(dayFlat));//12,36,63 f17
        dayStats.add(AndroidFrame.CORR_AccelVertical_AccelHorizontal(dayFlat));//13,37,64 f18
        dayStats.add(AndroidFrame.IRQ_AccelNorm(dayFlat));//14,38,65 f19
        dayStats.add(AndroidFrame.IRQ_GyroVertical_BF(dayFlat));//15,39,66 f20
        dayStats.add(AndroidFrame.IRQ_GyroHorizontal_BF(dayFlat));//16,40,67 f21
        dayStats.add(AndroidFrame.Direction_AccelVertical_GF(dayFlat, (double) fps, (int) fps / 2));//17,41,68 f22

        dayStats.add(AndroidFrame.ATT_RotQuat_StandingDiff_RateOfChange(dayFlat));//18,42,69 f23

        dayStats.add((double) data.size() / fps);//19,43,70 f24

        /////stationary stats////////////////////////////////
        if(stationaryDurations != null)
        {
            //calculate features in relation to durations of no movement
            if (stationaryDurations.length > 0)
            {
                double stationaryVar = 0;
                double avgStationary = Average(stationaryDurations);
                stationaryVar = Average_variance;
                double stationaryIQR = AndroidFrame.IQR(stationaryDurations);
                dayStats.add(stationaryVar);//20,44,71
                dayStats.add(avgStationary);//21,45,72
                dayStats.add(stationaryIQR);//22,46,73


                double ratio = stationaryVar / stationaryIQR;
                if (Double.isNaN(ratio)) {
                    int error = 1;
                    dayStats.add(0.0);
                } else if (Double.isInfinite(ratio)) {
                    dayStats.add(0.0);
                } else {
                    dayStats.add(ratio);//23,47,74
                }
            }
            else
            {
                dayStats.add(0.0);
                dayStats.add(0.0);
                dayStats.add(0.0);
                dayStats.add(0.0);
            }
        }


        return dayStats.toArray(new Double[dayStats.size()]);
    }


    public static double[] ExtractDataWithExistingDay(double[] filledInData, DateTime[] filledInDates, DateTime[] realDates)
    {
        double[] extractedData = new double[realDates.length];
        for (int i = 0; i < realDates.length; i++)
        {
            int indexOf = Exists(filledInDates, realDates[i]);
            if (indexOf != -1)
            {
                extractedData[i] = filledInData[indexOf];
            }
        }
        return extractedData;
    }

    private static int Exists(DateTime[] datesAll, DateTime date)
    {
        boolean found = false;
        int index = -1;
        for (int i = 0; i < datesAll.length; i++)
        {
            if (datesAll[i].year() == date.year() && datesAll[i].dayOfYear() == date.dayOfYear())
            {
                found = true;
                index = i;
                break;
            }
        }
        return index;
    }

    public  DateTime[] FillInBlankDays_newDates;
    public Double[] FillInBlankDays(double[] data, DateTime[] dates)
    {
        DateTime baseTime = new DateTime(2000, 1, 1,0,0,0);
        int previousDay = Days.daysBetween(dates[0].toLocalDate(), baseTime.toLocalDate()).getDays();
        //int previousDay = (int)(dates[0] - baseTime).TotalDays;

        ArrayList<Double> filledInData = new ArrayList<Double>();
        ArrayList<DateTime> filledInDates = new ArrayList<DateTime>();
        filledInData.add(data[0]);
        filledInDates.add(dates[0]);

        for (int i = 1; i < dates.length; i++)
        {
            int currentDay = Days.daysBetween(dates[i].toLocalDate(), baseTime.toLocalDate()).getDays();
            //int currentDay = (int)(dates[i].Date - baseTime.Date).TotalDays;
            int dayIncrease = currentDay - previousDay;

            double inc = (data[i] - data[i - 1]) / (double)dayIncrease;
            double value = data[i - 1] + inc;
            for (int j = 0; j < dayIncrease; j++)
            {
                filledInDates.add(dates[i - 1].plusDays(j + 1));
                filledInData.add(value);
                value += inc;
            }
            previousDay = currentDay;
        }
        FillInBlankDays_newDates = filledInDates.toArray(new DateTime[filledInDates.size()]);
        return filledInData.toArray(new Double[filledInData.size()]);
    }



    public void Clear()
    {
        for(int i=0;i<daily_timeSlotMeasurements.size();i++) {
            daily_timeSlotMeasurements.clear();
        }
        timeSlotMeasurements = new double[noTimeSlots][];

        secondsCounts = new int[noTimeSlots];
        stationaryCount = new double[noTimeSlots];
        movementCount = new double[noTimeSlots];

    }

    public double SecondsSaved(int timeslot)
    {
        return movementCount[timeslot] + stationaryCount[timeslot];
    }

    public void SaveTimeSlotFeatures_SingleSlot(DateTime date,double[] data,int index)
    {
        File f = Environment.getExternalStorageDirectory();
        String fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER;

        File fileToWrite = new File(fullfilename,MotionFileProcessor.MOTION_FEATURES_SAVE_FILE_NAME_FOR_UPLOAD);

        File fileToWrite_graph = new File(fullfilename,MotionFileProcessor.MOTION_FEATURES_SAVE_FILE_NAME_FOR_GRAPHS);
        try
        {
            FileOutputStream fos = new FileOutputStream(fileToWrite, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos);

            FileOutputStream fos_g = new FileOutputStream(fileToWrite_graph, true);
            OutputStreamWriter osw_g = new OutputStreamWriter(fos_g);

            //for(int i=0;i<timeSlotMeasurements.length;i++)
            {
                if(data != null)
                {
                    String line = "";
                    line += date.toString("yyyy-MM-dd HH:mm:ss") + ",";
                    double timeslotAsPercent = (double) index / (double) timeSlotMeasurements.length;
                    line += timeslotAsPercent + ",";
                    for (int j = 0; j < data.length; j++) {
                        line += data[j] + ",";
                    }
                    osw.write(line + "\n");
                    osw_g.write(line + "\n");
                }
            }
            osw.close();
            osw_g.close();
        }
        catch (FileNotFoundException ex)
        {

        }
        catch (IOException ioex)
        {

        }
    }


    public Double[] GetFramesWithSpecifiedFPS_segmentSizes;
    public ArrayList<ArrayList<Double>> GetFramesWithSpecifiedFPS(double specifiedFPS,ArrayList<Double> fpsValues,ArrayList<Double> data)
    {
        ArrayList<ArrayList<Double>> segments_all = new ArrayList<ArrayList<Double>>();

        ArrayList<Double> segment = new ArrayList<Double>();

        for(int i=0;i<data.size();i++)
        {
            double currentFPS = fpsValues.get(i);
            if(Math.abs(currentFPS - specifiedFPS) < specifiedFPS * 0.25)
            {
                segment.add(data.get(i));
            }

            if(i < data.size()-1)
            {
                double nextFPS = fpsValues.get(i+1);
                if(Math.abs(currentFPS - specifiedFPS) < specifiedFPS * 0.25 && Math.abs(nextFPS - specifiedFPS) > specifiedFPS * 0.25)
                {
                    if (segment.size() > 0)
                    {
                        segments_all.add(segment);
                        segment = new ArrayList<Double>();
                    }
                }
            }
        }

        if(segment.size() > 0)
        {
            segments_all.add(segment);
            segment = new ArrayList<Double>();
        }

        GetFramesWithSpecifiedFPS_segmentSizes = new Double[segments_all.size()];
        for(int i=0;i<GetFramesWithSpecifiedFPS_segmentSizes.length;i++)
        {
            GetFramesWithSpecifiedFPS_segmentSizes[i] = (double)segments_all.get(i).size();
        }


        return  segments_all;
    }

    Double[] GetDifferentFrameRates_secondsPerRate;
    Double GetDifferentFrameRates_total;
    public Double[] GetDifferentFrameRates(ArrayList<Double> fpsValues)
    {
        double startRate = fpsValues.get(0);

        ArrayList<Double> rates = new ArrayList<Double>();

        ArrayList<Double> totals = new ArrayList<Double>();
        ArrayList<Double> counts = new ArrayList<Double>();

        rates.add(startRate);
        totals.add(new Double(0.0));
        counts.add(new Double(0.0));

        for(int i=1;i<fpsValues.size();i++)
        {
            double currentFPS = fpsValues.get(i);

            boolean currentFPSassigned = false;
            for(int j=0;j< rates.size();j++)
            {
                double currentRate = rates.get(j);

                if(Math.abs(currentFPS - currentRate) < currentRate * 0.25)
                {
                    currentFPSassigned = true;
                    double currentTotal = totals.get(j);
                    totals.remove(j);
                    totals.add(j,currentTotal + currentFPS);

                    double currentCount = counts.get(j);
                    counts.remove(j);
                    counts.add(j,currentCount + 1);
                    //current value belongs to rate i
                }
            }
            if(!currentFPSassigned)
            {
                rates.add(currentFPS);
                totals.add(new Double(0.0));
                counts.add(new Double(0.0));
            }
        }
        GetDifferentFrameRates_total = 0.0;
        ArrayList<Double> seconds = new ArrayList<Double>();
        for(int i=0;i<rates.size();i++)
        {
            double avg = totals.get(i) / counts.get(i);

            double sec= counts.get(i) / avg;
            seconds.add(new Double(sec));

            GetDifferentFrameRates_total += sec;
            rates.remove(i);
            rates.add(i,avg);
        }
        GetDifferentFrameRates_secondsPerRate = seconds.toArray(new Double[seconds.size()]);

        return rates.toArray(new Double[rates.size()]);
    }

    public Double[] AvgDouble(Double[][] data,Double[] weights)
    {
        if(data.length > 0 && data.length == weights.length)
        {
            double total = 0;
            for (int i = 0; i < weights.length; i++) {
                total += weights[i];
            }
            for (int i = 0; i < weights.length; i++) {
                weights[i] = weights[i] / total;
            }

            Double[] averagedFeature = new Double[data[0].length];
            for(int i=0;i<averagedFeature.length;i++)
            {
                averagedFeature[i] = new Double(0.0);
            }


            for(int i=0;i<data.length;i++)
            {
                for(int j=0;j<data[i].length;j++)
                {
                    averagedFeature[j] += data[i][j] * weights[i];
                }
            }

            return averagedFeature;
        }
        else
        {
            return  null;
        }

    }

    public double[] Avg(ArrayList<Double[]> data)
    {
        if(data.size() > 0)
        {


            double[] averagedFeature = new double[data.get(0).length];


            for(int i=0;i<data.size();i++)
            {
                for(int j=0;j<data.get(i).length;j++)
                {
                    averagedFeature[j] += data.get(i)[j] / (double)data.size();
                }
            }

            return averagedFeature;
        }
        else
        {
            return  null;
        }

    }

    public double[] Avg(Double[][] data,Double[] weights)
    {
        if(data.length > 0 && data.length == weights.length)
        {
            double total = 0;
            for (int i = 0; i < weights.length; i++) {
                total += weights[i];
            }
            for (int i = 0; i < weights.length; i++) {
                weights[i] = weights[i] / total;
            }

            double[] averagedFeature = new double[data[0].length];


            for(int i=0;i<data.length;i++)
            {
                for(int j=0;j<data[i].length;j++)
                {
                    averagedFeature[j] += data[i][j] * weights[i];
                }
            }

            return averagedFeature;
        }
        else
        {
            return  null;
        }

    }

    public Double[] ComputeFeatures(ArrayList<AndroidFrame> data,double fps)
    {
        if(data != null && data.size() > 0)
        {
            Double[] f = FeatureGenerate(data, null, fps);
            return f;
        }
        else
        {
            return null;
        }
    }

    public double[] ComputeFeatures(int timeSlotIndex)
    {

        if(values.get(timeSlotIndex).size() > 0)
        {

            Double[] frameRates = GetDifferentFrameRates(values_fps.get(timeSlotIndex));

            Double[][] features = new Double[frameRates.length][];


            for (int i = 0; i < frameRates.length; i++) {
                ArrayList<ArrayList<Double>> timeslot_Accel_i = GetFramesWithSpecifiedFPS(frameRates[i], values_fps.get(timeSlotIndex), values.get(timeSlotIndex));
                Double[][] features_currentFPS = new Double[timeslot_Accel_i.size()][];
                for (int j = 0; j < timeslot_Accel_i.size(); j++) {
                    Double[] f = null;
                    if (timeslot_Accel_i.size() > 0) {
                        features_currentFPS[j] = FeatureGenerate_FFT_Double(timeslot_Accel_i.get(j), frameRates[i]);
                        // features[i] = f;

                    }
                }
                features[i] = AvgDouble(features_currentFPS, GetFramesWithSpecifiedFPS_segmentSizes);
            }

            double[] f = Avg(features, GetDifferentFrameRates_secondsPerRate);


            secondsCounts[timeSlotIndex] += GetDifferentFrameRates_total;// values.get(timeSlotIndex).size();

            return f;
        }
        else
        {
            return null;
        }



    }

    public Double[] AverageTimeslotMeasurements()
    {
        double[] weights = new double[secondsCounts.length];
        double sum = 0;
        for (int i = 0; i < weights.length; i++)
        {
            sum += secondsCounts[i];
        }
        for (int i = 0; i < weights.length; i++)
        {
            weights[i] = (double) secondsCounts[i] / (double)sum;
        }

        int count = 0;
        int dim = 0;
        for (int i = 0; i < timeSlotMeasurements.length; i++)
        {
            if (timeSlotMeasurements[i] != null)
            {
                count++;
                dim = timeSlotMeasurements[i].length;
            }
        }
        if (count >= 2)
        {
            double[] avg = new double[dim];

            for (int i = 0; i < dim; i++)
            {
                for (int j = 0; j < timeSlotMeasurements.length; j++)//have a weight per time slot???
                {
                    if (timeSlotMeasurements[j] != null)
                    {
                        avg[i] += timeSlotMeasurements[j][i] * weights[j];// / (double)count;
                    }
                }

            }

            double[] var = new double[avg.length];
            for (int i = 0; i < dim; i++)
            {
                for (int j = 0; j < timeSlotMeasurements.length; j++)
                {
                    if (timeSlotMeasurements[j] != null)
                    {
                        var[i] += Math.abs(avg[i] - timeSlotMeasurements[j][i]) * weights[j];// (double)count;
                    }
                }

            }


            double[] fftWindow = new double[FFT_NORMALIZE_SIZE];

            for (int i = 0; i < fftWindow.length; i++)
            {
                int index = (avg.length - FFT_NORMALIZE_SIZE) + i;

                fftWindow[i] = avg[index];
            }

            Complex[] c = AndroidFrame.GetComplexWindow(fftWindow);
            FFT_Features[] ffts = AndroidFrame.GetPeaks(c, 3);

            ArrayList<Double> avg_all = new ArrayList<Double>();
            for (int i = 0; i < avg.length - FFT_NORMALIZE_SIZE; i++)
            {
                avg_all.add(avg[i]);
            }

            for (int i = 0; i < var.length - FFT_NORMALIZE_SIZE; i++)
            {
                avg_all.add(var[i]);
            }

            for (int i = 0; i < ffts.length; i++)
            {
                avg_all.add(ffts[i].Frequency);
            }

            //daily_timeSlotMeasurements.add(timeSlotMeasurements);
            return avg_all.toArray(new Double[avg_all.size()]);
        }
        else
        {
            return null;
        }
    }


    private double Average_variance;
    private double Average(Double[] data)
    {
        Average_variance =0;
        double avg = 0;
        for (int i = 0; i < data.length; i++)
        {
            avg += data[i];
        }
        avg = avg / (double)data.length;

        double var = 0;
        for (int i = 0; i < data.length; i++)
        {
            var += Math.pow(data[i] - avg, 2);
        }
        var = var / (double)data.length;

        Average_variance = Math.sqrt(var);

        return avg;
    }

    public static int pow2(int n) { int x = 1; while (x < n) { x <<= 1; } return x; }

    public Double[] StationaryFeatureGenerate(ArrayList<Double> stationaryDurations,double stationaryTime,double moveTime)
    {
        Double[] data = new Double[stationaryDurations.size()];
        for(int i=0;i<data.length;i++)
        {
            data[i] = stationaryDurations.get(i);
        }

        ArrayList<Double> dayStats = new ArrayList<Double>();

        double secondsPerTimeSlot = (60 * 60 * 24) / MotionFileProcessor.SAMPLES_PER_DAY;
        double movement = moveTime / (secondsPerTimeSlot);
        dayStats.add(movement);


        //compute 4 feature: Variance, Mean, IQR, Variance/IQR
        if (stationaryDurations.size() > 0)
        {
            double total =0;
            for(int i=0;i<stationaryDurations.size();i++)
            {
                total+= stationaryDurations.get(i);
            }
            double stationaryVar = 0;
            double avgStationary = Average(data);
            stationaryVar = Average_variance;
            double stationaryIQR = AndroidFrame.IQR(data);

            dayStats.add(stationaryVar);//20,44,71
            dayStats.add(avgStationary);//21,45,72
            dayStats.add(stationaryIQR);//22,46,73

            double ratio = stationaryVar / stationaryIQR;
            if (Double.isNaN(ratio))
            {
                int error = 1;
                dayStats.add(0.0);
            }
            else if(Double.isInfinite(ratio))
            {
                dayStats.add(0.0);
            }
            else
            {
                dayStats.add(ratio);
            }
        }
        else
        {
            //add 4 dummy features set to zero
            dayStats.add(0.0);
            dayStats.add(0.0);
            dayStats.add(0.0);
            dayStats.add(0.0);
        }
        return dayStats.toArray(new Double[dayStats.size()]);
    }

    public Double[] FeatureGenerate_FFT_Double(ArrayList<Double> data, double fps)
    {
        ArrayList<Double> dayStats = new ArrayList<Double>();

        ArrayList<Double> dayFlat_ = data;// FlattenTimeSlots();
        Double[] dayFlat = dayFlat_.toArray(new Double[dayFlat_.size()]);
        int pow2Size = Math.min(8192, pow2(dayFlat.length / 2));

        ArrayList<double[]> mfss = new ArrayList<double[]>();
        double count = 0;
        for (int l = 0; l < dayFlat_.size(); l += pow2Size)
        {
            Double[] window = GetWindowDouble(dayFlat_, l, pow2Size);
            double[] mfc = AndroidFrame.MFC_AccelArray(window);
            double[] mfc_nrom = AndroidFrame.NormalizeLength(mfc, FFT_NORMALIZE_SIZE);
            mfss.add(mfc_nrom);
            count++;
        }
        double[] mfc_avg = new double[mfss.get(0).length];
        for (int i = 0; i < mfss.size(); i++)
        {
            for (int j = 0; j < mfc_avg.length; j++)
            {
                mfc_avg[j] += mfss.get(i)[j] / (double)mfss.size();
            }
        }


        for (int i = 0; i < mfc_avg.length; i++)
        {
            dayStats.add(mfc_avg[i]);
        }

        return dayStats.toArray(new Double[dayStats.size()]);
    }

    public static Double[] GetWindowDouble(List<Double> data, int index, int size)
    {
        ArrayList<Double> window = new ArrayList<Double>();
        boolean StartMissing = false;
        boolean EndMissing = false;

        int start = index - (size / 2);
        if (start < 0)
        {
            start = 0;
            StartMissing = true;
        }

        int end = index + (size / 2);
        if (end >= data.size())
        {
            end = data.size() - 1;
            EndMissing = true;
        }

        for (int i = start; i < end; i++)
        {
            window.add(data.get(i));
        }

        if (StartMissing)
        {
            int numMissing = size - window.size();
            Double firstFrame = window.get(0);
            for (int i = 0; i < numMissing; i++)
            {
                window.add(0, firstFrame);
            }
        }
        else if (EndMissing)
        {
            int numMissing = size - window.size();
            Double lastFrame = window.get(window.size() - 1);
            for (int i = 0; i < numMissing; i++)
            {
                window.add(lastFrame);
            }
        }
        return window.toArray(new Double[window.size()]);
    }


}
