package com.csri.ami.health_u.dataManagement.analyze.classifiers;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import SVM.Model;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;


public class SoundClassify 
{
	private static String SVM_FILE = "SVM";// "/Android/data/info.androidhive.materialtabs.QOL/Sound/SVM";
	//DD_Hyposthesis[][] hyps;
	Normalize norm;
	svm_model[] model;
	ArrayList<svm_model[]> models_oneVsAll;
	
	private static int[][] dt_subsets = new int[][]{{4,5,9,11,14,19,22},
									 {3,5,6,12,13,17,19,20,22,23,26},
									 {0,2,3,4,9,11,18,21,23,24}};
	
	//ArrayList<Double> distances;
	//ArrayList<String> classified_labels;
	
	public static String[] dt_labels = new String[]{"music","voice","ambient"};
	public double[] ClassificationResults;
	
	boolean UseOverVsAllSVM=false;//true <--changed 4-9-15;
	int numModels = 1;

	public SoundClassify(Context context)
	{
		//distances = new ArrayList<Double>();
		//classified_labels=  new ArrayList<String>();
		
		model = new svm_model[numModels];
		//hyps = new DD_Hyposthesis[numModels][];
		models_oneVsAll = new ArrayList<svm_model[]>();

		AssetManager am = context.getAssets();


		for(int i=0;i<numModels;i++)
		{


			ArrayList<svm_model> modelsCurrent = new ArrayList<svm_model>();
			int svm_id=0;
			String[] modelList=null;
			if(UseOverVsAllSVM) {
				try {

					modelList = am.list("");

					while (FileExists(modelList, SVM_FILE + i + "_" + svm_id + ".txt")) {
						InputStream is = am.open(SVM_FILE + i + "_" + svm_id + ".txt");
						svm_model current = Model.Read(is);//SVM_FILE + i + "_" + svm_id + ".txt");
						modelsCurrent.add(current);
						svm_id++;
					}
				} catch (IOException ioex) {
					String em = ioex.getMessage();
				}
				;

				svm_model[] modelsCurrent_a = new svm_model[modelsCurrent.size()];
				for (int m = 0; m < modelsCurrent.size(); m++) {
					modelsCurrent_a[m] = modelsCurrent.get(m);
				}
				models_oneVsAll.add(modelsCurrent_a);
			}
			else
			{
				try {

					modelList = am.list("");

					if (FileExists(modelList, SVM_FILE + i + "_" + 0 + ".txt")) {
						InputStream is = am.open(SVM_FILE + i + "_" + 0 + ".txt");
						svm_model current = Model.Read(is);//SVM_FILE + i + "_" + svm_id + ".txt");

						model[i] = current;
					}
				} catch (IOException ioex) {
					String em = ioex.getMessage();
				}


				//svm_model[] modelsCurrent_a = new svm_model[modelsCurrent.size()];
				//for (int m = 0; m < modelsCurrent.size(); m++) {
				//	modelsCurrent_a[m] = modelsCurrent.get(m);
				//}
				//models_oneVsAll.add(modelsCurrent_a);
			}
				

			//dt_labels = DD_Hyposthesis.LoadLabels(am);
			//hyps[i] = DD_Hyposthesis.Load(am);
		}
		//libsvm.svm_model s = new libsvm.svm_model();
		//LoadSVM();
		
		
		ClassificationResults = new double[dt_labels.length+1];//+1 is for additional class...the queit class which is classified by averageSound only..i.e volume
		
		norm = new Normalize(am);
	}

	private boolean FileExists(String[] assets,String modelName)
	{
		boolean found = false;
		if (assets != null)
		{
			for (int i = 0; i < assets.length; i++) {
				if (assets[i].compareTo(modelName) == 0) {
					found = true;
					break;
				}
			}
		}
		return found;
	}
	
