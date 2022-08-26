package com.csri.ami.health_u.dataManagement.record;


/**
 * Compute Low pass, Highpass and Bandpass filters
 */
public class Filter {

	private double previous_LP = Double.NaN;
    private double previous_HP = Double.NaN;
    private double previousFiltered_HP = Double.NaN;

    private boolean previous_LP_init = false;
    private boolean previous_HP_init = false;
    private boolean previousFiltered_HP_init = false;
    public Filter()
    {
    }

    public double[] ApplyLowPassFilter(Double[] data, double cutoff)
    {
        double[] filteredArray = new double[data.length];
        for (int i = 0; i < data.length; i++)
        {
            if (!previous_LP_init)
            {
                previous_LP = data[i];
                previous_LP_init = true;
            }
            double filtered = 0;
            double rc = 1 / (2 * Math.PI * cutoff);
            double timeIntervalAccel = (double)1 / (double)5;
            double timeConstantAccel = rc;// 0.25;
            double alphaA = timeIntervalAccel / (timeIntervalAccel + timeConstantAccel);

            filtered = previous_LP + alphaA * (data[i] - previous_LP);

            previous_LP = /*filtered;*/ data[i];
            filteredArray[i] = filtered;
        }
        return filteredArray;
    }

    public double[] ApplyLowPassFilter(double[] data, double cutoff)
    {
        double[] filteredArray = new double[data.length];
        for (int i = 0; i < data.length; i++)
        {
            if (!previous_LP_init)
            {
                previous_LP = data[i];
                previous_LP_init = true;
            }
            double filtered = 0;
            double rc = 1 / (2 * Math.PI * cutoff);
            double timeIntervalAccel = (double)1 / (double)5;
            double timeConstantAccel = rc;// 0.25;
            double alphaA = timeIntervalAccel / (timeIntervalAccel + timeConstantAccel);

            filtered = previous_LP + alphaA * (data[i] - previous_LP);

            previous_LP = /*filtered;*/ data[i];
            filteredArray[i] = filtered;
        }
        return filteredArray;
    }

    public double ApplyLowPassFilter(double current,double cutoff)
    {
        if (!previous_LP_init)
        {
            previous_LP = current;
            previous_LP_init = true;
        }
        double filtered=0;
        double rc = 1 / (2 * Math.PI * cutoff);
        double timeIntervalAccel = (double)1 / (double)100;
        double timeConstantAccel = rc;// 0.25;
        double alphaA = timeIntervalAccel / (timeIntervalAccel + timeConstantAccel);

        filtered = previous_LP + alphaA * (current - previous_LP);

        previous_LP = current;
        return filtered;


    }

    public double ApplyHighPassFilter(double current,double cutoff)
    {
        double filtered = 0;
        if (!previous_HP_init)
        {
            previous_HP = current;
            previous_HP_init = true;
        }
        if (!previousFiltered_HP_init)
        {
            previousFiltered_HP = current;
            previousFiltered_HP_init = true;
        }
        double rc = 1 / (2 * Math.PI * cutoff);
        double timeIntervalAccel = (double)1 / (double)100;
        double timeConstantAccel = rc;// 0.05;
        double alpha = timeIntervalAccel / (timeIntervalAccel + timeConstantAccel);

        filtered = alpha * (previousFiltered_HP + current - previous_HP);//previousPacket.Accelerometer.X + alpha * (currentPacket.Accelerometer.X - previousPacket.Accelerometer.X);

        previousFiltered_HP = filtered;
        previous_HP = current;


        return filtered;


    }

    public double BandPassFilter(double current, double cuttoffMin, double cutoffMax)
    {
        double bandpass = ApplyHighPassFilter(current, cuttoffMin);
        double lowpass = ApplyLowPassFilter(bandpass, cutoffMax);
        
        return lowpass;
    }

}
