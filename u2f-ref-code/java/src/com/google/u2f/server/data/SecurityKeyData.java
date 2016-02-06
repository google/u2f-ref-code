// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.data;

import org.apache.commons.codec.binary.Base64;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SecurityKeyData {
  public enum Transports {
    BLUETOOTH_BREDR("bt"),
    BLUETOOTH_LOW_ENERGY("ble"),
    USB("usb"),
    NFC("nfc");

    private String mValue;

    Transports(String value) {
      mValue = value;
    }

    @Override
    public String toString() {
      return mValue;
    }
  }

  private final long enrollmentTime;
  private final List<Transports> transports;
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
    this(enrollmentTime, null /* transports */, keyHandle, publicKey, attestationCert, counter);
  }

  public SecurityKeyData(
      long enrollmentTime,
      List<Transports> transports,
      byte[] keyHandle,
      byte[] publicKey,
      X509Certificate attestationCert,
      int counter) {
    this.enrollmentTime = enrollmentTime;
    this.transports = transports;
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

  public List<Transports> getTransports() {
    return transports;
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
    return Objects.hash(
        enrollmentTime,
        transports,
        keyHandle,
        publicKey,
        attestationCert,
        counter);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SecurityKeyData)) {
      return false;
    }
    SecurityKeyData that = (SecurityKeyData) obj;
    return Arrays.equals(this.keyHandle, that.keyHandle)
        && (this.enrollmentTime == that.enrollmentTime)
        && containSameTransports(this.transports, that.transports)
        && Arrays.equals(this.publicKey, that.publicKey)
        && Objects.equals(this.attestationCert, that.attestationCert)
        && Objects.equals(counter, counter);
  }

  /**
   * Compares the two Lists of Transports and says if they are equal.
   *
   * @param transports1 first List of Transports
   * @param transports2 second List of Transports
   * @return true if both lists are null or if both lists contain the same transport values
   */
  public static boolean containSameTransports(List<Transports> transports1,
      List<Transports> transports2) {
    if (transports1 == null && transports2 == null) {
      return true;
    } else if (transports1 == null || transports2 == null) {
      return false;
    }
    return transports1.containsAll(transports2) && transports2.containsAll(transports1);
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
      .append("transports: ")
      .append(transports)
      .append("\n")
      .toString();
  }
}
