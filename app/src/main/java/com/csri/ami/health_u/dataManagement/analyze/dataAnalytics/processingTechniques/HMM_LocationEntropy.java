package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.DataElement;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.ArrayList;

/**
 * Models the set of learned locations to build a markov chain of locations and transition proabilities
 * Model is used to compute location predictabilit for user
 * Based on novel technique developed by Daniel Kelly: https://doi.org/10.1109/TSMC.2013.2238926
 * Created by daniel on 06/10/2015.
 */
public class HMM_LocationEntropy
{
    public static int DAY_TYPE_WEEKEND=0;
    public static int DAY_TYPE_WEEKDAY=1;


    double[] Probabilities;
    double[][] Transitions;

    StateProbability[] ObsProbs;
    int States;

    int daytype;

    public HMM_LocationEntropy(DataElement[][] learningData,GPS_Summary[] transitionSamples,ArrayList<GPS_Summary[]> days,HierarchicalCluster clusters,int type,boolean doHMMLearning)
    {
        daytype = type;
        States = clusters.NumberClusters();
        Transitions = GenerateTransitionProbs(transitionSamples, clusters);
        Probabilities = clusters.ClusterProbabilities();

        ObsProbs = new StateProbability[clusters.NumberClusters()];
        for (int i = 0; i < clusters.NumberClusters(); i++)
        {
            ObsProbs[i] = new StateProbability(clusters.clusters.get(i));
        }


    }

    private double[][] GenerateTransitionProbs(GPS_Summary[] data,HierarchicalCluster clusters)
    {
        int MaxElementsToLookForNext = 4 * 6;
        int[] count = new int[clusters.NumberClusters()];
        double[][] A = new double[clusters.NumberClusters()][];
        for (int i = 0; i < A.length; i++)
        {
            A[i] = new double[A.length];
        }


        for (int i = 0; i < data.length; i++)
        {

            ClusterElement currentElement = data[i];


            ClusterElement nextElement = null;//
            int next = 1;
            while (nextElement == null && next < MaxElementsToLookForNext && i + next < data.length)
            {
                nextElement = data[i + next];
                next++;
            }

            if (currentElement != null && nextElement != null)
            {
                DateTime time = TimeSyncTable.UnixTimeToDateTime(currentElement.GetElement().time);
                if (IsCorrectDayType(time))
                {
                    double[] probs = clusters.GetProbabilities(currentElement.GetElement());
                    int currentIndex = HierarchicalCluster.MaxProb(probs);
                    double[] nextprobs = clusters.GetProbabilities(nextElement.GetElement());
                    for (int j = 0; j < nextprobs.length; j++)
                    {
                        A[currentIndex][j]+= nextprobs[j];
                    }
                    int nextIndex = HierarchicalCluster.MaxProb(nextprobs);


                    count[currentIndex]++;
                }
            }


        }

        for (int i = 0; i < A.length; i++)
        {
            for (int j = 0; j < A[i].length; j++)
            {
                if (count[i] > 0)
                {
                    A[i][j] = A[i][j] / (double)count[i];
                }
                else
                {
                    A[i][j] = 1 / (double)A[i].length;
                }
            }
        }

        return A;
    }

