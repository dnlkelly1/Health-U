package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis;

import android.util.Log;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques.TimeSyncTable;

import org.joda.time.DateTime;

import java.util.ArrayList;

/**
 * The MotionSensor class will record and store raw motion data in a file.
 * At the end of each day, this raw motion data will be processed by the MotionFileProcessor
 *
 * Created by daniel kelly on 02/10/2015.
 */
public class MotionFileProcessor
{
    public static String MOTION_FEATURES_SAVE_FILE_NAME_FOR_UPLOAD = "TimeSlotFeatures_Motion.csv";
    public static String MOTION_FEATURES_SAVE_FILE_NAME_FOR_GRAPHS = "GraphFeatures_Motion.csv";
    int FPS = 50;

    int overallCount = 0;
    DateTime lastTime;
    DateTime previousOLEDate = null;
    public static int SAMPLES_PER_DAY = 24;
    public static int SECONDS_PER_TIMESLOT = (60 * 60 * 24)/SAMPLES_PER_DAY;
    public SignalStatsBuilder stats;

    public void MotionFileProcessor()
    {

    }

    /**
     * This is the main method of this class. It will load the raw motion data and iteratively process batches of the file
     * Stats are computed for each window of raw data using the SignalStatsBuilder and SignalStats classes
     * @param folder
     * @param timeTable
     */
    public void AnalyzeMotionFile(String folder,TimeSyncTable timeTable)
    {
        stats = new SignalStatsBuilder(SAMPLES_PER_DAY);
        int FramesPerPass = 800;

        int offset = 0;

        MotionFile mf = new MotionFile();
        mf.Init_LoadRawData_Iterative(folder);

        ArrayList<AndroidFrame> data = new ArrayList<AndroidFrame>();

        long totalSize = 0;
        mf.LoadData_Iterative(FramesPerPass, folder + "\\log.txt");
        totalSize += mf.LoadData_Iterative_totalSize;

        int frameSkip = 100;
        double lastFastFPS =0;

        data = mf.LoadData_Iterative_data;
        if (data.size() > 0)
        {
            boolean slowSensingStarted = false;
            double slowSensingStartTime = 0;
            DateTime slwoSensingStartTime_joda=null;

            //double avg_peakFPS = 0;
            double peakFPScount = 0;


            int previouisWindow=-1;
            int currentWindow=-1;
            while (data.size() > 0 && offset >= 0)
            {
                boolean allowNewReadCycle = data.size() == FramesPerPass;
                int lastIndex = 0;
                int count = 0;

                int indexOfNextFeature = -1;

                double FPS_current = ComputeFPS(4, data, offset);
                int windowsSeconds = 2;
                frameSkip = (int)Math.round(FPS_current * (double)windowsSeconds);

                if(frameSkip > 200)
                {
                    frameSkip = 200;
                }


                for (int i = offset; i < (data.size() - frameSkip) /*/ 4) * 3*/; i += frameSkip)
                {
                    if(i < 0 || i > data.size())
                    {
                        int error =1;
                    }
                    overallCount++;

                    double FPS_2 = ComputeFPS(2, data, i);
                    double FPS_4 = ComputeFPS(4, data, i);

                    //if current index has consistent frame rate above zero...process with processing
                    if (FPS_4 > 0 && FPS_2 > 0 && i > 0 && i < data.size())
                    {
                        DateTime currentTime = TimeSyncTable.UnixTimeToDateTime(data.get(i).EventTimeStamp());

                        double secondsSinceLastFrame =0;
                        if(lastTime != null) {
                            secondsSinceLastFrame = (double)(currentTime.getMillis() - lastTime.getMillis())/(double)1000;
                        }

                        if(secondsSinceLastFrame > windowsSeconds * 2)
                        {
                            secondsSinceLastFrame = windowsSeconds;
                        }

                        lastTime = currentTime;

                        currentWindow = stats.GetCurrentTimeSlotIndex(lastTime,SAMPLES_PER_DAY);

                        double slowDuration = -1;

                        boolean savedAsSlow = data.get(i).SetToSlow;
                        if(savedAsSlow && slowSensingStartTime ==0)
                        {
                            slwoSensingStartTime_joda = TimeSyncTable.UnixTimeToDateTime(data.get(i).EventTimeStamp());
                            slowSensingStartTime = data.get(i).EventTimeStamp();
                            slowSensingStarted = true;
                        }

                        boolean goingFast = !savedAsSlow;//FPS_4 > 45..(48)

                        if(goingFast){
                            stats.UpdateMovementTime(currentWindow,secondsSinceLastFrame);
                        }
                        else{
                            stats.UpdateStationaryTime(currentWindow,secondsSinceLastFrame);
                        }


                        double fpsAvg = (FPS_2 + FPS_4)/2;
                        double steadyDiff_fluc = fpsAvg * 0.02;
                        double steadyDiff = fpsAvg * 0.01;

                        if ((!slowSensingStarted && Math.abs(FPS_4 - FPS_2) < steadyDiff_fluc && goingFast) // if currently in fast sensning mode...allow some fluctuations in FPS..i.e. (FPS_4 - FPS_4)<2
                                || (slowSensingStarted && Math.abs(FPS_4 - FPS_2) < steadyDiff && goingFast)) // however, if currently in slow mode, sensor must get to a stable fast state before being defined "fast"
                        {
                            lastFastFPS = FPS_4;
                            //handle change in frame rate
                            if (slowSensingStarted)
                            {
                                double slowSensingEndTime = data.get(i).EventTimeStamp();
                                slowDuration = slowSensingEndTime - slowSensingStartTime;
                                if (slowDuration < 0 || slowDuration > (60 * 60 * 2)) {
                                    slowDuration = -1;
                                }
                                slowSensingStarted = false;
                            }
                            peakFPScount++;

                            int window = (int)FPS_4 * windowsSeconds;
                            //Caclulate stats for current batch of data
                            stats.UpdateStatistics(lastTime, i, window, data, previousOLEDate, SAMPLES_PER_DAY, FPS_4, 1, slowDuration,currentWindow,previouisWindow);
                            previousOLEDate = lastTime;

                        }
                        else
                        {
                            if (peakFPScount > 0 && data.get(i).EventTimeStamp() - data.get(i - 1).EventTimeStamp() < 2)
                            {


                                if (!slowSensingStarted)
                                {
                                    slwoSensingStartTime_joda = TimeSyncTable.UnixTimeToDateTime(data.get(i).EventTimeStamp());
                                    slowSensingStartTime = data.get(i).EventTimeStamp();
                                    slowSensingStarted = true;

                                }

                            }
                            else if (peakFPScount > 0)
                            {
                                slowSensingStarted = false;
                            }

                            CheckEndOfTimeSlot(slowSensingStarted, currentWindow, previouisWindow, slwoSensingStartTime_joda);
                            if(CheckEndOfTimeSlot_slwoSensingStartTime_joda != null)
                            {
                                slowSensingStartTime = CheckEndOfTimeSlot_slowSensingStartTime;
                                slwoSensingStartTime_joda = CheckEndOfTimeSlot_slwoSensingStartTime_joda;
                            }
                        }
                    }
                    else
                    {
                        //otherwise default to 5FPS...which is commonly the slowest FPS setting
                        FPS_4 = 5;
                    }

                    frameSkip = (int)FPS_4 * windowsSeconds;
                    if(frameSkip > 200)
                    {
                        frameSkip = 200;
                    }
                    lastIndex = i + frameSkip;

                    previouisWindow = currentWindow;


                }


                int removeAmount = FramesPerPass / 2;
                offset = lastIndex - 1 - removeAmount;
                if(offset <0 || offset > FramesPerPass)
                {
                    double FPS_4 = ComputeFPS(4, data, lastIndex);
                    int x =5;
                    int y = x+1;
                }
                for (int i = 0; i < removeAmount && i < data.size(); i++)
                {
                    data.remove(0);
                }
                // offset = removeAmount;
                Log.i("Analaysis Alarm", "Read Cycle bytes:" + totalSize + " " + lastTime.toString("HH:mm dd/MM/yyyy"));

                //after processing for previous batch is complete, check if new read from file is needed for next batch
                if (allowNewReadCycle)
                {
                    long size = 0;
                    //load next batch of data
                    mf.LoadData_Iterative(FramesPerPass,folder + "\\log.txt");
                    data = mf.LoadData_Iterative_data;
                    size = mf.LoadData_Iterative_totalSize;

                    if(data.size() < FramesPerPass)
                    {
                        int x =5;
                        int y = x+1;
                    }

                    totalSize += size;
                }
                else
                {
                    break;
                }
            }
        }
        stats.FinishProcessingCurrentDay(previousOLEDate, SAMPLES_PER_DAY);
        mf.Close_File();

    }

