package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import com.csri.ami.health_u.dataManagement.analyze.classifiers.Normalize;
import com.csri.ami.health_u.dataManagement.record.SensorRecorder;
import com.csri.ami.health_u.dataManagement.record.sound.SoundFeatureSummary;
import com.csri.ami.health_u.dataManagement.record.sound.SoundFeatures;
import com.csri.ami.health_u.dataManagement.record.sound.Sound_Sensor;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Minutes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import SVM.Model;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

/**
 * SoundFileProcessor loads stored sound features created by SoundSensor
 * Stored features are then classified using a pretrained Support Vector Machine (SVM)
 * The SVM is trained to predict voice, music and ambient sounds based on the sound feature
 * The amount of voice activity is stored as a summary feature to describe user social activity
 * The technique used to train the SVM is described here: https://doi.org/10.1109/TCYB.2015.2396291
 * Created by daniel on 02/10/2015.
 */
public class SoundFileProcessor
{
    public static String SOUND_FEATURES_SAVE_FILE_NAME_FOR_UPLOAD = "TimeSlotFeatures_Sound.csv";
    public static String SOUND_FEATURES_SAVE_FILE_NAME_FOR_GRAPH = "GraphFeatures_Sound.csv";

    svm_model model;
    Normalize norm;
    private static String SVM_FILE = "SVM";

    int overallCount = 0;
    Context t;

    int numberTimeSlots = MotionFileProcessor.SAMPLES_PER_DAY;
    int previousWindowIndex = -1;
    DateTime previousTime = null;

    ArrayList<ArrayList<SoundFeatureSummary>> timeSlotResultsRaw = new ArrayList<ArrayList<SoundFeatureSummary>>();
    SoundFeatureSummary[] timeSlotResultsAvg = new SoundFeatureSummary[numberTimeSlots];

    /**
     * Init Class and Load pre-trained SVM files and feature scaler
     * @param context
     */
    public SoundFileProcessor(Context context)
    {
        t = context;
        AssetManager am = context.getAssets();

        try
        {
            for(int i=0;i<numberTimeSlots;i++)
            {
                timeSlotResultsRaw.add(new ArrayList<SoundFeatureSummary>());
            }

            String[] modelList = am.list("");
            if(FileExists(modelList, SVM_FILE + 0 + "_" + 0 + ".txt"))
            {
                InputStream is = am.open(SVM_FILE + 0 + "_" + 0 + ".txt");
                svm_model current = Model.Read(is);//SVM_FILE + i + "_" + svm_id + ".txt");
                model = current;

            }
            norm = new Normalize(am);

        }
        catch (IOException ex)
        {

        }


    }

