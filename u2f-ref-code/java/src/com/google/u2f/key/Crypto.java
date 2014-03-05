package com.google.u2f.key;

import java.security.PrivateKey;

import com.google.u2f.U2FException;

public interface Crypto {
  byte[] sign(byte[] signedData, PrivateKey certificatePrivateKey) throws U2FException;
}
