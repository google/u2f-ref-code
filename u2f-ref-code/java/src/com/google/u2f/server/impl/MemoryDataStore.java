// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.impl;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.u2f.server.DataStore;
import com.google.u2f.server.SessionIdGenerator;
import com.google.u2f.server.data.EnrollSessionData;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SignSessionData;

public class MemoryDataStore implements DataStore {
  private final Set<X509Certificate> trustedCertificateDataBase = Sets.newHashSet();
  private final HashMap<String, EnrollSessionData> sessionDataBase = Maps.newHashMap();
  private final HashMap<String, List<SecurityKeyData>> securityKeyDataBase = Maps.newHashMap();
  private final SessionIdGenerator sessionIdGenerator;

  public MemoryDataStore(SessionIdGenerator sessionIdGenerator) {
	  this.sessionIdGenerator = sessionIdGenerator;
  }

  @Override
  public String storeSessionData(EnrollSessionData sessionData) {
	String sessionId = sessionIdGenerator.generateSessionId(sessionData.getAccountName());
    sessionDataBase.put(sessionId, sessionData);
    return sessionId;
  }

  @Override
  public EnrollSessionData getEnrollSessionData(String sessionId) {
    return sessionDataBase.get(sessionId);
  }

  @Override
  public SignSessionData getSignSessionData(String sessionId) {
    return (SignSessionData) sessionDataBase.get(sessionId);
  }

  @Override
  public void addSecurityKeyData(String accountName, SecurityKeyData securityKeyData) {
    List<SecurityKeyData> tokens = getSecurityKeyData(accountName);
    tokens.add(securityKeyData);
    securityKeyDataBase.put(accountName, tokens);
  }

  @Override
  public List<SecurityKeyData> getSecurityKeyData(String accountName) {
    return Objects.firstNonNull(
        securityKeyDataBase.get(accountName),
        Lists.<SecurityKeyData>newArrayList());
  }

  @Override
  public Set<X509Certificate> getTrustedCertificates() {
    return trustedCertificateDataBase;
  }

  @Override
  public void addTrustedCertificate(X509Certificate certificate) {
    trustedCertificateDataBase.add(certificate);
  }

  @Override
  public void removeSecuityKey(String accountName, byte[] publicKey) {
    List<SecurityKeyData> tokens = getSecurityKeyData(accountName);
    for (SecurityKeyData token : tokens) {
      if (Arrays.equals(token.getPublicKey(), publicKey)) {
        tokens.remove(token);
        break;
      }
    }
  }

  @Override
  public void updateSecurityKeyCounter(String accountName, byte[] publicKey,
      int newCounterValue) {
    List<SecurityKeyData> tokens = getSecurityKeyData(accountName);
    for (SecurityKeyData token : tokens) {
      if (Arrays.equals(token.getPublicKey(), publicKey)) {
        token.setCounter(newCounterValue);
        break;
      }
    }
  }
}
