// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.messages;

import java.util.Objects;

public class SignResponse {

  /** websafe-base64 key handle from U2F device */
  private final String keyHandle;

  /** websafe-base64(client data) */
  private final String clientData;

  /** websafe-base64(raw response from U2F device) */
  private final String signatureData;

  /** session id originally passed */
  private final String sessionId;


  public SignResponse(String keyHandle, String signatureData, String clientData, String sessionId) {
    this.keyHandle = keyHandle;
    this.signatureData = signatureData;
    this.clientData = clientData;
    this.sessionId = sessionId;
  }

  public String getKeyHandle() {
    return keyHandle;
  }

  public String getClientData() {
    return clientData;
  }

  public String getSignatureData() {
    return signatureData;
  }

  public String getSessionId() {
    return sessionId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(keyHandle, clientData, signatureData, sessionId);
  }

  @Override
  public boolean equals(Object obj) {
    if (getClass() != obj.getClass())
      return false;
    SignResponse other = (SignResponse) obj;
    return Objects.equals(keyHandle, other.keyHandle)
        && Objects.equals(clientData, other.clientData)
        && Objects.equals(signatureData, other.signatureData)
        && Objects.equals(sessionId, other.sessionId);
  }
}
