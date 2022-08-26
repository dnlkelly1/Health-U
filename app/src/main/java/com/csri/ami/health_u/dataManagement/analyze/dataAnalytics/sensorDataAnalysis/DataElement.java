package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis;

import Jama.Matrix;

/**
 * Created by daniel on 06/10/2015.
 */
public class DataElement
{
    public double X;
    public double Y;
    public double Z;

    public double time;

    public DataElement()
    {
    }

    public Matrix ToMatrix()
    {
        double[][] data = new double[1][];
        data[0] = new double[2];
        data[0][0] = X;
        data[0][1] = Y;
        // data[0][2] = Z;

        return new Matrix(data);
    }



    public double GetValue(int index)
    {
        if (index == 0)
        {
            return X;
        }
        else if (index == 1)
        {
            return Y;
        }
        else
        {
            return Z;
        }
    }


    public static double Distance(DataElement a, DataElement b)
    {
        return Math.sqrt(Math.pow(a.X - b.X, 2) + Math.pow(a.Y - b.Y, 2) + Math.pow(a.Z - b.Z, 2));
    }


}
