package com.csri.ami.health_u.dataManagement.record.wifi;

/**
 * Data Structure to store link between wifi bssid and GPS lat/lon
 */
public class WiFiLocation 
{
	public String BSSID;
	public double Londitude;
	public double Latitude;
	public double TimeDiff;
	
	public WiFiLocation(String bssid,double lon,double lat,double diff)
	{
		BSSID = bssid;
		Londitude = lon;
		Latitude = lat;
		TimeDiff = diff;
	}
	
	public WiFiLocation(String line)
	{
		String[] line_split = line.split(",");
		TimeDiff = Double.parseDouble(line_split[2]);
		Londitude = Double.parseDouble(line_split[3]);
		Latitude = Double.parseDouble(line_split[4]);
		BSSID = line_split[6];
	}
	
	public String toString()
	{
		return 0/*userId*/ + "," + 0/*time created*/+ "," + TimeDiff/*TZ*/+ "," + Londitude + "," + Latitude + "," + 0/*OUI*/ + "," + BSSID + "\n";
	}
	
	
	
}
