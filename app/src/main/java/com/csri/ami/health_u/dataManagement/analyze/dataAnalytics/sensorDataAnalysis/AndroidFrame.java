package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis;

import com.csri.ami.health_u.dataManagement.analyze.classifiers.Complex;
import com.csri.ami.health_u.dataManagement.analyze.classifiers.FFT_Features;
import com.csri.ami.health_u.dataManagement.analyze.classifiers.InterQuartile;
import com.csri.ami.health_u.dataManagement.record.motion.MadgwickAHRS;
import com.csri.ami.health_u.dataManagement.record.motion.Quaternion;
import com.csri.ami.health_u.dataManagement.record.motion.RotationMatrix;
import com.csri.ami.health_u.dataManagement.record.motion.Vector3D;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;

import Jama.Matrix;

/**
 * Data Structure to store motion sensor data
 * Created by daniel on 30/09/2015.
 */
public class AndroidFrame
{
    public double timestamp;

    public static boolean USE_MADGWICK = true;

    public static boolean UseOrientationIndependence = true;


    double Duration = 0;
    double timestamp_eventTime;
    public double EventTimeStamp()
    {
        return timestamp_eventTime;
    }

    public String Label;

    private Vector3D _accel_GF;
    public Vector3D Accel_GF()
    {
        return _accel_GF;
    }

    public double AccelMag()
    {
        return Accel_GF().Magnitude;
    }

    private Vector3D _gyro_GF;
    public Vector3D Gyro_GF()
    {
        return _gyro_GF;
    }

    private Vector3D _mag_GF;
    public Vector3D Magnetometer_GF()
    {
        return _mag_GF;
    }

    private Vector3D _Rotation2_GF;
    public Vector3D Rotation2_GF()
    {
        return _Rotation2_GF;
    }

    public RotationMatrix _Euler_matrix;
    public RotationMatrix Euler_Matrix()
    {
        return _Euler_matrix;
    }



    private Vector3D _accel_BF;
    public Vector3D Accel_BF()
    {

            if (UseOrientationIndependence)
            {
                return _accel_BF;
            }
            else
            {
                return _accel_GF;
            }

    }

    private Vector3D _gyro_BF;
    public Vector3D Gyro_BF()
    {

            if (UseOrientationIndependence)
            {
                return _gyro_BF;
            }
            else
            {
                return _gyro_GF;
            }

    }

    private Vector3D _mag_BF;
    public Vector3D Magnetometer_BF()
    {
        return _mag_BF;
    }

    public Quaternion PitchRoll_quaternion;
    public Quaternion Rotation_Quaternion;

    public boolean SetToSlow = false;

    public AndroidFrame()
    {
        _accel_BF = new Vector3D();
        _accel_GF = new Vector3D();
        _gyro_GF = new Vector3D();
        _mag_GF = new Vector3D();
        PitchRoll_quaternion = new Quaternion();
        Rotation_Quaternion = new Quaternion();
    }

