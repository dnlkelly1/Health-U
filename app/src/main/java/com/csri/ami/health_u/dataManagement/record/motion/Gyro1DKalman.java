package com.csri.ami.health_u.dataManagement.record.motion;

/**
 * Implementation of Kalman filter for accelerometer and gyroscope data
 *
 * @author Daniel Kelly
 * @version 1.0
 * @since 2014-06-1
 */
public class Gyro1DKalman 
{
	float x_angle;
	float x_bias;
	
	float P_00,
    P_01,
    P_10,
    P_11;
	
	float Q_angle, Q_gyro;
	float R_angle;
	
	public Gyro1DKalman(float q_angle, float q_gyro, float r_angle)
	{
		Q_angle = q_angle;
		Q_gyro  = q_gyro;
		R_angle = r_angle;
	}
	
	public void Update(float q_angle, float q_gyro, float r_angle)
	{
		Q_angle = q_angle;
		Q_gyro  = q_gyro;
		R_angle = r_angle;
	}
	
	void ars_predict( float dotAngle, float dt)
	{
		x_angle += dt * (dotAngle -x_bias);

		P_00 +=  - dt * (P_10 + P_01) + Q_angle * dt;
		P_01 +=  - dt * P_11;
		P_10 +=  - dt * P_11;
		P_11 +=  + Q_gyro * dt;
	}
	
	void ars_predict_wrap( float dotAngle, float dt)
	{
		x_angle += dt * (dotAngle -x_bias);
		
		if(x_angle > Math.PI)
	    {
	    	double diff = (Math.PI) - x_angle;
	    	//Log.v("roll check","roll:" + pitchangle + " PI/2 - pitch=" + diff);
	    	x_angle =(float)((Math.PI) + diff) ;
	    }
	    else if(x_angle < -(Math.PI))
	    {
	    	double diff = -(Math.PI) - x_angle;
	    	//Log.v("roll check","roll:" + pitchangle + " PI/2 - pitch=" + diff);
	    	x_angle = (float)(-(Math.PI) + diff );
	    }

		P_00 +=  - dt * (P_10 + P_01) + Q_angle * dt;
		P_01 +=  - dt * P_11;
		P_10 +=  - dt * P_11;
		P_11 +=  + Q_gyro * dt;
	}
	
	float ars_update(float angle_m)
	{
		float y = angle_m - x_angle;
		
		
		float S = P_00 + R_angle;
		float K_0 = P_00 / S;
		float K_1 = P_10 / S;
		
		x_angle +=  K_0 * y;
		x_bias  +=  K_1 * y;
		
		P_00 -= K_0 * P_00;
		P_01 -= K_0 * P_01;
		P_10 -= K_1 * P_00;
		P_11 -= K_1 * P_01;
		
		return x_angle;
	}
	
	
	
	void manualSet(float angle)
	{
		x_angle = angle;
	}
	
}

