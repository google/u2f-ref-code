// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.gaedemo.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SecurityKeyData.Transports;
import com.google.u2f.server.impl.CaCerts;
import com.google.u2f.server.impl.attestation.android.AndroidKeyStoreAttestation;
import com.google.u2f.tools.X509Util;

import org.apache.commons.codec.binary.Hex;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TokenStorageData {
  private long enrollmentTime;
  private List<Transports> transports;
  private byte[] keyHandle;
  private byte[] publicKey;
  private byte[] attestationCertChain;
  private int counter;

  // used by the storage layer
  public TokenStorageData() {}

  public TokenStorageData(SecurityKeyData tokenData) {
    this.enrollmentTime = tokenData.getEnrollmentTime();
    this.keyHandle = tokenData.getKeyHandle();
    this.publicKey = tokenData.getPublicKey();
    this.attestationCertChain =
        X509Util.encodeCertArray(tokenData.getAttestationCertificateChain());
    this.transports = tokenData.getTransports();
    this.counter = tokenData.getCounter();
  }

  public void updateCounter(int newCounterValue) {
    counter = newCounterValue;
  }

  public SecurityKeyData getSecurityKeyData() {
    X509Certificate[] x509certChain = null;
    if (attestationCertChain != null) {
      x509certChain = X509Util.parseCertificateChain(attestationCertChain);
    }
    return new SecurityKeyData(
        enrollmentTime, transports, keyHandle, publicKey, x509certChain, counter);
  }

  public JsonObject toJson() {
    X509Certificate[] x509certChain = getSecurityKeyData().getAttestationCertificateChain();
    JsonObject json = new JsonObject();
    json.addProperty("enrollment_time", enrollmentTime);
    json.add("transports", getJsonTransports());
    json.addProperty("key_handle", Hex.encodeHexString(keyHandle));
    json.addProperty("public_key", Hex.encodeHexString(publicKey));
    json.addProperty("issuer", x509certChain[0].getIssuerX500Principal().getName());

    try {
      // We currently use the temporary Android software CA root.  This root is
      // TEMPORARY until a final root is chosen.
      // TODO(aczeskis): use the actual root ca cert when it's available
      AndroidKeyStoreAttestation androidKeyStoreAttestation =
          AndroidKeyStoreAttestation.Parse(x509certChain,
              X509Util.parseCertificateChain(CaCerts.TEMPORARY_ANDROID_EC_ROOT));
      if (androidKeyStoreAttestation != null) {
        json.add("android_attestation", androidKeyStoreAttestation.toJson());
      }
    } catch (CertificateParsingException e) {
      throw new RuntimeException(e);
    }

    return json;
  }

  /**
   * Transforms the List of Transports in a JsonArray of Strings.
   *
   * @return a JsonArray object containing transport values as strings
   */
  private JsonArray getJsonTransports() {
    if (transports == null) {
      return null;
    }
    JsonArray jsonTransports = new JsonArray();
    for (Transports transport : transports) {
      jsonTransports.add(new JsonPrimitive(transport.toString()));
    }
    return jsonTransports;
  }

  @Override
  public String toString() {
    return toJson().toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        enrollmentTime, transports, keyHandle, publicKey, attestationCertChain, counter);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TokenStorageData))
      return false;
    TokenStorageData that = (TokenStorageData) obj;
    return (this.enrollmentTime == that.enrollmentTime)
        && SecurityKeyData.containSameTransports(this.transports, that.transports)
        && (this.counter == that.counter) && Arrays.equals(this.keyHandle, that.keyHandle)
        && Arrays.equals(this.publicKey, that.publicKey)
        && Arrays.equals(this.attestationCertChain, that.attestationCertChain);
  }
}
