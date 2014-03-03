package com.google.u2f.server;

import java.security.cert.X509Certificate;
import java.util.Set;

import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SessionData;

public interface DataStore {
	public void addTrustedCertificate(X509Certificate certificate);

	public Set<X509Certificate> getTrustedCertificates();

	public void storeSessionData(String sessionId, SessionData sessionData);

	public SessionData getSessionData(String sessionId);

	public void storeSecurityKeyData(String accountName, SecurityKeyData securityKeyData);

	public SecurityKeyData getSecurityKeyData(String accountName);
}
