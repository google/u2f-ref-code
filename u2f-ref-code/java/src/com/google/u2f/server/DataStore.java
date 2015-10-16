// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import com.google.u2f.server.data.EnrollSessionData;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SignSessionData;

public interface DataStore {

  // attestation certs and trust
  public void addTrustedCertificate(X509Certificate certificate);

  public Set<X509Certificate> getTrustedCertificates();


  // session handling
  public /* sessionId */ String storeSessionData(EnrollSessionData sessionData);

  public SignSessionData getSignSessionData(String sessionId);

  public EnrollSessionData getEnrollSessionData(String sessionId);


  // security key management
  public void addSecurityKeyData(String accountName, SecurityKeyData securityKeyData);

  public List<SecurityKeyData> getSecurityKeyData(String accountName);

  public void removeSecuityKey(String accountName, byte[] publicKey);

  public void updateSecurityKeyCounter(String accountName, byte[] publicKey, int newCounterValue);
}
