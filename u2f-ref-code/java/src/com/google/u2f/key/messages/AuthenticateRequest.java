// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.key.messages;

import java.util.Arrays;
import java.util.Objects;

public class AuthenticateRequest extends U2FRequest {
  public static final byte CHECK_ONLY = 0x07;
  public static final byte USER_PRESENCE_SIGN = 0x03;

  private final byte control;
  private final byte[] challengeSha256;
  private final byte[] applicationSha256;
  private final byte[] keyHandle;

  public AuthenticateRequest(byte control, byte[] challengeSha256, byte[] applicationSha256,
      byte[] keyHandle) {
    this.control = control;
    this.challengeSha256 = challengeSha256;
    this.applicationSha256 = applicationSha256;
    this.keyHandle = keyHandle;
  }

  /** The FIDO Client will set the control byte to one of the following values:
   * 0x07 ("check-only")
   * 0x03 ("enforce-user-presence-and-sign")
   */
  public byte getControl() {
    return control;
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

  /** The key handle obtained during registration. */
  public byte[] getKeyHandle() {
    return keyHandle;
  }

  @Override
  public int hashCode() {
    return Objects.hash(control, challengeSha256, applicationSha256, keyHandle);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AuthenticateRequest other = (AuthenticateRequest) obj;
    return Objects.equals(control, other.control)
        && Arrays.equals(challengeSha256, other.challengeSha256)
        && Arrays.equals(applicationSha256, other.applicationSha256)
        && Arrays.equals(keyHandle, other.keyHandle);
    }
}
