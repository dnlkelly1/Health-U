//package com.sensors.record;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStreamWriter;
//import java.util.ArrayList;
//
//import com.sensor.analyze.UI.Diary;
//import com.sensor.analyze.UI.OnAlarmReceiver;
//import com.sensor.analyze.UI.SensorMainUI;
//import com.sensors.analyze.Complex;
//import com.sensors.analyze.FFT_Features;
//import com.sensors.analyze.MotionFeatures;
//import com.sensors.analyze.SoundClassify;
//
//import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
//
//import android.app.AlarmManager;
//import android.app.PendingIntent;
//import android.bluetooth.BluetoothDevice;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.media.AudioFormat;
//import android.media.AudioRecord;
//import android.media.AudioRecord.OnRecordPositionUpdateListener;
package com.csri.ami.health_u.dataManagement.record.sound;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.media.MediaRecorder.AudioSource;
import android.os.Handler;
import android.util.Log;

import com.csri.ami.health_u.dataManagement.analyze.classifiers.Complex;
import com.csri.ami.health_u.dataManagement.analyze.classifiers.FFT_Features;
import com.csri.ami.health_u.dataManagement.analyze.classifiers.MotionFeatures;
import com.csri.ami.health_u.dataManagement.analyze.classifiers.SoundClassify;
import com.csri.ami.health_u.dataManagement.record.GPS_Sensor;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * SoundSensor is a manager class that handles recording of raw sound data from device microphone
 * Recordings are performed periodically and features are then extracted
 * Features are stored periodically
 *
 * @author Daniel Kelly
 * @version 1.0
 * @since 2014-06-1
 */
public class Sound_Sensor
{
	private Thread recordingThread;

	private int bufferSize =800;
	public static double DURATION_SECS = 3;
	private int bufferSamples = 100;

	private ArrayList<short[]> buffersAll;
	private ArrayList<Integer> buffersSizes;


	private boolean recorderStarted = false;
	private AudioRecord recorder;

	public double AverageSound = 0;
	public double Freq = 0;


	int SampleRate = -1;
	private Handler handler = new Handler();

	public static int RecheckDelay = 1000 * 60 * 3;//Defines how long in millseconds between attempted sound recordings
	private int Short_RecheckDelay = 1000 * 1;
	private int windowSize = 1024;
	private int windowSize_Cleaning = 1024;
	private double windowSize_PercentageOfSecond = 0.04;
	private double windowSize_PercentageOfSecond_Cleaning = 0.02;

	boolean DoRecheck = false;

	public boolean DeviceNotMoving = true;
	public boolean NotCurrentlySavingToDisk = true;

	private SoundVector sounds;
	private SoundVector sounds_UsedForSaving;
	private SoundVector sounds_UsedForClassification;

	private long lastStartTime = 0;

	MFCC featureMFCC;
	private static int NumFrequncyBins = 8;
	private static int MFCCS_VALUE = 12;
    private static int MEL_BANDS = 20;


    private static FFT_Features[] previousFFT;

    private SoundClassify soundclassify;

    private double[] EqualizationScalers;
    private double[] EqualizationFreqs;
	//Define Equalization settings for sound filtering
    private double[] eqBasis = new double[]{1.0,1.3,1.1,1.05,1.02,1,0.95,0.94,0.85,0.80,0.70,0.6,0.5,0.45,0.4,0.3,0.2,0.15,0.12,0.1,0.2};


	int SamplePeriod = (int)((double)SampleRate * DURATION_SECS);
	double[] freqBands=null;
	private double SampleCount =0;
	private double DurationCount =0;
	double[][] eqs;
	DoubleFFT_1D fft2;

	Context appContext;

	public Sound_Sensor(Context context)
	{
		appContext = context;
	}

	public void ClearClassificationResults()
	{
		if(soundclassify != null)
		{
			soundclassify.ClearClassificationResults();
		}
	}

	public boolean IsRunning()
	{
		return recorderStarted;
	}

	public int RecordDuration()
	{
		return (int)((double)1000 * DURATION_SECS);
	}

