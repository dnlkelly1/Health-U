package com.csri.ami.health_u.dataManagement.record;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques.TimeSyncTable;

import org.joda.time.DateTime;

import java.util.ArrayList;

/**
 * Data Structure for storing GPS data
 * Structure has utility for power management through recording WiFi bss ids for particular locations
 * Detection of a know wifi bss id will allow location sensor recorded to switch GPS off
 */
public class GPS
{
	public double Londitude =0;
	public double Latitude =0;
	public double Altitude =0;
	public double Accuracy =0;
	public long TimeStamp=0;
	public long phoneTimeStamp;
	public DateTime dateTime;
	public double Speed=0;
	public static int UserID =0;
	public static int TZ =0;

	public ArrayList<String> wifi_bssids;
	
	public GPS()
	{
		
	}
	
	public GPS(double lon, double lat)
	{
		Londitude = lon;
		Latitude = lat;
	}

	public GPS(double lon, double lat,String bssid)
	{
		Londitude = lon;
		Latitude = lat;
		wifi_bssids = new ArrayList<String>();
		wifi_bssids.add(bssid);
	}

	public GPS(double lon, double lat,ArrayList<String> bssid)
	{
		Londitude = lon;
		Latitude = lat;
		wifi_bssids = bssid;
	}
	
	public static GPS Avg(ArrayList<GPS> data)
	{
		double avg_lon = 0;
		double avg_lat = 0;
		ArrayList<String> wifis = new ArrayList<String>();

		for(int i=0;i<data.size();i++)
		{
			avg_lon += data.get(i).Londitude / (double)data.size();
			avg_lat += data.get(i).Latitude / (double)data.size();

			if(data.get(i).wifi_bssids != null)
			{
				wifis.add(data.get(i).wifi_bssids.get(0));
			}
		}

		GPS gps =null;
		if(wifis.size() > 0)
		{
			gps = new GPS(avg_lon,avg_lat,wifis);
		}
		else
		{
			gps = new GPS(avg_lon,avg_lat);
		}

		return gps;
		
	}

	public static boolean checkForSameWifi(GPS a,GPS b)
	{
		boolean same = false;
		if(a.wifi_bssids != null && b.wifi_bssids != null)
		{
			for(int i=0;i<a.wifi_bssids.size();i++)
			{
				String a_bssid = a.wifi_bssids.get(i);

				for(int j=0;j<b.wifi_bssids.size();j++)
				{
					String b_bbsid = b.wifi_bssids.get(j);

					if(a_bssid.compareTo(b_bbsid) == 0)
					{
						same = true;
						break;
					}
				}
			}
		}

		return same;
	}

	public double timestamp;

	public double Alt;
	public double Lat;
	public double Long;
	//public double Speed;
	public double Heading;
	public double Horizontal_Acc;
	public double Horizontal_DOP;
	public double Vertical_Acc;
	public double Vertical_DOP;
	public double Speed_Accuracy;
	public double Heading_Accuracy;
	public double TimeSinceGPSBoot;
	public double UTM_Y;
	public double UTM_X;

	public double GPS_time_raw;
	public DateTime GPS_time;

	public String toString()
	{
		return UserID + "," + phoneTimeStamp + "," + TZ + "," + /*dateTime.toMillis(false)*/TimeStamp + "," + Altitude + "," + Londitude  + "," + Latitude + "," + Speed + "," + 0/*Heading*/ + "," + Accuracy + ",0,0,0,0,0,0" + "\n";
	}

