package com.paranoiaworks.unicus.android.sse.utils;

import com.paranoiaworks.unicus.android.sse.config.Argon2Params;

import de.wuthoehle.argon2jni.Argon2;
import de.wuthoehle.argon2jni.EncodedArgon2Result;
import de.wuthoehle.argon2jni.SecurityParameters;

/**
 * Argon2 KDF Executor
 * 
 * @author Paranoia Works
 * @version 1.0.0
 */
public class Argon2Provider {
	
	private Argon2Provider(){}
	
	public static byte[] Argon2idHash(byte[] password, byte[] salt, Argon2Params ap, int outputLength)
	{		
		if(Argon2.isNativeCodeAvailable())
		{
			 SecurityParameters securityParameters = new SecurityParameters(ap.getT(), ap.getM(), ap.getH());
			 Argon2 argon2 = new Argon2(securityParameters, outputLength, Argon2.TypeIdentifiers.ARGON2ID, Argon2.VersionIdentifiers.VERSION_13);	    
			 EncodedArgon2Result results = argon2.argon2_hash(getByteArrayCopy(password), getByteArrayCopy(salt));
			 byte[] output = results.getResult();
			    
			 return output;
		}
		else
		{
		    at.gadermaier.argon2.Argon2 argon2pi = at.gadermaier.argon2.Argon2Factory.create();
		    argon2pi.setType(at.gadermaier.argon2.model.Argon2Type.Argon2id);
		    argon2pi.setIterations(ap.getT());
		    argon2pi.setMemoryInKiB(ap.getM());
		    argon2pi.setParallelism(ap.getH());
		    argon2pi.setOutputLength(outputLength);
		    byte[] output = argon2pi.hashBytes(getByteArrayCopy(password), getByteArrayCopy(salt));
		    
		    return output;
		}
	}
	
	private static byte[] getByteArrayCopy(byte[] data)
	{
		byte[] dataCopy = new byte[data.length];
		System.arraycopy(data, 0, dataCopy, 0, data.length);
		return dataCopy;
	}
}
