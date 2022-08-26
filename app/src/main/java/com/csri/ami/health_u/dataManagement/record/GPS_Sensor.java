package com.csri.ami.health_u.dataManagement.record;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.format.Time;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;


/**
 * Sensor Manager to handle recording of data from GPS
 */
public class GPS_Sensor implements LocationListener
{
	LocationManager lm;
	int GPSstatus;
	public GPS currentReading;
	private ArrayList<GPS> ALL_GPSreadings;
	private ArrayList<GPS> ALL_GPSreadings_UsedForSaving;
	private ArrayList<GPS> ALL_GPSreadings_UsedForWiFiLocation;
	private ArrayList<Time> timeStamps;
	Context context;
	boolean Suspended = true;
	
	public static final String BROADCAST_ACTION_FRAMELIMIT = "com.sensors.record.Motion_Sesor.DataLimit";
	public static final String FrameLimitBroadcast = "FrameLimit";
	Intent frameLimitIntent;
	
	static int MAXNUMBERFRAMES = 2000;
	
	public static int GPS_SAMPLE_INTERVAL = SensorRecorder.GPSDeviceRecheckDelay;
	public static int GPS_MIN_DISTANCECHANGE = 100;
	
	public double FirstTime = -1;
	public double GPS_TimeOffset =0;
	
	public GPS_Sensor(Context t)
	{

		context = t;
		lm = (LocationManager)t.getSystemService(Context.LOCATION_SERVICE);
		
		frameLimitIntent = new Intent(BROADCAST_ACTION_FRAMELIMIT );	

	  
	}
	
	public void SuspendGPSReading()
	{

		Log.i("GPS","Removing GPS Updates!!!");
		if(ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
		{
			lm.removeUpdates(this);

			Suspended = true;

		}

	}
	
	public void ResumeGPSReading()
	{
		Resume();
	}
	
	public void Stop()
	{
		SuspendGPSReading();
		lm = null;
	}

	/**
	 * Start GPS recording by requesting location updates from LocationManager
	 */
	public void Start()
	{
		if(!Suspended && lm != null)
		{
			lm.removeUpdates(this);
		}

		if((ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_SAMPLE_INTERVAL, GPS_MIN_DISTANCECHANGE, this);
			Suspended = false;

			ALL_GPSreadings = new ArrayList<GPS>();
			ALL_GPSreadings_UsedForWiFiLocation = new ArrayList<GPS>();
			timeStamps = new ArrayList<Time>();

			currentReading = new GPS();
			Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);


			if (location != null) {
				long diff = Math.abs(location.getTime() - GetUTMTime());

				long twoMins = 1000 * 60 * 5;
				if (location != null && diff < twoMins) {
					currentReading.Londitude = location.getLongitude();
					currentReading.Latitude = location.getLatitude();
					currentReading.Altitude = location.getAltitude();
					currentReading.Accuracy = location.getAccuracy();
					currentReading.TimeStamp = location.getTime();
					currentReading.Speed = location.getSpeed();

					currentReading.phoneTimeStamp = GetUTMTime();// SystemClock.elapsedRealtime();
					currentReading.dateTime = DateTime.now();
					ALL_GPSreadings.add(currentReading);
					ALL_GPSreadings_UsedForWiFiLocation.add(currentReading);

				}
			}
		}
	}
	
	public void Resume()
	{
		currentReading = new GPS();

		if(Suspended && (ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {

			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_SAMPLE_INTERVAL, GPS_MIN_DISTANCECHANGE, this);
			Suspended = false;

			Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);


			if (location != null) {
				long diff = Math.abs(location.getTime() - GetUTMTime());

				long twoMins = 1000 * 60 * 5;
				if (location != null && diff < twoMins) {
					currentReading.Londitude = location.getLongitude();
					currentReading.Latitude = location.getLatitude();
					currentReading.Altitude = location.getAltitude();
					currentReading.Accuracy = location.getAccuracy();
					currentReading.TimeStamp = location.getTime();
					currentReading.Speed = location.getSpeed();

					currentReading.phoneTimeStamp = GetUTMTime();// SystemClock.elapsedRealtime();
					currentReading.dateTime = DateTime.now();
					ALL_GPSreadings.add(currentReading);
					ALL_GPSreadings_UsedForWiFiLocation.add(currentReading);

				}
			}
		}
	}

	
	public ArrayList<GPS> GetGPSForWifiLocation()
	{
		if(ALL_GPSreadings_UsedForWiFiLocation != null)
		{
			ArrayList<GPS> temp = (ArrayList<GPS>) ALL_GPSreadings_UsedForWiFiLocation.clone();
			//ALL_GPSreadings_UsedForWiFiLocation.clear();
			return temp;
		}
		else
		{
			return null;
		}
			
			
		
	}
	