	public static GPS Parse(String[] data, boolean scaletime)
	{
		GPS current = new GPS();

		double gpsRawTime = Double.parseDouble(data[3]);
		double phoneRawTime = Double.parseDouble(data[1]);

		double secs = phoneRawTime;
		double timeStart = secs;

		current.timestamp = timeStart;


		if (scaletime)
		{
			current.GPS_time_raw = gpsRawTime / 1000;
			current.GPS_time = TimeSyncTable.UnixTimeToDateTime(gpsRawTime / 1000);
		}
		else
		{
			current.GPS_time_raw = gpsRawTime;
			current.GPS_time = TimeSyncTable.UnixTimeToDateTime(gpsRawTime);
		}

		if (scaletime)
		{
			current.timestamp = phoneRawTime / 1000;
		}
		else
		{
			current.timestamp = phoneRawTime;
		}
		if (data.length >= 5)
			current.Alt = Double.parseDouble(data[4]);
		if (data.length >= 6)
			current.Long = Double.parseDouble(data[5]);
		if (data.length >= 7)
			current.Lat = Double.parseDouble(data[6]);
		if (data.length >= 8)
			current.Speed = Double.parseDouble(data[7]);
		if (data.length >= 9)
			current.Heading = Double.parseDouble(data[8]);
		if (data.length >= 10)
			current.Horizontal_Acc = Double.parseDouble(data[9]);
		if (data.length >= 11)
			current.Horizontal_DOP = Double.parseDouble(data[10]);
		if (data.length >= 12)
			current.Vertical_Acc = Double.parseDouble(data[11]);
		if (data.length >= 13)
			current.Vertical_DOP = Double.parseDouble(data[12]);
		if (data.length >= 14)
			current.Speed_Accuracy = Double.parseDouble(data[13]);
		if (data.length >= 15)
			current.Heading_Accuracy = Double.parseDouble(data[14]);
		if (data.length >= 16)
			current.TimeSinceGPSBoot = Double.parseDouble(data[15]);

		current.LoadUTMValues();

		return current;
	}

	public static GPS Parse(ArrayList<String> data, boolean scaletime)
	{
		GPS current = new GPS();

		double gpsRawTime = Double.parseDouble(data.get(3));
		double phoneRawTime = Double.parseDouble(data.get(1));

		double secs = phoneRawTime;
		double timeStart = secs;

		current.timestamp = timeStart;


		if (scaletime)
		{
			current.GPS_time_raw = gpsRawTime / 1000;
			current.GPS_time = TimeSyncTable.UnixTimeToDateTime(gpsRawTime / 1000);
		}
		else
		{
			current.GPS_time_raw = gpsRawTime;
			current.GPS_time = TimeSyncTable.UnixTimeToDateTime(gpsRawTime);
		}

		if (scaletime)
		{
			current.timestamp = phoneRawTime / 1000;
		}
		else
		{
			current.timestamp = phoneRawTime;
		}
		if (data.size() >= 5)
			current.Alt = Double.parseDouble(data.get(4));
		if (data.size() >= 6)
			current.Long = Double.parseDouble(data.get(5));
		if (data.size() >= 7)
			current.Lat = Double.parseDouble(data.get(6));
		if (data.size() >= 8)
			current.Speed = Double.parseDouble(data.get(7));
		if (data.size() >= 9)
			current.Heading = Double.parseDouble(data.get(8));
		if (data.size() >= 10)
			current.Horizontal_Acc = Double.parseDouble(data.get(9));
		if (data.size() >= 11)
			current.Horizontal_DOP = Double.parseDouble(data.get(10));
		if (data.size() >= 12)
			current.Vertical_Acc = Double.parseDouble(data.get(11));
		if (data.size() >= 13)
			current.Vertical_DOP = Double.parseDouble(data.get(12));
		if (data.size() >= 14)
			current.Speed_Accuracy = Double.parseDouble(data.get(13));
		if (data.size() >= 15)
			current.Heading_Accuracy = Double.parseDouble(data.get(14));
		if (data.size() >= 16)
			current.TimeSinceGPSBoot = Double.parseDouble(data.get(15));

		current.LoadUTMValues();

		return current;
	}



	private static double k0 = 0.9996;
	private static double a = 6378137;
	private static double f = 1 / 298.257223563;
	private static double b = a * (1 - f);
	private static double n = (a - b) / (a + b);
	private static double e = Math.sqrt(1 - (b * b) / (a * a));
	private static double e1sq = e * e;
	private static double e0 = e / Math.sqrt(1 - e * e);
	private static double esq = (1 - (b / a) * (b / a));
	private static double e0sq = e * e / (1 - e * e);
	private static double drad = Math.PI / 180;

