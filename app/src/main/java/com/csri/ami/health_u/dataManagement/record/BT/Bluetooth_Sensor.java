package com.csri.ami.health_u.dataManagement.record.BT;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;

import com.csri.ami.health_u.dataManagement.record.GPS_Sensor;
import com.csri.ami.health_u.dataManagement.record.SensorRecorder;
import com.csri.ami.health_u.dataManagement.record.wifi.WirelessMetaData;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;

public class Bluetooth_Sensor
{
	private static final int REQUEST_ENABLE_BT = 2;
	
	private BluetoothAdapter mBtAdapter;
	private ArrayList<BluetoothDevice> mPairedDevicesArrayAdapter;
    private ArrayList<BluetoothDevice> mNewDevicesArrayAdapter;
    private ArrayList<BluetoothDevice> newDevicesFullSet;
    private ArrayList<ArrayList<BluetoothDevice>> ALLDevicesFullSet;
    private ArrayList<ArrayList<BluetoothDevice>> ALLDevicesFullSet_UsedForSaving;
    private ArrayList<WirelessMetaData> timeStamps;
    private ArrayList<WirelessMetaData> timeStamps_UsedForSaving;
    
    private static String NODEVICES = "NO DEVICES";
    public int RecheckDelay = SensorRecorder.BTDeviceRecheckDelay_Fast;
    int MoveingCheckFrequency = 1;//i.e check once every 1 recheckDelays (every 1 * 30000ms = 30 seconds)
    int NotMovingCheckFrequency = 6;//i.e check once every 6 recheckDelays (every 6 * 30000 ms = 3 mins)
    int CheckCount = 0;
    long MillisecondMoveThreshold = 2000;
    
    public boolean Moving = true;
    
    private long lastTimeRecieve = 0;
    
    boolean SpeedUpdatePerformed = false;
    private int nextUpdateSpeed = RecheckDelay;
    
    Timer scheduler;
	private Handler handler = new Handler();
	
	SensorRecorder Parent;
	Context context;
    
