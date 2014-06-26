// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.key.impl;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Hex;

import com.google.u2f.U2FException;
import com.google.u2f.codec.RawMessageCodec;
import com.google.u2f.key.Crypto;
import com.google.u2f.key.DataStore;
import com.google.u2f.key.KeyHandleGenerator;
import com.google.u2f.key.KeyPairGenerator;
import com.google.u2f.key.U2FKey;
import com.google.u2f.key.UserPresenceVerifier;
import com.google.u2f.key.messages.AuthenticateRequest;
import com.google.u2f.key.messages.AuthenticateResponse;
import com.google.u2f.key.messages.RegisterRequest;
import com.google.u2f.key.messages.RegisterResponse;

public class U2FKeyReferenceImpl implements U2FKey {
  private static final Logger Log = Logger.getLogger(U2FKeyReferenceImpl.class.getName());

  private final X509Certificate vendorCertificate;
  private final PrivateKey certificatePrivateKey;
  private final KeyPairGenerator keyPairGenerator;
  private final KeyHandleGenerator keyHandleGenerator;
  private final DataStore dataStore;
  private final UserPresenceVerifier userPresenceVerifier;
  private final Crypto crypto;

  public U2FKeyReferenceImpl(X509Certificate vendorCertificate, PrivateKey certificatePrivateKey,
      KeyPairGenerator keyPairGenerator, KeyHandleGenerator keyHandleGenerator,
      DataStore dataStore, UserPresenceVerifier userPresenceVerifier, Crypto crypto) {
    this.vendorCertificate = vendorCertificate;
    this.certificatePrivateKey = certificatePrivateKey;
    this.keyPairGenerator = keyPairGenerator;
    this.keyHandleGenerator = keyHandleGenerator;
    this.dataStore = dataStore;
    this.userPresenceVerifier = userPresenceVerifier;
    this.crypto = crypto;
  }

  @Override
  public RegisterResponse register(RegisterRequest registerRequest) throws U2FException {
    Log.info(">> register");

    byte[] applicationSha256 = registerRequest.getApplicationSha256();
    byte[] challengeSha256 = registerRequest.getChallengeSha256();

    Log.info(" -- Inputs --");
    Log.info("  applicationSha256: " + Hex.encodeHexString(applicationSha256));
    Log.info("  challengeSha256: " + Hex.encodeHexString(challengeSha256));

    byte userPresent = userPresenceVerifier.verifyUserPresence();
    if ((userPresent & UserPresenceVerifier.USER_PRESENT_FLAG) == 0) {
      throw new U2FException("Cannot verify user presence");
    }

    KeyPair keyPair = keyPairGenerator.generateKeyPair(applicationSha256, challengeSha256);
    byte[] keyHandle = keyHandleGenerator.generateKeyHandle(applicationSha256, keyPair);

    dataStore.storeKeyPair(keyHandle, keyPair);

    byte[] userPublicKey = keyPairGenerator.encodePublicKey(keyPair.getPublic());

    byte[] signedData = RawMessageCodec.encodeRegistrationSignedBytes(applicationSha256, challengeSha256,
        keyHandle, userPublicKey);
    Log.info("Signing bytes " + Hex.encodeHexString(signedData));

    byte[] signature = crypto.sign(signedData, certificatePrivateKey);

    Log.info(" -- Outputs --");
    Log.info("  userPublicKey: " + Hex.encodeHexString(userPublicKey));
    Log.info("  keyHandle: " + Hex.encodeHexString(keyHandle));
    Log.info("  vendorCertificate: " + vendorCertificate);
    Log.info("  signature: " + Hex.encodeHexString(signature));

    Log.info("<< register");

    return new RegisterResponse(userPublicKey, keyHandle, vendorCertificate, signature);
  }

  @Override
  public AuthenticateResponse authenticate(AuthenticateRequest authenticateRequest)
      throws U2FException {
    Log.info(">> authenticate");

    byte control = authenticateRequest.getControl();
    byte[] applicationSha256 = authenticateRequest.getApplicationSha256();
    byte[] challengeSha256 = authenticateRequest.getChallengeSha256();
    byte[] keyHandle = authenticateRequest.getKeyHandle();

    Log.info(" -- Inputs --");
    Log.info("  control: " + control);
    Log.info("  applicationSha256: " + Hex.encodeHexString(applicationSha256));
    Log.info("  challengeSha256: " + Hex.encodeHexString(challengeSha256));
    Log.info("  keyHandle: " + Hex.encodeHexString(keyHandle));

    KeyPair keyPair = dataStore.getKeyPair(keyHandle);
    int counter = dataStore.incrementCounter();
    byte userPresence = userPresenceVerifier.verifyUserPresence();
    byte[] signedData = RawMessageCodec.encodeAuthenticateSignedBytes(applicationSha256, userPresence,
        counter, challengeSha256);

    Log.info("Signing bytes " + Hex.encodeHexString(signedData));

    byte[] signature = crypto.sign(signedData, keyPair.getPrivate());

    Log.info(" -- Outputs --");
    Log.info("  userPresence: " + userPresence);
    Log.info("  counter: " + counter);
    Log.info("  signature: " + Hex.encodeHexString(signature));

    Log.info("<< authenticate");

    return new AuthenticateResponse(userPresence, counter, signature);
  }
}