	public void ClearClassificationResults()
	{
		if(ClassificationResults == null)
		{
			ClassificationResults = new double[dt_labels.length+1];
		}
		else
		{
			for(int i=0;i<ClassificationResults.length;i++)
			{
				ClassificationResults[i] =0;
			}
		}
	}
	
//	public void LoadSVM()
//	{
//		
//			boolean mExternalStorageAvailable = false;
//			boolean mExternalStorageWriteable = false;
//			String state = Environment.getExternalStorageState();
//
//			if (Environment.MEDIA_MOUNTED.equals(state)) {
//				// We can read and write the media
//				mExternalStorageAvailable = mExternalStorageWriteable = true;
//			} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
//				// We can only read the media
//				mExternalStorageAvailable = true;
//				mExternalStorageWriteable = false;
//			} else {
//				// Something else is wrong. It may be one of many other states, but all we need
//				//  to know is we can neither read nor write
//				mExternalStorageAvailable = mExternalStorageWriteable = false;
//			}
//		
//			if( mExternalStorageAvailable && mExternalStorageWriteable)
//			{
//				try
//				{
//					File f = Environment.getExternalStorageDirectory();
//			
//					String fullfilename = f.getAbsoluteFile() + "/Android/data/info.androidhive.materialtabs.QOL/Sound/";
//					File folderToWrite = new File(fullfilename);
//					folderToWrite.mkdirs();
//					fullfilename += SVM_FILE;
//					model = svm.svm_load_model(fullfilename);
//					//param =  model.param;
//				}
//				catch(Exception e){}
//			}
//		
//	}
	
	private Object[] GetDTFeatures(int dt_index,double[] raw)
	{
		ArrayList<Object> features = new ArrayList<Object>();
		
		int[] currentIndices = dt_subsets[dt_index];
		for(int i=0;i<raw.length;i++)
		{
//			boolean found = false;
//			for(int j=0;j<currentIndices.length;j++)
//			{
//				if(currentIndices[j] == i)
//				{
//					found = true;
//					break;
//				}
//			}
//			
//			if(found)
			{
				features.add((Object)raw[i]);
			}
		}
		
		Object[] features_a = new Object[features.size()];
		for(int i=0;i<features.size();i++)
		{
			features_a[i] = features.get(i);
		}
		
		return features_a;
	}
	
