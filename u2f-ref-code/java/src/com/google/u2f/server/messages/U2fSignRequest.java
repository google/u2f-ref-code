// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.messages;

import java.util.List;

import com.google.gson.JsonArray;

public class U2fSignRequest {
  /** The websafe-base64-encoded challenge. */
  private final String challenge;
  /** List of registered keys */
  private List<RegisteredKey> registeredKeys;

  public U2fSignRequest(String challenge, List<RegisteredKey> registeredKeys) {
    this.challenge = challenge;
    this.registeredKeys = registeredKeys;
  }

  public String getChallenge() {
    return challenge;
  }

  public List<RegisteredKey> getRegisteredKeys() {
    return registeredKeys;
  }

  public JsonArray getRegisteredKeysAsJson(String defaultAppId) {
    if (registeredKeys == null) {
      return null;
    }
    JsonArray result = new JsonArray();
    for (RegisteredKey registeredKey : registeredKeys) {
      result.add(registeredKey.getJson(defaultAppId));
    }
    return result;
  }
}
