package com.csri.ami.health_u.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.csri.ami.health_u.R;
import com.csri.ami.health_u.dataManagement.record.SensorRecorder;

import java.text.DecimalFormat;
import java.util.Timer;

public class BackgroundService extends Service 
{
	public static String SHARED_PREF_NAME = "preferencesFileHealthU";
	public static String SENSOR_ON_PREF = "sensorOnPref";

	public static String DIARY_DATA = "diary_data";
	public static String DIARY_NUMBERSOUNDSEGMENTS = "diary_soundsegments";

	public static String VAR_BANDS_HOUR_START = "varbandhourstart";
	public static String VAR_BANDS_WINDOW_START = "varbandwindowstart";

	public static String VAR_BANDS_WINDOW_DATA = "windowdata";
	public static String VAR_BANDS_HOUR_DATA = "hourdata";
	public static String VAR_BANDS_DAY_DATA = "daydata";
	public static String GPS_DISTANCE = "gpsdist";
	public static String SOUND_MOSTRECENT = "soundrecent";
	public static String SOUND_MOSTOTOAL = "soundtotal";

	public static final String PREFS_NAME = "ClaritySensorPrefsFile";
	public static final String CORRECTLY_SHUTDOWN_PREF_FLAG = "ShutDownCorrectly";
	private static final String TAG = "BroadcastService";
	public static final String BROADCAST_ACTION = "com.sensors.analyze.bluetoothlabel.displayevent";
	private final Handler handler = new Handler();
	Intent intent;
	int counter = 0;
	Notification notification;
	PowerManager pm;
	PowerManager.WakeLock wl;
	
	static final int MSG_SET_INT_VALUE = 3;
	
	private NotificationManager mNM;
	SensorRecorder recorder;
	
	int RecheckDelay = 1000;
    Timer scheduler;
	private Handler handlerUpdate = new Handler();
	
	public static boolean useDiaryAlarm = false; //set true for full qol recording
	
	//qol checklist - make sure times are updated in SensorRecorder.java
	//              - make sure all sensors are created in SensorRecorder constructor
	//				- change MaxNoFrames in GPS_Sensor to 2000
	//              - set useDiaryAlarm to true in this class
	//				- set StartupIntentReciever to only turn on when not shut down correctly
	
	//gps only checklist - make sure times are updated in SensorRecorder.java
	//              - make sure only gps sensor is created in SensorRecorder constructor
	//				- change MaxNoFrames in GPS_Sensor to 2
	//              - set useDiaryAlarm to false in this class
	//				- set StartupIntentReciever to only turn on always

