package com.csri.ami.health_u.dataManagement.record.motion;


/**
 * Data Structure for storage of 3d vector
 *
 * @author Daniel Kelly
 * @version 1.0
 * @since 2014-06-1
 */
public class Vector3D {

	public double X=0;
	public double Y=0;
	public double Z=0;
	public double Horizontal_Magnitude =0;
	public double Vertical_Magnitude =0;
	public double Magnitude =0;
	public double LowPass_Mag=0;
	public double BandPass_Mag = 0;
	public double MilliTimeStamp;
	public long UTMDateTime;
	public long FrameNumber = 0;
	public boolean SetToSlowMode = false;
	public Vector3D()
	{
	
	}
	
	public Vector3D(double x, double y, double z)
	{
		X= x;Y = y;Z = z;
		Update();
	}
	
	public float[] Vector()
	{
		float[] vec = new float[3];
		vec[0] = (float)X;
		vec[1] = (float)Y;
		vec[2] = (float)Z;
		
		return vec;
		
	}
	
	public double[] VectorD()
	{
		double[] vec = new double[3];
		vec[0] = X;
		vec[1] = Y;
		vec[2] = Z;
		
		return vec;
		
	}
	
	public void Update()
	{
		Magnitude = Math.sqrt(Math.pow(X, 2) + Math.pow(Y, 2) + Math.pow(Z, 2));
		Horizontal_Magnitude = Math.sqrt(Math.pow(X, 2) + Math.pow(Y, 2));
		Vertical_Magnitude = Z;
	}
	
	public Vector3D Clone()
	{
		Vector3D c = new Vector3D();
		c.FrameNumber = FrameNumber;
		c.X = X;
		c.Y = Y;
		c.Z = Z;
		c.Magnitude = Magnitude;
		c.Vertical_Magnitude = Vertical_Magnitude;
		c.Horizontal_Magnitude = Horizontal_Magnitude;
		c.MilliTimeStamp = MilliTimeStamp;
		c.UTMDateTime = UTMDateTime;
		c.LowPass_Mag = LowPass_Mag;
		c.BandPass_Mag = BandPass_Mag;
		c.SetToSlowMode = SetToSlowMode;
		
		return c;
	}
	
	public String GyroXString()
	{
		StringBuffer bufferX = new StringBuffer(String.valueOf(X));
		bufferX = bufferX.deleteCharAt(bufferX.length() - 1);
		return bufferX.toString();
	}
	
	public String GyroYString()
	{
		StringBuffer bufferY = new StringBuffer(String.valueOf(Y));
		bufferY = bufferY.deleteCharAt(bufferY.length() - 1);
		return bufferY.toString();
	}
	
	public String GyroZString()
	{
		StringBuffer bufferZ = new StringBuffer(String.valueOf(Z));
		bufferZ = bufferZ.deleteCharAt(bufferZ.length() - 1);
		return bufferZ.toString();
	}
	
	public String ToGyroString()
	{
		return MilliTimeStamp + "," + UTMDateTime + "," +  GyroXString() + "," + GyroYString() + "," + GyroZString() + "\n";
	}
	
	public String toString()
	{
		return MilliTimeStamp + "," + UTMDateTime + "," +  X + "," + Y + "," + Z + "\n";
	}
}
