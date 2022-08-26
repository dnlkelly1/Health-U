package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.DataElement;
import com.csri.ami.health_u.dataManagement.record.GPS;

import java.util.ArrayList;

/**
 * Data Structure used to summarise a group of GPS data points
 * Used to calculate number of different statistical features for GPS clusters for location clustering
 * Created by daniel on 06/10/2015.
 */
public class GPS_Summary extends ClusterElement
{
    private double Lat;
    private double Long;
    private double UTM_X;
    private double UTM_Y;
    private double Lat_Long_Variance;
    public double Max_Radius;
    public double Avg_Radius;
    public double Avg_UTM_Radius;

    private double Speed;
    private double Speed_Var;
    private double Speed_Acc;

    private double Accuracy;
    private double Accuracy_Var;


    private double Heading;
    private double Heading_Var;
    private double Heading_Acc;

    private double MovementStatus;

    private int NumberGPSSamples = 0;
    private int NumberEmptyGPSSamples = 0;
    private double GPS_coverage = 0;

    private GPS[] _data;

    public GPS[] StoredData()
    {
        return _data;
    }
    public double TimeStamp;
    public String TimeStamp_string;

    @Override
    public ClusterElement FromString(String[] filedata)
    {
        return new GPS_Summary(filedata);
    }

    @Override
    public String ToString()
    {
        String line = UTM_X + "," + UTM_Y + "," + 0 + "," + TimeStamp + ",";

        if (_data != null)
        {
         line += _data.length + ",";
            for (int i = 0; i < _data.length; i++)
            {
                line += _data[i].UTM_X + "," + _data[i].UTM_Y + "," + 0 + "," + _data[i].timestamp + ",";
            }
        }
        else
        {
         line += 0 + ",";
        }

        return line;

    }

    @Override
    public DataElement GetElement()
    {
        DataElement x = new DataElement();
        if (_data != null && _data.length == 1)
        {
            double avgx = 0;
            double avgy = 0;

            for (int i = 0; i < _data.length; i++)
            {
                avgx += (_data[i].UTM_X / (double)_data.length);
                avgy += (_data[i].UTM_Y / (double)_data.length);
                x.X = avgx;
                x.Y = avgy;
                x.time = TimeStamp;
            }
        }
        else
        {
            x.X = UTM_X;
            x.Y = UTM_Y;
            x.time = TimeStamp;
        }
        return x;
    }

    @Override
    public DataElement[] GetRawElements()
    {
        if (_data != null)
        {
            DataElement[] data = new DataElement[_data.length];

            for (int i = 0; i < _data.length; i++)
            {
                DataElement x = new DataElement();

                x.X = _data[i].UTM_X;
                x.Y = _data[i].UTM_Y;
                x.time = TimeStamp;
                data[i] = x;
            }

            return data;
        }
        else
        {
            DataElement[] data = new DataElement[1];
            data[0] = new DataElement();
            data[0].X = UTM_X;
            data[0].Y = UTM_Y;
            data[0].time = TimeStamp;

            return data;
        }
    }

    public GPS_Summary(double time)
    {
        TimeStamp = time;

        UTM_X = Double.NaN;
        UTM_Y = Double.NaN;
    }



    public GPS_Summary(GPS[] data, int emptyCount, int gpsDataCount, double time, boolean summariseStoredData)
    {
        TimeStamp = time;
        TimeStamp_string = TimeSyncTable.UnixTimeToDateTime(TimeStamp).toString("HH:mm dd/MM/yyyy");
        Init(data, emptyCount, gpsDataCount, summariseStoredData);

    }

