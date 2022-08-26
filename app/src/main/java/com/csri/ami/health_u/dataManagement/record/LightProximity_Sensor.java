package com.csri.ami.health_u.dataManagement.record;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.csri.ami.health_u.dataManagement.record.motion.Vector3D;


public class LightProximity_Sensor 
{
	boolean previous = true;
	Vector3D lightData;
	Vector3D proximityData;
	//private static Sensor sensor;
	private static SensorManager sensorManager;
	private static LightProximity_Listener listener;
	private static boolean running = false;
	Context context;

	public LightProximity_Sensor(Context t)
	{


		context = t;

		sensorManager = (SensorManager) t.getSystemService(Context.SENSOR_SERVICE);
		lightData = new com.csri.ami.health_u.dataManagement.record.motion.Vector3D();
		lightData.X = 300;
		proximityData = new Vector3D();
		proximityData.X = 5;
		//sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);


	}

	public void startListening(LightProximity_Listener lightProximityListener) 
	{


		running = sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_GAME);
		running = sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_GAME);
		listener = lightProximityListener;
	}

	private SensorEventListener sensorEventListener = new SensorEventListener() 
	{
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}

		public void onSensorChanged(SensorEvent event) 
		{
			switch (event.sensor.getType())
			{
			case Sensor.TYPE_LIGHT:
				lightData = new Vector3D();
				lightData.X = event.values[0];
				break;
			case Sensor.TYPE_PROXIMITY:
				proximityData = new Vector3D();
				proximityData.X = event.values[0];
				break;
			}
			boolean onoutside = lightData.X > 50 || proximityData.X > 1;
			
			if(onoutside != previous)
			{
				previous = onoutside;
				listener.onLightProximityChanged(onoutside);
			}
			
		}
	};
}



