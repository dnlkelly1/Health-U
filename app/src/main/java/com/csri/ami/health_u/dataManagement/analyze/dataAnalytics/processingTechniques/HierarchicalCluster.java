package com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.processingTechniques;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.DataElement;

import java.util.ArrayList;

/**
 * HierarchicalCluster for set of locations
 * Use to dynamically find locations of interest for a user
 * Each location of interest can be represented by a cluster.
 * The clustering technique is based on a hierarchical clustering algorihm, but modified to speicically work with GPS location
 * Created by daniel on 06/10/2015.
 */
public class HierarchicalCluster
{
    public static int NUMBER_TIMEPROB_TIMESLOTS = 8;
    ArrayList<Cluster> clusters;

    public HierarchicalCluster(GPS_Summary[] gps,double minsPerGPSPoint)
    {
        ArrayList<ClusterElement> data = new ArrayList<ClusterElement>();

        for (int i = 0; i < gps.length; i++)
        {
            if (gps[i] != null)
            {
                ClusterElement c = gps[i];
                data.add(c);
            }
        }


        boolean removeSingles = true;
        clusters = new ArrayList<Cluster>();
        for (int i = 0; i < data.size(); i++)
        {
            ArrayList<ClusterElement> elementCurrent = new ArrayList<ClusterElement>();
            elementCurrent.add(data.get(i));
            Cluster newCluster = new Cluster(elementCurrent,Cluster.CLUSTER_VARIANCE_TYPE_AVGMEAN,Cluster.CLUSTER_DISTANCE_TYPE);
            clusters.add(newCluster);
        }



        boolean finish = false;

        double increment = 4;
        double VarianceThreshold = 0.5;

        double VarianceThreshold_Lower = VarianceThreshold - 6 ;
        int minMinutesPerCluster = 30;
        int minClusterSize = (int)(minMinutesPerCluster / minsPerGPSPoint);// 15 mins
        double[][] distancematrix = GenerateDistanceMatrix(clusters);

        boolean firstPassComplete = false;

        while (!finish)
        {
            Integer[] allI = null, allJ = null;
            int minClusters = 200;
            int numberCluster = (int)(((double)clusters.size() / (double)100) * 5);
            if (numberCluster < minClusters)
            {
                numberCluster = minClusters;
            }

            GetMinIndices(distancematrix,numberCluster,0);
            allI = GetMinIndices_I;
            allJ = GetMinIndices_J;

            int validCount = 0;

            ArrayList<Cluster> newClusters=null;

            newClusters = new ArrayList<Cluster>();
            for (int i = 0; i < allI.length; i++)
            {
                int m = allI[i];
                int n = allJ[i];
                Cluster newCluster = clusters.get(m).Merge(clusters.get(n));

                double weightedThreshold = 1-  ((double)newCluster.Elements.size() / (double)data.size());
                double distance = distancematrix[m][n];
                double variable_VarianceThreshold = 2.0;


                if ((newCluster.Variance() < VarianceThreshold && clusters.get(m).Variance() < VarianceThreshold && clusters.get(n).Variance() < VarianceThreshold)
                   || (firstPassComplete && clusters.get(m).size() + clusters.get(n).size() < minClusterSize && newCluster.Variance() < variable_VarianceThreshold * weightedThreshold))// * weightedThreshold) || clusters[m].Count + clusters[n].Count < minClusterSize ))
                {

                    validCount++;
                    newClusters.add(newCluster);
                }
                else if (firstPassComplete && distance < (variable_VarianceThreshold * 2) && ((double)newCluster.NumberElements * minsPerGPSPoint) < minMinutesPerCluster)
                {
                    validCount++;
                    newClusters.add(newCluster);
                }
                else
                {
                    newClusters.add(null);
                }
            }

            if (validCount == 0)
            {
                if (!firstPassComplete)
                {
                    firstPassComplete = true;
                }
                else if (removeSingles)
                {
                    finish = true;
                }
                else if (!removeSingles)
                {
                    newClusters = new ArrayList<Cluster>();
                    boolean singleFound = false;
                    int index = -1;
                    for (int i = 0; i < clusters.size(); i++)
                    {
                        if (clusters.get(i).size() == 1 || !clusters.get(i).IsNonSingular())
                        {
                            singleFound = true;
                            index = i;
                        }
                    }


                    if (singleFound)
                    {
                        //if there exists a cluster with 1 element then find the cluster which is clostests to this element and set them up to merge
                        double minDist = Double.MAX_VALUE;
                        int closest = -1;
                        Cluster closestCluster = null;
                        while (closest == -1)
                        {
                            for (int i = 0; i < clusters.size(); i++)
                            {
                                if (i != index && (clusters.get(index).size() + clusters.get(i).size() < minClusterSize))
                                {
                                    Cluster newCluster = clusters.get(index).Merge(clusters.get(i));
                                    double d = newCluster.Variance();// clusters[index].Distance(clusters[i]);
                                    if (d < minDist)
                                    {
                                        closestCluster = newCluster;
                                        minDist = d;
                                        closest = i;
                                    }
                                }
                            }
                            if (closest == -1)
                            {
                                minClusterSize++;
                            }
                        }
                        allI = new Integer[] { index };
                        allJ = new Integer[] { closest };

                        newClusters.add(closestCluster);
                    }


                    if (newClusters.size() == 0)
                    {

                        finish = true;
                    }
                }
            }

            if (!finish)
            {
                ArrayList<Integer> removeM = new ArrayList<Integer>();
                ArrayList<Integer> removeN = new ArrayList<Integer>();
                for (int i = 0; i < newClusters.size(); i++)
                {
                    if (newClusters.get(i) != null)
                    {
                        int m = allI[i];
                        int n = allJ[i];
                        removeM.add(m);
                        removeN.add(n);

                        clusters.remove(m);
                        clusters.add(m, null);// = null;//.RemoveAt(m);
                        clusters.remove(n);
                        clusters.add(n,null);// = null;//.RemoveAt(n);
                        //clusters.RemoveAt(m);
                        //clusters.RemoveAt(n);

                        clusters.add(newClusters.get(i));


                    }


                }

                ArrayList<Cluster> newClusterset = new ArrayList<Cluster>();
                for (int i = 0; i < clusters.size(); i++)
                {
                    if (clusters.get(i) != null)
                    {
                        newClusterset.add(clusters.get(i));
                    }
                }
                clusters = newClusterset;


                distancematrix = GenerateDistanceMatrix(clusters);
            }
        }

        //any clusters that only have one location, remove them
        if (removeSingles)
        {
            boolean finished = false;
            while (!finished)
            {
                finished = true;
                for (int i = 0; i < clusters.size(); i++)
                {
                    if (clusters.get(i).NumberElements < minClusterSize/2)
                    {
                        clusters.remove(i);
                        finished = false;
                        break;
                    }
                }
            }
        }

        SetClusterProbabilities();
        for (int i = 0; i < clusters.size(); i++)
        {
            clusters.get(i).GenerateTimeProbabilityDistibution(NUMBER_TIMEPROB_TIMESLOTS);
        }
    }

