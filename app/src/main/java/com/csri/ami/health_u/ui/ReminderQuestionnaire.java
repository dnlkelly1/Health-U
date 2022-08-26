package com.csri.ami.health_u.ui;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemClock;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.csri.ami.health_u.R;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Random;

/**
 * Created by daniel on 28/10/2015.
 */
public class ReminderQuestionnaire extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context,Intent intent)
    {
        final SharedPreferences prefs = context.getSharedPreferences(InformedConsent.PREFS_INFORMED_CONSENT, Context.MODE_PRIVATE);
        boolean alreadyTaken = prefs.getBoolean(InformedConsent.PREFS_INFORMED_TAKEN_QUESTIONNAIRE, false);

        if(!alreadyTaken)
        {
            Intent showTaskIntent = new Intent(context, Questionnaire.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, showTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            Notification not = new NotificationCompat.Builder(context)
                    .setContentTitle("Reminder: " + context.getResources().getString(R.string.app_name) + " Questionnaire")
                    .setContentText(context.getResources().getString(R.string.questionnaireReminder))
                    .setSound(soundUri)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_local_hospital_white_36dp)
                    .setOngoing(false)
                    .build();


            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            mNotificationManager.notify(1, not);


            SetAlarm(context);
        }
    }

    private static void SetAlarm(Context context)
    {

        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        SharedPreferences prefs = context.getSharedPreferences(InformedConsent.PREFS_INFORMED_CONSENT, Context.MODE_PRIVATE);


        long nextMillis_offset=-1;
        DateTime nextAlarmTime=null;



        DateTime now = DateTime.now();



        int startHour = 18;
        int endHour = 21;
        Random r = new Random();
        int randomH =r.nextInt(endHour - startHour);

        int randomM = r.nextInt(59);

        int hoursAdd = startHour + randomH;


        nextAlarmTime = now.withTimeAtStartOfDay().plusDays(1).plusHours(hoursAdd).plusMinutes(randomM);


        DateTime time = DateTime.now();
        long elapsedMillis = SystemClock.elapsedRealtime();


        long clockMillis = time.getMillis();

        long offset = clockMillis - elapsedMillis;

        nextMillis_offset = nextAlarmTime.getMillis() - offset;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(InformedConsent.PREFS_INFORMED_LAST_QUESTIONNAIRE_REMINDER_TIME, nextAlarmTime.toString("yyyy-MM-dd HH:mm:ss"));
        editor.commit();



        Log.i("Reminder Alarm", "Simple set: " + nextAlarmTime.toString("yyyy-MM-dd HH:mm:ss"));
        Intent i2 = new Intent(context,ReminderQuestionnaire.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i2, PendingIntent.FLAG_UPDATE_CURRENT);
        am.set(AlarmManager.ELAPSED_REALTIME, nextMillis_offset /*milliseconsTomorrow*/, pi);


        //CancelAlarm(context);
    }

    public static void SetAnalysisAlarm(Context context)
    {

        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        final SharedPreferences prefs = context.getSharedPreferences(InformedConsent.PREFS_INFORMED_CONSENT, Context.MODE_PRIVATE);
        String lastSetTime = prefs.getString(InformedConsent.PREFS_INFORMED_LAST_QUESTIONNAIRE_REMINDER_TIME, "");

        long nextMillis_offset=-1;
        DateTime nextAlarmTime=null;

        if(lastSetTime != "")
        {
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
            DateTime time;
            try
            {
                nextAlarmTime = formatter.parseDateTime(lastSetTime);


            }
            catch (Exception ex)
            {

            }
        }

        DateTime now = DateTime.now();

        if(nextAlarmTime != null && nextAlarmTime.getMillis() < now.getMillis())//if last saved time of alarm is before now...then set next alarm a bit sooner
        {
            int startHour = 18;
            int endHour = 21;
            Random r = new Random();
            int randomH =r.nextInt(endHour - startHour);

            int randomM = r.nextInt(59);

            int hoursAdd = startHour + randomH;


            //nextAlarmTime = now.plusSeconds(30);//test
            nextAlarmTime = now.plusDays(1).plusHours(hoursAdd).plusMinutes(randomM);// now.plusMinutes(1);// now.plusSeconds((int) (millisFromNow / 1000));
            Log.i("Reminder Alarm", "saved Alarm is before now");
        }
        else if(nextAlarmTime == null)//else....set alarm for default distance...i.e. about 2 days
        {

            int startHour = 18;
            int endHour = 21;
            Random r = new Random();
            int randomH =r.nextInt(endHour - startHour);

            int randomM = r.nextInt(59);

            int hoursAdd = startHour + randomH;


            nextAlarmTime = now.withTimeAtStartOfDay().plusDays(1).plusHours(hoursAdd).plusMinutes(randomM);
            //nextAlarmTime = now.plusSeconds(60);// now.plusMinutes(1);// now.plusSeconds((int) (millisFromNow / 1000));
            Log.i("Reminder Alarm", "No Saved Alarm Found");


        }
        else
        {
            Log.i("Reminder Alarm", "saved Alarm is valid");
        }

        DateTime time = DateTime.now();
        long elapsedMillis = SystemClock.elapsedRealtime();


        long clockMillis = time.getMillis();

        long offset = clockMillis - elapsedMillis;

        Log.i("Reminder Alarm", "About to set " +  nextAlarmTime.toString("yyyy-MM-dd HH:mm:ss"));
        nextMillis_offset = nextAlarmTime.getMillis() - offset;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(InformedConsent.PREFS_INFORMED_LAST_QUESTIONNAIRE_REMINDER_TIME, nextAlarmTime.toString("yyyy-MM-dd HH:mm:ss"));
        editor.commit();




        Intent i2 = new Intent(context,ReminderQuestionnaire.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i2, PendingIntent.FLAG_UPDATE_CURRENT);
        am.set(AlarmManager.ELAPSED_REALTIME, nextMillis_offset /*milliseconsTomorrow*/, pi);


        //CancelAlarm(context);
    }
}
