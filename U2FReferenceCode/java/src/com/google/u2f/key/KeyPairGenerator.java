package com.google.u2f.key;

import java.security.KeyPair;
import java.security.PublicKey;

public interface KeyPairGenerator {
	KeyPair generateKeyPair(byte[] applicationSha256, byte[] challengeSha256);
	byte[] encodePublicKey(PublicKey publicKey);
}
