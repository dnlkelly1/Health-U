package com.csri.ami.health_u.dataManagement.record.sound;

import com.csri.ami.health_u.dataManagement.analyze.classifiers.Normalize;

import org.joda.time.DateTime;

import java.util.ArrayList;

/**
 * Data Structure to store and summarize sound features
 */
public class SoundFeatureSummary 
{
	public DateTime Timestamp;
	public long time;
	public double[] featureVector;
	public double AverageSound;

	public  double AverageRMS;
	
	public SoundFeatureSummary ()
	{
		
	}

	public SoundFeatureSummary(DateTime time,double[] data)
	{
		Timestamp = time;
		featureVector = data;
	}
	
	public SoundFeatureSummary (int userIndex,double avgSound,long t,double[] f1,double[] f2,boolean useF2)
	{
		
        time = t;
        AverageSound = avgSound;
		AverageRMS = f1[6]; //f1[6] == RMS from Spectral Features
        if(useF2)
        {
	        ArrayList<Double> f_all = new ArrayList<Double>();
	        for (int i = 0; i < f1.length; i++)
	        {
	            f_all.add(f1[i]);
	        }
	        for (int i = 0; i < f2.length; i++)
	        {
	            f_all.add(f2[i]);
	        }
	        
	        featureVector = new double[f_all.size()];
	        for(int i=0;i<featureVector.length;i++)
	        {
	        	featureVector[i] = f_all.get(i);
	        }
        }
        else
        {
        	featureVector = f1;
        }
        
        
	}

	public SoundFeatureSummary(int userindex, int clusterId, DateTime t, double[] f1, double[] f2, boolean useF2, double avgSound)
	{
		AverageSound = avgSound;

		Timestamp = t;

		AverageRMS = f1[6];

		if (useF2)
		{
			ArrayList<Double> f_all = new ArrayList<Double>();
			for (int i = 0; i < f1.length; i++)
			{
				f_all.add(f1[i]);
			}
			for (int i = 0; i < f2.length; i++)
			{
				f_all.add(f2[i]);
			}

			featureVector = new double[f_all.size()];
			for(int i=0;i<featureVector.length;i++)
			{
				featureVector[i] = f_all.get(i);
			}

		}

		featureVector = f1;
	}

	public static SoundFeatureSummary Average(ArrayList<SoundFeatureSummary> data)
	{
		SoundFeatureSummary result = new SoundFeatureSummary();

		double[] avgFeature = new double[data.get(0).featureVector.length];
		double size = (double)data.size();
		for(int i=0;i<data.size();i++)
		{
			for(int j=0;j<data.get(i).featureVector.length;j++)
			{
				avgFeature[j] += data.get(i).featureVector[j] / size;
			}
		}

		result.Timestamp = data.get(data.size()-1).Timestamp;
		result.featureVector = avgFeature;

		return result;
	}
	
	public static SoundFeatureSummary PerformNormalize(SoundFeatureSummary all,Normalize n)
    {
        
        SoundFeatureSummary current = all;
        current.featureVector = n.NormalizeWithSavedVecotrs(current.featureVector);
        return current;
        
    }
	
	public static void PerformNormalize(ArrayList<SoundFeatureSummary> all,Normalize n)
    {
        for (int i = 0; i < all.size(); i++)
        {
        	SoundFeatureSummary current = all.get(i);
        	current.featureVector = n.NormalizeWithSavedVecotrs(current.featureVector);
        }
    }
}
