package com.paranoiaworks.unicus.android.sse.nativecode;

import com.paranoiaworks.unicus.android.sse.misc.CounterCTR;
import com.paranoiaworks.unicus.android.sse.misc.EncryptorPI;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Native Code Helper + Wrapper
 * 
 * @author Paranoia Works
 * @version 1.1.0
 */
public class EncryptorNC {

	public static final int AC_THREEFISH_1024 = 7;

	private static boolean initOk = false;
	private static Map<Integer, Integer> tweakSizeMap = new HashMap<Integer, Integer>();

	private ExecutorService executor = null;
	private int parallelization = 1;
	
	static {
		try {			
			System.loadLibrary("pwncenc");
			initOk = true;

			tweakSizeMap.put(AC_THREEFISH_1024, 16);

		} catch (UnsatisfiedLinkError e) {
			// disable N.C.
		}
	}
	
	private native int encryptByteArrayNC(byte[] iv, byte[] key, byte[] data, int algorithmCode);
	private native int decryptByteArrayNC(byte[] iv, byte[] key, byte[] data, int algorithmCode);
	
	private native int encryptByteArrayCTRNC(byte[] nonce, byte[] key, byte[] data, int algorithmCode);
	private native int encryptByteArrayWithParamsCTRNC(byte[] nonce, byte[] key, byte[] data, int algorithmCode, int offset, int length);
	private native int decryptByteArrayCTRNC(byte[] nonce, byte[] key, byte[] data, int algorithmCode);
	private native int decryptByteArrayWithParamsCTRNC(byte[] nonce, byte[] key, byte[] data, int algorithmCode, int offset, int length);

	public EncryptorNC()
	{
		this(1);
	}

	public EncryptorNC(int parallelization)
	{
		if(!isPowerOfTwo(parallelization)) throw new IllegalArgumentException("Parallelization has to be power of two.");
		this.parallelization = parallelization;
		if(parallelization > 1) executor = Executors.newFixedThreadPool(parallelization);
	}

	public boolean encryptByteArray(byte[] iv, byte[] key, byte[] data, int algorithmCode)  //CBC
	{
		Integer tweakSize = tweakSizeMap.get(algorithmCode);
		byte[] keyCopy = getByteArrayCopy(key);
		if (tweakSize != null) keyCopy = concat(keyCopy, new byte[tweakSize]);
		byte[] ivCopy = getByteArrayCopy(iv);
		
		int ok = encryptByteArrayNC(ivCopy, keyCopy, data, algorithmCode);
		return ok == 1 ? true : false;		
	}

	public boolean encryptByteArrayCTR(CounterCTR counters[], byte[] key, int[] keySplit, byte[] data, int[] algorithmCodes) //CTR
	{
		if(keySplit == null) {
			return encryptByteArrayCTR(counters[0].getCounter(), key, data, algorithmCodes[0]);
		}
		else {
			int keyOffset = 0;
			boolean okFinal = true;
			for(int i = 0; i < counters.length; ++i)
			{
				int keyLength = keySplit[i];
				byte[] keyTemp = Helpers.getSubarray(key, keyOffset, keyLength);
				boolean ok = encryptByteArrayCTR(counters[i].getCounter(), keyTemp, data, algorithmCodes[i]);
				okFinal = okFinal && ok;
				keyOffset += keyLength;
				Arrays.fill(keyTemp, (byte) 0);
			}
			return okFinal;
		}
	}

	public boolean encryptByteArrayCTR(byte[] nonce, byte[] key, byte[] data, int algorithmCode) //CTR
	{
		return encryptDecryptByteArrayCTR(nonce, key, data, algorithmCode, true);
	}

	public boolean decryptByteArrayCTR(CounterCTR counters[], byte[] key, int[] keySplit, byte[] data, int[] algorithmCodes) //CTR
	{
		if(keySplit == null) {
			return decryptByteArrayCTR(counters[0].getCounter(), key, data, algorithmCodes[0]);
		}
		else {
			int keyOffset = key.length;
			boolean okFinal = true;
			for(int i = counters.length - 1; i > -1; --i)
			{
				int keyLength = keySplit[i];
				keyOffset -= keyLength;
				byte[] keyTemp = Helpers.getSubarray(key, keyOffset, keyLength);
				boolean ok = decryptByteArrayCTR(counters[i].getCounter(), keyTemp, data, algorithmCodes[i]);
				okFinal = okFinal && ok;
				Arrays.fill(keyTemp, (byte) 0);
			}
			return okFinal;
		}
	}

	public boolean decryptByteArrayCTR(byte[] nonce, byte[] key, byte[] data, int algorithmCode) //CTR
	{
		return encryptDecryptByteArrayCTR(nonce, key, data, algorithmCode, false);
	}
	
