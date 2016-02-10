package com.google.u2f.server.impl.attestation.android;

import java.security.cert.CertificateParsingException;

/**
 * Keymaster block mode values as taken from: keymaster_defs.h / KeymasterDefs.java
 */
public enum BlockMode {
  KM_MODE_ECB(1, "ecb"),
  KM_MODE_CBC(2, "cbc"),
  KM_MODE_CTR(3, "ctr"),
  KM_MODE_GCM(32, "gcm");

  private final int value;
  private final String description;

  public static BlockMode fromValue(int value) throws CertificateParsingException {
    for (BlockMode mode : BlockMode.values()) {
      if (mode.getValue() == value) {
        return mode;
      }
    }

    throw new CertificateParsingException("Invalid block mode value: " + value);
  }

  public static BlockMode fromString(String string) throws CertificateParsingException {
    for (BlockMode mode : BlockMode.values()) {
      if (mode.toString().equals(string)) {
        return mode;
      }
    }

    throw new CertificateParsingException("Invalid block mode string: " + string);
  }

  private BlockMode(int value, String description) {
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