    public AndroidFrame(String label, double[] sensorDataRaw, double timeoffset, MadgwickAHRS madgwick, double previouTime)
    {
        Label = label;

        timestamp_eventTime = (double)(sensorDataRaw[1] / 1000);
        timestamp = timestamp_eventTime;
        // double ms = sensorDataRaw[0] % 1;
        timestamp_eventTime = timeoffset + sensorDataRaw[0];

        int slow = (int)sensorDataRaw[2];
        if(slow == 1)
        {
            SetToSlow = true;
        }
        else
        {
            SetToSlow = false;
        }



        _accel_GF = new Vector3D(sensorDataRaw[3], sensorDataRaw[4], sensorDataRaw[5]);//All_Accel_UsedForSaving - list in Android Code
        if(!Double.isNaN(sensorDataRaw[6]) && !Double.isNaN(sensorDataRaw[7]) && !Double.isNaN(sensorDataRaw[8]) )
        {
            _gyro_GF = new Vector3D(sensorDataRaw[6], sensorDataRaw[7], sensorDataRaw[8]); //All_Gyro_UsedForSaving - list in Android Code
        }



        if(sensorDataRaw[9] != Double.NaN && sensorDataRaw[10] != Double.NaN && sensorDataRaw[11] != Double.NaN )
        {
            _Rotation2_GF = new Vector3D(sensorDataRaw[9], sensorDataRaw[10], sensorDataRaw[11]);//is from "All_Orientation" list in Android Code...uses different pitch calculation than one used to compute rotation matrix used to transform accel and gyro to BF
        }
        RotationMatrix r = new RotationMatrix(_Rotation2_GF);
        _Euler_matrix = r;
        Rotation_Quaternion = new Quaternion(r);// (_orientation);

        /////////////////////////IS NOT USED BUT KEPT FOR POSSIBLE EXPERIMENTS///////////////////
        PitchRoll_quaternion = new Quaternion();
        PitchRoll_quaternion.qx = _Rotation2_GF.X;// _mag_GF.X;
        PitchRoll_quaternion.qy = _Rotation2_GF.Y;// _mag_GF.Y;
        ////////////////////////////////////////////////////////////////////////////////////////


        if (!USE_MADGWICK)
        {
            ////////////pre transformed on phone//////////////////////////////////////////////////
            _accel_BF = new Vector3D(sensorDataRaw[12], sensorDataRaw[13], sensorDataRaw[14]); // All_AccelGlobal - list in Android Code

            _gyro_BF = new Vector3D(sensorDataRaw[15], sensorDataRaw[16], sensorDataRaw[17]); // All_GyroGlobal - list in Android Code
            //////////////////////////////////////////////////////////////////////////////////////
        }
        else if(_gyro_GF != null)
        {
            if (previouTime != 0)
            {
                double frameLength = timestamp_eventTime - previouTime;
                madgwick.SamplePeriod = (float)Math.abs(frameLength);// (float)(frameLength / (double)1000);
            }
            madgwick.Update((float)_gyro_GF.X, (float)_gyro_GF.Y, (float)_gyro_GF.Z, (float)_accel_GF.X, (float)_accel_GF.Y, (float)_accel_GF.Z);

            RotationMatrix r_m = new RotationMatrix(madgwick.Quaternion[0], madgwick.Quaternion[1], madgwick.Quaternion[2], madgwick.Quaternion[3]);
            Rotation_Quaternion = new Quaternion(madgwick.Quaternion[0], madgwick.Quaternion[1], madgwick.Quaternion[2], madgwick.Quaternion[3]);

            double[][] orArray = new double[3][];
            orArray[0] = new double[] { r_m.X_x, r_m.X_y, r_m.X_z };
            orArray[1] = new double[] { r_m.Y_x, r_m.Y_y, r_m.Y_z };
            orArray[2] = new double[] { r_m.Z_x, r_m.Z_y, r_m.Z_z };

            Matrix orientationMatrix = new Matrix(orArray);
            _Euler_matrix = r_m;

            double[][] accelArray = new double[1][];
            accelArray[0] = new double[] { _accel_GF.X, _accel_GF.Y, _accel_GF.Z };
            Matrix accelMatrix = new Matrix(accelArray);

            Matrix accel_BF = orientationMatrix.times(accelMatrix.transpose());
            _accel_BF = new Vector3D(Math.sqrt(Math.pow(accel_BF.get(0, 0), 2) + Math.pow(accel_BF.get(1, 0), 2)), 0, accel_BF.get(2, 0));

            double[][] gyroArray = new double[1][];
            gyroArray[0] = new double[] { _gyro_GF.X, _gyro_GF.Y, _gyro_GF.Z };
            Matrix gyroMatrix = new Matrix(gyroArray);

            Matrix gyro_BF = orientationMatrix.times(gyroMatrix.transpose());
            _gyro_BF = new Vector3D(gyro_BF.get(0, 0), gyro_BF.get(1, 0), gyro_BF.get(2, 0));
        }


    }