    private void Init(GPS[] data, int emptyCount, int gpsDataCount, boolean summariseStoredData)
    {

        NumberGPSSamples = gpsDataCount;
        NumberEmptyGPSSamples = emptyCount;
        GPS_coverage = (double)NumberGPSSamples / (double)(NumberEmptyGPSSamples + NumberGPSSamples);
        double LatVar = 0;
        Lat = Avg(LatArray(data));
        LatVar = Avg_var;

        double LongVar = 0;
        Long = Avg(LongArray(data));
        LongVar = Avg_var;

        double UTMXVar = 0;
        UTM_X = Avg(UTM_X_Array(data));
        UTMXVar = Avg_var;

        double UTMYVar = 0;
        UTM_Y = Avg(UTM_Y_Array(data));
        UTMYVar = Avg_var;

        Avg_UTM_Radius = GetAvgUTMDistance(UTM_X, UTM_Y, data);

        GPS avgGPS = new GPS();
        avgGPS.Lat = Lat;
        avgGPS.Long = Long;
        avgGPS.timestamp = data[0].timestamp;
        avgGPS.LoadUTMValues();
        Max_Radius = GetMaxDistance(avgGPS, data);
        Avg_Radius = GetAvgDistance(avgGPS, data);

        GPS closestToAvg = GetMinDistancePoint(avgGPS,data);
        Lat = closestToAvg.Lat;
        Long = closestToAvg.Long;
        avgGPS.Lat = closestToAvg.Lat;
        avgGPS.Long = closestToAvg.Long;


        Lat_Long_Variance = (LatVar + LongVar) / 2;

        Speed_Acc = Avg(Speed_AccuracyArray(data));
        Speed_Var = Avg_var;
        Speed = Avg(SpeedArray(data));
        Speed_Var = Avg_var;

        Heading_Acc = Avg(Heading_AccuracyArray(data));
        Heading_Var = Avg_var;

        Heading = Avg(HeadingArray(data));
        Heading_Var = Avg_var;

        double H_Var = 0;
        double H_acc = Avg(H_AccuracyArray(data));
        H_Var = Avg_var;

        double V_Var = 0;
        double V_acc = Avg(H_AccuracyArray(data));
        V_Var = Avg_var;

        Accuracy = (H_acc);
        Accuracy_Var = (H_Var);

        MovementStatus = Avg_Radius / Accuracy;

        if (summariseStoredData)
        {
            //GPS x = new GPS();
            double minDistance = Double.MAX_VALUE;
            int index = -1;
            for (int i = 0; i < data.length; i++)
            {
                double dist = GPS.UTMDistance(data[i].UTM_X, data[i].UTM_Y, avgGPS);
                if (dist < minDistance)
                {
                    minDistance = dist;
                    index = i;
                }
            }

            _data = new GPS[] { data[index] };
            UTM_X = data[index].UTM_X;
            UTM_Y = data[index].UTM_Y;

        }
        else
        {
            _data = data;
        }
    }

    public static GPS_Summary[] Flatten(ArrayList<GPS_Summary[]> data)
    {
        ArrayList<GPS_Summary> dataall = new ArrayList<GPS_Summary>();

        for (int i = 0; i < data.size(); i++)
        {
            for (int j = 0; j < data.get(i).length; j++)
            {
                dataall.add(data.get(i)[j]);
            }
        }
        return dataall.toArray(new GPS_Summary[dataall.size()]);
    }

    private double Avg_var;
    private double Avg(double[] data)
    {
        double avg = 0;
        for (int i = 0; i < data.length; i++)
        {
            avg += data[i] / (double)data.length;
        }



        double v = 0;
        for (int i = 0; i < data.length; i++)
        {
            v += Math.pow(data[i] - avg, 2) / (double)data.length;
        }
        Avg_var = Math.sqrt(v);

        return avg;
    }

    public GPS_Summary(String[] fromFile)
    {
        UTM_X = Double.parseDouble(fromFile[0]);
        UTM_Y = Double.parseDouble(fromFile[1]);
        TimeStamp = Double.parseDouble(fromFile[3]);

        int size = Integer.parseInt(fromFile[4]);

        int numHeaderElements = 5;
        int numElementsPerPoint = 4;

        _data = new GPS[size];
        int count = 0;

        for (int j = numHeaderElements; j < numHeaderElements + (size * numElementsPerPoint) && j < fromFile.length; j += numElementsPerPoint)
        {
            GPS x = new GPS(Double.parseDouble(fromFile[j]), Double.parseDouble(fromFile[j + 1]));
            _data[count] = x;
            count++;
        }

    }

