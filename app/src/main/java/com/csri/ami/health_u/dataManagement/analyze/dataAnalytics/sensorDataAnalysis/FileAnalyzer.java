package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;

import com.csri.ami.health_u.dataManagement.record.SensorRecorder;
import com.csri.ami.health_u.ui.RawDataAnalysisAlarm;
import com.csri.ami.health_u.ui.SummaryDataUploadAlarm;

import org.joda.time.DateTime;

import java.io.File;

/**
 * Background Service class to manage process of all sensor files
 * This is a manager class to process the motion, sound and location files recorded
 * by the SensorRecorder class (via the MotionSensor, SoundSensor, Wifi_Sensor and GPS_Sensor classes)
 * Created by daniel on 29/09/2015.
 */
public class FileAnalyzer extends Service
{


    public FileAnalyzer()
    {

    }

    public void onCreate()
    {
        super.onCreate();

    }

    /**
     * Initiate the ProcessFiles method as a background service
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ProcessFiles();
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startId)
    {

    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    /**
     * Process the Motion, Sound and Location files in turn
     */
    private void ProcessFiles()
    {
        File f = Environment.getExternalStorageDirectory();
        String fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER;


        boolean rename = true;

        ProcessMotionFiles(fullfilename, rename);

        ProcessSoundFiles(fullfilename, rename);

        ProcessLocation(fullfilename, rename);

        SummaryDataUploadAlarm.SetPreferencesDataReadyToBeUploaded(getApplicationContext(), true);

        RawDataAnalysisAlarm.SetPreferencesDataLastProcess(getApplicationContext(),DateTime.now());
        // SummaryDataUploadAlarm uploadAlarm = new SummaryDataUploadAlarm();

        // uploadAlarm.AttemptUpload(getApplicationContext());

        stopSelf();
    }





    private String tempSoundFileName = "_sound_temp";
    private String mainSoundFileName = "QOL_0sound.csv";
    private void ProcessSound(String folder,String filename, boolean rename,SoundFileProcessor sfp,boolean isTempFile)
    {


        try {
            File s_fileToRead = new File(folder + filename);// "QOL_0sound.csv");

            DateTime now = DateTime.now();
            String timestamp = now.toString("dd:MM:yyyy");

            if (s_fileToRead.exists()) {
                if (rename) {
                    File s_newTempFile = new File(folder + tempSoundFileName + timestamp + ".csv");

                    boolean filesLocked = SensorRecorder.GetRawSensorFileLock(getApplicationContext());
                    while (filesLocked) {
                        try {
                            Thread.sleep((long) 200);
                        } catch (InterruptedException ex) {
                        }
                        filesLocked = SensorRecorder.GetRawSensorFileLock(getApplicationContext());
                    }

                    boolean fileRenamed = false;
                    if (!isTempFile) {
                        SensorRecorder.SetRawSensorFileLock(getApplicationContext(), true);
                        fileRenamed = s_fileToRead.renameTo(s_newTempFile);
                        SensorRecorder.SetRawSensorFileLock(getApplicationContext(), false);
                    }

                    if (fileRenamed) {
                        // File m_fileToDelete = new File(folder + "QOL_0sound.csv");
                        // m_fileToDelete.delete();
                        sfp.Process(s_newTempFile.getAbsolutePath());
                        s_newTempFile.delete();
                    } else {
                        sfp.Process(s_fileToRead.getAbsolutePath());
                        s_fileToRead.delete();
                    }
                } else {
                    boolean filesLocked = SensorRecorder.GetRawSensorFileLock(getApplicationContext());
                    while (filesLocked) {
                        try {
                            Thread.sleep((long) 200);
                        } catch (InterruptedException ex) {
                        }
                        filesLocked = SensorRecorder.GetRawSensorFileLock(getApplicationContext());
                    }

                    SensorRecorder.SetRawSensorFileLock(getApplicationContext(), true);
                    sfp.Process(s_fileToRead.getAbsolutePath());
                    SensorRecorder.SetRawSensorFileLock(getApplicationContext(), false);

                }
            }
        }
        catch (Exception e){}
    }

