package com.csri.ami.health_u.ui;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.FileAnalyzer;

import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by daniel on 01/10/2015.
 */
public class RawDataAnalysisAlarm extends BroadcastReceiver
{
    public static String PREFS_DATAPROCESS = "dataprocessprefs";
    public static String PREFS_DATAPROCESS_LASTTIME = "dataprocesslasttime";
    public static String PREFS_DATAPUPLOAD_LASTTIME = "datauploadlasttime";
    //public static String PREFS_UPLOAD_DATA_LOCK = "uploaddataprefsLOCK";



    @Override
    public void onReceive(Context context,Intent intent)
    {
        //double b = BatteryLevelReceiver.batteryLevel(context);
        if((isPluggedIn(context) && !isScreenOn(context)) || (BatteryLevelReceiver.batteryLevel(context) > 0.2 && !isScreenOn(context)))
        {
            //CancelAlarm(context);
            boolean serviceRunning = isMyServiceRunning(context);
            if(!serviceRunning)
            {
                Log.i("Analaysis Alarm", "Entering OnRecieveAlarm");
                Intent backgroundServiceIntent = new Intent(context, FileAnalyzer.class);
                context.startService(backgroundServiceIntent);
                SetAnalysisAlarmStartOfDayExplicit(context);
            }



        }
        else
        {
            SetAnalysisAlarm(context,1000 * 60 * 15);//30 minutes time...try again
        }
        //do analysis here
    }

    public static boolean isMyServiceRunning(Context context)
    {

        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.csri.ami.health_u.sensors.analyze.postProccessing.FileAnalyzer".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void SetAnalysisAlarmStartOfDayExplicit(Context context)
    {

        CancelAlarm(context);
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(context,RawDataAnalysisAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

        //TimeZone tz = TimeZone.getDefault();
        //int timeZoneOffsetMillis= tz.getOffset(DateTime.now().getMillis());


        DateTime time = DateTime.now();
        long elapsedMillis = SystemClock.elapsedRealtime();

        DateTime timeStartOfDay = time.withTimeAtStartOfDay();
        long clockMillis = time.getMillis();

        long offset = clockMillis- elapsedMillis;

        DateTime nextDay = timeStartOfDay.plusDays(1);
        //long nextDayMillis = nextDay.getMillis();

        //DateTime testTime = nextDay;

        long nextDayMillis_offset = nextDay.getMillis() - offset;



        am.set(AlarmManager.ELAPSED_REALTIME, nextDayMillis_offset /*milliseconsTomorrow*/, pi);




        //CancelAlarm(context);
    }


    public void SetAnalysisAlarmStartOfDay(Context context)
    {
        DateTime lastProcessTime = GetPreferencesDataLastProcessed(context);

        DateTime now = DateTime.now();


        boolean lastProcessWasLongTimeAgo = false;
        if(lastProcessTime != null)
        {
            int hoursDiff = Hours.hoursBetween(lastProcessTime, now).getHours();
            if(hoursDiff > 24)
            {
                lastProcessWasLongTimeAgo = true;
            }
        }


        CancelAlarm(context);
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(context,RawDataAnalysisAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

        //TimeZone tz = TimeZone.getDefault();
        //int timeZoneOffsetMillis= tz.getOffset(DateTime.now().getMillis());






        if(lastProcessWasLongTimeAgo)
        {
            DateTime time = DateTime.now();
            long elapsedMillis = SystemClock.elapsedRealtime();


            long clockMillis = time.getMillis();

            long offset = clockMillis- elapsedMillis;

            DateTime next = now.plusSeconds((int)(2));


            long millis_offset = next.getMillis() - offset;



            am.set(AlarmManager.ELAPSED_REALTIME, millis_offset /*milliseconsTomorrow*/, pi);
        }
        else
        {
            DateTime time = DateTime.now();
            long elapsedMillis = SystemClock.elapsedRealtime();

            DateTime timeStartOfDay = time.withTimeAtStartOfDay();
            long clockMillis = time.getMillis();

            long offset = clockMillis- elapsedMillis;

            DateTime nextDay = timeStartOfDay.plusDays(1);
            //long nextDayMillis = nextDay.getMillis();

            //DateTime testTime = nextDay;

            long nextDayMillis_offset = nextDay.getMillis() - offset;



            am.set(AlarmManager.ELAPSED_REALTIME, nextDayMillis_offset /*milliseconsTomorrow*/, pi);
        }



        //CancelAlarm(context);
    }

    public void SetAnalysisAlarm(Context context,long millisFromNow)
    {
        CancelAlarm(context);
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(context,RawDataAnalysisAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

        //TimeZone tz = TimeZone.getDefault();
        //int timeZoneOffsetMillis= tz.getOffset(DateTime.now().getMillis());



        DateTime time = DateTime.now();
        long elapsedMillis = SystemClock.elapsedRealtime();

        DateTime now = DateTime.now();
        long clockMillis = time.getMillis();

        long offset = clockMillis- elapsedMillis;

        DateTime next = now.plusSeconds((int) (millisFromNow / 1000));


        long nextDayMillis_offset = next.getMillis() - offset;



        am.set(AlarmManager.ELAPSED_REALTIME, nextDayMillis_offset /*milliseconsTomorrow*/, pi);

        //CancelAlarm(context);
    }

    public void CancelAlarm(Context context)
    {

        {
            Intent i = new Intent(context,RawDataAnalysisAlarm.class);
            PendingIntent sender = PendingIntent.getBroadcast(context, 0, i, 0);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(sender);
            sender.cancel();
        }
    }

    public static boolean isPluggedIn(Context context)
    {
        Intent intent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

    public static  boolean isScreenOn(Context context)
    {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean result= Build.VERSION.SDK_INT>= Build.VERSION_CODES.KITKAT_WATCH&&powerManager.isInteractive()|| Build.VERSION.SDK_INT< Build.VERSION_CODES.KITKAT_WATCH&&powerManager.isScreenOn();
        return result;
    }


    public static DateTime GetPreferencesDataLastProcessed(Context t)
    {
        SharedPreferences pref = t.getSharedPreferences(PREFS_DATAPROCESS, Context.MODE_PRIVATE);
        String state = pref.getString(PREFS_DATAPROCESS_LASTTIME, DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));

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

    public static void SetPreferencesDataLastProcess(Context t,DateTime time)
    {
        SharedPreferences.Editor pref = t.getSharedPreferences(PREFS_DATAPROCESS , Context.MODE_PRIVATE).edit();
        pref.putString(PREFS_DATAPROCESS_LASTTIME, time.toString("yyyy-MM-dd HH:mm:ss"));
        pref.apply();
    }

    public static void CheckPreferencesDataLastProcessInitialized(Context t)
    {
        SharedPreferences pref = t.getSharedPreferences(PREFS_DATAPROCESS, Context.MODE_PRIVATE);
        String state = pref.getString(PREFS_DATAPROCESS_LASTTIME, null);

        if(state == null)
        {
            SharedPreferences.Editor ed = t.getSharedPreferences(PREFS_DATAPROCESS , Context.MODE_PRIVATE).edit();
            ed.putString(PREFS_DATAPROCESS_LASTTIME, DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));
            ed.apply();
        }
    }
}
