// Copyright 2014 Google Inc. All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.u2f.server.data;

import java.io.Serializable;

public class EnrollSessionData implements Serializable {
  private static final long serialVersionUID = 1750990095756334568L;

  private final String accountName;
  private final byte[] challenge;
  private final String appId;

  public EnrollSessionData(String accountName, String appId, byte[] challenge) {
    this.accountName = accountName;
    this.challenge = challenge;
    this.appId = appId;
  }

  public String getAccountName() {
    return accountName;
  }

  public byte[] getChallenge() {
    return challenge;
  }

  public String getAppId() {
	return appId;
  }
}
