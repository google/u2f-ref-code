// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.messages;

import java.util.List;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.u2f.server.data.SecurityKeyData;
import com.google.u2f.server.data.SecurityKeyData.Transports;

public class RegisteredKey {
  /**
   * Version of the protocol that the to-be-registered U2F token must speak. For
   * the version of the protocol described herein, must be "U2F_V2"
   */
  private final String version;

  /**
   * websafe-base64 encoding of the key handle obtained from the U2F token
   * during registration.
   */
  private final String keyHandle;

  /**
   * 
   */
  private final List<Transports> transports;
  /**
   * The application id that the RP would like to assert. The U2F token will
   * enforce that the key handle provided above is associated with this
   * application id. The browser enforces that the calling origin belongs to the
   * application identified by the application id.
   */
  private final String appId;

  /**
   * A session id created by the RP. The RP can opaquely store things like
   * expiration times for the sign-in session, protocol version used, public key
   * expected to sign the identity assertion, etc. The response from the API
   * will include the sessionId. This allows the RP to fire off multiple signing
   * requests, and associate the responses with the correct request
   */
  private final String sessionId;

  public RegisteredKey(String version, String keyHandle, List<Transports> transports,
      String appId, String sessionId) {
    super();
    this.version = version;
    this.keyHandle = keyHandle;
    this.transports = transports;
    this.appId = appId;
    this.sessionId = sessionId;
  }

  public String getVersion() {
    return version;
  }

  public List<Transports> getTransports() {
    return transports;
  }
  
  public String getAppId() {
    return appId;
  }

  public String getKeyHandle() {
    return keyHandle;
  }

  public String getSessionId() {
    return sessionId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, keyHandle, transports, appId, sessionId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    RegisteredKey other = (RegisteredKey) obj;
    return Objects.equals(version, other.version)
        && Objects.equals(keyHandle, other.keyHandle)
        && SecurityKeyData.containSameTransports(transports, other.transports)
        && Objects.equals(appId, other.appId)
        && Objects.equals(sessionId, other.sessionId);
  }

  private JsonArray getTransportsAsJson() {
    if (this.transports == null) {
      return null;
    }
    JsonArray transportsArray =  new JsonArray();
    for (Transports transport : this.transports) {
      transportsArray.add(new JsonPrimitive(transport.toString()));
    }
    return transportsArray;
  }

  public JsonObject getJson(String defaultAppId) {
    JsonObject result = new JsonObject();
    if (appId != null && !appId.equals(defaultAppId)) {
      result.addProperty("appId", appId);
    }
    result.addProperty("version", version);
    result.addProperty("keyHandle", keyHandle);
    result.addProperty("sessionId", sessionId);
    JsonArray transportsJson = getTransportsAsJson();
    if (transportsJson != null) {
      result.add("transports", transportsJson);
    }
    return result;
  }
}