	public void ClearWiFiLocationBuffer()
	{
		if(ALL_GPSreadings_UsedForWiFiLocation != null)
		{
			ALL_GPSreadings_UsedForWiFiLocation.clear();
			
		}
	}
	
	public void PrepareForSaving()
	{
		if(ALL_GPSreadings != null)
		{
			ALL_GPSreadings_UsedForSaving = (ArrayList<GPS>)ALL_GPSreadings.clone();
			ALL_GPSreadings.clear();
		}
		
	}
	
	public void ClearSavingBuffers()
	{
		synchronized(this)
		{
			if(ALL_GPSreadings_UsedForSaving != null)
			{
			ALL_GPSreadings_UsedForSaving.clear();
			}
			
		}
	}
	
	public void Write(OutputStreamWriter osw) throws IOException
	{
		//synchronized(this)
		//{
		if(ALL_GPSreadings_UsedForSaving != null)
		{
			//String accelString = "";
			for(int i=0;i<ALL_GPSreadings_UsedForSaving.size();i++)
			{
				osw.write(ALL_GPSreadings_UsedForSaving.get(i).toString());
			}
		}
			//return accelString;
		//}
	}
	
	
	
	public long TimeSinceLastLocationChange()
	{
		if(currentReading != null)
		{
			return GPS_Sensor.GetUTMTime() - currentReading.phoneTimeStamp;
		}
		else
		{
			return 0;
		}
	}
	
	public static long GetUTMTime()
	{
		DateTime t = new DateTime();
		t = DateTime.now();
		long temp = t.getMillis();
		return temp;

	}

	public double Distance =0;
	private GPS previous = null;
	//private DateTime previousTime;

	private static String GPS_STORED_PREFS = "gpssotredprefs";
	private static String GPS_STORED_DISTANCE = "gpssotredprefs_distance";
	private static String GPS_STORED_TIME = "gpssotredprefs_time";

	public double GetStoredDistance()
	{
		SharedPreferences prefs = context.getSharedPreferences(GPS_STORED_PREFS, Context.MODE_PRIVATE);

		float currentTotal = prefs.getFloat(GPS_STORED_DISTANCE, 0);


		return currentTotal;
	}

	public DateTime GetLastUpdateTime()
	{
		SharedPreferences prefs = context.getSharedPreferences(GPS_STORED_PREFS, Context.MODE_PRIVATE);

		String current = prefs.getString(GPS_STORED_TIME, DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));

		DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

		DateTime time = formatter.parseDateTime(current);

