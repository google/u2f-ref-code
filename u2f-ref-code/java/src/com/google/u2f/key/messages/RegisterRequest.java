// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.key.messages;

import java.util.Arrays;
import java.util.Objects;

public class RegisterRequest extends U2FRequest {
  private final byte[] challengeSha256;
  private final byte[] applicationSha256;

  public RegisterRequest(byte[] applicationSha256, byte[] challengeSha256) {
    this.challengeSha256 = challengeSha256;
    this.applicationSha256 = applicationSha256;
  }

  /**
   * The challenge parameter is the SHA-256 hash of the Client Data, a
   * stringified JSON datastructure that the FIDO Client prepares. Among other
   * things, the Client Data contains the challenge from the relying party
   * (hence the name of the parameter). See below for a detailed explanation of
   * Client Data.
   */
  public byte[] getChallengeSha256() {
    return challengeSha256;
  }

  /**
   * The application parameter is the SHA-256 hash of the application identity
   * of the application requesting the registration
   */
  public byte[] getApplicationSha256() {
    return applicationSha256;
  }

  @Override
  public int hashCode() {
    return Objects.hash(applicationSha256, challengeSha256);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    RegisterRequest other = (RegisterRequest) obj;
    return Arrays.equals(applicationSha256, other.applicationSha256)
        && Arrays.equals(challengeSha256, other.challengeSha256);
  }
}
