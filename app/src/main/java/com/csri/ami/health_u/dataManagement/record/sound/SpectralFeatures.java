package com.csri.ami.health_u.dataManagement.record.sound;

import android.media.AudioFormat;

import com.csri.ami.health_u.dataManagement.analyze.classifiers.FFT_Features;

import java.util.ArrayList;

public class SpectralFeatures 
{
	public double Rolloff;
	public double Entropy;
	public double RelativeEntropy;
	public double Centroid;
	public double Flux;
	public double ZCR;
	public double RMS;
	public double LEF;
	public double LowEnergyFR;
	public double BandWidth;
	public double[] Cepstral;
	
	private double mag_sum = 0;
	private double mag_sum_1 = 0;
	
	public SpectralFeatures()
    {
    }
	
	public SpectralFeatures(FFT_Features[] fft,FFT_Features[] fft_prev,short[] raw,double[] cepstral)
	{
		for(int i=0;i<fft.length-2;i++)
		{
			mag_sum += fft[i].Mag;
			mag_sum_1 += fft_prev[i].Mag;
		}
		
		
		Rolloff = SpectralRolloff(fft,0.93);
		Entropy = SpectralEntropy(fft);
		RelativeEntropy = SpectralRelativeEntropy(fft,fft_prev);
		Flux = SpectralFlux(fft,fft_prev);
		Centroid = SpectralCentroid(fft);
		BandWidth = SpectralBandWidth(fft,Centroid);
		ZCR = ZeroCrossingRate(raw);
		RMS = RootMeanSquare(raw);
		LEF = LowEnergyFrameRate(raw,RMS);
		Cepstral = cepstral;
	}
	
	public double[] FeatureVector()//RMS is [6]
	{
		ArrayList<Double> data = new ArrayList<Double>();
		
		data.add(Rolloff);data.add(Entropy);data.add(RelativeEntropy);data.add(Centroid);data.add(Flux);data.add(ZCR);data.add(RMS);data.add(LEF);data.add(BandWidth);
		
		
		
		double[] data_array = new double[data.size()];
		
		for(int i=0;i<data_array.length;i++)
		{
			data_array[i] = data.get(i);
		}
		
		return data_array;
	}
	
	
	public static SpectralFeatures Average(SpectralFeatures[] f,SpectralFeatures var)
    {
        SpectralFeatures av = new SpectralFeatures();
        
		var = new SpectralFeatures();
        av.Cepstral = new double[f[0].Cepstral.length];
        var.Cepstral = new double[f[0].Cepstral.length];
        for (int i = 0; i < f.length; i++)
        {
            for (int j = 0; j < av.Cepstral.length; j++)
            {
                av.Cepstral[j] += f[i].Cepstral[j] / (double)f.length;
            }
            av.BandWidth += f[i].BandWidth / (double)f.length;
            av.Centroid += f[i].Centroid / (double)f.length;
            av.Entropy += f[i].Entropy / (double)f.length;
            av.Flux += f[i].Flux / (double)f.length;
            av.LEF += f[i].LEF / (double)f.length;
            av.LowEnergyFR += f[i].LowEnergyFR / (double)f.length;
            av.RelativeEntropy += f[i].RelativeEntropy / (double)f.length;
            av.RMS += f[i].RMS / (double)f.length;
            av.Rolloff += f[i].Rolloff / (double)f.length;
            av.ZCR += f[i].ZCR / (double)f.length;
        }

        for (int i = 0; i < f.length; i++)
        {
            for (int j = 0; j < av.Cepstral.length; j++)
            {
                var.Cepstral[j] += Math.abs(f[i].Cepstral[j] - av.Cepstral[j]) / (double)f.length;
            }
            var.BandWidth += Math.abs(f[i].BandWidth - av.BandWidth) / (double)f.length;
            var.Centroid += Math.abs(f[i].Centroid - av.Centroid) / (double)f.length;
            var.Entropy += Math.abs(f[i].Entropy - av.Entropy) / (double)f.length;
            var.Flux += Math.abs(f[i].Flux - av.Flux) / (double)f.length;
            var.LEF += Math.abs(f[i].LEF - av.LEF) / (double)f.length;
            var.LowEnergyFR += Math.abs(f[i].LowEnergyFR - av.LowEnergyFR) / (double)f.length;
            var.RelativeEntropy += Math.abs(f[i].RelativeEntropy - av.RelativeEntropy) / (double)f.length;
            var.RMS += Math.abs(f[i].RMS - av.RMS) / (double)f.length;
            var.Rolloff += Math.abs(f[i].Rolloff - av.Rolloff) / (double)f.length;
            var.ZCR += Math.abs(f[i].ZCR - av.ZCR) / (double)f.length;
        }

        
        return av;
    }
	
