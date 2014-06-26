// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.key.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.Signature;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.google.u2f.TestVectors;
import com.google.u2f.key.DataStore;
import com.google.u2f.key.KeyHandleGenerator;
import com.google.u2f.key.KeyPairGenerator;
import com.google.u2f.key.U2FKey;
import com.google.u2f.key.UserPresenceVerifier;
import com.google.u2f.key.messages.AuthenticateRequest;
import com.google.u2f.key.messages.AuthenticateResponse;
import com.google.u2f.key.messages.RegisterRequest;
import com.google.u2f.key.messages.RegisterResponse;

public class U2FKeyReferenceImplTest extends TestVectors {

  @Mock KeyPairGenerator mockKeyPairGenerator;
  @Mock KeyHandleGenerator mockKeyHandleGenerator;
  @Mock DataStore mockDataStore;
  @Mock UserPresenceVerifier mockUserPresenceVerifier;

  private U2FKey u2fKey;

  @Before
  public void setup() {

    initMocks(this);

    u2fKey = new U2FKeyReferenceImpl(
        VENDOR_CERTIFICATE,
        VENDOR_CERTIFICATE_PRIVATE_KEY,
        mockKeyPairGenerator,
        mockKeyHandleGenerator,
        mockDataStore,
        mockUserPresenceVerifier,
        new BouncyCastleCrypto());

    when(mockUserPresenceVerifier.verifyUserPresence()).thenReturn(
        UserPresenceVerifier.USER_PRESENT_FLAG);
    when(mockKeyPairGenerator.generateKeyPair(APP_ID_ENROLL_SHA256, BROWSER_DATA_ENROLL_SHA256))
    .thenReturn(USER_KEY_PAIR_ENROLL);
    when(mockKeyPairGenerator.encodePublicKey(USER_PUBLIC_KEY_ENROLL)).thenReturn(
        USER_PUBLIC_KEY_ENROLL_HEX);
    when(mockKeyHandleGenerator.generateKeyHandle(APP_ID_ENROLL_SHA256, USER_KEY_PAIR_ENROLL))
    .thenReturn(KEY_HANDLE);
    when(mockDataStore.getKeyPair(KEY_HANDLE)).thenReturn(USER_KEY_PAIR_SIGN);
    when(mockDataStore.incrementCounter()).thenReturn(COUNTER_VALUE);
  }

  @Test
  public void testRegister() throws Exception {
    RegisterRequest registerRequest = new RegisterRequest(APP_ID_ENROLL_SHA256,
        BROWSER_DATA_ENROLL_SHA256);

    RegisterResponse registerResponse = u2fKey.register(registerRequest);

    verify(mockDataStore).storeKeyPair(KEY_HANDLE, USER_KEY_PAIR_ENROLL);
    assertArrayEquals(USER_PUBLIC_KEY_ENROLL_HEX, registerResponse.getUserPublicKey());
    assertEquals(VENDOR_CERTIFICATE, registerResponse.getAttestationCertificate());
    assertArrayEquals(KEY_HANDLE, registerResponse.getKeyHandle());
    Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA");
    ecdsaSignature.initVerify(VENDOR_CERTIFICATE.getPublicKey());
    ecdsaSignature.update(EXPECTED_REGISTER_SIGNED_BYTES);
    assertTrue(ecdsaSignature.verify(registerResponse.getSignature()));
  }

  @Test
  public void testAuthenticate() throws Exception {
    AuthenticateRequest authenticateRequest = new AuthenticateRequest(
        AuthenticateRequest.USER_PRESENCE_SIGN, BROWSER_DATA_SIGN_SHA256, APP_ID_SIGN_SHA256,
        KEY_HANDLE);

    AuthenticateResponse authenticateResponse = u2fKey.authenticate(authenticateRequest);

    assertEquals(UserPresenceVerifier.USER_PRESENT_FLAG, authenticateResponse.getUserPresence());
    assertEquals(COUNTER_VALUE, authenticateResponse.getCounter());
    Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA");
    ecdsaSignature.initVerify(USER_PUBLIC_KEY_SIGN);
    ecdsaSignature.update(EXPECTED_AUTHENTICATE_SIGNED_BYTES);
    assertTrue(ecdsaSignature.verify(authenticateResponse.getSignature()));
  }
}
