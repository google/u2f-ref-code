// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.client.impl;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.google.common.collect.ImmutableList;
import com.google.u2f.TestVectors;
import com.google.u2f.U2FConsts;
import com.google.u2f.client.ChannelIdProvider;
import com.google.u2f.client.OriginVerifier;
import com.google.u2f.key.U2FKey;
import com.google.u2f.key.UserPresenceVerifier;
import com.google.u2f.key.messages.AuthenticateRequest;
import com.google.u2f.key.messages.AuthenticateResponse;
import com.google.u2f.key.messages.RegisterRequest;
import com.google.u2f.key.messages.RegisterResponse;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.messages.RegisteredKey;
import com.google.u2f.server.messages.RegistrationRequest;
import com.google.u2f.server.messages.RegistrationResponse;
import com.google.u2f.server.messages.SignResponse;
import com.google.u2f.server.messages.U2fSignRequest;

public class U2FClientReferenceImplTest extends TestVectors {

  @Mock U2FKey mockU2fKey;
  @Mock U2FServer mockU2fServer;
  @Mock OriginVerifier mockOriginVerifier;
  @Mock ChannelIdProvider mockChannelIdProvider;

  private U2FClientReferenceImpl u2fClient;

  @Before
  public void setup() throws Exception {
    initMocks(this);

    u2fClient = new U2FClientReferenceImpl(new CryptoImpl(), mockOriginVerifier,
        mockChannelIdProvider, mockU2fServer, mockU2fKey);

    when(mockChannelIdProvider.getJsonChannelId()).thenReturn(CHANNEL_ID_JSON);
  }

  @Test
  public void testRegister() throws Exception {
    when(mockU2fServer.getRegistrationRequest(ACCOUNT_NAME, APP_ID_ENROLL)).thenReturn(
        new RegistrationRequest(U2FConsts.U2F_V2, SERVER_CHALLENGE_ENROLL_BASE64, APP_ID_ENROLL,
            SESSION_ID));
    doNothing().when(mockOriginVerifier).validateOrigin(APP_ID_ENROLL, ORIGIN);
    when(mockU2fKey.register(new RegisterRequest(APP_ID_ENROLL_SHA256, BROWSER_DATA_ENROLL_SHA256)))
        .thenReturn(new RegisterResponse(USER_PUBLIC_KEY_ENROLL_HEX, KEY_HANDLE, VENDOR_CERTIFICATE,
            SIGNATURE_ENROLL));
    when(mockU2fServer.processRegistrationResponse(
        new RegistrationResponse(REGISTRATION_DATA_BASE64, BROWSER_DATA_ENROLL_BASE64, SESSION_ID), 0L))
        .thenReturn(new SecurityKeyData(0L, KEY_HANDLE, USER_PUBLIC_KEY_ENROLL_HEX, VENDOR_CERTIFICATE, 0));

    u2fClient.register(ORIGIN, ACCOUNT_NAME);
  }

  @Test
  public void testAuthenticate() throws Exception {
    when(mockU2fServer.getSignRequest(ACCOUNT_NAME, ORIGIN)).thenReturn(
        new U2fSignRequest(SERVER_CHALLENGE_SIGN_BASE64,  
        ImmutableList.of(new RegisteredKey(U2FConsts.U2F_V2, KEY_HANDLE_BASE64, null /* transports */ , APP_ID_SIGN,
            SESSION_ID))));
    doNothing().when(mockOriginVerifier).validateOrigin(APP_ID_SIGN, ORIGIN);
    when(
        mockU2fKey.authenticate(new AuthenticateRequest(UserPresenceVerifier.USER_PRESENT_FLAG,
            BROWSER_DATA_SIGN_SHA256, APP_ID_SIGN_SHA256, KEY_HANDLE)))
        .thenReturn(
                new AuthenticateResponse(UserPresenceVerifier.USER_PRESENT_FLAG, COUNTER_VALUE,
                    SIGNATURE_AUTHENTICATE));
    when(mockU2fServer.processSignResponse(
        new SignResponse(KEY_HANDLE_BASE64, SIGN_RESPONSE_DATA_BASE64, BROWSER_DATA_SIGN_BASE64, SESSION_ID)))
        .thenReturn(new SecurityKeyData(0L, KEY_HANDLE, USER_PUBLIC_KEY_ENROLL_HEX, VENDOR_CERTIFICATE, 0));

    u2fClient.authenticate(ORIGIN, ACCOUNT_NAME);
  }
}
