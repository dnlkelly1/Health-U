package com.csri.ami.health_u.dataManagement.record.sound;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques.TimeSyncTable;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniel on 02/10/2015.
 */
public class SoundFeatures
{
    public double timestamp;

    public DateTime Timestamp()
    {
        return TimeSyncTable.UnixTimeToDateTime(timestamp);
    }

    private double _duration;
    public double Duration() { return _duration; }

    private double _avergaeSound;
    public double AverageSound() { return _avergaeSound;  }


    private double[] _fftFeatures;
    public double[] FFTFeatures() {  return _fftFeatures;  }

    private SpectralFeatures _spectralFeatures;
    public SpectralFeatures spectralFeatures() {  return _spectralFeatures;  }

    private static int NUMFREQBINS = 8;

    public int ClusterId = -1;

    public int[] BadColumnIndices;

    public double[] PCA_Data;

    public SoundFeatures()
    {

    }

    public double[] FeaturesAll()
    {
        if (PCA_Data != null)
        {
            return PCA_Data;
        }
        else
        {
            return FeaturesAllRaw();
        }
    }

    public double[] FeaturesAllRaw()
    {
        ArrayList<Double> current = new ArrayList<Double>();
        //rawFeatures[i] = new double[data[i].spectralFeatures.FeatureVector.Length - 1];
        int c = 0;
        for (int j = 0; j < spectralFeatures().FeatureVector().length; j++)
        {
            current.add(spectralFeatures().FeatureVector()[j]);
            //rawFeatures[i][c] = data[i].spectralFeatures.FeatureVector[j];
            //c++;
        }

        for (int j = 0; j < FFTFeatures().length; j++)//was FFTFeatures.Length - 1...on 12/9/13
        {
            current.add(FFTFeatures()[j]);
        }


        for (int j = 0; j < spectralFeatures().Cepstral.length; j++)
        {
            current.add(spectralFeatures().Cepstral[j]);
        }

        double[] res = new double[current.size()];
        for(int i=0;i<res.length;i++)
        {
            res[i] = current.get(i);
        }

        return res;

    }

    public SoundFeatures(String[] fileStringData,boolean scaleTime)
    {


        double[] fileData = new double[fileStringData.length-1];
        for (int i = 0; i < fileData.length; i++)
        {

            double val = 0;
            val = Double.parseDouble(fileStringData[i]);
           // boolean numberPresent = Double.TryParse(fileStringData[i],out val);

            fileData[i] = val;


        }

        if (scaleTime)
        {
            timestamp = fileData[0] / 1000;
        }
        else
        {
            timestamp = fileData[0 ];
        }
        _duration = fileData[1 ];
        _avergaeSound = fileData[2 ];

        int offset = 3 ;

        _fftFeatures = new double[NUMFREQBINS];
        for (int i = 0; i < _fftFeatures.length; i++)
        {
            _fftFeatures[i] = fileData[offset + i];
        }

        double[] spectralFeatureData = new double[fileData.length -  (offset + NUMFREQBINS) ];
        for (int i = 0; i < spectralFeatureData.length; i++)
        {
            spectralFeatureData[i] = fileData[offset + NUMFREQBINS + i];
        }

        _spectralFeatures = new SpectralFeatures(spectralFeatureData);
    }

    public static ArrayList<SoundFeatureSummary> ConvertSoundFeaturesToFeatureSummarys(SoundFeatures[] sound,boolean summarizeData,double soundThreshold)
    {
        double windowLength = 0.05;

        ArrayList<SoundFeatureSummary> featuresummaries = new ArrayList<SoundFeatureSummary>();

        ArrayList<Integer> sizes = new  ArrayList<Integer>();

        SoundFeatures[][] windows = new SoundFeatures[1][]; // GetRecordingWindowFeatures(sound);
        windows[0] = sound;

        //SoundFeatures[] windowSummaries_currentPerson = new SoundFeatures[windows.length];
        for (int i = 0; i < windows.length; i++)
        {
            if (summarizeData)
            {
                SoundFeatures var = null;
               // windowSummaries_currentPerson[i] = SoundFeatures.Average(windows[i],var);// SoundFeatures.Median(windows[i], m);//SummarizeFeatureWindow(windows[i]);
                SoundFeatures[] variance = null;
                SoundFeatures[] currentWindow = /*new SoundFeatures[] { windowSummaries_currentPerson[i] };//*/ SoundFeatures.WindowAverage(windows[i],windowLength,variance);//

                for (int j = 0; j < currentWindow.length; j++)
                {
                    if (currentWindow[j].AverageSound() > soundThreshold)
                    {
                        SoundFeatureSummary f = new SoundFeatureSummary(0, currentWindow[j].ClusterId, currentWindow[j].Timestamp(), currentWindow[j].FeaturesAll(),null /*variance[j].FeaturesAll()*/, false, currentWindow[j].AverageSound());
                        featuresummaries.add(f);
                        sizes.add(f.featureVector.length);
                    }
                }
            }

        }


        return featuresummaries;
    }

