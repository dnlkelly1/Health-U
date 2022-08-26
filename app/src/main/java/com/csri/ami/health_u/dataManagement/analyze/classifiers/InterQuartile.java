package com.csri.ami.health_u.dataManagement.analyze.classifiers;

import java.util.ArrayList;
import java.util.Collections;

public class InterQuartile 
{
	public InterQuartile()
	{

	}

	public static double InterQuartileRange(ArrayList<Double> values)
	{
		
		double[] quartiles = Quartiles(values);
		return quartiles[2] - quartiles[0];
	}

	public static double Median(ArrayList<Double> values)
	{
		Collections.sort(values);

		if (values.size() % 2 == 1)
			return (Double) values.get((values.size()+1)/2-1);
		else
		{
			double lower = (Double)values.get(values.size()/2-1);
			double upper = (Double)values.get(values.size()/2);

			return (lower + upper) / 2.0;
		}	
	}

	public static double[] Quartiles(ArrayList<Double> values)
	{
		if (values.size() < 3)
			return new double[] {0,0,0};
		else
		{
			double median = Median(values);

			ArrayList<Double> lowerHalf = GetValuesLessThan(values, median, true);
			ArrayList<Double> upperHalf = GetValuesGreaterThan(values, median, true);

		return new double[] {Median(lowerHalf), median, Median(upperHalf)};
		}
	}

	public static ArrayList<Double> GetValuesGreaterThan(ArrayList<Double> values, double limit, boolean orEqualTo)
	{
		ArrayList<Double> modValues = new ArrayList<Double>();

		//for (Double value : values)
		for(int i=0;i<values.size();i++)
		{
			double value = (Double)values.get(i);
			if (value > limit || (value == limit && orEqualTo))
				modValues.add(value);
		}

		return modValues;
	}

	public static ArrayList<Double> GetValuesLessThan(ArrayList<Double> values, double limit, boolean orEqualTo)
	{
		ArrayList<Double> modValues = new ArrayList<Double>();

		for (double value : values)
			if (value < limit || (value == limit && orEqualTo))
				modValues.add(value);

		return modValues;
	}
}