	/**
	 * Map Projection calculations from GPS to Universal Transverse Mercator (UTM) co-ordinates.
	 */
	public void LoadUTMValues()
	{
		double phi = DegToRad(Lat);
		double lng = DegToRad(Long);

		double lngd = Long;// RadToDeg(Long);
		double latd = Lat;// RadToDeg(Lat);
		double utmz = 1 + Math.floor((lngd + 180) / 6);
		double latz = 0;//Latitude zone: A-B S of -80, C-W -80 to +72, X 72-84, Y,Z N of 84

		if (latd > -80 && latd < 72) { latz = Math.floor((latd + 80) / 8) + 2; }

		if (latd > 72 && latd < 84) { latz = 21; }

		if (latd > 84) { latz = 23; }

		double zcm = 3 + 6 * (utmz - 1) - 180;

		double N = a / Math.sqrt(1 - Math.pow(e * Math.sin(phi), 2));

		double T = Math.pow(Math.tan(phi), 2);

		//alert("T=  "+T);

		double C = e0sq * Math.pow(Math.cos(phi), 2);

		//alert("C=  "+C);

		double A = DegToRad(lngd - zcm) * Math.cos(phi);

		double E0 = (315 * a * Math.pow(n, 4) / 51) * (1 - n);
		double D0 = (35 * a * Math.pow(n, 3) / 48) * (1 - n + 11 * n * n / 16);
		double C0 = (15 * a * n * n / 16) * (1 - n + (3 * n * n / 4) * (1 - n));
		double B0 = (3 * a * n / 2) * (1 - n - (7 * n * n / 8) * (1 - n) + 55 * Math.pow(n, 4) / 64);
		double A0 = a * (1 - n + (5 * n * n / 4) * (1 - n) + (81 * Math.pow(n, 4) / 64) * (1 - n));

		double M = A0 * phi - B0 * Math.sin(2 * phi) + C0 * Math.sin(4 * phi) - D0 * Math.sin(6 * phi) + E0 * Math.sin(8 * phi);

		double K_i = M * k0;
		double K_ii = N * Math.sin(phi) * Math.cos(phi) / 2;
		double K_iii = ((N * Math.sin(phi) * Math.pow(Math.cos(phi), 3)) / 24) * (5 - Math.pow(Math.tan(phi), 2) + 9 * e1sq * Math.pow(Math.cos(phi), 2) + 4 * Math.pow(e1sq, 2) * Math.pow(Math.cos(phi), 4)) * k0;

		double deltaLong = (Long - zcm) * (Math.PI / 180);


		double M0 = 0;
		double x_utm = k0 * N * A * (1 + A * A * ((1 - T + C) / 6 + A * A * (5 - 18 * T + T * T + 72 * C - 58 * e0sq) / 120));//Easting relative to CM

		x_utm = x_utm + 500000;//Easting standard

		double y_utm = (K_i + K_ii * deltaLong * deltaLong + K_iii * Math.pow(deltaLong, 4));// k0 * (M - M0 + N * Math.Tan(phi) * (A * A * (1 / 2 + A * A * ((5 - T + 9 * C + 4 * C * C) / 24 + A * A * (61 - 58 * T + T * T + 600 * C - 330 * e0sq) / 720))));//Northing from equator

		double y_g = y_utm + 10000000;//yg = y global, from S. Pole

		if (y_utm < 0) { y_utm = 10000000 + y_utm; }

		UTM_X = x_utm / 1000;
		UTM_Y = y_utm / 1000;


	}

	private static double RadToDeg(double radians)
	{
		return radians * (180 / Math.PI);
	}

	private static double DegToRad(double degrees)
	{
		return degrees * (Math.PI / 180);
	}

	public static double Distance(GPS x, GPS y)//in Meters
	{
		//uses Haversine formula: http://www.movable-type.co.uk/scripts/latlong.html


		double R = 6371.8; // km
		double dLat = DegToRad(x.Latitude - y.Latitude);
		double dLon = DegToRad(x.Londitude - y.Londitude);
		double lat1 = DegToRad(x.Latitude);
		double lat2 = DegToRad(y.Latitude);

		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
				Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
		double c = 2 * Math.asin(Math.sqrt(a));// 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double d = R * c;
		return d * 1000;//multiply by 1000 to convert from KM to Meters
	}

	public static double UTMDistance(double utm_x, double utm_y, GPS x)
	{
		return Math.sqrt(Math.pow(utm_x - x.UTM_X, 2) + (Math.pow(utm_y - x.UTM_Y, 2)));
	}
}
