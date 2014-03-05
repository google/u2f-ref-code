package com.google.u2f.server.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.cert.X509Certificate;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.google.u2f.TestVectors;
import com.google.u2f.U2FException;
import com.google.u2f.server.ChallengeGenerator;
import com.google.u2f.server.Crypto;
import com.google.u2f.server.DataStore;
import com.google.u2f.server.SessionIdGenerator;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SessionData;
import com.google.u2f.server.messages.RegistrationRequest;
import com.google.u2f.server.messages.RegistrationResponse;
import com.google.u2f.server.messages.SignRequest;
import com.google.u2f.server.messages.SignResponse;

public class U2FServerReferenceImplTest extends TestVectors {
  @Mock ChallengeGenerator mockChallengeGenerator;
  @Mock SessionIdGenerator mockSessionIdGenerator;
  @Mock DataStore mockDataStore;

  private final Crypto cryto = new BouncyCastleCrypto();
  private U2FServer u2fServer;

  @Before
  public void setup() throws Exception {
    initMocks(this);

    HashSet<X509Certificate> trustedCertificates = new HashSet<X509Certificate>();
    trustedCertificates.add(VENDOR_CERTIFICATE);

    when(mockChallengeGenerator.generateChallenge(ACCOUNT_NAME))
    .thenReturn(SERVER_CHALLENGE_ENROLL);
    when(mockSessionIdGenerator.generateSessionId(ACCOUNT_NAME)).thenReturn(SESSION_ID);
    when(mockDataStore.getTrustedCertificates()).thenReturn(trustedCertificates);
    when(mockDataStore.getSessionData(SESSION_ID)).thenReturn(
        new SessionData(ACCOUNT_NAME, SERVER_CHALLENGE_ENROLL));
    when(mockDataStore.getSecurityKeyData(ACCOUNT_NAME)).thenReturn(
        new SecurityKeyData(KEY_HANDLE, USER_PUBLIC_KEY_SIGN_HEX));
  }

  @Test
  public void testGetRegistrationRequest() throws U2FException {
    u2fServer = new U2FServerReferenceImpl(APP_ID_ENROLL, mockChallengeGenerator,
        mockSessionIdGenerator, mockDataStore, cryto);

    RegistrationRequest registrationRequest = u2fServer.getRegistrationRequest(ACCOUNT_NAME);

    assertEquals(new RegistrationRequest("U2F_V2", SERVER_CHALLENGE_ENROLL_BASE64, APP_ID_ENROLL,
        SESSION_ID), registrationRequest);
  }

  @Test
  public void testProcessRegistrationResponse() throws U2FException {
    u2fServer = new U2FServerReferenceImpl(APP_ID_ENROLL, mockChallengeGenerator,
        mockSessionIdGenerator, mockDataStore, cryto);

    RegistrationResponse registrationResponse = new RegistrationResponse(REGISTRATION_DATA_BASE64,
        BROWSER_DATA_ENROLL_BASE64, SESSION_ID);

    u2fServer.processRegistrationResponse(registrationResponse);

    verify(mockDataStore).storeSecurityKeyData(ACCOUNT_NAME,
        new SecurityKeyData(KEY_HANDLE, USER_PUBLIC_KEY_ENROLL_HEX));
  }

  @Test
  public void testProcessRegistrationResponse2() throws U2FException {
    HashSet<X509Certificate> trustedCertificates = new HashSet<X509Certificate>();
    trustedCertificates.add(VENDOR_CERTIFICATE);
    trustedCertificates.add(TRUSTED_CERTIFICATE_2);
    when(mockDataStore.getTrustedCertificates()).thenReturn(trustedCertificates);
    u2fServer = new U2FServerReferenceImpl(APP_ID_2, mockChallengeGenerator,
        mockSessionIdGenerator, mockDataStore, cryto);

    RegistrationResponse registrationResponse = new RegistrationResponse(REGISTRATION_DATA_2_BASE64,
        BROWSER_DATA_2_BASE64, SESSION_ID);

    u2fServer.processRegistrationResponse(registrationResponse);

    verify(mockDataStore).storeSecurityKeyData(ACCOUNT_NAME,
        new SecurityKeyData(KEY_HANDLE_2, USER_PUBLIC_KEY_2));
  }

  @Test
  public void testGetSignRequest() throws U2FException {
    u2fServer = new U2FServerReferenceImpl(APP_ID_SIGN, mockChallengeGenerator,
        mockSessionIdGenerator, mockDataStore, cryto);
    when(mockChallengeGenerator.generateChallenge(ACCOUNT_NAME)).thenReturn(SERVER_CHALLENGE_SIGN);

    SignRequest signRequest = u2fServer.getSignRequest(ACCOUNT_NAME);

    assertEquals(new SignRequest("U2F_V2", SERVER_CHALLENGE_SIGN_BASE64, APP_ID_SIGN,
        KEY_HANDLE_BASE64, SESSION_ID), signRequest);
  }

  @Test
  public void testProcessSignResponse() throws U2FException {
    u2fServer = new U2FServerReferenceImpl(APP_ID_SIGN, mockChallengeGenerator,
        mockSessionIdGenerator, mockDataStore, cryto);
    SignResponse signResponse = new SignResponse(BROWSER_DATA_SIGN_BASE64,
        SIGN_RESPONSE_DATA_BASE64, SERVER_CHALLENGE_SIGN_BASE64, SESSION_ID, APP_ID_SIGN);

    u2fServer.processSignResponse(signResponse);
  }

  @Test
  public void testProcessSignResponse2() throws U2FException {
    when(mockDataStore.getSecurityKeyData(ACCOUNT_NAME)).thenReturn(
        new SecurityKeyData(KEY_HANDLE_2, USER_PUBLIC_KEY_2));
    u2fServer = new U2FServerReferenceImpl(APP_ID_2, mockChallengeGenerator,
        mockSessionIdGenerator, mockDataStore, cryto);
    SignResponse signResponse = new SignResponse(BROWSER_DATA_2_BASE64, SIGN_DATA_2_BASE64,
        CHALLENGE_2_BASE64, SESSION_ID, APP_ID_2);

    u2fServer.processSignResponse(signResponse);
  }
}
