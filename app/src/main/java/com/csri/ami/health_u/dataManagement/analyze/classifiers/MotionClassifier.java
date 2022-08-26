package com.csri.ami.health_u.dataManagement.analyze.classifiers;



import java.util.ArrayList;
import java.util.List;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.ObservationDiscrete;
import be.ac.ulg.montefiore.run.jahmm.ObservationInteger;
import be.ac.ulg.montefiore.run.jahmm.ObservationVector;
import be.ac.ulg.montefiore.run.jahmm.OpdfInteger;
import be.ac.ulg.montefiore.run.jahmm.OpdfIntegerFactory;
import be.ac.ulg.montefiore.run.jahmm.ViterbiCalculator;


public class MotionClassifier 
{
	String CurrentActivity = "";
	double walk=-1;
	double sit=-1;
	double stand=-1;
	double transup=-1;
	double transdown =-1;
	long duration=0;

	private int currentClass;
	MotionFeatures motionAnalyzer;
	
	ActivityDescriptor positionDescriptor;// = new ActivityDescriptor(ClassifierType.PositionClasifier, "Test", 1000 * 60);
	ActivityDescriptor lowerBodyDescriptor;// = new ActivityDescriptor(ClassifierType.LowerBodyClassifier, "Test", 1000 * 60);
	ActivityDescriptor torsoDescriptor;// = new ActivityDescriptor(ClassifierType.TorsoClassifier, "Test", 1000 * 60);
	ActivityDescriptor overalDescriptor;// = new ActivityDescriptor(ClassifierType.Overall, "Test", 1000 * 60);
	ActivityDescriptor combinedDescriptor;
	private Hmm<ObservationInteger> hmm_position;
	private Hmm<ObservationInteger> hmm_lower;
	private Hmm<ObservationInteger> hmm_torso;
	private List<ObservationInteger> observations_position;
	private List<ObservationInteger> observations_lower;
	private List<ObservationInteger> observations_torso;
	boolean LastPosition_Torso = false;


	ActivityDescriptor activityDes;
	//private static String[] ACTIVITIES = new String[]{"STAND","TRANSDOWN","SIT","TRANSUP","WALK"};
	//private long[] ActivityDurations;
	private int WALK_INDEX;// = ActivityDescriptor.ActivityIndex("WALK");

	private List<ObservationVector> observations_v;
	private List<ObservationInteger> observations;
	private List<Long> activityTimeSpans;
	private static int MAX_OBSERVATION_WINDOW_SIZE = 100;
	private static long MAX_LENGTH_TIME_PER_WINDOW = 1000 * 60;
	private int WalkCount = 0;
	private int[] CurrentHmSequence;
	private long LastClassificationTime = -1;
	private long CurrentClassificationTime = -1;
	private Hmm<ObservationInteger> hmm_i;
	private Hmm<ObservationVector> hmm_v;
	double startTime =-1;
	private String User = "";
	public enum Packet {
		STAND, TRANSDOWN, SIT, TRANSUP, WALK;

		public ObservationDiscrete<Packet> observation() {
			return new ObservationDiscrete<Packet>(this);
		}
	};

