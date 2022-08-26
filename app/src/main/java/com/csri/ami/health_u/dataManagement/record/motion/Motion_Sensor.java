package com.csri.ami.health_u.dataManagement.record.motion;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import com.csri.ami.health_u.dataManagement.record.Filter;
import com.csri.ami.health_u.dataManagement.record.GPS_Sensor;
import com.csri.ami.health_u.dataManagement.record.LightProximity_Listener;
import com.csri.ami.health_u.dataManagement.record.LightProximity_Sensor;

import org.joda.time.DateTime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * MotionSensor handles sensor recording of Accelerometer and Gyroscope data
 * Includes power management techniques to reduce frequency of recording when movement variation is low
 * Includes batch writing of data to disk at regular intervals to ensure consistent recording over long persiods of time (tested for 1 week)
 *
 * @author Daniel Kelly
 * @version 1.0
 * @since 2014-06-1
 */
public class Motion_Sensor implements SensorEventListener, LightProximity_Listener
{

	CalibrationData calibData;
	boolean Calibrating = false;

	long CalibStartTime = 0;
	int calibration_numberFrames = 0;
	private static long CALIB_DURATION= 100;
	Vector3D accelData;
	Vector3D accelGlobalData;
	Vector3D orientationData;
	Vector3D gyroData;

	Vector3D gyroGlobalData;
	Vector3D magneticData;
	Vector3D orientationFiltered;

	Vector3D quat_test1;
	Vector3D quat_test2;

	Vector3D PreviousAccelData;
	Vector3D PreviousGyroData;

	boolean OutInOpen = false;
	public boolean Running = false;
	boolean FirstStartFound = false;
	LightProximity_Sensor lightProx;

	Filter lowpass;

	Filter bandpass;

	Gyro1DKalman filter_roll;
	Gyro1DKalman filter_pitch;
	Gyro1DKalman filter_pitch2;
	Gyro1DKalman filter_yaw;
	double PreviouPitch = 0;
	double PreviousRoll =0;

	private static double ACCEL_TIMECONSTANT = 0.05;
	Handler handler;

	private ArrayList<Vector3D> All_Accel;
	private ArrayList<Vector3D> All_Accel_SlidingWindow;
	private ArrayList<Vector3D> AccelRaw_CalibrationData;
	private ArrayList<Vector3D> All_Accel_UsedForSaving;

	private ArrayList<Vector3D> All_Orientation;
	private ArrayList<Vector3D> All_Orientation_SlidingWindow;
	private ArrayList<Vector3D> All_Orientation_UsedForSaving;
	private ArrayList<Vector3D> All_Orientation2;

	private ArrayList<Vector3D> All_Orientation2_UsedForSaving;
	private ArrayList<Vector3D> All_AccelGlobal;
	private ArrayList<Vector3D> All_AccelGlobal_SlidingWindow;
	private ArrayList<Vector3D> All_AccelGlobal_UsedForSaving;
	private ArrayList<Vector3D> All_Gyro;
	private ArrayList<Vector3D> All_Gyro_SlidingWindow;
	private ArrayList<Vector3D> All_Gyro_UsedForSaving;
	private ArrayList<Vector3D> All_GyroGlobal;
	private ArrayList<Vector3D> All_GyroGlobal_SlidingWindow;
	private ArrayList<Vector3D> All_GyroGlobal_UsedForSaving;
	private ArrayList<Quaternion> All_Quaternion;
	private ArrayList<Quaternion> All_Quaternion_SlidingWindow;

	private ArrayList<String> Labels;
	private ArrayList<Long> LabelFrames;

	private ArrayList<Double> AccelWindowForSpeedTest;

	private static double PreviousFrameTime = 0;
	private double FPS = 10;
	public boolean AccelSetToSlow = true;
	private double MinFastSensorDuration_ms = 1000 * 2;//10
	private double LastFastStartTime = 0;
	private double Moving_VariationThreshold = 0.35;//0.3

	public DateTime bands_DailyStart=null;
	public DateTime bands_HourStart=null;
	public DateTime bands_WindowStart=null;
	int VAR_BANDS_WINDOW_SIZE_MINS = 5;

	public double[] VariationBandThresholds = new double[]{Moving_VariationThreshold,1.25,2.5};
	public double[] VariationBands_Daily = new double[VariationBandThresholds.length+1];
	public double[] VariationBands_Hourly = new double[VariationBandThresholds.length+1];
	public double[] VariationBands_Window = new double[VariationBandThresholds.length+1];

	public double Variation;

	private boolean IsMoving = true;
	private boolean HasMoved = false;

	private static int SLIDING_WINDOW_SIZE = 3000;

	Intent frameLimitIntent;

	public static int MAX_NUMBER_ACCEL_FRAMES = (50 * 30) * 1;
	public static final String BROADCAST_ACTION_FRAMELIMIT = "com.sensors.record.Motion_Sesor.DataLimit";
	public static final String BROADCAST_ACTION_MOVING = "com.sensors.record.Motion_Sesor.SensorMoving";
	public static final String FrameLimitBroadcast = "FrameLimit";

	double PreviousReadyTime = 0;
	Context context;
	Sensor mSensor;
	SensorManager sensorManager = null;

