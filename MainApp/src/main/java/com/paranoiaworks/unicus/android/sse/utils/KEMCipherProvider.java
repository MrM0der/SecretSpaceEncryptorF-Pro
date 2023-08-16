package com.paranoiaworks.unicus.android.sse.utils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import com.paranoiaworks.unicus.android.sse.PasswordVaultActivity;
import com.paranoiaworks.unicus.android.sse.dao.VaultItemV2;
import com.paranoiaworks.unicus.android.sse.misc.ExtendedEntropyProvider;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sse.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import sse.org.bouncycastle.crypto.SecretWithEncapsulation;
import sse.org.bouncycastle.pqc.crypto.crystals.kyber.KyberKEMExtractor;
import sse.org.bouncycastle.pqc.crypto.crystals.kyber.KyberKEMGenerator;
import sse.org.bouncycastle.pqc.crypto.crystals.kyber.KyberKeyGenerationParameters;
import sse.org.bouncycastle.pqc.crypto.crystals.kyber.KyberKeyPairGenerator;
import sse.org.bouncycastle.pqc.crypto.crystals.kyber.KyberParameters;
import sse.org.bouncycastle.pqc.crypto.crystals.kyber.KyberPrivateKeyParameters;
import sse.org.bouncycastle.pqc.crypto.crystals.kyber.KyberPublicKeyParameters;
import sse.org.bouncycastle.pqc.crypto.crystals.kyber.PWKyberKEMExtractor;
import sse.org.bouncycastle.util.encoders.Base64;

/**
 * KEM Algorithms Helper
 *
 * @author Paranoia Works
 * @version 1.0.0
 */
public class KEMCipherProvider {

	private static final String ALGORITHM_FAMILY_CRYSTALS_KYBER = "CRYSTALS-Kyber";
	public static final String ALGORITHM_CRYSTALS_KYBER_512 = "CRYSTALS-Kyber-512";
	public static final String ALGORITHM_CRYSTALS_KYBER_768 = "CRYSTALS-Kyber-768";
	public static final String ALGORITHM_CRYSTALS_KYBER_1024 = "CRYSTALS-Kyber-1024";

	private static final int ITEM_PRIVATEKEY = 0;
	private static final int ITEM_PUBLICKEY = 1;
	private static final int ITEM_SECRETENCAPSULATED = 3;

	private static List<String> availableAlgorithms = new ArrayList<String>();
	private static Map<String, ExpectedSizes> expectedSizes = new HashMap<String, ExpectedSizes>();

	private String algorithm;
	private boolean extendedSeed = false;
	private SecureRandom secureRandom;
	private Context context;

	static {
		availableAlgorithms.add(ALGORITHM_CRYSTALS_KYBER_512);
		expectedSizes.put(ALGORITHM_CRYSTALS_KYBER_512, new ExpectedSizes(1632, 800, 768));
		availableAlgorithms.add(ALGORITHM_CRYSTALS_KYBER_768);
		expectedSizes.put(ALGORITHM_CRYSTALS_KYBER_768, new ExpectedSizes(2400, 1184, 1088));
		availableAlgorithms.add(ALGORITHM_CRYSTALS_KYBER_1024);
		expectedSizes.put(ALGORITHM_CRYSTALS_KYBER_1024, new ExpectedSizes(3168, 1568, 1568));
	}

	public static List<String> getAvailableAlgorithms() {
		return availableAlgorithms;
	}

	public static int getAlgorithmIndex(String algorithmName)
	{
		return availableAlgorithms.indexOf(algorithmName);
	}

	public static String getAlgorithmNameByIndex(int index)
	{
		return availableAlgorithms.get(index);
	}

	public KEMCipherProvider(String algorithm)
	{
		this.algorithm = algorithm;
	}

	public KEMCipherProvider(String algorithm, boolean enableExtendedSeed)
	{
		this.algorithm = algorithm;
		this.extendedSeed = enableExtendedSeed;
	}

