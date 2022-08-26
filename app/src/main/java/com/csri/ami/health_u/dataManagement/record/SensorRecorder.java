package com.csri.ami.health_u.dataManagement.record;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;

import com.csri.ami.health_u.dataManagement.record.BT.Bluetooth_Sensor;
import com.csri.ami.health_u.dataManagement.record.motion.Motion_Sensor;
import com.csri.ami.health_u.dataManagement.record.motion.Vector3D;
import com.csri.ami.health_u.dataManagement.record.sound.Sound_Sensor;
import com.csri.ami.health_u.dataManagement.record.wifi.WiFiLocationManager;
import com.csri.ami.health_u.dataManagement.record.wifi.WiFiScanResult;
import com.csri.ami.health_u.dataManagement.record.wifi.WiFi_Sensor;
import com.csri.ami.health_u.ui.RawDataAnalysisAlarm;
import com.csri.ami.health_u.ui.SummaryDataUploadAlarm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 * High level Sensor manager class for all individual sensors (Motion, Sound, Wifi and GPS)
 * Class handles co-ordination between different sensor devices
 * For example, when no motion is detected, wifi detection is reduced while sound sensing is started
 * Class manages location data. Location fluctuates between GPS sensor data and Wifi location data to preserve power
 * If known wifi ids are detected, GPS can be switched off.
 * If no known wifi is detected, GPS is switch on and new wifi ids are associated with detected GPS for later
 */
public class SensorRecorder
{
	public  static String PREFS_FILE_LOCK = "filelockprefs";
	public static String PREF_FILE_LOCK_RAWSENSORFILES = "rawsensorfileslocked";

	GPS_Sensor gps;
	Motion_Sensor motion;
	Bluetooth_Sensor bt;
	WiFi_Sensor wifi;
	WiFiLocationManager wifiLocationM;
	Sound_Sensor sound;
	
	private ArrayList<WiFiScanResult> previosWifiAddresses;
	
	private static String rotationMatrix_filename = "rotation.csv";
	private static String notRotatedMotion_filename = "motion.csv";
	private static String rotatedMotion_filename = "motionR.csv";
	public static String SAVEFOLDER = "/Android/data/QOLMeasurement/files/";
	
	public boolean Started = false;
	private boolean SaveData = true;
	
	//File fileToWrite_Or_Test;
	File fileToWrite_Accel ;
	File fileToWrite_Or;
	File fileToWrite_MotionLabels ;
	File fileToWrite_MotionLabelFrames;
	//File fileToWrite_Sound;
	boolean SaveLabels = false;
	boolean AutoIterateFileNames = false;
	boolean GPS_On = true;

	//	int fileNumber = 0;
	int motionFileNumber =0;
	private static long FileSizeLimit =  (long)((double)1073741824 * 3.8);

	
	private Handler DiskHandler = new Handler();
	private Handler MotionVariationHandler = new Handler();
	private Handler handler = new Handler();

	
	public static int BTDeviceRecheckDelay_Fast =  60000;
	public static int BTDeviceRecheckDelay_Slow =  60000 * 15;

	public static int WiFiDeviceRecheckDelay_Fast = 30000;//10 seconds
	public static int WiFiDeviceRecheckDelay_Slow = 60000 * 15;//15 mins
	public int WiFiDeviceRecheckDelay_Current = 30000;
	
	public static int GPSDeviceRecheckDelay =   20000;
	public static int MotionVariationRecheckDelay = 1000;
	
	private long LastFastStartTime = 0;
	private long LastSlowStartTime = 0;
	private boolean AccelSetToSlow = true;
	private double MinFastSensorDuration_ms = WiFiDeviceRecheckDelay_Fast;


	public boolean wifiSlowEnabled = true;
	public boolean MovedSinceLastWifiUpdate = false;
	int WriteToDiskInterval =  60000 * 15;
	boolean SaveSeperateMotionFiles = false;
	//for the info.androidhive.materialtabs.QOL running all day, it was better to save a single file rather than seperate rotation
	// matrix and accel, gyro files that were used originally in the EMBC data collection
	
	Context context;
	boolean currentlySaving = false;
	boolean IsBackgroundSavingState=false;

	//Intent backgroundServiceIntent = null;