	long startTime=-1;
	private boolean Saving = true;
	boolean SaveLimitAnnounced = false;
	public long FrameNo = 0;

	MadgwickAHRS testMg;

	public boolean AllowChangeToSlow=true;

	public Motion_Sensor(Context t,boolean SaveData,boolean allowChangeToSlow)
	{
		AllowChangeToSlow = allowChangeToSlow;

		calibData = new CalibrationData();
		calibData.LoadCalibrationValues();
		Saving = SaveData;


		float q_angle = 0.9f;//1.9  0.1
		float q_gyro = 0.4f;//0.9
		float r_angle = 1.1f;//1.9  0.1


		lowpass = new Filter();
		bandpass = new Filter();
		filter_roll = new Gyro1DKalman(q_angle, q_gyro, r_angle);
		filter_pitch = new Gyro1DKalman(q_angle, q_gyro, r_angle);
		filter_pitch2 = new Gyro1DKalman(q_angle, q_gyro, r_angle);
		filter_yaw = new Gyro1DKalman(q_angle, q_gyro, r_angle);

		testMg = new MadgwickAHRS((float)1/(float)50,1.0f);

		context = t;
		lightProx = new LightProximity_Sensor(t);

		handler = new Handler();
		sensorManager = (SensorManager) t.getSystemService(Context.SENSOR_SERVICE);
		mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);





