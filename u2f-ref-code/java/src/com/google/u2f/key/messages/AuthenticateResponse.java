// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.key.messages;

import java.util.Arrays;
import java.util.Objects;

import com.google.common.base.Preconditions;

public class AuthenticateResponse extends U2FResponse {
  private final byte controlFlags;
  private final int counter;
  private final byte[] signature;

  public AuthenticateResponse(byte controlFlags, int counter, byte[] signature) {
    super();
    this.controlFlags = controlFlags;
    this.counter = counter;
    this.signature = Preconditions.checkNotNull(signature);
  }

  /**
   * Returns a bitfield of flags. Setting the least significant bit indicates that user presence was 
   * verified. (This version of the protocol doesn't specify a way to request authentication 
   * responses without requiring user presence.) Setting the second least significant bit indicates
   * this response contains a TransferAccessMessage. A different value for the LSB as well as the 
   * remaining bits are reserved for future use.
   */
  public byte getControlFlagByte() {
    return controlFlags;
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
    return Objects.hash(controlFlags, counter, signature);
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
        && Objects.equals(controlFlags, other.controlFlags);
  }
}
