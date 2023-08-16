package com.paranoiaworks.unicus.android.sse.utils;

import android.os.Handler;
import android.os.Message;

import com.paranoiaworks.unicus.android.sse.misc.CipherOutputStreamCTRPIV4;
import com.paranoiaworks.unicus.android.sse.misc.CipherOutputStreamCTRPInoMAC;
import com.paranoiaworks.unicus.android.sse.nativecode.CipherOutputStreamCTRNCV4;
import com.paranoiaworks.unicus.android.sse.nativecode.CipherOutputStreamCTRNCnoMAC;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor.AlgorithmBean;

import org.apache.commons.io.output.NullOutputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

/**
 * Simple Benchmark for comparing algorithms speed
 * 
 * @author Paranoia Works
 * @version 1.2.0
 */
public class AlgorithmBenchmark {
	
	public final static int BENCHMARK_SHOW_DIALOG = -10001;
	public final static int BENCHMARK_COMPLETED = -10002;
	public static final int BENCHMARK_APPEND_TEXT = -10003;
	public static final int BENCHMARK_APPEND_TEXT_RESOURCE = -10004;
	public static final int BENCHMARK_FLUSH_BUFFER = -10005;
	
	public static final int NATIVE_CODE_OFFSET = 100;
	
	private final int FIRST_TEST_ITERATIONS = 2;
	private final int SECOND_TEST_ITERATIONS = 2;
	
	private final int FIRST_TEST_FILE_SIZE = 1048576;
	private final int SECOND_TEST_FILE_SIZE = 1048576;

	private Encryptor encryptor;
	AlgorithmBean ab;
	private OutputStream cipherOutputStreamPI;
	private OutputStream cipherOutputStreamNC;
	private OutputStream cipherOutputStreamPInoMAC;
	private OutputStream cipherOutputStreamNCnoMAC;
	private Handler handler;
	private int multiplicatorFirst = 1;
	private int multiplicatorSecond = 1;
	private boolean nativeCode = false;
	
	private byte[] resultEnc = null;
	
	private long overAllScore = 0;
	
	private long firstTestEnc = -1;
	private long secondTestEnc = -1;
	
	private double firstTestEncRes;
	private double secondTestEncRes;
	
	public AlgorithmBenchmark(int algorithmCode)
	{
		this(algorithmCode, null);
	}
	
