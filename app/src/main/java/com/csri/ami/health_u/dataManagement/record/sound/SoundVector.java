package com.csri.ami.health_u.dataManagement.record.sound;

import com.csri.ami.health_u.dataManagement.analyze.classifiers.FFT_Features;

import java.util.ArrayList;


/**
 * Data Structure for storing and processing sound feature vectors
 */
public class SoundVector 
{
	private ArrayList<Long> StartTimes;
	private ArrayList<Long> Durations;
	private ArrayList<Double> AverageSounds;
	private ArrayList<FFT_Features[]> SoundsFFT;
	private ArrayList<SpectralFeatures> SoundsFeatures;
	
	private static boolean USE_VARIANCE = false;
	
	public SoundVector()
	{
		StartTimes = new ArrayList<Long>();
		Durations = new ArrayList<Long>();
		AverageSounds = new ArrayList<Double>();
		SoundsFFT = new  ArrayList<FFT_Features[]>();
		SoundsFeatures = new ArrayList<SpectralFeatures>();
		
	}
	
	
	
	public static ArrayList<SoundVector> WindowAverage(SoundVector data,double windowLenSeconds,ArrayList<SoundVector> variance)
	{
		int index = 0;

        ArrayList<SoundVector> windowFeatures = new ArrayList<SoundVector>();

        while (index < data.size())
        {
            SoundVector currentWindow = new SoundVector();
            double currentLength = 0;
            double start = data.StartTimes.get(index);

            while (currentLength < windowLenSeconds && index < data.size()-1)
            {
                currentWindow.Add(data.StartTimes.get(index),data.Durations.get(index),data.AverageSounds.get(index),data.SoundsFFT.get(index),data.SoundsFeatures.get(index));

                index++;
                currentLength = data.StartTimes.get(index) - start;
            }
            if (currentWindow.size() > 0 && currentLength > windowLenSeconds * 0.9)
            {
                SoundVector var = new SoundVector();
                SoundVector current = SoundVector.Average(currentWindow,var);
                windowFeatures.add(current);
                variance.add(var);
                
                index -= (currentWindow.size() / 2);
            }
            else
            {
                index++;
            }

           // index++;
        }
        
        return windowFeatures;
	}
	
	public static SoundVector Average(SoundVector v,SoundVector variance)
	{
		SoundVector av = new SoundVector();
		Long time = v.StartTimes.get(0);
		Long duration = (long)0;// v.Durations.get(0);
		
		for(int i=0;i<v.Durations.size();i++)
		{
			duration += v.Durations.get(i); 
		}
		
		double avSounds =0;
		double varSounds = 0;
		
		FFT_Features[][] av_FFT_window = new FFT_Features[v.size()][] ;
		
		
		SpectralFeatures[] av_specFeatures_window = new SpectralFeatures[v.size()];
		for(int i=0;i<v.size();i++)
		{
			
				avSounds += v.AverageSounds.get(i) / (double)v.size();
			
				av_FFT_window[i] = v.SoundsFFT.get(i);
			
				av_specFeatures_window[i] = v.SoundsFeatures.get(i);
			
		}
		
		FFT_Features[] var_FFT= new FFT_Features[av_FFT_window[0].length];
		FFT_Features[] av_FFT = FFT_Features.Average(av_FFT_window,var_FFT);
		SpectralFeatures var_specFeatures= new SpectralFeatures();
		SpectralFeatures av_specFeatures = SpectralFeatures.Average(av_specFeatures_window,var_specFeatures);
		
		SoundVector av_vector = new SoundVector();
		av_vector.Add(time, duration, avSounds, av_FFT, av_specFeatures);
		//SoundVector var_vector = new SoundVector();
		variance.Add(time, duration, varSounds, var_FFT, var_specFeatures);
		
		//variance = var_vector;
		
		return av_vector;
	}
	
	public void Add(long start,long dur,double avg,FFT_Features[] fft,SpectralFeatures features)
	{
		StartTimes.add(start);
		Durations.add(dur);
		AverageSounds.add(avg);
		SoundsFFT.add(fft);
		SoundsFeatures.add(features);
		
	}

	public static double DecibelsEstimate(double amp)
	{
		double max = 32767;
		double amp_scale = amp / max;
		return 20 * Math.log10(amp_scale);
	}
	