	public static void SetRawSensorFileLock(Context t,boolean state)
	{
		SharedPreferences.Editor pref = t.getSharedPreferences(PREF_FILE_LOCK_RAWSENSORFILES, Context.MODE_PRIVATE).edit();
		pref.putBoolean(PREFS_FILE_LOCK, state);
		pref.apply();
	}

	public static boolean GetRawSensorFileLock(Context t)
	{
		SharedPreferences pref = t.getSharedPreferences(PREF_FILE_LOCK_RAWSENSORFILES, Context.MODE_PRIVATE);
		boolean state = pref.getBoolean(PREFS_FILE_LOCK,false);
		return state;
	}

	public SensorRecorder(Context t,boolean Save,boolean backgroundSavingState)
	{
		IsBackgroundSavingState = backgroundSavingState;
		context = t;
		SaveData = Save;
		wifiLocationM = new WiFiLocationManager();

		if(!backgroundSavingState)
		{
			motion = new Motion_Sensor(t,SaveData,false);
		}
		else
		{
			
			wifi = new WiFi_Sensor(t);
			gps = new GPS_Sensor(t);
			motion = new Motion_Sensor(t,SaveData,true);
			sound = new Sound_Sensor(t);


			RawDataAnalysisAlarm.CheckPreferencesDataLastProcessInitialized(t);
			SummaryDataUploadAlarm.CheckPreferencesDataLastUploadInitialized(t);


			SetRawSensorFileLock(context, false);

			RawDataAnalysisAlarm analysisIntentReceiver = new RawDataAnalysisAlarm();

			analysisIntentReceiver.SetAnalysisAlarmStartOfDay(t);

			SummaryDataUploadAlarm uploadAlarm = new SummaryDataUploadAlarm();
			uploadAlarm.SetAlarmIn1Hour(t);

		}


		Time ti = new Time();
		ti.setToNow();

		
	}
	

	public Runnable beginSensing = new Runnable() {
		public void run() 
    	{
			Start();
    	}
	};
	
	public void Start()
	{

		//Start all individual sensors
		if(sound != null)
			sound.start();
		if(wifi != null)
			wifi.Start();
		if(bt != null)
			bt.Start();
		if(gps != null)
			gps.Start();
		if(motion != null)
			motion.Start();

		StartMotionVariationChecks();
		Started = true;

		//We will keep track of screen on/off to help with power management also.
		//Screen on means a user is interacting with the device and thus sensor reading should be reduced
		IntentFilter screenfilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		screenfilter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(screenBroadcastReciever, screenfilter);
		context.registerReceiver(broadcastReceiver_WiFiFound, new IntentFilter(WiFi_Sensor.BROADCAST_ACTION_WIFIFOUND));
		

		context.registerReceiver(broadcastReceiver_FrameLimit, new IntentFilter(Motion_Sensor.BROADCAST_ACTION_FRAMELIMIT));

	}


