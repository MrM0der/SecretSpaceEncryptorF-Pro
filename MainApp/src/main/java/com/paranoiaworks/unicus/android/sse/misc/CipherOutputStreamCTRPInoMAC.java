package com.paranoiaworks.unicus.android.sse.misc;

import com.paranoiaworks.unicus.android.sse.config.DynamicConfig;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.concurrent.CyclicBarrier;

import sse.org.bouncycastle.crypto.digests.Blake3Digest;
import sse.org.bouncycastle.crypto.params.Blake3Parameters;

/**
 * Cipher OuputStream for CTR mode - Platform Independent
 *
 * @author Paranoia Works
 * @version 1.0.0
 */
public class CipherOutputStreamCTRPInoMAC extends PWCipherOutputStream {

	private EncryptorPI encryptorPI;

	final int BUFFER_SIZE_BASE = 1048576;
	private int blocksInBuffer = -1;
	private ByteBuffer writeBuffer = null;
	private int bufferedBytesCounter = 0;
	private CounterCTR ctrCounters[] = null;
	private Encryptor.AlgorithmBean algorithmBean = null;
	private int parallelization;
	private int bufferSize;

	public CipherOutputStreamCTRPInoMAC(final OutputStream out, byte[] nonce, byte[] key, Encryptor.AlgorithmBean algorithmBean) {
		super(out, nonce, key, null, algorithmBean.getInnerCode());
		this.parallelization = DynamicConfig.getCTRParallelizationPI();
		this.encryptorPI = new EncryptorPI(parallelization);
		this.algorithmBean = algorithmBean;
		this.bufferSize = BUFFER_SIZE_BASE * parallelization;
		writeBuffer = ByteBuffer.allocate(bufferSize);
		blocksInBuffer = bufferSize / nonce.length;

		if(algorithmBean.getNestedAlgs() == null) {
			ctrCounters = new CounterCTR[1];
			ctrCounters[0] = new CounterCTR(nonce);
		}
		else {
			ctrCounters = new CounterCTR[algorithmBean.getNestedAlgs().length];
			int nonceOffset = 0;
			for(int i = 0; i < ctrCounters.length; ++i) {
				int[] nonceSplit = algorithmBean.getNonceSplit();
				int nonceLength = nonceSplit[i];
				byte[] nonceTemp = Helpers.getSubarray(nonce, nonceOffset, nonceLength);
				ctrCounters[i] = new CounterCTR(nonceTemp);
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

			byte[] output = encryptorPI.encryptByteArrayCTR(ctrCounters, key, algorithmBean.getKeySplit(), writeBufferData,
					algorithmBean.getNestedAlgs() == null ? new int[] {algorithmBean.getInnerCode()} : algorithmBean.getNestedAlgs());

			out.write(output, 0, bufferSize);

			writeBuffer.clear();

			ctrCounters[0].add(blocksInBuffer);

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
		byte[] data = Helpers.getSubarray(writeBuffer.array(), 0, bufferedBytesCounter);

		byte[] output = encryptorPI.encryptByteArrayCTR(ctrCounters, key, algorithmBean.getKeySplit(), data,
				algorithmBean.getNestedAlgs() == null ? new int[] {algorithmBean.getInnerCode()} : algorithmBean.getNestedAlgs());
		out.write(output, 0, data.length);

		flush();
		encryptorPI.shutDownThreadExecutor();
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
