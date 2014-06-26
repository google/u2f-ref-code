// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.codec;

import com.google.gson.JsonObject;

public class ClientDataCodec {
  // Constants for ClientData.typ
  public static final String REQUEST_TYPE_REGISTER = "navigator.id.finishEnrollment";
  public static final String REQUEST_TYPE_AUTHENTICATE = "navigator.id.getAssertion";

  // Constants for building ClientData.challenge
  public static final String JSON_PROPERTY_REQUEST_TYPE = "typ";
  public static final String JSON_PROPERTY_SERVER_CHALLENGE_BASE64 = "challenge";
  public static final String JSON_PROPERTY_SERVER_ORIGIN = "origin";
  public static final String JSON_PROPERTY_CHANNEL_ID = "cid_pubkey";

  /** Computes ClientData.challenge */
  public static String encodeClientData(String requestType, String serverChallengeBase64,
      String origin, JsonObject jsonChannelId) {
    JsonObject browserData = new JsonObject();
    browserData.addProperty(JSON_PROPERTY_REQUEST_TYPE, requestType);
    browserData.addProperty(JSON_PROPERTY_SERVER_CHALLENGE_BASE64, serverChallengeBase64);
    browserData.add(JSON_PROPERTY_CHANNEL_ID, jsonChannelId);
    browserData.addProperty(JSON_PROPERTY_SERVER_ORIGIN, origin);
    return browserData.toString();
  }
}
