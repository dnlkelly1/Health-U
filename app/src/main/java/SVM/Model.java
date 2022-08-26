package SVM;

import android.os.Environment;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;

public class Model 
{
	public Parameter Parameter;
    public int NumberOfClasses;
    public int SupportVectorCount;
    public Node[][] SupportVectors;
    public double[][] SupportVectorCoefficients;
    public double[] Rho;
    public double[] PairwiseProbabilityA;
    public double[] PairwiseProbabilityB;

    public int[] ClassLabels;
    public int[] NumberOfSVPerClass;
    
    public Model()
    {
    	
    }
    
    public static svm_model Convert(Model m)
    {
    	libsvm.svm_model s = new libsvm.svm_model();
    	s.param = new svm_parameter();
    	s.param.C = m.Parameter.C;
    	s.param.cache_size = m.Parameter.CacheSize;
    	s.param.coef0 = m.Parameter.Coef0;
    	s.param.degree = m.Parameter.Degree;
    	s.param.eps = m.Parameter.Eps;
    	s.param.gamma = m.Parameter.Gamma;
    	s.param.kernel_type = m.Parameter.KernelType.getValue();
    	s.param.svm_type = m.Parameter.SvmType.getValue();
    	s.param.nr_weight = m.Parameter.Weights.size();
    	s.param.nu = m.Parameter.Nu;
    	s.param.p = m.Parameter.P;
    	s.l = m.SupportVectorCount;
    	
    	if(m.Parameter.Shrinking)
    		s.param.shrinking = 1;
    	else
    		s.param.shrinking = 0;
    	if(m.Parameter.Probability)
    		s.param.probability = 1;
    	else
    		s.param.probability = 0;
    	
    	
    	s.param.weight = new double[m.Parameter.Weights.size()];
    	s.param.weight_label = new int[m.Parameter.Weights.size()];
    	for(int i=0;i<m.Parameter.Weights.size();i++)
    	{
    		s.param.weight[i] = m.Parameter.Weights.get(i);
    		s.param.weight_label[i] = m.Parameter.Weights_Label.get(i);
    	}
    	
    	s.label = m.ClassLabels;
    	s.nr_class = m.NumberOfClasses;
    	s.nSV = m.NumberOfSVPerClass;
    	s.probA = m.PairwiseProbabilityA;
    	s.probB = m.PairwiseProbabilityB;
    	s.rho = m.Rho;
    	
    	
    	s.sv_coef = m.SupportVectorCoefficients;
    	
    	
    	s.SV = new svm_node[m.SupportVectors.length][];
    	
    	
    	for(int i=0;i<s.SV.length;i++)
    	{
    		s.SV[i] = new svm_node[m.SupportVectors[i].length];
    		for(int j=0;j<s.SV[i].length;j++)
    		{
    			s.SV[i][j] = new svm_node();
    			s.SV[i][j].index = m.SupportVectors[i][j].Index;
    			s.SV[i][j].value = m.SupportVectors[i][j].Value;
    		}
    	}
    	
    	return s;
    }
    