	public Bluetooth_Sensor (Context t,SensorRecorder parent)
	{
		context =t;
		Parent = parent;
		mPairedDevicesArrayAdapter = new ArrayList<BluetoothDevice>();
        mNewDevicesArrayAdapter = new ArrayList<BluetoothDevice>();
        ALLDevicesFullSet = new ArrayList<ArrayList<BluetoothDevice>>();
        timeStamps = new ArrayList<WirelessMetaData>();
        
        
		
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		
		lastTimeRecieve = GPS_Sensor.GetUTMTime();
		
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
	
	public void Stop()
	{
		mBtAdapter.cancelDiscovery();
		mBtAdapter = null;
		context.unregisterReceiver(mReceiver);
		handler.removeCallbacks(updateDataTask);
	}
	
	public void Start()
	{
		
		
		
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(mReceiver, filter);
        
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(mReceiver, filter);
        
		DoPairedDiscovery();
		DoDiscovery();
		
		handler.removeCallbacks(updateDataTask);
        handler.postDelayed(updateDataTask, 100);
	}
	
	public void ClearBuffers()
	{
		synchronized(this)
		{
			if(ALLDevicesFullSet != null)
				ALLDevicesFullSet.clear();
			if(timeStamps != null)
				timeStamps.clear();
		}
	}
	
	public void PrepareForSaving()
	{
		if(ALLDevicesFullSet != null)
		{
			ALLDevicesFullSet_UsedForSaving = (ArrayList<ArrayList<BluetoothDevice>>)ALLDevicesFullSet.clone();
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
	
	public void Write(OutputStreamWriter osw) throws IOException
	{
		//synchronized(this)
		//{
			//String accelString = "";
			for(int i=0;i<ALLDevicesFullSet_UsedForSaving.size();i++)
			{
				ArrayList<BluetoothDevice> devices = ALLDevicesFullSet_UsedForSaving.get(i);
				//long milliseconds = timeStamps_UsedForSaving.get(i);
				WirelessMetaData currentMeta = timeStamps_UsedForSaving.get(i);
				//osw.write(ALL_GPSreadings_UsedForSaving.get(i).toString());
				osw.write(DeviceArrayToString(devices,currentMeta));
			}
			//return accelString;
		//}
	}
	
	private String DeviceArrayToString(ArrayList<BluetoothDevice> devices,WirelessMetaData meta)
	{
		String timeStampString = Long.toString(meta.TimeStamp) + "," + meta.DateTime.format("%T") + ",";
		String EmptyTimeStampString = " " + "," + " " + ",";
		String deviceString = "";//Long.toString(meta.EventTimeStamp) + "," + meta.DateTime.format("%T") + ",";
		for(int i=0;i<devices.size();i++)
		{
//			if(i==0)
//			{
//				deviceString += timeStampString;
//			}
//			else
//			{
//				deviceString += EmptyTimeStampString;
//			}
			BluetoothDevice device = devices.get(i);
			BluetoothClass devClass = device.getBluetoothClass();
			
			deviceString += 0/*UserId*/ + "," + Long.toString(meta.TimeStamp) + "," + 0/*TZ*/ + "," + GetDeviceClassDescriptor(devClass) + "," + device.getAddress() + "," + device.getName() + "\n";
			//deviceString += DeviceToString(devices.get(i));// devices.get(i).getName() + ",";
		}
		//deviceString += "\n";
		return deviceString;
	}
	
	private String DeviceToString(BluetoothDevice device)
	{
		BluetoothClass devClass = device.getBluetoothClass();
		return  device.getName() + "," + device.getAddress() + "," + GetDeviceClassDescriptor(devClass) + "," + devClass.getDeviceClass() + "," + device.getBondState() + "\n";
	}
	
	private String GetDeviceClassDescriptor(BluetoothClass devClass)
	{
		String descriptor = "";
		if(devClass.getMajorDeviceClass() == BluetoothClass.Device.Major.AUDIO_VIDEO)
		{
			descriptor = "AUDIO_VIDEO";
		}
		else if(devClass.getMajorDeviceClass() == BluetoothClass.Device.Major.COMPUTER)
		{
			descriptor = "COMPUTER";
		}
		else if(devClass.getMajorDeviceClass() == BluetoothClass.Device.Major.HEALTH)
		{
			descriptor = "HEALTH";
		}
		else if(devClass.getMajorDeviceClass() == BluetoothClass.Device.Major.IMAGING)
		{
			descriptor = "IMAGING";
		}
		else if(devClass.getMajorDeviceClass() == BluetoothClass.Device.Major.MISC)
		{
			descriptor = "MISC";
		}
		else if(devClass.getMajorDeviceClass() == BluetoothClass.Device.Major.NETWORKING)
		{
			descriptor = "NETWORKING";
		}
		else if(devClass.getMajorDeviceClass() == BluetoothClass.Device.Major.PERIPHERAL)
		{
			descriptor = "PERIPHERAL";
		}
		else if(devClass.getMajorDeviceClass() == BluetoothClass.Device.Major.PHONE)
		{
			descriptor = "PHONE";
		}
		else if(devClass.getMajorDeviceClass() == BluetoothClass.Device.Major.TOY)
		{
			descriptor = "TOY";
		}
		else if(devClass.getMajorDeviceClass() == BluetoothClass.Device.Major.UNCATEGORIZED)
		{
			descriptor = "UNCATEGORIZED";
		}
		else if(devClass.getMajorDeviceClass() == BluetoothClass.Device.Major.WEARABLE)
		{
			descriptor = "WEARABLE";
		}
		else
		{
			descriptor = "Other";
		}
		return descriptor;
	}
	
	private Runnable updateDataTask = new Runnable() {
    	public void run() 
    	{
    		UpdateData();
    		if(SpeedUpdatePerformed)
    		{
    			RecheckDelay = nextUpdateSpeed;
    			Log.i("BT Update Speed:",Double.toString(RecheckDelay));
    			SpeedUpdatePerformed = false;
    			handler.postDelayed(this, RecheckDelay);
    		}
    		else
    		{
    			handler.postDelayed(this, RecheckDelay);
    		}
    	}
    	};
    	
    public void UpdateData()
    {
    	if (!mBtAdapter.isDiscovering())
    	{
//    		if(!mBtAdapter.isEnabled())
//    		{
//    			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
//    		}
    			
    		DoDiscovery();
    	}
    }
    
    public void UpdateCheckSpeed(int delay)
	{	
    	//handler.removeCallbacks(updateDataTask);
		//handler.postDelayed(updateDataTask, RecheckDelay - 1000);
		//Log.i("BT Update Speed:",Double.toString(RecheckDelay));
		
		if(delay <= RecheckDelay)//if the new delay is going to happen sooner than we expect the old delay to occur then implement new delay asap
		{
			RecheckDelay = delay;
			handler.removeCallbacks(updateDataTask);
			handler.postDelayed(updateDataTask, RecheckDelay - 1000);
			Log.i("BT Update Speed:",Double.toString(RecheckDelay));
		}
		else//if new delay is going to happen longer away than current delay, then we would like current delay to do one more iteration before new delay is implemened
		{
			SpeedUpdatePerformed = true;
			nextUpdateSpeed = delay;
		}

	}
    
    public ArrayList<BluetoothDevice> GetDetectedDevices()
    {
    	//if(newDevicesFullSet != null)
    	//{
    		return newDevicesFullSet;
    	///}
    	//else
    	//{
    	//	return null;
    	//}
    }
	
	public void DoPairedDiscovery()
	{
		
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
		 if (pairedDevices.size() > 0)
		 {
	            for (BluetoothDevice device : pairedDevices) 
	            {
	                mPairedDevicesArrayAdapter.add(device);
	            }
	     } 
		 else 
		 {
	            //String noDevices = NODEVICES;
	            //mPairedDevicesArrayAdapter.add(noDevices);
	     }
	}
	
	public void DoDiscovery()
	{
//		CheckCount++;
//		long millisecondsSinceLastMovement = Parent.gps.TimeSinceLastLocationChange();
//		if(millisecondsSinceLastMovement > MillisecondMoveThreshold)
//		{
//			Moving = false;
//		}
//		else
//		{
//			Moving = true;
//		}
//		int frequency = MoveingCheckFrequency;
//		if(!Moving)
//		{
//			frequency = NotMovingCheckFrequency;
//		}
//		
//		if(CheckCount % frequency == 0)
		{
			mNewDevicesArrayAdapter.clear();
			// If we're already discovering, stop it
			if (mBtAdapter.isDiscovering()) {
				mBtAdapter.cancelDiscovery();
			}

			// Request discover from BluetoothAdapter
			mBtAdapter.startDiscovery();
		}
	}
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            
        	synchronized (this) {
        	String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) 
            {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                	
                	
                    mNewDevicesArrayAdapter.add(device);
                }
            // When discovery is finished, change the Activity title
            } 
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) 
            {
            	
            	if(newDevicesFullSet != null)
            	{
            		newDevicesFullSet.clear();
            	}
            	newDevicesFullSet = (ArrayList<BluetoothDevice>)mNewDevicesArrayAdapter.clone();
            	//Time t = new Time();
            	//t.setToNow();
            	//long milliseconds = SystemClock.elapsedRealtime();
            	WirelessMetaData currentMeta = new WirelessMetaData();
            	currentMeta.TimeStamp = GPS_Sensor.GetUTMTime();
            	currentMeta.DateTime = new Time();
            	currentMeta.DateTime.setToNow();
            	
            	//Log.d("BlueTooth",Integer.toString(newDevicesFullSet.size()) + " bluetooth devices found!");
            	
            	timeStamps.add(currentMeta);
            	ALLDevicesFullSet.add((ArrayList<BluetoothDevice>)newDevicesFullSet.clone());
                if (mNewDevicesArrayAdapter.size() == 0) 
                {
                    //String noDevices = getResources().getText(R.string.none_found).toString();
                   // mNewDevicesArrayAdapter.add(NODEVICES);
                }
                
                if(timeStamps != null && timeStamps.size() > 0)
	           	{
	           		lastTimeRecieve = timeStamps.get(timeStamps.size()-1).TimeStamp;
	           	}
            }
        	}
        }

		
    };
}
