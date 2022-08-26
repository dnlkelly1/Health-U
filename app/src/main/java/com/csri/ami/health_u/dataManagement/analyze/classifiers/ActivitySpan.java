package com.csri.ami.health_u.dataManagement.analyze.classifiers;

public class ActivitySpan 
{
	public int StartTime = -1;
	public int EndTime = -1;
	
	public String Activity = "";
	public int ActivityIndex = -1;
	
	public ActivitySpan()
	{
		
	}
	
	public boolean Overlaps(ActivitySpan X)
	{
		double X_Duration = X.EndTime - X.StartTime;
		double Duration = EndTime - StartTime;
		
		double scale = 0.4;
		double Size_Limit = Duration * (scale);
		
		if(X.StartTime >= StartTime && X.StartTime <= EndTime)
		{
			// this ->    //////////////////
			//    x ->        ///////////////////
			
			double overlap = EndTime - X.StartTime;
			if((overlap / Duration) > scale)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else if(X.EndTime < EndTime && X.EndTime >= StartTime)
		{
			// this ->                 //////////////////
			//    x ->        ///////////////////
			double overlap = X.EndTime - StartTime;
			if((overlap / Duration) > scale)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}
}