	/**
	 * Inilialze recording.
	 * This method resets data from previous recording and prepares them for new recording
	 */
	public void start()
	{

		double[][] eqs = GetEqulizationArray(256,22050,eqBasis);
		EqualizationScalers = eqs[0];
		EqualizationFreqs = eqs[1];


		SampleCount =0;
		DurationCount=0;
		sounds = new SoundVector();
		sounds_UsedForSaving = new SoundVector();
		sounds_UsedForClassification = new SoundVector();
		DoRecheck = true;
		handler.removeCallbacks(updateDataTask);
		handler.postDelayed(updateDataTask, 0);
	}

	/**
	 * Stop recording and reset sound buffers
	 */
	public void stop()
	{
		handler.removeCallbacks(updateDataTask);
		DoRecheck = false;
		stopListenToMicrophone();
		//recorder.stop();
		recorder.release();
		recorder = null;

		if(buffersAll != null)
		{
			buffersAll.clear();
			buffersAll = null;
		}
		if(buffersSizes != null)
		{
			buffersSizes.clear();
			buffersSizes = null;
		}
		if(sounds != null)
		{
			sounds.Blank();
			sounds = null;
		}

	}


	/**
	 * Background method to keep checking if we are allowed to start sound recording
	 * We only want to begin recording when the phone is stationary to get best possible signal
	 */
	private Runnable updateDataTask = new Runnable() {
    	public void run()
    	{
    		if(DeviceNotMoving && NotCurrentlySavingToDisk && !Currently_AnalysingSounds && !recorderStarted)
    		{
    			Log.i("SoundRecorder", "BEGIN RECORDING!");
    			startListenToMicrophone();
    		}
    		else
    		{
    			Log.i("SoundRecorder", "DELAY RECORDING!!! DeviceNotMoving =" + DeviceNotMoving + " NotCurrentlySavingToDisk =" + NotCurrentlySavingToDisk);
    			handler.removeCallbacks(updateDataTask);
    			handler.postDelayed(updateDataTask, Short_RecheckDelay);
    		}
    	}
    };

    AudioFormat audioFormat;

	protected void startListenToMicrophone() {
	    if (!recorderStarted) {

	    	lastStartTime = GPS_Sensor.GetUTMTime();

	    	recordingThread = new Thread() {

	            @Override
	            public void run() {
	            	try
	            	{

	            		if(recorder == null)
	            		{
	            			recorder = findAudioRecord();



	            			windowSize = nearest_pow((int)(windowSize_PercentageOfSecond * SampleRate));
	            			windowSize_Cleaning = nearest_pow((int)(windowSize_PercentageOfSecond_Cleaning * SampleRate));
	            			SamplePeriod = SampleRate * (int)DURATION_SECS;
	            			fft2 = new DoubleFFT_1D(windowSize);


	            		}
	            		else if(recorder.getState() == AudioRecord.STATE_UNINITIALIZED)
	            		{
	            			recorder.release();
	            			recorder = null;
	            			recorder = findAudioRecord();
	            		}

	            		if(freqBands == null)
	            		{
	            			freqBands = MFCC.GetMelBins(NumFrequncyBins,0,(int)MFCC.maxMelFreq);
	            		}
	            		if(featureMFCC == null)
	            		{
	            			featureMFCC = new MFCC(windowSize ,MFCCS_VALUE,MEL_BANDS,SampleRate);
	            		}


	                    if(recorder.getState() == AudioRecord.STATE_INITIALIZED)
	                    {

							//Get Ready to do actual sound recording
	    	                recorder.setPositionNotificationPeriod(SamplePeriod);
	    	                recorder.setRecordPositionUpdateListener(positionListener);


	    	                recorder.startRecording();

	    	                int read =0;
	    	                int readCount =0;

	    	                while (true)//loop end condition is handled by isInterrupted() method call
	    	                {
	    	                    if (isInterrupted())//check is loop complete
	    	                    {
	    	                        recorder.stop();
	    	                        recorderStarted = false;

	    	                        break;
	    	                    }
	    	                    else
	    	                    {
									//read raw sound from microphone
	    	                    	short[] buffer = new short[(int)((double)SampleRate)];
	    	                    	read = recorder.read(buffer, 0, buffer.length);
	    	                    	if(AudioRecord.ERROR_INVALID_OPERATION != read && read > 1)
	    	                    	{
										//add raw sound to sound buffer
	    	                    		readCount+=read;
	    	                    		buffersAll.add(buffer);
	    	                    		buffersSizes.add(read);
	    	                    	}
	    	                    	Log.v("Recording","Logged" + read + " frames");
	    	                    }

	    	                }
	                    }
	            	}
	            	catch(Exception e)
	            	{

	            	}
	            }
	        };
	        buffersSizes = new ArrayList<Integer>();
	        buffersAll = new ArrayList<short[]>();
	        recordingThread.start();

	        recorderStarted = true;
	    }
	}

