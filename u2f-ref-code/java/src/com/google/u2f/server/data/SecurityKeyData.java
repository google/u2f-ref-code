package com.google.u2f.server.data;

import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

import com.google.common.base.Objects;

public class SecurityKeyData {
  private final long enrollmentTime;
  private final byte[] keyHandle;
  private final byte[] publicKey;
  private final X509Certificate attestationCert;
  private int counter;

  public SecurityKeyData(
		  long enrollmentTime, 
		  byte[] keyHandle, 
		  byte[] publicKey, 
		  X509Certificate attestationCert,
		  int counter) {
    this.enrollmentTime = enrollmentTime;
    this.keyHandle = keyHandle;
    this.publicKey = publicKey;
    this.attestationCert = attestationCert;
    this.counter = counter;
  }

  /**
   * When these keys were created/enrolled with the relying party.
   */
  public long getEnrollmentTime() {
    return enrollmentTime;
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
  
  public int getCounter() {
	return counter; 
  }
  
  public void setCounter(int newCounterValue) {
    counter = newCounterValue;
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(
        enrollmentTime,
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
        && (this.enrollmentTime == that.enrollmentTime)
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
      .append("counter: ")
      .append(counter)
      .append("\n")
      .append("attestation certificate:\n")
      .append(attestationCert.toString())
      .toString();
  }
}
