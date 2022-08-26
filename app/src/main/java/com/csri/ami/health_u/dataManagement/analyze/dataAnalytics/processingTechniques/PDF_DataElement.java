package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.DataElement;

import Jama.Matrix;

/**
 * Probaility Density Function class to comput PDF from setof 2 dimensional data points
 * Created by daniel on 29/09/2015.
 */
class PDF_DataElement
{
    private DataElement _Mean;
    public DataElement Mean()
    {
        return _Mean;
    }
    private double[][] _CovarianceMatrix;
    public double[][] CovarianceMatrix()
    {
        return _CovarianceMatrix;
    }

    public double AverageProbability = 0;

    public PDF_DataElement(double[][] covariance, DataElement mean)
    {
        _Mean = mean;
        _CovarianceMatrix = covariance;
    }

    public PDF_DataElement()
    {
        _Mean = new DataElement();

    }


    /**
     * Compute probablity of point x belonging to stored PDF
     * @param x
     * @return probability
     */
    public double Probability(DataElement x)
    {
        if (Mean() != null && _CovarianceMatrix != null)
        {
            Matrix x_mat = x.ToMatrix();
            Matrix mean_Mat = Mean().ToMatrix();
            Matrix cov_mat = new Matrix(CovarianceMatrix());

            double det = cov_mat.det();
            if (!Double.isNaN(det))
            {
                double k = 2;

                double a = Math.pow((2 * Math.PI), (k / 2)) * Math.pow(det, 0.5);
                a = 1 / a;

                Matrix diff = x_mat.minus(mean_Mat);
                Matrix diff_T = diff.transpose();

                Matrix inv = null;
                if (det != 0)
                {
                    inv = cov_mat.inverse();

                    Matrix b = diff.times(inv);
                    b = b.times(diff_T);
                    double m = b.get(0, 0);
                    m = Math.exp(-0.5 * m);
                    double p = a * m;
                    if (!Double.isNaN(p) && !Double.isInfinite(p))
                    {
                        return p;
                    }
                    else
                    {
                        return 0;
                    }
                }
                else
                {
                    return 0;
                }

            }
            else
            {
                return 0;
            }
        }
        else
        {
            return 0;
        }

    }

    /**
     * Compute difference between two PDF_Elements
     * @param a
     * @param b
     * @return
     */
    public static PDF_DataElement Difference(PDF_DataElement a, PDF_DataElement b)
    {
        PDF_DataElement d = new PDF_DataElement();
        d._Mean.X = b.Mean().X - a.Mean().X;
        d._Mean.Y = b.Mean().Y - a.Mean().Y;
        d._Mean.Z = b.Mean().Z - a.Mean().Z;

        d._CovarianceMatrix = new double[a.CovarianceMatrix().length][];

        for (int i = 0; i < d._CovarianceMatrix.length; i++)
        {
            d._CovarianceMatrix[i] = new double[a._CovarianceMatrix[i].length];

            for (int j = 0; j < d._CovarianceMatrix[i].length; j++)
            {
                d._CovarianceMatrix[i][j] = b._CovarianceMatrix[i][j] - a._CovarianceMatrix[i][j];
            }
        }

        return d;
    }
}