	//private AlarmManager mgr;
	//private PendingIntent pi;
	Context context;// = this.getApplicationContext();
	

	
	public class LocalBinder extends Binder
	{
		BackgroundService getService()
		{
			return BackgroundService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	@Override
    public void onCreate() 
	{
		context = getApplicationContext();
		Log.d("BackService","++ Create ++");
		super.onCreate();
		intent = new Intent(BROADCAST_ACTION);	
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		recorder = new SensorRecorder(this,true,true);
		//showNotification();
//		if(useDiaryAlarm)
//		{
//			SetDiaryAlarm();
//		}
		
		Start();
		//showDiaryNotification();
    }
	
	//@Override
    public void Start() 
	{







		//super.onStart(intent, startid);
		if (recorder == null) {
			onCreate();
		}

		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tag");
		wl.acquire();
		//do what you need to do

		//recorder.Start();

		Handler h = new Handler();
		h.post(recorder.beginSensing);

		Intent showTaskIntent = new Intent(getApplicationContext(), IconTextTabsActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, showTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT);


		Notification not = new NotificationCompat.Builder(getApplicationContext())
				.setContentTitle(/*getResources().getString(R.string.app_name) + " " + */getResources().getString(R.string.background_notification))
				.setContentText(getResources().getString(R.string.background_notification_mes))
				.setContentIntent(pendingIntent)
				.setSmallIcon(R.drawable.medical)
				.setOngoing(false)
				.build();

		startForeground(1234,not);
		StartNewDataChecks();


		
    }
	
	@Override
    public void onDestroy() 
	{
		EndNewDataChecks();

		super.onDestroy();

		//BeginDestroy();
		


		recorder.BeginStop();
		recorder = null;

		wl.release();

		mNM.cancel(Notification.FLAG_ONGOING_EVENT);
		//mNM.cancel(Notification.FLAG_ONLY_ALERT_ONCE);

		Toast.makeText(this, getResources().getString(R.string.background_stopped), Toast.LENGTH_SHORT).show();
		stopForeground(true);
    }


	private void ShowText()
	{
		Toast.makeText(this, context.getResources().getString(R.string.background_stopped), Toast.LENGTH_SHORT).show();
	}


	
	private void EndNewDataChecks()
	{
		handlerUpdate.removeCallbacks(announceNewDataTask);
	}
	
	public void StartNewDataChecks()
	{

		handlerUpdate.removeCallbacks(announceNewDataTask);
        handlerUpdate.postDelayed(announceNewDataTask, 200);
	}
	
	private Runnable announceNewDataTask = new Runnable() {
    	public void run() {
    	AnnounceNewData();
    	handler.postDelayed(this, RecheckDelay);
    	}
    	};
    	
    private void AnnounceNewData()
    {
    	if(recorder != null)
    	{
    		Bundle b = new Bundle();
    		double[] soundClasses = recorder.GetSoundClassificationResults();
    		double soundSampleCount = recorder.GetSoundSampleCount();
    		double durationCount = recorder.GetSoundDurationCount();

			double[] variationBands_day = recorder.GetMotionVariationBands_Day();
			double[] variationBands_hour = recorder.GetMotionVariationBands_Hour();
			double[] variationBands_min = recorder.GetMotionVariationBands_Min();

			double gps_distance = recorder.GetGPSDistance();
			double sound_recent = recorder.GetMostRecentSoundAvg();
			double sound_total = recorder.GetTotalSoundAvg();

    		if(variationBands_day != null && variationBands_hour != null && variationBands_min != null)
    		{
				DecimalFormat df = new DecimalFormat("0.00");

				String[] status_string = new String[variationBands_day.length];
	    		//status_string[0] = recorder.GetStatusString();
	    		for(int i=0;i<variationBands_day.length;i++)
	    		{
	    			status_string[i] = df.format(variationBands_day[i]);
	    		}
	    		
	    		//status_string[status_string.length-1] = Double.toString(soundSampleCount);
	    		
	    		b.putStringArray(BackgroundService.DIARY_DATA, status_string);
	    		b.putDoubleArray(BackgroundService.VAR_BANDS_DAY_DATA, variationBands_day);//(, soundSampleCount);
				b.putDoubleArray(BackgroundService.VAR_BANDS_HOUR_DATA, variationBands_hour);//(, soundSampleCount);
				b.putDoubleArray(BackgroundService.VAR_BANDS_WINDOW_DATA, variationBands_min);//(, soundSampleCount);

				b.putString(BackgroundService.VAR_BANDS_HOUR_START, recorder.GetVariationBand_HourStartTime());
				b.putString(BackgroundService.VAR_BANDS_WINDOW_START,recorder.GetVariationBand_WindowStartTime());

				b.putDouble(BackgroundService.GPS_DISTANCE, gps_distance);
				b.putDouble(BackgroundService.SOUND_MOSTRECENT,sound_recent);
				b.putDouble(BackgroundService.SOUND_MOSTOTOAL,sound_total);
	    		//recorder.ClearClassificationResults();
    		}
    		else
    		{
    			
	    		b.putStringArray(BackgroundService.DIARY_DATA, new String[]{recorder.GetStatusString()});
    		}
    		intent.putExtras(b);
	    	//intent.putExtra("status", recorder.GetStatusString());
	    	//intent.putExtra("counter", String.valueOf(++counter));
	    	sendBroadcast(intent);
    	}
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
//		int myID = 1234;
//
//		//The intent to launch when the user clicks the expanded notification
//		Intent intent2 = new Intent(this, com.sensor.analyze.bluetoothlabel.SensorMainUI.class);
//		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//		PendingIntent pendIntent = PendingIntent.getActivity(this, 0, intent2, 0);
//
//		//This constructor is deprecated. Use Notification.Builder instead
//		Notification notice = new Notification(android.R.drawable.ic_media_play, "Ticker text", System.currentTimeMillis());
//
//		//This method is deprecated. Use Notification.Builder instead.
//		notice.setLatestEventInfo(this, "Title text", "Content text", pendIntent);
//
//		notice.flags |= Notification.FLAG_NO_CLEAR;
		
	    //handleCommand(intent);
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
		
		Log.d("BackService","+ onStartCommand +");
		
	    return START_STICKY;
	}
	
	
	
//	private void SetDiaryAlarm()
//	{
//		long lastDiaryEntryTime = Diary.GetLastEntryTime();
//		Time currentTime = new Time();
//		currentTime.setToNow();
//
//		long diffInSeconds = (currentTime.toMillis(false) - lastDiaryEntryTime);
//
//		long delay = OnAlarmReceiver.REMINDER_DELAY;
//		if(diffInSeconds < OnAlarmReceiver.REMINDER_DELAY)
//		{
//			delay = OnAlarmReceiver.REMINDER_DELAY - diffInSeconds;
//		}
//		else if(diffInSeconds > OnAlarmReceiver.REMINDER_DELAY)
//		{
//			delay = diffInSeconds % OnAlarmReceiver.RE_REMINDER_DELAY;
//
//		}
//		if(delay > 0 && delay < OnAlarmReceiver.REMINDER_DELAY)
//		{
//			Log.i("SetDiaryAlarm","Alarm set to go off in " + Integer.toString((int)(delay / 1000)) + " seconds");
//			OnAlarmReceiver.SetOneTimeAlarm(context, (int)delay);
//		}
//		else if(delay < 0)
//		{
//			Log.i("SetDiaryAlarm","Alarm set to go off in " + Integer.toString((int)(3000 / 1000)) + " seconds");
//			OnAlarmReceiver.SetOneTimeAlarm(context, 3000);//if delay is minus..then it is u, and therefore should be fired asap..i.ie in 3000 ms
//		}
//		else
//		{
//			Log.i("SetDiaryAlarm","Alarm set to go off in " + Integer.toString((int)(OnAlarmReceiver.REMINDER_DELAY / 1000)) + " seconds");
//			OnAlarmReceiver.SetOneTimeAlarm(context, OnAlarmReceiver.REMINDER_DELAY);
//		}
//		//AlarmProcess ap = new AlarmProcess();
//		//ap.startAlarm();
////		mgr=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
////		Intent i=new Intent(context, OnAlarmReceiver.class);
////		pi=PendingIntent.getBroadcast(context, 0, i, 0);
////
////		mgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+30000, pi);
//	}
	
//	 private void showDiaryNotification() {
//	        // In this sample, we'll use the same text for the ticker and the expanded notification
//	        CharSequence text = "Diary Reminder";
//
//	        // Set the icon, scrolling text and timestamp
//	        Notification notificationDiary = new Notification(android.R.drawable.ic_menu_agenda, text,
//	                System.currentTimeMillis() + (1000 * 20));
//
//	        // The PendingIntent to launch our activity if the user selects this notification
//	        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//	                new Intent(this, info.androidhive.materialtabs.QOL.UI.SensorMainUI.class/*com.sensor.analyze.UI.Diary.class*/), 0);
//
//	        // Set the info for the views that show in the notification panel.
//	        notificationDiary.setLatestEventInfo(this, "Enter a diary entry",
//	                       text, contentIntent);
//
//	        // Send the notification.
//	        mNM.notify(Notification.FLAG_ONLY_ALERT_ONCE, notificationDiary);
//
//	        //startForeground(1234, notification);
//	    }
	
//	 private void showNotification() {
//	        // In this sample, we'll use the same text for the ticker and the expanded notification
//	        CharSequence text = "info.androidhive.materialtabs.QOL Started";
//
//	        // Set the icon, scrolling text and timestamp
//	        notification = new Notification(android.R.drawable.ic_menu_save, text,
//	                System.currentTimeMillis());
//
//	        // The PendingIntent to launch our activity if the user selects this notification
//	        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//	                new Intent(this, info.androidhive.materialtabs.QOL.UI.SensorMainUI.class), 0);
//
//	        // Set the info for the views that show in the notification panel.
//	        notification..setLatestEventInfo(this, "Recording Sensor Data!",
//	                       text, contentIntent);
//
//	        // Send the notification.
//	        //mNM.notify(Notification.FLAG_ONGOING_EVENT, notification);
//
//	        //startForeground(1234, notification);
//	    }

}
