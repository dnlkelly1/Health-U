package SVM;

import java.util.ArrayList;


public class Parameter 
{
	public SvmType SvmType;
	public KernelType KernelType;
    public int Degree;
    public double Gamma;
    public double Coef0;

    public double CacheSize;
    public double C;
    public double Eps;

    //public Map<Integer, Double> Weights;
    public ArrayList<Integer> Weights_Label;
    public ArrayList<Double> Weights;
    public double Nu;
    public double P;
    public boolean Shrinking;
    public boolean Probability;
    
    public Parameter()
    {
        SvmType = SvmType.C_SVC;
        KernelType = KernelType.RBF;
        Degree = 3;
        Gamma = 0; // 1/k
        Coef0 = 0;
        Nu = 0.5;
        CacheSize = 40;
        C = 1;
        Eps = 1e-3;
        P = 0.1;
        Shrinking = true;
        Probability = false;
        Weights = new ArrayList<Double>();
        Weights_Label = new ArrayList<Integer>();
    }
}
