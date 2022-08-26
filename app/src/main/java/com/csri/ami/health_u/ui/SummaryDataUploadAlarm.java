package com.csri.ami.health_u.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;

import com.csri.ami.health_u.dataManagement.servercomms.UploadManager;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by daniel on 09/10/2015.
 */
public class SummaryDataUploadAlarm extends BroadcastReceiver
{
    private static int SECONDS_DELAY_RETRY = 60 * 15;//set to 15


    public UploadManager up;
    public Context t;


    @Override
    public void onReceive(Context context,Intent intent)
    {
        boolean connected = ConnectivityStatus(context);
        boolean dataReady = UploadManager.IsDataAvailable();// GetPreferencesDataReadyToBeUploaded(context);



        if(connected && dataReady)
        {
            SetPreferencesDataReadyToBeUploaded(context,false);
            up = new UploadManager(context,this);

        }
        else if(!connected && dataReady)
        {
            SetAlarm(context, SECONDS_DELAY_RETRY);//every 30 minutes
        }
        else
        {
            SetAlarmIn1Hour(context);
        }
    }

    public void UploadAttemptCompleted(Context t)
    {
        //upload done for today...now set one for tomorrow
        //SetPreferencesDataLastUploaded(t,DateTime.now());
        SetAlarmIn1Hour(t);
    }

    public static String PREFS_UPLOAD_DATA = "uploaddataprefs";
    public static String PREFS_UPLOAD_DATA_READY = "uploaddataprefsREADY";
    public static String PREFS_UPLOAD_DATA_LOCK = "uploaddataprefsLOCK";

//    public static boolean GetPreferencesDataReadyToBeUploaded(Context t)
//    {
//        SharedPreferences pref = t.getSharedPreferences(PREFS_UPLOAD_DATA, Context.MODE_PRIVATE);
//        boolean state = pref.getBoolean(PREFS_UPLOAD_DATA_READY,true);
//        return state;
//    }

    public static void SetPreferencesDataReadyToBeUploaded(Context t,boolean dataReady)
    {
        SharedPreferences.Editor pref = t.getSharedPreferences(PREFS_UPLOAD_DATA , Context.MODE_PRIVATE).edit();
        pref.putBoolean(PREFS_UPLOAD_DATA_READY, dataReady);
        pref.apply();
    }

    public static boolean ConnectivityStatus(Context context)
    {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
        return isConnected;
    }

    public void SetAlarmIn1Hour(Context context)
    {
        CancelAlarm(context);

        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(context,SummaryDataUploadAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

        DateTime time = DateTime.now();
        long elapsedMillis = SystemClock.elapsedRealtime();

        long clockMillis = time.getMillis();

        long offset = clockMillis- elapsedMillis;


        DateTime t = time.plusMinutes(60);//start in 60 minutes to allow data processing time to finish

        long startOffset = t.getMillis() - offset;



        am.set(AlarmManager.ELAPSED_REALTIME, startOffset/*milliseconsTomorrow*/, pi);
    }


    public void SetAlarm(Context context,int Seconds)
    {
        CancelAlarm(context);

        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(context,SummaryDataUploadAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

        //TimeZone tz = TimeZone.getDefault();
        //int timeZoneOffsetMillis= tz.getOffset(DateTime.now().getMillis());



        DateTime time = DateTime.now();
        long elapsedMillis = SystemClock.elapsedRealtime();

        long clockMillis = time.getMillis();

        long offset = clockMillis- elapsedMillis;


        DateTime t = time.plusSeconds(Seconds);

        long startOffset = t.getMillis() - offset;



        am.set(AlarmManager.ELAPSED_REALTIME, startOffset/*milliseconsTomorrow*/, pi);
    }



    private void CancelAlarm(Context context)
    {

        {
            Intent i = new Intent(context,SummaryDataUploadAlarm.class);
            PendingIntent sender = PendingIntent.getBroadcast(context, 0, i, 0);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(sender);
            sender.cancel();
        }
    }

    public static DateTime GetPreferencesDataLastUploaded(Context t)
    {
        SharedPreferences pref = t.getSharedPreferences(RawDataAnalysisAlarm.PREFS_DATAPROCESS, Context.MODE_PRIVATE);
        String state = pref.getString(RawDataAnalysisAlarm.PREFS_DATAPUPLOAD_LASTTIME, DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));

        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        DateTime time=null;
        try
        {
            time = formatter.parseDateTime(state);


        }
        catch (Exception ex)
        {

        }
        return time;
    }

    public static void SetPreferencesDataLastUploaded(Context t,DateTime time)
    {
        SharedPreferences.Editor pref = t.getSharedPreferences(RawDataAnalysisAlarm.PREFS_DATAPROCESS , Context.MODE_PRIVATE).edit();
        pref.putString(RawDataAnalysisAlarm.PREFS_DATAPUPLOAD_LASTTIME, time.toString("yyyy-MM-dd HH:mm:ss"));
        pref.apply();
    }

    public static void CheckPreferencesDataLastUploadInitialized(Context t)
    {
        SharedPreferences pref = t.getSharedPreferences(RawDataAnalysisAlarm.PREFS_DATAPROCESS, Context.MODE_PRIVATE);
        String state = pref.getString(RawDataAnalysisAlarm.PREFS_DATAPUPLOAD_LASTTIME, null);

        if(state == null)
        {
            SharedPreferences.Editor ed = t.getSharedPreferences(RawDataAnalysisAlarm.PREFS_DATAPROCESS , Context.MODE_PRIVATE).edit();
            ed.putString(RawDataAnalysisAlarm.PREFS_DATAPUPLOAD_LASTTIME, DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));
            ed.apply();
        }
    }

}
