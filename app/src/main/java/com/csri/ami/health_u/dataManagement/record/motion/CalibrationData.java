//Copyright (c) 2011, Daniel Kelly (Clarity Center, University College Dublin)
//All rights reserved.
//
//Redistribution and use in source and binary forms, with or without
//modification, are permitted provided that the following conditions are met:
//    * Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.
//    * Neither the name of the <organization> nor the
//      names of its contributors may be used to endorse or promote products
//      derived from this software without specific prior written permission.
//
//THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
//ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
//WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//DISCLAIMED. IN NO EVENT SHALL Daniel Kelly (Clarity Center, University College Dublin) BE LIABLE FOR ANY
//DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
//(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
//LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
//ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
//SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.csri.ami.health_u.dataManagement.record.motion;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Scanner;

/**
 * CalibrationData class handles storing and computation of motion sensor calibration values
 *
 * @author Daniel Kelly
 * @version 1.0
 * @since 2014-06-1
 */
public class CalibrationData 
{
	public static double GRAVITY = 9.81;
	public static double NEG_GRAVITY = -GRAVITY;
	public double SensorAxisDiff_x;//= Math.max(sensorMaxADC_x, sensorMinADC_x) - Math.min(sensorMaxADC_x, sensorMinADC_x);
	public double SensorAxisDiff_y;//= Math.max(sensorMaxADC_y, sensorMinADC_y) - Math.min(sensorMaxADC_y, sensorMinADC_y);
	public double SensorAxisDiff_z;//= Math.max(sensorMaxADC_z, sensorMinADC_z) - Math.min(sensorMaxADC_z, sensorMinADC_z);
	
	public double AxisDiff;// = sensorMax_g - sensorMin_g;
    public double interval_x;// = AxisDiff / SensorAxisDiff_x;
    public double interval_y;// = AxisDiff / SensorAxisDiff_y;
	public double interval_z;// = AxisDiff / SensorAxisDiff_z;
	
	public double xMagMax = GRAVITY;
	public double xMagMin = -GRAVITY;
	public double yMagMax = GRAVITY;
	public double yMagMin = -GRAVITY;
	public double zMagMax = GRAVITY;
	public double zMagMin = -GRAVITY;
	
	
	
	public CalibrationData()
	{
		Update();
	}
	
	public void Update()
	{
		AxisDiff = GRAVITY - NEG_GRAVITY;
		SensorAxisDiff_x = Math.max(xMagMax, xMagMin) - Math.min(xMagMax, xMagMin);
		SensorAxisDiff_y = Math.max(yMagMax, yMagMin) - Math.min(yMagMax, yMagMin);
		SensorAxisDiff_z = Math.max(zMagMax, zMagMin) - Math.min(zMagMax, zMagMin);
		interval_x = AxisDiff / SensorAxisDiff_x;
		interval_y = AxisDiff / SensorAxisDiff_y;
		interval_z = AxisDiff / SensorAxisDiff_z;

	}
	
	public Vector3D Calibrate(Vector3D raw)
	{
		Vector3D cVec = raw.Clone();
		
		cVec.X =  (((raw.X - Math.min(xMagMax, xMagMin)) * interval_x) + NEG_GRAVITY);
		cVec.Y =  (((raw.Y - Math.min(yMagMax, yMagMin)) * interval_y) + NEG_GRAVITY);
		cVec.Z =  (((raw.Z - Math.min(zMagMax, zMagMin)) * interval_z) + NEG_GRAVITY);
		
		cVec.Update();
		return cVec;
	}
	
	public void SetData(double SensorMax_x,double SensorMin_x,
			double SensorMax_y,double SensorMin_y,
			double SensorMax_z,double SensorMin_z)
	{
		xMagMax = SensorMax_x;
		xMagMin = SensorMax_y;
		yMagMax = SensorMax_z;
		yMagMin = SensorMin_x;
		zMagMax = SensorMin_y;
		zMagMin = SensorMin_z;
		//Update();
		
	}
	
	private double[] LoadCalibrationDataFromFile()
	{
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		if( mExternalStorageAvailable && mExternalStorageWriteable)
		{
			try
			{
				File f = Environment.getExternalStorageDirectory();
				
				String fullfilename = f.getAbsoluteFile() + "/Android/data/info.androidhive.materialtabs.QOL/" ;
				File folderToRead = new File(fullfilename);
				//String t = sensorID.replace(':', '_');
				File fileToRead = new File(fullfilename,"CalibAccel.txt");
				FileInputStream fis = new FileInputStream(fileToRead);
				InputStreamReader isw = new InputStreamReader(fis); 
				Scanner scanner = new Scanner(fileToRead,"UTF-8");
				
				String line = scanner.nextLine();
				
				String[] dataStrings = line.split(",");
				double[] data = new double[dataStrings.length];
				for(int i=0;i<dataStrings.length;i++)
				{
					data[i] = Double.parseDouble(dataStrings[i]);
				}
				return data;
			}
			catch(IOException e)
			{
				return null;
			}
		}
		else 
		{
			return null;
		}
	}
	
	
	public void LoadCalibrationValues()
	{
		double[] data = LoadCalibrationDataFromFile();
		if(data != null)
		{
			xMagMax = data[0];yMagMax = data[1];zMagMax = data[2];
			xMagMin = data[3];yMagMin = data[4];zMagMin = data[5];
		//xZeroRate = data[6];yZeroRate = data[7];zZeroRate = data[8];
			Update();
			
		}
		
	}
	
	
	public void SaveCalibrationData()
	{
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		if( mExternalStorageAvailable && mExternalStorageWriteable)
		{
			try
			{
			File f = Environment.getExternalStorageDirectory();
			
			String fullfilename = f.getAbsoluteFile() + "/Android/data/info.androidhive.materialtabs.QOL/" ;
			File folderToWrite = new File(fullfilename);
			folderToWrite.mkdirs();
			//String t = sensorID.replace(':', '_');
			File fileToWrite = new File(fullfilename, "CalibAccel.txt");
			FileOutputStream fos = new FileOutputStream(fileToWrite);
			OutputStreamWriter osw = new OutputStreamWriter(fos); 
			
			
			//osw.write("Example Text in a file in the "+f.getAbsolutePath()+" dir");
			osw.write(Double.toString(xMagMax) + "," + Double.toString(yMagMax) + "," + Double.toString(zMagMax) + "," + 
					  Double.toString(xMagMin) + "," + Double.toString(yMagMin) + "," + Double.toString(zMagMin) + ",");
			
			
			
			
			osw.close();
			
			}
			catch(IOException e)
			{
				
			}
		}
		
	}
}