    public AndroidFrame(String label, double[] orienationDataRaw, double[] sensorDataRaw, MadgwickAHRS madgwick, double previousTime)
    {
        int orOffSet = 0;
        int sensOffset = 0;
        if (orienationDataRaw.length == 11)
        {
            orOffSet = 1;
        }
        if (sensorDataRaw.length == 14)
        {
            sensOffset = 1;
        }

        Label = label;
        //timestamp_eventTime = sensorDataRaw[0+sensOffset];
        timestamp_eventTime = (double)(sensorDataRaw[0]);
        // double ms = sensorDataRaw[0] % 1;



        _accel_GF = new Vector3D(sensorDataRaw[1 + sensOffset], sensorDataRaw[2 + sensOffset], sensorDataRaw[3 + sensOffset]);
        _gyro_GF = new Vector3D(sensorDataRaw[4 + sensOffset], sensorDataRaw[5 + sensOffset], sensorDataRaw[6 + sensOffset]);
        _mag_GF = new Vector3D(sensorDataRaw[7 + sensOffset], sensorDataRaw[8 + sensOffset], sensorDataRaw[9 + sensOffset]);
        _Rotation2_GF = new Vector3D(sensorDataRaw[10 + sensOffset], sensorDataRaw[11 + sensOffset], sensorDataRaw[12 + sensOffset]);
        //DataElement4 q1= new DataElement4(0,_mag_GF.Y,_mag_GF.X);

        _Euler_matrix = new RotationMatrix(orienationDataRaw[1 + orOffSet], orienationDataRaw[2 + orOffSet], orienationDataRaw[3 + orOffSet], orienationDataRaw[4 + orOffSet], orienationDataRaw[5 + orOffSet], orienationDataRaw[6 + orOffSet],
                orienationDataRaw[7 + orOffSet], orienationDataRaw[8 + orOffSet], orienationDataRaw[9 + orOffSet]);

        RotationMatrix r = new RotationMatrix(_Rotation2_GF);
        Rotation_Quaternion = new Quaternion(r);// (_orientation);
        PitchRoll_quaternion = new Quaternion();
        PitchRoll_quaternion.qx = _Rotation2_GF.X;// _mag_GF.X;
        PitchRoll_quaternion.qy = _Rotation2_GF.Y;// _mag_GF.Y;



        if (!USE_MADGWICK)
        {
            double[][] orArray = new double[3][];
            orArray[0] = new double[] { orienationDataRaw[1 + orOffSet], orienationDataRaw[2 + orOffSet], orienationDataRaw[3 + orOffSet] };
            orArray[1] = new double[] { orienationDataRaw[4 + orOffSet], orienationDataRaw[5 + orOffSet], orienationDataRaw[6 + orOffSet] };
            orArray[2] = new double[] { orienationDataRaw[7 + orOffSet], orienationDataRaw[8 + orOffSet], orienationDataRaw[9 + orOffSet] };

            //orArray[0] = new double[] { orienationDataRaw[1], orienationDataRaw[2], orienationDataRaw[3] };
            //orArray[1] = new double[] { orienationDataRaw[4], orienationDataRaw[5], orienationDataRaw[6] };
            //orArray[2] = new double[] { orienationDataRaw[7], orienationDataRaw[8], orienationDataRaw[9] };
            Matrix orientationMatrix = new Matrix(orArray);

            //_rotationQuaternion = new Quaternion(_orientation);

            // RotationMatrix testM = new RotationMatrix(_mag_GF);

            double[][] accelArray = new double[1][];
            accelArray[0] = new double[] { sensorDataRaw[1 + sensOffset], sensorDataRaw[2 + sensOffset], sensorDataRaw[3 + sensOffset] };
            Matrix accelMatrix = new Matrix(accelArray);

            Matrix accel_BF = orientationMatrix.times(accelMatrix.transpose());
            _accel_BF = new Vector3D(accel_BF.get(0, 0), accel_BF.get(1, 0), accel_BF.get(2, 0));


            double[][] gyroArray = new double[1][];
            gyroArray[0] = new double[] { sensorDataRaw[4 + sensOffset], sensorDataRaw[5 + sensOffset], sensorDataRaw[6 + sensOffset] };
            Matrix gyroMatrix = new Matrix(gyroArray);

            Matrix gyro_BF = orientationMatrix.times(gyroMatrix.transpose());
            _gyro_BF = new Vector3D(gyro_BF.get(0, 0), gyro_BF.get(1, 0), gyro_BF.get(2, 0));
        }
        else
        {
            if (previousTime != 0)
            {
                double frameLength = timestamp_eventTime - previousTime;
                madgwick.SamplePeriod = (float)Math.abs(frameLength);// (float)(frameLength / (double)1000);
            }
            madgwick.Update((float)_gyro_GF.X, (float)_gyro_GF.Y, (float)_gyro_GF.Z, (float)_accel_GF.X, (float)_accel_GF.Y, (float)_accel_GF.Z);

            RotationMatrix r_m = new RotationMatrix(madgwick.Quaternion[0], madgwick.Quaternion[1], madgwick.Quaternion[2], madgwick.Quaternion[3]);
            Rotation_Quaternion = new Quaternion(madgwick.Quaternion[0], madgwick.Quaternion[1], madgwick.Quaternion[2], madgwick.Quaternion[3]);

            double[][] orArray = new double[3][];
            orArray[0] = new double[] { r_m.X_x, r_m.X_y, r_m.X_z };
            orArray[1] = new double[] { r_m.Y_x, r_m.Y_y, r_m.Y_z };
            orArray[2] = new double[] { r_m.Z_x, r_m.Z_y, r_m.Z_z };

            Matrix orientationMatrix = new Matrix(orArray);
            _Euler_matrix = r_m;

            double[][] accelArray = new double[1][];
            accelArray[0] = new double[] { _accel_GF.X, _accel_GF.Y, _accel_GF.Z };
            Matrix accelMatrix = new Matrix(accelArray);

            Matrix accel_BF = orientationMatrix.times(accelMatrix.transpose());
            _accel_BF = new Vector3D(accel_BF.get(0, 0), accel_BF.get(1, 0), accel_BF.get(2, 0));

            double[][] gyroArray = new double[1][];
            gyroArray[0] = new double[] { _gyro_GF.X, _gyro_GF.Y, _gyro_GF.Z };
            Matrix gyroMatrix = new Matrix(gyroArray);

            Matrix gyro_BF = orientationMatrix.times(gyroMatrix.transpose());
            _gyro_BF = new Vector3D(gyro_BF.get(0, 0), gyro_BF.get(1, 0), gyro_BF.get(2, 0));
        }


    }

