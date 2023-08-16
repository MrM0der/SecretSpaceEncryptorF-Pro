package sse.org.bouncycastle.crypto;

import sse.org.bouncycastle.crypto.params.AsymmetricKeyParameter;

public interface EncapsulatedSecretGenerator
{
    /**
     * Generate secret/encapsulation based on the recipient public key.
     *
     * @return An SecretWithEncapsulation derived from the recipient public key.
     */
    SecretWithEncapsulation generateEncapsulated(AsymmetricKeyParameter recipientKey);
}