    public static SoundFeatures[] WindowAverage(SoundFeatures[] data,double windowLenSeconds,SoundFeatures[] variance)
    {
        int index = 0;

        ArrayList<SoundFeatures> windowFeatures = new ArrayList<SoundFeatures>();
        ArrayList<SoundFeatures> windowFeatures_variance = new ArrayList<SoundFeatures>();

        while (index < data.length)
        {
            List<SoundFeatures> currentWindow = new ArrayList<SoundFeatures>();
            double currentLength = 0;
            double start = data[index].timestamp;

            while (currentLength < windowLenSeconds && index < data.length-1)
            {
                currentWindow.add(data[index]);

                index++;
                currentLength = data[index].timestamp - start;
            }
            if (currentWindow.size() > 0 && currentLength > windowLenSeconds * 0.9)
            {
                SoundFeatures var = null;
                SoundFeatures current = Average(currentWindow.toArray(new SoundFeatures[currentWindow.size()]),var);
                windowFeatures.add(current);
                windowFeatures_variance.add(var);

                index -= (currentWindow.size() / 2);
            }
            else
            {
                index++;
            }

            // index++;
        }
        variance = windowFeatures_variance.toArray(new SoundFeatures[windowFeatures_variance.size()]);
        return windowFeatures.toArray(new SoundFeatures[windowFeatures.size()]);
    }

    public static SoundFeatures Average(SoundFeatures[] data,SoundFeatures variance)
    {
        SoundFeatures av = new SoundFeatures();
        av.ClusterId = data[0].ClusterId;
        av.timestamp = data[0].timestamp;
        av._duration = data[0]._duration;

        SoundFeatures var = new SoundFeatures();
        SpectralFeatures sf_var=null;
        var.ClusterId = data[0].ClusterId;
        var.timestamp = data[0].timestamp;
        var._duration = data[0]._duration;


        av._fftFeatures = new double[data[0].FFTFeatures().length];
        var._fftFeatures = new double[data[0].FFTFeatures().length];
        SpectralFeatures[] sf = new SpectralFeatures[data.length];

        for (int i = 0; i < data.length; i++)
        {
            for (int j = 0; j < av._fftFeatures.length; j++)
            {
                av._fftFeatures[j] += data[i]._fftFeatures[j] / (double)data.length;
            }

            av._avergaeSound += data[i]._avergaeSound / (double)data.length;

            sf[i] = data[i].spectralFeatures();
        }

        ///variance/////////////////////////////////////////////
        for (int i = 0; i < data.length; i++)
        {
            for (int j = 0; j < av._fftFeatures.length; j++)
            {
                var._fftFeatures[j] += Math.abs(av._fftFeatures[j] - data[i]._fftFeatures[j]) / (double)data.length;
            }

            var._avergaeSound += Math.abs(av._avergaeSound - data[i]._avergaeSound) / (double)data.length;
        }
        /////////////////////////////////////////////////////////

        if (data[0].PCA_Data != null)
        {
            av.PCA_Data = new double[data[0].PCA_Data.length];
            for (int i = 0; i < data.length; i++)
            {
                for (int j = 0; j < data[i].PCA_Data.length; j++)
                {
                    av.PCA_Data[j] += data[i].PCA_Data[j] / (double)data.length;
                }
            }
        }

        av._spectralFeatures = SpectralFeatures.Average(sf,sf_var);
        var._spectralFeatures = sf_var;
        av.BadColumnIndices = data[0].BadColumnIndices;
        var.BadColumnIndices = data[0].BadColumnIndices;

        variance = var;

        return av;
    }

    public static SoundFeatures Average(SoundFeatures[] data,SoundFeatures[] variance)
    {
        SoundFeatures av = new SoundFeatures();
        av.ClusterId = data[0].ClusterId;
        av.timestamp = data[0].timestamp;
        av._duration = data[0]._duration;

        SoundFeatures var = new SoundFeatures();
        SpectralFeatures sf_var=null;
        var.ClusterId = data[0].ClusterId;
        var.timestamp = data[0].timestamp;
        var._duration = data[0]._duration;


        av._fftFeatures = new double[data[0].FFTFeatures().length];
        var._fftFeatures = new double[data[0].FFTFeatures().length];
        SpectralFeatures[] sf = new SpectralFeatures[data.length];

        for (int i = 0; i < data.length; i++)
        {
            for (int j = 0; j < av._fftFeatures.length; j++)
            {
                av._fftFeatures[j] += data[i]._fftFeatures[j] / (double)data.length;
            }

            av._avergaeSound += data[i]._avergaeSound / (double)data.length;

            sf[i] = data[i].spectralFeatures();
        }

        ///variance/////////////////////////////////////////////
        for (int i = 0; i < data.length; i++)
        {
            for (int j = 0; j < av._fftFeatures.length; j++)
            {
                var._fftFeatures[j] += Math.abs(av._fftFeatures[j] - data[i]._fftFeatures[j]) / (double)data.length;
            }

            var._avergaeSound += Math.abs(av._avergaeSound - data[i]._avergaeSound) / (double)data.length;
        }
        /////////////////////////////////////////////////////////

        if (data[0].PCA_Data != null)
        {
            av.PCA_Data = new double[data[0].PCA_Data.length];
            for (int i = 0; i < data.length; i++)
            {
                for (int j = 0; j < data[i].PCA_Data.length; j++)
                {
                    av.PCA_Data[j] += data[i].PCA_Data[j] / (double)data.length;
                }
            }
        }

        av._spectralFeatures = SpectralFeatures.Average(sf,sf_var);
        var._spectralFeatures = sf_var;
        av.BadColumnIndices = data[0].BadColumnIndices;
        var.BadColumnIndices = data[0].BadColumnIndices;



        return av;
    }
}