    private BroadcastReceiver screenBroadcastReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {

            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
            {

                if(motion != null)
                    motion.InformThatScreenWasTurnedOff();

            }
            else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
            {

                if(motion != null)
                    motion.InformThatScreenWasTurnedOn();

            }

        }
    };

	
	private BroadcastReceiver broadcastReceiver_FrameLimit = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
        	Log.i("FrameLimit","Saving Announcement Recieved");
        	String status = intent.getStringExtra(Motion_Sensor.FrameLimitBroadcast);
        	if(Boolean.parseBoolean(status))
        	{
        		if(sound != null)
        		{
        			if(!sound.IsRunning())
        			{
        				Log.i("FrameLimit","Saving Start Now");
        				DiskHandler.post(updateDiskDataTask );
        			}
        			else
            		{
            			Log.i("FrameLimit","Saving Delay");
            			DiskHandler.postDelayed(updateDiskDataTask,sound.RecordDuration() );
            			//WriteDataToDisk();
            		}
        		}
        		else
        		{
        			Log.i("FrameLimit","Saving Start Now");
    				DiskHandler.post(updateDiskDataTask );
        		}
        	}     
        }
    };
    

    private BroadcastReceiver broadcastReceiver_WiFiFound = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
        	String status = intent.getStringExtra(WiFi_Sensor.WifiFoundBroadcast);
        	if(Boolean.parseBoolean(status))
        	{
        		UpdateWiFiLocationStates();
        	}     
        }
    };


	/**
	 * Method performs shutdown of all sensor recording
	 * Shutdown is performed asynchronosly
	 */
	public void BeginStop()
	{
		context.unregisterReceiver(screenBroadcastReciever);
		context.unregisterReceiver(broadcastReceiver_WiFiFound);


		context.unregisterReceiver(broadcastReceiver_FrameLimit);
		if(wifi != null)
		{
			wifi.UnregisterRecievers();
		}



		new AsyncTask<Void, Void, Void>()
		{


			@Override
			protected Void doInBackground(Void... x) {
				try
				{
					Started = false;
					if(IsBackgroundSavingState)
					{
						Save();
					}
				}
				catch(Exception e)
				{

				}

				try
				{
					if(sound != null)
					{
						sound.stop();
						sound = null;
					}
					if(motion != null)
					{
						motion.Stop();
						motion = null;
					}
					if(wifi != null)
					{
						wifi.Stop();
						wifi = null;
					}
					if(gps != null)
					{
						gps.Stop();
						gps = null;
					}
					gps = null;

					if(bt != null)
					{
						bt.Stop();
						bt = null;
					}

				}
				catch(Exception e)
				{

				}
				return null;
			}

			protected void onPostExecute(Void res) {
				try
				{
					//DiskHandler.removeCallbacks(updateDiskDataTask);
					//handler.removeCallbacks(checkWiFiLocationStates);
					handler.removeCallbacks(checkWiFiLocationStates);

					context.unregisterReceiver(broadcastReceiver_FrameLimit);
					context.unregisterReceiver(broadcastReceiver_WiFiFound);
					MotionVariationHandler.removeCallbacks(startCheckingMotionVariation);

				}
				catch(Exception e)
				{

				}
			}

		}.execute();
	}


	/**
	 * Background method to write sensor data to disk
	 */
	private Runnable updateDiskDataTask = new Runnable() {
		public void run() {
			
			System.gc();
			WriteDataToDisk();

		}
	};
	
	private void WriteDataToDisk()
	{
		if(Started && !currentlySaving)
		{
			
			BeginSaving(false);
		}
	}


	



	public class SaveProcess implements Runnable {

		public void startSave() 
		{

			Thread t = new Thread(this);
			t.start();
		}

		public void run() 
		{
			if(sound != null)
			{
				Time t = new Time();
				t.setToNow();
				Log.i("SensorRecorder", "Begin Saving at: " + t.format("%d-%b-%G  %H:%M:%S"));
				sound.NotCurrentlySavingToDisk =false;
				Save();
				sound.NotCurrentlySavingToDisk =true;
				t.setToNow();
				Log.i("SensorRecorder", "End Saving at: " + t.format("%d-%b-%G  %H:%M:%S"));
				currentlySaving = false;
			}
			else
			{
				Time t = new Time();
				t.setToNow();
				Log.i("SensorRecorder", "Begin Saving at: " + t.format("%d-%b-%G  %H:%M:%S"));
				Save();
				t.setToNow();
				Log.i("SensorRecorder", "End Saving at: " + t.format("%d-%b-%G  %H:%M:%S"));
				currentlySaving = false;
			}
		}
	}

	public void BeginSaving(boolean saveLabels)
	{

		currentlySaving = true;
		SaveLabels = saveLabels;
		SaveProcess t = new SaveProcess();
		t.startSave();

	}
	
	public void StartMotionVariationChecks()
	{
		MotionVariationHandler.removeCallbacks(startCheckingMotionVariation);
		MotionVariationHandler.postDelayed(startCheckingMotionVariation, 0);
	}
	
	private Runnable startCheckingMotionVariation = new Runnable() {
    	public void run() 
    	{
    		 CheckMotionVariation();
    		 MotionVariationHandler.postDelayed(this, MotionVariationRecheckDelay);
    	}
    };
    
    private void UpdateIMUSpeed(boolean currentlyMoving,long time)
	{

    	{
			if(currentlyMoving)
			{
				LastFastStartTime = time;
			}
			else
			{
				LastSlowStartTime = time;
			}
			
			if(AccelSetToSlow && currentlyMoving)//switch from slow to fast
			{
				AccelSetToSlow = false;
			}
			else if(!AccelSetToSlow && !currentlyMoving)//switch from fast to slow
			{
				double diff = time - LastFastStartTime;
				//Log.i("FastToSlowDiff",Double.toString(diff));
				if(diff >= MinFastSensorDuration_ms)
				{
					Log.i("UpdateSpeed","Fast to Slow");
					AccelSetToSlow = true;
				}
			}
    	}

	}
    
    public void CheckMotionVariation()
    {
    	if(wifi != null)
    	{
			//if it has been a while since last wifi result...reset the device scanner
    		long timeleft = wifi.TimeSinceLastScanResult() % (long)(WiFiDeviceRecheckDelay_Slow * 1.5);
	    	if(wifi.TimeSinceLastScanResult() > WiFiDeviceRecheckDelay_Slow  && timeleft < 1000)
			{
				wifi.ResetWifi();
			}
    	}
    	
    	if(motion != null && motion.Running)
    	{

    		
    		boolean motionCurrentlyMoving = motion.HasMovedSinceLastCheck();
    		UpdateIMUSpeed(motionCurrentlyMoving,GPS_Sensor.GetUTMTime());
    		if(sound != null)
    		{
    			sound.DeviceNotMoving = motion.AccelSetToSlow;
    		}
    		if(motionCurrentlyMoving)
    		{
    			Log.i("Motion Variation","AccelSetToSlow=" + AccelSetToSlow + " wifiSlowEnabled=" + wifiSlowEnabled + " motion.AccelSetToSlow=" + motion.AccelSetToSlow);
    		}


    		//If motion is set to high freqency recording (i.e. device is moving) and wifi is set to slow update then change wifi to fast update
			//in order words....if we know device is on the move, ensure that wifi scanning is happening frequently
    		if(wifiSlowEnabled && !AccelSetToSlow)
    		{	
    			wifiSlowEnabled = false;

    			if(wifi != null)
				{
					wifi.UpdateCheckSpeed(WiFiDeviceRecheckDelay_Fast);

				}
    			if(bt != null)
				{
					bt.UpdateCheckSpeed(BTDeviceRecheckDelay_Fast);
				}
    		}
			//This is inverse of previous if
			//If wifi is running on high update frequency...but we know device is not moving, then change wifi to update less frequently
    		else if(!wifiSlowEnabled && AccelSetToSlow)// if(!wifiSlowEnabled && var < VariationMinThreshold)
    		{
    			//Log.i("Motion Variation","wifiSlowEnabled = true");
    			wifiSlowEnabled = true;
    			if(wifi != null)
				{
					{
						wifi.UpdateCheckSpeed(WiFiDeviceRecheckDelay_Slow);
					}
				}
    			if(bt != null)
    				bt.UpdateCheckSpeed(BTDeviceRecheckDelay_Slow);
    		}
    	}
    }

	
	private Runnable checkWiFiLocationStates = new Runnable() {
    	public void run() {
    		Log.i("checkWiFiLocationStates","...Updating!");
    	   
    		if(wifi.size == 0)
    		{
    			UpdateWiFiLocationStates();
    		}

    		
    		//Log.i("Motion Variation","Standard Post Delay");
    		handler.postDelayed(this, WiFiDeviceRecheckDelay_Current);
    		
    	}
    };


    private void UpdateWiFiLocationStates()
    {
    	
    	try
    	{
	    	//get new wifi addresses found since last check
    		if(wifi != null)
    		{
			    ArrayList<WiFiScanResult> raw_wifiAddresses = wifi.GetWiFiForWifiLocation();
			    //ArrayList<WiFiScanResult> valid_wifiAddresses = (ArrayList<WiFiScanResult>)raw_wifiAddresses.clone();
			    	
			    GPS loc = null;
			    	
		    	if(raw_wifiAddresses != null && raw_wifiAddresses.size() > 0)
		    	{

		    		
		    		//see if we can find a GPS address that have been previously seen with any of the wifi addresses
		    		loc = wifiLocationM.GetLocation(raw_wifiAddresses);
		    	
		    	
			    	//if none of the discovered wifi address have an associated gps stored.....or if the gps hasnt found at least one real gps location, then ...
			    	if(loc == null)// || gps.FirstTime == -1)
			    	{
			    		Log.i("SensorRecorder","Using GPS Location");
			    		gps.ResumeGPSReading();//ensure GPS receiver is turned on
			    		GPS_On = true;
			    		ArrayList<GPS> gpsData = gps.GetGPSForWifiLocation();//get all GPS values since last check
			    		for(int i=0;i<gpsData.size();i++)
			    		{
			    			Log.v("GPS Accuracy", Double.toString(gpsData.get(i).Accuracy));
			    		}

			    		//check if any of the detected wifi and gps were recorded around the same time...if they were, save the assocatiation between the wifi and gps location
			    		boolean updated = wifiLocationM.UpdateLocations(raw_wifiAddresses, gpsData,WiFiDeviceRecheckDelay_Current * 2);
			    		if(updated)
			    		{
			    			gps.ClearWiFiLocationBuffer();
			    		}
			    	}
			    	else
			    	{
			    		Log.i("SensorRecorder","Using WIFI Location");
			    		gps.SuspendGPSReading();
			    		GPS_On = false;
			    		gps.UpdateLocation(loc);
			    	}
		    	}
		    	else
		    	{
		    		Log.d("SensorRecorder","No Wifi:Resuming GPS");
		    		gps.ResumeGPSReading();
		    		GPS_On = true;
		    	}
		    	
		    	previosWifiAddresses = raw_wifiAddresses;
    		}
		}
    		
    	
    	catch(Exception e)
    	{
    		WriteToLog(e);
    	}
    	
    }
    
    public static void WriteToLog(Exception ex)
	{
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		if( mExternalStorageAvailable && mExternalStorageWriteable)
		{
			try
			{
			File f = Environment.getExternalStorageDirectory();
			
			//String fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER ;
			//File folderToWrite = new File(fullfilename);
			//folderToWrite.mkdirs();
			File f2 = new File("/storage/extSdCard/");
			
			String fullfilename = f2.getAbsoluteFile()  + SensorRecorder.SAVEFOLDER ;
			File folderToWrite = new File(fullfilename);
			folderToWrite.mkdirs();
			boolean sdFound = folderToWrite.exists() && folderToWrite.canWrite();
			
			if(!sdFound)
			{
				File f3 = new File("/mnt/extSdCard/");
				String fullfilename3 = f3.getAbsoluteFile() + SAVEFOLDER ;
				File folderToWrite3 = new File(fullfilename3);
				folderToWrite3.mkdirs();
				boolean sdFound3 = folderToWrite3.exists() && folderToWrite3.canWrite();
				if(sdFound3)
				{
					fullfilename = fullfilename3;
					folderToWrite = new File(fullfilename);
					folderToWrite.mkdirs();
				}
				else
				{
					fullfilename = f.getAbsoluteFile() + SAVEFOLDER ;
					folderToWrite = new File(fullfilename);
					folderToWrite.mkdirs();
				}
			}
			
			//String t = sensorID.replace(':', '_');
			File fileToWrite = new File(fullfilename, "Log.txt");
			FileOutputStream fos = new FileOutputStream(fileToWrite,true);
			OutputStreamWriter osw = new OutputStreamWriter(fos); 
			
			
			//osw.write("Example Text in a file in the "+f.getAbsolutePath()+" dir");
			Time t = new Time();
			t.setToNow();
			
			
			osw.write(t.format("%d-%b-%G  %H:%M:%S") + " Message: "   + ex.getMessage() + "\n");
			StackTraceElement[] st = ex.getStackTrace();
			for(int i=0;i<st.length;i++)
			{
				osw.write(st[i].toString() + "\n");
			}
			
			
			
			
			
			osw.close();
			
			}
			catch(IOException e)
			{
				
			}
		}
		
	}

	private void Save()
	{

		try
		{
			
			PrepareForSaving();
			Time t = new Time();
			t.setToNow();

			String name = "QOL";
			

			Save(name);//using strftime formatting see: http://php.net/manual/en/function.strftime.php
			if(SaveLabels)
			{
				motion.ClearLabelBuffers();
				SaveLabels= false;
			}
			ClearSavingBuffers();

		} 
		catch(Exception ex)
		{
			SensorRecorder.WriteToLog(ex);
		}

	}

	public void PrepareForSaving()
	{

		if(sound != null)
		sound.PrepareForSaving();
		if(motion != null)
		motion.PrepareForSaving();
		if(gps != null)
		gps.PrepareForSaving();
		if(bt != null)
		bt.PrepareForSaving();
		if(wifi != null)
		wifi.PrepareForSaving();

	}

	public void ClearSavingBuffers()
	{
		//synchronized(this)
		//{
		if(sound != null)
			sound.ClearSavingBuffers();
		if(motion != null)
			motion.ClearSavingBuffers();
		if(gps != null)
			gps.ClearSavingBuffers();
		if(bt != null)
			bt.ClearSavingBuffers();
		if(wifi != null)
			wifi.ClearSavingBuffers();
		
		//}
	}


	
	public double[] GetSoundClassificationResults()
	{
		if(sound != null)
		{
			return sound.GetSoundClassificationResults();
		}
		else
		{
			return null;
		}
	}
	
	public double GetSoundSampleCount()
	{
		if(sound != null)
		{
			return sound.GetSoundSampleCount();
		}
		else
		{
			return 0;
		}
	}

	public String GetVariationBand_HourStartTime()
	{
		if(motion != null)
		{
			if(motion.bands_HourStart != null) {
				String temp = motion.bands_HourStart.toString("hh:mm a");
				return temp;
			}
			else
			{
				return "";
			}
		}
		else
		{
			return "";
		}
	}

	public String GetVariationBand_WindowStartTime()
	{
		if(motion != null)
		{
			if(motion.bands_WindowStart != null) {
				return motion.bands_WindowStart.toString("hh:mm a");
			}
			else
			{
				return "";
			}
		}
		else
		{
			return "";
		}
	}

	public double GetTotalSoundAvg()
	{
		if(sound != null)
		{
			return sound.Get_TotalSoundAvg();
		}
		else
		{
			return 0;
		}
	}

	public double GetMostRecentSoundAvg()
	{
		if(sound != null)
		{
			return sound.Get_MostRecentSoundAvg();
		}
		else
		{
			return 0;
		}
	}

	public double GetGPSDistance()
	{
		if(gps != null)
		{
			return gps.GetStoredDistance();
		}
		else
		{
			return 0;
		}
	}

	public double[] GetMotionVariationBands_Day()
	{
		if(motion != null)
		{
			return motion.VariationBands_Daily;
		}
		else
		{
			return null;
		}
	}

	public double[] GetMotionVariationBands_Hour()
	{
		if(motion != null)
		{
			return motion.VariationBands_Hourly;
		}
		else
		{
			return null;
		}
	}

	public double[] GetMotionVariationBands_Min()
	{
		if(motion != null)
		{
			return motion.VariationBands_Window;
		}
		else
		{
			return null;
		}
	}
	
	public double GetSoundDurationCount()
	{
		if(sound != null)
		{
			return sound.GetSoundDurationeCount();
		}
		else
		{
			return 0;
		}
	}
	
	public String GetStatusString()
	{
		String line = "";
		if(gps !=null)
		{
			GPS g = gps.currentReading;
		}
		
		if(motion != null)
		{
			int frameMotionCount = (int)motion.FrameNo;
			
			
		
			line += "Motion Frames: " + frameMotionCount + " " + motion.movingCount + "\n" +
				"--------------" + "\n";// +
			
			Vector3D a = motion.getAccelerationGlobal();
			
			//double mag = Math.abs(9.81 - a.Magnitude);
			if(a != null)
			{
				line += "HorAccel-" + a.Horizontal_Magnitude + "\n" +
					"VerAccel-" + a.Vertical_Magnitude + "\n" +
					"--------------" + "\n";
			}
		}
		
		if(gps != null && GPS_On)
		{
			long time_diff = gps.TimeSinceLastLocationChange() / 1000;
			line += "GPS ON: Satellite Location" + "\n";
			line += "Last Update: " + Long.toString(time_diff) + " Seconds ago." + "\n";
		}
		else
		{
			line += "GPS OFF: WiFi Approx Location" + "\n";
		}
		
		if(gps != null)
		{
		line += "Lat-" + gps.currentReading.Latitude + "\n" + 
		"Lon-" + gps.currentReading.Londitude + "\n" +
		"--------------" + "\n";
		}
		
		
		if(bt != null)
		{
			ArrayList<BluetoothDevice> btDevices = bt.GetDetectedDevices();
			int bts = 0;
			if(btDevices != null)
			{
				bts = btDevices.size();
			}
			long bttime = bt.TimeSinceLastScanResult() / 1000;
			line += bts + " BT Devices Found " + bttime + " Seconds Ago" + "\n" + "--------------" + "\n";
		}
		if(wifi != null)
		{
			long wifiTime = wifi.TimeSinceLastScanResult() / 1000;
			line += wifi.NumberDevicesFoundOnLastScan() + " Wifi Devices Found " + wifiTime + " Seconds Ago" + "\n";
		}
		if(sound != null) {
			double[] soundClasses = GetSoundClassificationResults();
			if (soundClasses != null)
			{
				for (int i = 0; i < soundClasses.length; i++)
				{
					line += soundClasses[i] + " ";
				}
				line += "\n";
			}
		}
		return line;
	}

	private static int SoundFileNumber=0;
	public void Save(String session_name)
	{

		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) 
		{
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} 
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) 
		{
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} 
		else 
		{
			// Something else is wrong. It may be one of many other states, but all we need
			//  to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}

		if( mExternalStorageAvailable && mExternalStorageWriteable)
		{
			try
			{
				File f = Environment.getExternalStorageDirectory();
				String fullfilename = f.getAbsoluteFile() + SAVEFOLDER ;
				File folderToWrite = new File(fullfilename);
				folderToWrite.mkdirs();
				

				
				long timeOffset =0;
				if(gps != null)
				{
					timeOffset = (long)gps.GPS_TimeOffset;
				}

				//removed code here from original file...went more more simplistic file saving
				
				//commented out for data collection of activities--> uncomment when you want to save gps and bt
				boolean filesLocked =false;// GetRawSensorFileLock(context);
				while (filesLocked)
				{
					try{Thread.sleep((long)200);}catch(InterruptedException ex){}
					filesLocked = GetRawSensorFileLock(context);
				}

				if(!filesLocked)
				{
					SetRawSensorFileLock(context,true);
					if (motion != null) {
						if (motion.DataAvailableForSaving()) {
							File fileToWrite_Accel_this = new File(fullfilename, session_name + "_" + motionFileNumber + rotatedMotion_filename);
							long fileSize = fileToWrite_Accel_this.length();
							while (fileSize > FileSizeLimit) {
								motionFileNumber++;
								fileToWrite_Accel_this = new File(fullfilename, session_name + "_" + motionFileNumber + rotatedMotion_filename);
								fileSize = fileToWrite_Accel_this.length();
							}

							BufferedWriter fos_Accel = new BufferedWriter(new FileWriter(fileToWrite_Accel_this, true));
							motion.WriteMotionValues(fos_Accel, timeOffset);
							fos_Accel.close();

						}
					}
					if (gps != null) {
						File fileToWrite_GPS = new File(fullfilename, session_name + "_GPS.csv");
						FileOutputStream fos_GPS = new FileOutputStream(fileToWrite_GPS, true);
						OutputStreamWriter osw_GPS = new OutputStreamWriter(fos_GPS);
						gps.Write(osw_GPS);
						osw_GPS.close();
					}

					if (bt != null) {
						File fileToWrite_BT = new File(fullfilename, session_name + "_Bluetooth.csv");
						FileOutputStream fos_BT = new FileOutputStream(fileToWrite_BT, true);
						OutputStreamWriter osw_BT = new OutputStreamWriter(fos_BT);
						bt.Write(osw_BT);
						osw_BT.close();
					}

					if (wifi != null) {
						File fileToWrite_WiFi = new File(fullfilename, session_name + "_wlan.csv");
						FileOutputStream fos_WiFi = new FileOutputStream(fileToWrite_WiFi, true);
						OutputStreamWriter osw_WiFi = new OutputStreamWriter(fos_WiFi);
						wifi.Write(osw_WiFi);
						osw_WiFi.close();
					}

					if (sound != null) {
						File fileToWrite_Sound = new File(fullfilename, session_name + "_" + SoundFileNumber + "sound.csv");

						long fileSize = fileToWrite_Sound.length();
						while (fileSize > FileSizeLimit) {
							SoundFileNumber++;
							fileToWrite_Sound = new File(fullfilename, session_name + "_" + SoundFileNumber + "sound.csv");
							fileSize = fileToWrite_Sound.length();
						}

						FileOutputStream fos_Sound = new FileOutputStream(fileToWrite_Sound, true);
						OutputStreamWriter osw_Sound = new OutputStreamWriter(fos_Sound);
						sound.Write(osw_Sound, timeOffset);
						osw_Sound.close();
					}
					SetRawSensorFileLock(context,false);
				}



			}
			catch(IOException e)
			{
				Log.v("SoundRecord","Write Error " +  e.getMessage());
			}
		}

	}


}