	public void setContext(Context context)
	{
		this.context = context;
	}

	//** 0 - Private, 1 - Public */
	public byte[][] generateKeyPair()
	{
		byte[][] keys = new byte[2][];

		if(algorithm.indexOf(ALGORITHM_FAMILY_CRYSTALS_KYBER) > -1) {
			KyberKeyPairGenerator keyGen = new KyberKeyPairGenerator();
			KyberParameters parameters = getParametersKyber();
			keyGen.init(new KyberKeyGenerationParameters(getSecureRandom(), parameters));

			AsymmetricCipherKeyPair keyPair = keyGen.generateKeyPair();
			keys[0] = ((KyberPrivateKeyParameters) keyPair.getPrivate()).getPrivateKey();
			keys[1] = ((KyberPublicKeyParameters) keyPair.getPublic()).getPublicKey();
		}

		return keys;
	}

	//** 0 - Private, 1 - Public */
	public char[][] generateKeyPairEncoded()
	{
		return encode(generateKeyPair());
	}

	//** 0 - Secret, 1 - EncapsulatedSecret */
	public byte[][] generateSecrets(char[] publicKey)
	{
		byte[] publicKeyDecoded = Base64.decode(charsToBytes(publicKey));
		return generateSecrets(publicKeyDecoded);
	}

	//** 0 - Secret, 1 - EncapsulatedSecret */
	public byte[][] generateSecrets(byte[] publicKey)
	{
		verifySize(publicKey, ITEM_PUBLICKEY);
		byte[][] secrets = new byte[2][];

		if(algorithm.indexOf(ALGORITHM_FAMILY_CRYSTALS_KYBER) > -1) {
			KyberParameters parameters = getParametersKyber();
			KyberPublicKeyParameters pkp = new KyberPublicKeyParameters(parameters, publicKey);

			KyberKEMGenerator kemGen = new KyberKEMGenerator(getSecureRandom());
			SecretWithEncapsulation secretEncap = kemGen.generateEncapsulated(pkp);
			secrets[0] = secretEncap.getSecret();
			secrets[1] = secretEncap.getEncapsulation();
		}

		return secrets;
	}

	//** 0 - Secret, 1 - EncapsulatedSecret */
	public char[][] generateKeySecretsEncoded(char[] publicKey)
	{
		return encode(generateSecrets(publicKey));
	}

	//** 0 - Secret */
	public byte[][] extractSecret(char[] privateKey, char[] secretEncapsulated)
	{
		byte[] privateKeyDecoded = Base64.decode(charsToBytes(privateKey));
		Arrays.fill(privateKey, '\u0000');
		byte[] secretEncapsulatedDecoded = Base64.decode(charsToBytes(secretEncapsulated));
		return extractSecret(privateKeyDecoded, secretEncapsulatedDecoded);
	}

	//** 0 - Secret */
	public byte[][] extractSecret(byte[] privateKey, byte[] secretEncapsulated)
	{
		verifySize(privateKey, ITEM_PRIVATEKEY);
		verifySize(secretEncapsulated, ITEM_SECRETENCAPSULATED);

		byte[][] extractedSecret = new byte[1][];

		if(algorithm.indexOf(ALGORITHM_FAMILY_CRYSTALS_KYBER) > -1) {
			KyberParameters parameters = getParametersKyber();
			PWKyberKEMExtractor kex = new PWKyberKEMExtractor(parameters);

			extractedSecret[0] = kex.extract(privateKey, secretEncapsulated);
		}

		return extractedSecret;
	}

	//** 0 - Secret */
	public char[][] extractSecretEncoded(char[] privateKey, char[] secretEncapsulated)
	{
		return encode(extractSecret(privateKey, secretEncapsulated));
	}

	private char[][] encode(byte[][] input)
	{
		char[][] output = new char[input.length][];

		for(int i = 0; i < input.length; ++i) {
			output[i] = bytesToChars(Base64.encode(input[i]));
			Arrays.fill(input[i], (byte) 0);
		}

		return output;
	}