	public AlgorithmBenchmark(int algorithmCode, Handler handler)
	{
		try {
			final int BUFFER = 65536;
			OutputStream nullOutputStream = new NullOutputStream();

			if(algorithmCode >= NATIVE_CODE_OFFSET) { // native code
				this.encryptor = new Encryptor("AlgorithmBenchmarkPassword".toCharArray(), algorithmCode - NATIVE_CODE_OFFSET);
				ab = encryptor.getAvailableAlgorithms().get(algorithmCode - NATIVE_CODE_OFFSET);
				this.nativeCode = true;
				byte[] nonce = getPseudoRandomBytes(ab.getBlockSize() / 8);
				byte[] key = getPseudoRandomBytes(ab.getKeySize() / 8);
				byte[] authKey = getPseudoRandomBytes(32);
				this.cipherOutputStreamNC = new CipherOutputStreamCTRNCV4(new BufferedOutputStream(nullOutputStream, BUFFER), nonce, key, authKey, ab);
				this.cipherOutputStreamNCnoMAC = new CipherOutputStreamCTRNCnoMAC(new BufferedOutputStream(nullOutputStream, BUFFER), nonce, key, ab);
			}
			else {
				this.encryptor = new Encryptor("AlgorithmBenchmarkPassword".toCharArray(), algorithmCode);
				ab = encryptor.getAvailableAlgorithms().get(algorithmCode);
				byte[] nonce = getPseudoRandomBytes(ab.getBlockSize() / 8);
				byte[] key = getPseudoRandomBytes(ab.getKeySize() / 8);
				byte[] authKey = getPseudoRandomBytes(32);
				this.cipherOutputStreamPI = new CipherOutputStreamCTRPIV4(new BufferedOutputStream(nullOutputStream, BUFFER), nonce, key, authKey, ab);
				this.cipherOutputStreamPInoMAC = new CipherOutputStreamCTRPInoMAC(new BufferedOutputStream(nullOutputStream, BUFFER), nonce, key, ab);
			}
			
			this.handler = handler;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void startBenchmark()
	{
		try {
			go();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private synchronized void go() throws InterruptedException, IOException
	{
		HowLong.start_reset();
		if(handler != null)
		{
			handler.sendMessage(Message.obtain(handler, BENCHMARK_SHOW_DIALOG));
			appentText("<u><b>");
			appentText("ou_benchmark_benchmark" , true);
			appentText(": " + encryptor.getEncryptAlgorithmComment());
			if(nativeCode) {
				appentText(" ");
				appentText("ou_benchmark_nc", true);
			}
			appentText("</b></u><br/>...<br/>");
		}
		
		appentText("common_initialization_text", true);appentText(": "); flushBuffer();
		byte[] initBytesFirstTest = getPseudoRandomBytes(FIRST_TEST_FILE_SIZE);
		byte[] initBytesSecondTest = getPseudoRandomBytes(SECOND_TEST_FILE_SIZE);
		
		Thread.sleep(300);
		while(true)
		{
			HowLong.start_reset();
			resultEnc = initBytesFirstTest;
			for(int i = 0; i < FIRST_TEST_ITERATIONS * multiplicatorFirst; ++i)
			{
				if(nativeCode)
					cipherOutputStreamNCnoMAC.write(resultEnc);
				else
					cipherOutputStreamPInoMAC.write(resultEnc);
			}
			if(HowLong.getDuration() > 2500) {
				if(HowLong.getDuration() < 5000) multiplicatorFirst *= 2;
				break;
			}
			multiplicatorFirst *= 3;
		}
		while(true)
        {
    		HowLong.start_reset();
    		resultEnc = initBytesSecondTest;
    		for(int i = 0; i < SECOND_TEST_ITERATIONS * multiplicatorSecond; ++i)
    		{
				if(nativeCode)
					cipherOutputStreamNC.write(resultEnc);
				else
					cipherOutputStreamPI.write(resultEnc);
    		}
    		if(HowLong.getDuration() > 2500) {
				if(HowLong.getDuration() < 5000) multiplicatorSecond *= 2;
    			break;
			}
			multiplicatorSecond *= 3;
        }
        appentText("<b>OK</b><br/>"); flushBuffer();

        // Without MAC
        appentText("ou_benchmark_withoutMac", true); appentText(": "); flushBuffer();
        Thread.sleep(500);
		HowLong.start_reset();
		resultEnc = initBytesFirstTest;
		for(int i = 0; i < FIRST_TEST_ITERATIONS * multiplicatorFirst; ++i)
		{
			if(nativeCode)
				cipherOutputStreamNCnoMAC.write(resultEnc);
			else
				cipherOutputStreamPInoMAC.write(resultEnc);
		}
		firstTestEnc = HowLong.getDuration();
		if(cipherOutputStreamNCnoMAC != null) ((CipherOutputStreamCTRNCnoMAC)cipherOutputStreamNCnoMAC).doFinal();
		if(cipherOutputStreamPInoMAC != null) ((CipherOutputStreamCTRPInoMAC)cipherOutputStreamPInoMAC).doFinal();
		timeToSpeed();
		appentText("<b>" + numSpeedToString(firstTestEncRes) + "</b><br/>"); flushBuffer();

		// With MAC
		appentText("ou_benchmark_withMac", true); appentText(": "); flushBuffer();
		Thread.sleep(500);
		HowLong.start_reset();
		resultEnc = initBytesSecondTest;
		for(int i = 0; i < SECOND_TEST_ITERATIONS * multiplicatorSecond; ++i)
		{
			if(nativeCode)
				cipherOutputStreamNC.write(resultEnc);
			else
				cipherOutputStreamPI.write(resultEnc);
		}
		secondTestEnc = HowLong.getDuration();
		if(cipherOutputStreamNC != null) ((CipherOutputStreamCTRNCV4)cipherOutputStreamNC).doFinal();
		if(cipherOutputStreamPI != null) ((CipherOutputStreamCTRPIV4)cipherOutputStreamPI).doFinal();
		timeToSpeed();
		appentText("<b>" + numSpeedToString(secondTestEncRes) + "</b><br/>"); flushBuffer();
		appentText("...<br/><b>");
		appentText("ou_benchmark_overallScore_text", true);
		appentText(": " + overAllScore);
		appentText(" ");
		appentText("ou_benchmark_points_text", true);
		appentText("</b><<br/>");
		appentText("ou_benchmark_io_notincluded", true);
		appentText("<br/>"); flushBuffer();

		if(handler != null)
			handler.sendMessage(Message.obtain(handler, BENCHMARK_COMPLETED));
	}
	
	private byte[] getPseudoRandomBytes(int size)
	{
		byte[] bytes = new byte[size];
		Random rand = new Random(0);
		rand.nextBytes(bytes);
		return bytes;
	}
	
	private void timeToSpeed()
	{
		if(firstTestEnc > -1) firstTestEncRes = ((1.0 * FIRST_TEST_FILE_SIZE * FIRST_TEST_ITERATIONS * multiplicatorFirst) / firstTestEnc * 1000);
		if(secondTestEnc > -1) secondTestEncRes = ((1.0 * SECOND_TEST_FILE_SIZE * SECOND_TEST_ITERATIONS * multiplicatorSecond) / secondTestEnc * 1000);
		overAllScore = (long)((firstTestEncRes + secondTestEncRes ) / 2 / 1024);
	}
	
	private void appentText(String text)
	{
		appentText(text, false);
	}
	
	private void appentText(String text, boolean fromRes)
	{
		if(handler != null) 
		{
			if(fromRes)handler.sendMessage(Message.obtain(handler, BENCHMARK_APPEND_TEXT_RESOURCE , text));
			else handler.sendMessage(Message.obtain(handler, BENCHMARK_APPEND_TEXT , text));
		}		
	}
	
	private void flushBuffer()
	{
		if(handler != null)handler.sendMessage(Message.obtain(handler, BENCHMARK_FLUSH_BUFFER)); 
	}
	
	private String numSpeedToString(double speed)
	{
		return Helpers.getFormatedFileSize((long)speed) + "/s";	
	}
}