    public static double MIN_AccelNorm(AndroidFrame[] data)
    {
        double min = Double.MAX_VALUE;

        for (int i = 0; i < data.length; i++)
        {
            if(data[i].Accel_GF().Magnitude < min)
            {
                min = data[i].Accel_GF().Magnitude;
            }


        }
        return min;
    }

    public static double MAX_AccelNorm(AndroidFrame[] data)
    {
        double max = Double.MIN_VALUE;

        for (int i = 0; i < data.length; i++)
        {
            if(data[i].Accel_GF().Magnitude > max)
            {
                max = data[i].Accel_GF().Magnitude;
            }


        }
        return max;
    }



    public static double Skewness(AndroidFrame[] data, double mean, double stddev)
    {
        double sum = 0;
        for (int i = 0; i < data.length; i++)
        {
            sum += Math.pow((data[i].Accel_GF().Magnitude - mean), 3);
        }
        return sum / ((data.length - 1) * (Math.pow(stddev, 3)));
    }

    public static double Kurtosis(AndroidFrame[] data, double mean, double stddev)
    {
        double sum = 0;
        for (int i = 0; i < data.length; i++)
        {
            sum += Math.pow((data[i].Accel_GF().Magnitude - mean), 4);
        }
        return sum / ((data.length - 1) * (Math.pow(stddev, 4)));
    }

    public static double AVG_AccelNorm(AndroidFrame[] data)
    {
        double avg = 0;

        for (int i = 0; i < data.length; i++)
        {

            avg += (data[i].Accel_GF().Magnitude / (double)data.length);

        }
        return avg;
    }

    public static double AVG_AngelNorm(AndroidFrame[] data)
    {
        double avg = 0;

        for (int i = 0; i < data.length; i++)
        {

            avg += (data[i].Rotation_Quaternion.Angle() / (double)data.length);

        }
        return avg;
    }

    public static double Var_AccelNorm(AndroidFrame[] data, double avg)
    {
        double var = 0;

        for (int i = 0; i < data.length; i++)
        {

            var += Math.pow(data[i].Accel_GF().Magnitude - avg, 2);

        }
        var = Math.sqrt(var / data.length);
        return var;
    }

    public static double Avg_Gyro_BF(AndroidFrame[] data)
    {
        if(data[0].Gyro_BF() != null) {
            double avg = 0;
            double count=0;
            for (int i = 0; i < data.length; i++)
            {
                Vector3D g = data[i].Gyro_BF();
                if(g != null) {
                    avg += (g.Magnitude);
                    count++;
                }

            }
            avg = avg / count;
            return avg;
        }
        else
        {
            return Double.NaN;
        }
    }

    public static double Var_Gyro_BF(AndroidFrame[] data, double avg)
    {
        if(data[0].Gyro_BF() != null) {
            double var = 0;
            double count=0;
            for (int i = 0; i < data.length; i++)
            {
                if(data[i].Gyro_BF() != null) {
                    var += Math.pow(data[i].Gyro_BF().Magnitude - avg, 2);
                    count++;
                }

            }
            var = Math.sqrt(var / count);
            return var;
        }
        else {
            return Double.NaN;
        }
    }

    public static double Avg_AccelHorizontal_BF(AndroidFrame[] data)
    {
        if(data[0].Accel_BF() != null) {
            double avg = 0;
            double count=0;
            for (int i = 0; i < data.length; i++)
            {
                if(data[i].Accel_BF() != null) {
                    avg += (data[i].Accel_BF().Horizontal_Magnitude);
                    count++;
                }

            }
            return avg / count;
        }
        else
        {
            return Double.NaN;
        }
    }
    public static double Var_AccelHorizontal_BF(AndroidFrame[] data, double avg)
    {
        if(data[0].Accel_BF() != null) {
            double var = 0;
            double count=0;

            for (int i = 0; i < data.length; i++)
            {
                if(data[i].Accel_BF() != null)
                {
                    var += Math.pow(data[i].Accel_BF().Horizontal_Magnitude - avg, 2);
                    count++;
                }

            }
            var = Math.sqrt(var / count);
            return var;
        }
        else
        {
            return Double.NaN;
        }
    }

