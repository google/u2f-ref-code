// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.key.messages;

import java.util.Arrays;
import java.util.Objects;

public class AuthenticateResponse extends U2FResponse {
  private final byte userPresence;
  private final int counter;
  private final byte[] signature;

  public AuthenticateResponse(byte userPresence, int counter, byte[] signature) {
    super();
    this.userPresence = userPresence;
    this.counter = counter;
    this.signature = signature;
  }

  /**
   * Bit 0 is set to 1, which means that user presence was verified. (This
   * version of the protocol doesn't specify a way to request authentication
   * responses without requiring user presence.) A different value of Bit 0, as
   * well as Bits 1 through 7, are reserved for future use. The values of Bit 1
   * through 7 SHOULD be 0
   */
  public byte getUserPresence() {
    return userPresence;
  }

  /**
   * This is the big-endian representation of a counter value that the U2F token
   * increments every time it performs an authentication operation.
   */
  public int getCounter() {
    return counter;
  }

  /** This is a ECDSA signature (on P-256) */
  public byte[] getSignature() {
    return signature;
  }

  @Override
  public int hashCode() {
    return Objects.hash(userPresence, counter, signature);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AuthenticateResponse other = (AuthenticateResponse) obj;
    return Objects.equals(counter, other.counter)
        && Arrays.equals(signature, other.signature)
        && Objects.equals(userPresence, other.userPresence);
  }
}
