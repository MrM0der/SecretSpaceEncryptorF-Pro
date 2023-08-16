package sse.org.bouncycastle.pqc.crypto.crystals.kyber;

import sse.org.bouncycastle.crypto.EncapsulatedSecretExtractor;
import sse.org.bouncycastle.crypto.params.AsymmetricKeyParameter;

public class KyberKEMExtractor
    implements EncapsulatedSecretExtractor
{
    private KyberEngine engine;

    private KyberPrivateKeyParameters key;

    public KyberKEMExtractor(KyberPrivateKeyParameters privParams)
    {
        this.key = privParams;
        initCipher(privParams);
    }

    private void initCipher(AsymmetricKeyParameter recipientKey)
    {
        KyberPrivateKeyParameters key = (KyberPrivateKeyParameters)recipientKey;
        engine = key.getParameters().getEngine();
    }

    @Override
    public byte[] extractSecret(byte[] encapsulation)
    {
        // Decryption
        byte[] sharedSecret = engine.kemDecrypt(encapsulation, key.getPrivateKey());
        return sharedSecret;
    }

    public int getEncapsulationLength()
    {
        return engine.getCryptoCipherTextBytes();
    }
}