    public static double Avg_AccelVertical_BF(AndroidFrame[] data)
    {
        if(data[0].Accel_BF() != null) {
            double avg = 0;
            double count=0;

            for (int i = 0; i < data.length; i++)
            {
                if(data[i].Accel_BF() != null)
                {
                    avg += (data[i].Accel_BF().Vertical_Magnitude );
                    count++;
                }

            }
            avg = avg / count;
            return avg;
        }
        else
        {
            return Double.NaN;
        }
    }

    public static double Var_AccelVertical_BF(AndroidFrame[] data, double avg)
    {
        if(data[0].Accel_BF() != null) {
            double var = 0;
            double count=0;

            for (int i = 0; i < data.length; i++)
            {
                if(data[i].Accel_BF() != null)
                {
                    var += Math.pow(data[i].Accel_BF().Vertical_Magnitude - avg, 2);
                    count++;
                }

            }
            var = Math.sqrt(var / count);
            return var;
        }
        else
        {
            return Double.NaN;
        }
    }

    public static double Avg_GyroHorizontal_BF(AndroidFrame[] data)
    {
        if(data[0].Gyro_BF() != null) {
            double avg = 0;

            double count=0;
            for (int i = 0; i < data.length; i++)
            {
                if(data[i].Gyro_BF() != null) {
                    avg += (data[i].Gyro_BF().Horizontal_Magnitude);
                    count++;
                }


            }
            avg = avg / count;
            return avg;
        }
        else
        {
            return Double.NaN;
        }
    }

    public static double Var_GyroHorizontal_BF(AndroidFrame[] data, double avg)
    {
        if(data[0].Gyro_BF() != null) {
            double var = 0;

            double count =0;
            for (int i = 0; i < data.length; i++)
            {
                if(data[i].Gyro_BF() != null) {
                    var += Math.pow(data[i].Gyro_BF().Horizontal_Magnitude - avg, 2);
                    count++;
                }
            }
            var = Math.sqrt(var / count);
            return var;
        }
        else
        {
            return Double.NaN;
        }
    }

    public static double Avg_GyroVertical_BF(AndroidFrame[] data)
    {
        if(data[0].Gyro_BF() != null) {
            double avg = 0;
            double count=0;
            for (int i = 0; i < data.length; i++)
            {
                if(data[i].Gyro_BF() != null) {
                    avg += (data[i].Gyro_BF().Vertical_Magnitude / (double) data.length);
                    count++;
                }

            }
            avg = avg / count;
            return avg;
        }
        else
        {
            return Double.NaN;
        }
    }

    public static double Var_GyroVertical_BF(AndroidFrame[] data, double avg)
    {
        if(data[0].Gyro_BF() != null) {
            double var = 0;
            double count=0;
            for (int i = 0; i < data.length; i++)
            {
                if(data[i].Gyro_BF() != null) {
                    var += Math.pow(data[i].Gyro_BF().Vertical_Magnitude - avg, 2);
                    count++;
                }
            }
            var = Math.sqrt(var / count);
            return var;
        }
        else
        {
            return Double.NaN;
        }
    }

    public static double CORR_AccelVertBF_AccelNorm(AndroidFrame[] data)
    {
        if(data[0].Accel_BF() != null) {
            //AForge.Math.Metrics.PearsonCorrelation cor = new AForge.Math.Metrics.PearsonCorrelation();


            ArrayList<Double> accelvertBF = new ArrayList<Double>();

            ArrayList<Double>  accelNorm  = new ArrayList<Double>();
            //double[] accelvertBF = new double[data.length];
            //double[] accelNorm = new double[data.length];

            for (int i = 0; i < data.length; i++)
            {
                if(data[i].Accel_BF() != null)
                {
                    accelvertBF.add( data[i].Accel_BF().Vertical_Magnitude );
                    accelNorm.add( data[i].Accel_GF().Magnitude );
                }
            }

            double[] a = new double[accelvertBF.size()];
            double[] b = new double[accelNorm.size()];

            for(int i=0;i<accelvertBF.size();i++)
            {
                a[i] = accelvertBF.get(i);
                b[i] = accelNorm.get(i);
            }

            PearsonsCorrelation cor = new PearsonsCorrelation();
            return cor.correlation(a,b);
            //return cor.GetSimilarityScore(accelvertBF, accelNorm);
        }
        else
        {
            return Double.NaN;
        }
    }