    public static int MaxProb(double[] probs)
    {
        double max = Double.MIN_VALUE;
        int index = -1;

        for (int i = 0; i < probs.length; i++)
        {
            if (probs[i] > max)
            {
                max = probs[i];
                index = i;
            }
        }
        return index;
    }

    public double TimeProbabilities_NumberMinsPerTimeSlot()
    {
        double numberSlots = clusters.get(0).timeProbability.weekDayProbability.length;
        double mins = (60 * 24) / numberSlots;
        return mins;
    }

    public double[] GetTimeProbabilities(double t)
    {
        double total = 0;
        double[] p = new double[clusters.size()];


        for (int i = 0; i < clusters.size(); i++)
        {
            p[i] = clusters.get(i).Probability(t);// *clusters[i].clusterProbability;
            total += p[i];
        }

        for (int i = 0; i < clusters.size(); i++)
        {
            p[i] = p[i] / total;
        }
        return p;
    }

    public double[] ClusterProbabilities()
    {
        //get
        {
            double[] p = new double[clusters.size()];
            for (int i = 0; i < p.length; i++)
            {
                p[i] = clusters.get(i).clusterProbability;
            }
            return p;
        }
    }

    public double[] GetProbabilities(DataElement x)
    {
        double total = 0;
        double[] p = new double[clusters.size()];
        double[] p_time = new double[clusters.size()];
        double[] p_gps = new double[clusters.size()];
        double timeTotal = 0;

        for (int i = 0; i < clusters.size(); i++)
        {
            p_time[i] = clusters.get(i).Probability(x.time);
            timeTotal += p_time[i];
            p_gps[i] = clusters.get(i).Probability(x);
            p[i] = (/*clusters[i].Probability(x.time) */ clusters.get(i).Probability(x)) * clusters.get(i).clusterProbability;
            total += p[i];
        }
        if (total > 0)
        {
            for (int i = 0; i < clusters.size(); i++)
            {
                p[i] = p[i] / total;
            }
        }
        else
        {
            for (int i = 0; i < clusters.size(); i++)
            {
                p[i] = p_time[i] / timeTotal;
            }
        }
        return p;
    }

