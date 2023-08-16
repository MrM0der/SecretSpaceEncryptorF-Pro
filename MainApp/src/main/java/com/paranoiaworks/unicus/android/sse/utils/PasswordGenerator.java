package com.paranoiaworks.unicus.android.sse.utils;

import com.paranoiaworks.unicus.android.sse.misc.ExtendedEntropyProvider;

import java.nio.CharBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

import sse.org.bouncycastle.crypto.digests.SHA3Digest;
import sse.org.bouncycastle.crypto.digests.SkeinDigest;
import sse.org.bouncycastle.crypto.prng.ThreadedSeedGenerator;

/**
 * Password Generator
 * 
 * @author Paranoia Works
 * @version 1.0.6
 */
public class PasswordGenerator {

	private char[] charSet;
	private int[] substituteCache = new int[3];
	private int substituteCounter;
	private byte[] externalEntropy;
	private boolean customCharSet = false;

	private String lowerAlphaChars;
	private String upperAlphaChars;
	private String numberChars;
	private String specCharChars;
	private List<String> charSetList = new ArrayList<String>();
	
	
	public PasswordGenerator(boolean lowerAlpha, boolean upperAlpha, boolean number, boolean specChar, boolean removeMisspelling)
	{	
		if(removeMisspelling)
		{
			// 0O'`1l|I
			this.lowerAlphaChars = "abcdefghijkmnopqrstuvwxyz";
			this.upperAlphaChars = "ABCDEFGHJKLMNPQRSTUVWXYZ";
			this.numberChars = "23456789";
			this.specCharChars = "!\"#$%&()*+,-./:;<=>?@[\\]^_{}~";
		}
		else
		{
			this.lowerAlphaChars = "abcdefghijklmnopqrstuvwxyz";
			this.upperAlphaChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
			this.numberChars = "0123456789";
			this.specCharChars = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
		}

		String standardCharSet = "";

		if(lowerAlpha) {
			standardCharSet += lowerAlphaChars;
			charSetList.add(lowerAlphaChars);
		}
		if(upperAlpha) {
			standardCharSet += upperAlphaChars;
			charSetList.add(upperAlphaChars);
		}
		if(number) {
			standardCharSet += numberChars;
			charSetList.add(numberChars);
		}
		if(specChar) {
			standardCharSet += specCharChars;
			charSetList.add(specCharChars);
		}

		charSet = standardCharSet.toCharArray();
	}
	
	public PasswordGenerator(char[] customCharset)
	{	
		charSet = customCharset;
		if(charSet.length < 1) charSet = new char[]{'?', '!'};
		customCharSet = true;
	}
	
	public byte[] getExternalEntropy() {
		return externalEntropy;
	}

	public void setExternalEntropy(byte[] externalEntropy) {
		this.externalEntropy = externalEntropy;
	}

	public char[] getNewPassword(int length)
	{
		if(length < 4) length = 4;
		if(length > 128) length = 128;
		substituteCounter = 0;
		char[] password = new char[length];
		byte[] randomBytesBuffer = getRandomBA(length);

		shuffleArray(charSet);
		for(int i = 0; i < length; ++i)
		{
			password[i] = (getCharFromChosenCharset(charSet, randomBytesBuffer[i]));
		}

		if(!customCharSet) balancePassword(password);
		return password;
	}
	
	/** Get Random bytes */
	private byte[] getRandomBA(int length)
	{	
		int seedLength = length < 64 ? 64 : length;
		seedLength *= 2;
		ThreadedSeedGenerator tsg = new ThreadedSeedGenerator();
		byte[] tsgOutput = tsg.generateSeed(seedLength, true);		
		byte[] systemOutput = ExtendedEntropyProvider.getSystemStateDataDigested();
		
		byte[] seed = getSHA3Hash(Helpers.concat(tsgOutput, systemOutput, SecureRandom.getSeed(length)), 512);
		byte[] output = null;
		
		try {
			SecureRandom rand = new SecureRandom();
			rand.setSeed(seed);
			byte[]randomNum = new byte[seedLength];
			rand.nextBytes(randomNum);		
			
			int skeinLength = (length + 3) * 8;
			output = getSkeinHash(randomNum, skeinLength);
			
			if(externalEntropy != null)
				output = Helpers.xorit(output, getSkeinHash(externalEntropy, skeinLength));
			
			substituteCache[0] = (output[length] + 128);
			substituteCache[1] = (output[length + 1] + 128);
			substituteCache[2] = (output[length + 2] + 128);

		} catch (Exception e1) {
			e1.printStackTrace();
		}  

		return Helpers.getSubarray(output, 0, length);
	}
	
	private void balancePassword(char[] password)
	{
		int zeroCounter = 4;
		TreeMap<Integer, Integer> sortMap = new TreeMap<Integer, Integer>();

		while(zeroCounter > 0)
		{	
			if(sortMap.size() > 0)
			{
				String max = charSetList.get(sortMap.get(sortMap.lastKey()));
				String min = charSetList.get(sortMap.get(sortMap.firstKey()));
				char replacement = min.charAt(substituteCache[substituteCounter] % min.length());
				int replacementIndex = Helpers.indexOf(Pattern.compile("[" + Pattern.quote(max) + "]"), CharBuffer.wrap(password));
				if(replacementIndex > -1) password[replacementIndex] = replacement;
				++substituteCounter;
			}
			
			sortMap.clear();
			zeroCounter = 0;
			for(int i = 0; i < charSetList.size(); ++i)
			{
				int count = Helpers.regexGetCountOf(password, "[" + Pattern.quote(charSetList.get(i)) + "]");
				if(count == 0) ++zeroCounter;
				sortMap.put(count, i);		
			}			
		}
		shuffleArray(password);
	}

	public static void shuffleArray(char[] ar)
	{
		SecureRandom rand = new SecureRandom(SecureRandom.getSeed(32));
		for (int i = ar.length - 1; i > 0; i--)
		{
			int index = rand.nextInt(i + 1);
			char a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}

	public static char getCharFromChosenCharset(char[] charSet, byte deriveFromValue)
	{
		int byteValue = deriveFromValue + 128;
		int charIndex = (int)Math.floor(((double)byteValue / 255.001) * charSet.length);
		if(charIndex == charSet.length) --charIndex;
		return charSet[charIndex];
	}

	public static char[] removeDuplicateChars(char[] input)
	{
		int noUniqueChars = input.length;
		for (int i = 0; i < noUniqueChars; i++)
		{
			for (int j = i+1; j < noUniqueChars; j++)
			{
				if(input[i] == input[j])
				{
					input[j] = input[noUniqueChars - 1];
					noUniqueChars--;
					j--;
				}
			}
		}

		char[] output = Arrays.copyOf(input, noUniqueChars);
		Arrays.fill(input, '\u0000');
		return output;
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
}