	public MotionClassifier(String user)
	{
		User = user;

		activityDes = new ActivityDescriptor(ClassifierType.Overall, User,MAX_LENGTH_TIME_PER_WINDOW);
		WALK_INDEX = activityDes.ActivityIndex("walking");
		observations = new ArrayList<ObservationInteger>();
		observations_position = new ArrayList<ObservationInteger>();
		observations_lower = new ArrayList<ObservationInteger>();
		observations_torso = new ArrayList<ObservationInteger>();
		//observations_v = new ArrayList<ObservationVector>();
		activityTimeSpans = new ArrayList<Long>();
		CurrentHmSequence = new int[0];

		
		
		
		//hmm_i = buildHmm();
		//hmm_v = buildGaussianHmm();
		//List<ObservationInteger> ob = generateSequence(test);
		//ViterbiCalculator v = new ViterbiCalculator(ob,hmm); 
		//int[] ss = v.stateSequence();
		motionAnalyzer = new MotionFeatures();
		
		positionDescriptor = new ActivityDescriptor(ClassifierType.PositionClasifier, "Test", 1000 * 60);
		lowerBodyDescriptor = new ActivityDescriptor(ClassifierType.LowerBodyClassifier, "Test", 1000 * 60);
		torsoDescriptor = new ActivityDescriptor(ClassifierType.TorsoClassifier, "Test", 1000 * 60);
		overalDescriptor = new ActivityDescriptor(ClassifierType.Overall, "Test", 1000 * 60);
		combinedDescriptor = new ActivityDescriptor(ClassifierType.Combined, "Test", 1000 * 60);
		
		hmm_position = buildHmm(combinedDescriptor);
		hmm_lower = buildHmm(lowerBodyDescriptor);
		hmm_torso = buildHmm(torsoDescriptor);
		
//		int[] test = new int[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,2,2,3,3,0,1,2,2,2,2,2,2,3,0,0,0,0,0,0,0,0,0,0,0,0,0};
//		int[] test2 = HMM_Adjust(test);
//		int x = test2[0];
	}

//	private Hmm<ObservationVector> buildGaussianHmm()
//	{
//		int standindex = activityDes.ActivityIndex("STAND");
//		int tdindex = activityDes.ActivityIndex("TRANSDOWN");
//
//		ARFF_Loader loader = new ARFF_Loader("sit_stand_Android_test.arff");
//
//		OpdfMultiGaussianFactory  factory = new OpdfMultiGaussianFactory (5);
//		Hmm<ObservationVector> hmm = new Hmm<ObservationVector> (5,factory);
//
//		hmm.setPi(activityDes.ActivityIndex("STAND"), 0.4);
//		hmm.setPi(activityDes.ActivityIndex("TRANSDOWN"), 0.01);
//		hmm.setPi(activityDes.ActivityIndex("SIT"), 0.4);
//		hmm.setPi(activityDes.ActivityIndex("TRANSUP"), 0.01);
//		hmm.setPi(activityDes.ActivityIndex("WALK"), 0.18);
//
//		OpdfMultiGaussian o1 = new OpdfMultiGaussian(5);
//
//		o1.fit(loader.getObs(standindex));
//		//Log.w("o1",Integer.toString(standindex) + " " + SeqToString( o1.mean()));
//
//		OpdfMultiGaussian o2 = new OpdfMultiGaussian(5);
//
//		o2.fit(loader.getObs(tdindex));
//		//Log.w("o2",Integer.toString(tdindex) + " " + SeqToString( o2.mean()));
//
//		OpdfMultiGaussian o3 = new OpdfMultiGaussian(5);
//		o3.fit(loader.getObs(activityDes.ActivityIndex("SIT")));
//		//Log.w("o3",SeqToString(o3.mean()));
//
//		OpdfMultiGaussian o4 = new OpdfMultiGaussian(5);
//		o4.fit(loader.getObs(activityDes.ActivityIndex("TRANSUP")));
//		//Log.w("o4",SeqToString(o4.mean()));
//
//		OpdfMultiGaussian o5 = new OpdfMultiGaussian(5);
//		o5.fit(loader.getObs(activityDes.ActivityIndex("WALK")));
//		//Log.w("o5",SeqToString(o5.mean()));
//
//		hmm.setOpdf(activityDes.ActivityIndex("STAND"), o1);
//		hmm.setOpdf(activityDes.ActivityIndex("TRANSDOWN"), o2);
//		hmm.setOpdf(activityDes.ActivityIndex("SIT"), o3);
//		hmm.setOpdf(activityDes.ActivityIndex("TRANSUP"), o4);
//		hmm.setOpdf(activityDes.ActivityIndex("WALK"), o5);
//
//		double stand_stand = 0.6;
//		double stand_walk = 0.399; 
//		double stand_down = 1 - (stand_stand + stand_walk);
//
//		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("STAND"), stand_stand);
//		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("TRANSDOWN"), stand_down);
//		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("SIT"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("TRANSUP"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("WALK"), stand_walk);
//
//
//		double down_down = 0.01;
//		double down_sit = 1 - down_down;
//
//		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("STAND"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("TRANSDOWN"), down_down);
//		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("SIT"), down_sit);
//		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("TRANSUP"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("WALK"), 0.0);
//
//
//		double sit_sit = 0.9;
//		double sit_up = 1 - sit_sit;
//
//		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("STAND"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("TRANSDOWN"), 0.00);
//		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("SIT"), sit_sit);
//		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("TRANSUP"), sit_up);
//		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("WALK"), 0.0);
//
//
//		double up_up = 0.01;
//		double up_stand = 1 - up_up;
//
//		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("STAND"),up_stand);
//		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("TRANSDOWN"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("SIT"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("TRANSUP"), up_up);
//		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("WALK"), 0.0);
//
//
//
//		double walk_stand = 0.332171;
//		double walk_walk = 1 - walk_stand;
//
//		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("STAND"), walk_stand);
//		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("TRANSDOWN"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("SIT"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("TRANSUP"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("WALK"), walk_walk);
//		return hmm;
//	}
	
