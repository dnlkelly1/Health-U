package com.csri.ami.health_u.dataManagement.record.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.csri.ami.health_u.dataManagement.record.GPS_Sensor;
import com.csri.ami.health_u.dataManagement.record.SensorRecorder;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class WiFi_Sensor 
{
	public boolean TURN_OFF_WIFI = false;
	WifiManager wifi;       
	public int size = 0;
	Context context;
	BroadcastReceiver reciever;
    List<ScanResult> results;
    
    boolean Scanning = false;
    int ShutdownRecheckDelay = SensorRecorder.WiFiDeviceRecheckDelay_Fast/2;
    int RecheckDelay = SensorRecorder.WiFiDeviceRecheckDelay_Fast;
    Timer scheduler;
	private Handler handler = new Handler();
	private Handler shutdownhandler = new Handler();
	private Handler resethandler = new Handler();
	
	private ArrayList<WiFiScanResult> ALLDevicesFullSet;
	private ArrayList<WiFiScanResult> ALLDevicesFullSet_UsedForSaving;
	private ArrayList<WiFiScanResult> ALLDevicesFullSet_UsedForWiFiLocation;
    private ArrayList<WirelessMetaData> timeStamps;
	private ArrayList<WirelessMetaData> timeStamps_UsedForSaving;
	
	public static final String WifiFoundBroadcast = "WiFiFound";
	public static final String BROADCAST_ACTION_WIFIFOUND = "com.sensors.record.WIFI_Sesor.Found";
	Intent wifiFoundIntent;
	//private boolean TurnWiFiOffAfterScan = true;
	private long lastTimeRecieve = 0;
	
	private long turnedWiFiOnTime =0;
	private static long MaxWiFiONTime = 15000;
	private static boolean gotWifiReceive = false;
	
	public WiFi_Sensor(Context t)
	{
		
		context = t;
		ALLDevicesFullSet = new ArrayList<WiFiScanResult>();
		ALLDevicesFullSet_UsedForWiFiLocation = new ArrayList<WiFiScanResult>();
        timeStamps = new ArrayList<WirelessMetaData>();
        
		wifi = (WifiManager)t.getSystemService(Context.WIFI_SERVICE);
		wifi.createWifiLock(wifi.WIFI_MODE_SCAN_ONLY,"WIFI Sensor");
		if (wifi.isWifiEnabled() == false)
        {
			//TurnWiFiOffAfterScan = true;
            Toast.makeText(t.getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        } 
		
		wifiFoundIntent = new Intent(BROADCAST_ACTION_WIFIFOUND);
		lastTimeRecieve = GPS_Sensor.GetUTMTime();
		
		reciever = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent) 
            {
            	try
            	{
	               results = wifi.getScanResults();
	               size = results.size();
	               gotWifiReceive = true;
	               Log.i("WIFI SENSOR",size + " results revieved");
	               
	               WirelessMetaData currentMeta = new WirelessMetaData();
	           	   currentMeta.TimeStamp = GPS_Sensor.GetUTMTime();
	           	   currentMeta.DateTime = new Time();
	               currentMeta.DateTime.setToNow();
	           	   timeStamps.add(currentMeta);
	           	   
	           	   for(int i=0;i<results.size();i++)
	           	   {
	           		   WiFiScanResult res = new WiFiScanResult(GPS_Sensor.GetUTMTime(),results.get(i).BSSID,results.get(i).SSID);
	           		   ALLDevicesFullSet.add(res);
	           		   ALLDevicesFullSet_UsedForWiFiLocation.add(res);
	           	   }
	           	   
	           	   if(ALLDevicesFullSet != null && ALLDevicesFullSet.size() > 0)
	           	   {
	           		   lastTimeRecieve = ALLDevicesFullSet.get(ALLDevicesFullSet.size()-1).phoneTimeStamp;
	           		   
	           	   }
	           	   AnnounceData(wifiFoundIntent,WifiFoundBroadcast,"true");

            	}
            	catch(Exception ex)
            	{
            		SensorRecorder.WriteToLog(ex);
            	}

            }
        };
		
		
	}

	public void UpdateData()
	{
		if (!Scanning)
		{

			WifiInfo currentWifi =  getCurrentWifiConnection(context);


			if(currentWifi == null)
			{
				//Do Discovery of wifi devices
				try
				{
					context.unregisterReceiver(reciever);
				}
				catch (IllegalArgumentException ex){}
				context.registerReceiver(reciever,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
				DoDiscovery();
			}
			else
			{
				//store results of last set of wifi devices scanned
				WiFiScanResult res = new WiFiScanResult(GPS_Sensor.GetUTMTime(),currentWifi.getBSSID(),currentWifi.getSSID());

				ALLDevicesFullSet.add(res);
				ALLDevicesFullSet_UsedForWiFiLocation.add(res);

				if(ALLDevicesFullSet != null && ALLDevicesFullSet.size() > 0)
				{
					lastTimeRecieve = ALLDevicesFullSet.get(ALLDevicesFullSet.size()-1).phoneTimeStamp;

				}
				AnnounceData(wifiFoundIntent,WifiFoundBroadcast,"true");
			}
		}
	}
	

	public void AnnounceData(Intent intent,String name,String value)
	{
		intent.putExtra(name, value);
		context.sendBroadcast(intent);
	}
	
	public int NumberDevicesFoundOnLastScan()
	{
		return size;
	}
	
	public void Start()
	{
		context.registerReceiver(reciever,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

		
		handler.removeCallbacks(updateDataTask);
        handler.postDelayed(updateDataTask, 0);
        
        shutdownhandler.removeCallbacks(shutdownWifiTask);
        shutdownhandler.postDelayed(shutdownWifiTask,ShutdownRecheckDelay);
	}
	
	public void Stop()
	{
		try {
			shutdownhandler.removeCallbacks(shutdownWifiTask);
		}catch (Exception ex){}

		try {
			handler.removeCallbacks(updateDataTask);
		}catch (Exception ex){}
		try {
			context.unregisterReceiver(reciever);
		}catch (Exception ex){}

		wifi = null;
	}

	public void UnregisterRecievers()
	{
		try {
			context.unregisterReceiver(reciever);
		} catch (Exception ex){}
	}
	


	public long TimeSinceLastScanResult()
	{
		if(lastTimeRecieve > 0)
		{
			//WiFiScanResult lastResult = ALLDevicesFullSet.get(ALLDevicesFullSet.size()-1);
			return GPS_Sensor.GetUTMTime() - lastTimeRecieve;
		}
		else
		{
			return 0;
		}
	}


	public ArrayList<WiFiScanResult> GetWiFiForWifiLocation()
	{
		try
		{
			if(ALLDevicesFullSet_UsedForWiFiLocation != null)
			{
				ArrayList<WiFiScanResult> temp = (ArrayList<WiFiScanResult>) ALLDevicesFullSet_UsedForWiFiLocation.clone();
				if(!TURN_OFF_WIFI)
				{
					ALLDevicesFullSet_UsedForWiFiLocation.clear();
				}
				//NewDataReceived = false;
				return temp;
			}
			else
			{
				return null;
			}
			
		}
		catch(Exception e)
		{
			return null;
		}
			
		
	}
	
	public void Write(OutputStreamWriter osw) throws IOException
	{

		for(int i=0;i<ALLDevicesFullSet_UsedForSaving.size();i++)
		{
			WiFiScanResult device = ALLDevicesFullSet_UsedForSaving.get(i);

			osw.write(toString(device));
		}

	}
	
	private String toString(WiFiScanResult result)
	{
		String line = 0/*userId*/ + "," + result.phoneTimeStamp + "," + 0/*TZ*/ + "," + 0/*OUI*/ + "," + result.BSSID + "," + result.SSID + "," + 0 + ",0,0,0" + "\n";
		return line;
	}
	
	public void PrepareForSaving()
	{
		if(ALLDevicesFullSet != null)
		{
			ALLDevicesFullSet_UsedForSaving = (ArrayList<WiFiScanResult>)ALLDevicesFullSet.clone();
			ALLDevicesFullSet.clear();
			
			timeStamps_UsedForSaving = (ArrayList<WirelessMetaData>)timeStamps.clone();
			timeStamps.clear();
		}
		
	}
	
	public void ClearSavingBuffers()
	{
		synchronized(this)
		{
			
			ALLDevicesFullSet_UsedForSaving.clear();
			timeStamps_UsedForSaving.clear();
			
		}
	}


	/**
	 * Change the delay time between wifi scan
	 * @param delay time in milliseconds
	 */
	public void UpdateCheckSpeed(int delay)
	{	
		
		RecheckDelay = delay;
		handler.removeCallbacks(updateDataTask);
		handler.postDelayed(updateDataTask, RecheckDelay - 1000);
		Log.i("WiFi Update Speed:",Double.toString(RecheckDelay));

	}
	
	public void ResetWifi()
	{
		if(wifi != null)
		{
			ResetProcess t = new ResetProcess();
			t.startReset();
		}
	}
	
	public class ResetProcess implements Runnable {

		public void startReset() 
		{

			Thread t = new Thread(this);
			t.start();
		}

		public void run() 
		{
			Log.i("WiFI Sensor","Reset WIFI: Disable");
			wifi.setWifiEnabled(false);
			Scanning = false;
			if(handler != null)
			{
				handler.removeCallbacks(updateDataTask);
				shutdownhandler.removeCallbacks(shutdownWifiTask);
				try {
					context.unregisterReceiver(reciever);
				}
				catch (Exception ex){}
			}
			
			//resethandler.postDelayed(ResetWifiTask,5000);
			try
			{
				Thread.sleep(5000);
			}
			catch(InterruptedException e)
			{
			
			}
			if(wifi != null)
			{
				Log.i("WiFI Sensor","Reset WIFI: Enableing...");
				//wifi.setWifiEnabled(true);
				//gotWifiReceive = false; 
				if(handler != null)
				{
					//handler.removeCallbacks(updateDataTask);
					context.registerReceiver(reciever,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
					handler.postDelayed(updateDataTask, 0);
					shutdownhandler.postDelayed(shutdownWifiTask,ShutdownRecheckDelay);
				}
			}
		}
	}
	

	private Runnable shutdownWifiTask = new Runnable() {
    	public void run() 
    	{
    		long currentTime = GPS_Sensor.GetUTMTime();
    		long WifiOnTime = currentTime- turnedWiFiOnTime;
    		if(WifiOnTime > MaxWiFiONTime && wifi != null && gotWifiReceive && Scanning)
    		{
    			gotWifiReceive = false; 

				if(TURN_OFF_WIFI)
				{
					Log.i("WiFI Sensor","WIFI Disable");
					wifi.setWifiEnabled(false);
				}
    			Scanning = false;
				context.unregisterReceiver(reciever);
				Log.i("WiFI Sensor", "Unregistered Wifi Scanner");
    		}
    		
    		
    	
    		shutdownhandler.postDelayed(this, ShutdownRecheckDelay);
    	}
    	};
	
	private Runnable updateDataTask = new Runnable() {
    	public void run() {
    	
    	UpdateData();
    	handler.postDelayed(this, RecheckDelay);
    	}
    	};
    	


	public static WifiInfo getCurrentWifiConnection(Context context)
	{
		WifiInfo connectionInfo=null;
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (networkInfo.isConnected())
		{
			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			connectionInfo = wifiManager.getConnectionInfo();

		}
		return connectionInfo;
	}
	
	public void DoDiscovery()
	{
		if(wifi == null)
		{
			Log.i("WIFI SENSOR","WIFI NULL");
		}
		if(wifi != null)
		{
			
			Scanning = true;
			Log.i("WIFI SENSOR","Begin Scan");
			turnedWiFiOnTime = GPS_Sensor.GetUTMTime();
			if (wifi.isWifiEnabled() == false)
	        {

			   wifi.setWifiEnabled(true);
	        } 
			gotWifiReceive = false; 
			if(TURN_OFF_WIFI)
			{
				ALLDevicesFullSet_UsedForWiFiLocation.clear();
			}
			wifi.startScan();
			
		}
	}
	

}
