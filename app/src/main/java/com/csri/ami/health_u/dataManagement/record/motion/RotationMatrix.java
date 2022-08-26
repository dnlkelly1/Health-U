package com.csri.ami.health_u.dataManagement.record.motion;

import com.csri.ami.health_u.dataManagement.record.Vector;

import Jama.Matrix;

/**
 * Data Structure for storage of Rotation Matrix
 *
 * @author Daniel Kelly
 * @version 1.0
 * @since 2014-06-1
 */
public class RotationMatrix 
{
	 public double X_x;
     public double X_y;
     public double X_z;
     public double Y_x;
     public double Y_y;
     public double Y_z;
     public double Z_x;
     public double Z_y;
     public double Z_z;
     
     public Matrix R;
     
     
     public RotationMatrix(double xx,double xy,double xz,double yx,double yy,double yz,double zx,double zy,double zz)
     {
         X_x = xx; X_y = xy; X_z = xz;
         Y_x = yx; Y_y = yy; Y_z = yz;
         Z_x = zx; Z_y = zy; Z_z = zz;
     }
     
     public RotationMatrix(Vector3D euler)
     {
    	 
    	
    	 double pitch = euler.X;
         double roll = euler.Y;
         double yaw = euler.Z;

         double temp = pitch;
         if (pitch > Math.PI)
         {
             double diff = (Math.PI) - pitch;
             //Log.v("roll check","roll:" + pitchangle + " PI/2 - pitch=" + diff);
             temp = (float)((Math.PI) + diff);
         }
         else if (pitch < -(Math.PI))
         {
             double diff = -(Math.PI) - pitch;
             //Log.v("roll check","roll:" + pitchangle + " PI/2 - pitch=" + diff);
             temp = (float)(-(Math.PI) + diff);
         }
         double[][] r_x_d = new double[3][];
         r_x_d[0] = new double[] { 1, 0, 0 };
         r_x_d[1] = new double[] { 0, Math.cos(pitch), -Math.sin(pitch) };
         r_x_d[2] = new double[] { 0, Math.sin(pitch), Math.cos(pitch) };
         Matrix r_x = new Matrix(r_x_d);

         double[][] r_y_d = new double[3][];
         r_y_d[0] = new double[] { Math.cos(roll), 0, Math.sin(roll) };
         r_y_d[1] = new double[] { 0, 1, 0 };
         r_y_d[2] = new double[] { -Math.sin(roll), 0, Math.cos(roll) };
         Matrix r_y = new Matrix(r_y_d);

         double[][] r_z_d = new double[3][];
         r_z_d[0] = new double[] { Math.cos(yaw), -Math.sin(yaw), 0 };
         r_z_d[1] = new double[] { Math.sin(yaw), Math.cos(yaw), 0 };
         r_z_d[2] = new double[] { 0, 0, 1 };
         Matrix r_z = new Matrix(r_z_d);

         Matrix r_xy = r_y.times(r_x);
         //	Matrix r_xyz = r_xy.times(r_z);
         R = r_xy;

         //	int rows = r_x.getRowDimension();
         //	int cols = r_x.getColumnDimension();

         X_x = r_xy.get(0,0);
         X_y = r_xy.get(0, 1);
         X_z = r_xy.get(0, 2);

         Y_x = r_xy.get(1, 0);
         Y_y = r_xy.get(1, 1);
         Y_z = r_xy.get(1, 2);

         Z_x = r_xy.get(2, 0);
         Z_y = r_xy.get(2, 1);
         Z_z = r_xy.get(2, 2);


         //return r_xy;
     }

     public RotationMatrix(Vector euler)
     {
         double pitch = euler.X;
         double roll = euler.Y;
         double yaw = euler.Z;

         double temp = pitch;
         if (pitch > Math.PI)
         {
             double diff = (Math.PI) - pitch;
             //Log.v("roll check","roll:" + pitchangle + " PI/2 - pitch=" + diff);
             temp = (float)((Math.PI) + diff);
         }
         else if (pitch < -(Math.PI))
         {
             double diff = -(Math.PI) - pitch;
             //Log.v("roll check","roll:" + pitchangle + " PI/2 - pitch=" + diff);
             temp = (float)(-(Math.PI) + diff);
         }
         double[][] r_x_d = new double[3][];
         r_x_d[0] = new double[] { 1, 0, 0 };
         r_x_d[1] = new double[] { 0, Math.cos(pitch), -Math.sin(pitch) };
         r_x_d[2] = new double[] { 0, Math.sin(pitch), Math.cos(pitch) };
         Matrix r_x = new Matrix(r_x_d);

         double[][] r_y_d = new double[3][];
         r_y_d[0] = new double[] { Math.cos(roll), 0, Math.sin(roll) };
         r_y_d[1] = new double[] { 0, 1, 0 };
         r_y_d[2] = new double[] { -Math.sin(roll), 0, Math.cos(roll) };
         Matrix r_y = new Matrix(r_y_d);

         double[][] r_z_d = new double[3][];
         r_z_d[0] = new double[] { Math.cos(yaw), -Math.sin(yaw), 0 };
         r_z_d[1] = new double[] { Math.sin(yaw), Math.cos(yaw), 0 };
         r_z_d[2] = new double[] { 0, 0, 1 };
         Matrix r_z = new Matrix(r_z_d);

         Matrix r_xy = r_y.times(r_x);
         //	Matrix r_xyz = r_xy.times(r_z);


         //	int rows = r_x.getRowDimension();
         //	int cols = r_x.getColumnDimension();

         X_x = r_xy.get(0,0);
         X_y = r_xy.get(0, 1);
         X_z = r_xy.get(0, 2);

         Y_x = r_xy.get(1, 0);
         Y_y = r_xy.get(1, 1);
         Y_z = r_xy.get(1, 2);

         Z_x = r_xy.get(2, 0);
         Z_y = r_xy.get(2, 1);
         Z_z = r_xy.get(2, 2);


         //return r_xy;
     }
     