    /**
     * Load sound features file (created by SoundSensor)
     * Process each window of sound to generate a sound feature
     * Each feature is classified as ['Voice','Music','Ambient'] using an SVM
     * Sound type predictions are then stored as user summary of social activity
     * @param file
     */
    public void Process(String file)
    {
        SharedPreferences prefs = t.getSharedPreferences(Sound_Sensor.SOUND_STORED_PREFS, Context.MODE_PRIVATE);
        double currentTotal = prefs.getFloat(Sound_Sensor.SOUND_TOTAL_RMS, 0);
        double currentCurrentCount = prefs.getFloat(Sound_Sensor.SOUND_COUNT_RMS, 0);
        double currentMinRMS = prefs.getFloat(Sound_Sensor.SOUND_MIN_RMS,1200);

        double avg = currentTotal / currentCurrentCount;
        double diff = avg - currentMinRMS;
        double threshold = currentMinRMS + (diff * 0.5);

        SoundFile sf = new SoundFile();
        sf.Init_LoadRawData_Iterative(file );

        SoundFeatures previousFrame = sf.LoadFrame_Iterative();
        int blocks=0;
        while(previousFrame != null)
        {
            ArrayList<SoundFeatures> currentBlock = new ArrayList<>();
            currentBlock.add(previousFrame);
            SoundFeatures currentFrame = sf.LoadFrame_Iterative();


            while (currentFrame != null && ( currentFrame.timestamp - previousFrame.timestamp) < 2)
            {
                currentBlock.add(currentFrame);

                previousFrame = currentFrame;

                currentFrame = sf.LoadFrame_Iterative();
            }
            blocks++;
            Log.i("Analaysis Alarm", "Sound blocks read:" + blocks);

            //process currentblock and convert to feature summary array
            SoundFeatures[] currentWindow = currentBlock.toArray(new SoundFeatures[currentBlock.size()]);
            ArrayList<SoundFeatureSummary> soundSummaries = SoundFeatures.ConvertSoundFeaturesToFeatureSummarys(currentWindow,true,0);

            //scale the feature vector summary
            SoundFeatureSummary.PerformNormalize(soundSummaries, norm);


            double avgRMS =0;
            double count=0;
            double voiceCount =0;

            for(int i=0;i<soundSummaries.size();i++)
            {
                SoundFeatureSummary current = soundSummaries.get(i);
                avgRMS += current.AverageRMS;
            }
            avgRMS = avgRMS / (double)soundSummaries.size();

            double varRMS =0;
            for(int i=0;i<soundSummaries.size();i++)
            {
                SoundFeatureSummary current = soundSummaries.get(i);
                varRMS += Math.pow(current.AverageRMS - avgRMS,2);
            }
            //varRMS = Math.sqrt( varRMS / (double)soundSummaries.size());



            //for each sound summary, classify it using a set of binary svms
            for(int i=0;i<soundSummaries.size();i++)
            {
                SoundFeatureSummary current = soundSummaries.get(i);

                if(current.AverageRMS > threshold)
                {
                    count++;


                    svm_node[] svm_f = new svm_node[current.featureVector.length];
                    for(int j=0;j<svm_f.length;j++)
                    {
                        svm_f[j] = new svm_node();
                        svm_f[j].index = j;
                        svm_f[j].value = current.featureVector[j];
                    }
                    double[] svm_probs = new double[2];
                    svm.svm_predict_probability(model,svm_f,svm_probs);

                    double probThresh = 0.925;
                    double prob = svm_probs[0];

                    if(prob > probThresh)
                    {
                        voiceCount++;
                    }
                }
            }

            //summarise results of SVM classifcation and store results
            if(soundSummaries.size() > 0)
            {
                double[] features = new double[3];
                features[0] = avgRMS / (double)soundSummaries.size();
                if(count > 0) {
                    features[1] = voiceCount / count;
                }
                features[2] = count / (double)soundSummaries.size();


                StoreResults(soundSummaries.get(0).Timestamp, features);
            }

            previousFrame = currentFrame;

            //previousFrame = sf.LoadFrame_Iterative();
        }

        CompleteProcessingDay();
        sf.Close_File();
    }



    private void CompleteProcessingDay()
    {
        for(int i=0;i<timeSlotResultsRaw.size();i++)
        {
            if(timeSlotResultsRaw.get(i).size() > 0)
            {
                ArrayList<SoundFeatureSummary> current = timeSlotResultsRaw.get(i);
                DateTime time = current.get(current.size()-1).Timestamp;
                DateTime st = time.withTimeAtStartOfDay();

                int secondsBetweenStartOfDayAndStartOfWindow = MotionFileProcessor.SECONDS_PER_TIMESLOT * previousWindowIndex;

                int secondsElapsedDay = (int)(time.getMillis() - st.getMillis())/1000;
                int secondsElapsedInTimeSlot = secondsElapsedDay - secondsBetweenStartOfDayAndStartOfWindow;


                if(secondsElapsedInTimeSlot > MotionFileProcessor.SECONDS_PER_TIMESLOT / 2)
                {

                    SoundFeatureSummary avg = SoundFeatureSummary.Average(current);
                    //timeSlotResultsAvg[i] =avg ;


                    SaveTimeSlotFeatures_SingleSlot(avg.featureVector, avg.Timestamp, i);
                }

                timeSlotResultsRaw.get(i).clear();
            }

        }

        //SaveTimeSlotFeatures();

        timeSlotResultsAvg = new SoundFeatureSummary[numberTimeSlots];
        previousWindowIndex = -1;
    }

