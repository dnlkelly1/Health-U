package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.DataElement;

import java.util.ArrayList;

import Jama.Matrix;

/**
 * DataStructure for a cluster
 * Class manages data points for the cluster
 * Class also include methods to calculate different statistical measures of the cluster
 * Created by daniel on 06/10/2015.
 */
public class Cluster
{
    public static int CLUSTER_DISTANCE_TYPE_avg=0;
    public static int CLUSTER_DISTANCE_TYPE_max=1;
    public static int CLUSTER_DISTANCE_TYPE_min=2;
    public static int CLUSTER_DISTANCE_TYPE = CLUSTER_DISTANCE_TYPE_avg;

    public static int CLUSTER_VARIANCE_TYPE_AVGMEAN=0;
    public static int CLUSTER_VARIANCE_TYPE_MAXMEAN=1;
    public static int CLUSTER_VARIANCE_TYPE_MAXDIST=2;
    public static int CLUSTER_VARIANCE_TYPE =CLUSTER_VARIANCE_TYPE_MAXDIST;
    public ArrayList<ClusterElement> Elements;
    public static int NUMBER_CLUSTER_VARIABLES = 11;

    public int NumberElements;

    private PDF_DataElement pdf;
    public PDF_DataElement Pdf(){return pdf;}
    public DataElement Mean() { return pdf.Mean(); }
    public DataElement Center()
    {
        return pdf.Mean();
    }



    public double clusterProbability;

    private double Var_MaxDistance;
    private double Var_StdDev;

    private int DistanceType;
    private int VarType;
    public TimeProbabilityDistribution timeProbability;

    public double Var()
    {

        {
            if (VarType == CLUSTER_VARIANCE_TYPE_MAXMEAN)
            {
                return Var_MaxDistance;
            }
            else
            {
                return Var_StdDev;
            }
        }
    }

    public double Variance()
    {
        return Var();
    }

    public Cluster(ArrayList<ClusterElement> elements,int varType,int distType)
    {
        VarType = varType;
        DistanceType = distType;
        Elements = elements;

        NumberElements = Elements.size();
        DataElement Mean = CalculateMean();
        Var_StdDev = CalculateMean_Var;
        if (varType == CLUSTER_VARIANCE_TYPE_MAXMEAN)
        {
            Mean = CalculateMeanWithMaxDistance();
            Var_MaxDistance = CalculateMeanWithMaxDistance_Var;
        }

    }

    private double CalculateMeanWithMaxDistance_Var;
    private DataElement CalculateMeanWithMaxDistance()
    {
        DataElement mean = new DataElement();
        DataElement _var = new DataElement();

        for (int i = 0; i < Elements.size(); i++)
        {
            mean.X += Elements.get(i).GetElement().X / (double)Elements.size();
            mean.Y += Elements.get(i).GetElement().Y / (double)Elements.size();
            mean.Z += Elements.get(i).GetElement().Z / (double)Elements.size();
        }
        DataElement mean2 = MeanAll();

        double max = Double.MIN_VALUE;
        for (int i = 0; i < Elements.size(); i++)
        {
            DataElement[] raw = Elements.get(i).GetRawElements();
            for (int j = 0; j < raw.length; j++)
            {
                double dist = DataElement.Distance(mean, raw[j]);
                if (dist > max)
                {
                    max = dist;
                }
            }
        }
        CalculateMeanWithMaxDistance_Var = max;
        return mean;

    }

    private double CalculateMean_Var;
    private DataElement CalculateMean()
    {


        DataElement mean = MeanAll();
        CalculateMean_Var = CalcVariance(mean);
        return mean;

    }

    public Cluster(Cluster x,Cluster y)
    {
        VarType = x.VarType;
        DistanceType = x.DistanceType;

        int _xCount = 0;
        int _yCount = 0;
        if (x.Elements != null && y.Elements != null)
        {
            Elements = new ArrayList<ClusterElement>();

            for (int i = 0; i < x.Elements.size(); i++)
            {
                Elements.add(x.Elements.get(i));
            }
            _xCount = x.Elements.size();



            for (int i = 0; i < y.Elements.size(); i++)
            {
                Elements.add(y.Elements.get(i));
            }
            _yCount = y.Elements.size();

            NumberElements = _xCount + _yCount;
        }
        else
        {
            _yCount = y.NumberElements;
            _xCount = x.NumberElements;
        }

        DataElement xMean = x.Mean();
        DataElement yMean = y.Mean();
        DataElement Mean = new DataElement();
        double xCount = _xCount;
        double yCount = _yCount;
        double total = xCount + yCount;
        double xW = xCount / total;
        double yW = yCount / total;
        Mean.X = (xMean.X * xW) + (yMean.X * yW);
        Mean.Y = (xMean.Y * xW) + (yMean.Y * yW);
        Mean.Z = (xMean.Z * xW) + (yMean.Z * yW);

        Var_StdDev = CalcVariance(Mean);
        Var_MaxDistance = MaxDistance(Mean);

    }

    public int size()
    {
        int x = 0;
        for (int i = 0; i < Elements.size(); i++)
        {
            x += Elements.get(i).GetRawElements().length;
        }
        return x;
    }