	private double MaxSound = -24;
	private double MinSound = -31;
	
//	public void Classify(SoundVector sound,int format)
//	{
//		if(hyps != null && norm != null && model !=null)
//		{
//			sound.RemoveSoundsBelowVolume(1);
//			ArrayList<SoundFeatureSummary> soundSummaries = SoundVector.ConvertSoundFeaturesToFeatureSummary(sound);
//
//
//
//			SoundFeatureSummary.PerformNormalize(soundSummaries, norm);
//
//			//double[] classifier_set_weights = new double[]{0.6,0.2,0.2};
//			double hyp_weight = 0.00;
//			double svm_weight = 1.0;
//			double dt_weight = 0.0;
//
//			int decidedClass = -1;
//			int all_maxIndex=-1;
//			double all_max = Double.MIN_VALUE;
//
//			double entropy_threshold = 0.85;
//			double dentropy_thresh_decrement = 0.05;
//
//			double[][] probabililityResults = new double[soundSummaries.size()][];
//
//			int quietFrames =0;
//			for(int i=0;i<soundSummaries.size();i++)
//			{
//
//				probabililityResults[i] = new double[dt_labels.length];
//				SoundFeatureSummary current = soundSummaries.get(i);
//
//				double Db = SpectralFeatures.Decibels(current.AverageRMS, format);
//
//				if(Db > MaxSound){MaxSound = Db;}
//				if(Db < MinSound){MinSound = Db;}
//
//				double SoundThreshold =  MinSound + ((MaxSound - MinSound) * 0.3);
//
//				if(Db <=  -40)
//				{
//					quietFrames++;
//					//ClassificationResults[dt_labels.length]++;
//				}
//				else
//				{
//
//
//
//					svm_node[] svm_f = new svm_node[current.featureVector.length];
//					for(int j=0;j<svm_f.length;j++)
//					{
//						svm_f[j] = new svm_node();
//						svm_f[j].index = j;
//						svm_f[j].value = current.featureVector[j];
//					}
//
//					double[][] hyp_probs_all = new double[numModels][];
//					double[][] svm_probs_all = new double[numModels][];
//					double[][] dt_probs_all = new double[numModels][];
//
//					for(int c=0;c<numModels;c++)
//					{
//						/////hyp classify/////
//						double[] hyp_probs = new double[dt_labels.length];
//						if(hyp_weight > 0)
//						{
//
//							//double[] hyp_dist = new double[hyps[c].length];
//							double sum =0;
//							for(int j=0;j<hyps[c].length;j++)
//							{
//
//								////hyp_dist[j] = DD_Hyposthesis.Distance(hyps[c][j],current);
//								hyp_probs[j] = DD_Hyposthesis.Probability(current, hyps[c][j]);
//								sum += hyp_probs[j];
//
//							}
//							for(int j=0;j<hyps[c].length;j++)
//							{
//								hyp_probs[j] = hyp_probs[j] / sum;
//							}
//
//							hyp_probs_all[c] = hyp_probs;
//
//							double maxHYP = Double.MIN_VALUE;
//							int maxHYP_class = -1;
//							for(int l=0;l<hyp_probs.length;l++)
//							{
//								if(hyp_probs[l] > maxHYP)
//								{
//									maxHYP = hyp_probs[l];
//									maxHYP_class = l;
//								}
//							}
//						}
//						////////////////////
//
//						//decision tree classify////////
//						double[] dt_probs = new double[dt_labels.length];
//						if(dt_weight > 0)
//						{
//							Object[] data_object = GetDTFeatures(c,current.featureVector);
//
//
//							double minProb= 0;
//							double maxProb = 1- (minProb * (dt_probs.length-1));
//							for(int l=0;l<dt_probs.length;l++)
//							{
//								dt_probs[l] = minProb;
//							}
//							int dt_class=-1;
//							try
//							{
//
//								dt_class = (int) WekaClassifier.classify(data_object);
//
//							}
//							catch (Exception e)
//							{
//								e.printStackTrace();
//							}
//							if(dt_class != -1)
//							{
//								if(dt_class == 1)
//								{dt_class =0;}
//								else if(dt_class == 0)
//								{dt_class = 1;}
//								dt_probs[dt_class]=maxProb;
//								dt_probs_all[c] = dt_probs;
//							}
//						}
//						///////////////////////////////
//
//						////svm classify///////////////
//						double[] svm_probs = new double[dt_labels.length];
//						if(!UseOverVsAllSVM)
//						{
//							if(model[c].param.probability == 1)
//							{
//								svm.svm_predict_probability(model[c], svm_f, svm_probs);
//								//if(svm_probs[0] > 0.8){svm_probs[0] = 1;svm_probs[1] = 0;}
//								//else{svm_probs[0] = 0;svm_probs[1] = 1;}
//								if(svm_probs[0] < 0.9993){svm_probs[0] = 0;svm_probs[1] =1;}
//
//							}
//							else
//							{
//								int svmClass = (int)svm.svm_predict(model[c], svm_f);
//								svm_probs[svmClass] = 1;
//							}
//						}
//						else
//						{
//							svm_probs = ClassifySVM(models_oneVsAll.get(c),svm_f);
//						}
//						svm_probs_all[c] = svm_probs;
//
//						double maxSVm = Double.MIN_VALUE;
//						int maxSVM_class = -1;
//						for(int l=0;l<svm_probs.length;l++)
//						{
//							if(svm_probs[l] > maxSVm)
//							{
//								maxSVm = svm_probs[l];
//								maxSVM_class = l;
//							}
//						}
//
//						double[] combined_probs = new double[dt_labels.length];
//
//						for(int l=0;l<svm_probs.length;l++)
//						{
//							combined_probs[l] = (hyp_probs[l] * hyp_weight)
//									+ (svm_probs[l] * svm_weight)
//									+ (dt_probs[l] * dt_weight);
//
//
//
//							if(combined_probs[l] > all_max)
//							{
//								all_maxIndex=l;
//								all_max = combined_probs[l];
//							}
//						}
//
//						probabililityResults[i] = combined_probs;
//					}//end for(int c=0;c<hyps.length;c++)
//
//				    //ClassificationResults[decidedClass]++;
//
//				}// end else (i.e if(current.AverageSound > 450))
//			}//end for(int i=0;i<soundSummaries.size();i++)
//
//			int windowRadius = 0;
//            double[][] smoothedResults = new double[probabililityResults.length][];
//            for (int j = 0; j < probabililityResults.length; j++)
//            {
//                smoothedResults[j] = new double[probabililityResults[j].length];
//            }
//			int[] results_index = new int[probabililityResults.length];
//			double[] results_prob = new double[probabililityResults.length];
//            for (int j = 0; j < probabililityResults.length; j++)
//            {
//                int currentMaxIndex = -1;
//                double max = Double.MIN_VALUE;
//                for (int k = 0; k < probabililityResults[j].length;k++ )
//                {
//                    int w_count = 0;
//                    double w_val = 0;
//                    for (int w = j - windowRadius; w <= j + windowRadius; w++)
//                    {
//                        if (w >= 0 && w < probabililityResults.length)
//                        {
//                            w_val += probabililityResults[w][k];
//                            w_count++;
//                        }
//                    }
//                    w_val = w_val / (double)w_count;
//                    smoothedResults[j][k] = w_val;
//
//                    if (w_val > max)
//                    {
//                        max = w_val;
//                        currentMaxIndex = k;
//                    }
//                }
//
//				results_index[j] = currentMaxIndex;
//				results_prob[j] = max;
//
////                if (currentMaxIndex != -1)
////                {
////                	ClassificationResults[currentMaxIndex]++;
////                }
//
//
//            }
//			double quietPercentage = (double)quietFrames /  (double)soundSummaries.size();
//			if(quietPercentage > 0.80)
//			{
//				ClassificationResults[dt_labels.length]++;
//			}
//			else
//			{
//				boolean voiceVsAllClassification = true;
//				if(voiceVsAllClassification) {
//					int keyResultIndex = 0;
//					int otherIndex =1;
//					int keyCount = 0;
//					double keyAvg = 0;
//					for (int i = 0; i < results_index.length; i++) {
//						if (results_index[i] == keyResultIndex) {
//							keyCount++;
//							keyAvg += results_prob[i];
//						}
//					}
//					keyAvg /= (double) keyCount;
//
//					double keyThreshold = 0.6;
//					double percentageKey = (double) keyCount / (double) (results_index.length - quietFrames);
//					double combined = percentageKey * keyAvg;
//					if (percentageKey > keyThreshold && keyAvg > 0.993) {
//						ClassificationResults[keyResultIndex]++;
//					}
//					else
//					{
//						ClassificationResults[otherIndex]++;
//					}
//				}
//				else
//				{
//					Map<Integer, Integer> resultsCount = new HashMap<Integer, Integer>();
//					ArrayList<Integer> uniqueValues = new ArrayList<Integer>();
//					double avg = 0;
//					for (int j = 0; j < results_index.length; j++) {
//						if (results_index[j] != -1) {
//							if (!resultsCount.containsKey(results_index[j])) {
//								resultsCount.put(results_index[j], 1);
//								uniqueValues.add(results_index[j]);
//							} else {
//								resultsCount.put(results_index[j], resultsCount.get(results_index[j]) + 1);//  .[results_index[j]]++;
//							}
//
//							avg += results_prob[j] / ((double) results_prob.length);
//						}
//					}
//					if (resultsCount.size() > 0) {
//	//				Dictionary<int, int> sortedDict = (from entry in resultsCount orderby entry.Value descending select entry).ToDictionary(pair => pair.Key, pair => pair.Value);
//	//				int count = 0;
//	//				for (int j = 0; j < sortedDict.Count; j++)
//	//				{
//	//					count += sortedDict.ElementAt(j).Value;
//	//				}
//						double total = results_index.length;
//
//						int maxValue = -1;
//						int maxKey = -1;
//						for (int i = 0; i < resultsCount.size(); i++) {
//							if (resultsCount.get(uniqueValues.get(i)) > maxValue) {
//								maxValue = resultsCount.get(uniqueValues.get(i));
//								maxKey = uniqueValues.get(i);
//							}
//						}
//
//						double percentageBest = (double) maxValue / (double) (total - quietFrames);
//						int index = maxKey;
//						double threshold = 0.7;// (double)5 / (double)7;
//						double avgProb =0;
//						double count=0;
//						for(int i=0;i<results_prob.length;i++)
//						{
//							if(results_index[i] == index)
//							{
//								avgProb+= results_prob[i];
//								count++;
//							}
//						}
//						avgProb /= count;
//						if (percentageBest >= threshold && avg > 0.01 && ((index == 0 && avgProb > 0.8) || index != 0) ) {
//							//seqCounts[t] += (1.0 / (double)allSounds[t].Length) * results_prob.Length;
//							//totalSamplesClassified += results_prob.Length;
//							//classificationPerformance[diaryLabelIndex][index] += results_prob.Length;
//							ClassificationResults[index]++;
//						}
//					}
//				}
//			}
//
//		}//end (hyps != null && norm != null && model !=null)
//
//
//	}