	public String toString()
	{//12
		String line = Rolloff + "," + Centroid + "," + Entropy + "," + RelativeEntropy + "," + Flux + "," + BandWidth + "," + ZCR + "," + LEF + "," + RMS + ",";
		if(Cepstral != null)
		{
			for(int i=0;i<Cepstral.length;i++)
			{
				line +=Cepstral[i] + ",";
			}
		}
		return line;
	}

	public SpectralFeatures(double[] fileData)
	{
		Rolloff = fileData[0];
		Centroid = fileData[1];
		Entropy = fileData[2];
		RelativeEntropy = fileData[3];
		Flux = fileData[4];
		BandWidth = fileData[5];
		ZCR = fileData[6];
		LEF = fileData[7];
		RMS = fileData[8];

		int offset = 9;
		int cepLen = fileData.length - offset;
		Cepstral = new double[cepLen];
		for (int i = 0; i < cepLen; i++)
		{
			Cepstral[i] = fileData[offset + i];
		}

	}
	
	
	public static double LowEnergyFrameRate(short[] data,double rms)
	{
		int count = 0;
		for(int i=0;i<data.length;i++)
		{
			if(data[i] < rms)
			{
				count++;
			}
		}
		
		return (double)count / (double)data.length;
		
	}
	
	public static double RootMeanSquare(short[] data)
	{
		double rms = 0;
		
		for(int i=0;i<data.length;i++)
		{
			rms += Math.pow(data[i],2);
		}
		rms = rms / (double)data.length;
		
		return Math.sqrt(rms);
	}
	
	public double ZeroCrossingRate(short[] data)
	{
		double zcr = 0;
		for(int i=1;i<data.length;i++)
		{
			zcr += Math.abs(Math.signum(data[i]) - Math.signum(data[i - 1]));
		}
		
		return zcr /2;
	}
	
	public double SpectralBandWidth(FFT_Features[] fft,double SC)
	{
		//double mag_sum = 0;
		double sum_a = 0;
		double sum_b =0;
		
		
		
		for(int i=0;i<fft.length-2;i++)
		{
			double p_i = fft[i].Mag / mag_sum;
			
			sum_a += Math.abs(fft[i].Frequency - SC) * p_i;
			sum_b += p_i;
		}
		
		return sum_a / sum_b;
	}
	
	public double SpectralCentroid(FFT_Features[] fft)
	{
		//double mag_sum = 0;
		double sum_x = 0;
		double sum_f_x =0;
		
		
		
		for(int i=0;i<fft.length-2;i++)
		{
			double w = fft[i].Mag / mag_sum;
			sum_f_x += fft[i].Frequency * w;
			sum_x += w;
		}
		
		return sum_f_x / sum_x;
	}
	
	public double SpectralEntropy(FFT_Features[] fft)
	{
		//double mag_sum = 0;
		double entropy = 0;
		
		
		
		for(int i=0;i<fft.length-2;i++)
		{
			double p_i = fft[i].Mag / mag_sum;
			entropy += p_i * Math.log(2) * p_i;
		}
		
		return -entropy;
	}
	
	public double SpectralRelativeEntropy(FFT_Features[] fft,FFT_Features[] fft_1)
	{
		//double mag_sum = 0;
		//double mag_sum_1 = 0;
		double entropy = 0;
		
		
		
		for(int i=0;i<fft.length-2;i++)
		{
			double p_i = fft[i].Mag / mag_sum;
			double q_i = fft_1[i].Mag / mag_sum_1;
			entropy += p_i * Math.log(2) * (p_i/q_i);
		}
		
		return -entropy;
	}
	
	public double SpectralFlux(FFT_Features[] fft,FFT_Features[] fft_1)
	{
		
		double sum = 0;
		
		
		
		for(int i=0;i<fft.length-2;i++)
		{
			double p_i = fft[i].Mag / mag_sum;
			double q_i = fft_1[i].Mag / mag_sum_1;
			sum += Math.pow(p_i - q_i, 2);
		}
		
		return sum;
	}
	
	public double SpectralRolloff(FFT_Features[] fft,double perc)
	{
		//double total_sum = 0;
		double entropy = 0;
		double rollOffFreq = 0;
		
		
		
		double roll_sum=0;
		for(int i=0;i<fft.length-2;i++)
		{
			roll_sum += fft[i].Mag;
			double p = roll_sum / mag_sum;
			if(p >= perc)
			{
				rollOffFreq = fft[i].Frequency;
				break;
			}
		}
		return rollOffFreq;
	}

	public static double Decibels(double RMS,int audioFormat)
	{
		double max = 32768;
		if(audioFormat == AudioFormat.ENCODING_PCM_8BIT)
		{
			max = 128;
		}

		return 20 * Math.log10(RMS/max);
	}
}
