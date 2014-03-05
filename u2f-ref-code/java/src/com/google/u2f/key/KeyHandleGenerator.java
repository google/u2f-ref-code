package com.google.u2f.key;

import java.security.KeyPair;

public interface KeyHandleGenerator {
  byte[] generateKeyHandle(byte[] applicationSha256, KeyPair keyPair);
}