    public static boolean ModelExists(String file)
    {
    	File f = Environment.getExternalStorageDirectory();
		
		String fullfilename = f.getAbsoluteFile() + file ;
		//File folderToRead = new File(fullfilename);
		//String t = sensorID.replace(':', '_');
		File fileToRead = new File(fullfilename);
		return fileToRead.exists();
    }
    public static svm_model Read(String file)
    {
    	ArrayList<Integer> keys = new ArrayList<Integer>();
    	ArrayList<Double> weights = new ArrayList<Double>();
    	
    	Model model = new Model();
    	Parameter param = new Parameter();
    	model.Rho = null;
        model.PairwiseProbabilityA = null;
        model.PairwiseProbabilityB = null;
        model.ClassLabels = null;
        model.NumberOfSVPerClass = null;
        
        boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		if( mExternalStorageAvailable && mExternalStorageWriteable)
		{
			try
			{
				
				File f = Environment.getExternalStorageDirectory();
				
				String fullfilename = f.getAbsoluteFile() + file ;
				//File folderToRead = new File(fullfilename);
				//String t = sensorID.replace(':', '_');
				File fileToRead = new File(fullfilename);
				
				FileInputStream fis = new FileInputStream(fileToRead);
				InputStreamReader isw = new InputStreamReader(fis);
				Scanner scanner = new Scanner(fileToRead,"UTF-8");
				
				String line = "";// scanner.nextLine();
				boolean headerError =false;
				try
				{
					boolean headerFinished = false;
					while (!headerFinished)
					{
						line = scanner.nextLine();
						String cmd, arg;
						int splitIndex = line.indexOf(' ');
						if (splitIndex >= 0)
		                {
		                    cmd = line.substring(0, splitIndex);
		                    arg = line.substring(splitIndex + 1);
		                }
		                else
		                {
		                    cmd = line;
		                    arg = "";
		                }
						arg = arg.toLowerCase();
						int i,n;
						

						if(0 == cmd.compareTo("probability") )
	                    {
	                        param.Probability = Boolean.parseBoolean(arg);
	                    }
	                        

	                    else if(0 == cmd.compareTo("key"))
	                    {
	                        keys.add(Integer.parseInt(arg));
	                    }
	                        

	                    else if(0 == cmd.compareTo("weight"))
	                    {
	                        weights.add(Double.parseDouble(arg));
	                    }
	                       

	                    else if(0 == cmd.compareTo("svm_type"))
	                    {
	                    	if(arg.toUpperCase().compareTo("C_SVC") == 0)
	                    		param.SvmType = SvmType.C_SVC;
	                    	else if(arg.toUpperCase().compareTo("NU_SVC") == 0)
	                    		param.SvmType = SvmType.NU_SVC;
	                    	else if(arg.toUpperCase().compareTo("ONE_CLASS") == 0)
	                    		param.SvmType = SvmType.ONE_CLASS;
	                    	else if(arg.toUpperCase().compareTo("EPSILON_SVR") == 0)
	                    		param.SvmType = SvmType.EPSILON_SVR;
	                    	else if(arg.toUpperCase().compareTo("NU_SVR") == 0)
	                    		param.SvmType = SvmType.NU_SVR;
	                        
	                    }
	                       
	                        
	                    else if(0 == cmd.compareTo("kernel_type"))
	                    {
	                    	if(arg.toUpperCase().compareTo("LINEAR") == 0)
	                    		param.KernelType = KernelType.LINEAR;
	                    	else if(arg.toUpperCase().compareTo("POLY") == 0)
	                    		param.KernelType = KernelType.POLY;
	                    	else if(arg.toUpperCase().compareTo("PRECOMPUTED") == 0)
	                    		param.KernelType = KernelType.PRECOMPUTED;
	                    	else if(arg.toUpperCase().compareTo("RBF") == 0)
	                    		param.KernelType = KernelType.RBF;
	                    	else if(arg.toUpperCase().compareTo("SIGMOID") == 0)
	                    		param.KernelType = KernelType.SIGMOID;
	                    }
	                       

	                    else if(0 == cmd.compareTo("degree"))
	                    {
	                        param.Degree = Integer.parseInt(arg);
	                    }
	                       

	                    else if(0 == cmd.compareTo("gamma"))
	                    {
	                        param.Gamma =Double.parseDouble(arg);
	                    }
	                      

	                    else if(0 == cmd.compareTo("coef0"))
	                    {
	                        param.Coef0 = Double.parseDouble(arg);
	                    }
	                       

	                    else if(0 == cmd.compareTo("nr_class"))
	                    {
	                        model.NumberOfClasses = Integer.parseInt(arg);
	                    }
	                        

	                    else if(0 == cmd.compareTo("total_sv"))
	                    {
	                        model.SupportVectorCount = Integer.parseInt(arg);
	                    }
	                       

	                    else if(0 == cmd.compareTo("rho"))
	                    {
	                        n = model.NumberOfClasses * (model.NumberOfClasses - 1) / 2;
	                        model.Rho = new double[n];
	                        String[] rhoParts = arg.split(" ");
	                        for(i=0; i<n; i++)
	                            model.Rho[i] = Double.parseDouble(rhoParts[i]);
	                    }
	                        

	                    else if(0 == cmd.compareTo("label"))
	                    {
	                        n = model.NumberOfClasses;
	                        model.ClassLabels = new int[n];
	                        String[] labelParts = arg.split(" ");
	                        for (i = 0; i < n; i++)
	                            model.ClassLabels[i] = Integer.parseInt(labelParts[i]);
	                    }
	                        

	                    else if(0 == cmd.compareTo("probA"))
	                    {
	                        n = model.NumberOfClasses * (model.NumberOfClasses - 1) / 2;
	                        model.PairwiseProbabilityA = new double[n];
	                            String[] probAParts = arg.split(" ");
	                        for (i = 0; i < n; i++)
	                            model.PairwiseProbabilityA[i] = Double.parseDouble(probAParts[i]);
	                    }
	                        

	                    else if(0 == cmd.compareTo("probB"))
	                    {
	                        n = model.NumberOfClasses * (model.NumberOfClasses - 1) / 2;
	                        model.PairwiseProbabilityB = new double[n];
	                        String[] probBParts = arg.split(" ");
	                        for (i = 0; i < n; i++)
	                            model.PairwiseProbabilityB[i] = Double.parseDouble(probBParts[i]);
	                    }
	                       

	                    else if(0 == cmd.compareTo("nr_sv"))
	                    {
	                        n = model.NumberOfClasses;
	                        model.NumberOfSVPerClass = new int[n];
	                        String[] nrsvParts = arg.split(" ");
	                        for (i = 0; i < n; i++)
	                            model.NumberOfSVPerClass[i] = Integer.parseInt(nrsvParts[i]);
	                    }
	                        

	                    else if(0 == cmd.compareTo("SV"))
	                    {
	                        headerFinished = true;
	                    }
	                       
	                    else
	                    {
	                    	headerError = true;
	                    }
	                }
						
					param.Weights = new ArrayList<Double>();
					param.Weights_Label = new ArrayList<Integer>();
					for (int i = 0; i < keys.size(); i++)
		            {
						param.Weights.add(weights.get(i));
						param.Weights_Label.add(keys.get(i));
		            }	
					
					int m = model.NumberOfClasses - 1;
		            int l = model.SupportVectorCount;
		            model.SupportVectorCoefficients = new double[m][];
		            for (int i = 0; i < m; i++)
		            {
		                model.SupportVectorCoefficients[i] = new double[l];
		            }
		            model.SupportVectors = new Node[l][];
		            
		            for (int i = 0; i < l; i++)
		            {
		            	line = scanner.nextLine();
		            	line = line.trim();
		            	String[] parts = line.split(" ");
		               

		                for (int k = 0; k < m; k++)
		                    model.SupportVectorCoefficients[k][i] = Double.parseDouble(parts[k]);
		                int n = parts.length-m;
		                model.SupportVectors[i] = new Node[n];
		                for (int j = 0; j < n; j++)
		                {
		                    String[] nodeParts = parts[m + j].split(":");
		                    model.SupportVectors[i][j] = new Node();
		                    model.SupportVectors[i][j].Index =Integer.parseInt(nodeParts[0]);
		                    model.SupportVectors[i][j].Value = Double.parseDouble(nodeParts[1]);
		                }
		            }
					
				}
				catch(NoSuchElementException exc)
				{
					
				}
				model.Parameter = param;
				svm_model svm = Convert(model);
				
				return svm;
			}
			catch(IOException e)
			{
				return null;
			}
		}
		else 
		{
			return null;
		}
    }

