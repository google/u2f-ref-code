// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.u2f.TestVectors;
import com.google.u2f.U2FException;
import com.google.u2f.server.ChallengeGenerator;
import com.google.u2f.server.Crypto;
import com.google.u2f.server.DataStore;
import com.google.u2f.server.SessionIdGenerator;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.data.EnrollSessionData;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SecurityKeyData.Transports;
import com.google.u2f.server.data.SignSessionData;
import com.google.u2f.server.messages.RegisteredKey;
import com.google.u2f.server.messages.RegistrationRequest;
import com.google.u2f.server.messages.RegistrationResponse;
import com.google.u2f.server.messages.SignResponse;
import com.google.u2f.server.messages.U2fSignRequest;

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
    when(mockDataStore.storeSessionData(Matchers.<EnrollSessionData>any())).thenReturn(SESSION_ID);
    when(mockDataStore.getTrustedCertificates()).thenReturn(trustedCertificates);
    when(mockDataStore.getSecurityKeyData(ACCOUNT_NAME)).thenReturn(
        ImmutableList.of(new SecurityKeyData(0L, KEY_HANDLE, USER_PUBLIC_KEY_SIGN_HEX, VENDOR_CERTIFICATE, 0)));
  }

  @Test
  public void testSanitizeOrigin() {
    assertEquals("http://example.com", U2FServerReferenceImpl.canonicalizeOrigin("http://example.com"));
    assertEquals("http://example.com", U2FServerReferenceImpl.canonicalizeOrigin("http://example.com/"));
    assertEquals("http://example.com", U2FServerReferenceImpl.canonicalizeOrigin("http://example.com/foo"));
    assertEquals("http://example.com", U2FServerReferenceImpl.canonicalizeOrigin("http://example.com/foo?bar=b"));
    assertEquals("http://example.com", U2FServerReferenceImpl.canonicalizeOrigin("http://example.com/foo#fragment"));
    assertEquals("https://example.com", U2FServerReferenceImpl.canonicalizeOrigin("https://example.com"));
    assertEquals("https://example.com", U2FServerReferenceImpl.canonicalizeOrigin("https://example.com/foo"));
  }

  @Test
  public void testGetRegistrationRequest() throws U2FException {
    u2fServer = new U2FServerReferenceImpl(mockChallengeGenerator,
        mockDataStore, cryto, TRUSTED_DOMAINS);

    RegistrationRequest registrationRequest = u2fServer.getRegistrationRequest(ACCOUNT_NAME, APP_ID_ENROLL);

    assertEquals(new RegistrationRequest("U2F_V2", SERVER_CHALLENGE_ENROLL_BASE64, APP_ID_ENROLL,
        SESSION_ID), registrationRequest);
  }

  @Test
  public void testProcessRegistrationResponse_noTransports() throws U2FException {
	  when(mockDataStore.getEnrollSessionData(SESSION_ID)).thenReturn(
        new EnrollSessionData(ACCOUNT_NAME, APP_ID_ENROLL, SERVER_CHALLENGE_ENROLL));
    u2fServer = new U2FServerReferenceImpl(mockChallengeGenerator,
        mockDataStore, cryto, TRUSTED_DOMAINS);

    RegistrationResponse registrationResponse = new RegistrationResponse(REGISTRATION_DATA_BASE64,
        BROWSER_DATA_ENROLL_BASE64, SESSION_ID);

    u2fServer.processRegistrationResponse(registrationResponse, 0L);

    verify(mockDataStore).addSecurityKeyData(eq(ACCOUNT_NAME),
        eq(new SecurityKeyData(0L, null, KEY_HANDLE, USER_PUBLIC_KEY_ENROLL_HEX, VENDOR_CERTIFICATE, 0)));
  }

  @Test
  public void testProcessRegistrationResponse_oneTransport() throws U2FException {
    when(mockDataStore.getEnrollSessionData(SESSION_ID)).thenReturn(
        new EnrollSessionData(ACCOUNT_NAME, APP_ID_ENROLL, SERVER_CHALLENGE_ENROLL));
    HashSet<X509Certificate> trustedCertificates = new HashSet<X509Certificate>();
    trustedCertificates.add(TRUSTED_CERTIFICATE_ONE_TRANSPORT);
    when(mockDataStore.getTrustedCertificates()).thenReturn(trustedCertificates);
    u2fServer = new U2FServerReferenceImpl(mockChallengeGenerator,
        mockDataStore, cryto, TRUSTED_DOMAINS);

    RegistrationResponse registrationResponse = new RegistrationResponse(
        REGISTRATION_RESPONSE_DATA_ONE_TRANSPORT_BASE64,
        BROWSER_DATA_ENROLL_BASE64, SESSION_ID);
    u2fServer.processRegistrationResponse(registrationResponse, 0L);

    List<Transports> transports = new LinkedList<Transports>();
    transports.add(Transports.BLUETOOTH_BREDR);
    verify(mockDataStore).addSecurityKeyData(eq(ACCOUNT_NAME),
        eq(new SecurityKeyData(0L, transports, KEY_HANDLE, USER_PUBLIC_KEY_ENROLL_HEX,
            TRUSTED_CERTIFICATE_ONE_TRANSPORT, 0)));
  }

  @Test
  public void testProcessRegistrationResponse_multipleTransports() throws U2FException {
    when(mockDataStore.getEnrollSessionData(SESSION_ID)).thenReturn(
        new EnrollSessionData(ACCOUNT_NAME, APP_ID_ENROLL, SERVER_CHALLENGE_ENROLL));
    HashSet<X509Certificate> trustedCertificates = new HashSet<X509Certificate>();
    trustedCertificates.add(TRUSTED_CERTIFICATE_MULTIPLE_TRANSPORTS);
    when(mockDataStore.getTrustedCertificates()).thenReturn(trustedCertificates);
    u2fServer = new U2FServerReferenceImpl(mockChallengeGenerator,
        mockDataStore, cryto, TRUSTED_DOMAINS);

    RegistrationResponse registrationResponse = new RegistrationResponse(
        REGISTRATION_RESPONSE_DATA_MULTIPLE_TRANSPORTS_BASE64,
        BROWSER_DATA_ENROLL_BASE64, SESSION_ID);
    u2fServer.processRegistrationResponse(registrationResponse, 0L);

    List<Transports> transports = new LinkedList<Transports>();
    transports.add(Transports.BLUETOOTH_BREDR);
    transports.add(Transports.BLUETOOTH_LOW_ENERGY);
    transports.add(Transports.NFC);
    verify(mockDataStore).addSecurityKeyData(eq(ACCOUNT_NAME),
        eq(new SecurityKeyData(0L, transports, KEY_HANDLE, USER_PUBLIC_KEY_ENROLL_HEX,
            TRUSTED_CERTIFICATE_MULTIPLE_TRANSPORTS, 0)));
  }

  @Test
  public void testProcessRegistrationResponse_malformedTransports() throws U2FException {
    when(mockDataStore.getEnrollSessionData(SESSION_ID)).thenReturn(
        new EnrollSessionData(ACCOUNT_NAME, APP_ID_ENROLL, SERVER_CHALLENGE_ENROLL));
    HashSet<X509Certificate> trustedCertificates = new HashSet<X509Certificate>();
    trustedCertificates.add(TRUSTED_CERTIFICATE_MALFORMED_TRANSPORTS_EXTENSION);
    when(mockDataStore.getTrustedCertificates()).thenReturn(trustedCertificates);
    u2fServer = new U2FServerReferenceImpl(mockChallengeGenerator,
        mockDataStore, cryto, TRUSTED_DOMAINS);

    RegistrationResponse registrationResponse = new RegistrationResponse(
        REGISTRATION_RESPONSE_DATA_MALFORMED_TRANSPORTS_BASE64,
        BROWSER_DATA_ENROLL_BASE64, SESSION_ID);
    u2fServer.processRegistrationResponse(registrationResponse, 0L);

    verify(mockDataStore).addSecurityKeyData(eq(ACCOUNT_NAME),
        eq(new SecurityKeyData(0L, null /* transports */, KEY_HANDLE, USER_PUBLIC_KEY_ENROLL_HEX,
            TRUSTED_CERTIFICATE_MALFORMED_TRANSPORTS_EXTENSION, 0)));
  }

  @Test
  public void testProcessRegistrationResponse2() throws U2FException {
    when(mockDataStore.getEnrollSessionData(SESSION_ID)).thenReturn(
        new EnrollSessionData(ACCOUNT_NAME, APP_ID_ENROLL, SERVER_CHALLENGE_ENROLL));
    HashSet<X509Certificate> trustedCertificates = new HashSet<X509Certificate>();
    trustedCertificates.add(VENDOR_CERTIFICATE);
    trustedCertificates.add(TRUSTED_CERTIFICATE_2);
    when(mockDataStore.getTrustedCertificates()).thenReturn(trustedCertificates);
    u2fServer = new U2FServerReferenceImpl(mockChallengeGenerator,
        mockDataStore, cryto, TRUSTED_DOMAINS);

    RegistrationResponse registrationResponse = new RegistrationResponse(REGISTRATION_DATA_2_BASE64,
        BROWSER_DATA_2_BASE64, SESSION_ID);

    u2fServer.processRegistrationResponse(registrationResponse, 0L);
    verify(mockDataStore).addSecurityKeyData(eq(ACCOUNT_NAME),
        eq(new SecurityKeyData(0L, null /* transports */, KEY_HANDLE_2, USER_PUBLIC_KEY_2,
            TRUSTED_CERTIFICATE_2, 0)));
  }

  @Test
  public void testGetSignRequest() throws U2FException {
    u2fServer = new U2FServerReferenceImpl(mockChallengeGenerator,
        mockDataStore, cryto, TRUSTED_DOMAINS);
    when(mockChallengeGenerator.generateChallenge(ACCOUNT_NAME)).thenReturn(SERVER_CHALLENGE_SIGN);

    U2fSignRequest signRequest = u2fServer.getSignRequest(ACCOUNT_NAME, APP_ID_SIGN);
    assertEquals(new RegisteredKey("U2F_V2", KEY_HANDLE_BASE64, null /* transports */, APP_ID_SIGN,
       SESSION_ID), signRequest.getRegisteredKeys().get(0));
  }

  @Test
  public void testProcessSignResponse() throws U2FException {
    when(mockDataStore.getSignSessionData(SESSION_ID)).thenReturn(
        new SignSessionData(ACCOUNT_NAME, APP_ID_SIGN, SERVER_CHALLENGE_SIGN, USER_PUBLIC_KEY_SIGN_HEX));
    u2fServer = new U2FServerReferenceImpl(mockChallengeGenerator,
        mockDataStore, cryto, TRUSTED_DOMAINS);
    SignResponse signResponse = new SignResponse(KEY_HANDLE_BASE64, SIGN_RESPONSE_DATA_BASE64,
        BROWSER_DATA_SIGN_BASE64, SESSION_ID);

    u2fServer.processSignResponse(signResponse);
  }

  @Test
  public void testProcessSignResponse_badOrigin() throws U2FException {
    when(mockDataStore.getSignSessionData(SESSION_ID)).thenReturn(
        new SignSessionData(ACCOUNT_NAME, APP_ID_SIGN, SERVER_CHALLENGE_SIGN, USER_PUBLIC_KEY_SIGN_HEX));
    u2fServer = new U2FServerReferenceImpl(mockChallengeGenerator,
        mockDataStore, cryto, ImmutableSet.of("some-other-domain.com"));
    SignResponse signResponse = new SignResponse(KEY_HANDLE_BASE64, SIGN_RESPONSE_DATA_BASE64,
        BROWSER_DATA_SIGN_BASE64, SESSION_ID);

    try {
      u2fServer.processSignResponse(signResponse);
      fail("expected exception, but didn't get it");
    } catch(U2FException e) {
      assertTrue(e.getMessage().contains("is not a recognized home origin"));
    }
  }

  // @Test
  // TODO: put test back in once we have signature sample on a correct browserdata json
  // (currently, this test uses an enrollment browserdata during a signature)
  public void testProcessSignResponse2() throws U2FException {
    when(mockDataStore.getSignSessionData(SESSION_ID)).thenReturn(
        new SignSessionData(ACCOUNT_NAME, APP_ID_2, SERVER_CHALLENGE_SIGN, USER_PUBLIC_KEY_2));
    when(mockDataStore.getSecurityKeyData(ACCOUNT_NAME)).thenReturn(
        ImmutableList.of(new SecurityKeyData(0l, KEY_HANDLE_2, USER_PUBLIC_KEY_2, VENDOR_CERTIFICATE, 0)));
    u2fServer = new U2FServerReferenceImpl(mockChallengeGenerator,
        mockDataStore, cryto, TRUSTED_DOMAINS);
    SignResponse signResponse = new SignResponse(KEY_HANDLE_2_BASE64, SIGN_DATA_2_BASE64,
        BROWSER_DATA_2_BASE64, SESSION_ID);

    u2fServer.processSignResponse(signResponse);
  }
}
