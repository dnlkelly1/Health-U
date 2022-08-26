package com.csri.ami.health_u.dataManagement.analyze.classifiers;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class ARFF_Loader 
{
	public ArrayList<Object[]> allFeatures;
	public ArrayList<String> labels;

	public double[] counts;
	public double[][] m ;
	public double[][][] cov;

	private static String[] definedLabels =  new String[]{  "STNDING", "WALKING", "TRNSDWN", "SITTING", "TRANSUP"};

	public ARFF_Loader(String filename)
	{
		Load(filename,32);
		//m = LabelMeans();
		//cov = Covariance(m);
	}
	
	

//	private double[][][] Covariance(double[][] means)
//	{
//		double[][][] cov_all = new double[means.length][][];
//		double[][][] mats = new double[means.length][][];
//		for(int i=0;i<mats.length;i++)
//		{
//			mats[i] = getMatrix(definedLabels[i].toLowerCase(),means[i]);
//			Matrix m = new Matrix(mats[i]);
//			
//			Matrix c = m.transpose().times(m);
//			double n = 1/(double)mats[i].length;
//			c = c.times(n);
//			
//			cov_all[i] = c.getArray();
//			
//		}
//		return cov_all;
//		//		double[][][] cov_all = new double[means.length][][];
//		//		for(int i=0;i<cov_all.length;i++)
//		//		{
//		//			cov_all[i] = new double[means[i].length][means[i].length];
//		//			for(int j=0;j<means[i].length;j++)
//		//			{
//		//				for(int k=0;k<means[i].length;k++)
//		//				{
//		//					cov_all[i][j][k] = Cov(means[i],j,k,definedLabels[i].toLowerCase());
//		//				}
//		//			}
//		//		}
//		//		
//		//		return cov_all;
//	}

//	private double[][] LabelMeans()
//	{
//
//
//		double[][] means = new double[definedLabels.length][];
//
//		for(int i=0;i<means.length;i++)
//		{
//			means[i] = Mean(definedLabels[i].toLowerCase());
//		}
//
//		return means;
//	}

//	private double[][] getMatrix(String label,double[] means)
//	{
//		double c =0;
//		double[] m = null;
//		int count =0;
//		List<double[]> mat = new ArrayList<double[]>();
//		for(int i=0;i<allFeatures.size();i++)
//		{
//			if(labels.get(i).compareTo(label) == 0)
//			{
//				count++;
//				Object[] current = allFeatures.get(i);
//				double[] res = ADA_SitStand.classifyArray(current);
//				for(int j=0;j<means.length;j++)
//				{
//					res[j] = res[j] - means[j];
//				}
//				mat.add(res);
//
//			}
//
//		}
//
//		double[][] mat_array = new double[mat.size()][];
//		for(int i=0;i<mat.size();i++)
//		{
//			mat_array[i] = mat.get(i);
//		}
//		return mat_array;
//	}
	
//	public List<ObservationVector> getObs(int index)
//	{
//		return getObs(definedLabels[index]);
//	}
	
//	private List<ObservationVector> getObs(String label)
//	{
//		Log.w("getObs",label);
//		double c =0;
//		double[] m = null;
//		int count =0;
//		List<ObservationVector> mat = new ArrayList<ObservationVector>();
//		for(int i=0;i<allFeatures.size();i++)
//		{
//			if(labels.get(i).toLowerCase().compareTo(label.toLowerCase()) == 0)
//			{
//				count++;
//				Object[] current = allFeatures.get(i);
//				double[] res = ADA_SitStand.classifyArray(current);
//				
//				mat.add(new ObservationVector(res));
//
//			}
//
//		}
//
//		
//		return mat;
//	}

//	private double Cov(double[] mean, int a,int b,String label)
//	{
//		double c =0;
//		double[] m = null;
//		int count =0;
//		//List<double[]> mat = new ArrayList<double[]>();
//		for(int i=0;i<allFeatures.size();i++)
//		{
//			if(labels.get(i).compareTo(label) == 0)
//			{
//				count++;
//				Object[] current = allFeatures.get(i);
//				double[] res = ADA_SitStand.classifyArray(current);
//				//mat.add(res);
//				if(m == null)
//				{
//					m = new double[res.length];
//				}
//
//				c += (res[a] - mean[a]) * (res[b] - mean[b]);
//			}
//
//		}
//
//		return c / (double)(count-1);
//	}

//	private double[] Mean(String label)
//	{
//		double[] m = null;
//		int count =0;
//		for(int i=0;i<allFeatures.size();i++)
//		{
//			if(labels.get(i).compareTo(label) == 0)
//			{
//				count++;
//				Object[] current = allFeatures.get(i);
//				double[] res = ADA_SitStand.classifyArray(current);
//				if(m == null)
//				{
//					m = new double[res.length];
//				}
//
//				for(int j=0;j<res.length;j++)
//				{
//					m[j] += (Double)res[j];
//				}
//			}
//
//		}
//
//		if(m != null)
//		{
//			for(int i=0;i<m.length;i++)
//			{
//				m[i] = m[i] / (double)count;
//			}
//		}
//
//		return m;
//	}

	private void Load(String filename,int numFeatures)
	{
		try
		{
			boolean mExternalStorageAvailable = false;
			boolean mExternalStorageWriteable = false;
			String state = Environment.getExternalStorageState();

			if (Environment.MEDIA_MOUNTED.equals(state)) 
			{
				// We can read and write the media
				mExternalStorageAvailable = mExternalStorageWriteable = true;
			} 


			if( mExternalStorageAvailable)
			{

				File ex = Environment.getExternalStorageDirectory();

				String fullfilename = ex.getAbsoluteFile() + "/Android/data/com.sensor.record/files/" + filename;
				FileReader fr = new FileReader(fullfilename); 
				BufferedReader br = new BufferedReader(fr);

				labels = new ArrayList<String>();
				allFeatures = new ArrayList<Object[]>();

				String line = br.readLine();

				while(line != null && line.compareTo("@DATA") != 0)
				{
					line = br.readLine();
				}

				if(line != null && line.compareTo("@DATA") == 0)
				{
					line = br.readLine();
					while(line != null)
					{
						StringTokenizer st = null;

						st = new StringTokenizer(line, ",");
						List<String> tokens = new ArrayList<String>();
						while(st.hasMoreTokens())
						{
							String current = st.nextToken();
							tokens.add(current);
						}

						ArrayList<Double> features = new ArrayList<Double>();
						for(int i=0;i<numFeatures;i++)
						{
							features.add(Double.parseDouble(tokens.get(i)));
						}

						labels.add(tokens.get(tokens.size()-1));
						allFeatures.add(features.toArray());


						line = br.readLine();
					}

					int count = labels.size();


				}

			}
		}
		catch(IOException e)
		{

		}

	}
}
