package com.google.u2f.server.impl.attestation.android;

import java.security.cert.CertificateParsingException;

/**
 * Keysmaster algorithm values as taken from: keymaster_defs.h / KeymasterDefs.java
 */
public enum Algorithm {
  /* Asymmetric algorithms. */
  KM_ALGORITHM_RSA(1, "rsa"),
  KM_ALGORITHM_EC(3, "ec"),

  /* Block ciphers algorithms */
  KM_ALGORITHM_AES(32, "aes"),

  /* MAC algorithms */
  KM_ALGORITHM_HMAC(128, "hmac");

  private final int value;
  private final String description;

  public static Algorithm fromValue(int value) throws CertificateParsingException {
    for (Algorithm algorithm : Algorithm.values()) {
      if (algorithm.getValue() == value) {
        return algorithm;
      }
    }

    throw new CertificateParsingException("Invalid algorithm value: " + value);
  }

  private Algorithm(int value, String description) {
    this.value = value;
    this.description = description;
  }

  public int getValue() {
    return value;
  }

  @Override
  public String toString() {
    return description;
  }
}
