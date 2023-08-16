package com.paranoiaworks.unicus.android.sse.misc;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import java.util.concurrent.CyclicBarrier;

import sse.org.bouncycastle.crypto.digests.SHA3Digest;
import sse.org.bouncycastle.crypto.digests.SkeinDigest;

/**
 * Additional Entropy Provider
 * 
 * @author Paranoia Works
 * @version 1.0.3
 */
public class ExtendedEntropyProvider implements SensorEventListener {
	
	private SensorManager mSensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	
	private StringBuffer acc0;
	private StringBuffer acc1;
	private StringBuffer acc2;
	private StringBuffer mg0;
	private StringBuffer mg1;
	private StringBuffer mg2;
	private long iterations = 0;

	public ExtendedEntropyProvider(Context context)
	{
		reset();

		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		if(mSensorManager != null)
		{
			accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		}
	}

	public static byte[] getSystemStateDataDigested()
	{			
		return getSHA3Hash(getSystemStateData().getBytes(), 256);
	}
	
	public static String getSystemStateData()
	{
		StringBuffer systemVariables = new StringBuffer();
		
		systemVariables.append(String.valueOf(System.currentTimeMillis()));
		systemVariables.append(" ");
		
		try {
			if(android.os.Build.VERSION.SDK_INT >= 17)
			{
				systemVariables.append(SystemClock.elapsedRealtimeNanos());
				systemVariables.append(" ");
			}
		} catch (Throwable e) {
			// N/A
		}
		
		try {
			systemVariables.append(SystemClock.uptimeMillis());
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}
		
		try {
			systemVariables.append(SystemClock.currentThreadTimeMillis());
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}
		
		try {
			systemVariables.append(Runtime.getRuntime().freeMemory());
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}
		
		try {
			systemVariables.append(android.os.Process.myTid());
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}
		
		try {
			systemVariables.append(android.os.Process.getElapsedCpuTime());
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}

		try {
			systemVariables.append(System.identityHashCode(systemVariables));
			systemVariables.append("-");
			systemVariables.append(System.identityHashCode(new String()));
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}

		return systemVariables.toString();
	}
	
	public void startCollectors() 
	{
		if(accelerometer != null)
			mSensorManager.registerListener(ExtendedEntropyProvider.this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		if(magnetometer != null)
			mSensorManager.registerListener(ExtendedEntropyProvider.this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
	}
	
	public void stopCollectors() 
	{	        	   
		mSensorManager.unregisterListener(this);
	}

	public void reset()
	{
		acc0 = new StringBuffer();
		acc1 = new StringBuffer();
		acc2 = new StringBuffer();
		mg0 = new StringBuffer();
		mg1 = new StringBuffer();
		mg2 = new StringBuffer();
		iterations = 0;
	}
	
	public byte[] getActualDataDigested() 
	{			
		String actualData = getActualData();
		if(actualData == null)
			return null;
		return getSkeinHash(actualData.getBytes(), 1024);
	}
	
	public String getActualData() 
	{			
		if((acc0.length() + acc1.length() + acc2.length() + mg0.length() + mg1.length() + mg2.length()) < 1)
			return null;

		String output = iterations + " : " + acc0.toString() + " | " + acc1.toString()
				+ " | " + acc2.toString()  + " || " + mg0.toString()  + " | " + mg1.toString()
				+ " | " + mg2.toString() + " || " + getSystemStateData();
		return output;
	}
	
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) 
    {
    	// N/A
    }
 
    @Override
    public void onSensorChanged(SensorEvent event) 
    {
        try {
			++iterations;
        	if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			{
				acc0.append(event.values[0]);
				acc0.append(":");
				acc1.append(event.values[1]);
				acc1.append(":");
				acc2.append(event.values[2]);
				acc2.append(":");
			}
			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				mg0.append(event.values[0]);
				mg0.append(":");
				mg1.append(event.values[1]);
				mg1.append(":");
				mg2.append(event.values[2]);
				mg2.append(":");
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
    }
    
    public static byte[] getSHA3Hash(byte[] data, int bits)
    {
    	byte[] hash = new byte[bits / 8];
    	SHA3Digest digester = new SHA3Digest(bits);
    	digester.update(data, 0, data.length);
    	digester.doFinal(hash, 0);
    	return hash;
    }

	public static byte[] getSkeinHash(byte[] data, int outputSizeBits)
	{
		byte[] hash = new byte[outputSizeBits / 8];
		SkeinDigest digester = new SkeinDigest(SkeinDigest.SKEIN_1024, outputSizeBits);
		digester.update(data, 0, data.length);
		digester.doFinal(hash, 0);
		return hash;
	}
    
	public static ExtendedEntropyProvider getFilledInstance(Context context, long collectingTimeMs) throws Exception
	{
		CyclicBarrier barrier = new CyclicBarrier(2);

		final ExtendedEntropyProvider eep = new ExtendedEntropyProvider(context);
		eep.startCollectors();

		Thread collectorThread = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(collectingTimeMs);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				eep.stopCollectors();

				//System.out.println("DATA P: " + eep.getActualData());
				//System.out.println("DATA D: " + Helpers.byteArrayToHexString(eep.getActualDataDigested()));

				try {
					barrier.await();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		collectorThread.start();
		barrier.await();
		return eep;
	}
}