    public int NumberClusters()
    {
        return clusters.size();
    }

    private Integer[] GetMinIndices_I;
    private Integer[] GetMinIndices_J;

    /**
     * Given the distanceMatrix, find a set of clusters which can be candidates for merging based on min distances
     * @param distanceMatrix
     * @param max
     * @param VarianceThreshold_Lower
     */
    public void GetMinIndices( double[][] distanceMatrix,int max,double VarianceThreshold_Lower)
    {

        ArrayList<Integer> allI = new ArrayList<Integer>();
        ArrayList<Integer> allJ = new ArrayList<Integer>();

        boolean finished = false;
        while (!finished)
        {
            double minDiff = Double.MAX_VALUE;
            int min_i = -1;
            int min_j = -1;
            for (int i = 0; i < distanceMatrix.length; i++)
            {
                for (int j = 0; j < i; j++)
                {

                    if (distanceMatrix[i][ j] < minDiff || distanceMatrix[i][ j] == 0)
                    {

                        if(!(allI.contains(i) || allJ.contains(j) || allI.contains(j) || allJ.contains(i)))
                        {

                            minDiff = distanceMatrix[i][j];
                            min_i = i;
                            min_j = j;
                            if (minDiff == 0)
                            {
                                break;
                            }

                        }

                    }

                }
                if (minDiff == 0)
                {
                    break;
                }
            }

            if (min_i == -1 || min_j == -1)
            {
                finished = true;
            }
            else
            {
                allJ.add(min_j);
                allI.add(min_i);
            }
            if (allI.size() > max)
            {
                break;
            }
        }

        GetMinIndices_I = allI.toArray(new Integer[allI.size()]);
        GetMinIndices_J = allJ.toArray(new Integer[allJ.size()]);
    }

    /**
     * Calculate distance matrix where matrix element i,j is the distance between cluster i and cluster j
     * @param clusters
     * @return
     */
    public double[][] GenerateDistanceMatrix(ArrayList<Cluster> clusters)
    {
        double[][] distancematrix = new double[clusters.size()][];

        for (int i = 0; i < clusters.size(); i++)
        {
            distancematrix[i] = new double[i +1];
            for (int j = 0; j <= i; j++)
            {
                distancematrix[i][j] = clusters.get(i).Distance(clusters.get(j));
            }
        }

        return distancematrix;
    }

    private void SetClusterProbabilities()
    {
        double count = 0;
        for (int i = 0; i < clusters.size(); i++)
        {
            count += clusters.get(i).NumberElements;
        }

        for (int i = 0; i < clusters.size(); i++)
        {
            clusters.get(i).clusterProbability = (double)clusters.get(i).NumberElements / count;
        }
    }
}
