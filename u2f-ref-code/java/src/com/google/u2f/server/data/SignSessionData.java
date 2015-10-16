// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.data;

public class SignSessionData extends EnrollSessionData {
  private static final long serialVersionUID = -1374014642398686120L;

  private final byte[] publicKey;

  public SignSessionData(String accountName, String appId, byte[] challenge, byte[] publicKey) {
    super(accountName, appId, challenge);
    this.publicKey = publicKey;
  }

  public byte[] getPublicKey() {
    return publicKey;
  }
}