    private double CheckEndOfTimeSlot_slowSensingStartTime;
    private DateTime CheckEndOfTimeSlot_slwoSensingStartTime_joda;
    private void CheckEndOfTimeSlot(boolean slowSensingStarted,int currentWindow,int previouisWindow,DateTime slwoSensingStartTime_joda)
    {
        if(slowSensingStarted && currentWindow != previouisWindow && slwoSensingStartTime_joda != null && previouisWindow != -1)
        {
            int minutesPerWindow = (60 * 24)/SAMPLES_PER_DAY;
            int minutesPassedUntillEndOfPreviousWindow = ((previouisWindow + 1) * minutesPerWindow);

            DateTime startOfDay = slwoSensingStartTime_joda.withTimeAtStartOfDay();
            DateTime timeAtEndOfPreviousWindow = startOfDay.plusMinutes(minutesPassedUntillEndOfPreviousWindow);

            double secondsDiff = (timeAtEndOfPreviousWindow.getMillis() - slwoSensingStartTime_joda.getMillis())/1000;

            CheckEndOfTimeSlot_slowSensingStartTime = TimeSyncTable.DateTimeToUnixTime(timeAtEndOfPreviousWindow);
            CheckEndOfTimeSlot_slwoSensingStartTime_joda = timeAtEndOfPreviousWindow;

            stats.UpdateStatistics(lastTime,previousOLEDate,SAMPLES_PER_DAY,secondsDiff,currentWindow, previouisWindow);
            previousOLEDate = lastTime.minusSeconds((int)secondsDiff);// TimeSyncTable.DateTimeToUnixTime(lastTime);//int)lastTime.ToOADate();

        }
        else
        {
            CheckEndOfTimeSlot_slwoSensingStartTime_joda = null;

        }
    }