		return time;
	}

	private double UpdateStoredDistance(double distance)
	{
		SharedPreferences prefs = context.getSharedPreferences(GPS_STORED_PREFS, Context.MODE_PRIVATE);

		float currentTotal = prefs.getFloat(GPS_STORED_DISTANCE, 0);

		DateTime now = DateTime.now();


		currentTotal += distance;


		SharedPreferences.Editor editor = prefs.edit();

		editor.putFloat(GPS_STORED_DISTANCE, currentTotal);
		editor.putString(GPS_STORED_TIME, now.toString("yyyy-MM-dd HH:mm:ss"));


		editor.commit();

		return currentTotal;
	}

	private void ResetStoredDistance()
	{
		SharedPreferences prefs = context.getSharedPreferences(GPS_STORED_PREFS, Context.MODE_PRIVATE);

		SharedPreferences.Editor editor = prefs.edit();

		editor.putFloat(GPS_STORED_DISTANCE, 0);

		editor.commit();

	}

	/**
	 * Handle newly aquired GPS location
	 * @param wifiGPS
	 */
	public void UpdateLocation(GPS wifiGPS)
	{
		currentReading = new GPS();
		currentReading.Londitude = wifiGPS.Londitude;
		currentReading.Latitude = wifiGPS.Latitude;
		currentReading.Altitude = 0;
		currentReading.Accuracy = -1;
		currentReading.TimeStamp = GPS_Sensor.GetUTMTime();
		currentReading.Speed = -1;
		
		currentReading.phoneTimeStamp = GetUTMTime();// SystemClock.elapsedRealtime();
		currentReading.dateTime = DateTime.now();
		currentReading.LoadUTMValues();
		currentReading.wifi_bssids = wifiGPS.wifi_bssids;
		ALL_GPSreadings.add(currentReading);


		if(previous != null)
		{
			DateTime currentTime = DateTime.now();
			DateTime previousTime = GetLastUpdateTime();
			if (previousTime != null)
			{
				if (Days.daysBetween(currentTime.withTimeAtStartOfDay(), previousTime.withTimeAtStartOfDay()).getDays() != 0)
				{
					ResetStoredDistance();
				}
			}

			boolean isPreviousAndCurrentFromSameWifi = GPS.checkForSameWifi(previous,currentReading);
			if(!isPreviousAndCurrentFromSameWifi)
			{
				double currentDistance = GPS.Distance(previous, currentReading);

				Distance = UpdateStoredDistance(currentDistance);
			}
		}
		previous = currentReading;
	}


	/**
	 * Override method which will be called by LocationManager when new GPS data is available
	 * @param location
	 */
	@Override
	public void onLocationChanged(Location location) 
	{
		currentReading = new GPS();
		currentReading.Londitude = location.getLongitude();
		currentReading.Latitude = location.getLatitude();
		currentReading.Altitude = location.getAltitude();
		currentReading.Accuracy = location.getAccuracy();
		currentReading.TimeStamp = location.getTime();
		currentReading.Speed = location.getSpeed();
		
		
		Date d = new Date((long)currentReading.TimeStamp);
		if(FirstTime == -1)
		{
			FirstTime = currentReading.TimeStamp;
		}
		
		
		currentReading.phoneTimeStamp = GetUTMTime();
		GPS_TimeOffset = currentReading.TimeStamp - currentReading.phoneTimeStamp;
		
		currentReading.dateTime = DateTime.now();
		//Log.d("GPS","New GPS location found!");

		currentReading.LoadUTMValues();
		ALL_GPSreadings.add(currentReading);
		ALL_GPSreadings_UsedForWiFiLocation.add(currentReading);

		if(previous != null)
		{
			double currentDistance = GPS.Distance(previous, currentReading);

			DateTime currentTime = DateTime.now();
			DateTime previousTime = GetLastUpdateTime();
			if(previousTime != null)
			{
				if (Days.daysBetween(currentTime.withTimeAtStartOfDay(), previousTime.withTimeAtStartOfDay()).getDays() != 0) {

					ResetStoredDistance();
				}
			}

			Distance = UpdateStoredDistance(currentDistance);
		}
		
		if(ALL_GPSreadings.size() > MAXNUMBERFRAMES)
		{
			AnnounceData(frameLimitIntent,FrameLimitBroadcast,"true");
		}

		previous = currentReading;
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) 
	{
		 GPSstatus = status;
		switch (status) {
		case LocationProvider.OUT_OF_SERVICE:
			 
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			
			break;
		case LocationProvider.AVAILABLE:
			
			break;
		}
		
	}
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            
        	synchronized (this) 
        	{
				String action = intent.getAction();

				// When discovery finds a device
				if (LocationManager.KEY_LOCATION_CHANGED.equals(action))
				{
					// Get the BluetoothDevice object from the Intent
				Location location = intent.getParcelableExtra(LocationManager.GPS_PROVIDER);
				}
            
        	}
        }
    };
    
    public void AnnounceData(Intent intent,String name,String value)
	{
		intent.putExtra(name, value);
		context.sendBroadcast(intent);
	}

	public static GPS[] LoadGPS(String file, char delim, boolean headers)
	{
		ArrayList<GPS> allData = new ArrayList<GPS>();


		File f = new File(file);
		Scanner scan = null;
		if (f.exists())
		{
			try
			{
				scan = new Scanner(f);
			}
			catch (FileNotFoundException ex){}

		}





		while (scan.hasNextLine())
		{
			String line = scan.nextLine();

			String[] splitline = line.split(",");
			ArrayList<String> currentData_chars = new ArrayList<String>();
			for (int i = 0; i < splitline.length; i++)
			{
				if (splitline[i] != "")
				{
					currentData_chars.add(splitline[i]);
				}
			}

			GPS currentData = GPS.Parse(currentData_chars, !headers);


			allData.add(currentData);

		}
		scan.close();
		return allData.toArray(new GPS[allData.size()]);

	}

}
