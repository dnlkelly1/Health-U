package com.csri.ami.health_u.dataManagement.record;

public class Vector
{
	 public double X;
     public double Y;
     public double Z;
     public double Magnitude;
     public double LowPass_Magnitude;
     public double Bandpass_Magnitude;
     public double Horizontal_Magnitude;
     public double Vertical_Magnitude;
     public Vector()
     {
         X = Y = Z = 0;

     }


     
     public Vector(double x, double y, double z)
     {
         X = x; Y = y; Z = z;
         Magnitude = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
         Horizontal_Magnitude = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
         Vertical_Magnitude = z;
     }
     
     public Vector CrossProduct(Vector b)
     {
    	 Vector c = new Vector();
    	 c.X = (Y * b.Z) - (Z * b.Y);
    	 c.Y = (Z * b.X) - (X * b.Z);
    	 c.Z = (X * b.Y) - (Y * b.X);
    	 return c;
     }
     
     public double DotProduct(Vector b)
     {
    	 return (X * b.X) + (Y * b.Y) + (Z * b.Z);
     }

     public void Update()
     {
         Magnitude = Math.sqrt(Math.pow(X, 2) + Math.pow(Y, 2) + Math.pow(Z, 2));
         Horizontal_Magnitude = Math.sqrt(Math.pow(X, 2) + Math.pow(Y, 2));
         Vertical_Magnitude = Z;
     }
}