    /**
     * Calculate number of frames recorded per second for a specific window of motion data
     * @param seconds: size of window in seconds
     * @param data: raw motion data
     * @param frameIndex: index of where in raw data should accessed for FPS
     * @return
     */
    public double ComputeFPS(int seconds, ArrayList<AndroidFrame> data, int frameIndex)
    {
        double return_AvgFPS =0;



        int framesInFiveSeconds_Forward = GetNumberFramesInXSecond_Forward(frameIndex, data, (double)seconds / (double)2);
        double actualSeconds_Fwd = GetNumberFramesInXSecond_Forward_Seconds;

        int framesInFiveSeconds_Backward = GetNumberFramesInXSecond_Backward(frameIndex, data, (double) seconds / (double) 2);
        double actualSeconds_Bwd = GetNumberFramesInXSecond_Backward_Seconds;

        int total_Frames = framesInFiveSeconds_Backward + framesInFiveSeconds_Forward;
        double total_Time = actualSeconds_Bwd + actualSeconds_Fwd;
        double AvgFPS = ((double)total_Frames / total_Time);

        double fps_left_window =  (double)framesInFiveSeconds_Backward / (double)actualSeconds_Bwd;
        double fps_right_window = (double) framesInFiveSeconds_Forward / (double)actualSeconds_Fwd;

        if(GetNumberFramesInXSecond_Backward_Valid && GetNumberFramesInXSecond_Forward_Valid && AvgFPS < 500)
        {
            return_AvgFPS = AvgFPS;
        }
        else if(GetNumberFramesInXSecond_Forward_Valid && !GetNumberFramesInXSecond_Backward_Valid && fps_right_window < 500)
        {
            return_AvgFPS = fps_right_window;
        }
        else if(!GetNumberFramesInXSecond_Forward_Valid && GetNumberFramesInXSecond_Backward_Valid && fps_left_window < 500)
        {
            return_AvgFPS = fps_left_window;
        }
        else
        {
            if(frameIndex >0 && frameIndex < data.size())
            {
                double start = data.get(frameIndex).EventTimeStamp();
                double prev = data.get(frameIndex - 1).EventTimeStamp();
                double next = data.get(frameIndex + 1).EventTimeStamp();

                double diffPrev = start - prev;
                double diffNext = next - start;
                double diffAvg = (diffPrev + diffNext) / 2;

                if(diffPrev < 0.75 && diffNext < 0.75)
                {
                    double fpsDiff = 1/ diffAvg;
                    if(fpsDiff < 500)
                    return_AvgFPS = fpsDiff ;
                }
                else if(diffPrev < 0.75 && diffNext > 0.75)
                {
                    double fpsDiff = 1/ diffPrev;
                    if(fpsDiff < 500)
                    return_AvgFPS = fpsDiff ;
                }
                else if(diffPrev > 0.75 && diffNext < 0.75)
                {
                    double fpsDiff = 1/ diffNext;
                    if(fpsDiff < 500)
                    return_AvgFPS = fpsDiff ;
                }

            }
        }

        return return_AvgFPS;


    }