    int MINUTES_PER_DAY = 60 * 24;
    private void StoreResults(DateTime time,double[] data)
    {
        if (previousTime != null)
        {
            DateTime currentOLDData = time;
            int daysDiff = Days.daysBetween(previousTime.withTimeAtStartOfDay(),time.withTimeAtStartOfDay() ).getDays();
            if (daysDiff > 0 && daysDiff < 30)
            {

                CompleteProcessingDay();


                previousTime = time;

            }
            else if (previousTime == null)
            {
                previousTime = time;
            }


        }

        DateTime st = time.withTimeAtStartOfDay();
        int minutesElapsed = Minutes.minutesBetween(st, time).getMinutes();
        int minutesPerDay = MINUTES_PER_DAY ;// Hours.hours(24).toStandardMinutes().getMinutes();


        double window_minutes_span = (double)minutesPerDay / (double)numberTimeSlots;


        int window_index = (int)((double)minutesElapsed / window_minutes_span);

        if(window_index >= 0 && window_index < timeSlotResultsRaw.size())
        {
            timeSlotResultsRaw.get(window_index).add(new SoundFeatureSummary(time, data));
        }

        if(previousWindowIndex != -1 && window_index != previousWindowIndex)
        {
            if(timeSlotResultsRaw.get(previousWindowIndex).size() > 0)
            {
                int secondsBetweenStartOfDayAndStartOfWindow = MotionFileProcessor.SECONDS_PER_TIMESLOT * previousWindowIndex;

                int secondsElapsedDay = (int)(time.getMillis() - st.getMillis())/1000;
                int secondsElapsedInTimeSlot = secondsElapsedDay - secondsBetweenStartOfDayAndStartOfWindow;


                if(secondsElapsedInTimeSlot > MotionFileProcessor.SECONDS_PER_TIMESLOT / 2)
                {
                    SoundFeatureSummary avg = SoundFeatureSummary.Average(timeSlotResultsRaw.get(previousWindowIndex));
                    //timeSlotResultsAvg[previousWindowIndex] = avg;

                    SaveTimeSlotFeatures_SingleSlot(avg.featureVector, time, previousWindowIndex);
                }


                timeSlotResultsRaw.get(previousWindowIndex).clear();

            }
        }

        previousWindowIndex = window_index;
        previousTime = time;

    }




    public void SaveTimeSlotFeatures_SingleSlot(double[] data,DateTime time,int timeslot)
    {
        File f = Environment.getExternalStorageDirectory();
        String fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER;

        File fileToWrite = new File(fullfilename, SOUND_FEATURES_SAVE_FILE_NAME_FOR_UPLOAD);

        File fileToWrite_g = new File(fullfilename, SOUND_FEATURES_SAVE_FILE_NAME_FOR_GRAPH);
        try
        {
            FileOutputStream fos = new FileOutputStream(fileToWrite, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos);

            FileOutputStream fos_g = new FileOutputStream(fileToWrite_g, true);
            OutputStreamWriter osw_g = new OutputStreamWriter(fos_g);

            //for(int i=0;i<timeSlotResultsAvg.length;i++)
            {
                if(data != null)
                {
                    //date,timeslotpercent,RMS,voice/loud,loud/total
                    String line = "";
                    DateTime date = time;
                    line += date.toString("yyyy-MM-dd HH:mm:ss") +  ",";
                    double timeslotAsPercent = (double) timeslot / (double) timeSlotResultsAvg.length;
                    line += timeslotAsPercent + ",";
                    for (int j = 0; j < data.length; j++)
                    {
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

    private boolean FileExists(String[] assets,String modelName)
    {
        boolean found = false;
        if (assets != null)
        {
            for (int i = 0; i < assets.length; i++) {
                if (assets[i].compareTo(modelName) == 0) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }



}