    private boolean IsCorrectDayType(DateTime time)
    {
        if (DAY_TYPE_WEEKEND == daytype)
        {
            if (time.getDayOfWeek() == DateTimeConstants.SUNDAY || time.getDayOfWeek() == DateTimeConstants.SATURDAY )
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else if (DAY_TYPE_WEEKDAY == daytype)
        {
            if (time.getDayOfWeek() != DateTimeConstants.SUNDAY && time.getDayOfWeek() != DateTimeConstants.SATURDAY)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public Double[] forwardEntropy_c;
    public double forwardEntropy(DataElement[] observations)
    {
        int T = observations.length;
        double[] pi = Probabilities;
        double[][] A = Transitions;

        double[][] fwd = new double[T][];
        double[][] H = new double[T][];
        for (int i = 0; i < T; i++)
        {
            fwd[i] = new double[States];
            H[i] = new double[States];
        }
        forwardEntropy_c = new Double[T];
        for(int i=0;i<forwardEntropy_c.length;i++)
        {
            forwardEntropy_c[i] = new Double(0.0);
        }


        // 1. Initialization
        for (int i = 0; i < States; i++)
        {
            double p = Prob(observations[0], i);
            fwd[0][i] = pi[i] * p;
            forwardEntropy_c[0] += fwd[0][i];
        }

        if (forwardEntropy_c[0] != 0) // Scaling
        {
            for (int i = 0; i < States; i++)
                fwd[0][i] = fwd[0][i] / forwardEntropy_c[0];
        }

        ArrayList<Double> timeH = new ArrayList<Double>();



        // 2. Induction
        for (int t = 1; t < T; t++)
        {
            for (int i = 0; i < States; i++)
            {
                double p = Prob(observations[t], i);

                double sum = 0.0;
                for (int j = 0; j < States; j++)
                {

                    sum += fwd[t - 1][j] * A[j][i];
                }
                fwd[t][i] = sum * p;

                forwardEntropy_c[t] += fwd[t][i]; // scaling coefficient
            }


            if (forwardEntropy_c[t] != 0) // Scaling
            {
                for (int i = 0; i < States; i++)
                    fwd[t][i] = fwd[t][i] / forwardEntropy_c[t];
            }

            for (int Hj = 0; Hj < States; Hj++)
            {
                double sum1 = 0;
                for (int i = 0; i < States; i++)
                {
                    double p = Prob_St_1i__Stj(t, i, Hj, A, fwd);

                    sum1 += (H[t-1][i] * p);
                }
                if (!(sum1 < Double.MAX_VALUE && sum1 > Double.MIN_VALUE))
                {
                    int error = 1;
                }

                double sum2 = 0;
                for (int i = 0; i < States; i++)
                {
                    double p = Prob_St_1i__Stj(t, i, Hj, A, fwd);
                    if (p != 0)
                    {
                        sum2 += (p * Log(p, 2));
                    }

                }
                if (!(sum2 < Double.MAX_VALUE && sum2 > Double.MIN_VALUE))
                {
                    int error = 1;
                }

                H[t][Hj] = (sum1 -sum2);


            }

            double current = 0;

            double currentSum1 = 0;
            for (int i = 0; i < States; i++)
            {
                currentSum1 += H[t][i] * fwd[t][i];

            }
            double currentSum2 = 0;
            for (int i = 0; i < States; i++)
            {
                if (fwd[t][i] != 0)
                {
                    currentSum2 += fwd[t][i] * Log(fwd[t][i], 2);
                }

            }

            current = currentSum1 - currentSum2;
            timeH.add(current);
        }

        //3 termination
        double termSum1 = 0;
        for (int i = 0; i < States; i++)
        {
            termSum1 += H[T - 1][i] * fwd[T - 1][i];

        }
        double termSum2 = 0;
        for (int i = 0; i < States; i++)
        {
            if (fwd[T - 1][i] != 0)
            {
                termSum2 += fwd[T - 1][i] * Log(fwd[T - 1][i], 2);
            }

        }
        double H_ST_OT = (termSum1 -termSum2);
        forwardEntropy_c  = timeH.toArray(new Double[timeH.size()]);

        return H_ST_OT;
    }

    public static double Log(double num,int base)
    {
        return Math.log(num) / Math.log(base);
    }

    private double Prob_St_1i__Stj(int t,int i,int j,double[][] A,double[][] fwd)
    {
        double numerator = A[i][j] * fwd[t - 1][i];
        double denom = 0;
        for (int k = 0; k < States; k++)
        {
            denom += A[k][j] * fwd[t - 1][k];
        }
        if (denom == 0)
        {
            return 0;
        }
        else
        {
            return numerator / denom;
        }
    }

    private double Prob(DataElement currentObservation, int state_i)
    {
        double prob = ObsProbs[state_i].Probability(currentObservation);
        double total = 0;
        for (int i = 0; i < ObsProbs.length; i++)
        {
            total += (ObsProbs[i].Probability(currentObservation));
        }

        if (prob == 0 && total == 0)
        {
            prob = ObsProbs[state_i].TimeProbability(currentObservation);
            total = 0;
            for (int i = 0; i < ObsProbs.length; i++)
            {
                total += (ObsProbs[i].TimeProbability(currentObservation));
            }

        }

        if (total == 0)
        {
            return (double) 1 / (double)ObsProbs.length;
        }
        else if (prob == 0)
        {
            return 0;
        }
        else
        {
            double p =prob / total;
            return p;
        }
    }


}
