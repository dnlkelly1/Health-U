package com.csri.ami.health_u.dataManagement.analyze.classifiers;

public class FFT_Features {
	
	public double Real;
	public double Im;
	public double Mag;
    public double Index;
    public double Frequency;

    public FFT_Features()
    {
    }
    
    public static FFT_Features[] Average(FFT_Features[][] v,FFT_Features[] var)
    {
    	FFT_Features[] av = new FFT_Features[v[0].length];
    	
    	for(int i=0;i<av.length;i++)
    	{
    		av[i] = new FFT_Features();
    	}
    	
    	for(int i=0;i<v.length;i++)
    	{
    		
    		for(int j=0;j<v[i].length;j++)
    		{
    			av[j].Index = v[i][j].Index;
    			av[j].Mag += v[i][j].Mag / (double)v.length;
    			av[j].Frequency += v[i][j].Frequency / (double)v.length;
    		}
    	}
    	
    	//FFT_Features[] var = new FFT_Features[v[0].length];
    	for(int i=0;i<var.length;i++)
    	{
    		var[i] = new FFT_Features();
    	}
    	for(int i=0;i<v.length;i++)
    	{
    		
    		for(int j=0;j<v[i].length;j++)
    		{
    			var[j].Index = v[i][j].Index;
    			var[j].Mag += Math.abs(av[j].Mag - v[i][j].Mag) / (double)v.length;
    			var[j].Frequency += Math.abs(av[j].Frequency - v[i][j].Frequency) / (double)v.length;
    		}
    	}
    	
    	
    	
    	return av;
    }
}
