package sse.org.bouncycastle.pqc.crypto.crystals.kyber;

import java.security.SecureRandom;

import sse.org.bouncycastle.crypto.EncapsulatedSecretGenerator;
import sse.org.bouncycastle.crypto.SecretWithEncapsulation;
import sse.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import sse.org.bouncycastle.pqc.crypto.util.SecretWithEncapsulationImpl;

public class KyberKEMGenerator
    implements EncapsulatedSecretGenerator
{
    // the source of randomness
    private final SecureRandom sr;

    public KyberKEMGenerator(SecureRandom random)
    {
        this.sr = random;
    }

    public SecretWithEncapsulation generateEncapsulated(AsymmetricKeyParameter recipientKey)
    {
        KyberPublicKeyParameters key = (KyberPublicKeyParameters)recipientKey;
        KyberEngine engine = key.getParameters().getEngine();
        engine.init(sr);
        byte[][] kemEncrypt = engine.kemEncrypt(key.getPublicKey());
        return new SecretWithEncapsulationImpl(kemEncrypt[0], kemEncrypt[1]);
    }
}