    public boolean IsNonSingular()
    {
        Matrix cov = new Matrix(pdf.CovarianceMatrix());
        if (cov.det() != 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public void GenerateTimeProbabilityDistibution(int timeslots)
    {
        double[] times = new double[Elements.size()];
        for (int i = 0; i < times.length; i++)
        {
            times[i] = Elements.get(i).GetElement().time;
        }

        timeProbability = new TimeProbabilityDistribution(timeslots,times);
    }

    private double MaxDistance(DataElement mean)
    {
        double max = Double.MIN_VALUE;
        for (int i = 0; i < Elements.size(); i++)
        {
            DataElement[] raw = Elements.get(i).GetRawElements();
            for (int j = 0; j < raw.length; j++)
            {
                double dist = DataElement.Distance(mean, raw[j]);
                if (dist > max)
                {
                    max = dist;
                }
            }
        }
        return max;
    }



    public double Probability(DataElement x)
    {
        return pdf.Probability(x);
    }

    public double Probability(double time)
    {
        double p = timeProbability.Probability(time);
        return p;
    }

    public Cluster Merge(Cluster y)
    {
        return new Cluster(this,y);

    }

    public double Distance(Cluster y)
    {
        if (DistanceType == CLUSTER_DISTANCE_TYPE_avg)
        {
            return AvgDistance(y);
        }
        else if (DistanceType == CLUSTER_DISTANCE_TYPE_max)
        {
            return MaxDistance(y);
        }
        else if (DistanceType == CLUSTER_DISTANCE_TYPE_min)
        {
            return MinDistance(y);
        }
        else
        {
            return AvgDistance(y);
        }
    }

    private double MinDistance(Cluster y)
    {
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < Elements.size(); i++)
        {
            DataElement i_current = Elements.get(i).GetElement();

            double currentAvg = 0;
            for (int j = 0; j < y.Elements.size(); j++)
            {
                DataElement j_current = y.Elements.get(j).GetElement();
                double currentdist = DataElement.Distance(i_current, j_current);
                if (currentdist < minDistance)
                {
                    minDistance = currentdist;
                }
            }
        }

        return minDistance;
    }

    private double MaxDistance(Cluster y)
    {
        double maxDistance = Double.MIN_VALUE;
        for (int i = 0; i < Elements.size(); i++)
        {
            DataElement i_current = Elements.get(i).GetElement();

            double currentAvg = 0;
            for (int j = 0; j < y.Elements.size(); j++)
            {
                DataElement j_current = y.Elements.get(j).GetElement();
                double currentdist = DataElement.Distance(i_current, j_current);
                if (currentdist > maxDistance)
                {
                    maxDistance = currentdist;
                }
            }
        }

        return maxDistance;
    }

    private double AvgDistance(Cluster y)
    {
        double avgDistance = 0;
        if (Elements != null)
        {
            for (int i = 0; i < Elements.size(); i++)
            {
                DataElement i_current = Elements.get(i).GetElement();

                double currentAvg = 0;
                for (int j = 0; j < y.Elements.size(); j++)
                {
                    DataElement j_current = y.Elements.get(j).GetElement();
                    currentAvg += DataElement.Distance(i_current, j_current) / (double)y.Elements.size();
                }
                avgDistance += currentAvg / (double)Elements.size();
            }
        }
        else
        {
            avgDistance = DataElement.Distance(y.Center(), Center());
        }

        return avgDistance;
    }

    private DataElement MeanAll()
    {
        DataElement mean = new DataElement();

        double count = 0;

        for (int i = 0; i < Elements.size(); i++)
        {
            count += Elements.get(i).GetRawElements().length;
        }

        for (int i = 0; i < Elements.size(); i++)
        {
            mean.X += Elements.get(i).GetElement().X * ((double)Elements.get(i).GetRawElements().length / count);
            mean.Y += Elements.get(i).GetElement().Y * ((double)Elements.get(i).GetRawElements().length / count);
            mean.Z += Elements.get(i).GetElement().Z * ((double)Elements.get(i).GetRawElements().length / count);
        }

        return mean;
    }

    private double CalcVariance(DataElement mean)
    {

        double distance = 0;
        double total = 0;
        for (int i = 0; i < Elements.size(); i++)
        {
            DataElement[] rawElemements = Elements.get(i).GetRawElements();
            total += rawElemements.length;
            for (int j = 0; j < rawElemements.length; j++)
            {
                distance += DataElement.Distance(mean, rawElemements[j]);
            }

        }

        double[][] CovarianceMatrix  = new double[2][];
        for (int i = 0; i < CovarianceMatrix.length; i++)
        {
            CovarianceMatrix[i] = new double[2];
            for (int j = 0; j <= i; j++)
            {
                double cov = Covariance(i, j, mean);
                CovarianceMatrix[i][j] = cov;
                CovarianceMatrix[j][i] = cov;
            }
        }

        pdf = new PDF_DataElement(CovarianceMatrix, mean);


        return distance / total;
    }

    private double Covariance(int index1, int index2,DataElement mean)
    {
        double totalCount = 0;
        double total = 0;
        double mean1 = mean.GetValue(index1);
        double mean2 = mean.GetValue(index2);

        for (int i = 0; i < Elements.size(); i++)
        {
            DataElement[] rawElemements = Elements.get(i).GetRawElements();
            totalCount += rawElemements.length;
            for (int j = 0; j < rawElemements.length; j++)
            {
                total += (rawElemements[j].GetValue(index1) - mean1) * (rawElemements[j].GetValue(index2) - mean2);

            }
        }

        return total / (totalCount - 1);
    }
}
