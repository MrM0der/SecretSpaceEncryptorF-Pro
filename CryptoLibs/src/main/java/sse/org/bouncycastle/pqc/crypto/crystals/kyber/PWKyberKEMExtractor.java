package sse.org.bouncycastle.pqc.crypto.crystals.kyber;

import java.util.Arrays;

public class PWKyberKEMExtractor
{
    private KyberEngine engine;

    public PWKyberKEMExtractor(KyberParameters parameters) {
    	this.engine = parameters.getEngine();
    }
    
    public byte[] extract(byte[] privateKey, byte[] encapsulation)
    {
    	byte[] sharedSecret = engine.kemDecrypt(encapsulation, privateKey);
    	Arrays.fill(privateKey, (byte) 0);
        return sharedSecret;
    }
}