	private Hmm<ObservationInteger> buildHmm(ActivityDescriptor activity)
	{	


		OpdfIntegerFactory factory = new OpdfIntegerFactory(activity.ACTIVITIES.length);
		Hmm<ObservationInteger> hmm = new Hmm<ObservationInteger>(activity.ACTIVITIES.length, factory);


		try
		{
		for(int i=0;i<activity.ACTIVITIES.length;i++)
		{
			hmm.setPi(i, activity.PI[i]);
			hmm.setOpdf(i, new OpdfInteger(activity.ActivityProbs(i)));
			
			for(int j=0;j<activity.ACTIVITIES.length;j++)
			{
				hmm.setAij(i, j, activity.Transitions[i][j]);
			}
			
		}
		}
		catch(Exception e)
		{
			int error = 1;
		}
//		hmm.setPi(activityDes.ActivityIndex("STAND"), 0.4);
//		hmm.setPi(activityDes.ActivityIndex("TRANSDOWN"), 0.01);
//		hmm.setPi(activityDes.ActivityIndex("SIT"), 0.4);
//		hmm.setPi(activityDes.ActivityIndex("TRANSUP"), 0.01);
//		hmm.setPi(activityDes.ActivityIndex("WALK"), 0.18);
//
//		hmm.setOpdf(activityDes.ActivityIndex("STAND"), new OpdfInteger(activityDes.ActivityProbs("STAND")));
//		hmm.setOpdf(activityDes.ActivityIndex("TRANSDOWN"), new OpdfInteger(activityDes.ActivityProbs("TRANSDOWN")));
//		hmm.setOpdf(activityDes.ActivityIndex("SIT"), new OpdfInteger(activityDes.ActivityProbs("SIT")));
//		hmm.setOpdf(activityDes.ActivityIndex("TRANSUP"), new OpdfInteger(activityDes.ActivityProbs("TRANSUP")));
//		hmm.setOpdf(activityDes.ActivityIndex("WALK"), new OpdfInteger(activityDes.ActivityProbs("WALK")));
//
//		//		hmm.setOpdf(1, new OpdfInteger(
//		//				new double[] { 0.035, 0.88,0.035,0.5,0.0 }));
//		//		hmm.setOpdf(2, new OpdfInteger(
//		//				new double[] { 0.0, 0.1,0.85,0.05,0.0 }));
//		//		hmm.setOpdf(3, new OpdfInteger(
//		//				new double[] { 0.05, 0.1,0.05,0.89,0.0 }));
//		//		hmm.setOpdf(4, new OpdfInteger(
//		//				new double[] { 0.05, 0.00,0.0,0.1,0.85 }));
//		double stand_stand = 0.6;
//		double stand_walk = 0.399; 
//		double stand_down = 1 - (stand_stand + stand_walk);
//
//		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("STAND"), stand_stand);
//		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("TRANSDOWN"), stand_down);
//		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("SIT"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("TRANSUP"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("WALK"), stand_walk);
//
//
//		double down_down = 0.001;
//		double down_sit = 1 - down_down;
//
//		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("STAND"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("TRANSDOWN"), down_down);
//		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("SIT"), down_sit);
//		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("TRANSUP"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("WALK"), 0.0);
//
//
//		double sit_sit = 0.999;
//		double sit_up = 1 - sit_sit;
//
//		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("STAND"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("TRANSDOWN"), 0.00);
//		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("SIT"), sit_sit);
//		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("TRANSUP"), sit_up);
//		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("WALK"), 0.0);
//
//
//		double up_up = 0.001;
//		double up_stand = 1 - up_up;
//
//		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("STAND"),up_stand);
//		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("TRANSDOWN"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("SIT"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("TRANSUP"), up_up);
//		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("WALK"), 0.0);
//
//
//
//		double walk_stand = 0.332171;
//		double walk_walk = 1 - walk_stand;
//
//		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("STAND"), walk_stand);
//		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("TRANSDOWN"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("SIT"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("TRANSUP"), 0.0);
//		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("WALK"), walk_walk);
//
//
//		//		hmm.setAij(1, 0, 0.0);
//		//		hmm.setAij(1, 1, 0.1);
//		//		hmm.setAij(1, 2, 0.9);
//		//		hmm.setAij(1, 3, 0.0);
//		//		hmm.setAij(1, 4, 0.0);
//		//
//		//		hmm.setAij(2, 0, 0.0);
//		//		hmm.setAij(2, 1, 0.0);
//		//		hmm.setAij(2, 2, 0.999);
//		//		hmm.setAij(2, 3, 0.001);
//		//		hmm.setAij(2, 4, 0.0);
//		//
//		//		hmm.setAij(3, 0, 0.9);
//		//		hmm.setAij(3, 1, 0.0);
//		//		hmm.setAij(3, 2, 0.0);
//		//		hmm.setAij(3, 3, 0.1);
//		//		hmm.setAij(3, 4, 0.0);
//		//
//		//		hmm.setAij(4, 0, 0.001);
//		//		hmm.setAij(4, 1, 0.0);
//		//		hmm.setAij(4, 2, 0.0);
//		//		hmm.setAij(4, 3, 0.0);
//		//		hmm.setAij(4, 4, 0.999);
//
//
//		//ViterbiCalculator();

		return hmm;
	}

