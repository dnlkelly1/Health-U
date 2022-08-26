package com.csri.ami.health_u.dataManagement.record.wifi;

import android.os.Environment;

import com.csri.ami.health_u.dataManagement.record.GPS;
import com.csri.ami.health_u.dataManagement.record.GPS_Sensor;
import com.csri.ami.health_u.dataManagement.record.SensorRecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class WiFiLocationManager 
{
	ArrayList<WiFiLocation> wifiLocations;
	
	public static double ACCURACY_THRESHOLD = 100;
	
	private static double MillisecondThreshold =  GPS_Sensor.GPS_MIN_DISTANCECHANGE * 2;
	public int WiFi_GPS_TIMEDIFF = 15000;
	
	public WiFiLocationManager()
	{
		wifiLocations = new ArrayList<WiFiLocation>();
		Load();
	}
	
	public boolean UpdateLocations(ArrayList<WiFiScanResult> wifiAddresses,ArrayList<GPS> locations,int delayMilliSeconds)
	{
		boolean newAdded = false;
		if(wifiAddresses != null && locations != null)
		{
			
			for(int i=0;i<wifiAddresses.size();i++)
			{
				WiFiScanResult currentWiFi = wifiAddresses.get(i);
				
				int minIndex = -1;
				double minDiff = Double.MAX_VALUE;
				for(int j=0;j<locations.size();j++)
				{
					GPS currentGPS = locations.get(j);
					double timeDiff = Math.abs(currentWiFi.phoneTimeStamp - currentGPS.phoneTimeStamp);
					
					if(timeDiff < (delayMilliSeconds) && timeDiff < minDiff && currentGPS.Accuracy < ACCURACY_THRESHOLD)
					{
						minDiff = timeDiff;
						minIndex = j;
					}
				}
				
				if(minIndex != -1)
				{
					GPS currentGPS = locations.get(minIndex);
					WiFiLocation newLocation = new WiFiLocation(currentWiFi.BSSID,currentGPS.Londitude,currentGPS.Latitude,minDiff);
					
					wifiLocations.add(newLocation);
					newAdded = true;
				}
				
			}
			if(newAdded)
			{
				Save();
			}
		}
		return newAdded;
	}
	
	public GPS GetLocation(ArrayList<WiFiScanResult> wifiAddresses)
	{
		if(wifiLocations.size() == 0)
		{
			Load();
		}
		ArrayList<GPS> gps = new ArrayList<GPS>();
		if(wifiLocations != null)
		{
			for(int i=0;i<wifiLocations.size();i++)
			{
				for(int j=0;j<wifiAddresses.size();j++)
				{
					WiFiLocation current = wifiLocations.get(i);
					if(current.BSSID.compareTo(wifiAddresses.get(j).BSSID) == 0)
					{
						GPS currentFound = new GPS(current.Londitude,current.Latitude,current.BSSID);
						gps.add(currentFound);
					}
				}
			}
		}
		if(gps.size() > 0)
		{
			return GPS.Avg(gps);
		}
		else
		{
			return null;
		}
	}
	
	public static ArrayList<WiFiScanResult> GetOverlappingWifiAddresses(ArrayList<WiFiScanResult> a,ArrayList<WiFiScanResult> b)
	{
		ArrayList<WiFiScanResult> overlap = new ArrayList<WiFiScanResult>();
		for(int i=0;i<a.size();i++)
		{
			for(int j=0;j<b.size();j++)
			{
				if( a.get(i).BSSID.compareTo(b.get(j).BSSID) == 0)
				{
					overlap.add(a.get(i));
				}
			}
		}
		return overlap;
	}
	
	public static double TimeDiff(ArrayList<WiFiScanResult> a,ArrayList<WiFiScanResult> b)
	{
		double AVG = 0;
		for(int i=0;i<a.size();i++)
		{
			double avg_i =0;
			for(int j=0;j<b.size();j++)
			{
				avg_i += (a.get(i).phoneTimeStamp - b.get(j).phoneTimeStamp) / (double)b.size();
				
			}
			AVG += avg_i / (double)a.size();
		}
		return AVG;
	}
	
	private void Save()
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
				
				String fullfilename = f2.getAbsoluteFile()  +  SensorRecorder.SAVEFOLDER;
				File folderToWrite = new File(fullfilename);
				folderToWrite.mkdirs();
				boolean sdFound = folderToWrite.exists() && folderToWrite.canWrite();
				
				if(!sdFound)
				{
					fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER ;
					folderToWrite = new File(fullfilename);
					folderToWrite.mkdirs();
				}
				
				//String t = sensorID.replace(':', '_');
				File fileToWrite = new File(fullfilename, "WiFiLoc.txt");
				fileToWrite.createNewFile();
				FileOutputStream fos = new FileOutputStream(fileToWrite,false);
				OutputStreamWriter osw = new OutputStreamWriter(fos); 
				
				for(int i=0;i<wifiLocations.size();i++)
				{
					WiFiLocation current = wifiLocations.get(i);
					String temp = current.toString();
					osw.write(temp);
					
				}
				//osw.flush();
				osw.close();
				//fos.close();
			}
			catch(IOException e)
			{
				String s = e.getMessage();
			}
		}
	}
	
	private boolean Load()
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
				//File folderToRead = new File(fullfilename);
				File f2 = new File("/storage/extSdCard/");
				
				String fullfilename = f2.getAbsoluteFile()  +  SensorRecorder.SAVEFOLDER;
				File folderToWrite = new File(fullfilename);
				folderToWrite.mkdirs();
				boolean sdFound = folderToWrite.exists() && folderToWrite.canWrite();
				
				if(!sdFound)
				{
					fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER ;
					folderToWrite = new File(fullfilename);
					folderToWrite.mkdirs();
				}
				
				//String t = sensorID.replace(':', '_');
				File fileToRead = new File(fullfilename,"WiFiLoc.txt");
				if(fileToRead.exists())
				{
				
					FileInputStream fis = new FileInputStream(fileToRead);
					InputStreamReader isw = new InputStreamReader(fis); 
					Scanner scanner = new Scanner(fileToRead,"UTF-8");
					
					wifiLocations = new ArrayList<WiFiLocation>();
					String line = scanner.nextLine();
					while(line != null)
					{
						WiFiLocation current = new WiFiLocation(line);
						wifiLocations.add(current);
						try
						{
						line = scanner.nextLine();
						}
						catch(NoSuchElementException nse)
						{
							break;
						}
					}
					scanner.close();
					isw.close();
					
					return true;
				}
				else
				{
					return false;
				}
			}
			catch(Exception e)
			{
				return false;
			}
			
		}
		else
		{
			return false;
		}
	}
}
