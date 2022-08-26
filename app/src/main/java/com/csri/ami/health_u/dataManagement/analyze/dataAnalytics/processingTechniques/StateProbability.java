package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.DataElement;

import Jama.Matrix;

/**
 * Models the combibation of location probability density function and time probability distribution
 * Given a cluster of locations and a set of times the user has been in these locations, we can then calcuate probability user will be in this region given time
 */
public class StateProbability
{
    public PDF_DataElement pdf;
    public TimeProbabilityDistribution timeProb;


    public StateProbability()
    {

    }

    public StateProbability(Cluster c)
    {
        pdf = c.Pdf();
        timeProb = c.timeProbability;
    }

    public void Update(DataElement mean, Matrix cov)
    {

        pdf = new PDF_DataElement(cov.getArrayCopy(),mean);
    }

    public double TimeProbability(DataElement currentDay)
    {
        return timeProb.Probability(currentDay.time);
    }

    public double Probability(DataElement currentDay)
    {
        double p = 0;
        if (currentDay.X < Double.MAX_VALUE && currentDay.X > Double.MIN_VALUE  && currentDay.Y < Double.MAX_VALUE  && currentDay.Y > Double.MIN_VALUE )
        {
            // ClusterElement x = currentDay;
            p = pdf.Probability(currentDay);

        }
        else
        {
            p = timeProb.Probability(currentDay.time);
        }
        return p;
    }



}