    public static double CORR_AccelVertical_AccelHorizontal(AndroidFrame[] data)
    {
        if(data[0].Accel_BF() != null) {
            PearsonsCorrelation cor = new PearsonsCorrelation();

            ArrayList<Double> accelvertBF = new ArrayList<Double>();

            ArrayList<Double>  accelH  = new ArrayList<Double>();

            for (int i = 0; i < data.length; i++)
            {
                if(data[i].Accel_BF() != null)
                {
                    accelvertBF.add(data[i].Accel_BF().Vertical_Magnitude);
                    accelH.add( data[i].Accel_BF().Horizontal_Magnitude);
                }
            }

            double[] a = new double[accelvertBF.size()];
            double[] b = new double[accelH.size()];

            for(int i=0;i<accelvertBF.size();i++)
            {
                a[i] = accelvertBF.get(i);
                b[i] = accelH.get(i);
            }

            return cor.correlation(a,b);
        }
        else
        {
            return Double.NaN;
        }
    }

    public static double IRQ_AccelNorm(AndroidFrame[] data)
    {

        ArrayList<Double> rawData = new ArrayList<Double>();
        for (int i = 0; i < data.length; i++)
        {
            rawData.add(data[i].Accel_GF().Magnitude);
        }
        double iqr = InterQuartile.InterQuartileRange(rawData);


        return iqr;
    }

    public static double IRQ_GyroVertical_BF(AndroidFrame[] data)
    {


        ArrayList<Double> rawData = new ArrayList<Double>();
        for (int i = 0; i < data.length; i++)
        {
            if(data[i].Gyro_BF() != null)
            {
                rawData.add(data[i].Gyro_BF().Vertical_Magnitude);
            }
        }
        double iqr=0;
        if(rawData.size() > 0)
        {
            iqr = InterQuartile.InterQuartileRange(rawData);
        }
        else
        {
            iqr = Double.NaN;
        }


        return iqr;
    }

    public static double IRQ_GyroHorizontal_BF(AndroidFrame[] data)
    {
        ArrayList<Double> rawData = new ArrayList<Double>();
        for (int i = 0; i < data.length; i++)
        {
            if(data[i].Gyro_BF() != null)
            {
                rawData.add(data[i].Gyro_BF().Horizontal_Magnitude);
            }
        }
        double iqr=0;
        if(rawData.size() > 0)
        {
            iqr = InterQuartile.InterQuartileRange(rawData);
        }
        else
        {
            iqr = Double.NaN;
        }


        return iqr;
    }

    public static double Direction_AccelVertical_GF(AndroidFrame[] data, double fps, int window)
    {
        if(data[0].Accel_BF() != null) {
            Integer[] crossing = ZeroCrossings_AccelVert_BF(data);

            double max_abs = Double.MIN_VALUE;
            double max = 0;
            int bestIndex = -1;

            //int window = (int)(fps / (double)2);
            for (int i = 0; i < crossing.length; i++) {
                double currentVal = RateOfChange_AccelVert_BF(crossing[i], window, data);
                if (Math.abs(currentVal) > max_abs) {
                    max_abs = Math.abs(currentVal);
                    max = currentVal;
                    bestIndex = i;
                }
            }

            return max;
        }
        else
        {
            return Double.NaN;
        }
    }

    public static double RateOfChange_AccelVert_BF(int index, int window, AndroidFrame[] data)
    {
        if(data[0].Accel_BF() != null) {


            int left_start = index - window;
            if (left_start < 0) {
                left_start = 0;
            }
            int left_end = index;

            int right_start = index;

            int right_end = index + (window / 2);
            if (right_end >= data.length) {
                right_end = data.length - 1;
            }

            double avgLeft = 0;
            double left_len = left_end - left_start;

            double leftCount=0;
            for (int i = left_start; i < left_end; i++)
            {
                if(data[i].Accel_BF() != null)
                {
                    avgLeft += data[i]._accel_BF.Vertical_Magnitude;
                    leftCount++;
                }
            }
            avgLeft = avgLeft / leftCount;

            double avgRight = 0;
            double right_len = right_end - right_start;
            double rightCount=0;
            for (int i = right_start; i < right_end; i++)
            {
                if(data[i].Accel_BF() != null)
                {
                    avgRight += data[i]._accel_BF.Vertical_Magnitude;
                    rightCount++;
                }
            }
            avgRight = avgRight / rightCount;

            return avgLeft - avgRight;
        }
        else
        {
            return Double.NaN;
        }
    }

    public static Integer[] ZeroCrossings_AccelVert_BF(AndroidFrame[] data)
    {

            ArrayList<Integer> crossings = new ArrayList<Integer>();

            double GRAVITY = 9.81;

            for (int i = 1; i < data.length; i++)
            {
                if(data[i].Accel_BF() !=null && data[i-1].Accel_BF() != null)
                {
                    if (data[i - 1]._accel_BF.Vertical_Magnitude < GRAVITY && data[i]._accel_BF.Vertical_Magnitude > GRAVITY) {
                        crossings.add(i);
                    } else if (data[i - 1]._accel_BF.Vertical_Magnitude > GRAVITY && data[i]._accel_BF.Vertical_Magnitude < GRAVITY) {
                        crossings.add(i);
                    }
                }
            }

            return crossings.toArray(new Integer[crossings.size()]);


    }

