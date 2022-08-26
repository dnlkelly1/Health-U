package com.csri.ami.health_u.dataManagement.record.wifi;

public class WiFiScanResult
{
	public long phoneTimeStamp;
	public String BSSID;
	public String SSID;
	
	public WiFiScanResult(long time,String bssid,String ssid)
	{
		phoneTimeStamp = time;
		BSSID = bssid;
		SSID = ssid;
	}
}