	private byte[][] decode(char[][] input)
	{
		byte[][] output = new byte[input.length][];

		for(int i = 0; i < input.length; ++i) {
			output[i] = Base64.decode(charsToBytes(input[i]));
			Arrays.fill(input[i], '\u0000');
		}

		return output;
	}

	private KyberParameters getParametersKyber()
	{
		KyberParameters parameters = algorithm.equals(ALGORITHM_CRYSTALS_KYBER_1024)
				? KyberParameters.kyber1024 : algorithm.equals(ALGORITHM_CRYSTALS_KYBER_768)
				? KyberParameters.kyber768 : KyberParameters.kyber512;
		return parameters;
	}

	private char[] bytesToChars(byte[] bytes)
	{
		char[] chars = new char[bytes.length];
		for(int i = 0; i < bytes.length; ++i) {
			chars[i] = (char) (bytes[i] & 0xFF);
		}
		return chars;
	}

	private byte[] charsToBytes(char[] chars)
	{
		byte[] bytes = new byte[chars.length];
		for(int i = 0; i < chars.length; ++i) {
			bytes[i] = (byte) chars[i];
		}
		return bytes;
	}

	private synchronized SecureRandom getSecureRandom()
	{
		if(secureRandom != null) return secureRandom;
		SecureRandom random = null;
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				random = SecureRandom.getInstanceStrong();
			}
		} catch (NoSuchAlgorithmException e) {
			// N/A
		}
		if(random == null) random = new SecureRandom();
		if(extendedSeed && context != null) {
			try {
				ExtendedEntropyProvider eep = ExtendedEntropyProvider.getFilledInstance(context, 2500);
				random.setSeed(eep.getActualDataDigested());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		//System.out.println(random.getAlgorithm());
		secureRandom = random;

		return secureRandom;
	}

	public void verifySize(byte[] data, int itemCode)
	{
		int expectedSize = -1;
		String errorItem = null;
		switch (itemCode)
		{
			case ITEM_PRIVATEKEY:
				expectedSize = expectedSizes.get(algorithm).privateKeyLength;
				if(expectedSize != data.length) errorItem = getStringResource("pwv_pqc_privateKey");
				break;
			case ITEM_PUBLICKEY:
				expectedSize = expectedSizes.get(algorithm).publicKeyLength;
				if(expectedSize != data.length) errorItem = getStringResource("pwv_pqc_publicKey");
				break;
			case ITEM_SECRETENCAPSULATED:
				expectedSize = expectedSizes.get(algorithm).secretEncapsulatedLength;
				if(expectedSize != data.length) errorItem = getStringResource("pwv_pqc_sharedSecretEncapsulated");
				break;
		}

		if(errorItem != null) {
			String errorMessage = getStringResource("common_algorithm_text") + ": " + algorithm + "<br/>";
			errorMessage += getStringResource("common_incorrectSize") + ": " + errorItem + "<br/>";
			errorMessage += getStringResource("common_size_text") + ": " + data.length + " " + getStringResource("common_bytes_text") + "<br/>";
			errorMessage += getStringResource("common_expectedSize") + ": " + expectedSize + " " + getStringResource("common_bytes_text");
			throw new IllegalArgumentException(errorMessage);
		}
	}

	public String getStringResource(String name)
	{
		if(context == null) return name;
		String resText = null;
		int resID = context.getResources().getIdentifier(name, "string", context.getPackageName());
		if(resID == 0) resText = name;
		else resText = context.getResources().getString(resID);

		return resText;
	}

	private static class ExpectedSizes
	{
		private int privateKeyLength;
		private int publicKeyLength;
		private int secretEncapsulatedLength;

		private ExpectedSizes(int privateKeyLength, int publicKeyLength, int secretEncapsulatedLength) {
			this.privateKeyLength = privateKeyLength;
			this.publicKeyLength = publicKeyLength;
			this.secretEncapsulatedLength = secretEncapsulatedLength;
		}
	}
}