    public static double ATT_RotQuat_StandingDiff_RateOfChange(AndroidFrame[] data)
    {
        if(data[0].Rotation_Quaternion != null) {
            double diff = 0;
            Quaternion[] qs = new Quaternion[data.length - 1];
            for (int i = 1; i < data.length; i++)
            {

                qs[i - 1] = data[i].Rotation_Quaternion.Difference(data[i - 1].Rotation_Quaternion);
            }
            Quaternion avgDiff = Quaternion.Average(qs);
            double aq = avgDiff.Angle();
            return aq;// / (double)data.Length;
        }
        else
        {
            return Double.NaN;
        }
    }

    public static Complex[] GetComplexWindow(double[] data)
    {
        Complex[] window = new Complex[data.length];
        int count = 0;
        for (int i = 0; count < window.length && i < data.length; i++)
        {
            window[count] = new Complex(data[i],0);
            //window[count].Re = data[i];
            //window[count].Im = 0;
            count++;
        }
        return window;
    }

    private static double[] GetFFTMagnitude(Complex[] data)
    {
        double[] magA = new double[data.length];

        for (int i = 0; i < data.length; i++)
        {
            magA[i] = data[i].abs();
        }

        return magA;
    }

    public static double[] MFC_AccelArray(Double[] data)
    {
        double[] rawData = new double[data.length];

        for(int i=0;i<data.length;i++)
        {
            rawData[i] = data[i];
        }




        Complex[] windowC = GetComplexWindow(rawData);
        Complex[] fftRes = FFT_apache(rawData);
        //Complex[] fftRes = FFT.fft(windowC);
        //AForge.Math.FourierTransform.FFT(windowC, AForge.Math.FourierTransform.Direction.Forward);
        return GetFFTMagnitude(fftRes);
        //FFT_Features[] peaks = GetPeaks(windowC, numPeaks);
        //List<double> peaksAll = new List<double>();

        //for (int i = 0; i < peaks.Length; i++)
        //{
        //    peaksAll.Add(peaks[i].Frequency);
        //}
        //return peaksAll.ToArray();
    }

    public static double[] MFC_AccelArray(AndroidFrame[] data)
    {
        double[] rawData = new double[data.length];
        for (int i = 0; i < data.length; i++)
        {
            rawData[i] = data[i].Accel_GF().Magnitude;
        }



        Complex[] windowC = GetComplexWindow(rawData);
        Complex[] fftRes = FFT_apache(rawData);
        //Complex[] fftRes = FFT.fft(windowC);
        //AForge.Math.FourierTransform.FFT(windowC, AForge.Math.FourierTransform.Direction.Forward);
        return GetFFTMagnitude(fftRes);
        //FFT_Features[] peaks = GetPeaks(windowC, numPeaks);
        //List<double> peaksAll = new List<double>();

        //for (int i = 0; i < peaks.Length; i++)
        //{
        //    peaksAll.Add(peaks[i].Frequency);
        //}
        //return peaksAll.ToArray();
    }

    public static Complex[] FFT_apache(double[] data)
    {
        double avg =0;
        for(int i=0;i<data.length;i++)
        {
            avg += data[i];
        }
        avg = avg / (double)data.length;

        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        org.apache.commons.math3.complex.Complex[] complex =  transformer.transform(data, TransformType.FORWARD);


        Complex[] results = new Complex[complex.length];

        double scale = avg / complex[0].abs();

        for(int i=0;i<results.length;i++)
        {
            results[i] = new Complex(complex[i].getReal() * scale,complex[i].getImaginary() * scale);
        }

        return results;
    }


    public static double[] NormalizeLength(double[] data, int length)
    {
        double[] data_new = new double[length];

        double scale = (double)length / (double)data.length;

        if (scale > 1)//stretch out data
        {
            int s = (int)scale;
            data_new[0] = data[0];
            for (int i = 1; i < data.length; i ++)
            {
                for (int j = 0; (((i - 1) * s) + 1) + j < data_new.length; j++)
                {
                    int newIndex = (((i-1)*s)+1) + j;
                    data_new[newIndex] = data[i];
                }
            }
        }
        else//shrink data
        {
            double s = data.length / length;
            data_new[0] = data[0];
            for (int i = 1; i < data_new.length; i ++)
            {
                double avg = 0;
                for (int j = 0; j < s && i + j < data_new.length; j++)
                {
                    int index = (((i - 1) * (int)s) + 1) + j;
                    avg += data[index] / s;
                    //data_new[i + j] = data[i];
                }
                data_new[i] = avg;
            }

        }

        return data_new;
    }

