package com.csri.ami.health_u.dataManagement.analyze.classifiers;

import com.csri.ami.health_u.dataManagement.record.motion.Quaternion;
import com.csri.ami.health_u.dataManagement.record.motion.Vector3D;

import java.util.ArrayList;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

//import org.apache.commons.math.*;
//import org.apache.commons.math.complex.Complex;
//import org.apache.commons.math.transform.FastFourierTransformer;

public class MotionFeatures 
{
	public static int MAX_FEATURE_WINDOW_SIZE = 256;
	//private Vector STANDING_DEFAULT_BF = new Vector(Math.PI/2, 0, 0);
	private Quaternion STANDING_DEFAULT_QUAT_BF = new Quaternion(0.6932,0.7200,-0.199,0.0206);
	//private static Vector STANDING_DEFAULT_GF = new Vector(9.806, 0, 0);
	//private Vector[] globalOrWindow_MAXSIZE;
	private Quaternion[] globalQuatWindow_MAXSIZE;
	public double fft = 0;
	long LastStandPosUpdate =0;
	long NumberClassifications= 0;
	long UpdateThreshold = 50;
	public MotionFeatures()
	{

	}

	public static Object[] ExtractFeaturesObject(double[] features)
	{
		//double[] features = ExtractFeatures(accel,gyro,globalAccel,globalGyro);

		Object[] objs = new Object[features.length];

		for(int i=0;i<features.length;i++)
		{
			objs[i] = features[i];
		}

		return objs;
	}

	public Object[] ExtractFeaturesObject(Vector3D[] accel,Vector3D[] gyro,Vector3D[] globalAccel,Vector3D[] globalGyro)
	{
		double[] features = ExtractFeatures(accel,gyro,globalAccel,globalGyro);

		Object[] objs = new Object[features.length];

		for(int i=0;i<features.length;i++)
		{
			objs[i] = features[i];
		}

		return objs;
	}

	public void UpdateStandPosition()
	{
		Vector3D stand = new Vector3D();

		if( globalQuatWindow_MAXSIZE != null)
		{
			
//			double x_1=0,x_2=0,y_1=0,y_2=0;
//			for(int i=0;i< globalOrWindow_MAXSIZE.length;i++)
//			{
//				x_1 += Math.cos(globalOrWindow_MAXSIZE[i].X);
//				x_2 += Math.sin(globalOrWindow_MAXSIZE[i].X);
//				y_1 += Math.cos(globalOrWindow_MAXSIZE[i].Y);
//				y_2 += Math.sin(globalOrWindow_MAXSIZE[i].Y);
//				
//				//stand.Z += globalOrWindow_MAXSIZE[i].Z / (double)globalOrWindow_MAXSIZE.length;
//			}
//			stand.X = Math.atan2(x_2,x_1);
//			stand.Y = Math.atan2(y_2,y_1);
//			STANDING_DEFAULT_BF = stand;
			STANDING_DEFAULT_QUAT_BF = Quaternion.Average(globalQuatWindow_MAXSIZE);
			LastStandPosUpdate = NumberClassifications;
		}
	}
	