	public static svm_model Read(InputStream file)
	{
		ArrayList<Integer> keys = new ArrayList<Integer>();
		ArrayList<Double> weights = new ArrayList<Double>();

		Model model = new Model();
		Parameter param = new Parameter();
		model.Rho = null;
		model.PairwiseProbabilityA = null;
		model.PairwiseProbabilityB = null;
		model.ClassLabels = null;
		model.NumberOfSVPerClass = null;

		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but all we need
			//  to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}

		if( mExternalStorageAvailable && mExternalStorageWriteable)
		{
			try
			{

				//File f = Environment.getExternalStorageDirectory();

				//String fullfilename = f.getAbsoluteFile() + file ;
				//File folderToRead = new File(fullfilename);
				//String t = sensorID.replace(':', '_');
				//File fileToRead = new File(fullfilename);

				//FileInputStream fis = new FileInputStream(fileToRead);
				//InputStreamReader isw = new InputStreamReader(fis);
				BufferedReader scanner = new BufferedReader(new InputStreamReader(file,"UTF-8"));
				//Scanner scanner = new Scanner(file,"UTF-8");

				String line = "";// scanner.nextLine();
				boolean headerError =false;
				try
				{
					boolean headerFinished = false;
					while (!headerFinished)
					{
						line = scanner.readLine();
						String cmd, arg;

						Iterable<String> s = Splitter.on(' ').split(line);
						String[] line_split = Iterables.toArray(s, String.class);

						//int splitIndex = line.indexOf(' ');
						if (line_split.length > 1)
						{
							cmd = line_split[0];// line.substring(0, splitIndex);

							arg = line_split[1];//line.substring(splitIndex + 1);

							if(line_split.length > 2)
							{
								for(int i=2;i<line_split.length;i++)
								{
									arg += " " + line_split[i];
								}
							}

						}
						else
						{
							cmd = line_split[0];//line;
							arg = "";
						}
						arg = arg.toLowerCase();
						int i,n;


						if(0 == cmd.compareTo("probability") )
						{
							param.Probability = Boolean.parseBoolean(arg);
						}


						else if(0 == cmd.compareTo("key"))
						{
							keys.add(Integer.parseInt(arg));
						}


						else if(0 == cmd.compareTo("weight"))
						{
							weights.add(Double.parseDouble(arg));
						}


						else if(0 == cmd.compareTo("svm_type"))
						{
							if(arg.toUpperCase().compareTo("C_SVC") == 0)
								param.SvmType = SvmType.C_SVC;
							else if(arg.toUpperCase().compareTo("NU_SVC") == 0)
								param.SvmType = SvmType.NU_SVC;
							else if(arg.toUpperCase().compareTo("ONE_CLASS") == 0)
								param.SvmType = SvmType.ONE_CLASS;
							else if(arg.toUpperCase().compareTo("EPSILON_SVR") == 0)
								param.SvmType = SvmType.EPSILON_SVR;
							else if(arg.toUpperCase().compareTo("NU_SVR") == 0)
								param.SvmType = SvmType.NU_SVR;

						}


						else if(0 == cmd.compareTo("kernel_type"))
						{
							if(arg.toUpperCase().compareTo("LINEAR") == 0)
								param.KernelType = KernelType.LINEAR;
							else if(arg.toUpperCase().compareTo("POLY") == 0)
								param.KernelType = KernelType.POLY;
							else if(arg.toUpperCase().compareTo("PRECOMPUTED") == 0)
								param.KernelType = KernelType.PRECOMPUTED;
							else if(arg.toUpperCase().compareTo("RBF") == 0)
								param.KernelType = KernelType.RBF;
							else if(arg.toUpperCase().compareTo("SIGMOID") == 0)
								param.KernelType = KernelType.SIGMOID;
						}


						else if(0 == cmd.compareTo("degree"))
						{
							param.Degree = Integer.parseInt(arg);
						}


						else if(0 == cmd.compareTo("gamma"))
						{
							param.Gamma =Double.parseDouble(arg);
						}


						else if(0 == cmd.compareTo("coef0"))
						{
							param.Coef0 = Double.parseDouble(arg);
						}


						else if(0 == cmd.compareTo("nr_class"))
						{
							model.NumberOfClasses = Integer.parseInt(arg);
						}


						else if(0 == cmd.compareTo("total_sv"))
						{
							model.SupportVectorCount = Integer.parseInt(arg);
						}


						else if(0 == cmd.compareTo("rho"))
						{
							n = model.NumberOfClasses * (model.NumberOfClasses - 1) / 2;
							model.Rho = new double[n];
							String[] rhoParts = arg.split(" ");
							for(i=0; i<n; i++)
								model.Rho[i] = Double.parseDouble(rhoParts[i]);
						}


						else if(0 == cmd.compareTo("label"))
						{
							n = model.NumberOfClasses;
							model.ClassLabels = new int[n];
							String[] labelParts = arg.split(" ");
							for (i = 0; i < n; i++)
								model.ClassLabels[i] = Integer.parseInt(labelParts[i]);
						}


						else if(0 == cmd.compareTo("probA"))
						{
							n = model.NumberOfClasses * (model.NumberOfClasses - 1) / 2;
							model.PairwiseProbabilityA = new double[n];
							String[] probAParts = arg.split(" ");
							for (i = 0; i < n; i++)
								model.PairwiseProbabilityA[i] = Double.parseDouble(probAParts[i]);
						}


						else if(0 == cmd.compareTo("probB"))
						{
							n = model.NumberOfClasses * (model.NumberOfClasses - 1) / 2;
							model.PairwiseProbabilityB = new double[n];
							String[] probBParts = arg.split(" ");
							for (i = 0; i < n; i++)
								model.PairwiseProbabilityB[i] = Double.parseDouble(probBParts[i]);
						}


						else if(0 == cmd.compareTo("nr_sv"))
						{
							n = model.NumberOfClasses;
							model.NumberOfSVPerClass = new int[n];
							String[] nrsvParts = arg.split(" ");
							for (i = 0; i < n; i++)
								model.NumberOfSVPerClass[i] = Integer.parseInt(nrsvParts[i]);
						}


						else if(0 == cmd.compareTo("SV"))
						{
							headerFinished = true;
						}

						else
						{
							headerError = true;
						}
					}

					param.Weights = new ArrayList<Double>();
					param.Weights_Label = new ArrayList<Integer>();
					for (int i = 0; i < keys.size(); i++)
					{
						param.Weights.add(weights.get(i));
						param.Weights_Label.add(keys.get(i));
					}

					int m = model.NumberOfClasses - 1;
					int l = model.SupportVectorCount;
					model.SupportVectorCoefficients = new double[m][];
					for (int i = 0; i < m; i++)
					{
						model.SupportVectorCoefficients[i] = new double[l];
					}
					model.SupportVectors = new Node[l][];


					for (int i = 0; i < l; i++)
					{
						line = scanner.readLine();
						//line = line.trim();

						Iterable<String> s = Splitter.on(' ').split(line);
						String[] parts = Iterables.toArray(s, String.class);

						//String[] parts = line.split(" ");


						for (int k = 0; k < m; k++)
							model.SupportVectorCoefficients[k][i] = Double.parseDouble(parts[k]);
						int n = parts.length-m;
						model.SupportVectors[i] = new Node[n];
						for (int j = 0; j < n; j++)
						{
							int index = parts[m+j].indexOf(':');
							String a = parts[m+j].substring(0,index);
							String b = parts[m+j].substring(index+1,parts[m+j].length());

							//String[] nodeParts = parts[m + j].split(":");
							//Iterable<String> np = Splitter.on(':').split(parts[m + j]);
							//String[] nodeParts = Iterables.toArray(np, String.class);
							model.SupportVectors[i][j] = new Node();
							model.SupportVectors[i][j].Index =Integer.parseInt(a);
							model.SupportVectors[i][j].Value = Double.parseDouble(b);
						}
					}

				}
				catch(NoSuchElementException exc)
				{

				}
				model.Parameter = param;
				svm_model svm = Convert(model);

				return svm;
			}
			catch(Exception e)
			{
				return null;
			}
		}
		else
		{
			return null;
		}
	}
}
