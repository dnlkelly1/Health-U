package com.csri.ami.health_u.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

public class StartupIntentReciever extends BroadcastReceiver{

	
	
	@Override
	public void onReceive(Context context, Intent arg1) {
	
		Log.i("StartupIntentReciever", "Waiting to Start Background Service...");
		try
		{
			String state = Environment.getExternalStorageState();

			while(!Environment.MEDIA_MOUNTED.equals(state))
			{
				Log.i("StartupIntentReciever", "Media Not Mounted...Waiting to Start Background Service...");
				Thread.sleep(2000);
				state = Environment.getExternalStorageState();
			}
		}
		catch(InterruptedException e)
		{
			Log.i("StartupIntentReciever", "Error Waiting!");
		}
		

		if(true)//!shutdownCorrectly)
	    {
//			BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//			if(!bluetoothAdapter.isEnabled())
//			{
//				bluetoothAdapter.enable();
//			}
	    	Log.i("StartupIntentReciever", "Starting Background Service...");
	    	Intent serviceIntent = new Intent();
	    	serviceIntent = new Intent(context, BackgroundService.class);
	    	context.startService(serviceIntent);
	    }
	    
		
		
	}
	
	
	

}