	private int nearest_pow (int num)
	{
	    int n = num > 0 ? num - 1 : 0;

	    n |= n >> 1;
	    n |= n >> 2;
	    n |= n >> 4;
	    n |= n >> 8;
	    n |= n >> 16;
	    n++;

	    return n;
	}

	private static int[] mSampleRates = new int[] {44100,22050,11025,8000};
	public AudioRecord findAudioRecord()
	{
		AudioRecord r=null;

	    for (int rate : mSampleRates)
	    {
	        for (short audioFormat : new short[] {  AudioFormat.ENCODING_PCM_16BIT,AudioFormat.ENCODING_PCM_8BIT })
	        {
	            for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO })
	            {
	                try
	                {

	                    int _bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

	                    if (_bufferSize != AudioRecord.ERROR_BAD_VALUE) {
	                        // check if we can instantiate and have a success
	                        AudioRecord recorder = new AudioRecord(AudioSource.DEFAULT, rate, channelConfig, audioFormat, _bufferSize * 10);

	                        if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
	                        {

	                        	if(r == null)
	                        	{
	                        		bufferSize = _bufferSize;
		                        	SampleRate = rate;

	                        		r = recorder;
	                        	}
	                        }
	                    }
	                }
	                catch (Exception e)
	                {
	                    //Log.e(C.TAG, rate + "Exception, keep trying.",e);
	                }
	            }
	        }
	    }
	    return r;
	}


	public double Decibels(double amp)
	{

		if(recorder != null)
		{
			if(recorder.getAudioFormat() == AudioFormat.ENCODING_PCM_8BIT)
			{
				double max = 128;
				double amp_s = amp / max;
				return 20 * Math.log10(amp_s);
			}
			else
			{
				double max = 32768;
				double amp_s = amp / max;
				return 20 * Math.log10(amp_s);
			}
		}
		else
		{
			return 0;
		}

	}



	/**
	 * Take list of arrays and flatten to single array
	 * @param data
	 * @param sizes
	 * @return
	 */
	private short[] Flatten(ArrayList<short[]> data,ArrayList<Integer> sizes)
	{
		int startCount=sizes.size();
		int totalSamples =0;
        for(int i=0;i<data.size();i++)
        {
        	if(i < sizes.size())
        	{
        		totalSamples += sizes.get(i);
        	}
        }

        short[] flat_data_s = new short[totalSamples];
        int index=0;
        for(int i=0;i<data.size();i++)
        {
        	if(i < data.size())
        	{
        		short[] current = data.get(i);
        		if(i < sizes.size())
        		{
	        		for(int j=0;j<sizes.get(i);j++)
	        		{

	        			if(j < current.length)
	        			{
	        				//flat_data.add(current[j]);
	        				flat_data_s[index] = current[j];
	        				index++;
	        			}
	        		}
        		}
        	}
        }


		return flat_data_s;
	}


	private short[][] Windows(short[] data,int windowSize,double overlap)
	{

		ArrayList<short[]> subArrays = new ArrayList<short[]>();

		int count=0;
        while(count < data.length - (windowSize))
        {
        	short[] current_subarray = new short[windowSize];
        	int currentCount=0;
        	for(int j=0;j<windowSize && count < data.length;j++)
        	{
        		current_subarray[j] = data[count];
        		count++;
        		currentCount++;
        	}
        	if(currentCount == windowSize)
        	{
        		count -= (windowSize * overlap);
        	}

        	subArrays.add(current_subarray);
        }

        short[][] subArrays_array = new short[subArrays.size()][windowSize];
        for(int i=1;i<subArrays.size();i++)
        {
        	subArrays_array[i] = subArrays.get(i);
        }

        subArrays.clear();
        subArrays = null;

		return subArrays_array;

	}



    //convert short to byte
	private byte[] short2byte(short[] sData) {
	    int shortArrsize = sData.length;
	    byte[] bytes = new byte[shortArrsize * 2];
	    for (int i = 0; i < shortArrsize; i++) {
	        bytes[i * 2] = (byte) (sData[i] & 0x00FF);
	        bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
	        sData[i] = 0;
	    }
	    return bytes;

	}




	private double[] HammingWindow(int N)
	{
		double[] windowFilter = new double[N];
		for(int i=0;i<windowFilter.length;i++)
		{
			windowFilter[i] = 0.5 * (1 - (Math.cos(  (2 * Math.PI * i)  /  (N-1) )));//hanning window
		}

		return windowFilter;
	}



	public short[] CleanAudio(ArrayList<short[]> dataAll,ArrayList<Integer> sizesAll)
	{

		short[] bufferFlat = Flatten(dataAll,sizesAll);

		double[] bufferFinal = new double[bufferFlat.length];

		double[] windowFilter = HammingWindow(windowSize_Cleaning);


		double overlap = 0.5;

		for(int i=0;i<bufferFlat.length;i+=(windowSize_Cleaning * overlap))
        {
			if( i + windowSize_Cleaning < bufferFlat.length)
			{
	             double[] current = new double[windowSize_Cleaning];
	             for(int j=0;j<current.length;j++)
	             {
	            	 current[j] = ((double)bufferFlat[i + j] * windowFilter[j]);
	             }
	             double[] newBuffer = MotionFeatures.FrequencyEq(current, SampleRate,EqualizationFreqs,EqualizationScalers);

            	 int pos = i;

            	 int windowPos =0;
            	 for(int j=pos;j< i + current.length;j++)
            	 {
            		 bufferFinal[j] += (newBuffer[windowPos]);
            		 windowPos++;
            	 }

			}

        }

		short[] bufferFinal_short = new short[bufferFinal.length];
		for(int i=0;i<bufferFinal.length;i++)
		{
			bufferFinal_short[i] = (short)Math.round(bufferFinal[i]);
		}

		return bufferFinal_short;
	}

	boolean Currently_AnalysingSounds = false;
	private  double MostRecentSoundAvg =0;
	private double TotalRecentSoundAvg =0;
	private int todayCount = 0;
	private DateTime lastTime=null;


	public double Scale(double val)
	{
		double max = GetMaxDecibles();
		double min = GetMinDecibles();
		double dist = max - min;

		double offset = val - min;
		double percent = offset/dist;
		return percent * 100;
	}

	public double Get_MostRecentSoundAvg()
	{
		double avg =  MostRecentSoundAvg;
		if(avg > 0) {
			double dec = Decibels(avg);
			//return avg;
			return Scale(dec);
		}
		else
		{
			return 0;
		}

	}

	private ArrayList<Double> todaysAvgSounds = new ArrayList<Double>();
	boolean new_todaysAvgSounds = false;

	public double Get_TotalSoundAvg()
	{
		if(todaysAvgSounds!= null && todaysAvgSounds.size() > 0 && new_todaysAvgSounds)
		{

			TotalRecentSoundAvg = GetAVGDeciblesToday();
			return Scale(TotalRecentSoundAvg);
		}
		else
		{
			TotalRecentSoundAvg = GetAVGDeciblesToday();
			return  Scale(TotalRecentSoundAvg);
		}

	}

	/**
	 * Class that enables background processing of sound data
	 */
	public class AnalysisProcess implements Runnable {

		public void start()
		{

			Thread t = new Thread(this);
			Log.v("Analysing","Starting Thread!");
			t.start();

		}

		public void run()
		{
			stopListenToMicrophone();
			while(recorderStarted)
			{
				Log.v("Analysing","Waiting for Record to Fully Complete");
				try{Thread.sleep(10);}catch(Exception e){}
			}



			Currently_AnalysingSounds = true;
			Log.v("Analysing","Starting Analysis");

			long windowDuration = (long)(((double)windowSize / (double)SampleRate) * 1000);

			double overlap = 0.5;


			short[] buffer_cleaned = CleanAudio(buffersAll,buffersSizes);


			long dataDuration = (long)(((double)buffer_cleaned.length / (double)SampleRate) * 1000);

            short[][] buffer =  Windows(buffer_cleaned,windowSize,overlap);

			double max_rms = Double.MIN_VALUE;
			double min_rms = Double.MAX_VALUE;
            double currentSampleAvgSound =0;
			double rms=0;
			double rms_var=0;
			double rmscount=0;



			ArrayList<Double> rmsVals = new ArrayList<Double>();

            for(int i=1;i<buffer.length-2;i++)//skip first and last frame i.e start at pos 1 and finish at second last position
            {

				double rmsCurrent =0;
				for (int j = 0; j < buffer[i].length; j++)
				{
					rmsCurrent += Math.pow(buffer[i][j], 2);
				}

				rmsCurrent = rmsCurrent / (double)buffer[i].length;
				rmsCurrent = Math.sqrt(rmsCurrent);
				if(rmsCurrent < min_rms && rmsCurrent > 0){min_rms = rmsCurrent;}
				if(rmsCurrent > max_rms){max_rms = rmsCurrent;}

				rmsVals.add(rmsCurrent);
				rms += rmsCurrent;
				rmscount++;


				double sum = 0;
            	for (int j = 0; j < buffer[i].length; j++)
	            {
	                sum += Math.abs(buffer[i][j]);
	            }
	            AverageSound = (double) (sum / (double)buffer[i].length);//avgSound was calculated wrong untill 7-9-15..dont use in data analysis on .net app

	            if(AverageSound > 1)//50)
	            {

	            	double[] currentBufferWindow = new double[buffer[i].length];
	            	for(int j=0;j<currentBufferWindow.length;j++)
	            	{
	            		currentBufferWindow[j] = (double)buffer[i][j];// * windowFilter[i];
	            	}

	            	Complex[] c = MotionFeatures.FFT_Raw(currentBufferWindow,SampleRate,fft2);


		            currentBufferWindow = null;
		            FFT_Features[] ffts = MotionFeatures.GetFFTFeatures(c, false,SampleRate);
		            FFT_Features[] fft_bands = MotionFeatures.FFT_Bands(c,freqBands,SampleRate);

		            double[] ceps = featureMFCC.cepstrum(c);

		            Freq = fft_bands[0].Frequency ;

		            long time = lastStartTime + (i * (long)((double)windowDuration * overlap));
		            SpectralFeatures f;

		            if(previousFFT == null)
		            	f = new SpectralFeatures(ffts,ffts,buffer[i],ceps);
		            else
		            	f = new SpectralFeatures(ffts,previousFFT,buffer[i],ceps);

		            sounds.Add(time, windowDuration, AverageSound, fft_bands,f);
		            sounds_UsedForClassification.Add(time, windowDuration, AverageSound, fft_bands,f);

		            previousFFT = ffts;

		            buffer[i] = null;



	            }
            }

			rms /= rmscount;

			for(int i=0;i<rmsVals.size();i++)
			{
				rms_var += Math.pow(rmsVals.get(i) - rms,2);
			}
			double rms_proper_variance = rms / rmscount;
			rms_var = Math.sqrt( rms_var / rmscount);


			currentSampleAvgSound = rms;

			Log.i("Sound Var",Double.toString(rms_var));
			if(rms_var > 10)//if var is greater than 150...then there is def some sound happening...
			// updateaveragerms keeps a record of the current avg rms and the min rms which occured with a high var (>200)
			{
				UpdateAverageRMS(max_rms,rms);
			}
			UpdateMaxAndMinDecibels(rms,rms);

			todaysAvgSounds.add(rms);
			new_todaysAvgSounds = true;

			lastTime = GetLastSoundAvgTime();


			if(Days.daysBetween(lastTime.withTimeAtStartOfDay(),DateTime.now().withTimeAtStartOfDay()).getDays() != 0)
			{
				todayCount=0;
				TotalRecentSoundAvg=0;
				lastTime = DateTime.now();
				todaysAvgSounds.clear();

				double d_rms = Decibels(rms);
				ResetSoundAvgDetails((float)d_rms, DateTime.now());


			}
			else
			{
				double d_rms = Decibels(rms);
				UpdateSoundAvgDetails((float)d_rms, DateTime.now());
			}

			MostRecentSoundAvg = currentSampleAvgSound;
			TotalRecentSoundAvg += MostRecentSoundAvg;
			todayCount++;


            if(buffersAll != null) {
				buffersAll.clear();
				buffersAll = null;
			}

			if(buffersSizes != null) {
				buffersSizes.clear();
				buffersSizes = null;
			}


            DurationCount+=dataDuration;
            SampleCount++;


            Currently_AnalysingSounds = false;
            Log.v("Analysing","Ending Analysis");

        	if(DoRecheck)
            {
         	   handler.removeCallbacks(updateDataTask);
         	   handler.postDelayed(updateDataTask, RecheckDelay);
            }

		}
	}
	public static String SOUND_STORED_PREFS = "soundvalprefs";
	public static String SOUND_STORED_MAX = "maxsoundval";
	public static String SOUND_STORED_MIN = "minsoundval";
	public static String SOUND_STORED_MAXRMS = "maxrmssoundval";

	public static String SOUND_STORED_COUNT = "countsound";
	public static String SOUND_STORED_TOTAL = "totalsound";
	public static String SOUND_STORED_TIME = "timesound";

	private float GetAVGDeciblesToday()
	{
		SharedPreferences prefs = appContext.getSharedPreferences(SOUND_STORED_PREFS, Context.MODE_PRIVATE);

		float currentTotal = prefs.getFloat(SOUND_STORED_TOTAL, 0);
		int currentCount = prefs.getInt(SOUND_STORED_COUNT,0);

		if(currentCount > 0)
		{
			return currentTotal / (float) currentCount;
		}
		else
		{
			return  0;
		}
	}

	private DateTime GetLastSoundAvgTime()
	{
		SharedPreferences prefs = appContext.getSharedPreferences(SOUND_STORED_PREFS, Context.MODE_PRIVATE);
		String t =prefs.getString(SOUND_STORED_TIME ,DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));

		DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

		DateTime time = formatter.parseDateTime(t);

		return time;
	}

	private void UpdateSoundAvgDetails(float total,DateTime time)
	{
		SharedPreferences prefs = appContext.getSharedPreferences(SOUND_STORED_PREFS, Context.MODE_PRIVATE);

		float currentTotal = prefs.getFloat(SOUND_STORED_TOTAL, 0);
		int currentCount = prefs.getInt(SOUND_STORED_COUNT, 0);

		currentTotal += total;
		currentCount++;

		SharedPreferences.Editor editor = prefs.edit();

		editor.putFloat(SOUND_STORED_TOTAL, currentTotal);
		editor.putInt(SOUND_STORED_COUNT, currentCount);
		editor.putString(SOUND_STORED_TIME, time.toString("yyyy-MM-dd HH:mm:ss"));

		editor.commit();
	}

	private void ResetSoundAvgDetails(float total,DateTime time)
	{
		SharedPreferences prefs = appContext.getSharedPreferences(SOUND_STORED_PREFS, Context.MODE_PRIVATE);


		SharedPreferences.Editor editor = prefs.edit();

		editor.putFloat(SOUND_STORED_TOTAL, total);
		editor.putInt(SOUND_STORED_COUNT, 1);
		editor.putString(SOUND_STORED_TIME,time.toString("yyyy-MM-dd HH:mm:ss"));
		editor.commit();
	}

	private double minSoundDec = Double.NaN;
	public double GetMinDecibles()
	{
		if(Double.isNaN(minSoundDec)) {


			SharedPreferences prefs = appContext.getSharedPreferences(SOUND_STORED_PREFS, Context.MODE_PRIVATE);
			double currentMax = prefs.getFloat(SOUND_STORED_MAX, default_max);
			double currentMin = prefs.getFloat(SOUND_STORED_MIN, (float)currentMax - 1);
			minSoundDec = currentMin;
			return currentMin;
		}
		else {
			return minSoundDec;
		}
	}

	private double maxSoundDec = Double.NaN;
	private double maxSoundRMS = Double.NaN;
	public double GetMaxDecibles()
	{
		if(Double.isNaN(maxSoundDec)) {
			SharedPreferences prefs = appContext.getSharedPreferences(SOUND_STORED_PREFS, Context.MODE_PRIVATE);
			double currentMax = prefs.getFloat(SOUND_STORED_MAX, default_max);
			maxSoundDec = currentMax;
			return currentMax;
		}
		else
		{
			return maxSoundDec;
		}
	}