		frameLimitIntent = new Intent(BROADCAST_ACTION_FRAMELIMIT );

	}



	public void AddLabelStart(String l)
	{
		if(Labels == null)
		{
			Labels = new ArrayList<String>();
			LabelFrames = new ArrayList<Long>();
		}

		Labels.add(l);
		LabelFrames.add(new Long(FrameNo));
	}

	public void EndCurrentLabel()
	{
		if(LabelFrames != null)
		{
			if(LabelFrames.size() % 2 != 0)
			{
				LabelFrames.add(new Long(FrameNo-1));
			}
		}
	}



	public void SetSaveState(boolean save)
	{
		Saving = save;
	}

	public void Start()
	{
		if(AccelWindowForSpeedTest != null)
		{
			AccelWindowForSpeedTest.clear();
		}
		FPS = 10;


		LastFastStartTime = 0;

		Resume();
	}



	public void PrepareForSaving()
	{

		if(All_Accel != null)
		{
			All_Accel_UsedForSaving = (ArrayList<Vector3D>)All_Accel.clone();
			All_Accel.clear();
		}
		if(All_Orientation != null)
		{
			All_Orientation_UsedForSaving = (ArrayList<Vector3D>)All_Orientation.clone();
			All_Orientation.clear();
		}
		if(All_Orientation2 != null)
		{
			All_Orientation2_UsedForSaving = (ArrayList<Vector3D>)All_Orientation2.clone();
			All_Orientation2.clear();
		}
		if(All_Gyro != null)
		{
			All_Gyro_UsedForSaving = (ArrayList<Vector3D>)All_Gyro.clone();
			All_Gyro.clear();
		}
		if(All_GyroGlobal != null)
		{
			All_GyroGlobal_UsedForSaving = (ArrayList<Vector3D>)All_GyroGlobal.clone();
			All_GyroGlobal.clear();
		}
		if(All_AccelGlobal != null)
		{
			All_AccelGlobal_UsedForSaving = (ArrayList<Vector3D>)All_AccelGlobal.clone();
			All_AccelGlobal.clear();
		}
		if(All_Quaternion != null)
		{
			All_Quaternion.clear();
		}

		SaveLimitAnnounced = false;

	}

	public void ClearLabelBuffers()
	{

		if(Labels != null)
		{
			Labels.clear();
		}
		if(LabelFrames != null)
		{

			LabelFrames.clear();
		}
	}

	public void ClearBuffers()
	{
		synchronized(this)
		{

			if(All_Accel != null)
				All_Accel.clear();
			if(All_Orientation != null)
				All_Orientation.clear();
			if(All_Orientation2 != null)
				All_Orientation2.clear();
			if(All_Gyro != null)
				All_Gyro.clear();
			if(All_GyroGlobal != null)
				All_GyroGlobal.clear();
			if(All_AccelGlobal != null)
				All_AccelGlobal.clear();
			if(All_Quaternion != null)
				All_Quaternion.clear();

		}
	}

	public void ClearSavingBuffers()
	{

		synchronized(this)
		{
			if(All_Accel_UsedForSaving != null)
				All_Accel_UsedForSaving.clear();
			if(All_Orientation_UsedForSaving != null)
				All_Orientation_UsedForSaving.clear();
			if(All_Orientation2_UsedForSaving != null)
				All_Orientation2_UsedForSaving.clear();
			if(All_Gyro_UsedForSaving != null)
				All_Gyro_UsedForSaving.clear();
			if(All_GyroGlobal_UsedForSaving != null)
				All_GyroGlobal_UsedForSaving.clear();
			if(All_AccelGlobal_UsedForSaving != null)
				All_AccelGlobal_UsedForSaving.clear();
		}

	}

	public boolean DataAvailableForSaving()
	{
		if(All_Accel_UsedForSaving != null)
		{
			if(All_Accel_UsedForSaving.size() > 0)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}


	public void WriteMotionValues(BufferedWriter osw,long timeoffset) throws IOException
	{

		{

			int accelSize = All_Accel_UsedForSaving.size();
			int gyrosize = All_Gyro_UsedForSaving.size();
			int orsize = All_Orientation_UsedForSaving.size();

			int size = accelSize;
			if(gyrosize > 0)
			{
				size = Math.min(size, gyrosize);
			}
			if(orsize > 0)
			{
				size = Math.min(size, orsize);
			}


			String allData = "";
			for(int i=0;i<size;i++)
			{
				if(i < All_Accel_UsedForSaving.size())// && i < All_Gyro_UsedForSaving.size() && i < All_Orientation_UsedForSaving.size())
				{
					try
					{
						Vector3D A = All_Accel_UsedForSaving.get(i);

						Vector3D G=null;
						if(i < All_Gyro_UsedForSaving.size())
						{
							G = All_Gyro_UsedForSaving.get(i);
						}

						Vector3D O=null;
						if(i < All_Orientation_UsedForSaving.size())
						{
							O = All_Orientation_UsedForSaving.get(i);
						}


						long utm_correctedTime = A.UTMDateTime + timeoffset;

						int setToSlow = 0;
						if(A.SetToSlowMode)
						{
							setToSlow = 1;
						}

						String line = Double.toString(A.MilliTimeStamp  / 1000)  + ","
								+ Double.toString(utm_correctedTime)  + ","
								+ Integer.toString(setToSlow) + ","
								+ Double.toString(A.X) + "," //*
								+ Double.toString(A.Y) + "," //*
								+ Double.toString(A.Z) + ",";//*;

						if(G != null)
						{
							line += Double.toString(G.X) + "," //*
									+ Double.toString(G.Y) + "," //*
									+ Double.toString(G.Z) + ","; //*
						}
						else
						{
							line += Double.toString(Double.NaN) + "," + Double.toString(Double.NaN) + "," + Double.toString(Double.NaN) + ",";

						}

						if(O != null)
						{
							line += Double.toString(O.X) + "," //*
									+ Double.toString(O.Y) + "," //*
									+ Double.toString(O.Z) + "\n";
						}
						else
						{
							line += Double.toString(Double.NaN) + "," + Double.toString(Double.NaN) + "," + Double.toString(Double.NaN) + "\n";

						}



						osw.write(line);


					}
					catch(IndexOutOfBoundsException e)
					{
						break;
					}



				}
			}

		}

	}



	public void Resume()
	{

		All_Accel = new ArrayList<Vector3D>();
		All_Orientation = new ArrayList<Vector3D>();
		All_Orientation2 = new ArrayList<Vector3D>();
		All_Gyro = new ArrayList<Vector3D>();
		All_GyroGlobal = new ArrayList<Vector3D>();
		All_AccelGlobal = new ArrayList<Vector3D>();
		All_Quaternion = new ArrayList<Quaternion>();

		if(AllowChangeToSlow)
		{
			StartListeningSlow();
			AccelSetToSlow = true;
		}
		else
		{
			StartListeningFast();
			AccelSetToSlow = false;
		}


	}

	boolean fast = true;
	public void ToggleSpeed()
	{

		if(fast)
		{
			boolean temp = AllowChangeToSlow;
			AllowChangeToSlow = true;
			Stop();
			StartListeningSlow();
			fast = false;
			AllowChangeToSlow = temp;
		}
		else
		{
			Stop();
			StartListeningFast();
			fast = true;
		}
	}



	public void StartListeningSlow()
	{
		if(AllowChangeToSlow)
		{
			boolean r1 = sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_NORMAL);//NORMAL



			if(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null)
			{
				gyroAvailableOnPhone = true;
			}
			else
			{
				gyroAvailableOnPhone = false;
			}

			GyroCurrentlyDisabled = true;

			Running  = r1;// && r3;
		}
	}

	boolean gyroAvailableOnPhone = true;
	boolean GyroCurrentlyDisabled = false;
	public void StartListeningFast()
	{
		boolean r1 = sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_GAME);


		boolean r3 = sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),SensorManager.SENSOR_DELAY_GAME);
		gyroAvailableOnPhone = r3;
		GyroCurrentlyDisabled = !r3;

		Running  = r1 ;
	}

	public void Stop()
	{

		sensorManager.unregisterListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

		sensorManager.unregisterListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
		Running =false;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	public Vector3D[] getAccelWindow()
	{
		if(All_Accel_SlidingWindow != null)
		{
			return ToArray(All_Accel_SlidingWindow);
		}
		else
		{
			return null;
		}
	}

	public Vector3D[] ToArray(ArrayList<Vector3D> data)
	{
		Vector3D[] temp = new Vector3D[data.size()];

		for(int i=0;i<data.size();i++)
		{
			temp[i] = data.get(i);
		}
		return temp;
	}

	public Quaternion[] ToArray_Q(ArrayList<Quaternion> data)
	{
		Quaternion[] temp = new Quaternion[data.size()];

		for(int i=0;i<data.size();i++)
		{
			temp[i] = data.get(i);
		}
		return temp;
	}

	public Vector3D[] getGyroWindow()
	{
		synchronized(this)
		{
			if(All_Gyro_SlidingWindow != null)
			{
				return ToArray(All_Gyro_SlidingWindow);
			}
			else
			{
				return null;
			}
		}
	}

	public Quaternion[] getQuaternioWindow()
	{
		synchronized(this)
		{
			if(All_Quaternion_SlidingWindow != null)
			{
				return ToArray_Q(All_Quaternion_SlidingWindow);
			}
			else
			{
				return null;
			}
		}
	}

	public Vector3D[] getGyroGlobalWindow()
	{
		synchronized(this)
		{
			if(All_GyroGlobal_SlidingWindow != null)
			{
				return ToArray(All_GyroGlobal_SlidingWindow);
			}
			else
			{
				return null;
			}
		}
	}

	public Vector3D[] getAccelGlobalWindow()
	{
		if(All_AccelGlobal_SlidingWindow != null)
		{
			return ToArray(All_AccelGlobal_SlidingWindow);
		}
		else
		{
			return null;
		}
	}
	public Vector3D[] getOrientationWindow()
	{
		if(All_Orientation_SlidingWindow != null)
		{
			return ToArray(All_Orientation_SlidingWindow);
		}
		else
		{
			return null;
		}
	}


	public Vector3D getAcceleration()
	{

		return accelData;

	}

	public Vector3D getAccelerationGlobal()
	{

		return accelGlobalData;

	}

	public Vector3D getOrientation()
	{

		return orientationData;

	}

	public Vector3D getQuaternionFiltered()
	{

		return quat_test1;
	}

	public Vector3D getQuaternion2Filtered()
	{

		return quat_test2;

	}

	public Vector3D getOrientationFiltered()
	{

		return orientationFiltered;

	}

	public Vector3D getGyro()
	{

		return gyroData;

	}

	public Vector3D getGyroGlobal()
	{

		return gyroGlobalData;

	}

	public Vector3D getMag()
	{

		return magneticData;

	}

	public void onLightProximityChanged(boolean outInOpen)
	{
		OutInOpen  = outInOpen;
		if(outInOpen && Running)
		{
			//Stop();
		}
		else if(!outInOpen)
		{
			FirstStartFound = true;
			if(!Running)
			{
				StartListeningSlow();
			}
		}
	}

	public double Average_X_val(ArrayList<Vector3D> data)
	{
		double avg = 0;
		int startPoint = data.size() - (int)CALIB_DURATION;
		if(startPoint < 0)
		{
			startPoint = 0;
		}
		double len = data.size() - startPoint;

		for(int i=startPoint;i<data.size();i++)
		{
			avg += data.get(i).X / len;
		}
		return avg;
	}

	public double Average_Y_val(ArrayList<Vector3D> data)
	{
		double avg = 0;
		int startPoint = data.size() - (int)CALIB_DURATION;
		if(startPoint < 0)
		{
			startPoint = 0;
		}
		double len = data.size() - startPoint;

		for(int i=startPoint;i<data.size();i++)
		{
			avg += data.get(i).Y / len;
		}
		return avg;
	}

	public double Average_Z_val(ArrayList<Vector3D> data)
	{
		double avg = 0;
		int startPoint = data.size() - (int)CALIB_DURATION;
		if(startPoint < 0)
		{
			startPoint = 0;
		}
		double len = data.size() - startPoint;

		for(int i=startPoint;i<data.size();i++)
		{
			avg += data.get(i).Z / len;
		}
		return avg;
	}

	public void SetCalibtation()
	{
		if(!Calibrating)
		{
			Calibrating = true;

			AccelRaw_CalibrationData = new ArrayList<Vector3D>();
			CalibStartTime = System.currentTimeMillis();
		}
	}

	private CalibrationType GetCalibrationType(double x,double y,double z)
	{

		double X_UpDiff = Math.abs(x -CalibrationData.GRAVITY);
		double X_DownDiff = Math.abs(x - CalibrationData.NEG_GRAVITY);
		double Y_UpDiff = Math.abs(y -CalibrationData.GRAVITY);
		double Y_DownDiff = Math.abs(y - CalibrationData.NEG_GRAVITY);
		double Z_UpDiff = Math.abs(z -CalibrationData.GRAVITY);
		double Z_DownDiff = Math.abs(z - CalibrationData.NEG_GRAVITY);

		double[] diffs = new double[]{X_UpDiff,X_DownDiff,Y_UpDiff,Y_DownDiff,Z_UpDiff,Z_DownDiff};
		CalibrationType[] types = new CalibrationType[]{CalibrationType.X_Accel_UP,CalibrationType.X_Accel_DOWN,
				CalibrationType.Y_Accel_UP,CalibrationType.Y_Accel_DOWN,
				CalibrationType.Z_Accel_UP,CalibrationType.Z_Accel_DOWN};

		double min = Double.MAX_VALUE;
		int index = -1;
		for(int i=0;i<diffs.length;i++)
		{
			if(diffs[i] < min)
			{
				index = i;
				min = diffs[i];
			}
		}

		return types[index];

	}

	private void CalibrationChecks(Vector3D accel)
	{
		//CheckMagMaxAndMins();
		if(Calibrating)
		{
			AccelRaw_CalibrationData.add(accel);
			calibration_numberFrames++;
			//int packetsize = packets.size();

			if(calibration_numberFrames > CALIB_DURATION)//if the calibration has reached the of of the set window size
			{

				double x = Average_X_val(AccelRaw_CalibrationData);
				double y = Average_Y_val(AccelRaw_CalibrationData);
				double z = Average_Z_val(AccelRaw_CalibrationData);
				CalibrationType t = GetCalibrationType(x,y,z);

				if(t == CalibrationType.X_Accel_UP)
				{
					calibData.xMagMax = x;
				}
				else if(t == CalibrationType.X_Accel_DOWN)
				{
					calibData.xMagMin = x;
				}
				else if(t == CalibrationType.Y_Accel_UP)
				{
					calibData.yMagMax = y;
				}
				else if(t == CalibrationType.Y_Accel_DOWN)
				{
					calibData.yMagMin = y;
				}
				else if(t == CalibrationType.Z_Accel_UP)
				{

					calibData.zMagMax = z;

				}
				else if(t == CalibrationType.Z_Accel_DOWN)
				{
					calibData.zMagMin = z;
				}

				calibData.SaveCalibrationData();


				AccelRaw_CalibrationData.clear();
				AccelRaw_CalibrationData = null;
				calibration_numberFrames=0;
				Calibrating = false;
			}
		}
	}

	private boolean ScreenState = true;
	public void InformThatScreenWasTurnedOn()
	{

		ScreenState = true;
		if(AllowChangeToSlow)
		{
			Stop();
			StartListeningSlow();
			AccelSetToSlow = true;
		}
	}

	public void InformThatScreenWasTurnedOff()
	{
		ScreenState = false;

	}

	private void UpdateIMUSpeed(boolean EnableFast,double time)
	{
		if(AllowChangeToSlow)
		{
			if(EnableFast)
			{
				LastFastStartTime = time;
			}

			if(!ScreenState && AccelSetToSlow && EnableFast)//switch from slow to fast
			{
				Log.i("MotionSensor","Set to Fast: AccelSetToSlow=" + AccelSetToSlow + " EnableFast=" + EnableFast);
				Stop();
				StartListeningFast();
				AccelSetToSlow = false;


			}
			else if(!AccelSetToSlow && !EnableFast)//switch from fast to slow
			{
				double diff = time - LastFastStartTime;
				if(diff >= MinFastSensorDuration_ms)
				{
					Log.i("MotionSensor","Set to Slow: AccelSetToSlow=" + AccelSetToSlow + " EnableFast=" + EnableFast);
					Stop();
					StartListeningSlow();
					AccelSetToSlow = true;
				}
			}
		}
	}

	private double IsDynamicMoving_var;
	private boolean IsDynamicMoving(Vector3D x,double threshold)
	{
		if(AccelWindowForSpeedTest == null)
		{
			AccelWindowForSpeedTest = new ArrayList<Double>();
		}

		AccelWindowForSpeedTest.add(x.Magnitude);





		while(AccelWindowForSpeedTest.size() > Math.round(FPS))
		{
			AccelWindowForSpeedTest.remove(0);
		}

		double avg =0;
		for(int i=0;i< AccelWindowForSpeedTest.size();i++)
		{
			avg += AccelWindowForSpeedTest.get(i) / (double)AccelWindowForSpeedTest.size();
		}

		double var =0;
		for(int i=0;i< AccelWindowForSpeedTest.size();i++)
		{
			var += Math.abs(avg -  AccelWindowForSpeedTest.get(i)) / (double)AccelWindowForSpeedTest.size();
		}
		Variation = var;
		IsDynamicMoving_var = var;

		if(var < threshold)
		{
			return false;
		}
		else
		{
			return true;
		}


	}

	public boolean HasMovedSinceLastCheck()
	{
		if(HasMoved)
		{
			HasMoved = false;
			return true;
		}
		else
		{
			return false;
		}
	}

	public void AnnounceData(Intent intent,String name,String value)
	{
		intent.putExtra(name, value);
		context.sendBroadcast(intent);
	}





	public void SetKalmanParams(float x,float y,float z)
	{
		filter_pitch.Update(x, y, z);
		filter_pitch2.Update(x, y, z);
		filter_roll.Update(x, y, z);
	}

	public void UpdateVariationBand(double var,double frameLen)
	{
		/////////////Days Variation/////////////////
		DateTime new_bands_DailyStart = DateTime.now().withTimeAtStartOfDay();
		if(bands_DailyStart != null)
		{
			if(bands_DailyStart.getMillis() != new_bands_DailyStart.getMillis())
			{
				VariationBands_Daily = new double[VariationBandThresholds.length+1];
			}

		}
		bands_DailyStart = new_bands_DailyStart;
		////////////////////////////////////////

		//////////////Hours Variation//////////////////
		DateTime new_bands_HourStart = bands_DailyStart.plusHours(DateTime.now().getHourOfDay());
		if(bands_HourStart != null)
		{
			if(bands_HourStart.getMillis() != new_bands_HourStart.getMillis())
			{
				VariationBands_Hourly = new double[VariationBandThresholds.length+1];
			}
		}
		bands_HourStart = new_bands_HourStart;
		////////////////////////////////////////

		////////////Minutes Variation////////////////
		int minsPassedToday = DateTime.now().getMinuteOfDay();

		int remaineder = minsPassedToday % VAR_BANDS_WINDOW_SIZE_MINS;
		int windowStartTimeMinutes = minsPassedToday - remaineder;

		DateTime new_bands_WindowStart = bands_DailyStart.plusMinutes(windowStartTimeMinutes);
		if(bands_WindowStart != null)
		{
			if(bands_WindowStart.getMillis() != new_bands_WindowStart.getMillis())
			{
				VariationBands_Window = new double[VariationBandThresholds.length+1];
			}
		}
		bands_WindowStart = new_bands_WindowStart;
		////////////////////////////////////////

		VariationBands_Daily = UpdateBands(frameLen,var,VariationBands_Daily);
		VariationBands_Hourly = UpdateBands(frameLen,var,VariationBands_Hourly);
		VariationBands_Window = UpdateBands(frameLen,var,VariationBands_Window);


	}



	private double[] UpdateBands(double frameLen,double var,double[] bandData)
	{
		int index = 0;
		for (int i = 0; i < VariationBandThresholds.length; i++)
		{
			if (i == 0 && var < VariationBandThresholds[i])
			{
				index = 0;
			}
			else if (i == VariationBandThresholds.length - 1 && var > VariationBandThresholds[i])
			{
				index = bandData.length-1;
			}
			else if (var > VariationBandThresholds[i] && var < VariationBandThresholds[i + 1])
			{
				index = i+1;
			}
		}

		bandData[index] += frameLen;
		return bandData;
	}

	public int movingCount=0;
	private static boolean StoreSlidingWindows = false;


	@Override
	public void onSensorChanged(SensorEvent event)
	{
		Vector3D accelData_r=null;

		boolean Ready = false;
		long nanoTime = System.nanoTime();
		long utmTime = GPS_Sensor.GetUTMTime();

		switch (event.sensor.getType())
		{
			case Sensor.TYPE_ACCELEROMETER:
				//Log.v("OnSensorChanged","Accel");
				accelData_r = new Vector3D();
				accelData_r.X = event.values[0];
				accelData_r.Y = event.values[1];
				accelData_r.Z = event.values[2];
				accelData_r.FrameNumber = FrameNo;
				accelData_r.SetToSlowMode = AccelSetToSlow;
				accelData_r.Update();

				if(startTime == -1){
					startTime = event.timestamp ;
					PreviousReadyTime = startTime;
				}

				CalibrationChecks(accelData_r);

				double milliseconds = (double)(event.timestamp - startTime) / 1000000;//(nanoTime - startTime) / 1000000;
				accelData_r.MilliTimeStamp = milliseconds;
				if(PreviousFrameTime != 0)
				{
					double diff = milliseconds - PreviousFrameTime;
					FPS = (double)1 / diff;
				}
				PreviousFrameTime = milliseconds;
				accelData_r.UTMDateTime = utmTime;

				accelData_r =  calibData.Calibrate(accelData_r);
				accelData = ApplyLowPassFilter(accelData_r,PreviousAccelData);

				Ready = true;

				break;


			case Sensor.TYPE_MAGNETIC_FIELD:
				magneticData = new Vector3D();
				magneticData.X = event.values[0];
				magneticData.Y = event.values[1];
				magneticData.Z = event.values[2];
				double millisecondsRV = (double)(event.timestamp - startTime) / 1000000;//(nanoTime - startTime) / 1000000;
				magneticData.MilliTimeStamp = millisecondsRV;
				magneticData.UTMDateTime = utmTime;
				//magneticData.dateTime.setToNow();

				//All_Mag.add(magneticData);
				break;
			case Sensor.TYPE_GYROSCOPE:
				gyroData = new Vector3D();
				gyroData.X = event.values[0];
				gyroData.Y = event.values[1];
				gyroData.Z = event.values[2];
				gyroData.Update();
				double millisecondsG = (double)(event.timestamp - startTime) / 1000000;//(nanoTime - startTime) / 1000000;
				gyroData.MilliTimeStamp = millisecondsG ;
				gyroData.UTMDateTime = utmTime;
				//gyroData.dateTime.setToNow();
				gyroData.FrameNumber = FrameNo;
				gyroData.SetToSlowMode = AccelSetToSlow;
				break;



		}



		if(accelData != null && Ready && Running)
		{
			double frameTimeDiff = 0.01;
			if(PreviousAccelData != null)
			{
				frameTimeDiff = accelData.MilliTimeStamp - PreviousReadyTime;

			}
			float samplePer=-1;
			if(frameTimeDiff != 0)
			{
				samplePer = (float) (frameTimeDiff / (double) 1000);
				if(samplePer > 0 && samplePer <=1)
				{
					FPS = 1 / samplePer;
					testMg.SamplePeriod = samplePer;


				}
			}
			PreviousReadyTime = accelData.MilliTimeStamp;


			boolean Previous = IsMoving;

			IsMoving = IsDynamicMoving(accelData,Moving_VariationThreshold);

			if(samplePer != -1)
			{
				UpdateVariationBand(IsDynamicMoving_var,samplePer);
			}

			if(IsMoving)
			{
				HasMoved = true;
			}
			UpdateIMUSpeed(IsMoving,accelData_r.MilliTimeStamp);


			if(Saving)
			{
				//Log.v("--Motion","Accel Stored");
				All_Accel.add(accelData);
			}

			if(StoreSlidingWindows) {
				All_Accel_SlidingWindow = UpdateSlidingWindows(accelData, All_Accel_SlidingWindow);
			}






			//////////angle filter////////////////////////////////////
			Vector3D tempAccel = accelData.Clone();
			double swap = tempAccel.Y;
			tempAccel.Y = tempAccel.X;
			tempAccel.X = swap;
			tempAccel.X = tempAccel.X / SensorManager.GRAVITY_EARTH;
			tempAccel.Y = tempAccel.Y / SensorManager.GRAVITY_EARTH;
			tempAccel.Z = tempAccel.Z / SensorManager.GRAVITY_EARTH;
			tempAccel.SetToSlowMode = AccelSetToSlow;
			tempAccel = Noramilze(tempAccel);

			double tmp_roll = PredictAccG_roll(tempAccel.Z,tempAccel.Y,tempAccel.X,PreviousRoll);
			double tmp_pitch = PredictAccG_pitch(tempAccel.Z,tempAccel.Y,tempAccel.X,PreviouPitch);

			double pitchangle = tmp_pitch;

			double rollangle = -1 * tmp_roll;

			if((gyroAvailableOnPhone && GyroCurrentlyDisabled) )
			{
				gyroData = new Vector3D();
				gyroData.X = 0;
				gyroData.Y = 0;
				gyroData.Z = 0;
				gyroData.Update();
				double millisecondsG = (double)(event.timestamp - startTime) / 1000000;//(nanoTime - startTime) / 1000000;
				gyroData.MilliTimeStamp = millisecondsG ;
				gyroData.UTMDateTime = utmTime;

				gyroData.FrameNumber = FrameNo;
				gyroData.SetToSlowMode = AccelSetToSlow;

				if(Saving)
				{
					All_Gyro.add(gyroData);
				}
			}
			else if(!gyroAvailableOnPhone)
			{
				gyroData = new Vector3D();
				gyroData.X = Double.NaN;
				gyroData.Y = Double.NaN;
				gyroData.Z = Double.NaN;

				double millisecondsG = (double)(event.timestamp - startTime) / 1000000;//(nanoTime - startTime) / 1000000;
				gyroData.MilliTimeStamp = millisecondsG ;
				gyroData.UTMDateTime = utmTime;

				gyroData.FrameNumber = FrameNo;
				gyroData.SetToSlowMode = AccelSetToSlow;

				if(Saving)
				{
					All_Gyro.add(gyroData);
				}

			}
			else if(gyroData != null && !GyroCurrentlyDisabled)
			{


				if(Saving)
				{
					All_Gyro.add(gyroData);
				}

				if(StoreSlidingWindows) {
					All_Gyro_SlidingWindow = UpdateSlidingWindows(gyroData, All_Gyro_SlidingWindow);
				}

				double tmp_roll_gyro_rad = -1 * /*DegreesToRad*/(Predict_gyroRoll(tempAccel,gyroData));//yes

				double tmp_pitch_gyro_rad = -1 * /*DegreesToRad*/(Predict_gyroPitch(tempAccel,gyroData));


				filter_roll.ars_predict((float)tmp_roll_gyro_rad, ((float)frameTimeDiff)/1000);//yes
				rollangle = -1 * filter_roll.ars_update((float)tmp_roll);//yes

				pitchangle = tmp_pitch;

				if(Math.abs(tmp_pitch - PreviouPitch) >= (Math.PI/2)*0.9  && tmp_pitch_gyro_rad < Math.PI / 2)
				{

					filter_pitch.manualSet((float)tmp_pitch);
				}
				else
				{

					filter_pitch.ars_predict((float)tmp_pitch_gyro_rad, ((float)frameTimeDiff)/1000);
					pitchangle = filter_pitch.ars_update((float)tmp_pitch);


				}
			}



			PreviouPitch = pitchangle;
			PreviousRoll = rollangle;
			//end angle filter///////////////////////////////////////////////////////////////////////////

			double millisecondsRV = (double)(event.timestamp - startTime) / 1000000;//(nanoTime - startTime) / 1000000;



			orientationFiltered  = new Vector3D();
			orientationFiltered.X = pitchangle;// a_r.get(0, 0);// newHeading;//  accelFiltered[0];
			orientationFiltered.Y = rollangle;//actual_orientation[1];//;
			orientationFiltered.Z = 0;//quat.qz;//tmp_pitch;//yawangle;//actual_orientation[2];//accelFiltered[2];
			orientationFiltered.MilliTimeStamp = millisecondsRV;
			orientationFiltered.UTMDateTime = utmTime;
			//orientationFiltered.dateTime.setToNow();
			orientationFiltered.FrameNumber = FrameNo;

			if(StoreSlidingWindows) {
				All_Orientation_SlidingWindow = UpdateSlidingWindows(orientationFiltered, All_Orientation_SlidingWindow);
			}



			if(Saving)
			{

				All_Orientation.add(orientationFiltered);
			}


			if(Saving)
			{
				FrameNo++;
			}
			Ready = false;

		}
		if(accelData != null)
		{
			PreviousAccelData = accelData.Clone();
		}
		if(gyroData != null)
		{
			PreviousGyroData = gyroData.Clone();
		}

		if(All_Accel.size() > MAX_NUMBER_ACCEL_FRAMES && !SaveLimitAnnounced)
		{
			SaveLimitAnnounced = true;
			AnnounceData(frameLimitIntent,FrameLimitBroadcast,"true");
		}


	}



	double wrapAngle(double a)
	{
		double angle = a;
		double pi = Math.PI;
		if( angle > pi)
		{
			angle -= (2*pi);
		}
		if (angle < -pi)
		{
			angle += (2*pi);
		}
		if (angle < 0)
		{
			angle += 2*pi;
		}
		return angle;
	}



	private ArrayList<Vector3D> UpdateSlidingWindows(Vector3D data,ArrayList<Vector3D> window)
	{
		if(window == null)
		{
			window = new ArrayList<Vector3D>();
		}

		window.add(data);

		if(window.size() > SLIDING_WINDOW_SIZE)
		{
			window.remove(0);
		}
		return window;
	}

	private Vector3D Noramilze(Vector3D v)
	{
		double m = Math.sqrt(Math.pow(v.X, 2)+Math.pow(v.Y, 2)+Math.pow(v.Z, 2));
		Vector3D newV = v.Clone();
		newV.X = v.X / m;
		newV.Y = v.Y / m;
		newV.Z = v.Z / m;
		return newV;
	}

	private Vector3D ApplyLowPassFilter(Vector3D current,Vector3D previous)
	{
		if(previous == null)
		{
			previous = current.Clone();
		}
		double diff = current.MilliTimeStamp - previous.MilliTimeStamp;
		double intervaltime = 1/  (1000 / (double)diff);
		double alphaA = intervaltime / (intervaltime + ACCEL_TIMECONSTANT);
		Vector3D filtered = new Vector3D();

		filtered = current.Clone();
		filtered.X = previous.X + alphaA * (current.X - previous.X);
		filtered.Y = previous.Y + alphaA * (current.Y - previous.Y);
		filtered.Z = previous.Z + alphaA * (current.Z - previous.Z);

		return filtered;
	}

	double PredictAccG_roll(double a_z, double a_y, double a_x,double previous)
	{

		double minDiff = 0.02;
		if(Math.abs(a_y - a_z) < minDiff && (Math.abs(a_y - 0) < minDiff || Math.abs(a_y - 1) < minDiff || Math.abs(a_z - 0) < minDiff || Math.abs(a_z - 1) < minDiff))
		{

			return 0;

			//return previous;
		}
		else if(Math.abs(a_y - 0) < minDiff && a_z < 0)
		{
			//Log.v("Roll","2");
			return 0;
		}
		else
		{
			double l_z = Math.sqrt(a_x * a_x + a_z * a_z);
			return Math.atan2(a_y, l_z /* Math.signum(a_z)*/);
		}

	}

	double Predict_gyroRoll(Vector3D currentAccel,Vector3D currentGyro)
	{
		double a_x = currentAccel.X;
		double a_y = currentAccel.Y;
		double a_z = currentAccel.Z;
		//Ayz = Math.atan2(Est_y_prev, Est_z_prev)
		double minDiff = 0.02;
		if(Math.abs(a_y - a_z) < minDiff && (Math.abs(a_y - 0) < minDiff || Math.abs(a_y - 1) < minDiff || Math.abs(a_z - 0) < minDiff || Math.abs(a_z - 1) < minDiff))
		{
			//"Roll Less than MinDiff"
			return 0;


		}
		else if(Math.abs(a_z - 0) < minDiff)//pitch is 90 degrees
		{
			return currentGyro.Z;
		}
		else if(Math.abs(a_y - 0) < minDiff && a_z < 0)
		{
			return 0;
		}
		else
		{
			return currentGyro.X;
		}

	}

	double Predict_gyroPitch(Vector3D currentAccel,Vector3D currentGyro)
	{
		double a_x = currentAccel.X;
		double a_y = currentAccel.Y;
		double a_z = currentAccel.Z;

		double minDiff = 0.1;

		if(Math.abs(a_y - 1) < minDiff)
		{
			return 0;
		}
		else if(a_z < 0 && Math.abs(a_x - 0) < minDiff)
		{

			return 0;

		}
		else if(Math.abs(a_x - 0) < minDiff && Math.abs(a_z - 0) < minDiff)
		{
			//Log.v("Pitch", "|X - 0| & |Y-0| < Min");
			return 0;
		}
		else if(Math.abs(a_z - 0) < minDiff && Math.abs(a_y - 1) < minDiff)
		{
			return 0;
		}
		else
		{
			return currentGyro.Y;
		}

	}

	double PredictAccG_pitch(double a_z, double a_y, double a_x,double previous)
	{
		double minDiff = 0.1;
		boolean flip = false;
		if(a_z < 0)
		{
			flip = true;
		}

		{

			double l_z = Math.sqrt(a_y * a_y + a_z * a_z) * Math.signum(a_z);
			double pitchangle = Math.atan2(a_x, a_z);

			return pitchangle;
		}

	}




}
