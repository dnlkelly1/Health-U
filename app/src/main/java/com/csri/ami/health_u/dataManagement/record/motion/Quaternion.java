package com.csri.ami.health_u.dataManagement.record.motion;

import com.csri.ami.health_u.dataManagement.record.Vector;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * Data Structure for storage of Quaternion
 *
 * @author Daniel Kelly
 * @version 1.0
 * @since 2014-06-1
 */
public class Quaternion 
{
	public double qx;
    public double qy;
    public double qz;
    public double qw;
    
    public double timeStamp;
    public Quaternion()
    {
        qx = qy = qz = qw=0;

    }


    public Quaternion(double w,double x,double y,double z)
    {
        qx = x; qy = y; qz = z; qw = w;

    }

    public Quaternion(double heading, double attitude, double bank)
    {
        // Assuming the angles are in radians.
        double c1 = Math.cos(heading/2);
        double s1 = Math.sin(heading/2);
        double c2 = Math.cos(attitude/2);
        double s2 = Math.sin(attitude/2);
        double c3 = Math.cos(bank/2);
        double s3 = Math.sin(bank/2);
        double c1c2 = c1*c2;
        double s1s2 = s1*s2;
        qw =c1c2*c3 - s1s2*s3;
	        qx =c1c2*s3 + s1s2*c3;
        qy =s1*c2*c3 + c1*s2*s3;
        qz =c1*s2*c3 - s1*c2*s3;
    }
    
    public Quaternion(Vector3D v)
    {
        double pitch = v.X;
        double roll = v.Y;
        double yaw = 0;// v.Z;
            // Assuming the angles are in radians.
            double c1 = Math.cos(yaw/2);
            double s1 = Math.sin(yaw/2);
            double c2 = Math.cos(pitch/2);
            double s2 = Math.sin(pitch/2);
            double c3 = Math.cos(roll/2);
            double s3 = Math.sin(roll/2);
            double c1c2 = c1*c2;
            double s1s2 = s1*s2;
            qw =c1c2*c3 - s1s2*s3;
	            qx =c1c2*s3 + s1s2*c3;
            qy =s1*c2*c3 + c1*s2*s3;
            qz =c1*s2*c3 - s1*c2*s3;
    }


    public Quaternion(Vector v)
    {
        double pitch = v.X;
        double roll = v.Y;
        double yaw = 0;// v.Z;
            // Assuming the angles are in radians.
            double c1 = Math.cos(yaw/2);
            double s1 = Math.sin(yaw/2);
            double c2 = Math.cos(pitch/2);
            double s2 = Math.sin(pitch/2);
            double c3 = Math.cos(roll/2);
            double s3 = Math.sin(roll/2);
            double c1c2 = c1*c2;
            double s1s2 = s1*s2;
            qw =c1c2*c3 - s1s2*s3;
	            qx =c1c2*s3 + s1s2*c3;
            qy =s1*c2*c3 + c1*s2*s3;
            qz =c1*s2*c3 - s1*c2*s3;
    }


    public Quaternion(RotationMatrix matrix)
    {

        float m00 = (float)matrix.X_x;
        float m01 = (float)matrix.X_y;
        float m02 = (float)matrix.X_z;

        float m10 = (float)matrix.Y_x;
        float m11 = (float)matrix.Y_y;
        float m12 = (float)matrix.Y_z;

        float m20 = (float)matrix.Z_x;
        float m21 = (float)matrix.Z_y;
        float m22 = (float)matrix.Z_z;

        float tr = m00 + m11 + m22;

        if (tr > 0) { 
          float S = (float)Math.sqrt((double)tr+1.0) * 2; // S=4*qw 
          qw = 0.25 * S;
          qx = (m21 - m12) / S;
          qy = (m02 - m20) / S; 
          qz = (m10 - m01) / S; 
        } else if ((m00 > m11)&(m00 > m22)) {
            float S = (float)Math.sqrt(1.0 + m00 - m11 - m22) * 2; // S=4*qx 
          qw = (m21 - m12) / S;
          qx = 0.25 * S;
          qy = (m01 + m10) / S; 
          qz = (m02 + m20) / S; 
        } else if (m11 > m22) {
            float S = (float)Math.sqrt(1.0 + m11 - m00 - m22) * 2; // S=4*qy
          qw = (m02 - m20) / S;
          qx = (m01 + m10) / S; 
          qy = 0.25 * S;
          qz = (m12 + m21) / S; 
        } else {
            float S = (float)Math.sqrt(1.0 + m22 - m00 - m11) * 2; // S=4*qz
          qw = (m10 - m01) / S;
          qx = (m02 + m20) / S;
          qy = (m12 + m21) / S;
          qz = 0.25 * S;
        }
    }

