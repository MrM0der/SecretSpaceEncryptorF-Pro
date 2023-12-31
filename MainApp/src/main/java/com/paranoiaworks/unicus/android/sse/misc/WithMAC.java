package com.paranoiaworks.unicus.android.sse.misc;

/**
 * Interface for stream with MAC
 * 
 * @author Paranoia Works
 * @version 1.0.0
 */

public interface WithMAC {
	
	void addToMAC(final byte[] data);
	byte[] getMAC();

}
