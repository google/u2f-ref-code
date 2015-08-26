// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.client.impl;

import java.util.List;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.JsonObject;
import com.google.u2f.U2FConsts;
import com.google.u2f.U2FException;
import com.google.u2f.client.ChannelIdProvider;
import com.google.u2f.client.Crypto;
import com.google.u2f.client.OriginVerifier;
import com.google.u2f.client.U2FClient;
import com.google.u2f.codec.ClientDataCodec;
import com.google.u2f.codec.RawMessageCodec;
import com.google.u2f.key.U2FKey;
import com.google.u2f.key.UserPresenceVerifier;
import com.google.u2f.key.messages.AuthenticateRequest;
import com.google.u2f.key.messages.AuthenticateResponse;
import com.google.u2f.key.messages.RegisterRequest;
import com.google.u2f.key.messages.RegisterResponse;
import com.google.u2f.server.U2FServer;
import com.google.u2f.server.messages.RegisteredKey;
import com.google.u2f.server.messages.RegistrationRequest;
import com.google.u2f.server.messages.RegistrationResponse;
import com.google.u2f.server.messages.U2fSignRequest;
import com.google.u2f.server.messages.SignResponse;

public class U2FClientReferenceImpl implements U2FClient {
  private final Crypto crypto;
  private final OriginVerifier appIdVerifier;
  private final ChannelIdProvider channelIdProvider;
  private final U2FServer server;
  private final U2FKey key;

  public U2FClientReferenceImpl(Crypto crypto, OriginVerifier appIdVerifier,
      ChannelIdProvider channelIdProvider, U2FServer server, U2FKey key) {
    this.crypto = crypto;
    this.appIdVerifier = appIdVerifier;
    this.channelIdProvider = channelIdProvider;
    this.server = server;
    this.key = key;
  }

  @Override
  public void register(String origin, String accountName) throws U2FException {
    RegistrationRequest registrationRequest = server.getRegistrationRequest(accountName, origin);

    String version = registrationRequest.getVersion();
    String serverChallengeBase64 = registrationRequest.getChallenge();
    String appId = registrationRequest.getAppId();
    String sessionId = registrationRequest.getSessionId();

    if (!version.equals(U2FConsts.U2F_V2)) {
      throw new U2FException(String.format("Unsupported protocol version: %s", version));
    }

    appIdVerifier.validateOrigin(appId, origin);

    JsonObject channelIdJson = channelIdProvider.getJsonChannelId();

    String clientData = ClientDataCodec.encodeClientData(ClientDataCodec.REQUEST_TYPE_REGISTER,
        serverChallengeBase64, origin, channelIdJson);

    byte[] appIdSha256 = crypto.computeSha256(appId);
    byte[] clientDataSha256 = crypto.computeSha256(clientData);

    RegisterResponse registerResponse = key.register(new RegisterRequest(appIdSha256,
        clientDataSha256));

    byte[] rawRegisterResponse = RawMessageCodec.encodeRegisterResponse(registerResponse);
    String rawRegisterResponseBase64 = Base64.encodeBase64URLSafeString(rawRegisterResponse);
    String clientDataBase64 = Base64.encodeBase64URLSafeString(clientData.getBytes());

    server.processRegistrationResponse(new RegistrationResponse(rawRegisterResponseBase64,
        clientDataBase64, sessionId), System.currentTimeMillis());
  }

  @Override
  public void authenticate(String origin, String accountName) throws U2FException {
    U2fSignRequest signRequest = server.getSignRequest(accountName, origin);

    String serverChallengeBase64 = signRequest.getChallenge();
    List<RegisteredKey> registeredKeys = signRequest.getRegisteredKeys();
    String version = registeredKeys.get(0).getVersion();
    String appId = registeredKeys.get(0).getAppId();
    String keyHandleBase64 = registeredKeys.get(0).getKeyHandle();
    String sessionId = registeredKeys.get(0).getSessionId();

    if (!version.equals(U2FConsts.U2F_V2)) {
      throw new U2FException(String.format("Unsupported protocol version: %s", version));
    }

    appIdVerifier.validateOrigin(appId, origin);

    JsonObject channelIdJson = channelIdProvider.getJsonChannelId();

    String clientData = ClientDataCodec.encodeClientData(ClientDataCodec.REQUEST_TYPE_AUTHENTICATE,
        serverChallengeBase64, origin, channelIdJson);

    byte[] clientDataSha256 = crypto.computeSha256(clientData);
    byte[] appIdSha256 = crypto.computeSha256(appId);
    byte[] keyHandle = Base64.decodeBase64(keyHandleBase64);
    AuthenticateResponse authenticateResponse = key.authenticate(new AuthenticateRequest(
        UserPresenceVerifier.USER_PRESENT_FLAG, clientDataSha256, appIdSha256, keyHandle));

    byte[] rawAuthenticateResponse = RawMessageCodec.encodeAuthenticateResponse(authenticateResponse);
    String rawAuthenticateResponse64 = Base64.encodeBase64URLSafeString(rawAuthenticateResponse);
    String clientDataBase64 = Base64.encodeBase64URLSafeString(clientData.getBytes());

    server.processSignResponse(
        new SignResponse(keyHandleBase64, rawAuthenticateResponse64, clientDataBase64, sessionId));
  }
}