    public double Angle()
    {
        if ((qx * qx) + (qy * qy) + (qz * qz) < 0.000000000000001)
        {
            return 0;
        }
        else
        {
            return 2 * Math.acos(qw);
        }
    }

    public Quaternion Difference(Quaternion r)
    {
        Quaternion l_inv = this.Invert();
        Quaternion diff = r.Multiply(l_inv);
        return diff;
    }

    public Quaternion Multiply(Quaternion r)
    {
        Quaternion l = this;
        Quaternion mult = new Quaternion();
        mult.qx = (r.qx * l.qw + l.qx * r.qw + r.qy * l.qz) - (r.qz * l.qy);
        mult.qy = (r.qy * l.qw + l.qy * r.qw + r.qz * l.qx) - (r.qx * l.qz);
        mult.qz = (r.qz * l.qw + l.qz * r.qw + r.qx * l.qy) - (r.qy * l.qx);
        mult.qw = (r.qw * l.qw) - (r.qx * l.qx + r.qy * l.qy + r.qz * l.qz);
        return mult;
    }

    public Quaternion Invert()
    {
        double d = qw * qw + qx * qx + qy * qy + qz * qz;
        return new Quaternion(qw / d, -qx / d, -qy / d, -qz / d);
    }

    public Quaternion Norm()
    {
        double d = Math.sqrt((qw * qw) + (qx * qx) + (qy * qy) + (qz * qz));
        return new Quaternion(qw / d, qx / d, qy / d, qz / d);
    }
    


    public static Quaternion Average(Quaternion[] qs)
    {
        Matrix total = new Matrix(4, 4);

        for (int i = 0; i < qs.length; i++)
        {
            double[] qi_a = new double[] {qs[i].qz, qs[i].qy, qs[i].qx, qs[i].qw };
            Quaternion inv = qs[i].Invert();
            double[] qi_inv_a = new double[]{inv.qw,inv.qx,inv.qy,inv.qz};
            Matrix qi = new Matrix(qi_a, 1);
            Matrix qi_inv = new Matrix(qi_a, 4);

            Matrix t = qi.transpose().times(qi);
            total = total.plus(t);// += t;

        }
        double scale = (1 / (double)qs.length);
        total = total.times(scale);

        EigenvalueDecomposition e = total.eig();//.Eigen();
        Matrix ev = e.getV();

        double[][] arrayMat = ev.getArray();
        int max = MaxIndex(e.getRealEigenvalues());
        Quaternion avgQuaternion = new Quaternion( arrayMat[3][max],arrayMat[2][max], arrayMat[1][max], arrayMat[0][max]).Norm();

        return avgQuaternion;
    }
    
    public static Quaternion FromAccelerometer(Vector accel)
    {
    	Vector ref = new Vector(0,0,1);
    	Vector axis = accel.CrossProduct(ref);
    	double dp = accel.DotProduct(ref);
    	double angle = Math.acos(dp);
    	
    	Quaternion q = new Quaternion();
    	double s = Math.sin(angle/2);
    	q.qx = axis.X * s;
    	q.qy = axis.Y * s;
    	q.qz = axis.Z * s;
    	q.qw = Math.cos(angle/2);
    	
    	return q;
    	
    }

    public static int MaxIndex(double[] a)
    {
        double max = Double.MIN_VALUE;
        int maxindex = -1;
        for (int i = 0; i < a.length; i++)
        {
            if (a[i] > max)
            {
                max = a[i];
                maxindex = i;
            }
        }
        return maxindex;
    }
}