	private Hmm<ObservationInteger> buildHmm()
	{	


		OpdfIntegerFactory factory = new OpdfIntegerFactory(5);
		Hmm<ObservationInteger> hmm = new Hmm<ObservationInteger>(5, factory);


		hmm.setPi(activityDes.ActivityIndex("STAND"), 0.4);
		hmm.setPi(activityDes.ActivityIndex("TRANSDOWN"), 0.01);
		hmm.setPi(activityDes.ActivityIndex("SIT"), 0.4);
		hmm.setPi(activityDes.ActivityIndex("TRANSUP"), 0.01);
		hmm.setPi(activityDes.ActivityIndex("WALK"), 0.18);

		hmm.setOpdf(activityDes.ActivityIndex("STAND"), new OpdfInteger(activityDes.ActivityProbs("STAND")));
		hmm.setOpdf(activityDes.ActivityIndex("TRANSDOWN"), new OpdfInteger(activityDes.ActivityProbs("TRANSDOWN")));
		hmm.setOpdf(activityDes.ActivityIndex("SIT"), new OpdfInteger(activityDes.ActivityProbs("SIT")));
		hmm.setOpdf(activityDes.ActivityIndex("TRANSUP"), new OpdfInteger(activityDes.ActivityProbs("TRANSUP")));
		hmm.setOpdf(activityDes.ActivityIndex("WALK"), new OpdfInteger(activityDes.ActivityProbs("WALK")));

		//		hmm.setOpdf(1, new OpdfInteger(
		//				new double[] { 0.035, 0.88,0.035,0.5,0.0 }));
		//		hmm.setOpdf(2, new OpdfInteger(
		//				new double[] { 0.0, 0.1,0.85,0.05,0.0 }));
		//		hmm.setOpdf(3, new OpdfInteger(
		//				new double[] { 0.05, 0.1,0.05,0.89,0.0 }));
		//		hmm.setOpdf(4, new OpdfInteger(
		//				new double[] { 0.05, 0.00,0.0,0.1,0.85 }));
		double stand_stand = 0.6;
		double stand_walk = 0.399; 
		double stand_down = 1 - (stand_stand + stand_walk);

		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("STAND"), stand_stand);
		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("TRANSDOWN"), stand_down);
		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("SIT"), 0.0);
		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("TRANSUP"), 0.0);
		hmm.setAij(activityDes.ActivityIndex("STAND"), activityDes.ActivityIndex("WALK"), stand_walk);


		double down_down = 0.001;
		double down_sit = 1 - down_down;

		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("STAND"), 0.0);
		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("TRANSDOWN"), down_down);
		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("SIT"), down_sit);
		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("TRANSUP"), 0.0);
		hmm.setAij(activityDes.ActivityIndex("TRANSDOWN"), activityDes.ActivityIndex("WALK"), 0.0);


		double sit_sit = 0.999;
		double sit_up = 1 - sit_sit;

		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("STAND"), 0.0);
		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("TRANSDOWN"), 0.00);
		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("SIT"), sit_sit);
		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("TRANSUP"), sit_up);
		hmm.setAij(activityDes.ActivityIndex("SIT"), activityDes.ActivityIndex("WALK"), 0.0);


		double up_up = 0.001;
		double up_stand = 1 - up_up;

		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("STAND"),up_stand);
		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("TRANSDOWN"), 0.0);
		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("SIT"), 0.0);
		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("TRANSUP"), up_up);
		hmm.setAij(activityDes.ActivityIndex("TRANSUP"), activityDes.ActivityIndex("WALK"), 0.0);



		double walk_stand = 0.332171;
		double walk_walk = 1 - walk_stand;

		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("STAND"), walk_stand);
		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("TRANSDOWN"), 0.0);
		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("SIT"), 0.0);
		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("TRANSUP"), 0.0);
		hmm.setAij(activityDes.ActivityIndex("WALK"), activityDes.ActivityIndex("WALK"), walk_walk);


		//		hmm.setAij(1, 0, 0.0);
		//		hmm.setAij(1, 1, 0.1);
		//		hmm.setAij(1, 2, 0.9);
		//		hmm.setAij(1, 3, 0.0);
		//		hmm.setAij(1, 4, 0.0);
		//
		//		hmm.setAij(2, 0, 0.0);
		//		hmm.setAij(2, 1, 0.0);
		//		hmm.setAij(2, 2, 0.999);
		//		hmm.setAij(2, 3, 0.001);
		//		hmm.setAij(2, 4, 0.0);
		//
		//		hmm.setAij(3, 0, 0.9);
		//		hmm.setAij(3, 1, 0.0);
		//		hmm.setAij(3, 2, 0.0);
		//		hmm.setAij(3, 3, 0.1);
		//		hmm.setAij(3, 4, 0.0);
		//
		//		hmm.setAij(4, 0, 0.001);
		//		hmm.setAij(4, 1, 0.0);
		//		hmm.setAij(4, 2, 0.0);
		//		hmm.setAij(4, 3, 0.0);
		//		hmm.setAij(4, 4, 0.999);


		//ViterbiCalculator();

		return hmm;
	}

	private List<ObservationInteger> generateSequence(int[] observations)
	{



		List<ObservationInteger> ob = new ArrayList<ObservationInteger>();

		for(int i=0;i<observations.length;i++)
		{
			ObservationInteger x = new ObservationInteger(observations[i]);
			ob.add(x);
		}



		return ob;
	}

	private List<ObservationVector> generateSequence(List<double[]> observations)
	{



		List<ObservationVector> ob = new ArrayList<ObservationVector>();

		for(int i=0;i<observations.size();i++)
		{
			ObservationVector x = new ObservationVector(observations.get(i));
			ob.add(x);
		}



		return ob;
	}

	public String getCurrentActivitySummmaryString()
	{
		return activityDes.toString();
	}

	private void UpdateObservations(double[] ob)
	{

		observations_v.add(new ObservationVector(ob));
		//Log.v("Classifier", "ob made");
		ViterbiCalculator v = new ViterbiCalculator(observations_v,hmm_v); 
		//Log.v("Classifier", "vit made");
		CurrentHmSequence = v.stateSequence();
		//Log.v("Classifier", "vit ran");

		if(observations_v.size() > MAX_OBSERVATION_WINDOW_SIZE)
		{

			int removeLen = MAX_OBSERVATION_WINDOW_SIZE/2;
			UpdateActivityDurations(removeLen);
			activityDes.BeginSaving(startTime);
			
			for(int i=0;i<removeLen;i++)
			{
				observations_v.remove(0);
				activityTimeSpans.remove(0);
			}
		}
	}

	private void UpdateObservations(int ob)
	{

		observations.add(new ObservationInteger(ob));


		//if(observations.size() > MAX_OBSERVATION_WINDOW_SIZE)
		//Log.v("Activity Duration",Long.toString(ActivityTimeSpan_Sum()));
		if(ActivityTimeSpan_Sum() > MAX_LENGTH_TIME_PER_WINDOW)
		{
			ViterbiCalculator v = new ViterbiCalculator(observations,hmm_i); 
			CurrentHmSequence = v.stateSequence();
			int removeLen = observations.size()/2;
			UpdateActivityDurations(removeLen);
			activityDes.BeginSaving(startTime);
			
			for(int i=0;i<removeLen;i++)
			{
				observations.remove(0);
				activityTimeSpans.remove(0);
				//Log.v("Remove Window","Complete");
			}
			//Log.v("Activity Duration_Logged",Double.toString(activityDes.TotalActivityDuration()));
		}
	}
	
	private int[] UpdateObservations(List<ObservationInteger> obList,Hmm<ObservationInteger> hmm,int ob)
	{

		obList.add(new ObservationInteger(ob));


		if(obList.size() > MAX_OBSERVATION_WINDOW_SIZE)
		//Log.v("Activity Duration",Long.toString(ActivityTimeSpan_Sum()));
		//if(ActivityTimeSpan_Sum() > MAX_LENGTH_TIME_PER_WINDOW)
		{
			ViterbiCalculator v = new ViterbiCalculator(obList,hmm); 
			int[] CurrentHmmSequence = v.stateSequence();
			int removeLen = obList.size()/2;
			//UpdateActivityDurations(removeLen);
			//activityDes.BeginSaving(startTime);
			
			int[] classifications_window = new int[removeLen];
			for(int i=0;i<removeLen;i++)
			{
				classifications_window[i] = CurrentHmmSequence[i];
				obList.remove(0);
				//activityTimeSpans.remove(0);
				//Log.v("Remove Window","Complete");
			}
			return classifications_window;
			//Log.v("Activity Duration_Logged",Double.toString(activityDes.TotalActivityDuration()));
		}
		else
		{
			return null;
		}
	}

	private long ActivityTimeSpan_Sum()
	{
		if(activityTimeSpans != null)
		{
			long total=0;
			for(int i=0;i<activityTimeSpans.size();i++)
			{
				total += activityTimeSpans.get(i);
			}
			return total;
		}
		else
		{
			return 0;
		}
	}

	private void UpdateActivityDurations(int size)
	{
		int removeLen = size;
		if(CurrentHmSequence.length >= removeLen)
		{
			for(int i=0;i<removeLen;i++)
			{
				int o = CurrentHmSequence[i];
				long len = activityTimeSpans.get(i);
				activityDes.UpdateActivity(o, len);
			}
		}
	}

	public int Max(double[] vec)
	{
		double max = Double.MIN_VALUE;
		int index = -1;

		for(int i=0;i<vec.length;i++)
		{
			if(vec[i] > max)
			{
				max = vec[i];
				index = i;
			}
		}
		return index;
	}

	public String SeqToString()
	{
		String seq = "";

		for(int i=0;i<CurrentHmSequence.length;i++)
		{
			seq += CurrentHmSequence[i] + ",";
		}
		return seq;
	}

	public String SeqToString(double[] a)
	{
		String seq = "";

		for(int i=0;i<a.length;i++)
		{
			seq += a[i] + ",";
		}
		return seq;
	}

	public double[] Normalize(double[] data)
	{
		double[] norm = new double[data.length];

		double sum = 0;
		for(int i=0;i<data.length;i++)
		{
			sum += data[i];
		}
		for(int i=0;i<data.length;i++)
		{
			norm[i] = data[i] / sum;
		}
		return norm;
	}

	public boolean IsSavingClassifications()
	{
		if(activityDes != null)
		{
			return activityDes.CurrentlySaving;
		}
		else
		{
			return false;
		}
	}

	public void SetIsCurrentlySendingToServer(boolean sending)
	{
		if(activityDes != null)
		{
			activityDes.CurrentSendingToServer = sending;
		}

	}
	
