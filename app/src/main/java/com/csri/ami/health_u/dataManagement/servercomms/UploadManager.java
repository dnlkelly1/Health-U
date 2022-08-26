package com.csri.ami.health_u.dataManagement.servercomms;

import android.content.Context;
import android.os.Environment;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.LocationAnalyser;
import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.MotionFileProcessor;
import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.SoundFileProcessor;
import com.csri.ami.health_u.dataManagement.record.SensorRecorder;
import com.csri.ami.health_u.ui.SummaryDataUploadAlarm;

import java.io.File;

/**
 * Created by daniel on 08/10/2015.
 */
public class UploadManager
{
    SummaryDataUploadAlarm uploadAlarmManager;
    Context t;

    public String soundResultStatus;
    public String motionResultStatus;
    public String locationResultStatus;

    MotionAnalysisUpload motion;
    LocationAnalysisUpload location;
    SoundAnalysisUpload sound;

    public static boolean IsDataAvailable()
    {
        boolean fileFound = false;

        File f = Environment.getExternalStorageDirectory();
        String fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER;

        File motionFile = new File(fullfilename, MotionFileProcessor.MOTION_FEATURES_SAVE_FILE_NAME_FOR_UPLOAD);

        File soundFile = new File(fullfilename, SoundFileProcessor.SOUND_FEATURES_SAVE_FILE_NAME_FOR_UPLOAD);

        File locationFile = new File(fullfilename, LocationAnalyser.LOCATION_FEATURES_SAVE_FILE_NAME_FOR_UPLOAD);

        if(motionFile.exists() || soundFile.exists() || locationFile.exists())
        {
            fileFound = true;
        }

        return fileFound;
    }

    public UploadManager(Context context,SummaryDataUploadAlarm summaryDataUploadAlarm)
    {
        t = context;
        uploadAlarmManager = summaryDataUploadAlarm;
        sound = new SoundAnalysisUpload(context,this);
        sound.execute();

    }

    public void SoundFinised(String status)
    {
        if(status.compareTo(ConnectionInfo.STATUS_SUCCESS) == 0)
        {
            sound.Finish();
        }
        soundResultStatus = status;

        motion = new MotionAnalysisUpload(t,this);
        motion.execute();
    }

    public void MotionFinised(String status)
    {
        if(status.compareTo(ConnectionInfo.STATUS_SUCCESS) == 0)
        {
            motion.Finish();
        }
        motionResultStatus = status;

        location = new LocationAnalysisUpload(t,this);
        location.execute();
    }

    public void LocationFinised(String status)
    {
        if(status.compareTo(ConnectionInfo.STATUS_SUCCESS) == 0)
        {
            location.Finish();
        }
        locationResultStatus = status;

        if(uploadAlarmManager != null)
        {
           uploadAlarmManager.UploadAttemptCompleted(t);
        }
    }
}
