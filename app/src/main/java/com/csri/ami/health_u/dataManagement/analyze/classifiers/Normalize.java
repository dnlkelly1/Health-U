package com.csri.ami.health_u.dataManagement.analyze.classifiers;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class Normalize 
{
	public double[] max = null;
	public double[] min = null;
	public double[] mean=null;
    public double[] stdDev = null;
    
    public Normalize(AssetManager am)
    {
    	Init(am);
    }
    
    public double[] NormalizeWithSavedVecotrs(double[] data)
    {
        
        
         double[] normData = new double[data.length];
        for (int j = 0; j < data.length; j++)
        {
            if (stdDev[j] != 0)
            {
                normData[j] = (data[j] - mean[j]) / stdDev[j];
            }
            else
            {
                normData[j] = data[j];
            }
        }
        

        return normData;
    }
    
    private void Init(AssetManager am)
    {

		

			try
			{
//				ArrayList<DD_Hyposthesis> hAll = new ArrayList<DD_Hyposthesis>();
//				File f = Environment.getExternalStorageDirectory();
//
//				String fullfilename = f.getAbsoluteFile() + "/Android/data/info.androidhive.materialtabs.QOL/Sound/" ;
//				File folderToRead = new File(fullfilename);
//				//String t = sensorID.replace(':', '_');
//				File fileToRead = new File(fullfilename,"Normalize.txt");
//				FileInputStream fis = new FileInputStream(fileToRead);
//				InputStreamReader isw = new InputStreamReader(fis);

				InputStream is = am.open("NORMALIZE.txt");
				Scanner scanner = new Scanner(is,"UTF-8");

				String maxLine = scanner.nextLine();
				String[] maxLineSplit = maxLine.split(",");
				String minLine = scanner.nextLine();
				String[] minLineSplit = minLine.split(",");

				String avgLine = scanner.nextLine();
				String[] avgLineSplit = avgLine.split(",");
				String varLine = scanner.nextLine();
				String[] varLineSplit = varLine.split(",");

				max = new double[maxLineSplit.length];
				for(int i=0;i<max.length;i++)
				{
					max[i] = Double.parseDouble(maxLineSplit[i]);
				}

				min = new double[minLineSplit.length];
				for(int i=0;i<min.length;i++)
				{
					min[i] = Double.parseDouble(minLineSplit[i]);
				}
				
				mean = new double[avgLineSplit.length];
				for(int i=0;i<mean.length;i++)
				{
					mean[i] = Double.parseDouble(avgLineSplit[i]);
				}
				
				stdDev = new double[varLineSplit.length];
				for(int i=0;i<stdDev.length;i++)
				{
					stdDev[i] = Double.parseDouble(varLineSplit[i]);
				}
				
			}
			catch(IOException e)
			{
				
			}

		
    }
}
