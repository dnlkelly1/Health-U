package com.csri.ami.health_u.dataManagement.analyze.classifiers;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TestClassify 
{
	//ArrayList<double[]> features;
	//ArrayList<String> labels;

	BufferedReader scanner;
	MotionClassifier classifier;

	public TestClassify()
	{
		//String filename = "DOL_T_hmmtest.arff";
		//LoadARFFFile(filename);
		//classifier = new MotionClassifier("test");
		//Test();
	}

	public void Test()
	{

		if(scanner != null)
		{
			ActivityDescriptor descriptor_HMM = new ActivityDescriptor(ClassifierType.Combined, "Test", 1000 * 60);
			ActivityDescriptor descriptor = new ActivityDescriptor(ClassifierType.Combined, "Test", 1000 * 60);
			ActivityDescriptor descriptor_GT = new ActivityDescriptor(ClassifierType.Combined, "Test", 1000 * 60);
			int[][] confusion = new int[descriptor_GT.ACTIVITIES.length][];
			for(int i=0;i<confusion.length;i++)
			{
				confusion[i] = new int[descriptor_GT.ACTIVITIES.length];
			}
			int count = 0;
			try
			{

				String line = scanner.readLine();
				int startLine = 0;
				int maxNumFrames = 400000;

				int correct =0;
				
				while(line != null && count < maxNumFrames + startLine)
				{
					if(count >= startLine)
					{
						String[] dataStrings = line.split(",");
						double[] data = new double[dataStrings.length-1];
						for(int i=0;i<dataStrings.length-1;i++)
						{
							data[i] = Double.parseDouble(dataStrings[i]);
						}

						String label = dataStrings[dataStrings.length-1];
						if(label.compareTo("strsdwn_t") == 0)
						{
							int x = 1;
							//Log.v("TestClassfiy", label);
						}

						Log.v("TestClassfiy", Integer.toString(count) + " " + label);

						Object[] t_features = MotionFeatures.ExtractFeaturesObject(data);

						int currentClass = 0;//classifier.Classify(t_features);

						if(currentClass != -1)
						{
							//if(currentClasses != null)
							//{
							descriptor.UpdateClassificationRecord(currentClass);
							//}

							int GT_index = descriptor_GT.ActivityIndex(label);
							descriptor_GT.UpdateClassificationRecord(GT_index);

							confusion[GT_index][currentClass]++;
						}

					}

					line = scanner.readLine();

					count++;
				}
			}
			catch(IOException e)
			{

			}
			
			
			
			int[] groundTruthClassification = descriptor_GT.GetClassificationRecords();
			int[] classifications = descriptor.GetClassificationRecords();
			int[] hmm_classifications = classifications;// classifier.HMM_Adjust(classifications);
			descriptor_HMM.UpdateClassificationRecord(hmm_classifications);
			
			int[][] hmmConfusion = GetConfusionMatrix(groundTruthClassification,hmm_classifications,descriptor_HMM.ACTIVITIES.length);
			
			//ActivitySpan[] gt_Spans = ConvertToActivitySpans(descriptor_GT.GetClassificationRecords());
			//ActivitySpan[] hmm_Spans = ConvertToActivitySpans(hmm_classifications);
			
			//ErrorMeasures e = ComputeErrorMeasures(gt_Spans,hmm_Spans);

			int hmm_acc = CountSame(groundTruthClassification,hmm_classifications);
			int acc = CountSame(groundTruthClassification,classifications);

			boolean append = false;
			descriptor_HMM.Save_ClassificationRecord("HMM_Classification.csv",append);
			descriptor.Save_ClassificationRecord("Classifications.csv",append);
			descriptor_GT.Save_ClassificationRecord("GroundTruths.csv",append);

			descriptor.Save_String("Accuracy_H.txt", Integer.toString(count) + "->  Hmm: " + Integer.toString(hmm_acc) + "  Acc: " + Integer.toString(acc),confusion, append);
			descriptor.Save_String("Accuracy_Hmm_H.txt", Integer.toString(count) + "->  Hmm: " + Integer.toString(hmm_acc) + "  Acc: " + Integer.toString(acc),hmmConfusion, append);
		}
	}
	
	private int[][] GetConfusionMatrix(int[] GT,int[] Classifications,int size)
	{
		int[][] matrix = new int[size][];
		
		for(int i=0;i<size;i++)
		{
			matrix[i] = new int[size];
		}
		
		for(int i=0;i<GT.length;i++)
		{
			matrix[GT[i]][Classifications[i]]++;
		}
		
		return matrix;
	}
	
	private ErrorMeasures ComputeErrorMeasures(ActivitySpan[] groundTruths,ActivitySpan[] classifieds)
	{
		ErrorMeasures e = new ErrorMeasures();
		for(int i=0;i<classifieds.length;i++)
		{
			ActivitySpan[] overlaps = GetOverlapping(groundTruths,classifieds[i]);
			boolean found = false;
			for(int j=0;j<overlaps.length;j++)
			{
				if(overlaps[j].ActivityIndex == classifieds[i].ActivityIndex)
				{
					found = true;
				}
			}
			
			if(found)
			{
				e.Correct++;
			}
			else 
			{
				e.Substitutions++;
				
			}
		}
		return e;
	}
	
	private ActivitySpan[] GetOverlapping(ActivitySpan[] groundTruths,ActivitySpan current)
	{
		List<ActivitySpan> activities = new ArrayList<ActivitySpan>();
		for(int i=0;i<groundTruths.length;i++)
		{
			if(groundTruths[i].Overlaps(current))
			{
				activities.add(groundTruths[i]);
			}
		}
		
		ActivitySpan[] spans = new ActivitySpan[activities.size()];
		
		for(int i=0;i<spans.length;i++)
		{
			spans[i] = activities.get(i); 
		}
		
		return spans;
	}
	
	private ActivitySpan[] ConvertToActivitySpans(int[] frameClassifications)
	{
		List<ActivitySpan> activities = new ArrayList<ActivitySpan>();
		ActivityDescriptor descriptor = new ActivityDescriptor(ClassifierType.Combined, "Test", 1000 * 60);
		
		int currentActivityIndex = frameClassifications[0];
		ActivitySpan current = new ActivitySpan();
		current.ActivityIndex = currentActivityIndex;
		current.StartTime = 0;
		current.Activity = descriptor.ACTIVITIES[currentActivityIndex];
		
		for(int i=1;i<frameClassifications.length;i++)
		{
			if(frameClassifications[i] != current.ActivityIndex || i + 1 >= frameClassifications.length)
			{
				current.EndTime = i-1;
				activities.add(current);
				
				current = new ActivitySpan();
				current.ActivityIndex = frameClassifications[i];
				current.StartTime = i;
				current.Activity = descriptor.ACTIVITIES[current.ActivityIndex];
			}
		}
		
		ActivitySpan[] spans = new ActivitySpan[activities.size()];
		
		for(int i=0;i<spans.length;i++)
		{
			spans[i] = activities.get(i); 
		}
		
		return spans;
	}

	private int CountSame(int[] a,int[] b)
	{
		int count =0;
		for(int i=0;i<a.length;i++)
		{
			if(a[i] == b[i])
			{
				count++;
			}
		}
		return count;
	}

	private void LoadARFFFile(String filename)
	{
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

				String fullfilename = f.getAbsoluteFile() + "/Android/data/info.androidhive.materialtabs.QOL/" ;
				File folderToRead = new File(fullfilename);
				//String t = sensorID.replace(':', '_');
				File fileToRead = new File(fullfilename,filename);
				FileInputStream fis = new FileInputStream(fileToRead);
				InputStreamReader isw = new InputStreamReader(fis); 

				scanner = new BufferedReader(new FileReader(fileToRead));
				//scanner = new Scanner(fileToRead,"UTF-8");

				String line = scanner.readLine();

				while(line.compareTo("@DATA") != 0)
				{
					line = scanner.readLine();
				}

				//features = new ArrayList<double[]>();
				//labels = new ArrayList<String>();

				//				line = scanner.nextLine();
				//				while(line != null)
				//				{
				//					String[] dataStrings = line.split(",");
				//					double[] data = new double[dataStrings.length];
				//					for(int i=0;i<dataStrings.length-1;i++)
				//					{
				//						data[i] = Double.parseDouble(dataStrings[i]);
				//					}
				//					features.add(data);
				//					labels.add(dataStrings[dataStrings.length-1]);
				//
				//					line = scanner.nextLine();
				//				}

			}
			catch(IOException e)
			{

			}
		}
		else 
		{

		}
	}

}
