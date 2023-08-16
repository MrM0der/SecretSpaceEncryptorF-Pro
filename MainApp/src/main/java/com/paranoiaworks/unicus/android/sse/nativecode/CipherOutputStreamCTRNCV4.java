package com.paranoiaworks.unicus.android.sse.nativecode;

import com.paranoiaworks.unicus.android.sse.config.DynamicConfig;
import com.paranoiaworks.unicus.android.sse.misc.CounterCTR;
import com.paranoiaworks.unicus.android.sse.misc.PWCipherOutputStream;
import com.paranoiaworks.unicus.android.sse.misc.WithMAC;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.concurrent.CyclicBarrier;

import io.lktk.NativeBlake3;
import io.lktk.NativeBlake3Util;

/**
 * Cipher OuputStream for CTR mode - Native Code
 * 
 * @author Paranoia Works
 * @version 1.0.1
 */
public class CipherOutputStreamCTRNCV4 extends PWCipherOutputStream implements WithMAC {

	private EncryptorNC encryptorNC;
	
	final int BUFFER_SIZE_BASE = 1048576;
	private int blocksInBuffer[];
	private ByteBuffer writeBuffer;
	private int bufferedBytesCounter = 0;
	private CounterCTR ctrCounters[];
	private Encryptor.AlgorithmBean algorithmBean;
	private int parallelization;
	private int bufferSize;
	
	private NativeBlake3 blake3Mac;
	
	private CyclicBarrier barrier = new CyclicBarrier(2);
	private ByteBuffer macTempData = null;
	private boolean macTempDataUsed = false;
	
	public CipherOutputStreamCTRNCV4(final OutputStream out, byte[] nonce, byte[] key, byte[] macKey, Encryptor.AlgorithmBean algorithmBean) {
        super(out, nonce, key, macKey, algorithmBean.getInnerCode());
		this.parallelization = DynamicConfig.getCTRParallelizationNC();
        this.encryptorNC = new EncryptorNC(parallelization);
        this.algorithmBean = algorithmBean;
		this.bufferSize = BUFFER_SIZE_BASE * parallelization;
        writeBuffer = ByteBuffer.allocate(bufferSize);
		macTempData = ByteBuffer.allocate(bufferSize);
		blake3Mac = NativeBlake3.createHasher(macKey);
        addToMAC(nonce);

        if(algorithmBean.getNestedAlgs() == null) {
			ctrCounters = new CounterCTR[1];
			ctrCounters[0] = new CounterCTR(nonce);
			blocksInBuffer = new int[1];
			blocksInBuffer[0] = bufferSize / nonce.length;
		}
		else {
			ctrCounters = new CounterCTR[algorithmBean.getNestedAlgs().length];
			blocksInBuffer = new int[algorithmBean.getNestedAlgs().length];
			int nonceOffset = 0;
			for(int i = 0; i < ctrCounters.length; ++i) {
				int[] nonceSplit = algorithmBean.getNonceSplit();
				int nonceLength = nonceSplit[i];
				byte[] nonceTemp = Helpers.getSubarray(nonce, nonceOffset, nonceLength);
				ctrCounters[i] = new CounterCTR(nonceTemp);
				blocksInBuffer[i] = bufferSize / nonceTemp.length;
				nonceOffset += nonceLength;
			}
		}
    }
	
	@Override
	public void write(int b) throws IOException {
		byte [] singleByte = {(byte) b};
		write(singleByte, 0, 1);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}
	
    @Override
	public void write(byte[] b, int off, int len) throws IOException {
    	if(len < 1) return;
    	
    	if(getBufferFreeSpace() <= len) // write to stream
    	{
    		writeBuffer.put(b, off, getBufferFreeSpace());
    		
    		byte[] writeBufferData = writeBuffer.array();		
    		
    		if(macTempDataUsed) {
	    		new Thread (new Runnable() 
				{
					public void run() 
					{
						addToMAC(macTempData.array());
		    			try {
							barrier.await();
						} catch (Exception e) {
							e.printStackTrace();
						} 
					}
				}).start();
			}
    		
    		boolean ok = encryptorNC.encryptByteArrayCTR(ctrCounters, key, algorithmBean.getKeySplit(), writeBufferData,
					algorithmBean.getNestedAlgs() == null ? new int[] {algorithmBean.getInnerCode()} : algorithmBean.getNestedAlgs());
    		
    		if(!ok) throw new IOException("Unexpected Error COSNC");
    		out.write(writeBufferData, 0, bufferSize);
    		
    		if(macTempDataUsed) {
    			try {
					barrier.await();
				} catch (Exception e) {
					throw new IOException("canceled");
				} 
    		}
    		
    		barrier.reset();

			macTempData.clear();
			macTempData.put(writeBufferData);
			macTempDataUsed = true;
    		
    		writeBuffer.clear();
    		
    		for(int i = 0; i < ctrCounters.length; ++i) ctrCounters[i].add(blocksInBuffer[i]);
    		
    		if(len - getBufferFreeSpace() != 0) {
	    		writeBuffer.put(Helpers.getSubarray(b, off + getBufferFreeSpace(), len - getBufferFreeSpace()));
	    		bufferedBytesCounter = len - getBufferFreeSpace();
    		} else {
    			bufferedBytesCounter = 0;
    		}
    	}
    	else // put data to buffer
    	{
    		writeBuffer.put(b, off, len);
    		countBufferedBytes(len);
    	} 
    }
    
    public void doFinal() throws IOException, InvalidParameterException
    {  	
    	if(macTempDataUsed) {
    		addToMAC(macTempData.array());
    	}
    	
    	byte[] data = Helpers.getSubarray(writeBuffer.array(), 0, bufferedBytesCounter);
    	
    	boolean ok  = encryptorNC.encryptByteArrayCTR(ctrCounters, key, algorithmBean.getKeySplit(), data,
				algorithmBean.getNestedAlgs() == null ? new int[] {algorithmBean.getInnerCode()} : algorithmBean.getNestedAlgs());
    	if(!ok) throw new InvalidParameterException("Unexpected Error COSNC LB");
    	out.write(data, 0, data.length);
    	addToMAC(data);
    	
		flush();
		encryptorNC.shutDownThreadExecutor();
    }
    
	public void addToMAC(final byte[] data)
	{
		blake3Mac.update(data);
	}
	
	public byte[] getMAC()
	{
		byte[] mac = null;
		try {
			mac = blake3Mac.getOutput();
		} catch (NativeBlake3Util.InvalidNativeOutput invalidNativeOutput) {
			invalidNativeOutput.printStackTrace();
		}

		return mac;
	}
    
    private void countBufferedBytes(long count) {
        if (count > 0) {
        	bufferedBytesCounter += count;
        }
    }
    
    protected int getBufferFreeSpace(){
    	return bufferSize - bufferedBytesCounter;
    }
    
	private static byte[] getByteArrayCopy(byte[] data)
	{
		byte[] dataCopy = new byte[data.length];
		System.arraycopy(data, 0, dataCopy, 0, data.length);
		return dataCopy;
	}
}