	public double[] FeaturesAll(int index)//RMS is [6]
	{
		ArrayList<Double> data = new ArrayList<Double>();
		
		SpectralFeatures f = SoundsFeatures.get(index);
		double[] spectralFeatures = f.FeatureVector();
		
		for(int i=0;i<spectralFeatures.length;i++)
		{
			data.add(spectralFeatures[i]);
		}
		
		FFT_Features[] fft = SoundsFFT.get(index);

		for(int i=0;i<fft.length;i++)
		{
			data.add(fft[i].Mag);
		}
		
		double[] cepstral = f.Cepstral;
		for(int i=0;i<cepstral.length;i++)
		{
			data.add(cepstral[i]);
		}
		
		int[] BadColumnIndices = new int[]{};
		if (BadColumnIndices != null && BadColumnIndices.length > 0)
        {
            ArrayList<Double> withRemoveColumns = new ArrayList<Double>();

            for (int i = 0; i < data.size(); i++)
            {
                if (!Contains(BadColumnIndices,i))
                {
                    withRemoveColumns.add(data.get(i));
                }
            }
           data = withRemoveColumns;
        }
		
		double[] arrayData = new double[data.size()];
		
		for(int i=0;i<arrayData.length;i++)
		{
			arrayData[i] = data.get(i);
		}
		
		return arrayData;
		
	}
	
	private boolean Contains(int[] data,int id)
	{
		boolean found = false;
		for(int i=0;i<data.length;i++)
		{
			if(data[i] == id)
			{
				found = true;
				break;
			}
		}
		
		return found;
	}
	
	public void RemoveSoundsBelowVolume(double threshold)
	{
		ArrayList<Integer> indicesToKeep = new ArrayList<Integer>();
		for(int i=0;i<AverageSounds.size();i++)
		{
			if(AverageSounds.get(i) > threshold)
			{
				indicesToKeep.add(i);
			}
		}
		
		ArrayList<Long> newStartTimes = new ArrayList<Long>();
		ArrayList<Long> newDurations = new ArrayList<Long>();
		ArrayList<Double> newAverageSounds = new ArrayList<Double>();
		ArrayList<FFT_Features[]> newSoundsFFT = new  ArrayList<FFT_Features[]>();
		ArrayList<SpectralFeatures> newSoundsFeatures = new ArrayList<SpectralFeatures>();
		
		for(int i=0;i<indicesToKeep.size();i++)
		{
			int currentIndex = indicesToKeep.get(i);
			newStartTimes.add(StartTimes.get(currentIndex));
			newDurations.add(Durations.get(currentIndex));
			newAverageSounds.add(AverageSounds.get(currentIndex));
			newSoundsFFT.add(SoundsFFT.get(currentIndex));
			newSoundsFeatures.add(SoundsFeatures.get(currentIndex));
		}
		
		StartTimes = newStartTimes;
		Durations = newDurations;
		AverageSounds = newAverageSounds;
		SoundsFFT = newSoundsFFT;
		SoundsFeatures = newSoundsFeatures;
		
	}
	
	public void Blank()
	{
		if(SoundsFFT != null && AverageSounds != null && Durations != null && StartTimes != null && SoundsFeatures != null)
		{
			StartTimes.clear();
			Durations.clear();
			AverageSounds.clear();
			SoundsFFT.clear();
			SoundsFeatures.clear();
			
			StartTimes = null;
			Durations = null;
			AverageSounds = null;
			SoundsFFT = null;
			SoundsFeatures = null;
			
		
		}
	}
	
	public void Clear()
	{
		if(SoundsFFT != null && AverageSounds != null && Durations != null && StartTimes != null && SoundsFeatures != null)
		{
			StartTimes.clear();
			Durations.clear();
			AverageSounds.clear();
			SoundsFFT.clear();
			SoundsFeatures.clear();
			
			StartTimes = null;
			Durations = null;
			AverageSounds = null;
			SoundsFFT = null;
			SoundsFeatures = null;
			
			StartTimes = new ArrayList<Long>();
			Durations = new ArrayList<Long>();
			AverageSounds = new ArrayList<Double>();
			SoundsFFT = new  ArrayList<FFT_Features[]>();
			SoundsFeatures = new ArrayList<SpectralFeatures>();
		}
	}
	
	public int size()
	{
		return StartTimes.size();
	}
	
	public void UpdateTime(int i,long offset)
	{
		if(StartTimes != null)
		{
			long current = StartTimes.get(i);
			StartTimes.set(i, current + offset);
		}
	}
	