//	public int[] Classify_WithHMM(Object[] features)
//	{
//		try
//		{
//			double currentClass = Position_model.classify(features);
//			
//			//currentClass = T_model.classify(t_features);
//			String classString = "";
//			if(positionDescriptor.ACTIVITIES[(int)currentClass].compareTo("_other") == 0)
//			{
//				if(LastPosition_Torso)
//				{
//					currentClass = H_model.classify(features);
//					classString = lowerBodyDescriptor.ACTIVITIES[(int)currentClass];
//				}
//				else
//				{
//					currentClass = T_model.classify(features);
//					classString = torsoDescriptor.ACTIVITIES[(int)currentClass];
//				}
//
//			}
//			else if(positionDescriptor.ACTIVITIES[(int)currentClass].compareTo("walking_t") == 0)
//			{
//				LastPosition_Torso =false;
//				currentClass = overalDescriptor.ActivityIndex("walking_t");
//				classString = "walking_t";
//
//			}
//			else if(positionDescriptor.ACTIVITIES[(int)currentClass].compareTo("walking_h") == 0)
//			{
//				LastPosition_Torso =true;
//				currentClass = overalDescriptor.ActivityIndex("walking_t");
//				classString = "walking_h";
//
//			}
//			int classCombined = combinedDescriptor.ActivityIndex(classString);
//			int[] classes = UpdateObservations(observations_position,hmm_position,(int)classCombined);
//			
//			//overalDescriptor.UpdateClassificationRecord((int)currentClass);//.UpdatedClassificationRecord(currentClass);
//			return classes;
//		}
//		catch(Exception e)
//		{
//			Log.v("e",e.getMessage());
//			return null;
//		}
//	}
//	
//	public int[] HMM_Adjust(int[] classifications)
//	{
//		List<ObservationInteger> obList = new ArrayList<ObservationInteger>();
//		
//		for(int i=0;i<classifications.length;i++)
//		{
//			obList.add(new ObservationInteger(classifications[i]));
//		}
//		
//		ViterbiCalculator v = new ViterbiCalculator(obList,hmm_position); 
//		int[] CurrentHmmSequence = v.stateSequence();
//		
//		return CurrentHmmSequence;
//	}
//	
//	public int Classify(Object[] features)
//	{
//		try
//		{
//			double currentClass = Position_model.classify(features);
//			
//			//currentClass = T_model.classify(t_features);
//			String classString = "";
//			if(positionDescriptor.ACTIVITIES[(int)currentClass].compareTo("_other") == 0)
//			{
//				if(!LastPosition_Torso)
//				{
//					currentClass = T_model.classify(features);
//					classString = lowerBodyDescriptor.ACTIVITIES[(int)currentClass];
//				}
//				else
//				{
//					currentClass = H_model.classify(features);
//					classString = torsoDescriptor.ACTIVITIES[(int)currentClass];
//				}
//
//			}
//			else if(positionDescriptor.ACTIVITIES[(int)currentClass].compareTo("walking_t") == 0)
//			{
//				LastPosition_Torso =false;
//				currentClass = overalDescriptor.ActivityIndex("walking_t");
//				classString = "walking_t";
//
//			}
//			else if(positionDescriptor.ACTIVITIES[(int)currentClass].compareTo("walking_h") == 0)
//			{
//				LastPosition_Torso =true;
//				currentClass = overalDescriptor.ActivityIndex("walking_h");
//				classString = "walking_h";
//
//			}
//			int classCombined = combinedDescriptor.ActivityIndex(classString);
//			//int[] classes = UpdateObservations(observations_position,hmm_position,(int)classCombined);
//			
//			//overalDescriptor.UpdateClassificationRecord((int)currentClass);//.UpdatedClassificationRecord(currentClass);
//			return classCombined;
//		}
//		catch(Exception e)
//		{
//			//Log.v("e",e.getMessage());
//			return -1;
//		}
//	}
//
//
//	public String Classify(Vector[] accelWindow ,Vector[] GyroWindow,Vector[] AccelGlobalWindow,Vector[] GyroGlobalWindow,Vector[] OrientationWindow,Quaternion[] quatWindow)
//	{
//		//long start = System.nanoTime() / 1000000;
//		CurrentActivity = "";
//		if(accelWindow.length >= MotionFeatures.MAX_FEATURE_WINDOW_SIZE)
//		{
//
//			if(GyroWindow != null)
//			{
//				if(LastClassificationTime == -1)
//				{
//					startTime = System.nanoTime();
//					CurrentClassificationTime = System.nanoTime() / 1000000;
//					LastClassificationTime = CurrentClassificationTime ;
//				}
//
//				double[] walkingFeatures = motionAnalyzer.ExtractFeaturesWalkingVsAll(accelWindow , GyroWindow, AccelGlobalWindow,GyroGlobalWindow,OrientationWindow,quatWindow);
//				Object[] t_features = MotionFeatures.ExtractFeaturesObject(walkingFeatures);// new double[]{3.86802314511146,1.40914998291889,0.839115963065664,14.7666028531,9.75283077346216,1.92917216427983,9.94180131239921,0.253755621871192,1.5532140625,9.06865433933714,10.385916260315,1.89558814868288,2.44647122274322,0.0078740157480315,113.952193726299,0.398910970081749,2.35381493730259,0.997754143785306,6.04568447066204};
//				//Log.v("Classifier", "f");
//				//Object[] features = motionAnalyzer.ExtractFeaturesObject(accelWindow , recorder.getGyroWindow(), recorder.getAccelGlobalWindow(),recorder.getGyroGlobalWindow());
//				try
//				{
//					
//					double obVec = T_model.classify(t_features);
//					//Log.v("Classifier", "1");
//			//obVec = Normalize(obVec);
//					currentClass = (int)obVec;
//					//Log.v("Classifier", "2");
//					//UpdateObservations(obVec);
//					UpdateObservations(currentClass);
//					//Log.v("Classifier", "3");
//					Log.v("Classifier Class",Integer.toString(currentClass));
//					//Log.v("Classifier Seq", SeqToString());
//
//					//currentClass = (int)ADA_SitStand.classify(t_features);
//
//
//					//	ViterbiCalculator v = new ViterbiCalculator(observations,hmm); 
//					//	CurrentHmSequence = v.stateSequence();
//
//					int hmmIndex = currentClass;// CurrentHmSequence[CurrentHmSequence.length-1];
//					CurrentActivity = activityDes.ACTIVITIES[hmmIndex];
//
//					CurrentClassificationTime = System.nanoTime() / 1000000;
//					long CurrentActivityDuration = CurrentClassificationTime - LastClassificationTime;
//					activityTimeSpans.add(CurrentActivityDuration);
//					//ActivityDurations[hmmIndex] += CurrentActivityDuration;
//					LastClassificationTime = CurrentClassificationTime;
//
//					if(hmmIndex  == WALK_INDEX /*&& obVec[WALK_INDEX] > 0.8*/)
//					{
//						WalkCount++;
//						if(motionAnalyzer.StandPositionDueUpdate() && WalkCount > 4)
//						{
//							motionAnalyzer.UpdateStandPosition();
//						}
//					}
//					else
//					{
//						WalkCount = 0;
//					}
//
//					//long end = System.nanoTime() / 1000000;
//					//duration = end - start;
//
//					//duration = duration / 100;
//				}
//				catch(Exception e)
//				{
//					Log.v("e",e.getMessage());
//				}
//			}
//
//
//		}
//		return CurrentActivity;
//	}
}