	public boolean encryptDecryptByteArrayCTR(byte[] nonce, byte[] key, byte[] data, int algorithmCode, boolean encrypt) //CTR
	{
		Integer tweakSize = tweakSizeMap.get(algorithmCode);
		int blockSize = Encryptor.getAlgorithmBean(algorithmCode).getBlockSize() / 8;
		byte[] keyCopy = getByteArrayCopy(key);
		if (tweakSize != null) keyCopy = concat(keyCopy, new byte[tweakSize]);
		byte[] nonceCopy = getByteArrayCopy(nonce);
		boolean okFinal = false;

		if(parallelization > 1 && data.length >= 524288 && (data.length / parallelization) % blockSize == 0)
		{
			byte[][] keys = new byte[parallelization][];
			byte[][] nonces = new byte[parallelization][];
			EncryptionDecryptionThread encryptionThreads[] = new EncryptionDecryptionThread[parallelization];
			CyclicBarrier barrier = new CyclicBarrier(parallelization + 1);

			for(int i = 0; i < parallelization; ++i) keys[i] = getByteArrayCopy(keyCopy);

			int chunkSize = data.length / parallelization;
			int counterOffset = chunkSize / blockSize;
			for(int i = 0; i < parallelization; ++i)
			{
				if(i == 0) {
					nonces[i] = nonceCopy;
				}
				else {
					CounterCTR tempCounter = new CounterCTR(nonceCopy);
					tempCounter.add(i * counterOffset);
					nonces[i] = tempCounter.getCounter();
				}
				encryptionThreads[i] = new EncryptionDecryptionThread(i, barrier, nonces[i], keys[i], data, i * chunkSize, chunkSize, algorithmCode, encrypt);
			}

			for(int i = 0; i < encryptionThreads.length; ++i)
			{
				executor.execute(encryptionThreads[i]);
			}

			try {
				barrier.await();
			} catch (BrokenBarrierException e) {
				throw new Error("ENC Error: Broken Barrier Enc: " + encrypt);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// ------ B ------ //

			okFinal = true;
			for(int i = 0; i < encryptionThreads.length; ++i)
			{
				okFinal = okFinal && encryptionThreads[i].isOk();
			}

			for(int i = 0; i < parallelization; ++i) Arrays.fill(keys[i], (byte) 0);
		}
		else {
			int ok = 0;
			if(encrypt)
				ok = encryptByteArrayCTRNC(nonceCopy, keyCopy, data, algorithmCode);
			else
				ok = decryptByteArrayCTRNC(nonceCopy, keyCopy, data, algorithmCode);
			okFinal = (ok == 1) ? true : false;
		}

		Arrays.fill(keyCopy, (byte) 0);

		return okFinal;
	}

	public boolean decryptByteArray(byte[] iv, byte[] key, byte[] data, int algorithmCode) //CBC
	{
		Integer tweakSize = tweakSizeMap.get(algorithmCode);
		byte[] keyCopy = getByteArrayCopy(key);
		if (tweakSize != null) keyCopy = concat(keyCopy, new byte[tweakSize]);
		byte[] ivCopy = getByteArrayCopy(iv);

		int ok = decryptByteArrayNC(ivCopy, keyCopy, data, algorithmCode);
		return ok == 1 ? true : false;
	}
	
	public byte[] encryptByteArrayWithPadding(byte[] iv, byte[] key, byte[] data, int algorithmCode) //CBC
	{
		Integer tweakSize = tweakSizeMap.get(algorithmCode);
		byte[] keyCopy = getByteArrayCopy(key);
		if (tweakSize != null) keyCopy = concat(keyCopy, new byte[tweakSize]);
        byte[] ivCopy = getByteArrayCopy(iv);
		
		byte[] padding = getPaddingBytes(iv.length, data.length);		
    	byte[] output = concat(data, padding);
    	
		int ok = encryptByteArrayNC(ivCopy, keyCopy, output, algorithmCode);
		return ok == 1 ? output : null;		
	}
	
	public boolean checkCipher(int algorithmCode, int blockSize, int keySize)
	{	
		if(!initOk) return false;
		
		byte[] ivA = getPseudoRandomBytes(blockSize);
		byte[] ivB = getByteArrayCopy(ivA);
		byte[] ivA2 = getByteArrayCopy(ivA);
		byte[] ivB2 = getByteArrayCopy(ivA);
		byte[] ivC2 = getByteArrayCopy(ivA);
		byte[] keyA = getPseudoRandomBytes(keySize);
        byte[] keyB = getByteArrayCopy(keyA);
        byte[] keyA2 = getByteArrayCopy(keyA);
        byte[] keyB2 = getByteArrayCopy(keyA);
        byte[] keyC2 = getByteArrayCopy(keyA);
		byte[] dataOrg = getPseudoRandomBytes(4096);
		byte[] dataCBC = getByteArrayCopy(dataOrg); 
		byte[] dataCTR = getByteArrayCopy(dataOrg);
		byte[] dataCTRPI = getByteArrayCopy(dataOrg);

		boolean ok = false;

		Encryptor.AlgorithmBean ab = Encryptor.getAlgorithmBean(algorithmCode);
		EncryptorPI encryptorPI = new EncryptorPI();
		
		if(algorithmCode < 9)
		{
			ok = encryptByteArray(ivA, keyA, dataCBC, algorithmCode);
			if (!ok) return false;
			ok = decryptByteArray(ivB, keyB, dataCBC, algorithmCode);
			if (!ok) return false;
		}

		if(ab.getNestedAlgs() == null)
		{
			ok = encryptByteArrayCTR(ivA2, keyA2, dataCTR, algorithmCode);
			if (!ok) return false;
			ok = Arrays.equals(encryptorPI.encryptByteArrayCTR(ivC2, keyC2, dataCTRPI, algorithmCode), dataCTR);
			if (!ok) return false;
			ok = decryptByteArrayCTR(ivB2, keyB2, dataCTR, algorithmCode);
			if (!ok) return false;
		}
		else
		{
			CounterCTR[] ctrCounters = new CounterCTR[ab.getNestedAlgs().length];
			int nonceOffset = 0;
			for(int i = 0; i < ctrCounters.length; ++i)
			{
				int[] nonceSplit = ab.getNonceSplit();
				int nonceLength = nonceSplit[i];
				byte[] nonceTemp = Helpers.getSubarray(ivA2, nonceOffset, nonceLength);
				ctrCounters[i] = new CounterCTR(nonceTemp);
				nonceOffset += nonceLength;
			}
			CounterCTR[] ctrCountersB = ctrCounters.clone();
			CounterCTR[] ctrCountersC = ctrCounters.clone();

			ok = encryptByteArrayCTR(ctrCounters, keyA2, ab.getKeySplit(), dataCTR, ab.getNestedAlgs());
			if (!ok) return false;
			ok = Arrays.equals(encryptorPI.encryptByteArrayCTR(ctrCountersC, keyC2, ab.getKeySplit(), dataCTRPI, ab.getNestedAlgs()), dataCTR);
			if (!ok) return false;
			ok = decryptByteArrayCTR(ctrCountersB, keyB2, ab.getKeySplit(), dataCTR, ab.getNestedAlgs());
			if (!ok) return false;
		}
		
		ok = Arrays.equals(dataOrg, dataCBC) && Arrays.equals(dataOrg, dataCTR);
		return ok;
	}
	
	protected static byte[] getPaddingBytes(int ivLength, int dataLength)
	{
		Random rand = new Random(System.currentTimeMillis());
		
		int paddingSize = ivLength - (dataLength % ivLength);  	
    	byte[] padding = new byte[paddingSize];
    	for (int i = 0; i < paddingSize - 1; ++i) padding[i] = (byte)(rand.nextInt());
    	padding[paddingSize - 1] = (byte)paddingSize;
    	
    	return padding;
	}

	private static boolean isPowerOfTwo(int number)
	{
		return (number != 0) && ((number & (number - 1)) == 0);
	}
	
	private static byte[] getPseudoRandomBytes(int size)
	{
		byte[] output = new byte[size];
		Random rand = new Random(System.currentTimeMillis());
		for(int i = 0; i < size; ++i)
			output[i] = (byte)(rand.nextInt());
		return output;
	}
	
	private static byte[] getByteArrayCopy(byte[] data)
	{
		byte[] dataCopy = new byte[data.length];
		System.arraycopy(data, 0, dataCopy, 0, data.length);
		return dataCopy;
	}

	private static byte[] concat(byte[]... args)
	{
		int fulllength = 0;
		for (byte[] arrItem : args)
		{
			fulllength += arrItem.length;
		}
		byte[] retArray = new byte[fulllength];
		int start = 0;
		for (byte[] arrItem : args)
		{
			System.arraycopy(arrItem, 0, retArray, start, arrItem.length);
			start += arrItem.length;
		}
		return retArray;
	}

	public void shutDownThreadExecutor()
	{
		if(executor != null) executor.shutdown();
	}

	class EncryptionDecryptionThread implements Runnable
	{
		int id;
		CyclicBarrier barrier;
		int ok = 0;
		byte[] nonce;
		byte[] key;
		byte[] data;
		int algorithmCode;
		int offset;
		int length;
		boolean encrypt;

		public EncryptionDecryptionThread(int i, CyclicBarrier barrier, byte[] nonce, byte[] key, byte[] data, int offset, int length, int algorithmCode, boolean encrypt)
		{
			this.id = i;
			this.barrier = barrier;
			this.nonce = nonce;
			this.key = key;
			this.data = data;
			this.offset = offset;
			this.length = length;
			this.algorithmCode = algorithmCode;
			this.encrypt = encrypt;
		}

		public boolean isOk() {
			return ok == 1 ? true : false;
		}

		public void run()
		{
			if(encrypt)
				ok = encryptByteArrayWithParamsCTRNC(nonce, key, data, algorithmCode, offset, length);
			else
				ok = decryptByteArrayWithParamsCTRNC(nonce, key, data, algorithmCode, offset, length);
			try {
				barrier.await();
			} catch (BrokenBarrierException e) {
				throw new Error("ENC Error: Broken Barrier in " + id + "Enc: " + encrypt);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}