	public boolean StandPositionDueUpdate()
	{
		if(LastStandPosUpdate == 0)
		{
			return true;
		}
		else
		{
			if(NumberClassifications - LastStandPosUpdate > UpdateThreshold)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}
	
	

	public double[] ExtractFeaturesWalkingVsAll(Vector3D[] accel,Vector3D[] gyro,Vector3D[] globalAccel,Vector3D[] globalGyro,Vector3D[] orientation,Quaternion[] quats)
	{
		NumberClassifications++;
		Vector3D[] accelWindow = getMainWindow(accel);
		double desiredTimeSpan = TimeSpan(accelWindow);
		double KeyTime = GetMidPointTime(accelWindow);

		Vector3D[] globalAccelWindow_MAXSIZE/*512*/ = getWindowAtTime(globalAccel,KeyTime,desiredTimeSpan);
		Vector3D[] globalGyroWindow_MAXSIZE/*512*/ = getWindowAtTime(globalGyro,KeyTime,desiredTimeSpan);
		//globalOrWindow_MAXSIZE/*512*/ = getWindowAtTime(orientation,KeyTime,desiredTimeSpan);
		globalQuatWindow_MAXSIZE/*512*/ = getWindowAtTime(quats,KeyTime,desiredTimeSpan);

		Vector3D[] globalAccelWindow_MAXSIZE_DIV2/*256*/ = getSubWindow(globalAccelWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/2);
		Vector3D[] globalGyroWindow_MAXSIZE_DIV2/*256*/ = getSubWindow(globalGyroWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/2);
		Vector3D[] globalAccelWindow_MAXSIZE_DIV4/*128*/ = getSubWindow(globalAccelWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/4);
		Vector3D[] globalGyroWindow_MAXSIZE_DIV4/*128*/ = getSubWindow(globalGyroWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/4);
		Vector3D[] globalAccelWindow_MAXSIZE_DIV8/*64*/ = getSubWindow(globalAccelWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/8);
		Vector3D[] globalGyroWindow_MAXSIZE_DIV8/*64*/ = getSubWindow(globalGyroWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/8);
		Vector3D[] globalAccelWindow_MAXSIZE_DIV16/*32*/ = getSubWindow(globalAccelWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/16);
		
		//Vector[] globalOrWindow_MAXSIZE_DIV2/*256*/ = getSubWindow(globalOrWindow_MAXSIZE,globalOrWindow_MAXSIZE.length/2);
		//Vector[] globalOrWindow_MAXSIZE_DIV4/*128*/ = getSubWindow(globalOrWindow_MAXSIZE,globalOrWindow_MAXSIZE.length/4);
		//Vector[] globalOrWindow_MAXSIZE_DIV16/*64*/ = getSubWindow(globalOrWindow_MAXSIZE,globalOrWindow_MAXSIZE.length/16);

		Quaternion[] globalQuatWindow_MAXSIZE_DIV2/*256*/ = getSubWindow(globalQuatWindow_MAXSIZE,globalQuatWindow_MAXSIZE.length/2);
		Quaternion[] globalQuatWindow_MAXSIZE_DIV4/*128*/ = getSubWindow(globalQuatWindow_MAXSIZE,globalQuatWindow_MAXSIZE.length/4);
		Quaternion[] globalQuatWindow_MAXSIZE_DIV16/*64*/ = getSubWindow(globalQuatWindow_MAXSIZE,globalQuatWindow_MAXSIZE.length/16);


		double[] features = new double[32];//2=256,  4=128,  8 = 64,   16=32
		features[0] = Max_Horizontal(globalAccelWindow_MAXSIZE_DIV8);
		features[1] = Avg_Horizontal(globalAccelWindow_MAXSIZE_DIV2);
		features[2] = Var_Horizontal(globalAccelWindow_MAXSIZE_DIV2,features[1]);
		
		features[3] = Max_Vertical(globalAccelWindow_MAXSIZE_DIV2);
		features[4] = Max_Vertical(globalAccelWindow_MAXSIZE_DIV4);
		features[5] = Max_Vertical(globalAccelWindow_MAXSIZE_DIV2);
		features[6] = Avg_Vertical(globalAccelWindow_MAXSIZE_DIV4);
		features[7] = Var_Vertical(globalAccelWindow_MAXSIZE_DIV4,features[5]);
		
		features[8] = RMS_Vertical(globalAccelWindow_MAXSIZE_DIV2);
		features[9] = IQR_Horizontal(globalGyroWindow_MAXSIZE_DIV4);
		
		features[10] = Avg_Vertical(globalAccelWindow_MAXSIZE_DIV16);
		features[11] = Avg_Mag(globalAccelWindow_MAXSIZE_DIV8);
		features[12] = Avg_Mag(globalAccelWindow_MAXSIZE);
		
		double avg_accelNorm_256 = Avg_Mag(globalAccelWindow_MAXSIZE);
		features[13] = Var_Mag(globalAccelWindow_MAXSIZE,avg_accelNorm_256);
		features[14] = IQR_Norm(globalAccelWindow_MAXSIZE_DIV4);
		features[15] = MFC_Mag(globalAccelWindow_MAXSIZE_DIV8);
		
		features[16] = ENERGY_LowPass(globalAccelWindow_MAXSIZE_DIV4);
		features[17] = ENERGY_BandPass(globalAccelWindow_MAXSIZE_DIV8);
		features[18] = ENERGY_BandPass(globalAccelWindow_MAXSIZE);
		features[19] = CORR_AccelVertBF_AccelNorm(globalAccelWindow_MAXSIZE_DIV4);
		//features[18] = ATT_StandingDiff(globalAccelWindow_MAXSIZE_DIV8);

		features[20] = StandingDiff(globalQuatWindow_MAXSIZE_DIV4,STANDING_DEFAULT_QUAT_BF);
		features[21] = StandingDiff_RateOfChange(globalQuatWindow_MAXSIZE_DIV4,STANDING_DEFAULT_QUAT_BF);
		features[22] = StandingDiff(globalQuatWindow_MAXSIZE_DIV16,STANDING_DEFAULT_QUAT_BF);
		features[23] = StandingDiff_RateOfChange(globalQuatWindow_MAXSIZE_DIV16,STANDING_DEFAULT_QUAT_BF);
		features[24] = StandingDiff(globalQuatWindow_MAXSIZE_DIV2,STANDING_DEFAULT_QUAT_BF);
		features[25] = StandingDiff_RateOfChange(globalQuatWindow_MAXSIZE_DIV2,STANDING_DEFAULT_QUAT_BF);

		features[26] = Avg_Horizontal(globalGyroWindow_MAXSIZE_DIV4);
		features[27] = Var_Horizontal(globalGyroWindow_MAXSIZE_DIV8,Avg_Horizontal(globalGyroWindow_MAXSIZE_DIV8));
		features[28] = Avg_Vertical(globalGyroWindow_MAXSIZE_DIV4);
		features[29] = Avg_Vertical(globalGyroWindow_MAXSIZE_DIV2);
		features[30] = Var_Vertical(globalGyroWindow_MAXSIZE_DIV4,features[27]);
		features[31] = CORR_Accel_Gyro(globalAccelWindow_MAXSIZE_DIV4,globalGyroWindow_MAXSIZE_DIV4);
		//fft = features[13];
		//double iqr = IQR_Horizontal(globalGyroWindow);
		//double[] test = new double[] { 1,2,3,2,1,1,2,3,2,1,1,2,3,4,5,4,3,2,1,2,3,4,3,2,1,2,3,4,3,2,1,1 };
		//double testfft = MFC_Mag(test);
		//fft = MFC_Mag(accelWindow);
		return features;
	}


	public double[] ExtractFeatures(Vector3D[] accel,Vector3D[] gyro,Vector3D[] globalAccel,Vector3D[] globalGyro)
	{
		Vector3D[] accelWindow = getMainWindow(accel);
		double desiredTimeSpan = TimeSpan(accelWindow);
		double KeyTime = GetMidPointTime(accelWindow);

		Vector3D[] globalAccelWindow_MAXSIZE/*512*/ = getWindowAtTime(globalAccel,KeyTime,desiredTimeSpan);
		Vector3D[] globalGyroWindow_MAXSIZE/*512*/ = getWindowAtTime(globalGyro,KeyTime,desiredTimeSpan);
		Vector3D[] globalAccelWindow_MAXSIZE_DIV2/*256*/ = getSubWindow(globalAccelWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/2);
		//Vector[] globalGyroWindow_MAXSIZE_DIV2/*256*/ = getSubWindow(globalGyroWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/2);
		Vector3D[] globalAccelWindow_MAXSIZE_DIV4/*128*/ = getSubWindow(globalAccelWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/4);
		Vector3D[] globalGyroWindow_MAXSIZE_DIV4/*128*/ = getSubWindow(globalGyroWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/4);
		Vector3D[] globalAccelWindow_MAXSIZE_DIV8/*64*/ = getSubWindow(globalAccelWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/8);
		//Vector[] globalGyroWindow_MAXSIZE_DIV8/*64*/ = getSubWindow(globalGyroWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/8);
		Vector3D[] globalAccelWindow_MAXSIZE_DIV16/*32*/ = getSubWindow(globalAccelWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/16);
		//Vector[] globalGyroWindow_MAXSIZE_DIV16/*32*/ = getSubWindow(globalGyroWindow_MAXSIZE,globalAccelWindow_MAXSIZE.length/16);


		double[] features = new double[19];
		features[0] = Max_Horizontal(globalAccelWindow_MAXSIZE_DIV4);
		features[1] = Avg_Horizontal(globalAccelWindow_MAXSIZE_DIV4);
		features[2] = Var_Horizontal(globalAccelWindow_MAXSIZE_DIV4,features[1]);
		features[3] = Max_Vertical(globalAccelWindow_MAXSIZE_DIV4);
		features[4] = Avg_Vertical(globalAccelWindow_MAXSIZE_DIV4);
		features[5] = Var_Vertical(globalAccelWindow_MAXSIZE_DIV4,features[4]);
		features[6] = RMS_Vertical(globalAccelWindow_MAXSIZE_DIV4);
		features[7] = IQR_Horizontal(globalGyroWindow_MAXSIZE_DIV4);
		features[8] = Avg_Vertical(globalAccelWindow_MAXSIZE_DIV16);
		features[9] = Avg_Mag(globalAccelWindow_MAXSIZE_DIV16);
		features[10] = Avg_Mag(globalAccelWindow_MAXSIZE);
		double avg_accelNorm_256 = Avg_Mag(globalAccelWindow_MAXSIZE_DIV2);
		features[11] = Var_Mag(globalAccelWindow_MAXSIZE_DIV2,avg_accelNorm_256);
		features[12] = IQR_Norm(globalAccelWindow_MAXSIZE_DIV4);
		features[13] = MFC_Mag(globalAccelWindow_MAXSIZE_DIV4);
		features[14] = ENERGY_LowPass(globalAccelWindow_MAXSIZE_DIV4);
		features[15] = ENERGY_BandPass(globalAccelWindow_MAXSIZE_DIV8);
		features[16] = ENERGY_BandPass(globalAccelWindow_MAXSIZE);
		features[17] = CORR_AccelVertBF_AccelNorm(globalAccelWindow_MAXSIZE_DIV4);
		//features[18] = ATT_StandingDiff(globalAccelWindow_MAXSIZE_DIV8);

		fft = features[13];
		//double iqr = IQR_Horizontal(globalGyroWindow);
		//double[] test = new double[] { 1,2,3,2,1,1,2,3,2,1,1,2,3,4,5,4,3,2,1,2,3,4,3,2,1,2,3,4,3,2,1,1 };
		//double testfft = MFC_Mag(test);
		//fft = MFC_Mag(accelWindow);
		return features;
	}

	public Vector3D[] getSubWindow(Vector3D[] data,int size)
	{
		Vector3D[] subwindow = new Vector3D[size];
		int startPoint = (data.length/2) - (size / 2);
		int endPoint = (data.length/2) + (size / 2);
		int count =0;
		for(int i=startPoint;i<endPoint;i++)
		{
			subwindow[count] = data[i];
			count++;
		}

		return subwindow;
	}
	
	public Quaternion[] getSubWindow(Quaternion[] data,int size)
	{
		Quaternion[] subwindow = new Quaternion[size];
		int startPoint = (data.length/2) - (size / 2);
		int endPoint = (data.length/2) + (size / 2);
		int count =0;
		for(int i=startPoint;i<endPoint;i++)
		{
			subwindow[count] = data[i];
			count++;
		}

		return subwindow;
	}

	public static double Max_Horizontal(Vector3D[] data)
	{
		double max = Double.MIN_VALUE;

		for (int i = 0; i < data.length; i++)
		{

			if (data[i].Horizontal_Magnitude > max)
			{
				max = data[i].Horizontal_Magnitude;
			}
		}
		return max;
	}

	public static double Avg_Horizontal(Vector3D[] data)
	{
		double avg = 0;

		for (int i = 0; i < data.length; i++)
		{
			avg += data[i].Horizontal_Magnitude / (double)data.length;
		}
		return avg;
	}

	public static double Var_Horizontal(Vector3D[] data,double avg)
	{
		double var = 0;

		for (int i = 0; i < data.length; i++)
		{
			var += Math.pow(data[i].Horizontal_Magnitude - avg, 2);//data[i].Horizontal_Magnitude / (double)data.length;
		}
		return Math.sqrt(var/(double)data.length);
	}

	public static double Max_Vertical(Vector3D[] data)
	{
		double max = Double.MIN_VALUE;

		for (int i = 0; i < data.length; i++)
		{

			if (data[i].Vertical_Magnitude > max)
			{
				max = data[i].Vertical_Magnitude;
			}
		}
		return max;
	}
	
	public static double Min_Vertical(Vector3D[] data)
	{
		double min = Double.MAX_VALUE;

		for (int i = 0; i < data.length; i++)
		{

			if (data[i].Vertical_Magnitude < min)
			{
				min = data[i].Vertical_Magnitude;
			}
		}
		return min;
	}


	public static double Avg_Vertical(Vector3D[] data)
	{
		double avg = 0;

		for (int i = 0; i < data.length; i++)
		{
			avg += data[i].Vertical_Magnitude / (double)data.length;
		}
		return avg;
	}

	public static double Var_Vertical(Vector3D[] data,double avg)
	{
		double var = 0;

		for (int i = 0; i < data.length; i++)
		{
			var += Math.pow(data[i].Vertical_Magnitude - avg, 2);//data[i].Horizontal_Magnitude / (double)data.length;
		}
		return Math.sqrt(var/(double)data.length);
	}

	public static double Avg_Mag(Vector3D[] data)
	{
		double avg = 0;

		for (int i = 0; i < data.length; i++)
		{
			avg += data[i].Magnitude / (double)data.length;
		}
		return avg;
	}

	public static double Var_Mag(Vector3D[] data,double avg)
	{
		double var = 0;

		for (int i = 0; i < data.length; i++)
		{
			var += Math.pow(data[i].Magnitude - avg, 2);//data[i].Horizontal_Magnitude / (double)data.length;
		}
		return Math.sqrt(var/(double)data.length);
	}

	public static double RMS_Vertical(Vector3D[] data)
	{
		double rms = 0;
		for (int i = 0; i < data.length; i++)
		{
			rms += Math.pow(data[i].Vertical_Magnitude, 2);
		}
		rms = Math.sqrt(rms / (double)data.length);

		return rms;
	}

	public static double IQR_Horizontal(Vector3D[] data)
	{
		ArrayList<Double> rawData = new ArrayList<Double>();
		for(int i=0;i<data.length;i++)
		{
			rawData.add(data[i].Horizontal_Magnitude);
		}
		double iqr = 0;
		try{
			InterQuartile i = new InterQuartile();
			iqr = i.InterQuartileRange(rawData);
		}
		catch(Exception e){}

		return iqr;
	}

	public static double IQR_Vertical(Vector3D[] data)
	{
		ArrayList<Double> rawData = new ArrayList<Double>();
		for(int i=0;i<data.length;i++)
		{
			rawData.add(data[i].Vertical_Magnitude);
		}
		double iqr = 0;
		try{
			InterQuartile i = new InterQuartile();
			iqr = i.InterQuartileRange(rawData);
		}
		catch(Exception e){}

		return iqr;
	}

	public static double IQR_Norm(Vector3D[] data)
	{
		ArrayList<Double> rawData = new ArrayList<Double>();
		for(int i=0;i<data.length;i++)
		{
			rawData.add(data[i].Magnitude);
		}
		double iqr = 0;
		try{
			InterQuartile i = new InterQuartile();
			iqr = i.InterQuartileRange(rawData);
		}
		catch(Exception e){}

		return iqr;
	}
	
	public static FFT_Features[] FFT(short[] data,int n)
	{
		double[] rawData = new double[data.length];
		for(int i=0;i<data.length;i++)
		{
			rawData[i] = data[i];
		}

		DoubleFFT_1D fft2 = new DoubleFFT_1D(data.length);
		fft2.realForward(rawData);
		Complex[] windowC = ConvertFFTresultsToComplex(rawData);
		//         Complex[] windowC = GetComplexWindow(rawData); 
		//         windowC = FFT.fft(windowC);
		FFT_Features[] peaks = GetPeaks(windowC, n,false);


		return peaks;
	}
	
	public static FFT_Features[] FFT_Bands(Complex[] windowC,double[] bands,int sampleRate)
	{
//		double[] rawData = new double[data.length];
//		for(int i=0;i<data.length;i++)
//		{
//			rawData[i] = data[i];
//		}

		//DoubleFFT_1D fft2 = new DoubleFFT_1D(data.length);
		//fft2.realForward(rawData);
		//Complex[] windowC = ConvertFFTresultsToComplex(rawData);
		//         Complex[] windowC = GetComplexWindow(rawData); 
		//         windowC = FFT.fft(windowC);
		FFT_Features[] peaks = GetFreqBands(windowC,false,bands,sampleRate);


		return peaks;
	}
	
	
	
	public static FFT_Features[] FFT(short[] data)
	{
		double[] rawData = new double[data.length];
		for(int i=0;i<data.length;i++)
		{
			rawData[i] = data[i];
		}

		DoubleFFT_1D fft2 = new DoubleFFT_1D(data.length);
		fft2.realForward(rawData);
		Complex[] windowC = ConvertFFTresultsToComplex(rawData);
		//         Complex[] windowC = GetComplexWindow(rawData); 
		//         windowC = FFT.fft(windowC);
		FFT_Features[] peaks = GetPeaks(windowC,false);


		return peaks;
	}

	public static double MFC_Mag(Vector3D[] data)
	{
		double[] rawData = new double[data.length];
		for(int i=0;i<data.length;i++)
		{
			rawData[i] = data[i].Magnitude;
		}

		DoubleFFT_1D fft2 = new DoubleFFT_1D(data.length);
		fft2.realForward(rawData);
		Complex[] windowC = ConvertFFTresultsToComplex(rawData);
		//         Complex[] windowC = GetComplexWindow(rawData); 
		//         windowC = FFT.fft(windowC);
		FFT_Features[] peaks = GetPeaks(windowC, 1,false);


		return peaks[0].Frequency;
	}

	public static double MFC_Mag(double[] rawData)
	{


		DoubleFFT_1D fft2 = new DoubleFFT_1D(rawData.length);
		fft2.realForward(rawData);
		Complex[] windowC = ConvertFFTresultsToComplex(rawData);
		// Complex[] windowC = GetComplexWindow(rawData); 
		// windowC = FFT.fft(windowC);
		FFT_Features[] peaks = GetPeaks(windowC, 1,false);


		return peaks[0].Frequency;
	}

	public static double ENERGY_LowPass(Vector3D[] data)
	{
		double e = 0;
		for (int i = 0; i < data.length; i++)
		{
			e += Math.pow(data[i].LowPass_Mag,2);
		}

		return Math.sqrt(e);
	}

	public static double ENERGY_BandPass(Vector3D[] data)
	{
		double e = 0;
		for (int i = 0; i < data.length; i++)
		{
			e += Math.pow(data[i].BandPass_Mag,2);
		}

		return Math.sqrt(e);
	}

	public static double StandingDiff(Vector3D[] data,Vector3D standing)
	{
		double diff = 0;
		for (int i = 0; i < data.length; i++)
		{

			double xDiff = Math.pow(angleDiff(data[i].X,standing.X), 2);
			double yDiff = Math.pow(angleDiff(data[i].Y,standing.Y), 2);
			//double zDiff = Math.pow(angleDiff(data[i].Z,standing.Z), 2);
			diff += Math.sqrt(xDiff + (yDiff));
		}
		return diff;// / (double)data.Length;
	}
	
	public static double StandingDiff(Quaternion[] data,Quaternion standing)
	{
		double diff = 0;
        Quaternion[] qs = new Quaternion[data.length];
        for (int i = 0; i < data.length; i++)
        {

            qs[i] = standing.Difference(data[i]);
        }
        Quaternion avgDiff = Quaternion.Average(qs);
        double aq = avgDiff.Angle();
        return aq;// / (double)data.Length;
	}
	
	public static double StandingDiff_RateOfChange(Quaternion[] data,Quaternion standing)
	{
		double diff = 0;
        Quaternion[] qs = new Quaternion[data.length-1];
        for (int i = 1; i < data.length; i++)
        {

            qs[i-1] = data[i].Difference(data[i-1]);
        }
        Quaternion avgDiff = Quaternion.Average(qs);
        double aq = avgDiff.Angle();
        return aq;// / (double)data.Length;
	}
	
	 public static double angleDiff(double a,double b)
     {
         double c_a = 0, s_a = 0, c_b = 0, s_b = 0;
         c_a = Math.cos(a);
         s_a = Math.sin(a);

         c_b = Math.cos(b);
         s_b = Math.sin(b);

         double sin_a_minus_b = (s_a * c_b) - (s_b * c_a);
         return Math.asin(sin_a_minus_b);
     }

	public static double StandingDiff_RateOfChange(Vector3D[] data,Vector3D standing)
	{
		double diff = 0;
		for (int i = 1; i < data.length; i++)
		{
			double xDiff = Math.pow(angleDiff(data[i].X,standing.X), 2) - Math.pow(angleDiff(data[i-1].X,standing.X), 2);
			double yDiff = Math.pow(angleDiff(data[i].Y,standing.Y), 2) - Math.pow(angleDiff(data[i-1].Y,standing.Y), 2);
			//double zDiff = Math.pow(angleDiff(data[i].Z,standing.Z), 2) - Math.pow(angleDiff(data[i-1].Z,standing.Z), 2);
			diff += xDiff + yDiff;
		}
		return diff / (double)data.length;
	}

	public static double CORR_AccelVertBF_AccelNorm(Vector3D[] data)
	{
		//  AForge.Math.Metrics.PearsonCorrelation cor = new AForge.Math.Metrics.PearsonCorrelation(); 

		double[] accelvertBF = new double[data.length];
		double[] accelNorm = new double[data.length];

		for (int i = 0; i < data.length; i++)
		{
			accelvertBF[i] = data[i].Vertical_Magnitude;
			accelNorm[i] = data[i].Magnitude;
		}

		return PearsonCorrelation(accelvertBF, accelNorm);
	}

	public static double CORR_Accel_Gyro(Vector3D[] accel,Vector3D[] gyro)
	{
		//  AForge.Math.Metrics.PearsonCorrelation cor = new AForge.Math.Metrics.PearsonCorrelation(); 

		double[] accelvertBF = new double[accel.length];
		double[] accelNorm = new double[gyro.length];

		for (int i = 0; i < accel.length; i++)
		{
			accelvertBF[i] = accel[i].Magnitude;
			accelNorm[i] = gyro[i].Magnitude;
		}

		return PearsonCorrelation(accelvertBF, accelNorm);
	}

//	public static double ATT_StandingDiff(Vector[] data)
//	{
//		double diff = 0;
//		for (int i = 0; i < data.length; i++)
//		{
//			double horDiff = Math.pow(data[i].Horizontal_Magnitude - STANDING_DEFAULT_BF.Horizontal_Magnitude, 2);
//			double verDiff =  Math.pow(data[i].Vertical_Magnitude - STANDING_DEFAULT_BF.Vertical_Magnitude, 2);
//			diff += horDiff + verDiff;
//		}
//		return diff / (double)data.length;
//	}

	private static Complex[] ConvertFFTresultsToComplex(double[] data)
	{
		Complex[] c = new Complex[(data.length/2)-1];
		int start =2;
		int count =0;
		for(int i=1;i<(data.length/2);i++)
		{
			c[count] = new Complex(data[i*2],data[(i*2)+1]);
			count++;
		}

		return c;
	}
	
	

	private static Complex[] GetComplexWindow(double[] data)
	{
		Complex[] window = new Complex[data.length];
		int count = 0;
		for (int i = 0; count < window.length && i < data.length; i++)
		{
			window[count] = new Complex(data[i],0);

			count++;
		}
		return window;
	}

	private static FFT_Features[] GetPeaks(Complex[] data, int numberPeaks,boolean symetricPresent)
	{
		FFT_Features[] peaks = new FFT_Features[numberPeaks];

		for (int i = 0; i < numberPeaks; i++)
		{
			double max = Double.MIN_VALUE;
			int maxindex = -1;
			int len = data.length;
			if( symetricPresent)
			{
				len = (data.length / 2)+1;
			}
			for (int j = 1;j <  len-1 && j < data.length-1; j++)
			{
				boolean contains = Contains(peaks, data[j].abs());
				if (data[j].abs() > max && !contains)
				{
					if (j == 0 && data[j + 1].abs() < data[j].abs())
					{
						max = data[j].abs();
						maxindex = j;
					}
					else if (j == data.length - 1 && data[j - 1].abs() < data[j].abs())
					{
						max = data[j].abs();
						maxindex = j;
					}
					else if (data[j + 1].abs() < data[j].abs() && data[j - 1].abs() < data[j].abs())
					{
						max = data[j].abs();
						maxindex = j;
					}

				}
			}
			peaks[i] = new FFT_Features();
			peaks[i].Mag = max;
			peaks[i].Index = maxindex +1;
			double sizeOfData = (double)data.length - 1;
			if(!symetricPresent)
			{

				sizeOfData = ((double)(data.length +1) * 2);
				// maxindex = (int)sizeOfData - maxindex;
			}
			peaks[i].Frequency = ((double)maxindex) / (sizeOfData-1);
		}

		//peakIndices = indices;
		return peaks;
	}
	
	private static FFT_Features[] GetFreqBands(Complex[] data,boolean symetricPresent,double[] bands,int sampleRate)
	{
		double sizeOfData = (double)data.length - 1;
		int len = data.length;
		if(!symetricPresent)
		{
			sizeOfData = ((double)(data.length +1) * 2);
			len = (data.length / 2)+1;
			// maxindex = (int)sizeOfData - maxindex;
		}
		
		
		double[] totalMags = new double[bands.length];
		
		for (int j = 1;j <  len-1 && j < data.length-1; j++)
		{
			double currentFreq = (((double)j) / (sizeOfData-1)) * sampleRate;
			int currentBand = GetBandIndex(bands,currentFreq);
			if(currentBand != -1)
			{
				totalMags[currentBand] += data[j].abs();
			}
			
			
		}
		
		FFT_Features[] ffts = new FFT_Features[bands.length];
		
		double prev =0;
		double sum = 0;
		for(int i=0;i<bands.length;i++)
		{
			double bandLen = bands[i] - prev;
			
			totalMags[i] = totalMags[i] / (double)bandLen;
			
			ffts[i] = new FFT_Features();
			ffts[i].Frequency = bands[i];
			ffts[i].Mag = totalMags[i];
			
			sum += ffts[i].Mag;
			prev = bands[i];
		}
		
		
		for(int i=0;i<ffts.length;i++)
		{
			ffts[i].Mag = ffts[i].Mag / sum;
		}
		return ffts;
	}
	
	private static int GetBandIndex(double[] bands,double freq)
	{
		double min =0;
		int bandIndex = -1;
		for(int i=0;i<bands.length;i++)
		{
			if(freq >= min && freq <= bands[i])
			{
				bandIndex = i;
				break;
			}
			else
			{
				min = bands[i];
			}
		}
		
		return bandIndex;
	}
	
	public static double[] FrequencyEq(double[] data,int sampleRate,double[] bands,double[] equalization)
	{
		double[] rawData = data;
		
		DoubleFFT_1D fft2 = new DoubleFFT_1D(rawData.length);
		fft2.realForward(rawData);
		Complex dc = new Complex(rawData[0],rawData[1]);
		Complex[] windowC = ConvertFFTresultsToComplex(rawData);
		
		double avg =0;
		for(int i=0;i<windowC.length;i++)
		{
			avg += Math.abs( windowC[i].re()) / (double)windowC.length;
		}
		
		FFT_Features[] fft = GetFFTFeatures(windowC,false,sampleRate);
		
		fft = ModifyFrequencies(fft,bands,equalization);
		
		double[] raw = FFT_To_Raw(dc,fft);
		double[] newRaw=null;
		try
		{
			fft2.realInverse(raw, true);
			newRaw = new double[raw.length];
			for(int i=0;i<raw.length;i++)
			{
				newRaw[i] = raw[i];
			}
		}
		catch(Exception e)
		{
			String error = e.getLocalizedMessage();
			int er =1;
		}
		
		
		
		return newRaw;
	}
	
	private static double[] FFT_To_Raw(Complex c,FFT_Features[] fft)
	{
		double[] raw = new double[2+ (fft.length * 2)];
		
		raw[0] = c.re();
		raw[1] = c.im();
		
		for(int i=0;i<fft.length;i++)
		{
			int index = 2 + (i * 2);
			raw[index] = fft[i].Real;
			raw[index +1] = fft[i].Im;
		}
		
		return raw;
	}
	
	
	
	private static FFT_Features[] ModifyFrequencies(FFT_Features[] f,double[] bands,double[] equalization)
	{
		
		
		for(int i=0;i<f.length;i++)
		{
			if(f[i] != null)
			{
				double freq = f[i].Frequency;
				int band = -1;
				for(int j=0;j<bands.length-1;j++)
				{
					if(freq > bands[j] && freq < bands[j+1])
					{
						band = j;
						break;
					}
				}
				
				if(band != -1)
				{
					f[i].Real *= equalization[band];
					f[i].Im *= equalization[band];
					f[i].Mag = Math.sqrt(Math.pow(f[i].Real, 2) + Math.pow(f[i].Im, 2));
				}
				
				
				
			}
		}
		
		return f;
	}
	
	public static Complex[] FFT_Raw(double[] data,int sampleRate,DoubleFFT_1D fft2)
	{
		double[] rawData = data;

		
		//DoubleFFT_1D fft2 = new DoubleFFT_1D(rawData.length);
		fft2.realForward(rawData);
		//fft2 = null;
		
		Complex[] windowC = ConvertFFTresultsToComplex(rawData);
		
		return windowC;
	}
	
	public static FFT_Features[] GetFFTFeatures(Complex[] data,boolean symetricPresent,int sampleRate)
	{
		
		//FFT_Features[] peaks = new FFT_Features[numberPeaks];
	
		
		int len = data.length;
		if( symetricPresent)
		{
			len = (data.length / 2)+1;
		}
		FFT_Features[] peaks = new FFT_Features[len];
		
		for (int j = 0;j <  len && j < data.length; j++)
		{

			FFT_Features peak = new FFT_Features();
			peak.Real = data[j].re();
			peak.Im = data[j].im();
			peak.Mag = data[j].abs();
			peak.Index = j +1;
			double sizeOfData = (double)data.length - 1;
			if(!symetricPresent)
			{

				sizeOfData = ((double)(data.length +1) * 2);
				// maxindex = (int)sizeOfData - maxindex;
			}
			peak.Frequency = ((double)(j+1)) / (sizeOfData-1) * sampleRate;
			
			
		    peaks[j] = peak;
		
		}
		
		return peaks;
	}
	
	private static FFT_Features[] GetPeaks(Complex[] data,boolean symetricPresent)
	{
		ArrayList<FFT_Features> peaks = new ArrayList<FFT_Features>();
		//FFT_Features[] peaks = new FFT_Features[numberPeaks];
		
		double minRatioThreshold = 0.5;
		int minRequiredPeaks = 2;
		double maxMag = Double.MIN_VALUE;
		boolean finished = false;
		while(!finished)
		{
			double max = Double.MIN_VALUE;
			int maxindex = -1;
			int len = data.length;
			if( symetricPresent)
			{
				len = (data.length / 2)+1;
			}
			for (int j = 1;j <  len-1 && j < data.length-1; j++)
			{
				boolean contains = Contains(peaks, data[j].abs());
				if (data[j].abs() > max && !contains)
				{
					if (j == 0 && data[j + 1].abs() < data[j].abs())
					{
						max = data[j].abs();
						maxindex = j;
					}
					else if (j == data.length - 1 && data[j - 1].abs() < data[j].abs())
					{
						max = data[j].abs();
						maxindex = j;
					}
					else if (data[j + 1].abs() < data[j].abs() && data[j - 1].abs() < data[j].abs())
					{
						max = data[j].abs();
						maxindex = j;
					}

				}
			}
			FFT_Features peak = new FFT_Features();
			peak.Mag = max;
			peak.Index = maxindex +1;
			double sizeOfData = (double)data.length - 1;
			if(!symetricPresent)
			{

				sizeOfData = ((double)(data.length +1) * 2);
				// maxindex = (int)sizeOfData - maxindex;
			}
			peak.Frequency = ((double)maxindex) / (sizeOfData-1);
			if(peak.Mag > maxMag)
			{
				maxMag = peak.Mag;
			}
			if(peaks.size() < minRequiredPeaks-1)
			{
				peaks.add(peak);
			}
			else if(peak.Mag > minRatioThreshold * maxMag)
			{
				peaks.add(peak);
			}
			else
			{
				finished = true;
			}
			
			
		}

		//peakIndices = indices;
		FFT_Features[] temp = new FFT_Features[peaks.size()];
		for(int i=0;i<temp.length;i++)
		{
			temp[i] = peaks.get(i);
		}
		return temp;
	}

	public static double PearsonCorrelation(double[] scores1,double[] scores2)
	{
		double result = 0;
		double sum_sq_x = 0;
		double sum_sq_y = 0;
		double sum_coproduct = 0;
		double mean_x = scores1[0];
		double mean_y = scores2[0];
		for(int i=2;i<scores1.length+1;i+=1){
			double sweep =Double.valueOf(i-1)/i;
			double delta_x = scores1[i-1]-mean_x;
			double delta_y = scores2[i-1]-mean_y;
			sum_sq_x += delta_x * delta_x * sweep;
			sum_sq_y += delta_y * delta_y * sweep;
			sum_coproduct += delta_x * delta_y * sweep;
			mean_x += delta_x / i;
			mean_y += delta_y / i;
		}
		double pop_sd_x = (double) Math.sqrt(sum_sq_x/scores1.length);
		double pop_sd_y = (double) Math.sqrt(sum_sq_y/scores1.length);
		double cov_x_y = sum_coproduct / scores1.length;
		result = cov_x_y / (pop_sd_x*pop_sd_y);
		return result;
	}
	
	private static boolean Contains(ArrayList<FFT_Features> data, double val)
	{
		boolean contains = false;
		for (int i = 0; i < data.size(); i++)
		{
			FFT_Features f = data.get(i);
			if (f != null)
			{
				
				if (f.Mag == val)
				{
					contains = true;
				}
			}
		}
		return contains;
	}

	private static boolean Contains(FFT_Features[] data, double val)
	{
		boolean contains = false;
		for (int i = 0; i < data.length; i++)
		{
			if (data[i] != null)
			{
				if (data[i].Mag == val)
				{
					contains = true;
				}
			}
		}
		return contains;
	}

	public double GetMidPointTime(Vector3D[] accelWindow)
	{
		int midPoint = accelWindow.length/2;

		return accelWindow[midPoint].MilliTimeStamp;
	}

	public double TimeSpan(Vector3D[] data)
	{
		double sumTime =0;
		sumTime = data[data.length-1].MilliTimeStamp - data[0].MilliTimeStamp;
		return sumTime;
	}

	public double TimeSpan(Vector3D[] data,int start,int end)
	{
		double sumTime =0;
		sumTime = data[end].MilliTimeStamp - data[start].MilliTimeStamp;
		return sumTime;
	}
	
	public double TimeSpan(Quaternion[] data,int start,int end)
	{
		double sumTime =0;
		sumTime = data[end].timeStamp - data[start].timeStamp;
		return sumTime;
	}

	public Vector3D[] getMainWindow(Vector3D[] data)
	{
		int startpoint = data.length - MAX_FEATURE_WINDOW_SIZE;
		Vector3D[] window = new Vector3D[MAX_FEATURE_WINDOW_SIZE];

		for(int i=startpoint;i<data.length;i++)
		{
			window[i-startpoint] = data[i];
		}
		return window;
	}

	public Vector3D[] getWindowAtTime(Vector3D[] data,double time,double desiredTimeSpan)
	{
		int index = FindTimeIndex(data,time);
		int windowLength = GetBestFitWindowLength(data,index,desiredTimeSpan);

		Vector3D[] window = new Vector3D[windowLength];
		int start = index - (windowLength/2);
		int end = index + (windowLength/2)-1;
		for(int i=start;i<=end;i++)
		{
			window[i-start] = data[i];
		}

		return window;
	}
	
	public Quaternion[] getWindowAtTime(Quaternion[] data,double time,double desiredTimeSpan)
	{
		int index = FindTimeIndex(data,time);
		int windowLength = GetBestFitWindowLength(data,index,desiredTimeSpan);

		Quaternion[] window = new Quaternion[windowLength];
		int start = index - (windowLength/2);
		int end = index + (windowLength/2)-1;
		for(int i=start;i<=end;i++)
		{
			window[i-start] = data[i];
		}

		return window;
	}

	public int GetBestFitWindowLength(Vector3D[] data,int MidPoint,double desiredTimeSpan)
	{
		int startPower = 4;
		int endPower = 10;
		double bestDiff = Double.MAX_VALUE;
		int bestPow = -1;
		for(int i=startPower;i<=endPower;i++)
		{
			int currentTestLength = (int)Math.pow(2, i);
			int diff = currentTestLength /2;
			int start = MidPoint - diff;
			int end = (MidPoint+ diff)-1;
			if(start >= 0 && end < data.length)
			{
				double currentSpan = TimeSpan(data,start,end);

				double diffSpan = Math.abs(currentSpan - desiredTimeSpan);
				if(diffSpan < bestDiff)
				{
					bestDiff = diffSpan;
					bestPow = currentTestLength;
				}
			}
		}
		return bestPow;
	}
	
	public int GetBestFitWindowLength(Quaternion[] data,int MidPoint,double desiredTimeSpan)
	{
		int startPower = 4;
		int endPower = 10;
		double bestDiff = Double.MAX_VALUE;
		int bestPow = -1;
		for(int i=startPower;i<=endPower;i++)
		{
			int currentTestLength = (int)Math.pow(2, i);
			int diff = currentTestLength /2;
			int start = MidPoint - diff;
			int end = (MidPoint+ diff)-1;
			if(start >= 0 && end < data.length)
			{
				double currentSpan = TimeSpan(data,start,end);

				double diffSpan = Math.abs(currentSpan - desiredTimeSpan);
				if(diffSpan < bestDiff)
				{
					bestDiff = diffSpan;
					bestPow = currentTestLength;
				}
			}
		}
		return bestPow;
	}

	public int FindTimeIndex(Vector3D[] data,double time)
	{
		int index = 0;
		double bestDiff = Double.MAX_VALUE;

		for(int i=0;i<data.length;i++)
		{
			double diff = Math.abs(data[i].MilliTimeStamp - time);

			if(diff < bestDiff)
			{
				bestDiff = diff;
				index = i;
			}
		}

		return index;
	}
	
	public int FindTimeIndex(Quaternion[] data,double time)
	{
		int index = 0;
		double bestDiff = Double.MAX_VALUE;

		for(int i=0;i<data.length;i++)
		{
			double diff = Math.abs(data[i].timeStamp - time);

			if(diff < bestDiff)
			{
				bestDiff = diff;
				index = i;
			}
		}

		return index;
	}
}
