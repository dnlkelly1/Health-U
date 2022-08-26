package com.csri.ami.health_u.dataManagement.record;

import com.csri.ami.health_u.dataManagement.record.motion.Vector3D;

public class RawFeature 
{
	double TimeStamp;
	Vector3D AccelRaw;
	Vector3D AccelGlobal;
	com.csri.ami.health_u.dataManagement.record.motion.Vector3D GyroRaw;
	Vector3D GlobalGlobal;
	
	public RawFeature()
	{
		
	}
}