	public String ItemToString(int item)
	{
		String line ="";
		if(SoundsFFT != null && AverageSounds != null && Durations != null && StartTimes != null && SoundsFeatures != null)
		{
			if(SoundsFFT.size() > item && AverageSounds.size() > item && Durations.size() > item && StartTimes.size() > item && SoundsFeatures.size() > item)
			{
				FFT_Features[] fft = SoundsFFT.get(item);
				double sound = AverageSounds.get(item);
				//long milliseconds = timeStamps_UsedForSaving.get(i);
				long start = StartTimes.get(item);
				long dur = Durations.get(item);
				
				line = start + "," + dur + "," + sound + ",";
				
				for(int i=0;i<fft.length;i++)
				{
					line += /*fft[i].Frequency + "," +*/ fft[i].Mag + ",";
				}
				//12
				SpectralFeatures f = SoundsFeatures.get(item);
				line += f.toString();
				
				line += "\n";
			}
			
		}
		return line;
	}
	
	public static double WindowLength =500;
	public static ArrayList<SoundFeatureSummary> ConvertSoundFeaturesToFeatureSummary(SoundVector sound)
	{
		
		ArrayList<SoundFeatureSummary> summaries = new ArrayList<SoundFeatureSummary>();
		
		SoundVector[] windows = GetRecordingWindows(sound);
		
		for (int i = 0; i < windows.length; i++)
        {
			ArrayList<SoundVector> var=new ArrayList<SoundVector>();
			ArrayList<SoundVector> currentWindow = WindowAverage(sound,WindowLength,var);
			
			for (int j = 0; j < currentWindow.size(); j++)
            {
				SoundFeatureSummary f = new SoundFeatureSummary(0,currentWindow.get(j).AverageSounds.get(0), currentWindow.get(j).StartTimes.get(0), currentWindow.get(j).FeaturesAll(0),var.get(j).FeaturesAll(0),USE_VARIANCE);
                summaries.add(f);
                
            }
        }
		return summaries;
	}
	
	public static SoundVector[] GetRecordingWindows(SoundVector soundAll)
	{
		ArrayList<SoundVector> windows = new ArrayList<SoundVector>();

        double diff = 0;
        long threshold = 2000;

        SoundVector currentWindow = new SoundVector();

        for (int i = 0; i < soundAll.size(); i++)
        {
            //TimeSpan t = soundAll[i + 1].Timestamp - soundAll[i].Timestamp;
        	if(i > 0)
        	{
	            diff = (long)soundAll.StartTimes.get(i) - (long)soundAll.StartTimes.get(i-1);
	
	            
	            if (diff > threshold)
	            {
	                windows.add(currentWindow);
	                currentWindow = new SoundVector();
	                currentWindow.Add(soundAll.StartTimes.get(i),soundAll.Durations.get(i),soundAll.AverageSounds.get(i), soundAll.SoundsFFT.get(i),soundAll.SoundsFeatures.get(i));
	            }
	            else
	            {
	            	currentWindow.Add(soundAll.StartTimes.get(i),soundAll.Durations.get(i),soundAll.AverageSounds.get(i), soundAll.SoundsFFT.get(i),soundAll.SoundsFeatures.get(i));
	            }
        	}
        	else
        	{
        		 currentWindow.Add(soundAll.StartTimes.get(i),soundAll.Durations.get(i),soundAll.AverageSounds.get(i), soundAll.SoundsFFT.get(i),soundAll.SoundsFeatures.get(i));
        	}
        }
        if(currentWindow.size() > 0)
        {
        	windows.add(currentWindow);
        }
        
        SoundVector[] windows_array = new SoundVector[windows.size()];
        for(int i=0;i<windows_array.length;i++)
        {
        	windows_array[i] = windows.get(i);
        }
		return windows_array;
	}
	
	public SoundVector Clone()
	{
		SoundVector c = new SoundVector();
		c.AverageSounds = (ArrayList<Double>)AverageSounds.clone();
		c.StartTimes = (ArrayList<Long>)StartTimes.clone();
		c.Durations = (ArrayList<Long>)Durations.clone();
		c.SoundsFFT = (ArrayList<FFT_Features[]>)SoundsFFT.clone();
		c.SoundsFeatures= (ArrayList<SpectralFeatures>)SoundsFeatures.clone();
		
		return c;
	}
}
