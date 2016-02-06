// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.impl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.u2f.U2FConsts;
import com.google.u2f.U2FException;
import com.google.u2f.codec.RawMessageCodec;
import com.google.u2f.key.UserPresenceVerifier;
import com.google.u2f.key.messages.AuthenticateResponse;
import com.google.u2f.key.messages.RegisterResponse;
import com.google.u2f.server.ChallengeGenerator;
import com.google.u2f.server.Crypto;
import com.google.u2f.server.DataStore;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.data.EnrollSessionData;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SecurityKeyData.Transports;
import com.google.u2f.server.data.SignSessionData;
import com.google.u2f.server.impl.attestation.u2f.U2fAttestation;
import com.google.u2f.server.messages.RegisteredKey;
import com.google.u2f.server.messages.RegistrationRequest;
import com.google.u2f.server.messages.RegistrationResponse;
import com.google.u2f.server.messages.SignResponse;
import com.google.u2f.server.messages.U2fSignRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class U2FServerReferenceImpl implements U2FServer {
  private static final String TYPE_PARAM = "typ";
  private static final String CHALLENGE_PARAM = "challenge";
  private static final String ORIGIN_PARAM = "origin";

  // TODO: use these for channel id checks in verifyBrowserData
  @SuppressWarnings("unused")
  private static final String CHANNEL_ID_PARAM = "cid_pubkey";

  @SuppressWarnings("unused")
  private static final String UNUSED_CHANNEL_ID = "";

  private static final Logger Log = Logger.getLogger(U2FServerReferenceImpl.class.getName());

  private final ChallengeGenerator challengeGenerator;
  private final DataStore dataStore;
  private final Crypto cryto;
  private final Set<String> allowedOrigins;

  public U2FServerReferenceImpl(ChallengeGenerator challengeGenerator, DataStore dataStore,
      Crypto cryto, Set<String> origins) {
    this.challengeGenerator = challengeGenerator;
    this.dataStore = dataStore;
    this.cryto = cryto;
    this.allowedOrigins = canonicalizeOrigins(origins);
  }

  @Override
  public RegistrationRequest getRegistrationRequest(String accountName, String appId) {
    Log.info(">> getRegistrationRequest " + accountName);

    byte[] challenge = challengeGenerator.generateChallenge(accountName);
    EnrollSessionData sessionData = new EnrollSessionData(accountName, appId, challenge);

    String sessionId = dataStore.storeSessionData(sessionData);

    String challengeBase64 = Base64.encodeBase64URLSafeString(challenge);

    Log.info("-- Output --");
    Log.info("  sessionId: " + sessionId);
    Log.info("  challenge: " + Hex.encodeHexString(challenge));

    Log.info("<< getRegistrationRequest " + accountName);

    return new RegistrationRequest(U2FConsts.U2F_V2, challengeBase64, appId, sessionId);
  }

  @Override
  public SecurityKeyData processRegistrationResponse(
      RegistrationResponse registrationResponse, long currentTimeInMillis) throws U2FException {
    Log.info(">> processRegistrationResponse");

    String sessionId = registrationResponse.getSessionId();
    String clientDataBase64 = registrationResponse.getClientData();
    String rawRegistrationDataBase64 = registrationResponse.getRegistrationData();

    Log.info(">> rawRegistrationDataBase64: " + rawRegistrationDataBase64);
    EnrollSessionData sessionData = dataStore.getEnrollSessionData(sessionId);

    if (sessionData == null) {
      throw new U2FException("Unknown session_id");
    }

    String appId = sessionData.getAppId();
    String clientData = new String(Base64.decodeBase64(clientDataBase64));
    byte[] rawRegistrationData = Base64.decodeBase64(rawRegistrationDataBase64);
    Log.info("-- Input --");
    Log.info("  sessionId: " + sessionId);
    Log.info("  challenge: " + Hex.encodeHexString(sessionData.getChallenge()));
    Log.info("  accountName: " + sessionData.getAccountName());
    Log.info("  clientData: " + clientData);
    Log.info("  rawRegistrationData: " + Hex.encodeHexString(rawRegistrationData));

    RegisterResponse registerResponse = RawMessageCodec.decodeRegisterResponse(rawRegistrationData);

    byte[] userPublicKey = registerResponse.getUserPublicKey();
    byte[] keyHandle = registerResponse.getKeyHandle();
    X509Certificate attestationCertificate = registerResponse.getAttestationCertificate();
    byte[] signature = registerResponse.getSignature();
    List<Transports> transports = null;
    try {
      transports = U2fAttestation.Parse(attestationCertificate).getTransports();
    } catch (CertificateParsingException e) {
      Log.warning("Could not parse transports extension " + e.getMessage());
    }

    Log.info("-- Parsed rawRegistrationResponse --");
    Log.info("  userPublicKey: " + Hex.encodeHexString(userPublicKey));
    Log.info("  keyHandle: " + Hex.encodeHexString(keyHandle));
    Log.info("  attestationCertificate: " + attestationCertificate.toString());
    Log.info("  transports: " + transports);
    try {
      Log.info("  attestationCertificate bytes: "
          + Hex.encodeHexString(attestationCertificate.getEncoded()));
    } catch (CertificateEncodingException e) {
      throw new U2FException("Cannot encode certificate", e);
    }
    Log.info("  signature: " + Hex.encodeHexString(signature));

    byte[] appIdSha256 = cryto.computeSha256(appId.getBytes());
    byte[] clientDataSha256 = cryto.computeSha256(clientData.getBytes());
    byte[] signedBytes = RawMessageCodec.encodeRegistrationSignedBytes(
        appIdSha256, clientDataSha256, keyHandle, userPublicKey);

    Set<X509Certificate> trustedCertificates = dataStore.getTrustedCertificates();
    if (!trustedCertificates.contains(attestationCertificate)) {
      Log.warning("attestion cert is not trusted");
    }

    verifyBrowserData(
        new JsonParser().parse(clientData), "navigator.id.finishEnrollment", sessionData);

    Log.info("Verifying signature of bytes " + Hex.encodeHexString(signedBytes));
    if (!cryto.verifySignature(attestationCertificate, signedBytes, signature)) {
      throw new U2FException("Signature is invalid");
    }

    // The first time we create the SecurityKeyData, we set the counter value to 0.
    // We don't actually know what the counter value of the real device is - but it will
    // be something bigger (or equal) to 0, so subsequent signatures will check out ok.
    SecurityKeyData securityKeyData = new SecurityKeyData(currentTimeInMillis, transports,
        keyHandle, userPublicKey, attestationCertificate, /* initial counter value */ 0);
    dataStore.addSecurityKeyData(sessionData.getAccountName(), securityKeyData);

    Log.info("<< processRegistrationResponse");
    return securityKeyData;
  }

  @Override
  public U2fSignRequest getSignRequest(String accountName, String appId) throws U2FException {
    Log.info(">> getSignRequest " + accountName);

    List<SecurityKeyData> securityKeyDataList = dataStore.getSecurityKeyData(accountName);

    byte[] challenge = challengeGenerator.generateChallenge(accountName);
    String challengeBase64 = Base64.encodeBase64URLSafeString(challenge);

    ImmutableList.Builder<RegisteredKey> registeredKeys = ImmutableList.builder();
    Log.info("  challenge: " + Hex.encodeHexString(challenge));
    for (SecurityKeyData securityKeyData : securityKeyDataList) {
      SignSessionData sessionData =
          new SignSessionData(accountName, appId, challenge, securityKeyData.getPublicKey());
      String sessionId = dataStore.storeSessionData(sessionData);

      byte[] keyHandle = securityKeyData.getKeyHandle();
      List<Transports> transports = securityKeyData.getTransports();
      Log.info("-- Output --");
      Log.info("  sessionId: " + sessionId);
      Log.info("  keyHandle: " + Hex.encodeHexString(keyHandle));

      String keyHandleBase64 = Base64.encodeBase64URLSafeString(keyHandle);

      Log.info("<< getRegisteredKey " + accountName);
      registeredKeys.add(
          new RegisteredKey(U2FConsts.U2F_V2, keyHandleBase64, transports, appId, sessionId));
    }

    return new U2fSignRequest(challengeBase64, registeredKeys.build());
  }

  @Override
  public SecurityKeyData processSignResponse(SignResponse signResponse) throws U2FException {
    Log.info(">> processSignResponse");

    String sessionId = signResponse.getSessionId();
    String browserDataBase64 = signResponse.getClientData();
    String rawSignDataBase64 = signResponse.getSignatureData();

    SignSessionData sessionData = dataStore.getSignSessionData(sessionId);

    if (sessionData == null) {
      throw new U2FException("Unknown session_id");
    }

    String appId = sessionData.getAppId();
    SecurityKeyData securityKeyData = null;

    for (SecurityKeyData temp : dataStore.getSecurityKeyData(sessionData.getAccountName())) {
      if (Arrays.equals(sessionData.getPublicKey(), temp.getPublicKey())) {
        securityKeyData = temp;
        break;
      }
    }

    if (securityKeyData == null) {
      throw new U2FException("No security keys registered for this user");
    }

    String browserData = new String(Base64.decodeBase64(browserDataBase64));
    byte[] rawSignData = Base64.decodeBase64(rawSignDataBase64);

    Log.info("-- Input --");
    Log.info("  sessionId: " + sessionId);
    Log.info("  publicKey: " + Hex.encodeHexString(securityKeyData.getPublicKey()));
    Log.info("  challenge: " + Hex.encodeHexString(sessionData.getChallenge()));
    Log.info("  accountName: " + sessionData.getAccountName());
    Log.info("  browserData: " + browserData);
    Log.info("  rawSignData: " + Hex.encodeHexString(rawSignData));

    verifyBrowserData(
        new JsonParser().parse(browserData), "navigator.id.getAssertion", sessionData);

    AuthenticateResponse authenticateResponse =
        RawMessageCodec.decodeAuthenticateResponse(rawSignData);
    byte userPresence = authenticateResponse.getUserPresence();
    int counter = authenticateResponse.getCounter();
    byte[] signature = authenticateResponse.getSignature();

    Log.info("-- Parsed rawSignData --");
    Log.info("  userPresence: " + Integer.toHexString(userPresence & 0xFF));
    Log.info("  counter: " + counter);
    Log.info("  signature: " + Hex.encodeHexString(signature));

    if ((userPresence & UserPresenceVerifier.USER_PRESENT_FLAG) == 0) {
      throw new U2FException("User presence invalid during authentication");
    }

    if (counter <= securityKeyData.getCounter()) {
      throw new U2FException("Counter value smaller than expected!");
    }

    byte[] appIdSha256 = cryto.computeSha256(appId.getBytes());
    byte[] browserDataSha256 = cryto.computeSha256(browserData.getBytes());
    byte[] signedBytes = RawMessageCodec.encodeAuthenticateSignedBytes(
        appIdSha256, userPresence, counter, browserDataSha256);

    Log.info("Verifying signature of bytes " + Hex.encodeHexString(signedBytes));
    if (!cryto.verifySignature(
            cryto.decodePublicKey(securityKeyData.getPublicKey()), signedBytes, signature)) {
      throw new U2FException("Signature is invalid");
    }

    dataStore.updateSecurityKeyCounter(
        sessionData.getAccountName(), securityKeyData.getPublicKey(), counter);

    Log.info("<< processSignResponse");
    return securityKeyData;
  }

  private void verifyBrowserData(JsonElement browserDataAsElement, String messageType,
      EnrollSessionData sessionData) throws U2FException {
    if (!browserDataAsElement.isJsonObject()) {
      throw new U2FException("browserdata has wrong format");
    }

    JsonObject browserData = browserDataAsElement.getAsJsonObject();

    // check that the right "typ" parameter is present in the browserdata JSON
    if (!browserData.has(TYPE_PARAM)) {
      throw new U2FException("bad browserdata: missing 'typ' param");
    }

    String type = browserData.get(TYPE_PARAM).getAsString();
    if (!messageType.equals(type)) {
      throw new U2FException("bad browserdata: bad type " + type);
    }

    // check that the right challenge is in the browserdata
    if (!browserData.has(CHALLENGE_PARAM)) {
      throw new U2FException("bad browserdata: missing 'challenge' param");
    }

    if (browserData.has(ORIGIN_PARAM)) {
      verifyOrigin(browserData.get(ORIGIN_PARAM).getAsString());
    }

    byte[] challengeFromBrowserData =
        Base64.decodeBase64(browserData.get(CHALLENGE_PARAM).getAsString());


    if (!Arrays.equals(challengeFromBrowserData, sessionData.getChallenge())) {
      throw new U2FException("wrong challenge signed in browserdata");
    }

    // TODO: Deal with ChannelID
  }

  private void verifyOrigin(String origin) throws U2FException {
    if (!allowedOrigins.contains(canonicalizeOrigin(origin))) {
      throw new U2FException(origin + " is not a recognized home origin for this backend"
          + Joiner.on(", ").join(allowedOrigins));
    }
  }

  @Override
  public List<SecurityKeyData> getAllSecurityKeys(String accountName) {
    return dataStore.getSecurityKeyData(accountName);
  }

  @Override
  public void removeSecurityKey(String accountName, byte[] publicKey) throws U2FException {
    dataStore.removeSecuityKey(accountName, publicKey);
  }

  private static Set<String> canonicalizeOrigins(Set<String> origins) {
    ImmutableSet.Builder<String> result = ImmutableSet.builder();
    for (String origin : origins) {
      result.add(canonicalizeOrigin(origin));
    }
    return result.build();
  }

  static String canonicalizeOrigin(String url) {
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      throw new RuntimeException("specified bad origin", e);
    }
    return uri.getScheme() + "://" + uri.getAuthority();
  }
}