    private GPS GetMinDistancePoint(GPS avg, GPS[] data)
    {
        double min = Double.MAX_VALUE;
        int index=-1;
        for (int i = 0; i < data.length; i++)
        {
            double dis = GPS.Distance(avg, data[i]);
            if (dis < min)
            {
                min = dis;
                index = i;
            }
        }
        return data[index];
    }

    private double GetMaxDistance(GPS avg, GPS[] data)
    {
        double max = Double.MIN_VALUE;
        for (int i = 0; i < data.length; i++)
        {
            double dis = GPS.Distance(avg, data[i]);
            if (dis > max)
            {
                max = dis;
            }
        }
        return max;
    }

    private double GetAvgUTMDistance(double utm_x, double utm_y, GPS[] data)
    {
        double a = 0;
        for (int i = 0; i < data.length; i++)
        {
            double dis = GPS.UTMDistance(utm_x, utm_y, data[i]);
            a += dis / (double)data.length;
        }
        return a;
    }

    private double GetAvgDistance(GPS avg, GPS[] data)
    {
        double a = 0;
        for (int i = 0; i < data.length; i++)
        {
            double dis = GPS.Distance(avg, data[i]);
            a += dis / (double)data.length;
        }
        return a;
    }

    private double[] UTM_X_Array(GPS[] data)
    {
        double[] data_array = new double[data.length];
        for (int i = 0; i < data.length; i++)
        {
            data_array[i] = data[i].UTM_X;
        }

        return data_array;
    }

    private double[] UTM_Y_Array(GPS[] data)
    {
        double[] data_array = new double[data.length];
        for (int i = 0; i < data.length; i++)
        {
            data_array[i] = data[i].UTM_Y;
        }

        return data_array;
    }

    private double[] LatArray(GPS[] data)
    {
        double[] data_array = new double[data.length];
        for (int i = 0; i < data.length; i++)
        {
            data_array[i] = data[i].Lat;
        }

        return data_array;
    }

    private double[] LongArray(GPS[] data)
    {
        double[] data_array = new double[data.length];
        for (int i = 0; i < data.length; i++)
        {
            data_array[i] = data[i].Long;
        }

        return data_array;
    }

    private double[] SpeedArray(GPS[] data)
    {
        double[] data_array = new double[data.length];
        for (int i = 0; i < data.length; i++)
        {
            data_array[i] = data[i].Speed;
        }

        return data_array;
    }

    private double[] HeadingArray(GPS[] data)
    {
        double[] data_array = new double[data.length];
        for (int i = 0; i < data.length; i++)
        {
            data_array[i] = data[i].Heading;
        }

        return data_array;
    }

    private double[] H_AccuracyArray(GPS[] data)
    {
        double[] data_array = new double[data.length];
        for (int i = 0; i < data.length; i++)
        {
            data_array[i] = data[i].Horizontal_Acc;
        }

        return data_array;
    }

    private double[] V_AccuracyArray(GPS[] data)
    {
        double[] data_array = new double[data.length];
        for (int i = 0; i < data.length; i++)
        {
            data_array[i] = data[i].Vertical_Acc;
        }

        return data_array;
    }

    private double[] Speed_AccuracyArray(GPS[] data)
    {
        double[] data_array = new double[data.length];
        for (int i = 0; i < data.length; i++)
        {
            data_array[i] = data[i].Speed_Accuracy;
        }

        return data_array;
    }

    private double[] Heading_AccuracyArray(GPS[] data)
    {
        double[] data_array = new double[data.length];
        for (int i = 0; i < data.length; i++)
        {
            data_array[i] = data[i].Heading_Accuracy;
        }

        return data_array;
    }
}