     public RotationMatrix(double q0,double q1,double q2,double q3)
     {
    	 X_x = 1 - (2*((q2*q2) + (q3*q3)));
    	 X_y = 2*((q1 * q2) - (q0*q3));
    	 X_z = 2*((q0*q2) + (q1*q3));
    	 
    	 Y_x = 2*((q1*q2) + (q0*q3));
    	 Y_y = 1 - (2*((q1*q1) + (q3*q3)));
    	 Y_z = 2*((q2*q3)-(q0*q1));
    	 
    	 Z_x = 2*((q1*q3)-(q0*q2));
    	 Z_y = 2*((q0*q1) + (q2*q3));
    	 Z_z = 1 - (2*((q1*q1)+(q2*q2)));
    	 
    	 R = new Matrix(3,3);
    	 R.set(0, 0, X_x);
    	 R.set(0, 1, X_y);
    	 R.set(0, 2, X_z);
    	 
    	 R.set(1, 0, Y_x);
    	 R.set(1, 1, Y_y);
    	 R.set(1, 2, Y_z);
    	 
    	 R.set(2, 0, Z_x);
    	 R.set(2, 1, Z_y);
    	 R.set(2, 2, Z_z);
     }
     
     public RotationMatrix(double X,double Y,double Z)
     {
    	 
    	
    	 double pitch = X;
         double roll = Y;
         double yaw = Z;

         double temp = pitch;
         if (pitch > Math.PI)
         {
             double diff = (Math.PI) - pitch;
             //Log.v("roll check","roll:" + pitchangle + " PI/2 - pitch=" + diff);
             temp = (float)((Math.PI) + diff);
         }
         else if (pitch < -(Math.PI))
         {
             double diff = -(Math.PI) - pitch;
             //Log.v("roll check","roll:" + pitchangle + " PI/2 - pitch=" + diff);
             temp = (float)(-(Math.PI) + diff);
         }
         double[][] r_x_d = new double[3][];
         r_x_d[0] = new double[] { 1, 0, 0 };
         r_x_d[1] = new double[] { 0, Math.cos(pitch), -Math.sin(pitch) };
         r_x_d[2] = new double[] { 0, Math.sin(pitch), Math.cos(pitch) };
         Matrix r_x = new Matrix(r_x_d);

         double[][] r_y_d = new double[3][];
         r_y_d[0] = new double[] { Math.cos(roll), 0, Math.sin(roll) };
         r_y_d[1] = new double[] { 0, 1, 0 };
         r_y_d[2] = new double[] { -Math.sin(roll), 0, Math.cos(roll) };
         Matrix r_y = new Matrix(r_y_d);

         double[][] r_z_d = new double[3][];
         r_z_d[0] = new double[] { Math.cos(yaw), -Math.sin(yaw), 0 };
         r_z_d[1] = new double[] { Math.sin(yaw), Math.cos(yaw), 0 };
         r_z_d[2] = new double[] { 0, 0, 1 };
         Matrix r_z = new Matrix(r_z_d);

         Matrix r_xy = r_y.times(r_x);
         	Matrix r_xyz = r_xy.times(r_z);
         R = r_xyz;

         //	int rows = r_x.getRowDimension();
         //	int cols = r_x.getColumnDimension();

         X_x = r_xy.get(0,0);
         X_y = r_xy.get(0, 1);
         X_z = r_xy.get(0, 2);

         Y_x = r_xy.get(1, 0);
         Y_y = r_xy.get(1, 1);
         Y_z = r_xy.get(1, 2);

         Z_x = r_xy.get(2, 0);
         Z_y = r_xy.get(2, 1);
         Z_z = r_xy.get(2, 2);


         //return r_xy;
     }
}
