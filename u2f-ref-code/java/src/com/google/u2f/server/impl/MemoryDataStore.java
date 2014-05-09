package com.google.u2f.server.impl;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.u2f.server.DataStore;
import com.google.u2f.server.SessionIdGenerator;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SessionData;

public class MemoryDataStore implements DataStore {
  private final Set<X509Certificate> trustedCertificateDataBase = new HashSet<X509Certificate>();
  private final HashMap<String, SessionData> sessionDataBase = new HashMap<String, SessionData>();
  private final HashMap<String, SecurityKeyData> securityKeyDataBase = new HashMap<String, SecurityKeyData>();
  private final SessionIdGenerator sessionIdGenerator;
  
  public MemoryDataStore(SessionIdGenerator sessionIdGenerator) {
	  this.sessionIdGenerator = sessionIdGenerator;
  }
  
  @Override
  public String storeSessionData(SessionData sessionData) {
	String sessionId = sessionIdGenerator.generateSessionId(sessionData.getAccountName());
    sessionDataBase.put(sessionId, sessionData);
    return sessionId;
  }

  @Override
  public SessionData getSessionData(String sessionId) {
    return sessionDataBase.get(sessionId);
  }

  @Override
  public void storeSecurityKeyData(String accountName, SecurityKeyData securityKeyData) {
    securityKeyDataBase.put(accountName, securityKeyData);
  }

  @Override
  public SecurityKeyData getSecurityKeyData(String accountName) {
    return securityKeyDataBase.get(accountName);
  }

  @Override
  public Set<X509Certificate> getTrustedCertificates() {
    return trustedCertificateDataBase;
  }

  @Override
  public void addTrustedCertificate(X509Certificate certificate) {
    trustedCertificateDataBase.add(certificate);
  }
}
