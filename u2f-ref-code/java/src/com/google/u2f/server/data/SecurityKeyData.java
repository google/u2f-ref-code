package com.google.u2f.server.data;

import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

import com.google.common.base.Objects;

public class SecurityKeyData {
  private final byte[] keyHandle;
  private final byte[] publicKey;
  private final X509Certificate attestationCert;

  public SecurityKeyData(byte[] keyHandle, byte[] publicKey, X509Certificate attestationCert) {
    this.keyHandle = keyHandle;
    this.publicKey = publicKey;
    this.attestationCert = attestationCert;
  }

  public byte[] getKeyHandle() {
    return keyHandle;
  }

  public byte[] getPublicKey() {
    return publicKey;
  }

  public X509Certificate getAttestationCertificate() {
    return attestationCert;
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(
        keyHandle, 
        publicKey, 
        attestationCert);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SecurityKeyData)) {
      return false;
    }
    SecurityKeyData that = (SecurityKeyData) obj;
    return Arrays.equals(this.keyHandle, that.keyHandle) 
        && Arrays.equals(this.publicKey, that.publicKey)
        && Objects.equal(this.attestationCert, that.attestationCert);
  }
  
  @Override
  public String toString() {
    return new StringBuilder()
      .append("public_key: ")
      .append(Base64.encodeBase64URLSafeString(publicKey))
      .append("\n")
      .append("key_handle: ")
      .append(Base64.encodeBase64URLSafeString(keyHandle))
      .append("\n")
      .append("attestation certificate:\n")
      .append(attestationCert.toString())
      .toString();
  }
}
