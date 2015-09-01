// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROctetString;

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
import com.google.u2f.server.messages.RegistrationRequest;
import com.google.u2f.server.messages.RegistrationResponse;
import com.google.u2f.server.messages.SignRequest;
import com.google.u2f.server.messages.SignResponse;

public class U2FServerReferenceImpl implements U2FServer {
  
  // Object Identifier for the attestation certificate transport extension fidoU2FTransports
  private static final String TRANSPORT_EXTENSION_OID = "1.3.6.1.4.1.45724.2.1.1";

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

  public U2FServerReferenceImpl(ChallengeGenerator challengeGenerator,
      DataStore dataStore, Crypto cryto, Set<String> origins) {
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
  public SecurityKeyData processRegistrationResponse(RegistrationResponse registrationResponse,
      long currentTimeInMillis) throws U2FException {
    Log.info(">> processRegistrationResponse");

    String sessionId = registrationResponse.getSessionId();
    String browserDataBase64 = registrationResponse.getBd();
    String rawRegistrationDataBase64 = registrationResponse.getRegistrationData();

    EnrollSessionData sessionData = dataStore.getEnrollSessionData(sessionId);

    if (sessionData == null) {
      throw new U2FException("Unknown session_id");
    }

    String appId = sessionData.getAppId();
    String browserData = new String(Base64.decodeBase64(browserDataBase64));
    byte[] rawRegistrationData = Base64.decodeBase64(rawRegistrationDataBase64);
    Log.info("-- Input --");
    Log.info("  sessionId: " + sessionId);
    Log.info("  challenge: " + Hex.encodeHexString(sessionData.getChallenge()));
    Log.info("  accountName: " + sessionData.getAccountName());
    Log.info("  browserData: " + browserData);
    Log.info("  rawRegistrationData: " + Hex.encodeHexString(rawRegistrationData));

    RegisterResponse registerResponse = RawMessageCodec.decodeRegisterResponse(rawRegistrationData);

    byte[] userPublicKey = registerResponse.getUserPublicKey();
    byte[] keyHandle = registerResponse.getKeyHandle();
    X509Certificate attestationCertificate = registerResponse.getAttestationCertificate();
    byte[] signature = registerResponse.getSignature();
    List<Transports> transports = null;
    try {
      transports = parseTransportsExtension(attestationCertificate);
    } catch (CertificateParsingException e1) {
      Log.warning("Could not parse transports extension " + e1.getMessage());
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
    byte[] browserDataSha256 = cryto.computeSha256(browserData.getBytes());
    byte[] signedBytes = RawMessageCodec.encodeRegistrationSignedBytes(appIdSha256, browserDataSha256,
        keyHandle, userPublicKey);

    Set<X509Certificate> trustedCertificates = dataStore.getTrustedCertificates();
    if (!trustedCertificates.contains(attestationCertificate)) {
      Log.warning("attestion cert is not trusted");    
    }

    verifyBrowserData(new JsonParser().parse(browserData), "navigator.id.finishEnrollment", sessionData);
    
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
  public List<SignRequest> getSignRequest(String accountName, String appId) throws U2FException {
    Log.info(">> getSignRequest " + accountName);

    List<SecurityKeyData> securityKeyDataList = dataStore.getSecurityKeyData(accountName);

    ImmutableList.Builder<SignRequest> result = ImmutableList.builder();
    
    for (SecurityKeyData securityKeyData : securityKeyDataList) {
      byte[] challenge = challengeGenerator.generateChallenge(accountName);

      SignSessionData sessionData = new SignSessionData(accountName, appId, 
          challenge, securityKeyData.getPublicKey());
      String sessionId = dataStore.storeSessionData(sessionData);

      byte[] keyHandle = securityKeyData.getKeyHandle();

      Log.info("-- Output --");
      Log.info("  sessionId: " + sessionId);
      Log.info("  challenge: " + Hex.encodeHexString(challenge));
      Log.info("  keyHandle: " + Hex.encodeHexString(keyHandle));

      String challengeBase64 = Base64.encodeBase64URLSafeString(challenge);
      String keyHandleBase64 = Base64.encodeBase64URLSafeString(keyHandle);

      Log.info("<< getSignRequest " + accountName);
      result.add(new SignRequest(U2FConsts.U2F_V2, challengeBase64, appId, keyHandleBase64, sessionId));
    }
    return result.build();
  }

  @Override
  public SecurityKeyData processSignResponse(SignResponse signResponse) throws U2FException {
    Log.info(">> processSignResponse");

    String sessionId = signResponse.getSessionId();
    String browserDataBase64 = signResponse.getBd();
    String rawSignDataBase64 = signResponse.getSign();

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

    verifyBrowserData(new JsonParser().parse(browserData), "navigator.id.getAssertion", sessionData);
    
    AuthenticateResponse authenticateResponse = RawMessageCodec.decodeAuthenticateResponse(rawSignData);
    byte userPresence = authenticateResponse.getUserPresence();
    int counter = authenticateResponse.getCounter();
    byte[] signature = authenticateResponse.getSignature();

    Log.info("-- Parsed rawSignData --");
    Log.info("  userPresence: " + Integer.toHexString(userPresence & 0xFF));
    Log.info("  counter: " + counter);
    Log.info("  signature: " + Hex.encodeHexString(signature));

    if (userPresence != UserPresenceVerifier.USER_PRESENT_FLAG) {
      throw new U2FException("User presence invalid during authentication");
    }

    if (counter <= securityKeyData.getCounter()) {
      throw new U2FException("Counter value smaller than expected!");      
    }
    
    byte[] appIdSha256 = cryto.computeSha256(appId.getBytes());
    byte[] browserDataSha256 = cryto.computeSha256(browserData.getBytes());
    byte[] signedBytes = RawMessageCodec.encodeAuthenticateSignedBytes(appIdSha256, userPresence,
        counter, browserDataSha256);

    Log.info("Verifying signature of bytes " + Hex.encodeHexString(signedBytes));
    if (!cryto.verifySignature(cryto.decodePublicKey(securityKeyData.getPublicKey()), signedBytes,
        signature)) {
      throw new U2FException("Signature is invalid");
    }

    dataStore.updateSecurityKeyCounter(sessionData.getAccountName(), securityKeyData.getPublicKey(), counter);
    
    Log.info("<< processSignResponse");
    return securityKeyData;
  }

  /**
   * Parses a transport extension from an attestation certificate and returns
   * a List of HardwareFeatures supported by the security key. The specification of
   * the HardwareFeatures in the certificate should match their internal definition in
   * device_auth.proto
   *
   * <p>The expected transport extension value is a BIT STRING containing the enabled
   * transports:
   *
   *  <p>FIDOU2FTransports ::= BIT STRING {
   *       bluetoothRadio(0), -- Bluetooth Classic
   *       bluetoothLowEnergyRadio(1),
   *       uSB(2),
   *       nFC(3)
   *     }
   *
   *   <p>Note that the BIT STRING must be wrapped in an OCTET STRING.
   *   An extension that encodes BT, BLE, and NFC then looks as follows:
   *
   *   <p>SEQUENCE (2 elem)
   *      OBJECT IDENTIFIER 1.3.6.1.4.1.45724.2.1.1
   *      OCTET STRING (1 elem)
   *        BIT STRING (4 bits) 1101
   *
   * @param cert the certificate to parse for extension
   * @return the supported transports as a List of HardwareFeatures or null if no extension
   * was found
   */
  public static List<Transports> parseTransportsExtension(X509Certificate cert)
      throws CertificateParsingException{
    byte[] extValue = cert.getExtensionValue(TRANSPORT_EXTENSION_OID);
    LinkedList<Transports> transportsList = new LinkedList<Transports>();
    if (extValue == null) {
      // No transports extension found.
      return null;
    }

    ASN1InputStream ais = new ASN1InputStream(extValue);
    ASN1Object asn1Object;
    // Read out the OctetString
    try {
      asn1Object = ais.readObject();
      ais.close();
    } catch (IOException e) {
      throw new CertificateParsingException("Not able to read object in transports extenion", e);
    }

    if (asn1Object == null || !(asn1Object instanceof DEROctetString)) {
      throw new CertificateParsingException("No Octet String found in transports extension");
    }
    DEROctetString octet = (DEROctetString) asn1Object;

    // Read out the BitString
    ais = new  ASN1InputStream(octet.getOctets());
    try {
      asn1Object = ais.readObject();
      ais.close();
    } catch (IOException e) {
      throw new CertificateParsingException("Not able to read object in transports extension", e);
    }
    if (asn1Object == null || !(asn1Object instanceof DERBitString)) {
      throw new CertificateParsingException("No BitString found in transports extension");
    }
    DERBitString bitString = (DERBitString) asn1Object;

    byte [] values = bitString.getBytes();
    BitSet bitSet = BitSet.valueOf(values);

    // We might have more defined transports than used by the extension
    for (int i = 0; i < 8; i++) {
      if (bitSet.get(8 - i - 1)) {
        transportsList.add(Transports.values()[i]);
      }
    }
    return transportsList;
  }

  private void verifyBrowserData(JsonElement browserDataAsElement, 
      String messageType, EnrollSessionData sessionData) throws U2FException {
    
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
      throw new U2FException(origin +
          " is not a recognized home origin for this backend");
    }
  }

  @Override
  public List<SecurityKeyData> getAllSecurityKeys(String accountName) {
    return dataStore.getSecurityKeyData(accountName);
  }

  @Override
  public void removeSecurityKey(String accountName, byte[] publicKey)
      throws U2FException {
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