//	public double GetMaxRMS()
//	{
//		if(Double.isNaN(maxSoundRMS)) {
//			SharedPreferences prefs = appContext.getSharedPreferences(SOUND_STORED_PREFS, Context.MODE_PRIVATE);
//			double currentMax = prefs.getFloat(SOUND_STORED_MAXRMS, default_max_rms);
//			maxSoundRMS = currentMax;
//			return currentMax;
//		}
//		else
//		{
//			return maxSoundRMS;
//		}
//	}

	public static String SOUND_TOTAL_RMS = "soundtotalrms";
	public static String SOUND_COUNT_RMS = "soundcountrms";
	public static String SOUND_MIN_RMS = "soundminrms";



	private void UpdateAverageRMS(double max_value,double avg_value)
	{
		SharedPreferences prefs = appContext.getSharedPreferences(SOUND_STORED_PREFS, Context.MODE_PRIVATE);
		double currentTotal = prefs.getFloat(SOUND_TOTAL_RMS, 0);
		double currentCurrentCount = prefs.getFloat(SOUND_COUNT_RMS, 0);
		double currentMinRMS = prefs.getFloat(SOUND_MIN_RMS,1200);

		double newVal = currentTotal+ avg_value;
		double newCount = currentCurrentCount+1;

		SharedPreferences.Editor editor = prefs.edit();
		editor.putFloat(SOUND_TOTAL_RMS, (float) newVal);
		editor.commit();
		editor.putFloat(SOUND_COUNT_RMS, (float) newCount);
		editor.commit();


		Log.i("Current Min Sound Value", Double.toString(currentMinRMS));
		Log.i("Avg Sound Value", Double.toString(newVal/newCount));
		if(avg_value < currentMinRMS)
		{
			Log.i("Min Sound Value", Double.toString(avg_value));
			editor.putFloat(SOUND_MIN_RMS,(float)avg_value);
			editor.commit();
		}


	}

	private float default_max_rms = 100;
	private float default_max =  -40;
	private float default_min = -45;
	private void UpdateMaxAndMinDecibels(double min_value,double max_value)
	{


		SharedPreferences prefs = appContext.getSharedPreferences(SOUND_STORED_PREFS, Context.MODE_PRIVATE);
		double currentMaxRMS = prefs.getFloat(SOUND_STORED_MAXRMS, default_max_rms);
		if(max_value > currentMaxRMS)
		{
			SharedPreferences.Editor editor = prefs.edit();
			editor.putFloat(SOUND_STORED_MAXRMS, (float)max_value );
			editor.commit();
			maxSoundRMS = max_value ;
		}

		double currentDec_min = Decibels(min_value);
		double currentDec_max =  Decibels(max_value);


		double currentMax = prefs.getFloat(SOUND_STORED_MAX, Float.NaN);


		if(Double.isNaN(currentMax) || currentDec_max >= currentMax)
		{
			SharedPreferences.Editor editor = prefs.edit();
			editor.putFloat(SOUND_STORED_MAX, (float)currentDec_max);
			editor.commit();
			maxSoundDec = currentDec_max;
			currentMax = currentDec_max;
		}

		if(min_value > 0)
		{
			double currentMin = prefs.getFloat(SOUND_STORED_MIN, (float)currentMax-1);
			if (currentDec_min < currentMin) {
				SharedPreferences.Editor editor = prefs.edit();
				editor.putFloat(SOUND_STORED_MIN, (float) currentDec_min);
				editor.commit();
				minSoundDec = currentDec_min;
			}
		}

	}
	
	public double GetSoundSampleCount()
	{
		if(soundclassify != null)
		{
			return SampleCount;
		}
		else
		{
			return 0;
		}
	}

	public double GetSoundDurationeCount()
	{
		if(soundclassify != null)
		{
			return DurationCount;
		}
		else
		{
			return 0;
		}
	}

	public double[] GetSoundClassificationResults()
	{
		if(soundclassify != null)
		{
			return soundclassify.ClassificationResults;
		}
		else
		{
			return null;
		}
	}
	
	private void stopListenToMicrophone() 
	{
	    if (recorderStarted) {
	        if (recordingThread != null && recordingThread.isAlive() && !recordingThread.isInterrupted()) {
	            recordingThread.interrupt();
	        }
	       
	    }
	}
	
	OnRecordPositionUpdateListener positionListener = new OnRecordPositionUpdateListener(){
        @Override
        public void onPeriodicNotification(AudioRecord recorder) {
        	
        	//stopListenToMicrophone();
        	AnalysisProcess a = new AnalysisProcess();
        	a.start();
        	
        }

        @Override
        public void onMarkerReached(AudioRecord recorder) {
        }
    };
    
    boolean IsCurrentlySaving=false;
    public void PrepareForSaving()
	{
    	while(/*recorderStarted || */Currently_AnalysingSounds)
    	{
    		try{Thread.sleep(100);Log.v("SoundPause","Pausing Prep" );}
    		catch(Exception e){}
    		
    	}
    		
		if(sounds != null)
		{
			IsCurrentlySaving=true;
			sounds_UsedForSaving = sounds.Clone();
			sounds.Clear();
			sounds = null;
			sounds = new SoundVector(); 
		    
		}
		
	}

	
	public void ClearSavingBuffers()
	{
		//synchronized(this)
		{
			if(sounds_UsedForSaving != null)
			{
				sounds_UsedForSaving.Clear();
				sounds_UsedForSaving = null;
				sounds_UsedForSaving = new SoundVector();
			}
			
		}
	}
	
	public void ClearBuffers()
	{
		synchronized(this)
		{
			if(sounds != null)
				sounds.Clear();
			
		}
	}
	
	public void Write(OutputStreamWriter osw,long timeOffset) throws IOException
	{
		while(Currently_AnalysingSounds)
    	{
    		try{Thread.sleep(100);Log.v("SoundPause","Pausing Write" );}
    		catch(Exception e){}
    		
    	}
		Log.v("SoundRecord","Actual Write Begins" );	
		//synchronized(this)
		//{
		if(sounds_UsedForSaving != null)
		{
			//String accelString = "";
			Log.v("SoundRecord","Size=" + sounds_UsedForSaving.size() );
			int writtenCount =0;
			for(int i=0;i<sounds_UsedForSaving.size();i++)
			{
				
				//osw.write(ALL_GPSreadings_UsedForSaving.get(i).toString());
				Log.v("SoundRecord","Write:" +i);
				osw.write(sounds_UsedForSaving.ItemToString(i));
				writtenCount++;
			}
			Log.v("SoundRecord","Written=" + writtenCount );
			//return accelString;
		}
		Log.v("SoundRecord","Actual Write Ends" );	
		IsCurrentlySaving=false;
	}
	
	private static double CosineInterpolate(double y1, double y2,double mu)
    {
        double mu2;

        mu2 = (1 - Math.cos(mu * Math.PI)) / 2;
        return (y1 * (1 - mu2) + y2 * mu2);
    }

	
	private static double[][] GetEqulizationArray(int len,double maxFreq,double[] equalization)
	{
		int bands = equalization.length;
		
		double max = MFCC.fhz2mel(maxFreq);
		
		
		double inc = max / (double)(bands-1);
		
		double[] melFreqs = new double[ bands];
		double[] melFreqs_inv = new double[ bands];
		
		double currentFreq = 0;
		for(int i=0;i<bands;i++)
		{
			melFreqs[i] = currentFreq;// MFCC.fmel2hz(currentFreq);
			melFreqs_inv[i] = MFCC.fmel2hz(currentFreq);
			currentFreq += inc;
		}
		double testInv = MFCC.fmel2hz(melFreqs[melFreqs.length-1]);
		
		double increaseRate = (double)len / (double)bands;
		
		double[] newBands = new double[bands * (int)increaseRate];
		double[] newEq = new double[bands * (int)increaseRate];
		
		int newElementsPerRegion = (int)increaseRate-1;
		int count =0;
		for(int i=0;i<melFreqs.length-1;i++)
		{
			
			for(int j=0;j<newElementsPerRegion;j++)
			{
				double perc = (double)j / (double)newElementsPerRegion;
				newBands[count]= CosineInterpolate(melFreqs[i],melFreqs[i+1],perc);
				newEq[count]= CosineInterpolate(equalization[i],equalization[i+1],perc);
				count++;
			}
		}
		
		for(int i=0;i<newBands.length;i++)
		{
			newBands[i] = MFCC.fmel2hz(newBands[i]);
		}
		
		return new double[][]{ newEq,newBands};
	}
}