	private boolean Contains(ArrayList<Integer> list,int value)
	{
		boolean found = false;
		for(int i=0;i<list.size();i++)
		{
			if(list.get(i) == value)
			{
				found = true;
				break;
			}
		}
		return found;
	}
	
	public double[] ClassifySVM(svm_model[] models,svm_node[] f)
	{
		double[] probs = new double[models.length];
		double sum =0;
		
		for(int i=0;i<models.length;i++)
		{
			if(models[i].param.probability == 1)
			{
				double[] currentProb = new double[2];
				svm.svm_predict_probability(models[i], f, currentProb);
				probs[i] = currentProb[0];
				sum += probs[i];
			}
			else
			{
				double index = svm.svm_predict(models[i], f);
				if(index ==0)
				{
					probs[i] = 1;
					sum+=1;
				}
			}
		}
		
		{
            for (int i = 0; i < probs.length; i++)
            {
                probs[i] = probs[i] / sum;
            }
        }

        return probs;
	}
	
	public String ArrayToString(double[] data)
	{
		String line = "";
		
		for(int i=0;i<data.length;i++)
		{
			line += data[i] + ",";
		}
		return line;
	}
	
	public static double log2(double num)
	{
		return (Math.log(num)/Math.log(2));
	} 

	//old version...pre multiple classifier instances...i.e. only one svm used and only one set of hypothesis's
//	public void Classify(SoundVector sound)
//	{
//		if(hyps != null && norm != null && model !=null)
//		{
//			ArrayList<SoundFeatureSummary> soundSummaries = SoundVector.ConvertSoundFeaturesToFeatureSummary(sound);
//		    
//			
//			SoundFeatureSummary.PerformNormalize(soundSummaries, norm);
//			
//			for(int i=0;i<soundSummaries.size();i++)
//			{
//				SoundFeatureSummary current = soundSummaries.get(i);
//				
//				if(current.AverageSound < 450)
//				{
//					ClassificationResults[hyps.length]++;
//				}
//				else
//				{
//					Object[] data_object = new Object[current.featureVector.length];
//					for(int j=0;j<data_object.length;j++)
//					{
//						data_object[j] = (Object)current.featureVector[j];
//					}
//					
//					int minIndex=-1;
//					double minVal = Double.MAX_VALUE;
//					for(int j=0;j<hyps.length;j++)
//					{
//						
//						double dist = DD_Hyposthesis.Distance(hyps[j], current);
//						if(dist < minVal)
//						{
//							minIndex = j;
//							minVal = dist;
//						}
//					}
//					String hyp_label = hyps[minIndex].label.toLowerCase();
//					
//					double dtClass=-1;
//					try 
//					{
//						//dtClass = sound_j48.classify(data_object);
//						dtClass = Regression(current.featureVector);
//					} 
//					catch (Exception e) {
//						
//					}
//					
//					String dt_label ="";
//					if(dtClass != -1)
//					{
//						dt_label = dt_labels[(int)dtClass];
//					}
//					
//					
//					
//					svm_node[] f = new svm_node[current.featureVector.length];
//					for(int j=0;j<f.length;j++)
//					{
//						f[j] = new svm_node();
//						f[j].index = j;
//						f[j].value = current.featureVector[j];
//					}
//					
//					int svmClass = -1;
//					
//					
//					double maxProb = Double.MIN_VALUE;
//					if(model.param.probability == 1)
//					{
//						double[] svm_probs = new double[model.nr_class];
//						svm.svm_predict_probability(model, f,svm_probs);
//						
//						for(int j=0;j<svm_probs.length;j++)
//						{
//							if(svm_probs[j] > maxProb)
//							{
//								maxProb = svm_probs[j];
//								svmClass =j;
//							}
//						}
//					}
//					else
//					{
//						svmClass = (int)svm.svm_predict(model, f);
//						maxProb= (double)1 / (double)model.nr_class;
//					}
//					String svmLabel = dt_labels[svmClass];
//						
//					double prob_threshold = (double)1 / (double)model.nr_class;	
//					double temp = prob_threshold + (prob_threshold/2);
//					prob_threshold = temp;
//					
////					if(maxProb > prob_threshold)
////					{
////						ClassificationResults[svmClass]++;
////					}
////					else 
//					if(svmLabel.compareTo(dt_label) == 0)// || svmLabel.compareTo(hyp_label)==0)
//					{
//						ClassificationResults[svmClass]++ ;
//						//ClassificationResults[svmClass]++;
//					}
////					else if(hyp_label.compareTo(dt_label) == 0 && minVal < hyps[minIndex].threshold)
////					{
////						ClassificationResults[(int)dtClass]++;
////					}
//				}
//				
////				else
////				{
////					double[] probs = new double[model.nr_class];
////					svm.svm_predict_probability(model, f,probs);
////					for(int k=0;k<probs.length;k++)
////					{
////						ClassificationResults[k] += probs[k];
////					}
////				}
////				if(minIndex == svmClass || minIndex == dtClass)//dt_label.toLowerCase().compareTo(hyps[minIndex].label.toLowerCase()) == 0 && dtClass == svmClass)
////				{
////					ClassificationResults[minIndex]++;
////				}
////				else if(dtClass == svmClass)
////				{
////					ClassificationResults[(int)svmClass]++;
////				}
//			}
//		}
//		
//		
//	}
	
