package com.google.u2f.server;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import com.google.u2f.server.data.EnrollSessionData;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SignSessionData;

public interface DataStore {
  public void addTrustedCertificate(X509Certificate certificate);

  public Set<X509Certificate> getTrustedCertificates();

  public /* sessionId */ String storeSessionData(EnrollSessionData sessionData);

  public SignSessionData getSignSessionData(String sessionId);
  
  public EnrollSessionData getEnrollSessionData(String sessionId);

  public void storeSecurityKeyData(String accountName, SecurityKeyData securityKeyData);

  public List<SecurityKeyData> getSecurityKeyData(String accountName);
}
