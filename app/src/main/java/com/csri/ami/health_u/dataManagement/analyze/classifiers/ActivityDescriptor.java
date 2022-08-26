package com.csri.ami.health_u.dataManagement.analyze.classifiers;

import android.os.Environment;
import android.os.SystemClock;
import android.text.format.Time;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class ActivityDescriptor {

	public double[] PI;
	public double[][] Transitions;
	public String[] ACTIVITIES;// = new String[]{"STAND","WALK","TRANSDOWN","SIT","TRANSUP"};
	public double[][] ACTIVITIES_PROBS;// = new double[][]{
//		{ 9044,   91, 1300,  765,  647},
//		{  222, 4856,    3,    5,  113},
//		{ 271,   26, 2533,   92,   79},
//		{   0,    0,  374, 4410,  143},
//		{  139,    3,   61,  142, 2263 }
//	};
	private String User = "";
	private long[] ActivityDurations;
	private long[] ActivityDurations_Saving;
	private DateTime time;
	private DateTime date;
	private int timeStamp;
	
	private ArrayList<int[]> classifiationRecord;

	public boolean CurrentlySaving = false;
	public boolean CurrentSendingToServer = false;
	long MaxTime = 0;

	public ActivityDescriptor (ClassifierType classifiertype,String user,long maxTime)
	{
		if(classifiertype == ClassifierType.PositionClasifier)
		{
			InitPositionClassifier();
		}
		else if(classifiertype == ClassifierType.LowerBodyClassifier)
		{
			InitLowerBodyClassifier();
		}
		else if(classifiertype == ClassifierType.TorsoClassifier)
		{
			InitTorsoClassifier();
		}
		else if(classifiertype == ClassifierType.Overall)
		{
			InitOverallClassifier();
			
		}
		else if(classifiertype == ClassifierType.Combined)
		{
			InitCombinedClassifier();
			classifiationRecord = new ArrayList<int[]>();
		}
		MaxTime = maxTime;
		User = user;
		double milliseconds = System.currentTimeMillis() ;//  SystemClock.elapsedRealtime();// (double)(System.nanoTime()) / 1000000;
		timeStamp = (int)milliseconds;
		time = new DateTime();
		time = DateTime.now();
		date = new DateTime();
		date = DateTime.now();
		ActivityDurations = new long[ACTIVITIES.length];
	}
	
	public void UpdateClassificationRecord(int classification)
	{
		int[] currentClassification = new int[ACTIVITIES.length];
		currentClassification[classification] = 1;
		 
		classifiationRecord.add(currentClassification);
	}
	
	int[] GetClassificationRecords()
	{
		int[] classifications = new int[classifiationRecord.size()];
		
		for(int i=0;i<classifications.length;i++)
		{
			classifications[i] = GetClassification(classifiationRecord.get(i));
		}
		
		return classifications;
	}
	
	private int GetClassification(int[] classes)
	{
		int classification = -1;
		for(int i=0;i<classes.length;i++)
		{
			if(classes[i] != 0)
			{
				classification = i;
				break;
			}
		}
		return classification;
	}
	
	public void UpdateClassificationRecord(int[] classification)
	{
		for(int i=0;i<classification.length;i++)
		{
			int[] currentClassification = new int[ACTIVITIES.length];
			currentClassification[classification[i]] = 1;
		 
			classifiationRecord.add(currentClassification);
		}
	}
	
	private void InitPositionClassifier()
	{
		ACTIVITIES = new String[]{"_other","walking_t","walking_h"};
		ACTIVITIES_PROBS = new double[][]{
			{ 21536,  1123,   606},
			{ 276,  7064,    32 },
			{ 867,   674,  9089}
		};
		
		PI = new double[]{0.6,0.2,0.2};
		
		Transitions = new double[][]{
				{ 0.995,0.0025,0.0025},
				{  0.005,0.995,0.0},
				{ 0.005,0.00,0.995}
			};
	}
	
	private void InitLowerBodyClassifier()
	{
		ACTIVITIES = new String[]{"stnding_t","strsupx_t","trnsdwn_t","sitting_t","transup_t","strsdwn_t"};
		ACTIVITIES_PROBS = new double[][]{
			{ 3086,28,4,39,0,10},
			{ 30,864,102,22,0,0},
			{ 0,60,2193,49,0,0},
			{ 84,168,35,813,0,0},
			{ 0,0,0,0,1891,95},
			{ 0,0,0,0,10,1907}
		};
		
		PI = new double[]{0.45,0.01,0.45,0.01,0.04,0.04};
		
		Transitions = new double[][]{
				{ 0.98,0.1,0,0,0.05,0.5},
				{  0,0.3,0.7,0,0,0},
				{ 0,0,0.98,0.2,0,0},
				{  0.7,0,0,0.3,0,0},
				{ 0.2,0,0,0,0.8,0},
				{ 0.2,0,0,0,0.0,0.8},
			};
	}
	
	private void InitOverallClassifier()
	{
		ACTIVITIES = new String[]{"stnding_t","trnsdwn_t","sitting_t","transup_t","strsupx_t","strsdwn_t","walking_t"};
		ACTIVITIES_PROBS = new double[][]{
			{ 3086,28,4,39,0,0,0},
			{ 0,1,0,0,0,0,0},
			{ 0,0,1,0,0,0,0},
			{ 0,0,0,1,0,0,0},
			{ 0,0,0,0,1,0,0},
			{ 0,0,0,0,0,1,0},
			{ 0,0,0,0,0,0,1}
		};
	}
	
	private void InitCombinedClassifier()
	{
		ACTIVITIES = new String[]{"stnding_t","strsupx_t","trnsdwn_t","sitting_t","transup_t","strsdwn_t",
								  "stnding_h","strsupx_h","trnsdwn_h","sitting_h","transup_h","strsdwn_h"
									,"walking_t","walking_h"};
		ACTIVITIES_PROBS = new double[][]{
				{3034,119,46,283,44,30,36,0,7,11,0,0,509,0},
				{63,761,1,0,0,14,0,0,0,0,0,0,151,0},
				{60,0,258,14,59,0,0,0,2,0,0,0,0,12},
				{120,0,8,498,9,0,0,0,4,59,6,0,0,0},
				{92,0,171,9,138,0,0,0,3,36,0,0,0,0},
				{89,765,8,23,26,20,0,0,0,0,0,0,68,0},
//		    {734,35,389,38,36,0,   186,0,0,0,0,0,   80,5},//{ 3086,28,1500,39,50,10,  0,0,0,0,0,0,  200,0},//1
//		    {10,305,32,86,0,0,    0,0,0,0,0,0,    0,0,},//{ 30,864,102,150,0,0,  0,0,0,0,0,0,  0,0},//2
//		    {172,98,621,48,0,0,   0,0,0,0,0,0,    0,0},//{ 1500,200,2193,200,0,0,  0,0,0,0,0,0,  0,0},//3
//		    {8,26,0,396,0,0,     0,0,0,0,0,0,     0,0},//{ 84,168,35,813,0,0,  0,0,0,0,0,0,  0,0},//4
//		    {0,0,0,0,585,0,     0,0,0,0,0,0,     85,0},//{ 0,0,0,0,1891,95,  0,0,0,0,0,0,  200,0},//5
//		    {0,0,0,0,6,660,    0,0,0,0,0,0,     20,0,},//{ 0,0,0,0,10,1907,  0,0,0,0,0,0,  200,0},//6
			
			{ 0,0,0,0,0,0,  934,38,138,22,0,10,  0,47},//7
			{ 0,0,0,0,0,0,  6,255,87,68,0,0,   0,0},//8
			{ 0,0,0,0,0,0,  0,34,806,32,0,0,   0,0},//9
			{ 0,0,0,0,0,0,  3,162,13,223,0,0,  0,0},//10
			{ 0,0,0,0,0,0,  0,0,0,0,585,25,   0,12},//11
			{ 0,0,0,0,0,5,  7,0,0,0,86,466,   16,53},//12
			
			{58,291,0,0,0,5,5,0,0,6,0,0,4283,0},//{ (7064/200),0,0,0,100,100, 0,0,0,0,10,10,  7064, 32},//13
			{ 0,0,0,0,0,20,   11,0,0,0,163,103,   21,1930,},//14
		};
		
		PI = new double[]{0.15,0.005,0.15,0.005,0.02,0.02,0.15,0.005,0.15,0.005,0.02,0.02,0.15,0.15};
		
		double p_changePos = 0.0000001;
		double[] s = new double[]{0.99979,0.999,0.99999,0.999,0.9998999,0.9998999,
								  0.99979,0.999,0.99999,0.999,0.9998999,0.9998999,
								  0.999999,0.999999};
		double[] r = new double[]{1 - s[0] - p_changePos,1 - s[1] - p_changePos,1 - s[2]- p_changePos,1 - s[3]- p_changePos,1 - s[4]- p_changePos,
				1 - s[5]- p_changePos,1 - s[6]- p_changePos,1 - s[7]- p_changePos,1 - s[8]- p_changePos,1 - s[9]- p_changePos,1 - s[10]- p_changePos,
				1 - s[11]- p_changePos,1 - s[12],1 - s[13]};
		Transitions = new double[][]{
				{ s[0],(r[0]/10)*2,0,0,(r[0]/10)*2,(r[0]/10)*2,  0,0,0,0,0,0,   (r[0]/10)*4,p_changePos},
				{  0,s[1],r[1],0,0,0,  0,0,0,0,0,0,   0,p_changePos},
				{ 0,0,s[2],r[2],0,0,  0,0,0,0,0,0,   0,p_changePos},
				{  r[3],0,0,s[3],0,0,  0,0,0,0,0,0,   0,p_changePos},
				{ r[4]/2,0,0,0,s[4],0,  0,0,0,0,0,0,   r[4]/2,p_changePos},
				{ r[5]/2,0,0,0,0.0,s[5],  0,0,0,0,0,0,   r[5]/2,p_changePos},
				
				{0,0,0,0,0,0,     s[6],(r[6]/10)*2,0,0,(r[6]/10)*2,(r[6]/10)*2,   p_changePos,(r[6]/10)*4},
				{0,0,0,0,0,0,      0,s[7],r[7],0,0,0,   p_changePos,0},
				{0,0,0,0,0,0,     0,0,s[8],r[8],0,0,   p_changePos,0},
				{0,0,0,0,0,0,    r[9],0,0,s[9],0,0,     p_changePos,0},
				{0,0,0,0,0,0,     r[10]/2,0,0,0,s[10],0,     p_changePos,r[10]/2},
				{0,0,0,0,0,0,     r[11]/2,0,0,0,0.0,s[11],    p_changePos,r[11]/2},
				
				{(r[12]/10)*8,0,0,0,(r[12]/10)*1,(r[12]/10)*1,     0,0,0,0,0.0,0,    s[12],0},
				{0,0,0,0,0,0,     (r[13]/10)*8,0,0,0,(r[13]/10)*1,(r[13]/10)*1,    0,s[13]},
			};
	}
//	
	private void InitTorsoClassifier()
	{
		ACTIVITIES = new String[]{"stnding_h","strsupx_h","trnsdwn_h","sitting_h","transup_h","strsdwn_h"};
		ACTIVITIES_PROBS = new double[][]{
			{ 3588,53,532,194,0,3},
			{ 123,780,179,230,0,7},
			{ 198,174,2714,106,0,0},
			{ 78,217,24,700,0,0},
			{ 0,0,0,0,1456,831},
			{ 0,0,0,0,135,2079}
		};
		
		PI = new double[]{0.45,0.01,0.45,0.01,0.04,0.04};
		
		Transitions = new double[][]{
				{ 0.98,0.1,0,0,0.05,0.5},
				{  0,0.3,0.7,0,0,0},
				{ 0,0,0.98,0.2,0,0},
				{  0.7,0,0,0.3,0,0},
				{ 0.2,0,0,0,0.8,0},
				{ 0.2,0,0,0,0.0,0.8},
			};
	}

	public int ActivityIndex(String activity)
	{
		int index = -1;
		for(int i=0;i<ACTIVITIES.length;i++)
		{
			if(activity.compareTo(ACTIVITIES[i]) == 0)
			{
				//Log.w("FindIndex","A:" + activity + " = B:" + ACTIVITIES[i]);
				index = i;
				break;
			}
		}
		return index;
	}

	public double ActivityProb(String currentActivity,String observedActivity)
	{
		int index0 = ActivityIndex(currentActivity);
		int index1 = ActivityIndex(observedActivity);

		double sum =0;
		for(int i=0;i<ACTIVITIES_PROBS.length;i++)
		{
			sum+= ACTIVITIES_PROBS[index0][i];
		}

		return ACTIVITIES_PROBS[index0][index1] / sum;
	}

	public double[] ActivityProbs(String currentActivity)
	{
		int index0 = ActivityIndex(currentActivity);


		double sum =0;
		for(int i=0;i<ACTIVITIES_PROBS.length;i++)
		{
			sum+= ACTIVITIES_PROBS[index0][i];
		}

		double[] probs = new double[ACTIVITIES_PROBS.length];
		for(int i=0;i<ACTIVITIES_PROBS.length;i++)
		{
			probs[i] = ACTIVITIES_PROBS[index0][i] / sum;
		}

		return probs;
	}
	
	public double[] ActivityProbs(int currentActivity)
	{
		int index0 = currentActivity;


		double sum =0;
		for(int i=0;i<ACTIVITIES_PROBS.length;i++)
		{
			sum+= ACTIVITIES_PROBS[index0][i];
		}

		double[] probs = new double[ACTIVITIES_PROBS.length];
		for(int i=0;i<ACTIVITIES_PROBS.length;i++)
		{
			probs[i] = ACTIVITIES_PROBS[index0][i] / sum;
		}

		return probs;
	}

	public void Clear()
	{
		for(int i=0;i< ActivityDurations.length;i++)
		{
			ActivityDurations[i] = 0;
		}
	}
	
	public double TotalActivityDuration()
	{
		if(ActivityDurations != null)
		{
			long total = 0;
			
			for(int i=0;i<ActivityDurations.length;i++)
			{
				total += ActivityDurations[i];
			}
			
			return total;
		}
		else
		{
			return 0;
		}
	}

	public void UpdateActivity(int index,long val)
	{
		if(ActivityDurations != null)
		{
			ActivityDurations[index]+= val;
		}
	}

	public String toString()
	{
		String t = User + "," + timeStamp + "," + time.toString("HH:MM:SS") /*time.format("%T")*/ + "," + time.toString("dd-mm-yy") /*time.format("%F")*/ + ",";

		for(int i=0;i< ActivityDurations.length;i++)
		{
			t +=   ToMinutes(ActivityDurations[i]) + ",";
		}
		t = t.substring(0,t.length()-1);
		return t ;
	}

	private double ToMinutes(double milliseconds)
	{
		return (milliseconds / 1000) / 60;
	}

	private String SaveString()
	{
		String t = User + "," + timeStamp + ","  + time.toString("HH:MM:SS") /*time.format("%T")*/ + "," + time.toString("dd-mm-yy") /*time.format("%F")*/ + ",";

		for(int i=0;i< ActivityDurations_Saving.length;i++)
		{
			t +=  ToMinutes(ActivityDurations_Saving[i]) + ",";
		}
		t = t.substring(0,t.length()-1);

		return t ;
	}

	public class SaveProcess implements Runnable {

		public void startSave() 
		{

			Thread t = new Thread(this);
			t.start();
		}

		public void run() 
		{
			try {
				while(CurrentSendingToServer)
				{

					Thread.sleep(500);

				}
				Save();
			}
			catch (InterruptedException e) {
				// TODO Auto-generated catch block

			}
		}
	}

	public void BeginSaving(double startTime)
	{
		//if(SaveData)
		//{
		double milliseconds =  SystemClock.elapsedRealtime();//(double)(System.nanoTime() - startTime) / 1000000;
		timeStamp = (int)milliseconds;
		time = new DateTime();
		time = DateTime.now();
		date = new DateTime();
		date = DateTime.now();
		SaveProcess t = new SaveProcess();
		t.startSave();
		//}
	}
	
	private long[] Normalize(long[] dataRaw,long TotalMaxTime)
	{
		long[] normData = new long[dataRaw.length];
		
		long total = 0;
		for(int i=0;i<normData.length;i++)
		{
			total += dataRaw[i];
		}
		double scale = (double)TotalMaxTime / (double)total;
		for(int i=0;i<normData.length;i++)
		{
			normData[i] = (long)((double)dataRaw[i] * scale);
		}
		return normData;
	}

	private void Save()
	{
		CurrentlySaving = true;
		ActivityDurations_Saving = new long[ActivityDurations.length];
		for(int i=0;i<ActivityDurations.length;i++)
		{
			ActivityDurations_Saving[i] = ActivityDurations[i];
		}
		ActivityDurations_Saving = Normalize(ActivityDurations_Saving,MaxTime);
		Clear();

		Time t = new Time();
		t.setToNow();

		Save(t.format("%a %d-%b-%G"));//using strftime formatting see: http://php.net/manual/en/function.strftime.php
		Clear();
		CurrentlySaving = false;
		//}
		//}
	}
	
	public void Save_ClassificationRecord(String session_name,boolean append)
	{

		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) 
		{
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} 
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) 
		{
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} 
		else 
		{
			// Something else is wrong. It may be one of many other states, but all we need
			//  to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}

		if( mExternalStorageAvailable && mExternalStorageWriteable)
		{
			try
			{
				File f = Environment.getExternalStorageDirectory();

				//String fullfilename = f.getAbsoluteFile() + "/Android/data/info.androidhive.materialtabs.QOL/" ;
				//File folderToWrite = new File(fullfilename);
				//folderToWrite.mkdirs();
				
				File f2 = new File("/storage/extSdCard/");
				
				String fullfilename = f2.getAbsoluteFile() + "/Android/data/info.androidhive.materialtabs.QOL/"  ;
				File folderToWrite = new File(fullfilename);
				folderToWrite.mkdirs();
				boolean sdFound = folderToWrite.exists() && folderToWrite.canWrite();
				
				if(!sdFound)
				{
					fullfilename = f.getAbsoluteFile() + "/Android/data/info.androidhive.materialtabs.QOL/"  ;
					folderToWrite = new File(fullfilename);
					folderToWrite.mkdirs();
				}


				File fileToWrite_ACT = new File(fullfilename,session_name);
				FileOutputStream fos_ACT = new FileOutputStream(fileToWrite_ACT,append);
				OutputStreamWriter osw_ACT = new OutputStreamWriter(fos_ACT); 

				
				for(int i=0;i<classifiationRecord.size();i++)
				{
					int[] currentFrame = classifiationRecord.get(i);
					String currentLine = "";
					for(int j=0;j<currentFrame.length;j++)
					{
						currentLine += currentFrame[j] + ",";
					}
					osw_ACT.write(currentLine + "\n");
				}
				

				osw_ACT.close();



				//motion.WriteAccel(osw_Accel);
				//motion.WriteOrienation(osw_Or);
				//motion.WriteGyro(osw_Gyro);
				//motion.WriteMag(osw_Mag);

				//osw.write("Example Text in a file in the "+f.getAbsolutePath()+" dir");
				//osw_Mag.close();


				//osw_Gyro.close();
				//osw_Or.close();

			}
			catch(IOException e)
			{

			}
		}

	}

	public void Save_String(String session_name,String line,int[][] matrix,boolean append)
	{

		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) 
		{
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} 
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) 
		{
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} 
		else 
		{
			// Something else is wrong. It may be one of many other states, but all we need
			//  to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}

		if( mExternalStorageAvailable && mExternalStorageWriteable)
		{
			try
			{
				File f = Environment.getExternalStorageDirectory();

				//String fullfilename = f.getAbsoluteFile() + "/Android/data/info.androidhive.materialtabs.QOL/" ;
				//File folderToWrite = new File(fullfilename);
				//folderToWrite.mkdirs();
				
				File f2 = new File("/storage/extSdCard/");
				
				String fullfilename = f2.getAbsoluteFile() + "/Android/data/info.androidhive.materialtabs.QOL/"  ;
				File folderToWrite = new File(fullfilename);
				folderToWrite.mkdirs();
				boolean sdFound = folderToWrite.exists() && folderToWrite.canWrite();
				
				if(!sdFound)
				{
					fullfilename = f.getAbsoluteFile() + "/Android/data/info.androidhive.materialtabs.QOL/"  ;
					folderToWrite = new File(fullfilename);
					folderToWrite.mkdirs();
				}


				File fileToWrite_ACT = new File(fullfilename,session_name);
				FileOutputStream fos_ACT = new FileOutputStream(fileToWrite_ACT,append);
				OutputStreamWriter osw_ACT = new OutputStreamWriter(fos_ACT); 

				
				
				osw_ACT.write(line + "\n");
				
				String row = "";
				for(int i=0;i<matrix.length;i++)
				{
					for(int j=0;j<matrix[i].length;j++)
					{
						row += matrix[i][j] + ",";
					}
					osw_ACT.write(row + "\n");
					row = "";
				}
				
				

				osw_ACT.close();



				//motion.WriteAccel(osw_Accel);
				//motion.WriteOrienation(osw_Or);
				//motion.WriteGyro(osw_Gyro);
				//motion.WriteMag(osw_Mag);

				//osw.write("Example Text in a file in the "+f.getAbsolutePath()+" dir");
				//osw_Mag.close();


				//osw_Gyro.close();
				//osw_Or.close();

			}
			catch(IOException e)
			{

			}
		}

	}

	public void Save(String session_name)
	{

		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) 
		{
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} 
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) 
		{
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} 
		else 
		{
			// Something else is wrong. It may be one of many other states, but all we need
			//  to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}

		if( mExternalStorageAvailable && mExternalStorageWriteable)
		{
			try
			{
				File f = Environment.getExternalStorageDirectory();

				//String fullfilename = f.getAbsoluteFile() + "/Android/data/QOLMeasurement/files/" ;
				//File folderToWrite = new File(fullfilename);
				//folderToWrite.mkdirs();
				File f2 = new File("/storage/extSdCard/");
				
				String fullfilename = f2.getAbsoluteFile() + "/Android/data/QOLMeasurement/files/"  ;
				File folderToWrite = new File(fullfilename);
				folderToWrite.mkdirs();
				boolean sdFound = folderToWrite.exists() && folderToWrite.canWrite();
				
				if(!sdFound)
				{
					fullfilename = f.getAbsoluteFile() + "/Android/data/QOLMeasurement/files/"  ;
					folderToWrite = new File(fullfilename);
					folderToWrite.mkdirs();
				}

				File fileToWrite_ACT = new File(fullfilename,session_name + "_ACTIVITY.csv");
				FileOutputStream fos_ACT = new FileOutputStream(fileToWrite_ACT,true);
				OutputStreamWriter osw_ACT = new OutputStreamWriter(fos_ACT); 

				osw_ACT.write(this.SaveString() + "\n");


				osw_ACT.close();



				//motion.WriteAccel(osw_Accel);
				//motion.WriteOrienation(osw_Or);
				//motion.WriteGyro(osw_Gyro);
				//motion.WriteMag(osw_Mag);

				//osw.write("Example Text in a file in the "+f.getAbsolutePath()+" dir");
				//osw_Mag.close();


				//osw_Gyro.close();
				//osw_Or.close();

			}
			catch(IOException e)
			{

			}
		}

	}

}
