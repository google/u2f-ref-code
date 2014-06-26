// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.codec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.u2f.TestVectors;
import com.google.u2f.key.UserPresenceVerifier;
import com.google.u2f.key.messages.AuthenticateRequest;
import com.google.u2f.key.messages.AuthenticateResponse;
import com.google.u2f.key.messages.RegisterRequest;
import com.google.u2f.key.messages.RegisterResponse;

public class RawCodecTest extends TestVectors {

  @Test
  public void testEncodeRegisterRequest() throws Exception {
    RegisterRequest registerRequest = new RegisterRequest(APP_ID_ENROLL_SHA256,
        BROWSER_DATA_ENROLL_SHA256);

    byte[] encodedBytes = RawMessageCodec.encodeRegisterRequest(registerRequest);

    assertArrayEquals(REGISTRATION_REQUEST_DATA, encodedBytes);
  }

  @Test
  public void testDecodeRegisterRequest() throws Exception {
    RegisterRequest registerRequest = RawMessageCodec.decodeRegisterRequest(REGISTRATION_REQUEST_DATA);

    assertEquals(new RegisterRequest(APP_ID_ENROLL_SHA256, BROWSER_DATA_ENROLL_SHA256),
        registerRequest);
  }

  @Test
  public void testEncodeRegisterResponse() throws Exception {
    RegisterResponse registerResponse = new RegisterResponse(USER_PUBLIC_KEY_ENROLL_HEX,
        KEY_HANDLE, VENDOR_CERTIFICATE, SIGNATURE_ENROLL);

    byte[] encodedBytes = RawMessageCodec.encodeRegisterResponse(registerResponse);

    assertArrayEquals(REGISTRATION_RESPONSE_DATA, encodedBytes);
  }

  @Test
  public void testEncodeRegisterSignedBytes() throws Exception {
    byte[] encodedBytes = RawMessageCodec.encodeRegistrationSignedBytes(APP_ID_ENROLL_SHA256,
        BROWSER_DATA_ENROLL_SHA256, KEY_HANDLE, USER_PUBLIC_KEY_ENROLL_HEX);

    assertArrayEquals(EXPECTED_REGISTER_SIGNED_BYTES, encodedBytes);
  }

  @Test
  public void testDecodeRegisterResponse() throws Exception {
    RegisterResponse registerResponse = RawMessageCodec.decodeRegisterResponse(REGISTRATION_RESPONSE_DATA);

    assertEquals(new RegisterResponse(USER_PUBLIC_KEY_ENROLL_HEX,
        KEY_HANDLE, VENDOR_CERTIFICATE, SIGNATURE_ENROLL), registerResponse);
  }

  @Test
  public void testEncodeAuthenticateRequest() throws Exception {
    AuthenticateRequest authenticateRequest = new AuthenticateRequest(
        AuthenticateRequest.USER_PRESENCE_SIGN, BROWSER_DATA_SIGN_SHA256, APP_ID_SIGN_SHA256,
        KEY_HANDLE);

    byte[] encodedBytes = RawMessageCodec.encodeAuthenticateRequest(authenticateRequest);

    assertArrayEquals(SIGN_REQUEST_DATA, encodedBytes);
  }

  @Test
  public void testDecodeAuthenticateRequest() throws Exception {
    AuthenticateRequest authenticateRequest = RawMessageCodec.decodeAuthenticateRequest(SIGN_REQUEST_DATA);

    assertEquals(new AuthenticateRequest(AuthenticateRequest.USER_PRESENCE_SIGN,
        BROWSER_DATA_SIGN_SHA256, APP_ID_SIGN_SHA256, KEY_HANDLE), authenticateRequest);
  }

  @Test
  public void testEncodeAuthenticateResponse() throws Exception {
    AuthenticateResponse authenticateResponse = new AuthenticateResponse(
        UserPresenceVerifier.USER_PRESENT_FLAG, COUNTER_VALUE, SIGNATURE_AUTHENTICATE);

    byte[] encodedBytes = RawMessageCodec.encodeAuthenticateResponse(authenticateResponse);

    assertArrayEquals(SIGN_RESPONSE_DATA, encodedBytes);
  }

  @Test
  public void testDecodeAuthenticateResponse() throws Exception {
    AuthenticateResponse authenticateResponse = RawMessageCodec.decodeAuthenticateResponse(SIGN_RESPONSE_DATA);

    assertEquals(new AuthenticateResponse(UserPresenceVerifier.USER_PRESENT_FLAG, COUNTER_VALUE,
        SIGNATURE_AUTHENTICATE), authenticateResponse);
  }

  @Test
  public void testEncodeAuthenticateSignedBytes() throws Exception {
    byte[] encodedBytes = RawMessageCodec.encodeAuthenticateSignedBytes(APP_ID_SIGN_SHA256,
        UserPresenceVerifier.USER_PRESENT_FLAG, COUNTER_VALUE, BROWSER_DATA_SIGN_SHA256);

    assertArrayEquals(EXPECTED_AUTHENTICATE_SIGNED_BYTES, encodedBytes);
  }
}