	private int Regression(double[] feature_)
    {
        double Class0 =
-2.18 + 
feature_[0] * -0.1 +
feature_[1] * 4.32 +
feature_[2] * -0.33 +
feature_[3] * -0.5 +
feature_[4] * 1.51 +
feature_[6] * -2.71 +
feature_[11] * 0.2  +
feature_[15] * -0.14 +
feature_[18] * -0.42 +
feature_[21] * -0.12 +
feature_[22] * 1.21 +
feature_[23] * 0.62 +
feature_[24] * 0.39 +
feature_[26] * 0.39 +
feature_[27] * -1.17;

        double Class1 =
0.3 +
feature_[2] * 0.24 +
feature_[6] * 0.58 +
feature_[9] * 1.49 +
feature_[13] * -0.45 +
feature_[14] * -1.18 +
feature_[15] * 0.61 +
feature_[17] * -0.17 +
feature_[21] * 0.28 +
feature_[22] * -1.6 +
feature_[23] * -0.88 +
feature_[25] * 0.75 +
feature_[26] * -0.24;

        double Class2 =
-2.96 +
feature_[0] * 2.5 +
feature_[1] * -1.01 +
feature_[3] * 0.55 +
feature_[4] * -0.2 +
feature_[5] * 0.84 +
feature_[6] * 0.22 +
feature_[11] * -5.5 +
feature_[18] * 0.28 +
feature_[19] * -0.82 +
feature_[20] * 0.41 +
feature_[24] * -2.47 +
feature_[27] * 0.45;


        double[] classRes = new double[]{Class0,Class1,Class2};
        double[] p = new double[classRes.length];
        double exp_sum = 0;
        for (int i = 0; i < classRes.length; i++)
        {
            exp_sum += Math.exp(classRes[i]);
        }
        double max = 0;
        int maxIndex =-1;
        for (int i = 0; i < classRes.length; i++)
        {
            p[i] = Math.exp(classRes[i]) / exp_sum;
            if (p[i] > max)
            {
                max = p[i];
                maxIndex = i;

            }
        }
        

        return maxIndex;
    }
}