    public double GetNumberFramesInXSecond_Forward_Seconds;
    public boolean GetNumberFramesInXSecond_Forward_Valid;
    public int GetNumberFramesInXSecond_Forward(int startFrame, ArrayList<AndroidFrame> data, double X_seconds)
    {
        GetNumberFramesInXSecond_Forward_Valid = true;
        GetNumberFramesInXSecond_Forward_Seconds = 0;
        double start = data.get(startFrame).EventTimeStamp();


        DateTime startT = TimeSyncTable.UnixTimeToDateTime(data.get(startFrame).EventTimeStamp());
        double current = data.get(startFrame).EventTimeStamp();
        int count = 0;
        int index = startFrame;

        int prev = index;

        while (current - start < X_seconds && index < data.size() - 1)
        {
            count++;
            index++;
            current = data.get(index).EventTimeStamp();
            GetNumberFramesInXSecond_Forward_Seconds = current - start;

            double timeDiffLastFrame = data.get(index).EventTimeStamp() - start;

            DateTime t1 = TimeSyncTable.UnixTimeToDateTime(data.get(index).EventTimeStamp());

            double diff = startT.getMillis() - t1.getMillis();

            if (GetNumberFramesInXSecond_Forward_Seconds < 0 || timeDiffLastFrame > X_seconds)
            {

                double previous = data.get(index - 1).EventTimeStamp();

                count--;
                GetNumberFramesInXSecond_Forward_Seconds = previous - start;

                double diffCurrent = current - start;
                if(Math.abs(diffCurrent - GetNumberFramesInXSecond_Forward_Seconds) > X_seconds * 0.75)
                {
                    GetNumberFramesInXSecond_Forward_Valid = false;
                }
                break;
            }
        }


        return count + 1;
    }

    public double GetNumberFramesInXSecond_Backward_Seconds;
    public boolean GetNumberFramesInXSecond_Backward_Valid;
    public int GetNumberFramesInXSecond_Backward(int startFrame, ArrayList<AndroidFrame> data, double X_seconds)
    {
        GetNumberFramesInXSecond_Backward_Valid = true;
        double start = data.get(startFrame).EventTimeStamp();
        double current = data.get(startFrame).EventTimeStamp();
        int count = 0;
        int index = startFrame;
        GetNumberFramesInXSecond_Backward_Seconds =0;
        int prev = index;

        while (start - current < X_seconds && index > 0)
        {
            count++;
            index--;
            current = data.get(index).EventTimeStamp();
            GetNumberFramesInXSecond_Backward_Seconds = start - current;

            double timeDiffLastFrame = start - data.get(index).EventTimeStamp();// data[index + 1].EventTimeStamp - data[index].EventTimeStamp;
            if (GetNumberFramesInXSecond_Backward_Seconds < 0 || timeDiffLastFrame > X_seconds)
            {
                double previous = data.get(index + 1).EventTimeStamp();
                count--;
                GetNumberFramesInXSecond_Backward_Seconds = start - previous;

                double currentDiff = start - current;
                if(Math.abs(currentDiff - GetNumberFramesInXSecond_Backward_Seconds) > X_seconds * 0.75)
                {
                    GetNumberFramesInXSecond_Backward_Valid = false;
                }

                break;
            }
        }


        return count;
    }


}