    public static double IQR(Double[] data)
    {
        if (data.length > 1)
        {
            ArrayList<Double> dataA = new ArrayList<Double>();
            for(int i=0;i<data.length;i++)
            {
                dataA.add(data[i]);
            }
            double iqr = InterQuartile.InterQuartileRange(dataA);
           // StatDescriptive.Descriptive des = new StatDescriptive.Descriptive(data);
           // des.Analyze();

            return iqr;
        }
        else
        {
            return 0;
        }
    }

    public static FFT_Features[] GetPeaks(Complex[] data, int numberPeaks)
    {
        FFT_Features[] peaks = new FFT_Features[numberPeaks];

        for (int i = 0; i < numberPeaks; i++)
        {
            double max = Double.MIN_VALUE;
            int maxindex = -1;
            for (int j = 1; j < (data.length / 2) + 1; j++)
            {
                boolean contains = Contains(peaks, data[j].abs());
                if (data[j].abs() > max && !contains)
                {
                    if (j == 0 && data[j + 1].abs() <= data[j].abs())
                    {
                        max = data[j].abs();
                        maxindex = j;
                    }
                    else if (j == data.length - 1 && data[j - 1].abs() <= data[j].abs())
                    {
                        max = data[j].abs();
                        maxindex = j;
                    }
                    else if (data[j + 1].abs() <= data[j].abs() && data[j - 1].abs() <= data[j].abs())
                    {
                        max = data[j].abs();
                        maxindex = j;
                    }

                }
            }

            if(maxindex != -1)
            {
                peaks[i] = new FFT_Features();
                peaks[i].Mag = max;
                peaks[i].Index = maxindex;
                peaks[i].Frequency = ((double) maxindex - 1) / ((double) data.length - 1);
            }
            else
            {
                peaks[i] = new FFT_Features();
                peaks[i].Mag = 0;
                peaks[i].Index = maxindex;
                peaks[i].Frequency = 0;
            }
        }

        //peakIndices = indices;
        return peaks;
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

    public static AndroidFrame GetAverage(ArrayList<AndroidFrame> frames)
    {
        Quaternion[] qs = new Quaternion[frames.size()];

        for (int i = 0; i < qs.length; i++)
        {
            qs[i] = frames.get(i).Rotation_Quaternion;
        }

        Quaternion q = Quaternion.Average(qs);
        //Quaternion.InertiaMatrix(qs);

        AndroidFrame f = new AndroidFrame();
        double x_1 = 0, x_2 = 0, y_1 = 0, y_2 = 0, z_1 = 0, z_2 = 0;
        for (int i = 0; i < frames.size(); i++)
        {
            f.Accel_BF().X += frames.get(i).Accel_BF().X / (double)frames.size();
            f.Accel_BF().Y += frames.get(i).Accel_BF().Y / (double)frames.size();
            f.Accel_BF().Z += frames.get(i).Accel_BF().Z / (double)frames.size();

            f.Accel_GF().X += frames.get(i).Accel_GF().X / (double)frames.size();
            f.Accel_GF().Y += frames.get(i).Accel_GF().Y / (double)frames.size();
            f.Accel_GF().Z += frames.get(i).Accel_GF().Z / (double)frames.size();

            x_1 += Math.cos(frames.get(i).PitchRoll_quaternion.qx);// frames[i].Quaternion.qx / (double)frames.Count;
            x_2 += Math.sin(frames.get(i).PitchRoll_quaternion.qx);// frames[i].Quaternion.qx / (double)frames.Count;
            y_1 += Math.cos(frames.get(i).PitchRoll_quaternion.qy);// frames[i].Quaternion.qx / (double)frames.Count;
            y_2 += Math.sin(frames.get(i).PitchRoll_quaternion.qy);// frames[i].Quaternion.qx / (double)frames.Count;
            z_1 += Math.cos(frames.get(i).PitchRoll_quaternion.qz);// frames[i].Quaternion.qx / (double)frames.Count;
            z_2 += Math.sin(frames.get(i).PitchRoll_quaternion.qz);// frames[i].Quaternion.qx / (double)frames.Count;
        }
        f.Rotation_Quaternion = q;
        f.PitchRoll_quaternion.qx = Math.atan2(x_2, x_1);
        f.PitchRoll_quaternion.qy = Math.atan2(y_2, y_1);
        f.PitchRoll_quaternion.qz = Math.atan2(z_2, z_1);
        f.Accel_BF().Update();
        f.Accel_GF().Update();

        return f;
    }

}
