package com.csri.ami.health_u.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

/**
 * Created by daniel on 19/11/2015.
 */
public class BatteryLevelReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i("BatteryLevelReciever", intent.getAction());
        if(intent.getAction().equals(Intent.ACTION_BATTERY_LOW))
        {
            TurnOffSensors(context);
        }
        else if(intent.getAction().equals(Intent.ACTION_BATTERY_OKAY))
        {
            Log.i("BatteryLevelReciever", "About to turn on sensors");
            TurnOnSensors(context);
        }
        else if(intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
            Log.i("BatteryLevelReciever", "Power Connected!!!!");

        }
        else if(intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
            Log.i("BatteryLevelReciever", "Power Connected!!!!");

        }

    }

    public void TurnOnSensors(Context context)
    {
        boolean isSensorRunning = SensorFragment.isMyServiceRunning(context);
        Log.i("BatteryLevelReciever", "is sensor running= " + Boolean.toString(isSensorRunning) );

        Intent backgroundServiceIntent = new Intent(context, BackgroundService.class);

        if(!isSensorRunning)//if not running...check should it be running and turn it on////if is is running, then leave it running, it was probably turned on at boot, user should choose to turn it off again if they want
        {
            SharedPreferences prefs2 = context.getSharedPreferences(InformedConsent.PREFS_INFORMED_CONSENT, Context.MODE_PRIVATE);
            boolean accepted = prefs2.getBoolean(InformedConsent.PREFS_INFORMED_CONSENT_ACCEPTED, false);

            SharedPreferences prefs = context.getSharedPreferences(BackgroundService.PREFS_NAME, Context.MODE_PRIVATE);
            boolean turnOnSensors = prefs.getBoolean(BackgroundService.SENSOR_ON_PREF,true);

            Log.i("BatteryLevelReciever", "turn on sensors= " + Boolean.toString(turnOnSensors) );
            if(turnOnSensors && accepted)
            {
                Log.i("BatteryLevelReciever", "about to call startService");
                context.startService(backgroundServiceIntent);

            }
        }
    }

    public void TurnOffSensors(Context context)
    {

        Intent backgroundServiceIntent = new Intent(context, BackgroundService.class);
        boolean isSensorRunning = SensorFragment.isMyServiceRunning(context);

        if(isSensorRunning)
        {
            context.stopService(backgroundServiceIntent);



        }
    }

    public static double batteryLevel(Context context)
    {
        Intent intent  = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int    level   = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int    scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        double percent = ((double)level)/(double)scale;
        return percent;

    }
}