    private void ProcessSoundFiles(String folder,boolean rename)
    {
        SoundFileProcessor sfp = new SoundFileProcessor(getApplicationContext());
        File f = new File(folder);
        File[] file = f.listFiles();

        for(int i=0;i<file.length;i++)
        {
            String filename = file[i].getName();
            boolean contains = filename.contains(tempSoundFileName);
            if(contains)
            {
                ProcessSound(folder,file[i].getName(),rename,sfp,true);
            }
        }

        ProcessSound(folder,mainSoundFileName,rename,sfp,false);

    }

    private String tempMotionFileName = "_motion_temp";
    private String mainMotionFileName = "QOL_0MotionR.csv";

    private void ProcessMotionFiles(String folder,boolean rename)
    {

        File f = new File(folder);
        File[] file = f.listFiles();

        for(int i=0;i<file.length;i++)
        {
            String filename = file[i].getName();
            boolean contains = filename.contains(tempMotionFileName);
            if(contains)
            {
                ProcessMotion(folder, file[i].getName(),rename,true);
            }
        }

        ProcessMotion(folder, mainMotionFileName, rename, false);

    }

    private void ProcessMotion(String folder,String filename,boolean rename,boolean isTempFile)
    {

        try {
            MotionFileProcessor mfp = new MotionFileProcessor();
            File m_fileToRead = new File(folder + filename);

            DateTime now = DateTime.now();
            String timestamp = now.toString("dd:MM:yyyy");
            if (m_fileToRead.exists()) {
                if (rename) {
                    File m_newTempFile = new File(folder + tempMotionFileName + timestamp + ".csv");
                    boolean filesLocked = SensorRecorder.GetRawSensorFileLock(getApplicationContext());
                    while (filesLocked) {
                        try {
                            Thread.sleep((long) 200);
                        } catch (InterruptedException ex) {
                        }
                        filesLocked = SensorRecorder.GetRawSensorFileLock(getApplicationContext());
                    }

                    boolean fileRenamed = false;
                    if (!isTempFile) {
                        SensorRecorder.SetRawSensorFileLock(getApplicationContext(), true);
                        fileRenamed = m_fileToRead.renameTo(m_newTempFile);
                        SensorRecorder.SetRawSensorFileLock(getApplicationContext(), false);
                    }

                    if (fileRenamed) {
                        mfp.AnalyzeMotionFile(m_newTempFile.getAbsolutePath(), null);
                        m_newTempFile.delete(); //removed this for testing 3/11/15
                    } else {
                        mfp.AnalyzeMotionFile(m_fileToRead.getAbsolutePath(), null);
                        m_fileToRead.delete();
                    }
                } else {
                    boolean filesLocked = SensorRecorder.GetRawSensorFileLock(getApplicationContext());
                    while (filesLocked) {
                        try {
                            Thread.sleep((long) 200);
                        } catch (InterruptedException ex) {
                        }
                        filesLocked = SensorRecorder.GetRawSensorFileLock(getApplicationContext());
                    }

                    SensorRecorder.SetRawSensorFileLock(getApplicationContext(), true);

                    mfp.AnalyzeMotionFile(m_fileToRead.getAbsolutePath(), null);

                    SensorRecorder.SetRawSensorFileLock(getApplicationContext(), false);
                }
            }
        }
        catch (Exception e){}
    }

    private void ProcessLocation(String folder,boolean rename)
    {
        try {
            LocationAnalyser lfp = new LocationAnalyser();

            DateTime now = DateTime.now();
            String timestamp = now.toString("dd:MM:yyyy");

            File m_fileToRead = new File(folder + "QOL_GPS.csv");

            if (m_fileToRead.exists())
            {


                boolean filesLocked = SensorRecorder.GetRawSensorFileLock(getApplicationContext());
                while (filesLocked) {
                    try {
                        Thread.sleep((long) 200);
                    } catch (InterruptedException ex) {
                    }
                    filesLocked = SensorRecorder.GetRawSensorFileLock(getApplicationContext());
                }

                SensorRecorder.SetRawSensorFileLock(getApplicationContext(), true);
                lfp.LoadLocation(m_fileToRead.getAbsolutePath());
                SensorRecorder.SetRawSensorFileLock(getApplicationContext(), false);

                lfp.AnalyzeLocation(3);

            }
        }
        catch (Exception e){}
    }




}
